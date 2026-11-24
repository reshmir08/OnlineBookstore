package com.bookstore.app.data.auth

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

import android.content.SharedPreferences

/** Persists the signed-in user id so the session survives app restarts. Only the id is stored, never credentials. */
class SessionManager private constructor(
    private val prefs: SharedPreferences?,
    initialUserId: Long?
) {
    constructor(context: Context) : this(
        context.getSharedPreferences("session", Context.MODE_PRIVATE).let { prefs ->
            prefs to prefs.getLong(KEY_USER_ID, NO_USER).takeIf { it != NO_USER }
        }
    )

    private constructor(pair: Pair<SharedPreferences, Long?>) : this(pair.first, pair.second)

    constructor(initialUserId: Long? = null) : this(null, initialUserId)

    private val _userId = MutableStateFlow(initialUserId)
    val userId: StateFlow<Long?> = _userId.asStateFlow()

    fun signIn(id: Long) {
        prefs?.edit()?.putLong(KEY_USER_ID, id)?.apply()
        _userId.value = id
    }

    fun signOut() {
        prefs?.edit()?.remove(KEY_USER_ID)?.apply()
        _userId.value = null
    }

    /** The signed-in user id; throws when called while signed out (a programming error in a screen behind auth). */
    fun requireUserId(): Long = _userId.value ?: error("No signed-in user")

    private companion object {
        const val KEY_USER_ID = "user_id"
        const val NO_USER = -1L
    }
}
