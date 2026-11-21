package com.pavlov.energymonitor.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * ПЗ 8: клас бази даних Room. Створюється один раз на процес (singleton через [getInstance]),
 * щоб усі репозиторії працювали з одним і тим самим з’єднанням SQLite.
 */
@Database(entities = [ApplianceEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun applianceDao(): ApplianceDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "energy_monitor.db",
                ).build().also { instance = it }
            }
    }
}
