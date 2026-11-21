package com.pavlov.energymonitor.viewmodel

import com.pavlov.energymonitor.data.ApplianceRepository
import com.pavlov.energymonitor.data.ApplianceSort
import com.pavlov.energymonitor.data.FakeApplianceDao
import com.pavlov.energymonitor.domain.TariffPlan
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

/**
 * ПЗ 7: `ViewModel` тестується окремо від UI — репозиторій підміняється [FakeApplianceDao],
 * `Dispatchers.Main` підміняється тестовим, і можна перевірити реактивну поведінку
 * (`combine`/`flatMapLatest`) так само, як звичайну функцію. Саме це мала на увазі
 * методичка, коли писала: "MVVM спрощує тестування та повторне використання компонентів".
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CalculatorViewModelTest {
    @Before fun setup() = Dispatchers.setMain(StandardTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private fun newViewModel() = CalculatorViewModel(ApplianceRepository(FakeApplianceDao()))

    @Test fun initialState_seedsSixAppliances() = runTest {
        val vm = newViewModel()
        val state = vm.uiState.first { it is CalculatorUiState.Success } as CalculatorUiState.Success
        assertEquals(6, state.appliances.size)
        assertEquals(TariffPlan.TWO_ZONE, state.plan) // planIndex = 1 за замовчуванням
    }

    @Test fun setSort_byPower_ordersDescending() = runTest {
        val vm = newViewModel()
        vm.uiState.first { it is CalculatorUiState.Success }

        vm.setSort(ApplianceSort.POWER)
        val sorted = vm.uiState.first { it is CalculatorUiState.Success && it.sort == ApplianceSort.POWER }
            as CalculatorUiState.Success
        val powers = sorted.appliances.map { it.powerW }
        assertEquals(powers.sortedDescending(), powers)
    }

    @Test fun setOnlyEnabled_hidesDisabledAppliances() = runTest {
        val vm = newViewModel()
        val initial = vm.uiState.first { it is CalculatorUiState.Success } as CalculatorUiState.Success
        val anyId = initial.appliances.first().id
        vm.toggle(anyId, false)

        vm.setOnlyEnabled(true)
        val filtered = vm.uiState.first { it is CalculatorUiState.Success && it.onlyEnabled }
            as CalculatorUiState.Success
        assertTrue(filtered.appliances.none { it.id == anyId })
        assertEquals(5, filtered.appliances.size)
    }

    @Test fun setPlanIndex_switchesTariff() = runTest {
        val vm = newViewModel()
        vm.uiState.first { it is CalculatorUiState.Success }

        vm.setPlanIndex(0)
        val single = vm.uiState.first { it is CalculatorUiState.Success && it.plan == TariffPlan.SINGLE }
            as CalculatorUiState.Success
        assertEquals(TariffPlan.SINGLE, single.plan)
    }
}
