package com.eventradar.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppSettings(
    val ticketmasterKey: String = "",
    val skiddleKey: String = "",
    val monthsAhead: Int = 3,
    val autoRefresh: Boolean = true,
    val lastRefresh: Long = 0L,
) {
    val hasApiKey: Boolean
        get() = ticketmasterKey.isNotBlank() || skiddleKey.isNotBlank()
}

/** Small SharedPreferences wrapper exposing settings as a StateFlow. Keys are stored on-device only. */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(read())
    val state: StateFlow<AppSettings> = _state.asStateFlow()
    val current: AppSettings get() = _state.value

    private fun read() = AppSettings(
        ticketmasterKey = prefs.getString("tm", "") ?: "",
        skiddleKey = prefs.getString("skiddle", "") ?: "",
        monthsAhead = prefs.getInt("months", 3),
        autoRefresh = prefs.getBoolean("auto", true),
        lastRefresh = prefs.getLong("last", 0L),
    )

    fun update(transform: (AppSettings) -> AppSettings) {
        val s = transform(_state.value)
        prefs.edit()
            .putString("tm", s.ticketmasterKey.trim())
            .putString("skiddle", s.skiddleKey.trim())
            .putInt("months", s.monthsAhead)
            .putBoolean("auto", s.autoRefresh)
            .putLong("last", s.lastRefresh)
            .apply()
        _state.value = s
    }
}
