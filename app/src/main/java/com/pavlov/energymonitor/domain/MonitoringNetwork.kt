package com.pavlov.energymonitor.domain

import java.util.Date

/** Тип вузла мережі моніторингу енергоспоживання (ПЗ 14-16). */
enum class NodeType(val label: String) {
    RESIDENTIAL("Житловий будинок"),
    INDUSTRIAL("Промислове підприємство"),
    SOLAR("Сонячна панель"),
    TRANSFORMER("Трансформаторна підстанція"),
    BATTERY("Накопичувач енергії"),
}

/** Поточний стан вузла: чи надходять дані і чи немає перевантаження. */
enum class NodeStatus(val label: String) {
    ONLINE("У мережі"),
    WARNING("Перевантаження"),
    OFFLINE("Немає зв'язку"),
}

/** Критерій сортування списку вузлів (ПЗ 15). */
enum class SortOption(val label: String) {
    NAME("За назвою"),
    POWER("За потужністю"),
    UPDATED("За часом оновлення"),
}

/**
 * Один вузол мережі моніторингу: будинок, підприємство, сонячна панель тощо.
 * Географічні координати — навколо Києва, для відображення на мапі (ПЗ 16).
 */
data class NetworkNode(
    val id: Int,
    val name: String,
    val type: NodeType,
    val status: NodeStatus,
    val powerW: Int,
    val lastUpdated: Date,
    val lat: Double,
    val lon: Double,
    val description: String,
)
