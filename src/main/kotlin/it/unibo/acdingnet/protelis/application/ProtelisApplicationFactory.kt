package it.unibo.acdingnet.protelis.application

import iot.GlobalClock
import iot.networkentity.Mote

object ProtelisApplicationFactory {

    fun createApplication(
        protelisApplication: String,
        motes: List<Mote>,
        timer: GlobalClock
    ): ProtelisApplication = when (protelisApplication) {
        AirQualityMonitoring::class.java.simpleName -> AirQualityMonitoring(motes, timer)
        SmartThermostats::class.java.simpleName -> SmartThermostats(motes, timer)
        else -> throw IllegalArgumentException("Application name is unknown")
    }
}
