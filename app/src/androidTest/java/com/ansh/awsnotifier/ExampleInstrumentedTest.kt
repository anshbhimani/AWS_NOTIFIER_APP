package com.ansh.awsnotifier

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ansh.awsnotifier.session.UserSession
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue

import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @After
    fun tearDown() {
        UserSession.clearAllData(context)
        context.getSharedPreferences("aws_prefs", android.content.Context.MODE_PRIVATE).edit().clear().apply()
    }

    @Test
    fun useAppContext() {
        assertEquals("com.ansh.awsnotifier", context.packageName)
    }

    @Test
    fun userSession_credentialsAndRegion_roundTripSuccessfully() {
        UserSession.clearAllData(context)

        UserSession.saveCredentials(context, "AKIA_TEST", "SECRET_TEST")
        UserSession.saveCurrentRegion(context, "us-east-1")

        assertEquals("AKIA_TEST" to "SECRET_TEST", UserSession.getCredentials(context))
        assertEquals("us-east-1", UserSession.getCurrentRegion(context))
    }

    @Test
    fun userSession_subscriptions_deduplicatesAndRemovesByTopic() {
        UserSession.clearAllData(context)

        UserSession.saveSubscription(context, "sub-1", "topic-1", "us-east-1")
        UserSession.saveSubscription(context, "sub-1", "topic-1", "us-east-1")
        UserSession.saveSubscription(context, "sub-2", "topic-2", "us-west-2")

        assertEquals(2, UserSession.getAllSubscriptions(context).size)

        UserSession.removeSubscriptionsByTopicArn(context, "topic-1")

        val remaining = UserSession.getAllSubscriptions(context)
        assertEquals(1, remaining.size)
        assertEquals("sub-2", remaining.first().subscriptionArn)
    }

    @Test
    fun userSession_tokenRefreshFlag_toggles() {
        UserSession.clearAllData(context)

        assertFalse(UserSession.isTokenRefreshPending(context))

        UserSession.setTokenRefreshPending(context, true)
        assertTrue(UserSession.isTokenRefreshPending(context))

        UserSession.clearAllData(context)
        assertNull(UserSession.getCredentials(context))
    }
}