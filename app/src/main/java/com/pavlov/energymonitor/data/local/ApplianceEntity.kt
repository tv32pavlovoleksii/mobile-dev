package com.pavlov.energymonitor.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * ПЗ 8: сутність Room — рядок таблиці "appliances" у локальній базі даних.
 * Дзеркалить [com.pavlov.energymonitor.domain.Appliance], але належить шару даних:
 * UI та бізнес-логіка (domain) ніколи не бачать Room-анотацій напряму.
 */
@Entity(tableName = "appliances")
data class ApplianceEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val emoji: String,
    val powerW: Int,
    val hoursPerDay: Double,
    val quantity: Int = 1,
    val enabled: Boolean = true,
)
