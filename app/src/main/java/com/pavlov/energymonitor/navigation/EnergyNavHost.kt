package com.pavlov.energymonitor.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.pavlov.energymonitor.ui.screens.ApplianceFormScreen
import com.pavlov.energymonitor.ui.screens.AuthScreen
import com.pavlov.energymonitor.ui.screens.CalculatorScreen
import com.pavlov.energymonitor.ui.screens.ComponentsScreen
import com.pavlov.energymonitor.ui.screens.MonitorScreen
import com.pavlov.energymonitor.viewmodel.ApplianceFormViewModel
import com.pavlov.energymonitor.viewmodel.AuthViewModel
import com.pavlov.energymonitor.viewmodel.CalculatorViewModel
import com.pavlov.energymonitor.viewmodel.MonitorViewModel
import com.pavlov.energymonitor.viewmodel.NEW_APPLIANCE_ID
import com.pavlov.energymonitor.viewmodel.SimpleViewModelFactory
import com.pavlov.energymonitor.viewmodel.rememberAppContainer

/**
 * ПЗ 5: навігаційний граф застосунку. [NavHost] пов’язує маршрути з [Routes.kt] та
 * composable-екранами; кожен екран отримує свою `ViewModel` через [androidx.lifecycle.viewmodel.compose.viewModel]
 * з фабрикою (ПЗ 7) — так `ViewModel` переживає рекомпозицію й поворот екрана, але
 * знищується, щойно користувач остаточно покидає маршрут (NavController сам це контролює).
 */
@Composable
fun EnergyNavHost(navController: NavHostController) {
    val app = rememberAppContainer()
    val screenModifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)

    NavHost(navController = navController, startDestination = Screen.Tab.Monitor.route) {
        composable(Screen.Tab.Monitor.route) {
            val vm: MonitorViewModel = viewModel()
            MonitorScreen(vm, screenModifier)
        }

        composable(Screen.Tab.Calculator.route) {
            val vm: CalculatorViewModel = viewModel(
                factory = SimpleViewModelFactory { CalculatorViewModel(app.applianceRepository) },
            )
            CalculatorScreen(
                vm = vm,
                onAddAppliance = { navController.navigate(Screen.ApplianceForm.routeFor()) },
                onEditAppliance = { id -> navController.navigate(Screen.ApplianceForm.routeFor(id)) },
                modifier = screenModifier,
            )
        }

        composable(Screen.Tab.Modes.route) {
            ComponentsScreen(screenModifier)
        }

        composable(Screen.Tab.Account.route) {
            val vm: AuthViewModel = viewModel(factory = SimpleViewModelFactory { AuthViewModel(app.authRepository) })
            AuthScreen(vm, screenModifier)
        }

        composable(
            route = Screen.ApplianceForm.route,
            arguments = listOf(navArgument(Screen.ApplianceForm.ARG_ID) {
                type = NavType.IntType
                defaultValue = NEW_APPLIANCE_ID
            }),
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt(Screen.ApplianceForm.ARG_ID) ?: NEW_APPLIANCE_ID
            val vm: ApplianceFormViewModel = viewModel(
                factory = SimpleViewModelFactory { ApplianceFormViewModel(app.applianceRepository, id) },
            )
            ApplianceFormScreen(vm, onSaved = { navController.popBackStack() }, modifier = screenModifier)
        }
    }
}
