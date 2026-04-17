package com.tripmetrics

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Foreground service that acquires GPS updates via [FusedLocationProviderClient] and
 * continuously computes real-time trip metrics.
 *
 * State is exposed through companion-object [StateFlow]s so that both the phone
 * [MainActivity] and the Android Auto [TripScreen][com.tripmetrics.auto.TripScreen]
 * can observe the same source of truth without binding.
 */
class TripService : Service() {

    companion object {
        const val ACTION_START = "com.tripmetrics.ACTION_START"
        const val ACTION_STOP = "com.tripmetrics.ACTION_STOP"

        private const val CHANNEL_ID = "trip_service_channel"
        private const val NOTIFICATION_ID = 1

        private val _metrics = MutableStateFlow(TripMetrics())
        /** Live trip metrics, updated at every GPS fix while a trip is running. */
        val metrics: StateFlow<TripMetrics> = _metrics.asStateFlow()

        private val _lastSavedTrip = MutableStateFlow<TripRecord?>(null)
        /** Emits the most recently saved [TripRecord] after a trip is stopped. */
        val lastSavedTrip: StateFlow<TripRecord?> = _lastSavedTrip.asStateFlow()

        /** Convenience helper: send ACTION_START to the service. */
        fun start(context: Context) {
            context.startForegroundService(
                Intent(context, TripService::class.java).setAction(ACTION_START)
            )
        }

        /** Convenience helper: send ACTION_STOP to the service. */
        fun stop(context: Context) {
            context.startService(
                Intent(context, TripService::class.java).setAction(ACTION_STOP)
            )
        }

        /** Clears the last saved trip so the naming dialog doesn't reappear on config change. */
        fun clearLastSavedTrip() {
            _lastSavedTrip.value = null
        }
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var repository: TripRepository

    private var tripStartTimeMs: Long = 0L
    private var totalDistanceMeters: Double = 0.0
    private var lastLocation: Location? = null
    private var maxSpeedKmh: Float = 0f
    private val speedSamplesKmh = mutableListOf<Float>()

    // ---------------------------------------------------------------------------
    // Service lifecycle
    // ---------------------------------------------------------------------------

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        repository = TripRepository(this)
        createNotificationChannel()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.forEach { onNewLocation(it) }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startTrip()
            ACTION_STOP -> stopTrip()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ---------------------------------------------------------------------------
    // Trip control
    // ---------------------------------------------------------------------------

    private fun startTrip() {
        tripStartTimeMs = System.currentTimeMillis()
        totalDistanceMeters = 0.0
        lastLocation = null
        maxSpeedKmh = 0f
        speedSamplesKmh.clear()
        _metrics.value = TripMetrics(isRunning = true)

        startForeground(NOTIFICATION_ID, buildNotification("Trip started…"))
        requestLocationUpdates()
    }

    private fun stopTrip() {
        stopLocationUpdates()

        val current = _metrics.value
        if (current.isRunning) {
            val record = TripRecord(
                id = UUID.randomUUID().toString(),
                startTimeMs = tripStartTimeMs,
                endTimeMs = System.currentTimeMillis(),
                durationSeconds = current.durationSeconds,
                distanceMeters = totalDistanceMeters,
                averageSpeedKmh = current.averageSpeedKmh,
                maxSpeedKmh = maxSpeedKmh
            )
            repository.saveTrip(record)
            _lastSavedTrip.value = record
        }

        _metrics.value = TripMetrics(isRunning = false)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // ---------------------------------------------------------------------------
    // Location processing
    // ---------------------------------------------------------------------------

    private fun onNewLocation(location: Location) {
        val currentSpeedKmh = if (location.hasSpeed()) location.speed * 3.6f else 0f
        if (currentSpeedKmh > maxSpeedKmh) maxSpeedKmh = currentSpeedKmh
        speedSamplesKmh.add(currentSpeedKmh)

        lastLocation?.let { prev -> totalDistanceMeters += prev.distanceTo(location).toDouble() }
        lastLocation = location

        val durationSeconds = (System.currentTimeMillis() - tripStartTimeMs) / 1000L
        // Average speed = total distance (km) / total time (h)
        val averageSpeedKmh = if (durationSeconds > 0) {
            ((totalDistanceMeters / 1_000.0) / (durationSeconds / 3_600.0)).toFloat()
        } else 0f

        _metrics.value = TripMetrics(
            isRunning = true,
            currentSpeedKmh = currentSpeedKmh,
            averageSpeedKmh = averageSpeedKmh,
            durationSeconds = durationSeconds,
            distanceMeters = totalDistanceMeters,
            maxSpeedKmh = maxSpeedKmh
        )

        updateNotification(currentSpeedKmh, averageSpeedKmh)
    }

    @Throws(SecurityException::class)
    private fun requestLocationUpdates() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1_000L)
            // Filter out GPS jitter when stationary; 5 m threshold balances accuracy vs battery.
            .setMinUpdateDistanceMeters(5f)
            .build()
        fusedLocationClient.requestLocationUpdates(request, locationCallback, mainLooper)
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    // ---------------------------------------------------------------------------
    // Notification helpers
    // ---------------------------------------------------------------------------

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Trip Metrics",
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Shows current trip metrics" }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(text: String) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Trip Metrics")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_directions)
            .setOngoing(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()

    private fun updateNotification(currentKmh: Float, avgKmh: Float) {
        val text = "Now: ${formatSpeed(currentKmh)}  ·  Avg: ${formatSpeed(avgKmh)}"
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, buildNotification(text))
    }
}
