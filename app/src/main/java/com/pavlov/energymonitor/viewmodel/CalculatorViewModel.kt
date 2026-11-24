package com.pavlov.energymonitor.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pavlov.energymonitor.data.ApplianceRepository
import com.pavlov.energymonitor.data.ApplianceSort
import com.pavlov.energymonitor.domain.Appliance
import com.pavlov.energymonitor.domain.EnergyCalculator
import com.pavlov.energymonitor.domain.EnergySummary
import com.pavlov.energymonitor.domain.TariffPlan
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Налаштування екрана, які впливають на те, що саме показувати (не самі дані). */
private data class CalculatorFilters(
    val sort: ApplianceSort = ApplianceSort.NAME,
    val onlyEnabled: Boolean = false,
    val planIndex: Int = 1,
    val nightShare: Float = 0.35f,
)

/**
 * ПЗ 7 (просунутий рівень): «реактивний» стан екрана — три можливі варіанти,
 * а не окремі булеві прапорці `isLoading`/`errorMessage`. UI завжди знає,
 * у якому саме стані перебуває, і не може показати їх одночасно.
 */
sealed interface CalculatorUiState {
    data object Loading : CalculatorUiState
    data class Success(
        val appliances: List<Appliance>,
        val summary: EnergySummary,
        val plan: TariffPlan,
        val sort: ApplianceSort,
        val onlyEnabled: Boolean,
        val nightShare: Float,
    ) : CalculatorUiState
    data class Error(val message: String) : CalculatorUiState
}

/**
 * ПЗ 7 (MVVM) + ПЗ 8 (Room): `ViewModel` калькулятора. У ПЗ 2 список приладів жив
 * у `mutableStateListOf` і зникав при видаленні застосунку з пам’яті. Тепер джерелом
 * істини є [ApplianceRepository] (Room), а `ViewModel` лише комбінує потік даних із
 * фільтрами (`sort`, `onlyEnabled`, тариф) і віддає екрану вже готовий [CalculatorUiState].
 * Усі формули, як і раніше, рахує `domain/EnergyCalculator` — `ViewModel` ним лише керує.
 */
class CalculatorViewModel(private val repository: ApplianceRepository) : ViewModel() {
    private val filters = MutableStateFlow(CalculatorFilters())

    /**
     * Repository-запит обирається заново лише тоді, коли реально змінились `sort`/`onlyEnabled`
     * (`flatMapLatest` скасовує попередню підписку на Room). Список іде в парі з фільтром,
     * що його породив, — інакше при швидкій зміні `sort` у `combine` нижче міг би на мить
     * з’явитися новий прапорець `sort` разом зі списком, відсортованим ще по-старому.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val appliancesByQuery: Flow<Pair<List<Appliance>, CalculatorFilters>> = filters
        .distinctUntilChanged { old, new -> old.sort == new.sort && old.onlyEnabled == new.onlyEnabled }
        .flatMapLatest { f -> repository.observeAppliances(f.sort, f.onlyEnabled).map { list -> list to f } }

    /** А підсумок (`summary`) перераховується при будь-якій зміні: і списку, і тарифу, і частки ночі. */
    val uiState: StateFlow<CalculatorUiState> = combine(appliancesByQuery, filters) { (list, query), latest ->
        val plan = TariffPlan.entries[latest.planIndex]
        val summary = EnergyCalculator.summarize(list, plan, latest.nightShare.toDouble())
        CalculatorUiState.Success(list, summary, plan, query.sort, query.onlyEnabled, latest.nightShare) as CalculatorUiState
    }
        .catch { e -> emit(CalculatorUiState.Error(e.message ?: "Не вдалося завантажити прилади")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalculatorUiState.Loading)

    init {
        // Асинхронне наповнення бази даними за замовчуванням при першому запуску (ПЗ 8).
        viewModelScope.launch { repository.ensureSeeded() }
    }

    fun setSort(sort: ApplianceSort) { filters.value = filters.value.copy(sort = sort) }
    fun setOnlyEnabled(value: Boolean) { filters.value = filters.value.copy(onlyEnabled = value) }
    fun setPlanIndex(index: Int) { filters.value = filters.value.copy(planIndex = index) }
    fun setNightShare(value: Float) { filters.value = filters.value.copy(nightShare = value) }

    fun toggle(id: Int, enabled: Boolean) = viewModelScope.launch { repository.setEnabled(id, enabled) }
    fun updateHours(id: Int, hours: Double) = viewModelScope.launch { repository.setHours(id, hours) }
    fun remove(id: Int) = viewModelScope.launch { repository.remove(id) }
}
