package com.ansh.awsnotifier.aws

import android.content.Context
import android.util.Log
import com.ansh.awsnotifier.App
import com.ansh.awsnotifier.session.UserSession
import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider

fun setupAWSCredentials(context: Context) {
    try {
        val creds = UserSession.getCredentials(context)
        if (creds != null) {
            val provider = StaticCredentialsProvider {
                accessKeyId = creds.first
                secretAccessKey = creds.second
            }
            (context.applicationContext as? App)?.applyAwsCredentialsProvider(provider)
            Log.d("SetupAWSCredentials", "AWS credentials configured successfully")
        } else {
            Log.e("SetupAWSCredentials", "Failed to setup AWS credentials, credentials are null")
        }
    } catch (e: Exception) {
        Log.e("SetupAWSCredentials", "Failed to setup AWS credentials", e)
    }
}
