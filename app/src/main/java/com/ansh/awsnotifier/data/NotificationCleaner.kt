package com.ansh.awsnotifier.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object NotificationCleaner {

    private const val MILLIS_IN_DAY = 86_400_000L

    fun cleanOldEntries(context: Context) {
        val retentionDays = com.ansh.awsnotifier.session.UserSession.getRetentionDays(context)
        val cutoffTime = System.currentTimeMillis() - (retentionDays * MILLIS_IN_DAY)

        CoroutineScope(Dispatchers.IO).launch {
            val db = NotificationDatabase.getDatabase(context)
            db.notificationDao().deleteOlderThan(cutoffTime)
        }
    }
}
