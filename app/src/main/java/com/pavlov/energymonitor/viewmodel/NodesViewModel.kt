package com.pavlov.energymonitor.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pavlov.energymonitor.data.NetworkNodeRepository
import com.pavlov.energymonitor.domain.NetworkNode
import com.pavlov.energymonitor.domain.NodeStatus
import com.pavlov.energymonitor.domain.NodeType
import com.pavlov.energymonitor.domain.SortOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * ПЗ 14-16: список вузлів мережі (список + картки + мапа). Один спільний екземпляр на весь
 * застосунок (див. [com.pavlov.energymonitor.EnergyMonitorApp]), тому вибір вузла в списку
 * (ПЗ 14) лишається синхронізованим з екраном мапи (ПЗ 16) — обидва читають [selectedId].
 */
class NodesViewModel(private val repository: NetworkNodeRepository) : ViewModel() {
    val allNodes: List<NetworkNode> get() = repository.nodes

    private val _query = MutableStateFlow("")
    private val _typeFilter = MutableStateFlow<NodeType?>(null)
    private val _statusFilter = MutableStateFlow<NodeStatus?>(null)
    private val _sortOption = MutableStateFlow(SortOption.NAME)
    private val _selectedId = MutableStateFlow<Int?>(null)

    val query: StateFlow<String> = _query.asStateFlow()
    val typeFilter: StateFlow<NodeType?> = _typeFilter.asStateFlow()
    val statusFilter: StateFlow<NodeStatus?> = _statusFilter.asStateFlow()
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()
    val selectedId: StateFlow<Int?> = _selectedId.asStateFlow()

    /** ПЗ 15: пошук за назвою + багаторівнева фільтрація (тип, статус) + сортування — одним потоком. */
    val visibleNodes: StateFlow<List<NetworkNode>> =
        combine(_query, _typeFilter, _statusFilter, _sortOption) { query, type, status, sort ->
            allNodes
                .filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
                .filter { type == null || it.type == type }
                .filter { status == null || it.status == status }
                .sortedWith(
                    when (sort) {
                        SortOption.NAME -> compareBy { it.name }
                        SortOption.POWER -> compareByDescending { it.powerW }
                        SortOption.UPDATED -> compareByDescending { it.lastUpdated }
                    },
                )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), allNodes)

    /** ПЗ 16: вузол, обраний або у списку, або натисканням маркера на мапі. */
    val selectedNode: StateFlow<NetworkNode?> = _selectedId
        .map { id -> id?.let(repository::findById) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun onQueryChange(value: String) { _query.value = value }
    fun onTypeFilterChange(value: NodeType?) { _typeFilter.value = value }
    fun onStatusFilterChange(value: NodeStatus?) { _statusFilter.value = value }
    fun onSortChange(value: SortOption) { _sortOption.value = value }
    fun select(id: Int?) { _selectedId.value = id }
}
