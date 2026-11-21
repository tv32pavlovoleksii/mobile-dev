package com.pavlov.energymonitor.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.pavlov.energymonitor.EnergyMonitorApp

/**
 * ПЗ 7 (MVVM): універсальна фабрика `ViewModel`. Android створює `ViewModel`
 * сам (щоб керувати її життєвим циклом), тому конструктор з параметрами
 * (репозиторій, id приладу) потребує саме фабрики — [creator] лише викликається
 * в потрібний момент.
 */
class SimpleViewModelFactory(private val creator: () -> ViewModel) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = creator() as T
}

/** Доступ до контейнера залежностей застосунку (репозиторії) з будь-якого composable. */
@Composable
fun rememberAppContainer(): EnergyMonitorApp = LocalContext.current.applicationContext as EnergyMonitorApp
