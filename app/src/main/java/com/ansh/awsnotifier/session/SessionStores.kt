package com.ansh.awsnotifier.session

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

internal class SecurePrefsProvider {
    companion object {
        private const val SECURE_PREF_NAME = "aws_notifier_secure_prefs"
        private const val LEGACY_AWS_PREF_NAME = "aws_prefs"
    }

    @Volatile
    private var securePrefsInstance: SharedPreferences? = null

    fun secure(context: Context): SharedPreferences {
        return securePrefsInstance ?: synchronized(this) {
            securePrefsInstance ?: createEncryptedSharedPreferences(context).also {
                securePrefsInstance = it
            }
        }
    }

    fun legacyAwsPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(LEGACY_AWS_PREF_NAME, Context.MODE_PRIVATE)
    }

    fun clearCache() {
        securePrefsInstance = null
    }

    private fun createEncryptedSharedPreferences(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            SECURE_PREF_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
}

internal class OnboardingStateStore(private val prefsProvider: SecurePrefsProvider) {
    companion object {
        private const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
    }

    fun isOnboardingComplete(context: Context): Boolean {
        return prefsProvider.secure(context).getBoolean(KEY_ONBOARDING_COMPLETE, false)
    }

    fun setOnboardingComplete(context: Context, done: Boolean) {
        prefsProvider.secure(context).edit().putBoolean(KEY_ONBOARDING_COMPLETE, done).apply()
    }

    fun isBiometricEnabled(context: Context): Boolean {
        return prefsProvider.secure(context).getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }

    fun setBiometricEnabled(context: Context, enabled: Boolean) {
        prefsProvider.secure(context).edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }
}

internal class CredentialStore(private val prefsProvider: SecurePrefsProvider) {
    companion object {
        private const val KEY_ACCESS_KEY = "access_key"
        private const val KEY_SECRET_KEY = "secret_key"
        private const val KEY_CURRENT_REGION = "current_region"
    }

    fun saveCredentials(context: Context, access: String, secret: String) {
        prefsProvider.secure(context).edit()
            .putString(KEY_ACCESS_KEY, access)
            .putString(KEY_SECRET_KEY, secret)
            .apply()
    }

    fun getCredentials(context: Context): Pair<String, String>? {
        val prefs = prefsProvider.secure(context)
        val access = prefs.getString(KEY_ACCESS_KEY, null)
        val secret = prefs.getString(KEY_SECRET_KEY, null)
        return if (access != null && secret != null) access to secret else null
    }

    fun clearCredentials(context: Context) {
        prefsProvider.secure(context).edit()
            .remove(KEY_ACCESS_KEY)
            .remove(KEY_SECRET_KEY)
            .apply()
    }

    fun saveCurrentRegion(context: Context, region: String) {
        prefsProvider.secure(context).edit().putString(KEY_CURRENT_REGION, region).apply()
    }

    fun getCurrentRegion(context: Context): String? {
        return prefsProvider.secure(context).getString(KEY_CURRENT_REGION, null)
    }
}

internal object SubscriptionJsonCodec {
    private val gson = Gson()
    private val typeToken = object : TypeToken<MutableList<StoredSubscription>>() {}.type

    fun fromJson(json: String?): MutableList<StoredSubscription> {
        if (json.isNullOrBlank()) return mutableListOf()
        return gson.fromJson(json, typeToken) ?: mutableListOf()
    }

    fun toJson(list: List<StoredSubscription>): String {
        return gson.toJson(list)
    }
}

internal class SubscriptionStore(private val prefsProvider: SecurePrefsProvider) {
    companion object {
        private const val KEY_SUBSCRIPTIONS_JSON = "subscriptions_json"
    }

    private fun loadSubscriptions(context: Context): MutableList<StoredSubscription> {
        val json = prefsProvider.secure(context).getString(KEY_SUBSCRIPTIONS_JSON, null)
        return SubscriptionJsonCodec.fromJson(json)
    }

    private fun saveSubscriptions(context: Context, list: MutableList<StoredSubscription>) {
        prefsProvider.secure(context).edit()
            .putString(KEY_SUBSCRIPTIONS_JSON, SubscriptionJsonCodec.toJson(list))
            .apply()
    }

    fun saveSubscription(context: Context, subscriptionArn: String, topicArn: String, region: String) {
        val list = loadSubscriptions(context)
        list.removeAll { it.subscriptionArn == subscriptionArn }
        list.add(StoredSubscription(subscriptionArn, topicArn, region))
        saveSubscriptions(context, list)
    }

    fun removeSubscription(context: Context, subscriptionArn: String) {
        val list = loadSubscriptions(context)
        list.removeAll { it.subscriptionArn == subscriptionArn }
        saveSubscriptions(context, list)
    }

