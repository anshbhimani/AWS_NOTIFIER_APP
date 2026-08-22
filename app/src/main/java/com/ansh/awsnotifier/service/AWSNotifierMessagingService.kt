package com.ansh.awsnotifier.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.ansh.awsnotifier.R
import com.ansh.awsnotifier.BuildConfig
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

internal object StructuredAlertParser {
    fun parse(raw: String): Pair<String?, String?> {
        return try {
            val json = org.json.JSONObject(raw)

            val alarmName = json.optString("AlarmName", "").takeIf { it.isNotEmpty() }
            val alarmReason = json.optString("NewStateReason", "").takeIf { it.isNotEmpty() }
            if (alarmName != null || alarmReason != null) {
                return alarmName to (alarmReason ?: raw)
            }

            val budgetName = json.optString("budgetName", json.optString("BudgetName", ""))
                .takeIf { it.isNotEmpty() }
            if (budgetName != null) {
                val actual = json.optString("actualAmount", json.optString("ActualAmount", ""))
                val limit = json.optString("budgetLimit", json.optString("BudgetLimit", ""))
                val unit = json.optString("unit", json.optString("Unit", ""))
                val body = if (actual.isNotEmpty() && limit.isNotEmpty()) {
                    "Spend of $actual $unit has crossed your budget limit of $limit $unit".trim()
                } else {
                    raw
                }
                return budgetName to body
            }

            val summary = json.keys().asSequence()
                .take(4)
                .joinToString("\n") { key -> "$key: ${json.optString(key)}" }
            null to summary.takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            null to raw.takeIf { it.isNotEmpty() }
        }
    }
}

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

    if (BuildConfig.DEBUG) {
        Log.d(TAG, "FCM payload keys = ${message.data.keys}")
    }

    val data = message.data
    var topicArn: String? = null
    var messageText: String? = null
    var subject: String? = null
    val timestamp = System.currentTimeMillis()

    // SNS standard payload. A raw (non-JSON-structured) SNS Publish - which is how CloudWatch
    // alarms and AWS Budgets actually notify a topic - delivers its message to a mobile endpoint
    // as the literal string value of "default", not wrapped in a TopicArn/Subject/Message envelope.
    if (data.containsKey("default")) {
        val raw = data["default"]!!
        val envelope = try {
            org.json.JSONObject(raw).takeIf { it.has("Message") }
        } catch (e: Exception) {
            null
        }
        if (envelope != null) {
            // Envelope shape (TopicArn/Subject/Message), e.g. from a relay that re-wraps the
            // notification before publishing.
            topicArn = envelope.optString("TopicArn").takeIf { it.isNotEmpty() }
            subject = envelope.optString("Subject").takeIf { it.isNotEmpty() }
            val (parsedTitle, parsedBody) = parseStructuredAlert(envelope.optString("Message"))
            subject = subject ?: parsedTitle
            messageText = parsedBody
        } else {
            // Delivered directly - the real-world case for CloudWatch alarms and AWS Budgets.
            val (parsedTitle, parsedBody) = parseStructuredAlert(raw)
            subject = parsedTitle
            messageText = parsedBody
        }
    }

    // Fallback fields
    topicArn = topicArn
        ?: data["TopicArn"]
        ?: data["topicArn"]
        ?: data["topic_arn"]

    val title = subject?.takeIf { it.isNotEmpty() }
        ?: topicArn?.substringAfterLast(":")
        ?: "AWS Notification"

    val body = messageText?.takeIf { it.isNotEmpty() }
        ?: data["message"]
        ?: "You have a new notification"

    showNotification(title, body, topicArn, timestamp)
}

    /**
     * Extracts a (title, body) pair from a structured alert JSON. Recognizes CloudWatch alarm
     * fields and common AWS Budgets field names; falls back to a readable key/value summary for
     * any other JSON shape, and to the raw text itself if it isn't JSON at all.
     */
    private fun parseStructuredAlert(raw: String): Pair<String?, String?> {
        return StructuredAlertParser.parse(raw)
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
                    topic = topicArn ?: title,
                    timestamp = timestamp
                )
                app.notificationRepository.insert(entity)
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
            .setSmallIcon(getIconForTopic(topicArn))
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
