package it.unibo.acdingnet.protelis.node

import it.unibo.acdingnet.protelis.executioncontext.BuildingExecutionContext
import it.unibo.acdingnet.protelis.model.LatLongPosition
import it.unibo.acdingnet.protelis.physicalnetwork.Host
import it.unibo.mqttclientwrapper.api.MqttClientBasicApi
import org.protelis.lang.datatype.impl.StringUID
import org.protelis.vm.ExecutionContext
import org.protelis.vm.ProtelisProgram

open class BuildingNode(
    protelisProgram: ProtelisProgram,
    sleepTime: Long,
    deviceUID: StringUID,
    applicationUID: String,
    netManagerMqttClient: MqttClientBasicApi,
    position: LatLongPosition,
    protected val desiredTemp: Double,
    protected val deltaTemp: Double,
    host: Host? = null,
    neighbors: Set<StringUID>? = null
) : SensorNode(protelisProgram, sleepTime, deviceUID, applicationUID,
    netManagerMqttClient, netManagerMqttClient, position, emptyList(), host, neighbors) {

    override fun createContext(): ExecutionContext =
        BuildingExecutionContext(
            this,
            desiredTemp,
            deltaTemp,
            applicationUID,
            execContextMqttClient,
            networkManager
        )

    fun getTemp(temp: String): Double = executionContext.executionEnvironment.get(temp) as Double
}
