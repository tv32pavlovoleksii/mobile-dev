package com.pavlov.energymonitor

import android.app.Application
import com.pavlov.energymonitor.data.ApplianceRepository
import com.pavlov.energymonitor.data.AuthRepository
import com.pavlov.energymonitor.data.MonitorRepository
import com.pavlov.energymonitor.data.local.AppDatabase
import com.pavlov.energymonitor.data.remote.TokenStore
import com.pavlov.energymonitor.data.remote.createEnergyApi
import com.pavlov.energymonitor.domain.ConsumptionSimulator

/**
 * Контейнер залежностей застосунку (просте ручне DI без сторонніх бібліотек).
 * База даних і репозиторії створюються один раз на процес (ліниво, через `by lazy`)
 * і живуть, доки живе застосунок, — саме звідси `ViewModel`-фабрики (ПЗ 7) беруть
 * репозиторій для конструктора конкретної `ViewModel`.
 */
class EnergyMonitorApp : Application() {
    private val database by lazy { AppDatabase.getInstance(this) }
    private val tokenStore by lazy { TokenStore(this) }
    private val energyApi by lazy { createEnergyApi(tokenStore) }

    val applianceRepository by lazy { ApplianceRepository(database.applianceDao()) }
    val authRepository by lazy { AuthRepository(energyApi, tokenStore) }
    val monitorRepository by lazy { MonitorRepository(ConsumptionSimulator(), energyApi) }
}
