package com.pavlov.energymonitor.viewmodel

import com.pavlov.energymonitor.data.NetworkNodeRepository
import com.pavlov.energymonitor.domain.NodeType
import com.pavlov.energymonitor.domain.SortOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

/**
 * ПЗ 14/15/16: пошук, фільтрація, сортування та синхронізація вибору (список ↔ мапа).
 * `_query`/`_typeFilter`/... — звичайні `MutableStateFlow`, тому значення, виставлене до
 * `collect`, одразу видно комбінованому потоку — досить одного `first()` без предикатів.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NodesViewModelTest {
    @Before fun setup() = Dispatchers.setMain(StandardTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private fun newViewModel() = NodesViewModel(NetworkNodeRepository(Random(1)))

    @Test fun visibleNodes_initiallyContainsAll() = runTest {
        val vm = newViewModel()
        assertEquals(vm.allNodes.size, vm.visibleNodes.first().size)
    }

    @Test fun query_filtersByName() = runTest {
        val vm = newViewModel()
        val target = vm.allNodes.first()
        val needle = target.name.take(4)
        vm.onQueryChange(needle)
        // `visibleNodes` — `SharingStarted.WhileSubscribed`: перше `collect` одразу бачить
        // початкове (ще нефільтроване) значення, тому чекаємо саме відфільтрований результат.
        val visible = vm.visibleNodes.first { list -> list.all { it.name.contains(needle, ignoreCase = true) } }
        assertTrue(visible.isNotEmpty())
    }

    @Test fun typeFilter_keepsOnlyMatchingType() = runTest {
        val vm = newViewModel()
        vm.onTypeFilterChange(NodeType.SOLAR)
        val visible = vm.visibleNodes.first { list -> list.isNotEmpty() && list.all { it.type == NodeType.SOLAR } }
        assertTrue(visible.all { it.type == NodeType.SOLAR })
    }

    @Test fun statusAndTypeFilter_combineWithAndLogic() = runTest {
        val vm = newViewModel()
        vm.onTypeFilterChange(NodeType.RESIDENTIAL)
        vm.onStatusFilterChange(com.pavlov.energymonitor.domain.NodeStatus.ONLINE)
        val visible = vm.visibleNodes.first { list ->
            list.all { it.type == NodeType.RESIDENTIAL && it.status == com.pavlov.energymonitor.domain.NodeStatus.ONLINE }
        }
        assertTrue(visible.isNotEmpty())
    }

    @Test fun sortByPower_ordersDescending() = runTest {
        val vm = newViewModel()
        vm.onSortChange(SortOption.POWER)
        val visible = vm.visibleNodes.first { list -> list == list.sortedByDescending { it.powerW } }
        val powers = visible.map { it.powerW }
        assertEquals(powers.sortedDescending(), powers)
    }

    @Test fun select_updatesSelectedNode() = runTest {
        val vm = newViewModel()
        val target = vm.allNodes[2]
        vm.select(target.id)
        val selected = vm.selectedNode.first { it != null }
        assertEquals(target, selected)
    }
}
