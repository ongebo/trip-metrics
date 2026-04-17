package com.tripmetrics.auto

import android.content.Intent
import androidx.car.app.Screen
import androidx.car.app.Session

/**
 * Manages the lifecycle of a single Android Auto connection.
 * Creates the initial [TripScreen] when the head unit connects.
 */
class TripCarSession : Session() {
    override fun onCreateScreen(intent: Intent): Screen = TripScreen(carContext)
}
