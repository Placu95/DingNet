package it.unibo.acdingnet.protelis.dingnetwrapper

import iot.GlobalClock
import it.unibo.acdingnet.protelis.executioncontext.BuildingExecutionContext
import it.unibo.acdingnet.protelis.physicalnetwork.PhysicalNetwork
import it.unibo.mqttclientwrapper.api.MqttClientBasicApi
import org.protelis.vm.NetworkManager

class BuildingECForDingNet(
    buildingNode: BuildingNodeWrapper,
    desiredTemp: Double,
    deltaTemp: Double,
    applicationUID: String,
    mqttClient: MqttClientBasicApi,
    netmgr: NetworkManager,
    private val physicalNetwork: PhysicalNetwork? = null
) : BuildingExecutionContext(
    buildingNode,
    desiredTemp,
    deltaTemp,
    applicationUID,
    mqttClient,
    netmgr
) {

    private val timer: GlobalClock = buildingNode.timer

    override fun instance(): BuildingExecutionContext = this

    override fun getCurrentTime(): Number {
        return timer.time.asSecond()
    }

    override fun setCurrentTemp(temp: Double) {
        super.setCurrentTemp(temp)
        physicalNetwork?.computeDelayPhysicalSmartphoneToProtelisNode(deviceUID)
    }
}
