package com.ansh.awsnotifier.aws

import android.util.Log
import aws.sdk.kotlin.services.sts.StsClient
import aws.sdk.kotlin.services.sts.model.GetCallerIdentityRequest
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AwsIdentityManager(
    private val credentialsProvider: CredentialsProvider
) {
    suspend fun getAccountDetails(): String? = withContext(Dispatchers.IO) {
        try {
            StsClient {
                region = "us-east-1" // Region doesn't matter much for STS global endpoint usually
                credentialsProvider = this@AwsIdentityManager.credentialsProvider
            }.use { sts ->
                val response = sts.getCallerIdentity(GetCallerIdentityRequest {})
                response.account
            }
        } catch (e: Exception) {
            Log.e("AwsIdentityManager", "Failed to fetch identity", e)
            null
        }
    }
}
