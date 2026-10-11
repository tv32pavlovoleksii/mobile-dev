package com.pavlov.energymonitor

import android.app.Application
import com.pavlov.energymonitor.data.ApplianceRepository
import com.pavlov.energymonitor.data.AuthRepository
import com.pavlov.energymonitor.data.local.AppDatabase

/**
 * Контейнер залежностей застосунку (просте ручне DI без сторонніх бібліотек).
 * База даних і репозиторії створюються один раз на процес (ліниво, через `by lazy`)
 * і живуть, доки живе застосунок, — саме звідси `ViewModel`-фабрики (ПЗ 7) беруть
 * репозиторій для конструктора конкретної `ViewModel`.
 */
class EnergyMonitorApp : Application() {
    private val database by lazy { AppDatabase.getInstance(this) }

    val applianceRepository by lazy { ApplianceRepository(database.applianceDao()) }
    val authRepository by lazy { AuthRepository() }
}
