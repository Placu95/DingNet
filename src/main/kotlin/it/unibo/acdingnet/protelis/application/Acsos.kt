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
import it.unibo.acdingnet.protelis.mqtt.MqttMockSerWithDelay
import it.unibo.acdingnet.protelis.neighborhood.NeighborhoodManager
import it.unibo.acdingnet.protelis.neighborhood.Node
import it.unibo.acdingnet.protelis.node.GenericNode
import it.unibo.acdingnet.protelis.physicalnetwork.HostType
import it.unibo.acdingnet.protelis.physicalnetwork.PhysicalNetwork
import it.unibo.acdingnet.protelis.physicalnetwork.configuration.Reader
import it.unibo.acdingnet.protelis.util.*
import org.jxmapviewer.JXMapViewer
import org.jxmapviewer.painter.Painter
import org.protelis.lang.ProtelisLoader
import org.protelis.lang.datatype.impl.StringUID
import util.time.DoubleTime
import java.util.*

class Acsos(
    motes: List<Mote>,
    timer: GlobalClock,
    protelisProgram: String = "protelis:homeHeating_timer"
) :
    ProtelisApplication(motes, timer, protelisProgram, emptyList()) {

    private val seed = 2L
    private val neigh: NeighborhoodManager
    private val random = Random(seed)
    private val loraNodes: List<SensorNodeWrapper>
    private val otherNodes: List<GenericNode>
    private val physicalNetwork: PhysicalNetwork =
        PhysicalNetwork(Reader("/config/physicalnetwork/defaultNetworkConfig.toml"), timer)

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
            MqttMockSerWithDelay(physicalNetwork, timer, neighUID),
            Const.NEIGHBORHOOD_RANGE,
            nodes
        )

        // creates lora nodes
        loraNodes = motes
            .filter { it !is UserMote }
            .map {
                val id = StringUID("lora_${it.eui}")
                val client = MqttClientHelper.addLoRaWANAdapters(
                    MqttMockSerWithDelay(physicalNetwork, timer, id)
                )
                SensorNodeWrapper(
                    ProtelisLoader.parse(protelisProgram),
                    DoubleTime(random.nextInt(100).toDouble()),
                    900,
                    id,
                    Const.APPLICATION_ID,
                    client,
                    client,
                    LatLongPosition(
                        it.pathPosition.latitude,
                        it.pathPosition.longitude
                    ),
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
                MqttMockSerWithDelay(physicalNetwork, timer, it.uid),
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
        val clientNetServer = MqttClientHelper.addLoRaWANAdapters(
            MqttMockSerWithDelay(physicalNetwork, timer, idClientNetServer))
        simulationRunner.networkServer.also {
            it.setMqttClientToApp(clientNetServer)
            it.setMqttClientToGateway(clientNetServer)
        }
        physicalNetwork.addDeviceTo(idClientNetServer, HostType.EDGE)
        simulationRunner.environment.gateways.forEach {
            val id = StringUID("G_${it.eui}")
            val client = MqttClientHelper.addLoRaWANAdapters(
                MqttMockSerWithDelay(physicalNetwork, timer, id))
            it.mqttClient = client
            physicalNetwork.addDeviceTo(id, HostType.EDGE)
        }
    }

    override fun getPainters(): List<Painter<JXMapViewer>> = emptyList()

    override fun consumePackets(topicFilter: String?, message: TransmissionWrapper?) {}
}
