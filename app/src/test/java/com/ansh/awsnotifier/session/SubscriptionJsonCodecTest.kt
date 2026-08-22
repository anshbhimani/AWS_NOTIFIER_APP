package com.ansh.awsnotifier.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SubscriptionJsonCodecTest {

    @Test
    fun fromJson_returnsEmpty_whenInputIsNullOrBlank() {
        assertTrue(SubscriptionJsonCodec.fromJson(null).isEmpty())
        assertTrue(SubscriptionJsonCodec.fromJson("").isEmpty())
        assertTrue(SubscriptionJsonCodec.fromJson("   ").isEmpty())
    }

    @Test
    fun toJson_roundTripsStoredSubscriptions() {
        val expected = mutableListOf(
            StoredSubscription("sub-1", "topic-1", "us-east-1"),
            StoredSubscription("sub-2", "topic-2", "eu-west-1")
        )

        val json = SubscriptionJsonCodec.toJson(expected)
        val actual = SubscriptionJsonCodec.fromJson(json)

        assertEquals(expected, actual)
    }
}
