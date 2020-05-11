package it.unibo.acdingnet.protelis.node

import it.unibo.acdingnet.protelis.model.LatLongPosition
import it.unibo.acdingnet.protelis.networkmanager.MQTTNetMgrWithMQTTNeighborhoodMgr
import it.unibo.acdingnet.protelis.physicalnetwork.Host
import it.unibo.mqttclientwrapper.api.MqttClientBasicApi
import org.protelis.lang.datatype.impl.StringUID
import org.protelis.vm.ExecutionContext
import org.protelis.vm.ProtelisProgram
import org.protelis.vm.ProtelisVM
import kotlin.properties.Delegates.observable

abstract class GenericNode(
    val protelisProgram: ProtelisProgram,
    val sleepTime: Long,
    val deviceUID: StringUID,
    val applicationUID: String,
    netManagerMqttClient: MqttClientBasicApi,
    val execContextMqttClient: MqttClientBasicApi,
    position: LatLongPosition,
    host: Host? = null,
    neighbors: Set<StringUID>? = null
) {
    constructor(
        protelisProgram: ProtelisProgram,
        sleepTime: Long,
        deviceUID: StringUID,
        applicationUID: String,
        mqttClient: MqttClientBasicApi,
        initialPosition: LatLongPosition,
        host: Host? = null,
        neighbors: Set<StringUID>? = null
    ) : this(protelisProgram, sleepTime, deviceUID, applicationUID,
        mqttClient, mqttClient, initialPosition, host, neighbors)

    var position: LatLongPosition by observable(position) {
        _, old, new -> if (old != new) networkManager.changePosition(new)
    }

    var host: Host? = host
        set(value) {
            checkNotNull(value) { "You cannot assign a null host to a node" }
            field?.removeDevice(deviceUID)
            field = value
            field?.addDevice(deviceUID)
        }

    protected val networkManager =
        MQTTNetMgrWithMQTTNeighborhoodMgr(
            deviceUID,
            netManagerMqttClient, applicationUID, this.position, neighbors
        )
    protected val executionContext by lazy { createContext() }
    private val protelisVM by lazy { ProtelisVM(protelisProgram, executionContext) }

    protected abstract fun createContext(): ExecutionContext

    fun runVM() {
        protelisVM.runCycle()
        host?.addRun()
    }
}
