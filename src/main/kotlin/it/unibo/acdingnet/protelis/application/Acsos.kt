package it.unibo.acdingnet.protelis.application

import iot.GlobalClock
import iot.mqtt.TransmissionWrapper
import iot.networkentity.Mote
import iot.networkentity.UserMote
import it.unibo.acdingnet.protelis.dingnetwrapper.SensorNodeWrapper
import it.unibo.acdingnet.protelis.model.LatLongPosition
import it.unibo.acdingnet.protelis.model.SensorType
import it.unibo.acdingnet.protelis.mqtt.MqttMockSerWithDelay
import it.unibo.acdingnet.protelis.neighborhood.NeighborhoodManager
import it.unibo.acdingnet.protelis.neighborhood.Node
import it.unibo.acdingnet.protelis.node.GenericNode
import it.unibo.acdingnet.protelis.physicalnetwork.PhysicalNetwork
import it.unibo.acdingnet.protelis.physicalnetwork.configuration.Reader
import it.unibo.acdingnet.protelis.util.Const
import it.unibo.acdingnet.protelis.util.MqttClientHelper
import org.jxmapviewer.JXMapViewer
import org.jxmapviewer.painter.Painter
import org.protelis.lang.ProtelisLoader
import org.protelis.lang.datatype.impl.StringUID
import util.time.DoubleTime
import java.util.*

class Acsos(
    motes: List<Mote>,
    timer: GlobalClock,
    protelisProgram: String = ""
) :
    ProtelisApplication(motes, timer, protelisProgram, emptyList()) {

    private val neigh: NeighborhoodManager
    private val random = Random(2)
    private val loraNodes: List<SensorNodeWrapper>
    private val otherNodes: List<GenericNode>
    private val physicalNetwork: PhysicalNetwork = PhysicalNetwork(Reader(""), timer)

    init {
        // creates node for neighborhood manager and neighborhood manager
        val nodes: MutableSet<Node> = motes.map {
            Node(
                StringUID("" + it.eui),
                LatLongPosition(
                    it.pathPosition.latitude,
                    it.pathPosition.longitude
                )
            )
        }.toMutableSet()
        // add to nodes set the other nodes
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
                val id = StringUID("lora_" + it.eui)
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

        // creates other nodes
        otherNodes = emptyList()

        // assigns node to hosts
        physicalNetwork.addNodes(loraNodes, otherNodes)

        // adds networkServer and gateway to a host and change their mqttClient

        // adds neighborhood manager to a host
        physicalNetwork.addDeviceToBrokerHost(neighUID)
    }

    override fun getPainters(): List<Painter<JXMapViewer>> = emptyList()

    override fun consumePackets(topicFilter: String?, message: TransmissionWrapper?) {}
}
