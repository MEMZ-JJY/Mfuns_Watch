package net.mfuns.watch.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "mfuns_session")

class SessionStore(private val context: Context) {

    data class Session(
        val token: String,
        val userId: Int,
        val name: String,
        val avatar: String,
    ) {
        val loggedIn: Boolean get() = token.isNotEmpty()
    }

    val sessionFlow: Flow<Session> = context.dataStore.data.map { prefs ->
        Session(
            token = prefs[KEY_TOKEN] ?: "",
            userId = prefs[KEY_USER_ID]?.toIntOrNull() ?: 0,
            name = prefs[KEY_NAME] ?: "",
            avatar = prefs[KEY_AVATAR] ?: "",
        )
    }

    suspend fun current(): Session = sessionFlow.first()

    suspend fun save(token: String, userId: Int, name: String, avatar: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TOKEN] = token
            prefs[KEY_USER_ID] = userId.toString()
            prefs[KEY_NAME] = name
            prefs[KEY_AVATAR] = avatar
        }
    }

    suspend fun updateUser(userId: Int, name: String, avatar: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_USER_ID] = userId.toString()
            prefs[KEY_NAME] = name
            prefs[KEY_AVATAR] = avatar
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }

    private companion object {
        val KEY_TOKEN = stringPreferencesKey("token")
        val KEY_USER_ID = stringPreferencesKey("user_id")
        val KEY_NAME = stringPreferencesKey("name")
        val KEY_AVATAR = stringPreferencesKey("avatar")
    }
}
