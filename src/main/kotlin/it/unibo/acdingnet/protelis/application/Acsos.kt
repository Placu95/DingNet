package it.unibo.acdingnet.protelis.application

import iot.GlobalClock
import iot.SimulationRunner
import iot.mqtt.TransmissionWrapper
import iot.networkentity.Mote
import iot.networkentity.UserMote
import it.unibo.acdingnet.protelis.dingnetwrapper.BuildingNodeWrapper
import it.unibo.acdingnet.protelis.dingnetwrapper.SensorNodeWrapper
import it.unibo.acdingnet.protelis.model.LatLongPosition
import it.unibo.acdingnet.protelis.model.SensorType
import it.unibo.acdingnet.protelis.mqtt.MqttMockCastWithDelay
import it.unibo.acdingnet.protelis.mqtt.MqttMockSerWithDelay
import it.unibo.acdingnet.protelis.neighborhood.NeighborhoodManager
import it.unibo.acdingnet.protelis.neighborhood.Node
import it.unibo.acdingnet.protelis.node.GenericNode
import it.unibo.acdingnet.protelis.physicalnetwork.HostType
import it.unibo.acdingnet.protelis.physicalnetwork.NetworkStatistic
import it.unibo.acdingnet.protelis.physicalnetwork.PhysicalNetwork
import it.unibo.acdingnet.protelis.physicalnetwork.configuration.Configuration
import it.unibo.acdingnet.protelis.util.*
import it.unibo.acdingnet.protelis.util.Utils.maxTime
import it.unibo.acdingnet.protelis.util.Utils.roundToDecimal
import org.jxmapviewer.JXMapViewer
import org.jxmapviewer.painter.Painter
import org.protelis.lang.ProtelisLoader
import org.protelis.lang.datatype.impl.StringUID
import util.time.DoubleTime
import util.time.Time
import util.time.TimeUnit
import java.io.File
import java.util.*

