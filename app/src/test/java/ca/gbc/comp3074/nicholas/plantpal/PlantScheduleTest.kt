package ca.gbc.comp3074.nicholas.plantpal

import ca.gbc.comp3074.nicholas.plantpal.data.Plant
import ca.gbc.comp3074.nicholas.plantpal.util.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Checks the watering-date calculation behind the status badges (FR3). */
class PlantScheduleTest {

    private val now = System.currentTimeMillis()

    @Test
    fun wateredToday_isDueAfterInterval() {
        val plant = Plant(name = "Basil", intervalDays = 3, lastWatered = now)
        assertEquals(3, plant.daysUntilWatering(now))
        assertFalse(plant.isDue(now))
    }

    @Test
    fun wateredExactlyIntervalAgo_isDueToday() {
        val plant = Plant(name = "Monstera", intervalDays = 7, lastWatered = DateUtils.addDays(now, -7))
        assertEquals(0, plant.daysUntilWatering(now))
        assertTrue(plant.isDue(now))
    }

    @Test
    fun wateredLongAgo_isOverdue() {
        val plant = Plant(name = "Fern", intervalDays = 2, lastWatered = DateUtils.addDays(now, -5))
        assertEquals(-3, plant.daysUntilWatering(now))
        assertTrue(plant.isDue(now))
    }
}
