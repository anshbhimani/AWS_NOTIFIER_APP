package com.ansh.awsnotifier.aws

import android.content.Context
import android.content.Intent
import android.util.Log
import com.ansh.awsnotifier.App
import com.ansh.awsnotifier.session.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DeviceRegistrar {

    private const val TAG = "DeviceRegistrar"

    suspend fun autoRegister(context: Context) {
        val region = UserSession.getCurrentRegion(context)
        if (region != null && region != "All Regions") {
            registerForRegion(context, region)
        } else {
            Log.d(TAG, "Skipping auto-register for region: $region")
        }
    }

    suspend fun registerForRegion(context: Context, region: String) = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting registration for region: $region")
        val app = context.applicationContext as App

        val sns = app.snsManager
        if (sns == null) {
            Log.e(TAG, "SNS Manager is null. Cannot register.")
            return@withContext
        }

        val platformArn = UserSession.getPlatformArnForRegion(context, region)
        Log.d(TAG, "Retrieved Platform ARN for $region: $platformArn")

        if (platformArn.isNullOrEmpty()) {
            Log.w(TAG, "Platform ARN missing for region=$region. Aborting.")
            return@withContext
        }

        val token = UserSession.getFcmToken(context)
        if (token.isNullOrEmpty()) {
            Log.w(TAG, "FCM Token is null or empty. Aborting.")
            return@withContext
        }

        val oldEndpoint = UserSession.getDeviceEndpointArn(context)
        Log.d(TAG, "Current saved endpoint: $oldEndpoint")

        // Already correct?
        if (oldEndpoint != null && oldEndpoint.contains(":$region:")) {
            Log.d(TAG, "Already registered for region $region with endpoint: $oldEndpoint")
            return@withContext
        }

        // Delete old endpoint if exists (we only support one active region registration at a time)
        if (oldEndpoint != null) {
            try {
                sns.deleteEndpoint(oldEndpoint)
                Log.d(TAG, "Deleted old endpoint: $oldEndpoint")
            } catch (e: Exception) {
                // It's okay if it fails (e.g. already deleted on server)
                Log.w(TAG, "Failed to delete old endpoint (non-fatal): $oldEndpoint", e)
            }
        }

        try {
            Log.d(TAG, "Creating platform endpoint with ARN: $platformArn")
            // Create new endpoint
            val newEndpoint = sns.createPlatformEndpoint(platformArn, token)

            if (newEndpoint.isEmpty()) {
                Log.e(TAG, "AWS returned empty endpoint ARN")
                return@withContext
            }

            UserSession.saveDeviceEndpointArn(context, newEndpoint)
            Log.i(TAG, "✅ Registered successfully! New Endpoint: $newEndpoint")

            // Notify UI
            context.sendBroadcast(Intent("DEVICE_REGISTERED"))
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to create platform endpoint for $region", e)
        }
    }
}
