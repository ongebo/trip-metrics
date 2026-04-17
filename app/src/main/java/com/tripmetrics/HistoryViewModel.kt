package com.tripmetrics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TripRepository(application)

    private val _trips = MutableStateFlow<List<TripRecord>>(emptyList())
    val trips: StateFlow<List<TripRecord>> = _trips.asStateFlow()

    init {
        reload()
    }

    fun reload() {
        _trips.value = repository.loadAllTrips()
    }
}
