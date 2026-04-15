package com.tripmetrics.auto

import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator

/**
 * Entry point for Android Auto.  The Car App Library routes connections from the
 * head unit to this service, which creates a [TripCarSession] per connection.
 */
class TripCarAppService : CarAppService() {

    /**
     * **Development only.** [HostValidator.ALLOW_ALL_HOSTS_VALIDATOR] accepts connections from
     * any host without certificate verification.
     *
     * Before publishing to the Play Store, replace this with a properly configured
     * [HostValidator.Builder] that pins the expected host certificates:
     * ```kotlin
     * HostValidator.Builder(applicationContext)
     *     .addAllowedHost("com.google.android.projection.gearhead", R.array.hosts_allowlist_googlemobile)
     *     .build()
     * ```
     */
    override fun createHostValidator(): HostValidator = HostValidator.ALLOW_ALL_HOSTS_VALIDATOR

    override fun onCreateSession(): Session = TripCarSession()
}
