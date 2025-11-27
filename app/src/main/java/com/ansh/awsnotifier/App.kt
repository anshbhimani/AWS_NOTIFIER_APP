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
import kotlinx.coroutines.*

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

        Log.d(TAG,"Now loading Credentials ...")
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
