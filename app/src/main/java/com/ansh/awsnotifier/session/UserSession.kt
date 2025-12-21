package com.ansh.awsnotifier.session

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class StoredSubscription(
    val subscriptionArn: String,
    val topicArn: String,
    val region: String
)

object UserSession {

    private const val PREF_NAME = "aws_notifier_secure_prefs"

    private const val KEY_ACCESS_KEY = "access_key"
    private const val KEY_SECRET_KEY = "secret_key"

    private const val KEY_CURRENT_REGION = "current_region"
    private const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"

    private const val KEY_FCM_TOKEN = "fcm_token"
    private const val KEY_TOKEN_REFRESH_PENDING = "token_refresh_pending"

    private const val KEY_DEVICE_ENDPOINT_ARN = "device_endpoint_arn"

    // ❗ NEW: STRUCTURED subscription storage
    private const val KEY_SUBSCRIPTIONS_JSON = "subscriptions_json"

    // NEW: SNS Platform Application ARN
    private const val KEY_PLATFORM_APPLICATION_ARN = "platform_application_arn"

    @Volatile
    private var prefsInstance: SharedPreferences? = null

    private fun getPrefs(context: Context): SharedPreferences {
        return prefsInstance ?: synchronized(this) {
            prefsInstance ?: createEncryptedSharedPreferences(context).also {
                prefsInstance = it
            }
        }
    }

    private fun createEncryptedSharedPreferences(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            PREF_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // =====================================
    // ONBOARDING
    // =====================================

    fun isOnboardingComplete(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_ONBOARDING_COMPLETE, false)
    }

    fun setOnboardingComplete(context: Context, done: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_ONBOARDING_COMPLETE, done).apply()
    }

    // =====================================
    // AWS CREDENTIALS
    // =====================================

    fun saveCredentials(context: Context, access: String, secret: String) {
        getPrefs(context).edit()
            .putString(KEY_ACCESS_KEY, access)
            .putString(KEY_SECRET_KEY, secret)
            .apply()
    }

    fun getCredentials(context: Context): Pair<String, String>? {
        val p = getPrefs(context)
        val a = p.getString(KEY_ACCESS_KEY, null)
        val s = p.getString(KEY_SECRET_KEY, null)
        return if (a != null && s != null) a to s else null
    }

    fun clearCredentials(context: Context) {
        getPrefs(context).edit()
            .remove(KEY_ACCESS_KEY)
            .remove(KEY_SECRET_KEY)
            .apply()
    }

    // =====================================
    // REGION
    // =====================================

    fun saveCurrentRegion(context: Context, region: String) {
        getPrefs(context).edit().putString(KEY_CURRENT_REGION, region).apply()
    }

    fun getCurrentRegion(context: Context): String? {
        return getPrefs(context).getString(KEY_CURRENT_REGION, null)
    }

    // =====================================
    // DEVICE ENDPOINT
    // =====================================

    fun saveDeviceEndpointArn(context: Context, endpointArn: String) {
        getPrefs(context).edit().putString(KEY_DEVICE_ENDPOINT_ARN, endpointArn).apply()
    }

    fun getDeviceEndpointArn(context: Context): String? {
        return getPrefs(context).getString(KEY_DEVICE_ENDPOINT_ARN, null)
    }

    // =====================================
    // SUBSCRIPTIONS  (STRUCTURED JSON FORMAT)
    // =====================================

    private val gson = Gson()
    private val typeToken = object : TypeToken<MutableList<StoredSubscription>>() {}.type

    private fun loadSubscriptions(context: Context): MutableList<StoredSubscription> {
        val json = getPrefs(context).getString(KEY_SUBSCRIPTIONS_JSON, null) ?: return mutableListOf()
        return gson.fromJson(json, typeToken)
    }

    private fun saveSubscriptions(context: Context, list: MutableList<StoredSubscription>) {
        getPrefs(context).edit()
            .putString(KEY_SUBSCRIPTIONS_JSON, gson.toJson(list))
            .apply()
    }

    fun saveSubscription(context: Context, subscriptionArn: String, topicArn: String, region: String) {
        val list = loadSubscriptions(context)

        // Remove duplicates
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

    // =====================================
    // FCM TOKEN
    // =====================================

    fun saveFcmToken(context: Context, token: String) {
        getPrefs(context).edit().putString(KEY_FCM_TOKEN, token).apply()
    }

    fun getFcmToken(context: Context): String? {
        return getPrefs(context).getString(KEY_FCM_TOKEN, null)
    }

    fun isTokenRefreshPending(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_TOKEN_REFRESH_PENDING, false)
    }

    fun setTokenRefreshPending(context: Context, pending: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_TOKEN_REFRESH_PENDING, pending).apply()
    }

    // =====================================
    // PLATFORM APPLICATION ARN
    // =====================================

    fun savePlatformApplicationArn(context: Context, arn: String) {
        getPrefs(context).edit().putString(KEY_PLATFORM_APPLICATION_ARN, arn).apply()
    }

    fun getPlatformApplicationArn(context: Context): String? {
        return getPrefs(context).getString(KEY_PLATFORM_APPLICATION_ARN, null)
    }

    // =====================================
    // CLEAR ALL
    // =====================================

    fun clearAllData(context: Context) {
        getPrefs(context).edit().clear().apply()
        prefsInstance = null
    }

    // ===========================================================
    // PLATFORM APPLICATION ARNS (DYNAMIC PER REGION)
    // ===========================================================

    // Store ARNs per region instead of a single value
    private const val KEY_PLATFORM_ARNS_MAP = "platform_arns_json"

    private val gsonPlatform = Gson()
    private val mapTypeToken = object : TypeToken<MutableMap<String, String>>() {}.type

    private fun loadPlatformArnMap(context: Context): MutableMap<String, String> {
        val json = getPrefs(context).getString(KEY_PLATFORM_ARNS_MAP, null) ?: return mutableMapOf()
        return gsonPlatform.fromJson(json, mapTypeToken)
    }

    private fun savePlatformArnMap(context: Context, map: MutableMap<String, String>) {
        getPrefs(context).edit().putString(KEY_PLATFORM_ARNS_MAP, gsonPlatform.toJson(map)).apply()
    }

    fun savePlatformArnForRegion(context: Context, region: String, arn: String) {
        val prefs = context.getSharedPreferences("aws_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("platform_arn_$region", arn).apply()
    }

    fun getPlatformArnForRegion(context: Context, region: String): String? {
        val prefs = context.getSharedPreferences("aws_prefs", Context.MODE_PRIVATE)
        return prefs.getString("platform_arn_$region", null)
    }


    fun getPlatformArn(context: Context, region: String): String? {
        val map = loadPlatformArnMap(context)
        return map[region]
    }

    // ===========================================================
    // HISTORY RETENTION POLICY
    // ===========================================================
    private const val KEY_RETENTION_DAYS = "history_retention_days"

    fun saveRetentionDays(context: Context, days: Int) {
        getPrefs(context).edit().putInt(KEY_RETENTION_DAYS, days).apply()
    }

    fun getRetentionDays(context: Context): Int {
        return getPrefs(context).getInt(KEY_RETENTION_DAYS, 15) // Default 15 days
    }
}