package it.unibo.acdingnet.protelis.dingnetwrapper

import iot.GlobalClock
import it.unibo.acdingnet.protelis.executioncontext.SensorExecutionContext
import it.unibo.acdingnet.protelis.model.LoRaTransmission
import it.unibo.acdingnet.protelis.physicalnetwork.NetworkStatistic
import it.unibo.acdingnet.protelis.util.Const
import it.unibo.acdingnet.protelis.util.Const.DEFAULT_IAQ_LEVEL
import it.unibo.mqttclientwrapper.api.MqttClientBasicApi
import org.protelis.vm.NetworkManager
import util.time.DoubleTime
import util.time.TimeUnit

class SensorECForDingNet(
    private val sensorNode: SensorNodeWrapper,
    applicationUID: String,
    mqttClient: MqttClientBasicApi,
    netmgr: NetworkManager
) : SensorExecutionContext(sensorNode, applicationUID, mqttClient, netmgr) {
    private val timer: GlobalClock = sensorNode.timer

    init {
        execEnvironment.put(Const.ProtelisEnv.IAQLEVEL, DEFAULT_IAQ_LEVEL)
    }

    override fun instance(): SensorECForDingNet = this

    override fun getCurrentTime(): Number {
        return timer.time.asSecond()
    }

    override fun handleDeviceTransmission(message: LoRaTransmission) {
        NetworkStatistic.addDelay(DoubleTime(message.timeOnAir, TimeUnit.MILLIS), NetworkStatistic.Type.UPLOAD)
        super.handleDeviceTransmission(message)
    }
}