class Acsos(
    motes: List<Mote>,
    timer: GlobalClock,
    protelisProgram: String = "protelis:homeHeating_timer_v2"
) :
    ProtelisApplication(motes, timer, protelisProgram, emptyList()) {

    private val neigh: NeighborhoodManager
    private val loraNodes: List<SensorNodeWrapper>
    private val otherNodes: List<GenericNode>
    private val configuration = Configuration("/config/physicalnetwork/defaultNetworkConfig.toml")
    private val physicalNetwork: PhysicalNetwork =
        PhysicalNetwork(configuration, timer)
    private val random = Random(configuration.configurationNetwork.seed)
    private val samplings: MutableList<Sampling> = mutableListOf()

    init {
        // access point to simulation runner (it is shit, but it is the fastest way)
        val simulationRunner = SimulationRunner.getInstance()

        // creates node for neighborhood manager
        val nodes: MutableSet<Node> = motes.map {
            Node(
                StringUID("lora_${it.eui}"),
                LatLongPosition(
                    it.pathPosition.latitude,
                    it.pathPosition.longitude
                )
            )
        }.toMutableSet()

        // FIXME: take it form config file?
        val numOfBuilding = 300
        val mapHelper = simulationRunner.environment.mapHelper
        val maxX = simulationRunner.environment.maxXpos
        val maxY = simulationRunner.environment.maxYpos
        val buildingNodes = (0 until numOfBuilding).map {
            Node(
                StringUID("building_$it"),
                mapHelper.toGeoPosition(random.nextInt(maxX), random.nextInt(maxY))
                .toLatLongPosition()
            )
        }

        nodes.addAll(buildingNodes)

        // create neighborhood manager
        val neighUID = StringUID("NeighborhoodManager")
        neigh = NeighborhoodManager(
            Const.APPLICATION_ID,
            MqttMockCastWithDelay(physicalNetwork, timer, neighUID),
            Const.NEIGHBORHOOD_RANGE,
            nodes
        )

        // creates lora nodes
        loraNodes = motes
            .filter { it !is UserMote }
            .map {
                val id = StringUID("lora_${it.eui}")
                SensorNodeWrapper(
                    ProtelisLoader.parse(protelisProgram),
                    DoubleTime(random.nextInt(100).toDouble()),
                    900,
                    id,
                    Const.APPLICATION_ID,
                    getNewClientCast(id),
                    getNewClientSer(id),
                    it.pathPosition.toLatLongPosition(),
                    it.sensors.map { s -> SensorType.valueOf("$s") },
                    timer,
                    neigh.getNeighborhoodByNodeId(id).map { n -> n.uid }.toSet()
                )
            }

        // creates building nodes
        otherNodes = buildingNodes.map {
            BuildingNodeWrapper(
                ProtelisLoader.parse(protelisProgram),
                DoubleTime(random.nextInt(100).toDouble()).plusMinutes(1.0),
                900,
                it.uid,
                Const.APPLICATION_ID,
                getNewClientCast(it.uid),
                it.position,
                Utils.roundToDecimal(random.nextDouble(Const.MIN_TEMP, Const.MAX_TEMP)),
                0.1,
                timer,
                neigh.getNeighborhoodByNodeId(it.uid).map { n -> n.uid }.toSet()
            )
        }

        // assigns node to hosts
        physicalNetwork.addNodes(loraNodes, otherNodes)

        // adds neighborhood manager to a host
        physicalNetwork.addDeviceToBrokerHost(neighUID)

        // adds networkServer and gateway to a host and change their mqttClient
        val idClientNetServer = StringUID("NetworkServer")
        simulationRunner.networkServer.also {
            it.setMqttClientToApp(getNewClientSer(idClientNetServer))
            it.setMqttClientToGateway(getNewClientCast(idClientNetServer))
        }
        physicalNetwork.addDeviceTo(idClientNetServer, HostType.EDGE)
        simulationRunner.environment.gateways.forEach {
            val id = StringUID("G_${it.eui}")
            it.mqttClient = getNewClientCast(id)
            physicalNetwork.addDeviceTo(id, HostType.EDGE)
        }

        // add trigger for sampling
        samplings.add(Sampling(DoubleTime.zero(), 0, DoubleTime.zero(),
                DoubleTime.zero(), 0, DoubleTime.zero(), DoubleTime.zero()))
        timer.addPeriodicTrigger(DoubleTime(15.0, TimeUnit.MINUTES), 900) {
            val lastSample = samplings.last()
            val newSampling = Sampling(
                timer.time,
                NetworkStatistic.count,
                NetworkStatistic.delayToT,
                NetworkStatistic.delayMax,
                lastSample.delayCountTot + NetworkStatistic.count,
                lastSample.delaySumTot + NetworkStatistic.delayToT,
                maxTime(lastSample.delayMaxTot, NetworkStatistic.delayMax)
            )
            NetworkStatistic.reset()
            samplings.add(newSampling)
        }
    }

    private fun getNewClientSer(id: StringUID) = MqttClientHelper.addLoRaWANAdapters(
        MqttMockSerWithDelay(physicalNetwork, timer, id))

    private fun getNewClientCast(id: StringUID) = MqttClientHelper.addLoRaWANAdapters(
        MqttMockCastWithDelay(physicalNetwork, timer, id))

    override fun getPainters(): List<Painter<JXMapViewer>> = emptyList()

    override fun consumePackets(topicFilter: String?, message: TransmissionWrapper?) {}

    override fun storeSimulationResults(pathDir: String) {
        val parameter = configuration.configurationNetwork.print() +
            ", hostBroker = ${configuration.brokerHostConfig.type}"
        val timeUnit = TimeUnit.SECONDS
        val output =
            """
                # $parameter 
                #
                # The columns have the following meaning: 
                # ${Sampling.header(timeUnit)}
            """.trimIndent()
                .plus("\n")
                .plus(samplings.map { it.print(timeUnit) }.reduce { s1, s2 -> s1 + "\n" + s2 })
        val fileName = "sim_" +
                parameter.replace(" = ", "-").replace(", ", "_") +
                ".txt"
        val file = File(pathDir, fileName)
        file.printWriter().use { it.println(output) }
    }
}

data class Sampling(
    val instant: Time,
    val delayCountPartial: Int,
    val delaySumPartial: Time,
    val delayMaxPartial: Time,
    val delayCountTot: Int,
    val delaySumTot: Time,
    val delayMaxTot: Time
) {
    fun print(tU: TimeUnit) = "${printTime(instant, tU)} $delayCountPartial " +
        "${printTime(delaySumPartial, tU)} ${printTime(delayMaxPartial, tU)} $delayCountTot " +
        "${printTime(delaySumTot, tU)} ${printTime(delayMaxTot, tU)}"

    private fun printTime(t: Time, tU: TimeUnit) = roundToDecimal(t.getAs(tU), 2).toString()

    companion object {
        fun header(timeUnit: TimeUnit) = "instant[${timeUnit.name}] delayCountPartial[Sum] " +
            "delaySumPartial[${timeUnit.name}] delayMaxPartial[${timeUnit.name}] " +
            "delayCountTot[Sum] delaySumTot[${timeUnit.name}] delayMaxTot[${timeUnit.name}]"
    }
}
