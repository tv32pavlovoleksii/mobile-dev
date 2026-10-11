package com.pavlov.energymonitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.pavlov.energymonitor.navigation.EnergyNavHost
import com.pavlov.energymonitor.navigation.Screen
import com.pavlov.energymonitor.ui.theme.EnergyMonitorTheme
import com.pavlov.energymonitor.ui.theme.energyColors

/** Точка входу: Activity лише вмикає edge-to-edge і показує Compose-дерево. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EnergyMonitorTheme { EnergyApp() }
        }
    }
}

/**
 * Кореневий composable: нижня панель вкладок (головне меню, ПЗ 5) керує навігацією
 * через єдиний [androidx.navigation.NavController]. Натискання на вкладку, яка вже
 * відкрита, нічого не робить (`launchSingleTop`); перехід між вкладками зберігає й
 * відновлює стан кожної (`saveState`/`restoreState`) — офіційний патерн bottom-nav
 * у Jetpack Navigation Compose.
 */
@Composable
fun EnergyApp() {
    val navController = rememberNavController()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = backStackEntry?.destination
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f), tonalElevation = 0.dp) {
                Screen.Tab.all.forEach { tab ->
                    val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label, fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.energyColors.secondaryLabel,
                            unselectedTextColor = MaterialTheme.energyColors.secondaryLabel,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        ),
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding).imePadding()) {
            EnergyNavHost(navController)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EnergyAppPreview() {
    EnergyMonitorTheme { EnergyApp() }
}
