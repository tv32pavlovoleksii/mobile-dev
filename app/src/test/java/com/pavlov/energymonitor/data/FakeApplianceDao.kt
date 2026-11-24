package com.pavlov.energymonitor.data

import com.pavlov.energymonitor.data.local.ApplianceDao
import com.pavlov.energymonitor.data.local.ApplianceEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Тестовий дублер [ApplianceDao] — звичайний `MutableStateFlow` у пам’яті замість SQLite.
 * Дозволяє тестувати [ApplianceRepository] і `ViewModel`, які від нього залежать,
 * без Room/Robolectric чи реального пристрою — у цьому й сенс репозиторію в MVVM (ПЗ 7).
 */
class FakeApplianceDao : ApplianceDao {
    private val rows = MutableStateFlow<List<ApplianceEntity>>(emptyList())
    private var nextId = 1

    override fun observeAllByName(): Flow<List<ApplianceEntity>> =
        rows.map { it.sortedBy { e -> e.name.lowercase() } }

    override fun observeAllByPower(): Flow<List<ApplianceEntity>> =
        rows.map { it.sortedByDescending { e -> e.powerW } }

    override fun observeEnabledByName(): Flow<List<ApplianceEntity>> =
        rows.map { it.filter { e -> e.enabled }.sortedBy { e -> e.name.lowercase() } }

    override fun observeEnabledByPower(): Flow<List<ApplianceEntity>> =
        rows.map { it.filter { e -> e.enabled }.sortedByDescending { e -> e.powerW } }

    override suspend fun findById(id: Int): ApplianceEntity? = rows.value.find { it.id == id }

    override suspend fun countByName(name: String, excludeId: Int): Int =
        rows.value.count { it.name.equals(name, ignoreCase = true) && it.id != excludeId }

    override suspend fun count(): Int = rows.value.size

    override suspend fun insert(entity: ApplianceEntity): Long {
        val withId = entity.copy(id = nextId++)
        rows.value = rows.value + withId
        return withId.id.toLong()
    }

    override suspend fun update(entity: ApplianceEntity) {
        rows.value = rows.value.map { if (it.id == entity.id) entity else it }
    }

    override suspend fun setEnabled(id: Int, enabled: Boolean) {
        rows.value = rows.value.map { if (it.id == id) it.copy(enabled = enabled) else it }
    }

    override suspend fun setHours(id: Int, hours: Double) {
        rows.value = rows.value.map { if (it.id == id) it.copy(hoursPerDay = hours) else it }
    }

    override suspend fun delete(entity: ApplianceEntity) {
        rows.value = rows.value.filterNot { it.id == entity.id }
    }

    override suspend fun deleteById(id: Int) {
        rows.value = rows.value.filterNot { it.id == id }
    }
}
