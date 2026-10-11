package com.pavlov.energymonitor.data

/** Один запис каталогу типових приладів: назва, піктограма та орієнтовні параметри. */
data class AppliancePreset(val name: String, val emoji: String, val powerW: Int, val hoursPerDay: Double)

/**
 * ПЗ 8 (середній рівень — "кілька джерел даних"): друге, довідкове джерело даних.
 * На відміну від [com.pavlov.energymonitor.data.local.ApplianceDao] (локальна таблиця,
 * яку користувач редагує), каталог — незмінний і в реальному застосунку завантажувався б
 * з сервера. [ApplianceRepository] об’єднує обидва джерела: каталог — для підказок у формі
 * додавання (ПЗ 6), Room — для збереження того, що користувач обрав чи ввів сам.
 */
object PresetApplianceCatalog {
    val presets: List<AppliancePreset> = listOf(
        AppliancePreset("Холодильник", "🧊", 150, 8.0),
        AppliancePreset("Кондиціонер", "❄️", 1200, 5.0),
        AppliancePreset("Пральна машина", "🧺", 500, 1.0),
        AppliancePreset("Посудомийна машина", "🍽️", 1400, 1.0),
        AppliancePreset("Освітлення LED", "💡", 60, 6.0),
        AppliancePreset("Комп’ютер", "💻", 250, 7.0),
        AppliancePreset("Телевізор", "📺", 120, 4.0),
        AppliancePreset("Чайник", "☕", 2000, 0.5),
        AppliancePreset("Мікрохвильова піч", "🍲", 1000, 0.5),
        AppliancePreset("Бойлер", "🚿", 2000, 2.0),
        AppliancePreset("Зарядка електромобіля", "🔌", 7000, 3.0),
        AppliancePreset("Обігрівач", "🔥", 2000, 4.0),
    )
}
