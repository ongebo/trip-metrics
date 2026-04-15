package com.tripmetrics

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.StateFlow

/**
 * ViewModel that bridges [TripService] state to the phone UI.
 *
 * It delegates start/stop requests to the service and exposes the service's
 * companion-object [StateFlow]s so that the UI only needs to observe this ViewModel.
 */
class TripViewModel(application: Application) : AndroidViewModel(application) {

    /** Live trip metrics from [TripService]. */
    val metrics: StateFlow<TripMetrics> = TripService.metrics

    /** Emits the last saved trip after [stopTrip] is called. */
    val lastSavedTrip: StateFlow<TripRecord?> = TripService.lastSavedTrip

    fun startTrip() {
        TripService.start(getApplication())
    }

    fun stopTrip() {
        TripService.stop(getApplication())
    }
}
