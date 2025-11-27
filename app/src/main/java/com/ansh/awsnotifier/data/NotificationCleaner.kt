package com.ansh.awsnotifier.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object NotificationCleaner {

    private const val RETENTION_DAYS = 30
    private const val MILLIS_IN_DAY = 86_400_000L

    fun cleanOldEntries(context: Context) {
        val cutoffTime = System.currentTimeMillis() - (RETENTION_DAYS * MILLIS_IN_DAY)

        CoroutineScope(Dispatchers.IO).launch {
            val db = NotificationDatabase.getDatabase(context)
            db.notificationDao().deleteOlderThan(cutoffTime)
        }
    }
}
