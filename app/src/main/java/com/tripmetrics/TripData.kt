package com.tripmetrics

import kotlin.math.roundToInt

/**
 * Live metrics emitted by TripService during an active trip.
 */
data class TripMetrics(
    val isRunning: Boolean = false,
    val currentSpeedKmh: Float = 0f,
    val averageSpeedKmh: Float = 0f,
    val durationSeconds: Long = 0L,
    val distanceMeters: Double = 0.0,
    val maxSpeedKmh: Float = 0f
)

/**
 * Immutable record of a completed trip, persisted to storage.
 */
data class TripRecord(
    val id: String,
    val name: String = "",
    val startTimeMs: Long,
    val endTimeMs: Long,
    val durationSeconds: Long,
    val distanceMeters: Double,
    val averageSpeedKmh: Float,
    val maxSpeedKmh: Float
)

/** Format a duration in seconds as [H:]MM:SS. */
fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%02d:%02d:%02d".format(h, m, s)
    else "%02d:%02d".format(m, s)
}

/** Format a speed value as "X.X km/h". */
fun formatSpeed(kmh: Float): String = "%.1f km/h".format(kmh)

/** Format a distance in metres, switching to kilometres above 1 000 m. */
fun formatDistance(meters: Double): String =
    if (meters >= 1_000) "%.2f km".format(meters / 1_000)
    else "%d m".format(meters.roundToInt())
