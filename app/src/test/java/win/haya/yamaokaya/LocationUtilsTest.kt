package win.haya.yamaokaya

import org.junit.Assert.assertEquals
import org.junit.Test

class LocationUtilsTest {

    @Test
    fun calculateDistanceMeters_returnsApproximateDistance() {
        // 東京駅から新宿駅のおよその距離
        val from = Coordinates(35.681236, 139.767125) // 東京駅
        val to = Coordinates(35.689634, 139.700646)   // 新宿駅
        val distance = calculateDistanceMeters(from, to)

        // およそ 6.2 km
        assertEquals(6200f, distance, 200f)
    }

    @Test
    fun calculateDistanceMeters_samePoint_returnsZero() {
        val point = Coordinates(35.681236, 139.767125)
        val distance = calculateDistanceMeters(point, point)

        assertEquals(0f, distance, 0.1f)
    }

    @Test
    fun calculateBearing_northIsZero() {
        val from = Coordinates(35.0, 139.0)
        val to = Coordinates(36.0, 139.0)
        val bearing = calculateBearing(from, to)

        assertEquals(0.0, bearing, 0.1)
    }

    @Test
    fun calculateBearing_eastIs90() {
        val from = Coordinates(0.0, 139.0)
        val to = Coordinates(0.0, 140.0)
        val bearing = calculateBearing(from, to)

        assertEquals(90.0, bearing, 0.1)
    }

    @Test
    fun normalizeDegrees_positiveValue_wrapsAround() {
        assertEquals(10f, normalizeDegrees(370f), 0.001f)
    }

    @Test
    fun normalizeDegrees_negativeValue_wrapsAround() {
        assertEquals(350f, normalizeDegrees(-10f), 0.001f)
    }

    @Test
    fun normalizeDegrees_zero_returnsZero() {
        assertEquals(0f, normalizeDegrees(0f), 0.001f)
    }
}
