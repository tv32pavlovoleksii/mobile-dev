# практична робота №1

Варіант 15 - **Система моніторингу енергоспоживання**. Рівень складності: просунутий.

| Вкладка | Заняття | Що реалізовано |
|---|---|---|
| Моніторинг | ПЗ 1 | Анімоване кільце потужності, `animateIntAsState/FloatAsState/ColorAsState`, State, Material Theme, Column/Row |
| Калькулятор | ПЗ 2 | Розрахунок кВт·год / грн / CO₂, тарифи, список приладів, графіки Vico, логіка у `domain/` |
| Режими | ПЗ 3 | Text, Image, всі типи Button, Box-накладання, адаптивний layout, кілька composable |
| Акаунт | ПЗ 4 | Вхід/реєстрація, валідація, індикатор надійності пароля, стан завантаження, адаптивна форма |

## Структура

```
app/src/main/java/com/pavlov/energymonitor
├── MainActivity.kt          # точка входу, нижня панель вкладок
├── domain/                  # чиста логіка (без Android/Compose) — покрита юніт-тестами
│   ├── EnergyModels.kt, EnergyCalculator.kt, ConsumptionSimulator.kt, EnergyMode.kt
│   ├── ApplianceInputValidator.kt, AuthValidator.kt, AuthRepository.kt
└── ui/
    ├── theme/               # iOS-палітра, типографіка, форми
    ├── components/          # IosCard, SegmentedControl, IosTextField, ...
    └── screens/             # MonitorScreen, CalculatorScreen, ComponentsScreen, AuthScreen
```

Залежності керуються централізовано у `gradle/libs.versions.toml` (Compose BOM, Vico, coroutines).

## Запуск

```
./gradlew :app:testDebugUnitTest   # 17 юніт-тестів
./gradlew :app:assembleDebug       # app/build/outputs/apk/debug/app-debug.apk
```

Демо-акаунт для входу: `demo@energy.ua` / `Energy2026!`.
