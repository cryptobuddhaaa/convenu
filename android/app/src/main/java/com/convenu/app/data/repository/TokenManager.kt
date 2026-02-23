package com.convenu.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private companion object {
        val KEY_JWT = stringPreferencesKey("jwt_token")
        val KEY_USER_ID = stringPreferencesKey("user_id")
        val KEY_REFRESH_TOKEN = stringPreferencesKey("refresh_token")
    }

    val tokenFlow: Flow<String?> = dataStore.data.map { prefs ->
        prefs[KEY_JWT]
    }

    val userIdFlow: Flow<String?> = dataStore.data.map { prefs ->
        prefs[KEY_USER_ID]
    }

    suspend fun saveSession(jwt: String, userId: String, refreshToken: String? = null) {
        dataStore.edit { prefs ->
            prefs[KEY_JWT] = jwt
            prefs[KEY_USER_ID] = userId
            refreshToken?.let { prefs[KEY_REFRESH_TOKEN] = it }
        }
    }

    suspend fun clearSession() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_JWT)
            prefs.remove(KEY_USER_ID)
            prefs.remove(KEY_REFRESH_TOKEN)
        }
    }
}