    fun removeSubscriptionsByTopicArn(context: Context, topicArn: String) {
        val list = loadSubscriptions(context)
        list.removeAll { it.topicArn == topicArn }
        saveSubscriptions(context, list)
    }

    fun getAllSubscriptions(context: Context): List<StoredSubscription> {
        return loadSubscriptions(context)
    }
}

internal class DeviceRegistrationStore(private val prefsProvider: SecurePrefsProvider) {
    companion object {
        private const val KEY_FCM_TOKEN = "fcm_token"
        private const val KEY_TOKEN_REFRESH_PENDING = "token_refresh_pending"
        private const val KEY_DEVICE_ENDPOINT_ARN = "device_endpoint_arn"
    }

    fun saveDeviceEndpointArn(context: Context, endpointArn: String) {
        prefsProvider.secure(context).edit().putString(KEY_DEVICE_ENDPOINT_ARN, endpointArn).apply()
    }

    fun getDeviceEndpointArn(context: Context): String? {
        return prefsProvider.secure(context).getString(KEY_DEVICE_ENDPOINT_ARN, null)
    }

    fun saveFcmToken(context: Context, token: String) {
        prefsProvider.secure(context).edit().putString(KEY_FCM_TOKEN, token).apply()
    }

    fun getFcmToken(context: Context): String? {
        return prefsProvider.secure(context).getString(KEY_FCM_TOKEN, null)
    }

    fun isTokenRefreshPending(context: Context): Boolean {
        return prefsProvider.secure(context).getBoolean(KEY_TOKEN_REFRESH_PENDING, false)
    }

    fun setTokenRefreshPending(context: Context, pending: Boolean) {
        prefsProvider.secure(context).edit().putBoolean(KEY_TOKEN_REFRESH_PENDING, pending).apply()
    }
}

internal class PlatformArnStore(private val prefsProvider: SecurePrefsProvider) {
    companion object {
        private const val KEY_PLATFORM_APPLICATION_ARN = "platform_application_arn"
        private const val KEY_PLATFORM_ARNS_MAP = "platform_arns_json"
    }

    private val gson = Gson()
    private val mapTypeToken = object : TypeToken<MutableMap<String, String>>() {}.type

    fun savePlatformApplicationArn(context: Context, arn: String) {
        prefsProvider.secure(context).edit().putString(KEY_PLATFORM_APPLICATION_ARN, arn).apply()
    }

    fun getPlatformApplicationArn(context: Context): String? {
        return prefsProvider.secure(context).getString(KEY_PLATFORM_APPLICATION_ARN, null)
    }

    fun savePlatformArnForRegion(context: Context, region: String, arn: String) {
        val map = loadPlatformArnMap(context)
        map[region] = arn
        savePlatformArnMap(context, map)

        // Keep legacy key in sync for backward compatibility
        prefsProvider.legacyAwsPrefs(context).edit().putString(legacyRegionKey(region), arn).apply()
    }

    fun getPlatformArnForRegion(context: Context, region: String): String? {
        val mapArn = loadPlatformArnMap(context)[region]
        if (mapArn != null) return mapArn

        val legacyArn = prefsProvider.legacyAwsPrefs(context).getString(legacyRegionKey(region), null)
        if (legacyArn != null) {
            val updatedMap = loadPlatformArnMap(context)
            updatedMap[region] = legacyArn
            savePlatformArnMap(context, updatedMap)
        }
        return legacyArn
    }

    fun getPlatformArn(context: Context, region: String): String? {
        return getPlatformArnForRegion(context, region)
    }

    private fun legacyRegionKey(region: String): String = "platform_arn_$region"

    private fun loadPlatformArnMap(context: Context): MutableMap<String, String> {
        val json = prefsProvider.secure(context).getString(KEY_PLATFORM_ARNS_MAP, null) ?: return mutableMapOf()
        return gson.fromJson(json, mapTypeToken) ?: mutableMapOf()
    }

    private fun savePlatformArnMap(context: Context, map: MutableMap<String, String>) {
        prefsProvider.secure(context).edit().putString(KEY_PLATFORM_ARNS_MAP, gson.toJson(map)).apply()
    }
}

internal class RetentionSettingsStore(private val prefsProvider: SecurePrefsProvider) {
    companion object {
        private const val KEY_RETENTION_DAYS = "history_retention_days"
        private const val DEFAULT_RETENTION_DAYS = 15
    }

    fun saveRetentionDays(context: Context, days: Int) {
        prefsProvider.secure(context).edit().putInt(KEY_RETENTION_DAYS, days).apply()
    }

    fun getRetentionDays(context: Context): Int {
        return prefsProvider.secure(context).getInt(KEY_RETENTION_DAYS, DEFAULT_RETENTION_DAYS)
    }
}
