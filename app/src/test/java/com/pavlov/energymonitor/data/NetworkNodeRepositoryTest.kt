package com.pavlov.energymonitor.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** ПЗ 14: джерело даних для списку й мапи — перевіряємо лише інваріанти, не конкретні значення. */
class NetworkNodeRepositoryTest {
    @Test fun nodes_haveUniqueIds() {
        val repo = NetworkNodeRepository(Random(1))
        val ids = repo.nodes.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test fun nodes_areScatteredAroundKyiv() {
        val repo = NetworkNodeRepository(Random(2))
        repo.nodes.forEach { node ->
            assertTrue(node.lat in 50.3..50.6)
            assertTrue(node.lon in 30.3..30.7)
            assertTrue(node.powerW > 0)
        }
    }

    @Test fun findById_returnsMatchingNode() {
        val repo = NetworkNodeRepository(Random(3))
        val first = repo.nodes.first()
        assertEquals(first, repo.findById(first.id))
        assertNull(repo.findById(-1))
    }
}
