package com.pavlov.energymonitor

import android.app.Application
import com.pavlov.energymonitor.data.ApplianceRepository
import com.pavlov.energymonitor.data.AuthRepository
import com.pavlov.energymonitor.data.MonitorRepository
import com.pavlov.energymonitor.data.NetworkNodeRepository
import com.pavlov.energymonitor.data.local.AppDatabase
import com.pavlov.energymonitor.data.remote.TokenStore
import com.pavlov.energymonitor.data.remote.createEnergyApi
import com.pavlov.energymonitor.domain.ConsumptionSimulator
import com.pavlov.energymonitor.viewmodel.NetworkSignalViewModel
import com.pavlov.energymonitor.viewmodel.NodesViewModel

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

    private val networkNodeRepository by lazy { NetworkNodeRepository() }

    /**
     * ПЗ 13-16: список/мапу і живий сигнал мережі тримаємо як застосунок-scoped синглтони
     * (як-от репозиторії вище), а не створюємо наново на кожному екрані — так вибір вузла
     * в списку лишається видимим і на мапі, а «гарячий» потік (ПЗ 13) не скидається при навігації.
     */
    val nodesViewModel by lazy { NodesViewModel(networkNodeRepository) }
    val networkSignalViewModel by lazy { NetworkSignalViewModel() }
}
