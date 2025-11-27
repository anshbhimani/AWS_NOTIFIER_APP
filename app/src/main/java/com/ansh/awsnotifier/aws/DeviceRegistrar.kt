package com.ansh.awsnotifier.aws

import android.content.Context
import android.content.Intent
import android.util.Log
import com.ansh.awsnotifier.App
import com.ansh.awsnotifier.session.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DeviceRegistrar {

    suspend fun autoRegister(context: Context) = withContext(Dispatchers.IO) {
        val app = context.applicationContext as App

        val sns = app.snsManager ?: return@withContext
        val region = UserSession.getCurrentRegion(context) ?: return@withContext
        val platformArn = UserSession.getPlatformArnForRegion(context, region)

        if (platformArn.isNullOrEmpty()) {
            Log.w("DeviceRegistrar", "Platform ARN missing for region=$region")
            return@withContext
        }

        val token = UserSession.getFcmToken(context) ?: return@withContext
        val oldEndpoint = UserSession.getDeviceEndpointArn(context)

        // Already correct
        if (oldEndpoint != null && oldEndpoint.contains(":$region:")) return@withContext

        // Delete old endpoint if region changed
        if (oldEndpoint != null) {
            try { sns.deleteEndpoint(oldEndpoint) }
            catch (e: Exception) {
                Log.w("DeviceRegistrar", "Failed to delete old endpoint: $oldEndpoint", e)
            }
        }

        // Create new endpoint
        val newEndpoint = sns.createPlatformEndpoint(platformArn, token)
        UserSession.saveDeviceEndpointArn(context, newEndpoint)

        // Notify UI
        context.sendBroadcast(Intent("DEVICE_REGISTERED"))
    }
}
