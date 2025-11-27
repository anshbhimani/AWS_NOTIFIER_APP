package com.ansh.awsnotifier.aws

import android.util.Log
import aws.sdk.kotlin.services.sns.SnsClient
import aws.sdk.kotlin.services.sns.model.*
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

class MultiRegionSnsManager(
    private val credentialsProvider: CredentialsProvider
) {

    private val supportedRegions = listOf(
        "us-east-1", "us-east-2",
        "us-west-1", "us-west-2",
        "ap-south-1", "ap-southeast-1",
        "ap-southeast-2", "ap-northeast-1",
        "eu-west-1", "eu-central-1",
        "sa-east-1"
    )

    private fun getClientForRegion(region: String): SnsClient =
        SnsClient {
            this.region = region
            this.credentialsProvider = this@MultiRegionSnsManager.credentialsProvider
        }

    // ------------------------------------------------------------
    //  ENDPOINT MANAGEMENT (wrapped in IO)
    // ------------------------------------------------------------

    suspend fun createPlatformEndpoint(
        platformApplicationArn: String,
        deviceToken: String
    ): String = withContext(Dispatchers.IO) {

        val region = platformApplicationArn.split(":")[3]
        getClientForRegion(region).use { sns ->
            val response = sns.createPlatformEndpoint(
                CreatePlatformEndpointRequest {
                    this.platformApplicationArn = platformApplicationArn
                    this.token = deviceToken
                    this.attributes = mapOf("Enabled" to "true")
                }
            )

            response.endpointArn ?: error("Failed to create platform endpoint")
        }
    }

    suspend fun updateEndpointToken(endpointArn: String, newToken: String) =
        withContext(Dispatchers.IO) {
            val region = endpointArn.split(":")[3]

            getClientForRegion(region).use { sns ->
                sns.setEndpointAttributes(
                    SetEndpointAttributesRequest {
                        this.endpointArn = endpointArn
                        this.attributes = mapOf(
                            "Token" to newToken,
                            "Enabled" to "true"
                        )
                    }
                )
            }
        }

    suspend fun deleteEndpoint(endpointArn: String) =
        withContext(Dispatchers.IO) {
            val region = endpointArn.split(":")[3]

            getClientForRegion(region).use { sns ->
                sns.deleteEndpoint(
                    DeleteEndpointRequest {
                        this.endpointArn = endpointArn
                    }
                )
            }
        }

    suspend fun registerDeviceEndpoint(
        platformApplicationArn: String,
        deviceToken: String
    ): String = createPlatformEndpoint(platformApplicationArn, deviceToken)

    // ------------------------------------------------------------
    //  LIST TOPICS (Across regions, safe in IO)
    // ------------------------------------------------------------

    suspend fun listAllTopics(): List<String> = withContext(Dispatchers.IO) {

        val result = mutableListOf<String>()

        // ⚡ Optional Parallel Region Scanning (faster)
        val tasks = supportedRegions.map { region ->
            async(Dispatchers.IO) {
                try {
                    getClientForRegion(region).use { sns ->
                        var next: String? = null

                        do {
                            val resp = sns.listTopics(
                                ListTopicsRequest { nextToken = next }
                            )

                            resp.topics?.forEach { t ->
                                t.topicArn?.let { result.add(it) }
                            }

                            next = resp.nextToken

                        } while (next != null)
                    }
                } catch (e: Exception) {
                    Log.e("SNSManager", "Failed to list topics in region $region", e)
                }
            }
        }

        tasks.forEach { it.await() }
        result
    }

    // ------------------------------------------------------------
    //  SUBSCRIPTIONS
    // ------------------------------------------------------------

    suspend fun listSubscriptions(): List<Subscription> = withContext(Dispatchers.IO) {

        val result = mutableListOf<Subscription>()

        val tasks = supportedRegions.map { region ->
            async(Dispatchers.IO) {
                try {
                    getClientForRegion(region).use { sns ->
                        var next: String? = null

                        do {
                            val resp = sns.listSubscriptions(
                                ListSubscriptionsRequest {
                                    this.nextToken = next
                                }
                            )

                            resp.subscriptions?.let { result.addAll(it) }
                            next = resp.nextToken

                        } while (next != null)
                    }
                } catch (e: Exception) {
                    Log.e("SNSManager", "Failed to list subscriptions in $region", e)
                }
            }
        }

        tasks.forEach { it.await() }
        result
    }

    suspend fun subscribe(topicArn: String, endpointArn: String): String =
        withContext(Dispatchers.IO) {
            val region = topicArn.split(":")[3]

            getClientForRegion(region).use { sns ->
                val resp = sns.subscribe(
                    SubscribeRequest {
                        this.topicArn = topicArn
                        this.protocol = "application"
                        this.endpoint = endpointArn
                    }
                )

                resp.subscriptionArn ?: error("Subscription ARN was null")
            }
        }

    suspend fun unsubscribe(subscriptionArn: String) =
        withContext(Dispatchers.IO) {

            if (!subscriptionArn.startsWith("arn:aws:sns:")) {
                Log.w("SNSManager", "Invalid ARN → skipping unsubscribe: $subscriptionArn")
                return@withContext
            }

            val region = subscriptionArn.split(":")[3]

            getClientForRegion(region).use { sns ->
                sns.unsubscribe(
                    UnsubscribeRequest {
                        this.subscriptionArn = subscriptionArn
                    }
                )
            }
        }

    // ------------------------------------------------------------
//  CREATE & DELETE TOPIC
// ------------------------------------------------------------

    suspend fun createTopic(name: String, region: String): String = withContext(Dispatchers.IO) {
        getClientForRegion(region).use { sns ->
            val response = sns.createTopic(
                CreateTopicRequest {
                    this.name = name
                }
            )

            response.topicArn ?: error("Failed to create topic")
        }
    }
    suspend fun deleteTopic(topicArn: String) = withContext(Dispatchers.IO) {
        val region = topicArn.split(":")[3]

        getClientForRegion(region).use { sns ->
            sns.deleteTopic(
                DeleteTopicRequest {
                    this.topicArn = topicArn
                }
            )
        }
    }

}
