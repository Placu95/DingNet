package it.unibo.acdingnet.protelis.application

import Simulator
import application.pollution.PollutionGrid
import gui.mapviewer.WayPointPainter
import iot.GlobalClock
import iot.SimulationRunner
import iot.mqtt.TransmissionWrapper
import iot.networkentity.Mote
import iot.networkentity.UserMote
import it.unibo.acdingnet.protelis.dingnetwrapper.BuildingNodeWrapper
import it.unibo.acdingnet.protelis.dingnetwrapper.SensorNodeWrapper
import it.unibo.acdingnet.protelis.executioncontext.SensorExecutionContext
import it.unibo.acdingnet.protelis.model.LatLongPosition
import it.unibo.acdingnet.protelis.model.SensorType
import it.unibo.acdingnet.protelis.mqtt.MqttBrokerMockSerWithDelay
import it.unibo.acdingnet.protelis.mqtt.MqttBrokerMockWithDelay
import it.unibo.acdingnet.protelis.mqtt.MqttMockCastWithDelay
import it.unibo.acdingnet.protelis.mqtt.MqttMockSerWithDelay
import it.unibo.acdingnet.protelis.neighborhood.NeighborhoodManager
import it.unibo.acdingnet.protelis.neighborhood.Node
import it.unibo.acdingnet.protelis.physicalnetwork.HostType
import it.unibo.acdingnet.protelis.physicalnetwork.PhysicalNetwork
import it.unibo.acdingnet.protelis.physicalnetwork.configuration.Configuration
import it.unibo.acdingnet.protelis.util.*
import it.unibo.acdingnet.protelis.util.gui.ProtelisPollutionGridDoubleLevel
import it.unibo.acdingnet.protelis.util.gui.ProtelisPulltionGridPainter
import org.jxmapviewer.JXMapViewer
import org.jxmapviewer.painter.Painter
import org.jxmapviewer.viewer.DefaultWaypoint
import org.jxmapviewer.viewer.Waypoint
import org.protelis.lang.ProtelisLoader
import org.protelis.lang.datatype.impl.StringUID
import util.time.DoubleTime
import util.time.TimeUnit
import java.awt.Color
import java.io.File
import java.util.*

class Acsos(
    motes: List<Mote>,
    timer: GlobalClock,
    protelisProgram: String = "/protelis/homeHeating_timer_v2.pt"
) :
    ProtelisApplication(motes, timer, protelisProgram, emptyList()) {

    companion object {
        private const val DEFAULT_NETWORK_CONFIG =
            "/config/physicalnetwork/defaultNetworkConfig.toml"
    }

    private val neigh: NeighborhoodManager
    private val loraNodes: List<SensorNodeWrapper>
    private val otherNodes: List<BuildingNodeWrapper>
    private val configuration = Configuration(
        Simulator.getNetworkConfigFilePath().orElse(DEFAULT_NETWORK_CONFIG))
    private val physicalNetwork: PhysicalNetwork =
        PhysicalNetwork(configuration)
    private val brokerCast = MqttBrokerMockWithDelay(timer, physicalNetwork)
    private val brokerSer = MqttBrokerMockSerWithDelay(timer, physicalNetwork)
    private val random = Random(configuration.configurationNetwork.seed)
    private val sampler: Sampler

    init {
        // access point to simulation runner (it is shit, but it is the fastest way)
        val simulationRunner = SimulationRunner.getInstance()

        // creates node for neighborhood manager
        val nodes: MutableSet<Node> = motes.map {
            Node(
                StringUID("${it.eui}"),
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
            MqttMockCastWithDelay(neighUID, brokerCast),
            Const.NEIGHBORHOOD_RANGE,
            nodes
        )

        // creates lora nodes
        loraNodes = motes
            .filter { it !is UserMote }
            .map {
                val id = StringUID("${it.eui}")
                SensorNodeWrapper(
                    ProtelisLoader.parse(protelisProgramResource),
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
                ProtelisLoader.parse(protelisProgramResource),
                DoubleTime(random.nextInt(100).toDouble()).plusMinutes(1.0),
                900,
                it.uid,
                Const.APPLICATION_ID,
                getNewClientCast(it.uid),
                it.position,
                Utils.roundToDecimal(random.nextDouble(Const.MIN_TEMP, Const.MAX_TEMP)),
                0.1,
                timer,
                neigh.getNeighborhoodByNodeId(it.uid).map { n -> n.uid }.toSet(),
                physicalNetwork = physicalNetwork
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

        var contexts: List<SensorExecutionContext> = loraNodes.map { it.getContext() }
        contexts += otherNodes.map { it.getContext() }

        sampler = Sampler(physicalNetwork, contexts, timer, DoubleTime(15.0, TimeUnit.MINUTES)).also { it.start() }
    }

    private fun getNewClientSer(id: StringUID) = MqttClientHelper.addLoRaWANAdapters(
        MqttMockSerWithDelay(id, brokerSer))

    private fun getNewClientCast(id: StringUID) = MqttClientHelper.addLoRaWANAdapters(
        MqttMockCastWithDelay(id, brokerCast))

    // override fun getPainters(): List<Painter<JXMapViewer>> = emptyList()

    override fun consumePackets(topicFilter: String?, message: TransmissionWrapper?) {}

    override fun storeSimulationResults(pathDir: String) {
        val parameter = configuration.configurationNetwork.print(TimeUnit.MILLIS) +
            ", hostBroker = ${configuration.brokerHostConfig.type}"
        val timeUnit = TimeUnit.SECONDS
        val output =
            """
                # $parameter 
                #
                # The columns have the following meaning: 
                # ${Sampler.header(timeUnit)}
            """.trimIndent()
                .plus("\n")
                .plus(
                    sampler.getSamplings().asSequence()
                        .map { it.print(timeUnit) }
                        .reduce { s1, s2 -> s1 + "\n" + s2 }
                )
        val fileName = "sim_" +
                parameter.replace(" = ", "-").replace(", ", "_") +
                ".txt"
        val file = File(pathDir, fileName)
        file.printWriter().use { it.println(output) }
    }

    override fun getPainters(): List<Painter<JXMapViewer>> {
        val gridPainter = ProtelisPulltionGridPainter(getPollutionGrid())
        val buildingPainter = WayPointPainter<Waypoint>(Color.BLACK, 10)
            .setWaypoints(otherNodes
                .map { it.position.toGeoPosition() }
                .map { DefaultWaypoint(it) }
                .toSet()
            )
        return listOf(gridPainter, buildingPainter)
    }

    private fun getPollutionGrid(): PollutionGrid = ProtelisPollutionGridDoubleLevel(
        loraNodes.map { Triple(it.deviceUID, it.position.toGeoPosition(), it.getPollutionValue()) },
        otherNodes.map { it.position.toGeoPosition() },
        Const.NEIGHBORHOOD_RANGE,
        // I take the node "6" because is not in the raining region, so its value correspond to the global value
        loraNodes.find { it.deviceUID == StringUID("-6") }?.getPollutionValue() ?: Const.DEFAULT_IAQ_LEVEL
    )
}
