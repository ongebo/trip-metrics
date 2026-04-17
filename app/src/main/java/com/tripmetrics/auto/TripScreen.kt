package com.tripmetrics.auto

import android.content.Intent
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarColor
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import com.tripmetrics.TripService
import com.tripmetrics.formatDistance
import com.tripmetrics.formatDuration
import com.tripmetrics.formatSpeed
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/**
 * Android Auto screen that displays live trip metrics and exposes a Start / Stop action.
 *
 * Uses [ListTemplate] to display up to 4 metric rows and an [ActionStrip] for the
 * Start / Stop button.  The screen listens to [TripService.metrics] and calls [invalidate]
 * on every update so the head unit re-renders with fresh data.
 */
class TripScreen(carContext: CarContext) : Screen(carContext) {

    init {
        lifecycleScope.launch {
            TripService.metrics.collect { invalidate() }
        }
    }

    override fun onGetTemplate(): Template {
        val metrics = TripService.metrics.value

        val startStopAction = Action.Builder()
            .setTitle(if (metrics.isRunning) "Stop" else "Start")
            .setBackgroundColor(if (metrics.isRunning) CarColor.RED else CarColor.GREEN)
            .setOnClickListener {
                val intent = Intent(carContext, TripService::class.java)
                if (metrics.isRunning) {
                    intent.action = TripService.ACTION_STOP
                    carContext.startService(intent)
                } else {
                    intent.action = TripService.ACTION_START
                    carContext.startForegroundService(intent)
                }
            }
            .build()

        val itemList = ItemList.Builder()
            .addItem(
                Row.Builder()
                    .setTitle("Current Speed")
                    .addText(formatSpeed(metrics.currentSpeedKmh))
                    .build()
            )
            .addItem(
                Row.Builder()
                    .setTitle("Average Speed")
                    .addText(formatSpeed(metrics.averageSpeedKmh))
                    .build()
            )
            .addItem(
                Row.Builder()
                    .setTitle("Duration")
                    .addText(formatDuration(metrics.durationSeconds))
                    .build()
            )
            .addItem(
                Row.Builder()
                    .setTitle("Distance")
                    .addText(formatDistance(metrics.distanceMeters))
                    .build()
            )
            .build()

        return ListTemplate.Builder()
            .setHeaderAction(Action.APP_ICON)
            .setTitle("Trip Metrics")
            .setSingleList(itemList)
            .setActionStrip(
                ActionStrip.Builder()
                    .addAction(startStopAction)
                    .build()
            )
            .build()
    }
}
