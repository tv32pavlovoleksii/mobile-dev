package com.pavlov.energymonitor.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector
import com.pavlov.energymonitor.viewmodel.NEW_APPLIANCE_ID

/**
 * ПЗ 5: усі маршрути застосунку в одному місці — sealed-ієрархія замість "магічних" рядків.
 * [Tab] — чотири верхньорівневі екрани з нижньої панелі (головне меню застосунку);
 * [ApplianceFormDestination] — другорядний екран, на який переходять з [Tab.Calculator]
 * і якому через аргумент маршруту передають id приладу (NEW_APPLIANCE_ID — для «додати»).
 */
sealed class Screen(val route: String) {
    sealed class Tab(route: String, val label: String, val icon: ImageVector) : Screen(route) {
        data object Monitor : Tab("monitor", "Моніторинг", Icons.Default.Home)       // ПЗ 1
        data object Calculator : Tab("calculator", "Калькулятор", Icons.Default.Build) // ПЗ 2, 6, 8
        data object Modes : Tab("modes", "Режими", Icons.Default.Star)               // ПЗ 3
        data object Account : Tab("account", "Акаунт", Icons.Default.Person)         // ПЗ 4, 7
        data object Network : Tab("network", "Мережа", Icons.Default.LocationOn)     // ПЗ 13-16

        companion object {
            val all = listOf(Monitor, Calculator, Modes, Account, Network)
        }
    }

    data object ApplianceForm : Screen("appliance_form?id={id}") {
        const val ARG_ID = "id"

        /** Будує конкретний маршрут: без id — нова форма, з id — редагування наявного приладу. */
        fun routeFor(id: Int = NEW_APPLIANCE_ID) = "appliance_form?id=$id"
    }

    /** ПЗ 14: детальний екран вузла мережі, відкривається з картки у списку. */
    data object NetworkDetail : Screen("network_detail/{id}") {
        const val ARG_ID = "id"
        fun routeFor(id: Int) = "network_detail/$id"
    }

    /** ПЗ 16: мапа з маркерами вузлів і GPS-позицією користувача. */
    data object NetworkMap : Screen("network_map")
}
