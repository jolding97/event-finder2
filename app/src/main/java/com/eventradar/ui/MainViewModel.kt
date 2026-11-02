package com.eventradar.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eventradar.EventRadarApp
import com.eventradar.data.AppSettings
import com.eventradar.data.EventEntity
import com.eventradar.data.Interest
import com.eventradar.work.RefreshWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as EventRadarApp
    private val repo = app.repository

    val settings: StateFlow<AppSettings> = app.settings.state
    val interests: StateFlow<List<Interest>> =
        repo.observeInterests().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val events: StateFlow<List<EventEntity>> =
        repo.observeEvents().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _refreshing = MutableStateFlow(false)
    val refreshing = _refreshing.asStateFlow()

    /** (done, total) searches during a full refresh; null when idle. */
    private val _progress = MutableStateFlow<Pair<Int, Int>?>(null)
    val progress = _progress.asStateFlow()

    /** One-shot messages for the snackbar. */
    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()
    fun messageShown() { _message.value = null }

    /** Searches all interests for the next [months] months (also remembered as the default). */
    fun refreshAll(months: Int = settings.value.monthsAhead) {
        if (_refreshing.value) return
        if (months != settings.value.monthsAhead) app.settings.update { it.copy(monthsAhead = months) }
        viewModelScope.launch {
            _refreshing.value = true
            try {
                val r = repo.refreshAll(months) { done, total -> _progress.value = done to total }
                _message.value = "Next $months month${if (months == 1) "" else "s"}: ${r.summary}"
            } catch (e: Exception) {
                _message.value = "Refresh failed: ${e.message}"
            } finally {
                _refreshing.value = false
                _progress.value = null
            }
        }
    }

    fun addInterest(name: String, category: String, feedUrl: String? = null, ukOnly: Boolean = true) {
        if (name.isBlank()) return
        if (interests.value.any { it.name.equals(name.trim(), ignoreCase = true) }) {
            _message.value = "\"${name.trim()}\" is already on your list"
            return
        }
        viewModelScope.launch {
            val interest = repo.addInterest(name, category, feedUrl, ukOnly)
            if (!settings.value.hasApiKey && interest.feedUrl == null) {
                _message.value = "Added. Add a free API key in Settings (or a calendar feed) to find events."
                return@launch
            }
            searchOne(interest)
        }
    }

    /** Saves changes to an interest (e.g. a feed URL added later) and re-searches it. */
    fun updateInterest(interest: Interest) {
        viewModelScope.launch {
            repo.updateInterest(interest)
            searchOne(interest)
        }
    }

    private suspend fun searchOne(interest: Interest) {
        _refreshing.value = true
        try {
            val r = repo.refreshInterest(interest)
            _message.value = "${interest.name}: ${r.summary}"
        } catch (e: Exception) {
            _message.value = "Search failed: ${e.message}"
        } finally {
            _refreshing.value = false
        }
    }

    fun deleteInterest(interest: Interest) = viewModelScope.launch { repo.deleteInterest(interest) }

    fun clearEvents() = viewModelScope.launch {
        repo.clearEvents()
        _message.value = "Calendar cleared"
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        val before = settings.value.autoRefresh
        app.settings.update(transform)
        if (settings.value.autoRefresh != before) RefreshWorker.schedule(app, settings.value.autoRefresh)
    }
}
