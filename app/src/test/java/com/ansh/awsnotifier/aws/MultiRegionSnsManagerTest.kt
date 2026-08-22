package com.ansh.awsnotifier.aws

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test

class MultiRegionSnsManagerTest {

    private fun manager() = MultiRegionSnsManager(
        StaticCredentialsProvider(
            Credentials(
                accessKeyId = "test-access-key",
                secretAccessKey = "test-secret-key"
            )
        )
    )

    private fun getClientForRegion(manager: MultiRegionSnsManager, region: String): Any {
        val method = MultiRegionSnsManager::class.java.getDeclaredMethod("getClientForRegion", String::class.java)
        method.isAccessible = true
        return method.invoke(manager, region)!!
    }

    @Test
    fun getClientForRegion_reusesClientPerRegion() {
        val manager = manager()
        try {
            val first = getClientForRegion(manager, "us-east-1")
            val second = getClientForRegion(manager, "us-east-1")
            val differentRegion = getClientForRegion(manager, "us-west-2")

            assertSame(first, second)
            assertNotSame(first, differentRegion)
        } finally {
            manager.close()
        }
    }

    @Test
    fun close_clearsCachedClients() {
        val manager = manager()
        val first = getClientForRegion(manager, "us-east-1")

        manager.close()

        val second = getClientForRegion(manager, "us-east-1")
        assertNotSame(first, second)

        manager.close()
    }
}
