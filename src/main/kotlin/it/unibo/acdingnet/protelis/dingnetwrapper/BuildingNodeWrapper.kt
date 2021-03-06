package it.unibo.acdingnet.protelis.dingnetwrapper

import iot.GlobalClock
import it.unibo.acdingnet.protelis.model.LatLongPosition
import it.unibo.acdingnet.protelis.node.BuildingNode
import it.unibo.acdingnet.protelis.physicalnetwork.Host
import it.unibo.acdingnet.protelis.physicalnetwork.PhysicalNetwork
import it.unibo.mqttclientwrapper.api.MqttClientBasicApi
import org.protelis.lang.datatype.impl.StringUID
import org.protelis.vm.ExecutionContext
import org.protelis.vm.ProtelisProgram
import util.time.Time

class BuildingNodeWrapper(
    protelisProgram: ProtelisProgram,
    startingTime: Time,
    sleepTime: Long,
    deviceUID: StringUID,
    applicationUID: String,
    netManagerMqttClient: MqttClientBasicApi,
    position: LatLongPosition,
    desiredTemp: Double,
    deltaTemp: Double,
    val timer: GlobalClock,
    neighbors: Set<StringUID>,
    host: Host? = null,
    private val physicalNetwork: PhysicalNetwork? = null
) : BuildingNode(
    protelisProgram,
    sleepTime,
    deviceUID,
    applicationUID,
    netManagerMqttClient,
    position,
    desiredTemp,
    deltaTemp,
    host,
    neighbors
) {

    override fun createContext(): ExecutionContext {
        return BuildingECForDingNet(
            this,
            desiredTemp,
            deltaTemp,
            applicationUID,
            execContextMqttClient,
            networkManager,
            physicalNetwork
        )
    }

    init {
        timer.addPeriodicTrigger(startingTime, sleepTime) { runVM() }
    }

    fun getContext(): BuildingECForDingNet = executionContext as BuildingECForDingNet
}
