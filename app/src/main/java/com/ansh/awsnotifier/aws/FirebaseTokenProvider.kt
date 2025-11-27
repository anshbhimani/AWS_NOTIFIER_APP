package com.ansh.awsnotifier.aws

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object FirebaseTokenProvider {
    private const val TAG = "FirebaseTokenProvider"

    @Volatile
    private var cachedToken: String? = null

    /**
     * Gets the current FCM token, fetching fresh if needed
     */
    suspend fun getToken(): String = suspendCancellableCoroutine { cont ->
        // Return cached token if available (can be invalidated by onNewToken)
        cachedToken?.let {
            cont.resume(it)
            return@suspendCancellableCoroutine
        }

        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                Log.d(TAG, "FCM token retrieved")
                cachedToken = token
                cont.resume(token)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to get FCM token", e)
                cont.resumeWithException(e)
            }
    }

    /**
     * Called when FCM token is refreshed
     */
    fun onTokenRefreshed(newToken: String) {
        Log.d(TAG, "FCM token refreshed")
        cachedToken = newToken
    }

    /**
     * Invalidates cached token (call when you need a fresh token)
     */
    fun invalidateCache() {
        cachedToken = null
    }
}