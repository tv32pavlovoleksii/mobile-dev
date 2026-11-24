package com.pavlov.energymonitor.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * ПЗ 8: DAO — єдина точка SQL-запитів до таблиці "appliances".
 * Запити, що повертають [Flow], автоматично переоновлюються при зміні таблиці —
 * саме завдяки цьому UI реагує на запис у базу без ручного оновлення.
 */
@Dao
interface ApplianceDao {

    /** Усі прилади, відсортовані за назвою (базовий рівень: вставка + читання). */
    @Query("SELECT * FROM appliances ORDER BY name COLLATE NOCASE ASC")
    fun observeAllByName(): Flow<List<ApplianceEntity>>

    /** Ті самі дані, відсортовані за потужністю (середній рівень: кілька способів читання). */
    @Query("SELECT * FROM appliances ORDER BY powerW DESC")
    fun observeAllByPower(): Flow<List<ApplianceEntity>>

    /** Лише ввімкнені прилади — фільтрація засобами SQL, а не в Kotlin-коді (просунутий рівень). */
    @Query("SELECT * FROM appliances WHERE enabled = 1 ORDER BY name COLLATE NOCASE ASC")
    fun observeEnabledByName(): Flow<List<ApplianceEntity>>

    @Query("SELECT * FROM appliances WHERE enabled = 1 ORDER BY powerW DESC")
    fun observeEnabledByPower(): Flow<List<ApplianceEntity>>

    @Query("SELECT * FROM appliances WHERE id = :id LIMIT 1")
    suspend fun findById(id: Int): ApplianceEntity?

    /** Перевірка унікальності назви (без урахування регістру), потрібна формі додавання (ПЗ 6). */
    @Query("SELECT COUNT(*) FROM appliances WHERE name = :name COLLATE NOCASE AND id != :excludeId")
    suspend fun countByName(name: String, excludeId: Int): Int

    @Query("SELECT COUNT(*) FROM appliances")
    suspend fun count(): Int

    @Insert
    suspend fun insert(entity: ApplianceEntity): Long

    @Update
    suspend fun update(entity: ApplianceEntity)

    @Query("UPDATE appliances SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Int, enabled: Boolean)

    @Query("UPDATE appliances SET hoursPerDay = :hours WHERE id = :id")
    suspend fun setHours(id: Int, hours: Double)

    @Delete
    suspend fun delete(entity: ApplianceEntity)

    @Query("DELETE FROM appliances WHERE id = :id")
    suspend fun deleteById(id: Int)
}
