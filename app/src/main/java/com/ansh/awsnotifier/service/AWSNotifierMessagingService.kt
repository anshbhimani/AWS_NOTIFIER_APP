package com.ansh.awsnotifier.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
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
import kotlinx.coroutines.*

class AWSNotifierMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "AWSNotifierFCM"
        private const val CHANNEL_ID = "aws_sns_notifications"
        private const val CHANNEL_NAME = "AWS SNS Notifications"
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token received")

        FirebaseTokenProvider.onTokenRefreshed(token)
        UserSession.saveFcmToken(this, token)

        serviceScope.launch {
            try {
                DeviceRegistrar.autoRegister(applicationContext)
            } catch (e: Exception) {
                Log.e(TAG, "Background auto-registration failed", e)
            }
        }

        serviceScope.launch {
            repeat(5) { attempt ->
                DeviceRegistrar.autoRegister(applicationContext)
                delay(1500)
                if (UserSession.getDeviceEndpointArn(applicationContext) != null) return@launch
            }
        }



        // If user is logged in, update all registered endpoints
        if (UserSession.getCredentials(this) != null) {
            updateEndpointsWithNewToken(token)
        } else {
            // Mark for later processing when credentials are available
            UserSession.setTokenRefreshPending(this, true)
        }

    }

    private fun updateEndpointsWithNewToken(token: String) {
        serviceScope.launch {
            try {
                val app = applicationContext as App

                if (UserSession.getCredentials(applicationContext) == null) {
                    Log.w(TAG, "No AWS credentials stored")
                    UserSession.setTokenRefreshPending(applicationContext, true)
                    return@launch
                }

                // check credentials first
                if (!app.hasCredentials()) {
                    Log.w(TAG, "No credentials available to update endpoint")
                    UserSession.setTokenRefreshPending(applicationContext, true)
                    return@launch
                }

                val sns = app.snsManager ?: run {
                    Log.w(TAG, "SNS Manager not initialized, cannot update token")
                    UserSession.setTokenRefreshPending(applicationContext, true)
                    return@launch
                }

                val endpointArn = UserSession.getDeviceEndpointArn(applicationContext)
                if (endpointArn == null) {
                    Log.w(TAG, "No endpoint ARN saved. Cannot refresh device token.")
                    UserSession.setTokenRefreshPending(applicationContext, true)
                    return@launch
                }

                // Your SNS manager's real function only takes 2 args
                sns.updateEndpointToken(endpointArn, token)

                Log.d(TAG, "Successfully updated device endpoint token")

                UserSession.setTokenRefreshPending(applicationContext, false)

            } catch (e: Exception) {
                Log.e(TAG, "Failed to update endpoint token", e)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "Message received from: ${message.from}")

        // Extract SNS message data
        val title = message.data["title"]
            ?: message.notification?.title
            ?: "AWS SNS Notification"

        val body = message.data["message"]
            ?: message.data["default"]
            ?: message.notification?.body
            ?: "You have a new notification"

        val topicArn = message.data["topicArn"] ?: message.from

        showNotification(title, body, topicArn)
    }

    private fun showNotification(title: String, body: String, topicArn: String?) {
        createNotificationChannel()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            topicArn?.let { putExtra("topicArn", it) }
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications from AWS SNS topics"
                enableVibration(true)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}