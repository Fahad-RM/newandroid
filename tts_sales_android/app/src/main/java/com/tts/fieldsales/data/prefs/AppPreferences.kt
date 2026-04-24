package com.tts.fieldsales.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tts_field_sales_prefs")

class AppPreferences(private val context: Context) {

    companion object {
        val KEY_ODOO_URL = stringPreferencesKey("odoo_url")
        val KEY_DB_NAME = stringPreferencesKey("db_name")
        val KEY_USERNAME = stringPreferencesKey("username")
        val KEY_PASSWORD = stringPreferencesKey("password")
        val KEY_USER_ID = intPreferencesKey("user_id")
        val KEY_USER_NAME = stringPreferencesKey("user_name")
        val KEY_IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val KEY_PRINTER_ADDRESS = stringPreferencesKey("printer_address")
        val KEY_PRINTER_NAME = stringPreferencesKey("printer_name")
        val KEY_PAPER_WIDTH = stringPreferencesKey("paper_width") // "3inch" or "4inch"
        val KEY_DAILY_GOAL = floatPreferencesKey("daily_goal")
        val KEY_SESSION_ID = stringPreferencesKey("session_id")
    }

    // Odoo URL
    val odooUrl: Flow<String> = context.dataStore.data.map { it[KEY_ODOO_URL] ?: "" }
    val dbName: Flow<String> = context.dataStore.data.map { it[KEY_DB_NAME] ?: "" }
    val username: Flow<String> = context.dataStore.data.map { it[KEY_USERNAME] ?: "" }
    val password: Flow<String> = context.dataStore.data.map { it[KEY_PASSWORD] ?: "" }
    val userId: Flow<Int> = context.dataStore.data.map { it[KEY_USER_ID] ?: 0 }
    val userName: Flow<String> = context.dataStore.data.map { it[KEY_USER_NAME] ?: "" }
    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { it[KEY_IS_LOGGED_IN] ?: false }
    val printerAddress: Flow<String> = context.dataStore.data.map { it[KEY_PRINTER_ADDRESS] ?: "" }
    val printerName: Flow<String> = context.dataStore.data.map { it[KEY_PRINTER_NAME] ?: "" }
    val paperWidth: Flow<String> = context.dataStore.data.map { it[KEY_PAPER_WIDTH] ?: "3inch" }
    val sessionId: Flow<String> = context.dataStore.data.map { it[KEY_SESSION_ID] ?: "" }

    suspend fun saveLoginInfo(url: String, db: String, username: String, password: String, userId: Int, userName: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ODOO_URL] = url
            prefs[KEY_DB_NAME] = db
            prefs[KEY_USERNAME] = username
            prefs[KEY_PASSWORD] = password
            prefs[KEY_USER_ID] = userId
            prefs[KEY_USER_NAME] = userName
            prefs[KEY_IS_LOGGED_IN] = true
        }
    }

    suspend fun savePrinterInfo(address: String, name: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PRINTER_ADDRESS] = address
            prefs[KEY_PRINTER_NAME] = name
        }
    }

    suspend fun savePaperWidth(width: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PAPER_WIDTH] = width
        }
    }

    suspend fun logout() {
        context.dataStore.edit { prefs ->
            prefs[KEY_IS_LOGGED_IN] = false
            prefs[KEY_USER_ID] = 0
            prefs[KEY_USER_NAME] = ""
            prefs[KEY_SESSION_ID] = ""
        }
    }

    suspend fun getOdooUrl(): String = context.dataStore.data.first()[KEY_ODOO_URL] ?: ""
    suspend fun getDbName(): String = context.dataStore.data.first()[KEY_DB_NAME] ?: ""
    suspend fun getUsername(): String = context.dataStore.data.first()[KEY_USERNAME] ?: ""
    suspend fun getPassword(): String = context.dataStore.data.first()[KEY_PASSWORD] ?: ""
    suspend fun getUserId(): Int = context.dataStore.data.first()[KEY_USER_ID] ?: 0
    suspend fun isLoggedIn(): Boolean = context.dataStore.data.first()[KEY_IS_LOGGED_IN] ?: false
    suspend fun getPrinterAddress(): String = context.dataStore.data.first()[KEY_PRINTER_ADDRESS] ?: ""
    suspend fun getPaperWidth(): String = context.dataStore.data.first()[KEY_PAPER_WIDTH] ?: "3inch"
}
