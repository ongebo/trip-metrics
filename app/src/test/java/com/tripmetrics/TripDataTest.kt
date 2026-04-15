package com.tripmetrics

import org.junit.Assert.assertEquals
import org.junit.Test

class TripDataTest {

    // ── formatDuration ────────────────────────────────────────────────────────

    @Test
    fun `formatDuration zero seconds`() {
        assertEquals("00:00", formatDuration(0L))
    }

    @Test
    fun `formatDuration under one minute`() {
        assertEquals("00:45", formatDuration(45L))
    }

    @Test
    fun `formatDuration exactly one minute`() {
        assertEquals("01:00", formatDuration(60L))
    }

    @Test
    fun `formatDuration minutes and seconds`() {
        assertEquals("12:34", formatDuration(754L)) // 12*60 + 34 = 754
    }

    @Test
    fun `formatDuration over one hour includes hours component`() {
        assertEquals("01:00:00", formatDuration(3600L))
    }

    @Test
    fun `formatDuration two hours fifteen minutes thirty seconds`() {
        assertEquals("02:15:30", formatDuration(2L * 3600 + 15 * 60 + 30))
    }

    // ── formatSpeed ───────────────────────────────────────────────────────────

    @Test
    fun `formatSpeed zero`() {
        assertEquals("0.0 km/h", formatSpeed(0f))
    }

    @Test
    fun `formatSpeed rounds to one decimal`() {
        assertEquals("65.4 km/h", formatSpeed(65.4f))
    }

    @Test
    fun `formatSpeed large value`() {
        assertEquals("120.0 km/h", formatSpeed(120.0f))
    }

    // ── formatDistance ────────────────────────────────────────────────────────

    @Test
    fun `formatDistance below threshold shows metres`() {
        assertEquals("500 m", formatDistance(500.0))
    }

    @Test
    fun `formatDistance exactly 1000m shows kilometres`() {
        assertEquals("1.00 km", formatDistance(1_000.0))
    }

    @Test
    fun `formatDistance above threshold shows kilometres with two decimals`() {
        assertEquals("2.50 km", formatDistance(2_500.0))
    }

    // ── TripMetrics defaults ──────────────────────────────────────────────────

    @Test
    fun `TripMetrics defaults to not running`() {
        val metrics = TripMetrics()
        assertEquals(false, metrics.isRunning)
        assertEquals(0f, metrics.currentSpeedKmh, 0.001f)
        assertEquals(0f, metrics.averageSpeedKmh, 0.001f)
        assertEquals(0L, metrics.durationSeconds)
        assertEquals(0.0, metrics.distanceMeters, 0.001)
        assertEquals(0f, metrics.maxSpeedKmh, 0.001f)
    }

    @Test
    fun `TripRecord stores all fields correctly`() {
        val record = TripRecord(
            id = "test-id",
            startTimeMs = 1_000L,
            endTimeMs = 61_000L,
            durationSeconds = 60L,
            distanceMeters = 1_000.0,
            averageSpeedKmh = 60.0f,
            maxSpeedKmh = 80.0f
        )
        assertEquals("test-id", record.id)
        assertEquals(1_000L, record.startTimeMs)
        assertEquals(61_000L, record.endTimeMs)
        assertEquals(60L, record.durationSeconds)
        assertEquals(1_000.0, record.distanceMeters, 0.001)
        assertEquals(60.0f, record.averageSpeedKmh, 0.001f)
        assertEquals(80.0f, record.maxSpeedKmh, 0.001f)
    }
}
