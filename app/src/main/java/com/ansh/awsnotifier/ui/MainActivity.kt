package com.ansh.awsnotifier.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import com.ansh.awsnotifier.App
import com.ansh.awsnotifier.aws.DeviceRegistrar
import com.ansh.awsnotifier.session.UserSession
import com.ansh.awsnotifier.ui.dialogs.EnterArnDialog
import com.ansh.awsnotifier.ui.onboarding.OnboardingActivity
import com.ansh.awsnotifier.ui.theme.AppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val ALL_REGIONS_DISPLAY_NAME = "All Regions"

    // UI State
    private val _topics = mutableStateListOf<TopicUiModel>()
    private val _regions = mutableStateListOf<String>()
    private val _currentRegion = mutableStateOf(ALL_REGIONS_DISPLAY_NAME)
    private val _isLoading = mutableStateOf(true)
    private val _showArnDialogRegion = mutableStateOf<String?>(null)

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Onboarding check
        if (!UserSession.isOnboardingComplete(this) ||
            UserSession.getCredentials(this) == null
        ) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }

        setContent {
            AppTheme {
                MainScreen(
                    topics = _topics,
                    regions = _regions,
                    currentRegion = _currentRegion.value,
                    isLoading = _isLoading.value,
                    onRegionSelected = { region ->
                        handleRegionSelection(region)
                    },
                    onRefresh = { loadTopics() },
                    onAddTopic = { showAddTopicArnDialog() },
                    onCreateTopic = { showCreateTopicDialog() },
                    onSettingsClick = {
                        startActivity(Intent(this, SettingsActivity::class.java))
                    },
                    onShowFcmToken = { showFcmTokenDialog() },
                    onSubscribe = { arn -> subscribe(arn) },
                    onUnsubscribe = { arn -> unsubscribe(arn) },
                    onDelete = { arn -> confirmDeleteTopic(arn) },
                    onSendMessage = { arn -> showSendMessageDialog(arn) },
                    onCopyArn = { arn -> copyToClipboard("Topic ARN", arn) }
                )

                // ARN Dialog Host
                _showArnDialogRegion.value?.let { region ->
                    EnterArnDialog(
                        region = region,
                        onDismiss = { _showArnDialogRegion.value = null },
                        onConfirm = { arn: String ->
                            UserSession.savePlatformArnForRegion(this, region, arn)
                            _showArnDialogRegion.value = null
                            _isLoading.value = true
                            scope.launch {
                                waitForFcmToken()
                                DeviceRegistrar.autoRegister(this@MainActivity)
                                _isLoading.value = false
                                loadTopics()
                            }
                        }
                    )
                }
            }
        }

        if (intent.hasExtra("message")) {
            val title = intent.getStringExtra("title")
            val message = intent.getStringExtra("message")
            val topicArn = intent.getStringExtra("topicArn")
            val timestamp = intent.getLongExtra("timestamp", System.currentTimeMillis())
            // Note: Simplification - we might want to show a dialog or navigate to detail
            // For now, let's just toast or log, or show a simple alert since we are in Compose
            // Ideally we'd have a 'notificationDialogState' in Compose.
            // Leaving as TODO or simple Toast for now to not overcomplicate the migration
            Toast.makeText(this, "Notification: $title", Toast.LENGTH_LONG).show()
        }

        val app = application as App
        if (!app.hasCredentials()) {
            app.loadCredentialsIfAvailable()
        }

        // Initialize Data
        scope.launch {
            try {
                loadAvailableRegions()

                // Set initial region from session
                val savedRegion = UserSession.getCurrentRegion(this@MainActivity)
                if (savedRegion != null && _regions.contains(savedRegion)) {
                    _currentRegion.value = savedRegion
                }

                waitForFcmToken()
                DeviceRegistrar.autoRegister(this@MainActivity)

                loadTopics()
            } catch (e: Exception) {
                Log.e("MainActivity", "Error initialization", e)
            } finally {
                _isLoading.value = false
            }
        }

        askNotificationPermission()
    }

    private fun handleRegionSelection(region: String) {
        _currentRegion.value = region
        UserSession.saveCurrentRegion(this, region)

        if (region == ALL_REGIONS_DISPLAY_NAME) {
            loadTopics()
            return
        }

        val arn = UserSession.getPlatformArnForRegion(this, region)
        if (arn.isNullOrEmpty()) {
            _showArnDialogRegion.value = region
            return
        }

        scope.launch {
            _isLoading.value = true
            try {
                waitForFcmToken()
                DeviceRegistrar.autoRegister(this@MainActivity)
                loadTopics()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadTopics() {
        scope.launch {
            _isLoading.value = true
            val app = application as App

            if (app.snsManager == null && app.hasCredentials()) {
                app.initSnsManager()
            }

            val sns = app.snsManager ?: run {
                _isLoading.value = false
                return@launch
            }

            try {
                if (_regions.isEmpty()) useFallbackRegions()

                val selectedRegion = _currentRegion.value
                // If selected region is not in the list (e.g. data corruption), default to All
                if (!_regions.contains(selectedRegion)) {
                    _currentRegion.value = ALL_REGIONS_DISPLAY_NAME
                }

                val regionsToQuery = if (_currentRegion.value == ALL_REGIONS_DISPLAY_NAME) {
                    // Filter out "All Regions" string from actual query
                    _regions.filter { it != ALL_REGIONS_DISPLAY_NAME }
                } else {
                    listOf(_currentRegion.value)
                }

                val topicArns = sns.listAllTopics(regionsToQuery)
                val localSubs = UserSession.getAllSubscriptions(this@MainActivity)

                val uiModels = topicArns.map { arn ->
                    val existing = localSubs.find { it.topicArn == arn }
                    TopicUiModel(
                        arn = arn,
                        name = arn.substringAfterLast(":"),
                        region = try {
                            arn.split(":")[3]
                        } catch (e: Exception) {
                            "unknown"
                        },
                        isSubscribed = existing != null,
                        subscriptionArn = existing?.subscriptionArn
                    )
                }

                _topics.clear()
                _topics.addAll(uiModels)

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@MainActivity, "Failed to load topics", Toast.LENGTH_SHORT)
                    .show()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun loadAvailableRegions() = withContext(Dispatchers.IO) {
        try {
            val app = application as App
            if (app.snsManager == null && app.hasCredentials()) app.initSnsManager()

            val sns = app.snsManager
            val fetchedRegions = sns?.fetchAvailableRegions() ?: emptyList()

            withContext(Dispatchers.Main) {
                _regions.clear()
                _regions.add(ALL_REGIONS_DISPLAY_NAME)
                if (fetchedRegions.isNotEmpty()) {
                    _regions.addAll(fetchedRegions)
                } else {
                    useFallbackRegions()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) { useFallbackRegions() }
        }
    }

    private fun useFallbackRegions() {
        val fallback = listOf(
            "us-east-1", "us-east-2", "us-west-1", "us-west-2",
            "ap-south-1", "ap-southeast-1", "ap-southeast-2", "ap-northeast-1",
            "eu-west-1", "eu-central-1", "sa-east-1"
        )
        if (!_regions.contains(ALL_REGIONS_DISPLAY_NAME)) {
            _regions.add(ALL_REGIONS_DISPLAY_NAME)
        }
        _regions.addAll(fallback.filter { !_regions.contains(it) })
    }

    private fun subscribe(topicArn: String) {
        scope.launch {
            val app = application as App
            val sns = app.snsManager ?: return@launch

            // Extract region
            val region = try {
                topicArn.split(":")[3]
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Invalid ARN", Toast.LENGTH_SHORT).show()
                return@launch
            }

            var endpoint = UserSession.getDeviceEndpointArn(this@MainActivity)
            val isRegistered = endpoint != null && endpoint.contains(":$region:")

            if (!isRegistered) {
                val platformArn = UserSession.getPlatformArnForRegion(this@MainActivity, region)
                if (platformArn.isNullOrEmpty()) {
                    _showArnDialogRegion.value = region
                    return@launch
                }

                _isLoading.value = true
                try {
                    waitForFcmToken()
                    DeviceRegistrar.registerForRegion(this@MainActivity, region)
                    endpoint = UserSession.getDeviceEndpointArn(this@MainActivity)
                } catch (e: Exception) {
                    Toast.makeText(
                        this@MainActivity,
                        "Registration failed: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    _isLoading.value = false
                    return@launch
                } finally {
                    _isLoading.value = false
                }
            }

            try {
                val subArn = sns.subscribe(topicArn, endpoint!!)
                UserSession.saveSubscription(this@MainActivity, subArn, topicArn, region)

                // Update the UI state directly
                val index = _topics.indexOfFirst { it.arn == topicArn }
                if (index != -1) {
                    val oldTopic = _topics[index]
                    _topics[index] = oldTopic.copy(isSubscribed = true, subscriptionArn = subArn)
                }
                
                Toast.makeText(this@MainActivity, "Subscribed!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Subscribe failed: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun unsubscribe(subscriptionArn: String) {
        scope.launch {
            val app = application as App
            val sns = app.snsManager ?: return@launch
            try {
                sns.unsubscribe(subscriptionArn)
                UserSession.removeSubscription(this@MainActivity, subscriptionArn)

                // Update the UI state directly
                val index = _topics.indexOfFirst { it.subscriptionArn == subscriptionArn }
                if (index != -1) {
                    val oldTopic = _topics[index]
                    _topics[index] = oldTopic.copy(isSubscribed = false, subscriptionArn = null)
                }
                
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Unsubscribe failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun confirmDeleteTopic(topicArn: String) {
        AlertDialog.Builder(this)
            .setTitle("Delete Topic")
            .setMessage("Are you sure you want to delete this topic?")
            .setPositiveButton("Delete") { _, _ -> deleteTopic(topicArn) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteTopic(topicArn: String) {
        scope.launch {
            val app = application as App
            val sns = app.snsManager ?: return@launch
            _isLoading.value = true

            try {
                val subscriptions = UserSession.getAllSubscriptions(this@MainActivity)
                    .filter { it.topicArn == topicArn }

                subscriptions.forEach {
                    try {
                        sns.unsubscribe(it.subscriptionArn)
                    } catch (_: Exception) {
                    }
                }

                sns.deleteTopic(topicArn)
                UserSession.removeSubscriptionsByTopicArn(this@MainActivity, topicArn)
                loadTopics() // Still needs full reload to remove from list
            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Failed to delete: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Dialogs & Helpers

    private fun showSendMessageDialog(topicArn: String) {
        val input = android.widget.EditText(this).apply {
            hint = "Message"
        }
        AlertDialog.Builder(this)
            .setTitle("Send Message")
            .setView(input)
            .setPositiveButton("Send") { _, _ ->
                if (input.text.isNotEmpty()) publishMessage(topicArn, input.text.toString())
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun publishMessage(topicArn: String, message: String) {
        scope.launch {
            val app = application as App
            val sns = app.snsManager ?: return@launch
            try {
                val sdf = SimpleDateFormat("dd MMM yyyy | HH:mm:ss", Locale.getDefault())
                val fcmPayload = JSONObject().apply {
                    put("notification", JSONObject().apply {
                        put("title", "AWS SNS Notification")
                        put("body", message)
                    })
                    put("data", JSONObject().apply {
                        put("message", message)
                        put("topicArn", topicArn)
                        put("timestamp", sdf.format(Date()))
                    })
                }
                val snsMessage = JSONObject().apply {
                    put("default", message)
                    put("GCM", fcmPayload.toString())
                }
                sns.publish(topicArn, snsMessage.toString(), messageStructure = "json")
                Toast.makeText(this@MainActivity, "Message published", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Publish failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showCreateTopicDialog() {
        if (_currentRegion.value == ALL_REGIONS_DISPLAY_NAME) {
            Toast.makeText(this, "Select a specific region first", Toast.LENGTH_SHORT).show()
            return
        }
        val input = android.widget.EditText(this).apply { hint = "Topic Name" }
        AlertDialog.Builder(this)
            .setTitle("Create Topic")
            .setView(input)
            .setPositiveButton("Create") { _, _ ->
                createTopic(input.text.toString())
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun createTopic(name: String) {
        if (!name.matches(Regex("^[a-zA-Z0-9_-]{1,256}${'$'}"))) {
            Toast.makeText(this, "Invalid name format", Toast.LENGTH_LONG).show()
            return
        }
        scope.launch {
            _isLoading.value = true
            try {
                val app = application as App
                val sns = app.snsManager!!
                sns.createTopic(name, _currentRegion.value)
                loadTopics()
                Toast.makeText(this@MainActivity, "Topic created", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun showAddTopicArnDialog() {
        val input = android.widget.EditText(this).apply { hint = "Topic ARN" }
        AlertDialog.Builder(this)
            .setTitle("Subscribe to ARN")
            .setView(input)
            .setPositiveButton("Subscribe") { _, _ ->
                if (input.text.isNotEmpty()) subscribe(input.text.toString())
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showFcmTokenDialog() {
        val token = UserSession.getFcmToken(this) ?: "No token"
        val endpoint = UserSession.getDeviceEndpointArn(this) ?: "Not registered"
        val msg = "Token: $token\n\nEndpoint: $endpoint"

        AlertDialog.Builder(this)
            .setTitle("Debug Info")
            .setMessage(msg)
            .setPositiveButton("Copy Token") { _, _ -> copyToClipboard("FCM Token", token) }
            .setNeutralButton("Copy Endpoint") { _, _ -> copyToClipboard("Endpoint", endpoint) }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun copyToClipboard(label: String, text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(this, "$label copied", Toast.LENGTH_SHORT).show()
    }

    private suspend fun waitForFcmToken(): String {
        return try {
            val token = com.ansh.awsnotifier.aws.FirebaseTokenProvider.getToken()
            UserSession.saveFcmToken(this, token)
            token
        } catch (e: Exception) {
            ""
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
