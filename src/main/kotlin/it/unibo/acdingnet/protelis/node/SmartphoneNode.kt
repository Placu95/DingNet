package it.unibo.acdingnet.protelis.node

import iot.GlobalClock
import it.unibo.acdingnet.protelis.executioncontext.SmartphoneEC
import it.unibo.acdingnet.protelis.model.GPSTrace
import it.unibo.acdingnet.protelis.model.LatLongPosition
import it.unibo.acdingnet.protelis.physicalnetwork.Host
import it.unibo.acdingnet.protelis.util.toGeoPosition
import it.unibo.acdingnet.protelis.util.travel
import it.unibo.mqttclientwrapper.api.MqttClientBasicApi
import org.protelis.lang.datatype.impl.StringUID
import org.protelis.vm.ExecutionContext
import org.protelis.vm.ProtelisProgram
import util.MapHelper
import util.time.Time

class SmartphoneNode(
    protelisProgram: ProtelisProgram,
    startingTime: Time,
    sleepTime: Long,
    deviceUID: StringUID,
    applicationUID: String,
    mqttClient: MqttClientBasicApi,
    initialPosition: LatLongPosition,
    private val timer: GlobalClock,
    private val trace: GPSTrace,
    host: Host? = null,
    neighbors: Set<StringUID>
) : GenericNode(protelisProgram, sleepTime, deviceUID, applicationUID, mqttClient,
    initialPosition, host, neighbors) {

    private val updatePositionTriggerId: Long

    override fun createContext(): ExecutionContext = SmartphoneEC(
        this,
        networkManager
    )

    init {
        timer.addPeriodicTrigger(startingTime, sleepTime) { runVM() }
        updatePositionTriggerId = timer.addPeriodicTrigger(
            startingTime.plusSeconds((sleepTime - 1).toDouble()), sleepTime) { position = move() } // generate MQTT message
    }

    private fun move(): LatLongPosition {
        val currentTime = timer.time
        val next = trace.positions
            .mapIndexed { index, pos -> Pair(index, pos) }
            .firstOrNull { it.second.time.isAfter(currentTime) }
            ?: return position
        // if the next position to reach is the first of the trace -> DON'T move
        if (next.first == 0) {
            return position
        }
        val (index, nextPos) = next
        val prePos = trace.positions[index - 1]
        if (nextPos.time == prePos.time) {
            return nextPos.position
        }
        val traveledTime = (currentTime - prePos.time).asMilli() /
            (nextPos.time - prePos.time).asMilli()
        val distanceToTravel = MapHelper.distanceMeter(prePos.position.toGeoPosition(),
            nextPos.position.toGeoPosition()) * traveledTime
        return prePos.position.travel(nextPos.position, distanceToTravel)
    }
}
