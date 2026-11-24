package com.pavlov.energymonitor.data

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** ПЗ 8: репозиторій тестується без Room — достатньо [FakeApplianceDao]. */
class ApplianceRepositoryTest {
    private val dao = FakeApplianceDao()
    private val repo = ApplianceRepository(dao)

    @Test fun ensureSeeded_fillsEmptyDatabaseOnce() = runBlocking {
        repo.ensureSeeded()
        val first = repo.snapshot()
        assertEquals(6, first.size)

        repo.ensureSeeded() // другий виклик нічого не додає
        assertEquals(6, repo.snapshot().size)
    }

    @Test fun add_thenFindAndSort() = runBlocking {
        repo.add("Чайник", 2000, 0.5)
        repo.add("Бойлер", 2000, 2.0)
        repo.add("Комп’ютер", 250, 7.0)

        val byName = repo.observeAppliances(ApplianceSort.NAME, onlyEnabled = false).first()
        assertEquals(listOf("Бойлер", "Комп’ютер", "Чайник"), byName.map { it.name })

        val byPower = repo.observeAppliances(ApplianceSort.POWER, onlyEnabled = false).first()
        assertEquals(2000, byPower.first().powerW)
    }

    @Test fun onlyEnabled_filtersDisabled() = runBlocking {
        val id = repo.add("Пральна машина", 500, 1.0)
        repo.setEnabled(id.toInt(), false)

        val all = repo.observeAppliances(ApplianceSort.NAME, onlyEnabled = false).first()
        val onlyEnabled = repo.observeAppliances(ApplianceSort.NAME, onlyEnabled = true).first()
        assertEquals(1, all.size)
        assertTrue(onlyEnabled.isEmpty())
    }

    @Test fun isNameTaken_isCaseInsensitiveAndIgnoresSelf() = runBlocking {
        val id = repo.add("Холодильник", 150, 8.0).toInt()

        assertTrue(repo.isNameTaken("холодильник", excludeId = null))
        assertFalse(repo.isNameTaken("холодильник", excludeId = id)) // редагуємо сам себе
        assertFalse(repo.isNameTaken("Телевізор", excludeId = null))
    }

    @Test fun update_changesStoredValues() = runBlocking {
        val id = repo.add("Обігрівач", 2000, 4.0).toInt()
        repo.update(id, "Обігрівач новий", 1800, 3.0)

        val updated = repo.find(id)
        assertEquals("Обігрівач новий", updated?.name)
        assertEquals(1800, updated?.powerW)
        assertEquals(3.0, updated?.hoursPerDay)
    }

    @Test fun remove_deletesAppliance() = runBlocking {
        val id = repo.add("Мікрохвильова піч", 1000, 0.5).toInt()
        repo.remove(id)
        assertEquals(null, repo.find(id))
    }

    @Test fun presets_areNotEmpty() {
        assertTrue(repo.presets().isNotEmpty())
    }
}
