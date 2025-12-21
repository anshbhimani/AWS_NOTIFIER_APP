package com.ansh.awsnotifier.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.ansh.awsnotifier.R
import com.ansh.awsnotifier.aws.DeviceRegistrar
import com.ansh.awsnotifier.aws.FirebaseTokenProvider
import com.ansh.awsnotifier.session.UserSession
import com.ansh.awsnotifier.ui.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AWSNotifierMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "AWSNotifierFCM"
        private const val CHANNEL_ID = "aws_sns_notifications"
        private const val CHANNEL_NAME = "AWS Notifications"
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Called when Firebase issues a new FCM token
     *
     * IMPORTANT:
     * - Do NOT do network work directly here
     * - Just persist state and schedule work
     */
    override fun onNewToken(token: String) {
        Log.d(TAG, "FCM token refreshed")

        FirebaseTokenProvider.onTokenRefreshed(token)
        UserSession.saveFcmToken(this, token)

        // Mark that backend sync is required
        UserSession.setTokenRefreshPending(this, true)

        // Trigger background registration safely
        serviceScope.launch {
            retryAutoRegistration()
        }
    }

    /**
     * Retry device registration safely with backoff
     */
    private suspend fun retryAutoRegistration() {
        repeat(5) { attempt ->
            try {
                DeviceRegistrar.autoRegister(applicationContext)

                if (UserSession.getDeviceEndpointArn(applicationContext) != null) {
                    Log.d(TAG, "Device registration successful")
                    UserSession.setTokenRefreshPending(applicationContext, false)
                    return
                }
            } catch (e: Exception) {
                Log.w(
                    TAG,
                    "Auto registration attempt ${attempt + 1} failed",
                    e
                )
            }

            delay(2000L * (attempt + 1)) // exponential-ish backoff
        }

        Log.w(TAG, "Device registration failed after retries")
    }

    /**
     * Handle incoming SNS → FCM messages
     */
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        Log.d(TAG, "FCM RAW PAYLOAD = ${message.data}")

        val data = message.data
        var topicArn: String? = null
        var messageText: String? = null
        var subject: String? = null
        val timestamp = System.currentTimeMillis()

        // SNS standard payload
        if (data.containsKey("default")) {
            try {
                val json = org.json.JSONObject(data["default"]!!)
                topicArn = json.optString("TopicArn")
                messageText = json.optString("Message")
                subject = json.optString("Subject")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse SNS JSON", e)
            }
        }

        // Fallback fields
        topicArn = topicArn
            ?: data["TopicArn"]
                    ?: data["topicArn"]
                    ?: data["topic_arn"]

        val title =
            topicArn?.substringAfterLast(":")
                ?: subject
                ?: "AWS Notification"

        val body =
            messageText
                ?: data["message"]
                ?: "You have a new notification"

        showNotification(title, body, topicArn, timestamp)
    }

    private fun getIconForTopic(topic: String?): Int {
        if (topic == null) return R.drawable.ic_notification

        return when {
            topic.contains("alerts", true) -> R.drawable.ic_alert
            topic.contains("security", true) -> R.drawable.ic_security
            topic.contains("server", true) -> R.drawable.ic_server
            else -> R.drawable.ic_notification
        }
    }

    private fun showNotification(
        title: String,
        body: String,
        topicArn: String?,
        timestamp: Long
    ) {
        // 1. Save to Database
        serviceScope.launch {
            try {
                val app = applicationContext as com.ansh.awsnotifier.App
                val entity = com.ansh.awsnotifier.data.NotificationEntity(
                    title = title,
                    message = body,
                    topic = topicArn ?: "Unknown",
                    timestamp = timestamp
                )
                app.notificationDao.insert(entity)
                Log.d(TAG, "Notification saved to history: $title")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save notification history", e)
            }
        }

        // 2. Show System Notification
        createNotificationChannel()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("topicArn", topicArn)
            putExtra("message", body)
            putExtra("title", title)
            putExtra("timestamp", timestamp)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(getIconForTopic(title))
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .notify(timestamp.toInt(), notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}