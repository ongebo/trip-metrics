package com.tripmetrics

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tripmetrics.databinding.ActivityMainBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: TripViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true
                || perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) viewModel.startTrip()
        else android.widget.Toast.makeText(this, R.string.permission_denied, android.widget.Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnStartStop.setOnClickListener {
            if (viewModel.metrics.value.isRunning) viewModel.stopTrip()
            else checkPermissionsAndStart()
        }

        binding.btnHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        lifecycleScope.launch {
            viewModel.metrics.collectLatest { updateUI(it) }
        }

        lifecycleScope.launch {
            viewModel.lastSavedTrip.collectLatest { record ->
                if (record != null) {
                    viewModel.clearLastSavedTrip()
                    showNamingDialog(record)
                }
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
        binding.tvAverageSpeed.text = formatSpeed(metrics.averageSpeedKmh)
        binding.tvCurrentSpeed.text = formatSpeed(metrics.currentSpeedKmh)
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

    private fun showNamingDialog(record: TripRecord) {
        val input = EditText(this).apply {
            hint = getString(R.string.name_trip_hint)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            setPadding(64, 32, 64, 16)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.name_trip_title)
            .setView(input)
            .setPositiveButton(R.string.save) { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) viewModel.updateTripName(record.id, name)
            }
            .setNegativeButton(R.string.skip, null)
            .show()
    }
}
