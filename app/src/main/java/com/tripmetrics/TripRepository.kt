package com.tripmetrics

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class TripRepository(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("trip_history", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val listType = object : TypeToken<List<TripRecord>>() {}.type

    fun saveTrip(record: TripRecord) {
        val trips = loadAllTrips().toMutableList()
        trips.add(0, record)
        prefs.edit().putString(KEY_TRIPS, gson.toJson(trips)).apply()
    }

    fun updateTripName(id: String, name: String) {
        val trips = loadAllTrips().map { if (it.id == id) it.copy(name = name) else it }
        prefs.edit().putString(KEY_TRIPS, gson.toJson(trips)).apply()
    }

    fun loadAllTrips(): List<TripRecord> {
        val json = prefs.getString(KEY_TRIPS, null) ?: return emptyList()
        return gson.fromJson(json, listType) ?: emptyList()
    }

    companion object {
        private const val KEY_TRIPS = "trips"
    }
}
