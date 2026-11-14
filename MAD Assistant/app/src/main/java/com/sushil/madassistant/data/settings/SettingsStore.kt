package com.sushil.madassistant.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "mad_settings")

object SettingsKeys {
    val PROVIDER = stringPreferencesKey("ai_provider")
    val GEMINI_KEY = stringPreferencesKey("gemini_api_key")
    val OPENAI_KEY = stringPreferencesKey("openai_api_key")
    val WAKE_WORD_ENABLED = booleanPreferencesKey("wake_word_enabled")
    val FIRST_RUN_DONE = booleanPreferencesKey("first_run_done")
    val TECH_NAME = stringPreferencesKey("tech_name")
    val TECH_MOBILE = stringPreferencesKey("tech_mobile")
    val TECH_BUSINESS = stringPreferencesKey("tech_business")
    val TECH_ADDRESS = stringPreferencesKey("tech_address")
}

class SettingsStore(private val context: Context) {

    val provider: Flow<String> = context.dataStore.data.map { it[SettingsKeys.PROVIDER] ?: "gemini" }
    val geminiKey: Flow<String> = context.dataStore.data.map { it[SettingsKeys.GEMINI_KEY] ?: "" }
    val openaiKey: Flow<String> = context.dataStore.data.map { it[SettingsKeys.OPENAI_KEY] ?: "" }
    val wakeWordEnabled: Flow<Boolean> = context.dataStore.data.map { it[SettingsKeys.WAKE_WORD_ENABLED] ?: false }
    val firstRunDone: Flow<Boolean> = context.dataStore.data.map { it[SettingsKeys.FIRST_RUN_DONE] ?: false }
    val techName: Flow<String> = context.dataStore.data.map { it[SettingsKeys.TECH_NAME] ?: "Sushil Sheokand" }
    val techMobile: Flow<String> = context.dataStore.data.map { it[SettingsKeys.TECH_MOBILE] ?: "" }
    val techBusiness: Flow<String> = context.dataStore.data.map { it[SettingsKeys.TECH_BUSINESS] ?: "RAJAT ELECTRO-MECH SOLUTIONS (REMS)" }
    val techAddress: Flow<String> = context.dataStore.data.map { it[SettingsKeys.TECH_ADDRESS] ?: "Saffa Kheri Road, Kharak Bhura, Uchana" }

    suspend fun setProvider(v: String) = context.dataStore.edit { it[SettingsKeys.PROVIDER] = v }
    suspend fun setGeminiKey(v: String) = context.dataStore.edit { it[SettingsKeys.GEMINI_KEY] = v.trim() }
    suspend fun setOpenaiKey(v: String) = context.dataStore.edit { it[SettingsKeys.OPENAI_KEY] = v.trim() }
    suspend fun setWakeWordEnabled(v: Boolean) = context.dataStore.edit { it[SettingsKeys.WAKE_WORD_ENABLED] = v }
    suspend fun setFirstRunDone() = context.dataStore.edit { it[SettingsKeys.FIRST_RUN_DONE] = true }

    suspend fun setTechnician(name: String, mobile: String, business: String, address: String) {
        context.dataStore.edit {
            it[SettingsKeys.TECH_NAME] = name.trim()
            it[SettingsKeys.TECH_MOBILE] = mobile.trim()
            it[SettingsKeys.TECH_BUSINESS] = business.trim()
            it[SettingsKeys.TECH_ADDRESS] = address.trim()
        }
    }

    suspend fun getActiveApiKey(): String {
        val p = context.dataStore.data.first()
        return when (p[SettingsKeys.PROVIDER] ?: "gemini") {
            "openai" -> p[SettingsKeys.OPENAI_KEY] ?: ""
            else -> p[SettingsKeys.GEMINI_KEY] ?: ""
        }
    }
}
