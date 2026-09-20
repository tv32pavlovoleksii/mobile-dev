package com.pavlov.energymonitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pavlov.energymonitor.ui.screens.AuthFormState
import com.pavlov.energymonitor.ui.screens.AuthScreen
import com.pavlov.energymonitor.ui.screens.CalculatorScreen
import com.pavlov.energymonitor.ui.screens.ComponentsScreen
import com.pavlov.energymonitor.ui.screens.MonitorScreen
import com.pavlov.energymonitor.ui.screens.defaultAppliances
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

/** Чотири вкладки = чотири практичні заняття. */
private enum class Tab(val label: String, val icon: ImageVector) {
    MONITOR("Моніторинг", Icons.Default.Home),      // ПЗ 1
    CALCULATOR("Калькулятор", Icons.Default.Build), // ПЗ 2
    MODES("Режими", Icons.Default.Star),            // ПЗ 3
    ACCOUNT("Акаунт", Icons.Default.Person),        // ПЗ 4
}

/**
 * Кореневий composable: нижня панель вкладок у стилі iOS Tab Bar.
 * Стан, який не повинен губитись при зміні вкладки (список приладів, форма входу),
 * піднято сюди (state hoisting); решта зберігається через SaveableStateProvider.
 */
@Composable
fun EnergyApp() {
    var tabIndex by rememberSaveable { mutableIntStateOf(0) }
    val saveable = rememberSaveableStateHolder()
    val appliances = remember { mutableStateListOf(*defaultAppliances().toTypedArray()) }
    val authForm = remember { AuthFormState() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f), tonalElevation = 0.dp) {
                Tab.entries.forEachIndexed { i, tab ->
                    NavigationBarItem(
                        selected = i == tabIndex,
                        onClick = { tabIndex = i },
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
        Crossfade(tabIndex, label = "tab", modifier = Modifier.fillMaxSize()) { index ->
            saveable.SaveableStateProvider(index) {
                Box(Modifier.fillMaxSize().padding(innerPadding).imePadding()) {
                    val content = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
                    when (Tab.entries[index]) {
                        Tab.MONITOR -> MonitorScreen(content)
                        Tab.CALCULATOR -> CalculatorScreen(appliances, content)
                        Tab.MODES -> ComponentsScreen(content)
                        Tab.ACCOUNT -> AuthScreen(authForm, content)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EnergyAppPreview() {
    EnergyMonitorTheme { EnergyApp() }
}
