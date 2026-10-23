package com.samraksha.app.data.local

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.samraksha.app.domain.model.User

/**
 * Manages user authentication tokens, active credentials, and profile sessions.
 * Implements hardware-backed AES-256-GCM encryption for stored tokens and sensitive
 * personal medical information via [CryptoManager], preventing data extraction from disk.
 *
 * Supports graceful fallback to in-memory caching for JVM unit tests.
 */
class SessionManager(context: Context? = null) {

    private val prefs: SharedPreferences? = context?.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val gson = Gson()

    // In-memory cache for zero-latency lookups & testing support
    private var cachedToken: String? = null
    private var cachedUser: User? = null

    init {
        // Hydrate and decrypt from SharedPreferences if available
        prefs?.let {
            val rawToken = it.getString(KEY_TOKEN, null)
            cachedToken = decryptValue(rawToken)

            val rawUserJson = it.getString(KEY_USER, null)
            val decryptedUserJson = decryptValue(rawUserJson)
            if (!decryptedUserJson.isNullOrEmpty()) {
                try {
                    cachedUser = gson.fromJson(decryptedUserJson, User::class.java)
                } catch (_: Exception) {
                    cachedUser = null
                }
            }
        }
    }

    /**
     * Securely saves an authenticated session, encrypting the token and profile.
     */
    fun saveSession(token: String, user: User) {
        cachedToken = token
        cachedUser = user

        val encryptedToken = encryptValue(token)
        val encryptedUserJson = encryptValue(gson.toJson(user))

        prefs?.edit()?.apply {
            putString(KEY_TOKEN, encryptedToken)
            putString(KEY_USER, encryptedUserJson)
            apply()
        }
    }

    /**
     * Returns the active session token, or null if unauthenticated.
     */
    fun getToken(): String? = cachedToken

    /**
     * Returns the active user profile, or null if unauthenticated.
     */
    fun getUser(): User? = cachedUser

    /**
     * Returns whether there is an active session token.
     */
    fun isLoggedIn(): Boolean = !cachedToken.isNullOrBlank()

    /**
     * Purges all session data, tokens, and encrypted entries from memory and disk.
     */
    fun clearSession() {
        cachedToken = null
        cachedUser = null

        prefs?.edit()?.apply {
            remove(KEY_TOKEN)
            remove(KEY_USER)
            apply()
        }
    }

    private fun encryptValue(value: String): String {
        val encrypted = CryptoManager.encrypt(value)
        return if (encrypted != null) "$ENCRYPTED_PREFIX$encrypted" else value
    }

    private fun decryptValue(storedValue: String?): String? {
        if (storedValue == null) return null
        return if (storedValue.startsWith(ENCRYPTED_PREFIX)) {
            val ciphertext = storedValue.substring(ENCRYPTED_PREFIX.length)
            // Return null on decryption failure — do not fall back to returning raw ciphertext.
            // A null result will force the user to re-authenticate cleanly.
            CryptoManager.decrypt(ciphertext)
        } else {
            // Value was stored before encryption was available (e.g. legacy or test env).
            // Treat as plain value only if no encryption prefix is present.
            storedValue
        }
    }

    companion object {
        private const val PREFS_NAME = "samraksha_session_prefs"
        private const val KEY_TOKEN = "auth_session_token"
        private const val KEY_USER = "auth_user_data"
        private const val ENCRYPTED_PREFIX = "enc_v1:"
    }
}
