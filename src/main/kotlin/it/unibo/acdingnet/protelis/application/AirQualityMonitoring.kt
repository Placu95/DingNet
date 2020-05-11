package it.unibo.acdingnet.protelis.application

import application.pollution.PollutionGrid
import gui.mapviewer.TextPainter
import gui.mapviewer.WayPointPainter
import iot.GlobalClock
import iot.mqtt.TransmissionWrapper
import iot.networkentity.Mote
import iot.networkentity.UserMote
import it.unibo.acdingnet.protelis.dingnetwrapper.BuildingNodeWrapper
import it.unibo.acdingnet.protelis.dingnetwrapper.SensorNodeWrapper
import it.unibo.acdingnet.protelis.model.LatLongPosition
import it.unibo.acdingnet.protelis.model.SensorType
import it.unibo.acdingnet.protelis.neighborhood.NeighborhoodManager
import it.unibo.acdingnet.protelis.neighborhood.Node
import it.unibo.acdingnet.protelis.node.BuildingNode
import it.unibo.acdingnet.protelis.util.Const
import it.unibo.acdingnet.protelis.util.MqttClientHelper
import it.unibo.acdingnet.protelis.util.gui.ProtelisPollutionGrid
import it.unibo.acdingnet.protelis.util.gui.ProtelisPulltionGridPainter
import it.unibo.acdingnet.protelis.util.toGeoPosition
import it.unibo.mqttclientwrapper.mock.serialization.MqttMockSer
import org.jxmapviewer.JXMapViewer
import org.jxmapviewer.painter.Painter
import org.jxmapviewer.viewer.DefaultWaypoint
import org.jxmapviewer.viewer.Waypoint
import org.protelis.lang.ProtelisLoader
import org.protelis.lang.datatype.impl.StringUID
import util.time.DoubleTime
import java.awt.Color
import java.util.*

class AirQualityMonitoring(
    motes: List<Mote>,
    timer: GlobalClock,
    protelisProgram: String = "/protelis/homeHeating_timer.pt"
) : ProtelisApplication(motes, timer, protelisProgram, emptyList()) {

    private val neigh: NeighborhoodManager
    private val random = Random(2)
    private val sensorNodes: List<SensorNodeWrapper>
    private val building: List<BuildingNode>

    init {
        val nodes: MutableSet<Node> = motes.map {
            Node(
                StringUID("" + it.eui),
                LatLongPosition(
                    it.pathPosition.latitude,
                    it.pathPosition.longitude
                )
            )
        }.toMutableSet()

        val buildingNode = listOf(
            Pair(
                Node(
                    StringUID("0"),
                    LatLongPosition(50.877910751397, 4.69141960144043)
                ), // 25
                23.2
            ),
            Pair(
                Node(
                    StringUID("1"),
                    LatLongPosition(50.884419292982145, 4.711053371429443)
                ), // 23
                24.0
            ),
            Pair(
                Node(
                    StringUID("2"),
                    LatLongPosition(50.86946149128906, 4.702663421630859)
                ), // 24
                23.5
            )
        )

        nodes.addAll(buildingNode.map { it.first })

        neigh = NeighborhoodManager(
            Const.APPLICATION_ID,
            MqttMockSer(), Const.NEIGHBORHOOD_RANGE, nodes
        )

        sensorNodes = motes
            .filter { it !is UserMote }
            .map {
                val id = StringUID("" + it.eui)
                SensorNodeWrapper(
                    ProtelisLoader.parse(protelisProgramResource),
                    DoubleTime(random.nextInt(100).toDouble()),
                    900,
                    id,
                    Const.APPLICATION_ID,
                    MqttMockSer(),
                    MqttClientHelper.addLoRaWANAdapters(MqttMockSer()),
                    LatLongPosition(
                        it.pathPosition.latitude,
                        it.pathPosition.longitude
                    ),
                    it.sensors.map { s -> SensorType.valueOf("$s") },
                    timer,
                    neigh.getNeighborhoodByNodeId(id).map { n -> n.uid }.toSet()
                )
            }

        building = buildingNode.map {
            BuildingNodeWrapper(
                ProtelisLoader.parse(protelisProgramResource),
                DoubleTime(random.nextInt(100).toDouble()).plusMinutes(1.0),
                900,
                it.first.uid,
                Const.APPLICATION_ID,
                MqttMockSer(),
                it.first.position,
                it.second,
                0.1,
                timer,
                neigh.getNeighborhoodByNodeId(it.first.uid).map { n -> n.uid }.toSet()
            )
        }
    }

    override fun getPainters(): List<Painter<JXMapViewer>> {
        val gridPainter = ProtelisPulltionGridPainter(getPollutionGrid())
        val buildingPainter = WayPointPainter<Waypoint>(Color.BLACK, 10)
            .setWaypoints(building
                .map { it.position.toGeoPosition() }
                .map { DefaultWaypoint(it) }
                .toSet()
        )
        val tempPainter = TextPainter<Waypoint>(TextPainter.Type.WAYPOINT).setWaypoints(
            building.map { DefaultWaypoint(it.position.toGeoPosition()) to "" +
                "${it.getTemp(Const.ProtelisEnv.CURRENT_TEMP)}\u00ba/" +
                "${it.getTemp(Const.ProtelisEnv.DESIRED_TEMP)}\u00ba/" +
                "${it.getTemp(Const.ProtelisEnv.MAX_TEMP)}\u00ba" }
                .toMap()
        )
        return listOf(gridPainter, buildingPainter, tempPainter)
    }

    override fun consumePackets(topicFilter: String?, message: TransmissionWrapper?) { }

    private fun getPollutionGrid(): PollutionGrid = ProtelisPollutionGrid(
        sensorNodes.map { Pair(it.position.toGeoPosition(), it.getPollutionValue()) },
        Const.NEIGHBORHOOD_RANGE,
        20.0 // value in good level
    )
}
