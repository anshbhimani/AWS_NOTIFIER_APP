package com.ansh.awsnotifier

import android.app.Application
import android.util.Log
import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import com.ansh.awsnotifier.aws.MultiRegionSnsManager
import com.ansh.awsnotifier.data.NotificationDao
import com.ansh.awsnotifier.data.NotificationDatabase
import com.ansh.awsnotifier.session.UserSession
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class App : Application() {

    companion object {
        const val TAG = "AWSNotifierApp"
    }

    // Room DAO
    val notificationDao: NotificationDao
        get() = NotificationDatabase.getDatabase(this).notificationDao()

    // AWS credentials
    var awsCredentialsProvider: CredentialsProvider? = null
        private set

    // SNS Manager
    var snsManager: MultiRegionSnsManager? = null
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun initSnsManager() {
        if (awsCredentialsProvider != null) {
            snsManager = MultiRegionSnsManager(awsCredentialsProvider!!)
            Log.d(TAG, "SNS Manager initialized (lazy)")
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "App starting")

        // Initialize Firebase first with detailed logging
        initializeFirebase()

        Log.d(TAG, "Now loading Credentials ...")

        // Load any saved credentials
        loadCredentialsIfAvailable()

        if (hasCredentials()) {
            snsManager = MultiRegionSnsManager(awsCredentialsProvider!!)
            Log.d(TAG, "SNS Manager initialized globally")
        }

        // Auto-register if token exists
        val token = UserSession.getFcmToken(this)
        if (!token.isNullOrEmpty()) {
            appScope.launch {
                try {
                    com.ansh.awsnotifier.aws.DeviceRegistrar.autoRegister(applicationContext)
                } catch (e: Exception) {
                    Log.e(TAG, "Auto-registration failed", e)
                }
            }
        }

        // Clean old notifications
        com.ansh.awsnotifier.data.NotificationCleaner.cleanOldEntries(this)
    }

    /**
     * Initialize Firebase with detailed error logging
     */
    private fun initializeFirebase() {
        try {
            Log.d(TAG, "Initializing Firebase...")

            // Initialize Firebase
            val firebaseApp = FirebaseApp.initializeApp(this)

            if (firebaseApp != null) {
                Log.d(TAG, "✓ Firebase initialized successfully")
                Log.d(TAG, "  App Name: ${firebaseApp.name}")
                Log.d(TAG, "  Project ID: ${firebaseApp.options.projectId}")
                Log.d(TAG, "  Application ID: ${firebaseApp.options.applicationId}")
                Log.d(TAG, "  API Key: ${firebaseApp.options.apiKey.take(20)}...")

                // Enable FCM auto-initialization
                FirebaseMessaging.getInstance().isAutoInitEnabled = true
                Log.d(TAG, "✓ FCM auto-init enabled")

                // Attempt to retrieve token immediately
                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val token = task.result
                        Log.d(TAG, "✓✓✓ FCM Token retrieved successfully!")
                        Log.d(TAG, "Token (first 50 chars): ${token.take(50)}...")
                        Log.d(TAG, "Token length: ${token.length}")

                        // Save it immediately
                        UserSession.saveFcmToken(this, token)
                        Log.d(TAG, "Token saved to UserSession")

                    } else {
                        Log.e(TAG, "✗✗✗ Failed to retrieve FCM token", task.exception)

                        task.exception?.let { e ->
                            Log.e(TAG, "════════════════════════════════════")
                            Log.e(TAG, "FCM TOKEN ERROR DETAILS:")
                            Log.e(TAG, "════════════════════════════════════")
                            Log.e(TAG, "Exception Type: ${e.javaClass.simpleName}")
                            Log.e(TAG, "Exception Message: ${e.message}")
                            Log.e(TAG, "Exception Cause: ${e.cause?.message ?: "None"}")

                            // Print full stack trace
                            e.printStackTrace()

                            // Specific error guidance
                            when {
                                e.message?.contains("FIS_AUTH_ERROR") == true -> {
                                    Log.e(TAG, "")
                                    Log.e(TAG, "⚠️ FIS_AUTH_ERROR DETECTED")
                                    Log.e(TAG, "════════════════════════════════════")
                                    Log.e(TAG, "Possible causes:")
                                    Log.e(
                                        TAG,
                                        "1. Firebase APIs not enabled in Google Cloud Console"
                                    )
                                    Log.e(TAG, "   - Firebase Cloud Messaging API")
                                    Log.e(TAG, "   - Firebase Installations API")
                                    Log.e(TAG, "2. google-services.json doesn't match package name")
                                    Log.e(TAG, "3. Firebase project has billing/quota issues")
                                    Log.e(TAG, "4. App needs to be re-added to Firebase project")
                                    Log.e(TAG, "════════════════════════════════════")
                                }

                                e.message?.contains("SERVICE_NOT_AVAILABLE") == true -> {
                                    Log.e(TAG, "")
                                    Log.e(TAG, "⚠️ SERVICE_NOT_AVAILABLE")
                                    Log.e(TAG, "Google Play Services is not available or outdated")
                                }

                                e.message?.contains("INTERNAL") == true -> {
                                    Log.e(TAG, "")
                                    Log.e(TAG, "⚠️ INTERNAL ERROR")
                                    Log.e(TAG, "This may be a temporary Firebase service issue")
                                }

                                e.message?.contains("TIMEOUT") == true -> {
                                    Log.e(TAG, "")
                                    Log.e(TAG, "⚠️ TIMEOUT ERROR")
                                    Log.e(TAG, "Network connectivity issue")
                                }
                            }
                            Log.e(TAG, "════════════════════════════════════")
                        }
                    }
                }

            } else {
                Log.e(TAG, "✗ FirebaseApp is null after initialization")
            }

        } catch (e: Exception) {
            Log.e(TAG, "✗ Failed to initialize Firebase", e)
            e.printStackTrace()
        }
    }

    /**
     * Called after onboarding / credential entry.
     */
    fun applyAwsCredentialsProvider(provider: CredentialsProvider) {
        awsCredentialsProvider = provider
        snsManager = MultiRegionSnsManager(provider)
        Log.d(TAG, "Credentials applied → SNS Manager initialized")
    }

    /**
     * Load creds on cold start.
     */
    fun loadCredentialsIfAvailable() {
        val creds = UserSession.getCredentials(this)
        if (creds == null) {
            Log.w(TAG, "No stored AWS credentials found")
            return
        }

        val (accessKey, secretKey) = creds
        val provider = StaticCredentialsProvider(
            Credentials(
                accessKeyId = accessKey,
                secretAccessKey = secretKey
            )
        )

        applyAwsCredentialsProvider(provider)
        Log.d(TAG, "AWS credentials loaded from storage")
    }

    fun hasCredentials(): Boolean = awsCredentialsProvider != null

    fun clearCredentials() {
        awsCredentialsProvider = null
        snsManager = null
    }

    override fun onTerminate() {
        super.onTerminate()
        appScope.cancel()
    }
}