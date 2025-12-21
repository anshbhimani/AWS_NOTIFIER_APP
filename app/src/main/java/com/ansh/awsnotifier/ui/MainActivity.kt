package com.ansh.awsnotifier.ui

import android.Manifest
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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

    private val ALL_REGIONS_DISPLAY_NAME = "All Regions"

    private var awsRegions = mutableListOf<String>()
    private var regions = mutableListOf<String>()

    private val showArnDialogForRegion = mutableStateOf<String?>(null)

    private lateinit var topicAdapter: TopicAdapter

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    // removed fcmTokenDeferred

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

        // Show loading state
        binding.progressBar.visibility = View.VISIBLE

        // Setup everything in the correct order
        scope.launch {
            try {
                // Step 1: Load regions first (this populates the regions list)
                loadAvailableRegions()

                // Step 2: Now setup UI (spinner will have data)
                setupUI()

                // Step 3: Register device and load topics
                waitForFcmToken()
                DeviceRegistrar.autoRegister(this@MainActivity)
                updateRegistrationStatus()

                // Step 4: Load topics for the selected region
                loadTopics()
            } catch (e: Exception) {
                Log.e("MainActivity", "Error during initialization", e)
                Toast.makeText(
                    this@MainActivity,
                    "Error loading app: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    // =======================================================================
    // FCM Token Button Setup
    // =======================================================================
    private fun setupFcmTokenButton() {
        binding.btnShowFcmToken.setOnClickListener {
            showFcmTokenDialog()
        }
    }

    private fun showFcmTokenDialog() {
        val currentRegion = UserSession.getCurrentRegion(this)
        val regionDisplayName =
            if (currentRegion == null || currentRegion == ALL_REGIONS_DISPLAY_NAME) {
                ALL_REGIONS_DISPLAY_NAME
            } else {
                currentRegion
            }

        val token = UserSession.getFcmToken(this) ?: "No token available"
        val endpointArn = UserSession.getDeviceEndpointArn(this) ?: "Not registered"
        val platformArn = if (currentRegion != null && currentRegion != ALL_REGIONS_DISPLAY_NAME) {
            UserSession.getPlatformArnForRegion(this, currentRegion) ?: "Not set for this region"
        } else {
            "Select a specific region"
        }

        val details = """
            Region: $regionDisplayName
            
            FCM Token:
            $token
            
            Platform ARN:
            $platformArn
            
            Endpoint ARN:
            $endpointArn
        """.trimIndent()

        val dialogView = LayoutInflater.from(this).inflate(
            android.R.layout.simple_list_item_1,
            null
        ).apply {
            findViewById<TextView>(android.R.id.text1).apply {
                text = details
                textSize = 12f
                setTextIsSelectable(true)
                setPadding(40, 40, 40, 40)
            }
        }

        AlertDialog.Builder(this)
            .setTitle("Registration Details")
            .setView(dialogView)
            .setPositiveButton("Copy Token") { _, _ ->
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("FCM Token", token)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "FCM Token copied to clipboard", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("Copy Platform ARN") { _, _ ->
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Platform ARN", platformArn)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "Platform ARN copied", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Copy Endpoint ARN") { _, _ ->
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Endpoint ARN", endpointArn)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "Endpoint ARN copied", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun deleteTopic(topicArn: String) {
        scope.launch {
            val app = application as App
            val sns = app.snsManager ?: return@launch

            binding.progressBar.visibility = View.VISIBLE

            try {
                // Step 1: Unsubscribe all local subscriptions from this topic first.
                val subscriptions = UserSession.getAllSubscriptions(this@MainActivity)
                    .filter { it.topicArn == topicArn }

                subscriptions.forEach { sub ->
                    try {
                        // Unsubscribe from AWS
                        sns.unsubscribe(sub.subscriptionArn)
                        Log.d(
                            "MainActivity",
                            "Unsubscribed from ${sub.subscriptionArn} before topic deletion"
                        )
                    } catch (e: Exception) {
                        Log.e(
                            "MainActivity",
                            "Failed to unsubscribe ${sub.subscriptionArn} during topic deletion process. Continuing...",
                            e
                        )
                        // Log but continue to delete the topic
                    }
                }

                // Step 2: Delete the topic from AWS
                sns.deleteTopic(topicArn)
                Log.d("MainActivity", "Deleted topic: $topicArn")

                // Step 3: Clean up all local records (both topic and subscriptions)
                UserSession.removeSubscriptionsByTopicArn(this@MainActivity, topicArn)
                loadTopics()

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    this@MainActivity,
                    "Failed to delete topic: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                binding.progressBar.visibility = View.GONE
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
                            loadTopics() // Ensure topics are loaded after ARN setup
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
                // SAFETY CHECK: Ensure regions list is populated
                if (regions.isEmpty()) {
                    Log.w("MainActivity", "Regions list is empty, using fallback")
                    useFallbackRegions()
                }

                // SAFETY CHECK: Validate spinner position
                val spinnerPosition = binding.regionSpinner.selectedItemPosition
                if (spinnerPosition < 0 || spinnerPosition >= regions.size) {
                    Log.w(
                        "MainActivity",
                        "Invalid spinner position: $spinnerPosition, defaulting to 0"
                    )
                    binding.regionSpinner.setSelection(0)
                    binding.progressBar.visibility = View.GONE
                    return@launch
                }

                val selectedRegion = regions[spinnerPosition]

                val regionsToQuery = if (selectedRegion == ALL_REGIONS_DISPLAY_NAME) {
                    awsRegions // Query all AWS regions
                } else {
                    listOf(selectedRegion) // Query only the selected region
                }

                val topicArns = sns.listAllTopics(regionsToQuery)
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
                Toast.makeText(
                    this@MainActivity,
                    "Failed to load topics: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private suspend fun loadAvailableRegions() = withContext(Dispatchers.IO) {
        try {
            val app = application as App
            if (app.snsManager == null && app.hasCredentials()) {
                app.initSnsManager()
            }

            val sns = app.snsManager
            if (sns != null) {
                val fetchedRegions = sns.fetchAvailableRegions()

                if (fetchedRegions.isNotEmpty()) {
                    awsRegions.clear()
                    awsRegions.addAll(fetchedRegions)

                    regions.clear()
                    regions.add(ALL_REGIONS_DISPLAY_NAME)
                    regions.addAll(awsRegions)

                    Log.d("MainActivity", "Loaded ${awsRegions.size} regions from AWS")
                } else {
                    useFallbackRegions()
                }
            } else {
                useFallbackRegions()
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to load regions", e)
            useFallbackRegions()
        }
    }

    private fun useFallbackRegions() {
        awsRegions.clear()
        awsRegions.addAll(
            listOf(
                "us-east-1", "us-east-2",
                "us-west-1", "us-west-2",
                "ap-south-1", "ap-southeast-1",
                "ap-southeast-2", "ap-northeast-1",
                "eu-west-1", "eu-central-1",
                "sa-east-1"
            )
        )

        regions.clear()
        regions.add(ALL_REGIONS_DISPLAY_NAME)
        regions.addAll(awsRegions)

        Log.d("MainActivity", "Using fallback regions")
    }

    private fun setupUI() {
        if (regions.isEmpty()) {
            Log.e("MainActivity", "Cannot setup UI - regions list is empty")
            useFallbackRegions()
        }
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
        setupFcmTokenButton()

        scope.launch {
            waitForFcmToken()
            DeviceRegistrar.autoRegister(this@MainActivity)
            updateRegistrationStatus()
        }
        loadTopics()
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

        // Set initial selection
        val saved = UserSession.getCurrentRegion(this)
        val idx = if (saved == null) {
            0 // Default to "All Regions"
        } else {
            val index = regions.indexOf(saved)
            if (index >= 0) index else 0
        }
        binding.regionSpinner.setSelection(idx)

        Log.d(
            "MainActivity",
            "Spinner initialized with ${regions.size} regions, selected index: $idx"
        )

        binding.regionSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    if (regions.isEmpty() || position < 0 || position >= regions.size) {
                        Log.e(
                            "MainActivity",
                            "Invalid spinner selection: position=$position, regions.size=${regions.size}"
                        )
                        return
                    }

                    val region = regions[position]
                    Log.d("MainActivity", "Region selected: $region at position $position")

                    if (region == ALL_REGIONS_DISPLAY_NAME) {
                        UserSession.saveCurrentRegion(this@MainActivity, region)
                        loadTopics()
                        return
                    }

                    UserSession.saveCurrentRegion(this@MainActivity, region)

                    val arn = UserSession.getPlatformArnForRegion(this@MainActivity, region)
                    if (arn.isNullOrEmpty()) {
                        showArnDialogForRegion.value = region
                        return
                    }

                    scope.launch {
                        binding.progressBar.visibility = View.VISIBLE
                        try {
                            waitForFcmToken()
                            DeviceRegistrar.autoRegister(this@MainActivity)
                            updateRegistrationStatus()
                            loadTopics()
                        } finally {
                            binding.progressBar.visibility = View.GONE
                        }
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    Log.d("MainActivity", "No region selected")
                }
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

            // 1. Determine region from topic
            val region = try {
                topicArn.split(":")[3]
            } catch (e: Exception) {
                Log.e("MainActivity", "Invalid topic ARN: $topicArn")
                Toast.makeText(this@MainActivity, "Invalid topic ARN format", Toast.LENGTH_SHORT)
                    .show()
                return@launch
            }

            // 2. Check if we need to switch registration
            var endpoint = UserSession.getDeviceEndpointArn(this@MainActivity)
            val isRegisteredForRegion = endpoint != null && endpoint.contains(":$region:")

            if (!isRegisteredForRegion) {
                // We need to register for this region
                val platformArn = UserSession.getPlatformArnForRegion(this@MainActivity, region)

                if (platformArn.isNullOrEmpty()) {
                    // Prompt user for Platform ARN
                    showArnDialogForRegion.value = region
                    Toast.makeText(
                        this@MainActivity,
                        "Please configure Platform ARN for $region to subscribe",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }

                binding.progressBar.visibility = View.VISIBLE
                try {
                    // Attempt registration
                    val token = waitForFcmToken()
                    if (token.isEmpty()) {
                        Toast.makeText(
                            this@MainActivity,
                            "FCM Token not available",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@launch
                    }

                    DeviceRegistrar.registerForRegion(this@MainActivity, region)
                    endpoint = UserSession.getDeviceEndpointArn(this@MainActivity)

                    if (endpoint == null) {
                        Toast.makeText(this@MainActivity, "Registration failed", Toast.LENGTH_SHORT)
                            .show()
                        return@launch
                    }

                    // Update the UI registration status since we just changed it
                    updateRegistrationStatus()

                } catch (e: Exception) {
                    Log.e("MainActivity", "Auto-registration failed", e)
                    Toast.makeText(
                        this@MainActivity,
                        "Registration error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                } finally {
                    binding.progressBar.visibility = View.GONE
                }
            }

            // 3. Now subscribe
            // endpoint is guaranteed to be non-null here
            val validEndpoint = endpoint!!

            try {
                val subArn = sns.subscribe(topicArn, validEndpoint)
                UserSession.saveSubscription(this@MainActivity, subArn, topicArn, region)
                loadTopics()
                Toast.makeText(this@MainActivity, "Subscribed!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
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
                // Create FCM-specific notification structure
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

                // Create the SNS message structure with protocol-specific formats
                val snsMessage = JSONObject().apply {
                    put("default", message)  // Fallback for other protocols
                    put("GCM", fcmPayload.toString())  // FCM-specific payload
                }

                // Publish with MessageStructure="json"
                sns.publish(topicArn, snsMessage.toString(), messageStructure = "json")

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

        if (currentRegion == null || currentRegion == ALL_REGIONS_DISPLAY_NAME) {
            Toast.makeText(
                this,
                "Please select a specific region before creating a topic.",
                Toast.LENGTH_LONG
            ).show()
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
                                else -> "Error: ${e.message}"
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
        val input = EditText(this).apply {
            hint = "Topic ARN"
            setPadding(50, 30, 50, 30)
        }

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
        return try {
            val token = com.ansh.awsnotifier.aws.FirebaseTokenProvider.getToken()
            UserSession.saveFcmToken(this, token)
            token
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to get FCM token", e)
            ""
        }
    }

    private fun updateRegistrationStatus() {
        scope.launch {
            val token = UserSession.getFcmToken(this@MainActivity)
            val endpointArn = UserSession.getDeviceEndpointArn(this@MainActivity)

            Log.d("FCM_TOKEN", "Token: ${token ?: "No token yet"}")
            Log.d("ENDPOINT_ARN", "Endpoint ARN: ${endpointArn ?: "Not registered"}")
        }
    }
}