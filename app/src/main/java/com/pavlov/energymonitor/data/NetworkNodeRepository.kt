package com.pavlov.energymonitor.data

import com.pavlov.energymonitor.domain.NetworkNode
import com.pavlov.energymonitor.domain.NodeStatus
import com.pavlov.energymonitor.domain.NodeType
import java.util.Date
import kotlin.random.Random

/**
 * ПЗ 14: джерело даних для списку й мапи — вузли мережі моніторингу енергоспоживання
 * (будинки, підприємства, сонячні панелі, підстанції, накопичувачі) навколо Києва.
 * Дані згенеровані один раз при старті застосунку (імітаційне джерело, без мережі).
 */
class NetworkNodeRepository(random: Random = Random.Default) {
    private val _nodes: List<NetworkNode> = generate(random)
    val nodes: List<NetworkNode> get() = _nodes

    fun findById(id: Int): NetworkNode? = _nodes.firstOrNull { it.id == id }

    /**
     * Назва вузла натякає на реальний район Києва (Позняки, Троєщина, Оболонь...),
     * тому координати прив'язані саме до цього району — лише з невеликим джиттером,
     * а не розкидані рівномірно по всьому місту (інакше «СЕС «Позняки»» могла б
     * опинитись біля Арсенальної, що виглядає як помилка на мапі).
     */
    private fun generate(random: Random): List<NetworkNode> {
        val entries = listOf(
            Entry("ЖК «Либідський»", NodeType.RESIDENTIAL, 50.4280, 30.4890),
            Entry("ЖК «Соняшник»", NodeType.RESIDENTIAL, 50.5090, 30.6080),
            Entry("Завод «Арсенал-Техно»", NodeType.INDUSTRIAL, 50.4404, 30.5410),
            Entry("Хлібокомбінат № 3", NodeType.INDUSTRIAL, 50.4830, 30.5000),
            Entry("СЕС «Позняки»", NodeType.SOLAR, 50.3960, 30.6160),
            Entry("СЕС «Троєщина»", NodeType.SOLAR, 50.5230, 30.6330),
            Entry("СЕС «Осокорки»", NodeType.SOLAR, 50.3910, 30.6150),
            Entry("ПС «Київська-110»", NodeType.TRANSFORMER, 50.5300, 30.6450),
            Entry("ПС «Дарницька-35»", NodeType.TRANSFORMER, 50.4200, 30.6050),
            Entry("ПС «Подільська»", NodeType.TRANSFORMER, 50.4680, 30.5150),
            Entry("Накопичувач «Оболонь»", NodeType.BATTERY, 50.5020, 30.4980),
            Entry("Накопичувач «Печерськ»", NodeType.BATTERY, 50.4320, 30.5400),
            Entry("ЖК «Атмосфера»", NodeType.RESIDENTIAL, 50.4820, 30.6160),
            Entry("Завод «Електровимірювач»", NodeType.INDUSTRIAL, 50.4320, 30.4700),
            Entry("СЕС «Виноградар»", NodeType.SOLAR, 50.5120, 30.3950),
            Entry("ПС «Святошинська»", NodeType.TRANSFORMER, 50.4560, 30.3710),
            Entry("ЖК «Паркленд»", NodeType.RESIDENTIAL, 50.3960, 30.6330),
            Entry("Накопичувач «Голосіїво»", NodeType.BATTERY, 50.3830, 30.4980),
        )
        return entries.mapIndexed { index, entry ->
            val basePower = when (entry.type) {
                NodeType.RESIDENTIAL -> 8_000
                NodeType.INDUSTRIAL -> 60_000
                NodeType.SOLAR -> 15_000
                NodeType.TRANSFORMER -> 120_000
                NodeType.BATTERY -> 25_000
            }
            val status = when {
                index % 7 == 0 -> NodeStatus.OFFLINE
                index % 5 == 0 -> NodeStatus.WARNING
                else -> NodeStatus.ONLINE
            }
            NetworkNode(
                id = index + 1,
                name = entry.name,
                type = entry.type,
                status = status,
                powerW = (basePower * (0.6 + random.nextDouble() * 0.8)).toInt(),
                lastUpdated = Date(System.currentTimeMillis() - random.nextLong(0, 3_600_000L)),
                lat = entry.lat + random.nextDouble(-0.004, 0.004),
                lon = entry.lon + random.nextDouble(-0.004, 0.004),
                description = describeNode(entry.name, entry.type, status),
            )
        }
    }

    private data class Entry(val name: String, val type: NodeType, val lat: Double, val lon: Double)

    private fun describeNode(name: String, type: NodeType, status: NodeStatus): String = when (type) {
        NodeType.RESIDENTIAL -> "Загальний лічильник будинку $name, сумарне навантаження квартир."
        NodeType.INDUSTRIAL -> "Промисловий ввід підприємства $name, облік за основною та реактивною потужністю."
        NodeType.SOLAR -> "Сонячна панель $name, генерація залежить від освітленості та часу доби."
        NodeType.TRANSFORMER -> "Трансформаторна підстанція $name, розподіляє навантаження району."
        NodeType.BATTERY -> "Накопичувач енергії $name, згладжує пікові навантаження мережі."
    }.plus(if (status == NodeStatus.OFFLINE) " Зв'язок із вузлом тимчасово втрачено." else "")
}
