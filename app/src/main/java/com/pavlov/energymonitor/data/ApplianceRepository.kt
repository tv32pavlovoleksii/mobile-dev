package com.pavlov.energymonitor.data

import com.pavlov.energymonitor.data.local.ApplianceDao
import com.pavlov.energymonitor.data.local.ApplianceEntity
import com.pavlov.energymonitor.domain.Appliance
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Спосіб сортування списку приладів — обирається користувачем у `CalculatorScreen`. */
enum class ApplianceSort(val title: String) { NAME("За назвою"), POWER("За потужністю") }

/**
 * ПЗ 8 (просунутий рівень): репозиторій — єдина точка доступу до даних про прилади
 * для всього застосунку. Приховує від `ViewModel`/UI, звідки саме беруться дані:
 * [ApplianceDao] (локальна база Room, яку можна змінювати) і [PresetApplianceCatalog]
 * (довідкове джерело для підказок). Повертає доменні моделі [Appliance], а не Entity —
 * шар domain нічого не знає про Room.
 */
class ApplianceRepository(
    private val dao: ApplianceDao,
    private val catalog: PresetApplianceCatalog = PresetApplianceCatalog,
) {
    fun observeAppliances(sort: ApplianceSort, onlyEnabled: Boolean): Flow<List<Appliance>> {
        val rows = when {
            onlyEnabled && sort == ApplianceSort.NAME -> dao.observeEnabledByName()
            onlyEnabled && sort == ApplianceSort.POWER -> dao.observeEnabledByPower()
            sort == ApplianceSort.NAME -> dao.observeAllByName()
            else -> dao.observeAllByPower()
        }
        return rows.map { list -> list.map { it.toDomain() } }
    }

    fun presets(): List<AppliancePreset> = catalog.presets

    suspend fun find(id: Int): Appliance? = dao.findById(id)?.toDomain()

    suspend fun isNameTaken(name: String, excludeId: Int?): Boolean =
        dao.countByName(name.trim(), excludeId ?: -1) > 0

    suspend fun add(name: String, powerW: Int, hoursPerDay: Double, emoji: String = "🔌"): Long =
        dao.insert(ApplianceEntity(name = name.trim(), emoji = emoji, powerW = powerW, hoursPerDay = hoursPerDay))

    suspend fun update(id: Int, name: String, powerW: Int, hoursPerDay: Double, emoji: String? = null) {
        val current = dao.findById(id) ?: return
        dao.update(current.copy(name = name.trim(), powerW = powerW, hoursPerDay = hoursPerDay, emoji = emoji ?: current.emoji))
    }

    suspend fun setEnabled(id: Int, enabled: Boolean) = dao.setEnabled(id, enabled)
    suspend fun setHours(id: Int, hours: Double) = dao.setHours(id, hours)
    suspend fun remove(id: Int) = dao.deleteById(id)

    suspend fun ensureSeeded() {
        if (dao.count() > 0) return
        catalog.presets.take(6).forEach { p ->
            dao.insert(ApplianceEntity(name = p.name, emoji = p.emoji, powerW = p.powerW, hoursPerDay = p.hoursPerDay))
        }
    }

    /** Разовий знімок списку (використовується формою для звірки унікальності назви без підписки). */
    suspend fun snapshot(): List<Appliance> = observeAppliances(ApplianceSort.NAME, onlyEnabled = false).first()

    private fun ApplianceEntity.toDomain() = Appliance(id, name, emoji, powerW, hoursPerDay, quantity, enabled)
}
