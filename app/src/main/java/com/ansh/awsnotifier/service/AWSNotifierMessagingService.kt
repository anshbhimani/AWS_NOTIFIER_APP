package com.ansh.awsnotifier.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.ansh.awsnotifier.App
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
import org.json.JSONObject

class AWSNotifierMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "AWSNotifierFCM"
        private const val CHANNEL_ID = "aws_sns_notifications"
        private const val CHANNEL_NAME = "AWS Notifications"
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        FirebaseTokenProvider.onTokenRefreshed(token)
        UserSession.saveFcmToken(this, token)

        serviceScope.launch {
            try {
                DeviceRegistrar.autoRegister(applicationContext)
            } catch (e: Exception) {
                Log.e(TAG, "Background auto-registration failed", e)
            }
        }

        // Retry auto registration up to 5 times
        serviceScope.launch {
            repeat(5) {
                DeviceRegistrar.autoRegister(applicationContext)
                delay(1500)
                if (UserSession.getDeviceEndpointArn(applicationContext) != null)
                    return@launch
            }
        }

        if (UserSession.getCredentials(this) != null) {
            updateEndpointsWithNewToken(token)
        } else {
            UserSession.setTokenRefreshPending(this, true)
        }
    }

    private fun updateEndpointsWithNewToken(token: String) {
        serviceScope.launch {
            try {
                val app = applicationContext as App
                val credentials = UserSession.getCredentials(applicationContext)

                if (credentials == null || !app.hasCredentials()) {
                    UserSession.setTokenRefreshPending(applicationContext, true)
                    return@launch
                }

                val sns = app.snsManager ?: return@launch
                val endpointArn = UserSession.getDeviceEndpointArn(applicationContext)
                    ?: return@launch

                sns.updateEndpointToken(endpointArn, token)
                UserSession.setTokenRefreshPending(applicationContext, false)

            } catch (e: Exception) {
                Log.e(TAG, "Failed to update SNS token", e)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        Log.e("SNS_DEBUG", "FCM RAW PAYLOAD = ${message.data}")


        val data = message.data
        var topicArn: String? = null
        var messageText: String? = null
        var subject: String? = null
        var timestamp: Long = System.currentTimeMillis()

        // SNS standard payload
        if (data.containsKey("default")) {
            try {
                val json = JSONObject(data["default"]!!)
                topicArn = json.optString("TopicArn")
                messageText = json.optString("Message")
                subject = json.optString("Subject")
                timestamp = json.optLong("Timestamp", timestamp)
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing SNS JSON", e)
            }
        }

        // fallback extraction
        topicArn = topicArn
            ?: data["TopicArn"]
                    ?: data["topicArn"]
                    ?: data["topic_arn"]

        val topicName = topicArn?.substringAfterLast(":")

        val title = topicName ?: subject ?: "AWS Notification"

        val body = messageText
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

    private fun showNotification(title: String, body: String, topicArn: String?, timestamp: Long) {
        createNotificationChannel()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("topicArn", topicArn)
            putExtra("message", body)
            putExtra("title", title)
            putExtra("timestamp", timestamp)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val icon = getIconForTopic(title)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            )
            (getSystemService(NotificationManager::class.java))
                .createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}