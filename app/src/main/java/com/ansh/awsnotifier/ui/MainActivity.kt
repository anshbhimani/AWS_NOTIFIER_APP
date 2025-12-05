package com.ansh.awsnotifier.ui

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.ansh.awsnotifier.App
import com.ansh.awsnotifier.R
import com.ansh.awsnotifier.aws.DeviceRegistrar
import com.ansh.awsnotifier.databinding.ActivityMainBinding
import com.ansh.awsnotifier.session.UserSession
import com.ansh.awsnotifier.ui.adapters.TopicAdapter
import com.ansh.awsnotifier.ui.dialogs.EnterArnDialog
import com.ansh.awsnotifier.ui.onboarding.OnboardingActivity
import kotlinx.coroutines.CompletableDeferred
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

    private lateinit var binding: ActivityMainBinding
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val regions = listOf(
        "us-east-1", "us-east-2",
        "us-west-1", "us-west-2",
        "ap-south-1", "ap-southeast-1",
        "ap-southeast-2", "ap-northeast-1",
        "eu-west-1", "eu-central-1",
        "sa-east-1"
    )

    private val showArnDialogForRegion = mutableStateOf<String?>(null)

    private lateinit var topicAdapter: TopicAdapter

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    private val fcmTokenDeferred = CompletableDeferred<String>()

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

        if (intent.hasExtra("message")) {
            val title = intent.getStringExtra("title")
            val message = intent.getStringExtra("message")
            val topicArn = intent.getStringExtra("topicArn")
            val timestamp = intent.getLongExtra("timestamp", System.currentTimeMillis())

            showNotificationDialog(title, message, topicArn, timestamp)
        }


        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val app = application as App
        if (!app.hasCredentials()) {
            app.loadCredentialsIfAvailable()
        }

        // Ensure initial region ARN exists
        val region = UserSession.getCurrentRegion(this)
        if (region != null && UserSession.getPlatformArnForRegion(this, region).isNullOrEmpty()) {
            showArnDialogForRegion.value = region
        }

        setupComposeArnDialogHost()
        setupRegionSpinner()
        setupTopicsRecycler()
        setupTopicControls()
        setupSearch()
        loadInitialState()
        askNotificationPermission()

        // Auto-register + initial topic load
        scope.launch {
            waitForFcmToken()
            DeviceRegistrar.autoRegister(this@MainActivity)
            updateRegistrationStatus()
        }
        loadTopics()
    }

    private fun deleteTopic(topicArn: String) {
        scope.launch {
            val app = application as App
            val sns = app.snsManager ?: return@launch

            try {
                sns.deleteTopic(topicArn)
                // Fix: Use the new function to remove all local subscriptions associated with the topicArn
                UserSession.removeSubscriptionsByTopicArn(this@MainActivity, topicArn) 
                loadTopics()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@MainActivity, "Failed to delete topic", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // =======================================================================
    // Compose Dialog (for platform application ARN)
    // =======================================================================
    private fun setupComposeArnDialogHost() {
        binding.composeDialogHost.setContent {
            val region = showArnDialogForRegion.value
            if (region != null) {
                EnterArnDialog(
                    region = region,
                    onDismiss = { showArnDialogForRegion.value = null },
                    onConfirm = { arn: String ->
                        UserSession.savePlatformArnForRegion(this, region, arn)
                        showArnDialogForRegion.value = null
                        binding.progressBar.visibility = View.VISIBLE

                        scope.launch {
                            waitForFcmToken()
                            DeviceRegistrar.autoRegister(this@MainActivity)
                            updateRegistrationStatus()
                            binding.progressBar.visibility = View.GONE
                        }
                    }
                )
            }
        }
        binding.composeDialogHost.visibility = View.VISIBLE
    }

    // =======================================================================
    // Topics loading (AWS SNS)
    // =======================================================================
    private fun loadTopics() {
        scope.launch {
            binding.progressBar.visibility = View.VISIBLE
            binding.swipeRefresh.isRefreshing = false

            val app = application as App

            if (app.snsManager == null && app.hasCredentials()) {
                app.initSnsManager()
            }

            val sns = app.snsManager ?: run {
                binding.progressBar.visibility = View.GONE
                return@launch
            }

            try {
                val topicArns = sns.listAllTopics()
                val localSubs = UserSession.getAllSubscriptions(this@MainActivity)

                val topicItems = topicArns.map { arn ->
                    val existing = localSubs.find { it.topicArn == arn }
                    TopicItem(
                        topicArn = arn,
                        topicName = arn.substringAfterLast(":"),
                        isSubscribed = existing != null,
                        subscriptionArn = existing?.subscriptionArn
                    )
                }

                binding.topicCountText.text = "${topicItems.size} topics available"
                binding.emptyState.visibility =
                    if (topicItems.isEmpty()) View.VISIBLE else View.GONE

                topicAdapter.submitListWithBackup(topicItems)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@MainActivity, "Failed to load topics", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    // =======================================================================
    // Region Spinner
    // =======================================================================
    private fun setupRegionSpinner() {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            regions
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.regionSpinner.adapter = adapter

        val saved = UserSession.getCurrentRegion(this)
        val idx = regions.indexOf(saved)
        if (idx >= 0) binding.regionSpinner.setSelection(idx)

        binding.regionSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val region = regions[position]
                    UserSession.saveCurrentRegion(this@MainActivity, region)
                    binding.currentRegionText.text = "Current: $region"

                    val arn = UserSession.getPlatformArnForRegion(this@MainActivity, region)
                    if (arn.isNullOrEmpty()) {
                        showArnDialogForRegion.value = region
                        return
                    }

                    scope.launch {
                        waitForFcmToken()
                        DeviceRegistrar.autoRegister(this@MainActivity)
                        updateRegistrationStatus()
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
    }

    // =======================================================================
    // Topics Recycler + controls
    // =======================================================================
    private fun setupTopicsRecycler() {
        topicAdapter = TopicAdapter(
            onSubscribe = { topicArn -> subscribe(topicArn) },
            onUnsubscribe = { subArn -> unsubscribe(subArn) },
            onSendMessage = { topicArn -> showSendMessageDialog(topicArn) },
            onDelete = { topicArn ->
                // Show confirmation dialog before deleting
                AlertDialog.Builder(this)
                    .setTitle("Delete Topic")
                    .setMessage("Are you sure you want to delete this topic? This action cannot be undone.")
                    .setPositiveButton("Delete") { _, _ ->
                        deleteTopic(topicArn)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )

        binding.topicsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.topicsRecyclerView.adapter = topicAdapter
    }

    private fun setupTopicControls() {
        binding.btnRefreshTopics.setOnClickListener {
            loadTopics()
        }

        binding.swipeRefresh.setOnRefreshListener {
            loadTopics()
        }

        binding.btnCreateTopic.setOnClickListener {
            showCreateTopicDialog()
        }

        binding.btnAddCustomTopic.setOnClickListener {
            showAddTopicArnDialog()
        }
    }

    private fun setupSearch() {
        binding.searchTopics.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                topicAdapter.filter(newText ?: "")
                return true
            }
        })
    }

    // =======================================================================
    // Subscribe / Unsubscribe
    // =======================================================================
    private fun subscribe(topicArn: String) {
        scope.launch {
            val app = application as App
            val sns = app.snsManager ?: return@launch

            val endpoint = UserSession.getDeviceEndpointArn(this@MainActivity)
            if (endpoint == null) {
                Toast.makeText(
                    this@MainActivity,
                    "Device not registered yet",
                    Toast.LENGTH_SHORT
                ).show()
                return@launch
            }

            try {
                val subArn = sns.subscribe(topicArn, endpoint)
                val region = topicArn.split(":")[3]
                UserSession.saveSubscription(this@MainActivity, subArn, topicArn, region)
                loadTopics()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@MainActivity, "Subscribe failed", Toast.LENGTH_SHORT).show()
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
                loadTopics()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@MainActivity, "Unsubscribe failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // =======================================================================
    // Publish Message
    // =======================================================================
    private fun showSendMessageDialog(topicArn: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_send_message, null)
        val messageInput = dialogView.findViewById<EditText>(R.id.messageInput)

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Send") { _, _ ->
                val message = messageInput.text.toString()
                if (message.isNotEmpty()) {
                    publishMessage(topicArn, message)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun publishMessage(topicArn: String, message: String) {
        scope.launch {
            val app = application as App
            val sns = app.snsManager ?: return@launch
            val sdf = SimpleDateFormat("dd MMM yyyy | HH:mm:ss", Locale.getDefault())

            try {
                // Build SNS JSON payload
                val json = JSONObject().apply {
                    put("Message", message)
                    put("Subject", "Notification")
                    put("TopicArn", topicArn)
                    put("Timestamp", sdf.format(Date()))
                }.toString()

                // Wrap into SNS JSON envelope
                val envelope = JSONObject().apply {
                    put("default", json)
                }.toString()

                // Publish with structured message
                sns.publish(topicArn, envelope)

                Toast.makeText(this@MainActivity, "Message published", Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Failed to publish message", Toast.LENGTH_SHORT)
                    .show()
                Log.e("MainActivity", "Publish failed", e)
            }
        }
    }


    // =======================================================================
    // Dialogs: create topic + add custom topic ARN
    // =======================================================================
    private fun showCreateTopicDialog() {
        val currentRegion = UserSession.getCurrentRegion(this)

        if (currentRegion == null) {
            Toast.makeText(this, "Please select a region first", Toast.LENGTH_SHORT).show()
            return
        }

        val input = EditText(this).apply {
            hint = "Topic name"
            setPadding(50, 30, 50, 30)
        }

        AlertDialog.Builder(this)
            .setTitle("Create SNS Topic")
            .setMessage("Region: $currentRegion")
            .setView(input)
            .setPositiveButton("Create") { _, _ ->
                val name = input.text.toString().trim()

                if (name.isEmpty()) {
                    Toast.makeText(this, "Topic name cannot be empty", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // Validate topic name (AWS SNS rules)
                if (!name.matches(Regex("^[a-zA-Z0-9_-]{1,256}$"))) {
                    Toast.makeText(
                        this,
                        "Topic name must be 1-256 characters and contain only letters, numbers, hyphens, and underscores",
                        Toast.LENGTH_LONG
                    ).show()
                    return@setPositiveButton
                }

                // Show progress
                binding.progressBar.visibility = View.VISIBLE

                scope.launch {
                    val app = application as App
                    val sns = app.snsManager

                    if (sns == null) {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(
                            this@MainActivity,
                            "SNS Manager not initialized",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@launch
                    }

                    try {
                        val topicArn = sns.createTopic(name, currentRegion)

                        withContext(Dispatchers.Main) {
                            binding.progressBar.visibility = View.GONE
                            Toast.makeText(
                                this@MainActivity,
                                "Topic created successfully",
                                Toast.LENGTH_SHORT
                            ).show()
                            loadTopics()
                        }

                        Log.d("MainActivity", "Created topic: $topicArn")

                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            binding.progressBar.visibility = View.GONE

                            val errorMessage = when {
                                e.message?.contains("already exists") == true ->
                                    "Topic '$name' already exists in $currentRegion"
                                e.message?.contains("credentials") == true ->
                                    "Authentication error. Check your AWS credentials"
                                else -> "Error"
                            }

                            Toast.makeText(this@MainActivity, errorMessage, Toast.LENGTH_LONG)
                                .show()

                            Log.e("MainActivity", "Failed to create topic", e)
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showNotificationDialog(
        title: String?,
        message: String?,
        topicArn: String?,
        time: Long
    ) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_notification_detail)

        dialog.findViewById<TextView>(R.id.title).text = title
        dialog.findViewById<TextView>(R.id.message).text = message
        dialog.findViewById<TextView>(R.id.topic).text = topicArn ?: "Unknown Topic"

        val sdf = SimpleDateFormat("dd MMM yyyy | HH:mm:ss", Locale.getDefault())
        dialog.findViewById<TextView>(R.id.time).text = sdf.format(Date(time))

        dialog.show()
    }


    private fun showAddTopicArnDialog() {
        val input = EditText(this)
        input.hint = "Topic ARN"

        AlertDialog.Builder(this)
            .setTitle("Subscribe to Topic ARN")
            .setView(input)
            .setPositiveButton("Subscribe") { _, _ ->
                val arn = input.text.toString().trim()
                if (arn.isNotEmpty()) {
                    subscribe(arn)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // =======================================================================
    // UI helpers
    // =======================================================================
    private fun loadInitialState() {
        updateRegistrationStatus()
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

    // =======================================================================
    // FCM Token & Registration
    // =======================================================================
    private suspend fun waitForFcmToken(): String {
        val fromPrefs = UserSession.getFcmToken(this)
        if (fromPrefs != null) return fromPrefs
        return fcmTokenDeferred.await()
    }

    private fun updateRegistrationStatus() {
        scope.launch {
            val token = UserSession.getFcmToken(this@MainActivity) ?: "No token yet"
            val endpointArn =
                UserSession.getDeviceEndpointArn(this@MainActivity) ?: "Not registered"
            val currentRegion = UserSession.getCurrentRegion(this@MainActivity)
            val platformArn =
                UserSession.getPlatformArnForRegion(this@MainActivity, currentRegion ?: "")

            val statusText = """
            FCM Token: ${token.take(32)}...
            Endpoint ARN: $endpointArn
            Platform ARN ($currentRegion): $platformArn
            """.trimIndent()

            // This will fail if binding is not initialized yet. Guarding it.
            if (::binding.isInitialized) {
                binding.registrationStatus.text = statusText
            }
            Log.d("FCM_TOKEN", "Token: $token")
            Log.d("ENDPOINT_ARN", "Endpoint ARN: $endpointArn")
        }
    }
}