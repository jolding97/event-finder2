package com.eventradar

import android.app.Application
import com.eventradar.data.AppDatabase
import com.eventradar.data.EventRepository
import com.eventradar.data.SettingsStore
import com.eventradar.work.RefreshWorker

class EventRadarApp : Application() {
    lateinit var settings: SettingsStore
        private set
    lateinit var repository: EventRepository
        private set

    override fun onCreate() {
        super.onCreate()
        settings = SettingsStore(this)
        repository = EventRepository(AppDatabase.create(this), settings)
        RefreshWorker.schedule(this, settings.current.autoRefresh)
    }
}
