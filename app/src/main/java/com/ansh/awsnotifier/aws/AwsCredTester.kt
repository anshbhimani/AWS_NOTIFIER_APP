package com.ansh.awsnotifier.aws

import android.util.Log
import aws.sdk.kotlin.services.sns.SnsClient
import aws.sdk.kotlin.services.sns.model.ListTopicsRequest
import aws.sdk.kotlin.services.sts.StsClient
import aws.sdk.kotlin.services.sts.model.GetCallerIdentityRequest
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AwsCredTester {

    private const val TAG = "AwsCredTester"

    /**
     * Runs STS GetCallerIdentity and SNS ListTopics in the given region.
     * Returns a human-readable log string.
     */
    suspend fun runFullTest(
        accessKey: String,
        secretKey: String,
        region: String
    ): String = withContext(Dispatchers.IO) {

        val provider = StaticCredentialsProvider(
            Credentials(
                accessKeyId = accessKey,
                secretAccessKey = secretKey
            )
        )

        val sb = StringBuilder()

        sb.appendLine("=== AWS TEST START ===")

        // 1) STS
        try {
            sb.appendLine("1️⃣ Testing STS GetCallerIdentity…")

            StsClient {
                this.region = "us-east-1"
                this.credentialsProvider = provider
            }.use { sts ->
                val id = sts.getCallerIdentity(GetCallerIdentityRequest {})
                sb.appendLine("✔ STS OK")
                sb.appendLine("Account: ${id.account}")
                sb.appendLine("ARN: ${id.arn}")
                sb.appendLine("UserID: ${id.userId}")
            }
        } catch (e: Exception) {
            sb.appendLine("❌ STS FAILED: ${e.message}")
            Log.e(TAG, "STS error", e)
            return@withContext sb.toString()
        }

        // 2) SNS
        try {
            sb.appendLine("2️⃣ Testing SNS ListTopics…")

            SnsClient {
                this.region = region
                this.credentialsProvider = provider
            }.use { sns ->
                sns.listTopics(ListTopicsRequest {})
            }

            sb.appendLine("✔ SNS OK")
        } catch (e: Exception) {
            sb.appendLine("❌ SNS FAILED: ${e.message}")
            Log.e(TAG, "SNS error", e)
            return@withContext sb.toString()
        }

        sb.appendLine("=== ALL TESTS PASSED ===")
        sb.toString()
    }

}
