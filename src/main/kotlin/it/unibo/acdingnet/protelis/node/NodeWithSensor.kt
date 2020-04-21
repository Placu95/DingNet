package it.unibo.acdingnet.protelis.node

import it.unibo.acdingnet.protelis.model.LatLongPosition
import it.unibo.acdingnet.protelis.model.SensorType
import it.unibo.acdingnet.protelis.physicalnetwork.Host
import it.unibo.mqttclientwrapper.api.MqttClientBasicApi
import org.protelis.lang.datatype.impl.StringUID
import org.protelis.vm.ProtelisProgram

abstract class NodeWithSensor(
    protelisProgram: ProtelisProgram,
    sleepTime: Long,
    sensorDeviceUID: StringUID,
    applicationUID: String,
    netManagerMqttClient: MqttClientBasicApi,
    execContextMqttClient: MqttClientBasicApi,
    position: LatLongPosition,
    val sensorTypes: List<SensorType>,
    host: Host? = null,
    neighbors: Set<StringUID>? = null
) : GenericNode(protelisProgram, sleepTime, sensorDeviceUID, applicationUID,
    netManagerMqttClient, execContextMqttClient, position, host, neighbors)
