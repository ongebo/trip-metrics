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
     * Allow all hosts during development.  For a production release replace with
     * [HostValidator.Builder] configured with the actual host certificates.
     */
    override fun createHostValidator(): HostValidator = HostValidator.ALLOW_ALL_HOSTS_VALIDATOR

    override fun onCreateSession(): Session = TripCarSession()
}
