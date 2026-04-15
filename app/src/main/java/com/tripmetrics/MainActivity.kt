package com.tripmetrics

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.tripmetrics.databinding.ActivityMainBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Main phone activity.  Shows real-time trip metrics and a single Start / Stop button.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: TripViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true
                || perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) viewModel.startTrip()
        else Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnStartStop.setOnClickListener {
            if (viewModel.metrics.value.isRunning) viewModel.stopTrip()
            else checkPermissionsAndStart()
        }

        lifecycleScope.launch {
            viewModel.metrics.collectLatest { updateUI(it) }
        }

        lifecycleScope.launch {
            viewModel.lastSavedTrip.collectLatest { record ->
                record?.let { showSavedToast(it) }
            }
        }
    }

    private fun checkPermissionsAndStart() {
        val fine = Manifest.permission.ACCESS_FINE_LOCATION
        val coarse = Manifest.permission.ACCESS_COARSE_LOCATION
        val hasFine = ContextCompat.checkSelfPermission(this, fine) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(this, coarse) == PackageManager.PERMISSION_GRANTED
        if (hasFine || hasCoarse) viewModel.startTrip()
        else permissionLauncher.launch(arrayOf(fine, coarse))
    }

    private fun updateUI(metrics: TripMetrics) {
        binding.tvCurrentSpeed.text = formatSpeed(metrics.currentSpeedKmh)
        binding.tvAverageSpeed.text = formatSpeed(metrics.averageSpeedKmh)
        binding.tvDuration.text = formatDuration(metrics.durationSeconds)
        binding.tvDistance.text = formatDistance(metrics.distanceMeters)
        binding.tvMaxSpeed.text = formatSpeed(metrics.maxSpeedKmh)

        if (metrics.isRunning) {
            binding.btnStartStop.setText(R.string.stop_trip)
            binding.btnStartStop.backgroundTintList =
                ColorStateList.valueOf(getColor(R.color.stop_red))
            binding.groupMetrics.visibility = View.VISIBLE
            binding.tvStatus.setText(R.string.status_running)
        } else {
            binding.btnStartStop.setText(R.string.start_trip)
            binding.btnStartStop.backgroundTintList =
                ColorStateList.valueOf(getColor(R.color.start_green))
            binding.groupMetrics.visibility = View.GONE
            binding.tvStatus.setText(R.string.status_ready)
        }
    }

    private fun showSavedToast(record: TripRecord) {
        val msg = getString(
            R.string.trip_saved_message,
            formatDistance(record.distanceMeters),
            formatSpeed(record.averageSpeedKmh)
        )
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }
}
