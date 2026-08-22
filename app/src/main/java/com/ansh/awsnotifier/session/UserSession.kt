package com.ansh.awsnotifier.session

import android.content.Context

data class StoredSubscription(
    val subscriptionArn: String,
    val topicArn: String,
    val region: String
)

/**
 * Backward-compatible facade for existing callers.
 *
 * Storage responsibilities are delegated to focused stores to avoid keeping
 * all session concerns in one God object.
 */
object UserSession {
    private val prefsProvider = SecurePrefsProvider()
    private val onboardingStateStore = OnboardingStateStore(prefsProvider)
    private val credentialStore = CredentialStore(prefsProvider)
    private val subscriptionStore = SubscriptionStore(prefsProvider)
    private val deviceRegistrationStore = DeviceRegistrationStore(prefsProvider)
    private val platformArnStore = PlatformArnStore(prefsProvider)
    private val retentionSettingsStore = RetentionSettingsStore(prefsProvider)

    fun isOnboardingComplete(context: Context): Boolean {
        return onboardingStateStore.isOnboardingComplete(context)
    }

    fun setOnboardingComplete(context: Context, done: Boolean) {
        onboardingStateStore.setOnboardingComplete(context, done)
    }

    fun isBiometricEnabled(context: Context): Boolean {
        return onboardingStateStore.isBiometricEnabled(context)
    }

    fun setBiometricEnabled(context: Context, enabled: Boolean) {
        onboardingStateStore.setBiometricEnabled(context, enabled)
    }

    fun saveCredentials(context: Context, access: String, secret: String) {
        credentialStore.saveCredentials(context, access, secret)
    }

    fun getCredentials(context: Context): Pair<String, String>? {
        return credentialStore.getCredentials(context)
    }

    fun clearCredentials(context: Context) {
        credentialStore.clearCredentials(context)
    }

    fun saveCurrentRegion(context: Context, region: String) {
        credentialStore.saveCurrentRegion(context, region)
    }

    fun getCurrentRegion(context: Context): String? {
        return credentialStore.getCurrentRegion(context)
    }

    fun saveDeviceEndpointArn(context: Context, endpointArn: String) {
        deviceRegistrationStore.saveDeviceEndpointArn(context, endpointArn)
    }

    fun getDeviceEndpointArn(context: Context): String? {
        return deviceRegistrationStore.getDeviceEndpointArn(context)
    }

    fun saveSubscription(context: Context, subscriptionArn: String, topicArn: String, region: String) {
        subscriptionStore.saveSubscription(context, subscriptionArn, topicArn, region)
    }

    fun removeSubscription(context: Context, subscriptionArn: String) {
        subscriptionStore.removeSubscription(context, subscriptionArn)
    }

    fun removeSubscriptionsByTopicArn(context: Context, topicArn: String) {
        subscriptionStore.removeSubscriptionsByTopicArn(context, topicArn)
    }

    fun getAllSubscriptions(context: Context): List<StoredSubscription> {
        return subscriptionStore.getAllSubscriptions(context)
    }

    fun saveFcmToken(context: Context, token: String) {
        deviceRegistrationStore.saveFcmToken(context, token)
    }

    fun getFcmToken(context: Context): String? {
        return deviceRegistrationStore.getFcmToken(context)
    }

    fun isTokenRefreshPending(context: Context): Boolean {
        return deviceRegistrationStore.isTokenRefreshPending(context)
    }

    fun setTokenRefreshPending(context: Context, pending: Boolean) {
        deviceRegistrationStore.setTokenRefreshPending(context, pending)
    }

    fun savePlatformApplicationArn(context: Context, arn: String) {
        platformArnStore.savePlatformApplicationArn(context, arn)
    }

    fun getPlatformApplicationArn(context: Context): String? {
        return platformArnStore.getPlatformApplicationArn(context)
    }

    fun savePlatformArnForRegion(context: Context, region: String, arn: String) {
        platformArnStore.savePlatformArnForRegion(context, region, arn)
    }

    fun getPlatformArnForRegion(context: Context, region: String): String? {
        return platformArnStore.getPlatformArnForRegion(context, region)
    }

    fun getPlatformArn(context: Context, region: String): String? {
        return platformArnStore.getPlatformArn(context, region)
    }

    fun saveRetentionDays(context: Context, days: Int) {
        retentionSettingsStore.saveRetentionDays(context, days)
    }

    fun getRetentionDays(context: Context): Int {
        return retentionSettingsStore.getRetentionDays(context)
    }

    fun clearAllData(context: Context) {
        prefsProvider.secure(context).edit().clear().apply()
        prefsProvider.legacyAwsPrefs(context).edit().clear().apply()
        prefsProvider.clearCache()
    }
}
