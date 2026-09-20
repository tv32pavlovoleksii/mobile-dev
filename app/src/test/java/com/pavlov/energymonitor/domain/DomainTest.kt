package com.pavlov.energymonitor.domain

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class EnergyCalculatorTest {
    @Test fun dailyKwh_basic() = assertEquals(1.2, EnergyCalculator.dailyKwh(150, 8.0, 1), 1e-9)

    @Test fun dailyKwh_clampsInvalidInput() {
        assertEquals(0.0, EnergyCalculator.dailyKwh(-100, 5.0), 1e-9)
        assertEquals(2.4, EnergyCalculator.dailyKwh(100, 99.0), 1e-9) // 24 год максимум
    }

    @Test fun cost_singleZone() = assertEquals(43.2, EnergyCalculator.cost(10.0, TariffPlan.SINGLE, 0.5), 1e-9)

    @Test fun cost_twoZoneIsCheaperThanSingle() {
        val single = EnergyCalculator.cost(100.0, TariffPlan.SINGLE, 0.4)
        val two = EnergyCalculator.cost(100.0, TariffPlan.TWO_ZONE, 0.4)
        assertTrue(two < single)
    }

    @Test fun summarize_skipsDisabled() {
        val list = listOf(
            Appliance(1, "A", "", 1000, 1.0),
            Appliance(2, "B", "", 1000, 1.0, enabled = false),
        )
        val s = EnergyCalculator.summarize(list, TariffPlan.SINGLE, days = 10)
        assertEquals(1.0, s.dailyKwh, 1e-9)
        assertEquals(10.0, s.periodKwh, 1e-9)
        assertEquals(1, s.perApplianceKwh.size)
        assertEquals(10.0 * 0.36, s.co2Kg, 1e-9)
    }
}

class LoadLevelTest {
    @Test fun levels() {
        assertEquals(LoadLevel.LOW, LoadLevel.of(1000, 5000))
        assertEquals(LoadLevel.NORMAL, LoadLevel.of(2000, 5000))
        assertEquals(LoadLevel.HIGH, LoadLevel.of(3500, 5000))
        assertEquals(LoadLevel.CRITICAL, LoadLevel.of(4800, 5000))
    }
}

class ConsumptionSimulatorTest {
    @Test fun reading_isWithinBounds() {
        val sim = ConsumptionSimulator(Random(42))
        repeat(200) { i ->
            val r = sim.nextReading(i % 24)
            assertTrue(r.powerW in ConsumptionSimulator.MIN_POWER_W..(sim.limitW * 1.1).toInt())
            assertTrue(r.voltageV in 224..236)
        }
    }

    @Test fun consumedToday_isMonotonic() {
        val sim = ConsumptionSimulator(Random(1))
        assertTrue(sim.consumedTodayKwh(10, 0) < sim.consumedTodayKwh(10, 30))
        assertTrue(sim.consumedTodayKwh(10, 59) <= sim.consumedTodayKwh(11, 0) + 1e-9)
    }
}

class ModeForecastTest {
    private val profile = listOf(1000, 2000)
    @Test fun scalesWithFactor() {
        assertEquals(3.0, ModeForecast.dailyKwh(profile, EnergyMode.STANDARD), 1e-9)
        assertEquals(2.1, ModeForecast.dailyKwh(profile, EnergyMode.ECO), 1e-9)
        assertEquals(2700, ModeForecast.peakW(profile, EnergyMode.PEAK))
    }
}

class ApplianceInputValidatorTest {
    @Test fun power() {
        assertEquals(1500, ApplianceInputValidator.parsePower(" 1500 ").first)
        assertNotNull(ApplianceInputValidator.parsePower("abc").second)
        assertNotNull(ApplianceInputValidator.parsePower("0").second)
        assertNotNull(ApplianceInputValidator.parsePower("99999").second)
    }

    @Test fun hoursAcceptCommaAndDot() {
        assertEquals(2.5, ApplianceInputValidator.parseHours("2,5").first!!, 1e-9)
        assertEquals(2.5, ApplianceInputValidator.parseHours("2.5").first!!, 1e-9)
        assertNotNull(ApplianceInputValidator.parseHours("25").second)
    }
}

class AuthValidatorTest {
    @Test fun email() {
        assertNull(AuthValidator.validateEmail("user@example.com"))
        assertNotNull(AuthValidator.validateEmail("user@"))
        assertNotNull(AuthValidator.validateEmail(""))
    }

    @Test fun password() {
        assertNull(AuthValidator.validatePassword("Energy2026!"))
        assertNotNull(AuthValidator.validatePassword("short1"))
        assertNotNull(AuthValidator.validatePassword("onlyletters"))
        assertNotNull(AuthValidator.validatePassword("12345678"))
    }

    @Test fun confirm() {
        assertNull(AuthValidator.validateConfirm("a", "a"))
        assertNotNull(AuthValidator.validateConfirm("a", "b"))
    }

    @Test fun strength() {
        assertEquals(PasswordStrength.EMPTY, AuthValidator.strength(""))
        assertEquals(PasswordStrength.WEAK, AuthValidator.strength("abc"))
        assertEquals(PasswordStrength.STRONG, AuthValidator.strength("Energy2026!"))
    }
}

class AuthRepositoryTest {
    private val repo = AuthRepository(latencyMs = 0)

    @Test fun demoLogin() = runBlocking {
        assertTrue(repo.login("DEMO@energy.ua", "Energy2026!") is AuthResult.Success)
        assertTrue(repo.login("demo@energy.ua", "wrong") is AuthResult.Failure)
        assertTrue(repo.login("nobody@energy.ua", "x") is AuthResult.Failure)
    }

    @Test fun registerThenLogin_andDuplicateRejected() = runBlocking {
        assertTrue(repo.register("Олексій", "a@b.ua", "Pass1234") is AuthResult.Success)
        assertTrue(repo.register("Хтось", "A@b.ua", "Pass1234") is AuthResult.Failure)
        assertTrue(repo.login("a@b.ua", "Pass1234") is AuthResult.Success)
    }
}
