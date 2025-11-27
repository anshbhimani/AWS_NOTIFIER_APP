package com.ansh.awsnotifier.aws

import android.content.Context
import android.util.Log
import com.ansh.awsnotifier.App
import com.ansh.awsnotifier.session.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import aws.sdk.kotlin.services.sns.SnsClient
import aws.sdk.kotlin.services.sns.model.AuthorizationErrorException
import aws.sdk.kotlin.services.sns.model.ListTopicsRequest
import aws.sdk.kotlin.services.sts.StsClient
import aws.sdk.kotlin.services.sts.model.GetCallerIdentityRequest
import aws.sdk.kotlin.services.sts.model.StsException
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider

object CredentialValidator {

    private const val TAG = "CredentialValidator"

    /**
     * Validate credentials using STS + SNS.
     * Only save if validation passes.
     */
    suspend fun validateAndSave(
        context: Context,
        accessKey: String,
        secretKey: String,
        region: String
    ): Result<CallerIdentity> = withContext(Dispatchers.IO) {

        try {
            // Local format sanity check
            validateFormat(accessKey, secretKey)?.let { error ->
                return@withContext Result.failure(IllegalArgumentException(error))
            }

            val provider = StaticCredentialsProvider(
                Credentials(
                    accessKeyId = accessKey.trim(),
                    secretAccessKey = secretKey.trim()
                )
            )

            // ============= 1) STS TEST =============
            val identity = testSts(provider)

            // ============= 2) SNS TEST =============
            testSns(provider, region.trim())

            // ============= SAVE ONLY AFTER SUCCESS =============
            UserSession.saveCredentials(context, accessKey.trim(), secretKey.trim())
            UserSession.saveCurrentRegion(context, region.trim())

            (context.applicationContext as? App)
                ?.applyAwsCredentialsProvider(provider)

            Log.d(TAG, "Credentials validated for AWS Account ${identity.accountId}")

            Result.success(identity)

        } catch (e: StsException) {
            Log.e(TAG, "STS validation failed", e)
            Result.failure(
                SecurityException(
                    when {
                        e.message?.contains("InvalidClientTokenId") == true ->
                            "Invalid Access Key ID"
                        e.message?.contains("SignatureDoesNotMatch") == true ->
                            "Secret Key incorrect"
                        e.message?.contains("ExpiredToken") == true ->
                            "Your credentials are expired"
                        else -> "STS error: ${e.message}"
                    }
                )
            )

        } catch (e: AuthorizationErrorException) {
            Log.e(TAG, "SNS permission error", e)
            Result.failure(
                SecurityException(
                    "Valid credentials but SNS permissions missing. Attach SNS policy."
                )
            )

        } catch (e: Exception) {
            Log.e(TAG, "Validation failed", e)
            Result.failure(Exception("Unexpected error: ${e.message}"))
        }
    }

    /**
     * Pre-validation formatting rules
     */
    private fun validateFormat(accessKey: String, secretKey: String): String? {
        if (!accessKey.startsWith("AKIA") && !accessKey.startsWith("ASIA")) {
            return "Access Key must start with AKIA or ASIA"
        }

        if (accessKey.length !in 16..128)
            return "Access Key ID length is invalid"

        if (secretKey.length < 32)
            return "Secret Key is too short"

        return null
    }

    /**
     * Test STS identity lookup
     */
    private suspend fun testSts(provider: CredentialsProvider): CallerIdentity {
        StsClient {
            region = "us-east-1"  // AWSQuery default working region
            credentialsProvider = provider
        }.use { sts ->
            val resp = sts.getCallerIdentity(GetCallerIdentityRequest {})
            return CallerIdentity(
                accountId = resp.account ?: "",
                arn = resp.arn ?: "",
                userId = resp.userId ?: ""
            )
        }
    }

    /**
     * Test SNS access in chosen region
     */
    private suspend fun testSns(provider: CredentialsProvider, region: String) {
        SnsClient {
            this.region = region
            this.credentialsProvider = provider
        }.use { sns ->
            sns.listTopics(ListTopicsRequest {})
        }
    }
}

data class CallerIdentity(
    val accountId: String,
    val arn: String,
    val userId: String
)
