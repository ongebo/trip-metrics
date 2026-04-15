package com.tripmetrics

import android.content.Context
import com.google.gson.Gson
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Persists and retrieves [TripRecord]s as JSON files in the app's external files directory.
 *
 * Files are stored under `<externalFilesDir>/trips/trip_<timestamp>.json` so they are
 * readable by the user via a file-manager app while still being scoped to this application.
 */
class TripRepository(private val context: Context) {

    private val gson = Gson()

    private val storageDir: File
        get() = File(context.getExternalFilesDir(null), "trips").also { it.mkdirs() }

    /** Serialise [record] to JSON and write it to a timestamped file. Returns the file. */
    fun saveTrip(record: TripRecord): File {
        val dateStr = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
            .format(Date(record.startTimeMs))
        val file = File(storageDir, "trip_$dateStr.json")
        file.writeText(gson.toJson(record))
        return file
    }

    /** Load all persisted trips, sorted newest-first. */
    fun loadAllTrips(): List<TripRecord> {
        val dir = storageDir
        if (!dir.exists()) return emptyList()
        return dir.listFiles { f -> f.extension == "json" }
            ?.mapNotNull { file ->
                runCatching { gson.fromJson(file.readText(), TripRecord::class.java) }.getOrNull()
            }
            ?.sortedByDescending { it.startTimeMs }
            ?: emptyList()
    }
}
