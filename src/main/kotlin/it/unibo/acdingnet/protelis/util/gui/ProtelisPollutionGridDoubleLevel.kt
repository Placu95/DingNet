package it.unibo.acdingnet.protelis.util.gui

import application.pollution.PollutionLevel
import org.jxmapviewer.viewer.GeoPosition
import org.protelis.lang.datatype.DeviceUID
import util.MapHelper

/**
 * Pollution grid implementation based on the idw of the sensed value with `range` maximum distance
 */
class ProtelisPollutionGridDoubleLevel(
    private val sensorsWithId: List<Triple<DeviceUID, GeoPosition, Double>>,
    private val building: List<GeoPosition>,
    range: Double,
    defaultValue: Double
) : ProtelisPollutionGrid(sensorsWithId.map { Pair(it.second, it.third) }, range, defaultValue) {

    override fun clean() { }

    override fun getCloseSensors(position: GeoPosition): List<Pair<Double, Double>> {
        val closeSensors = getCloseSensorsWithId(position)
        val closeSensorByBuilding = building.asSequence()
            .map { Pair(MapHelper.distanceMeter(it, position), it) }
            .filter { it.first < range }
            .flatMap { getCloseSensorsWithId(position) }
        return (closeSensors + closeSensorByBuilding)
            .distinctBy { it.first }
            .map { Pair(it.second, it.third) }
            .toList()
    }

    private fun getCloseSensorsWithId(position: GeoPosition) = sensorsWithId.asSequence()
        .map { Triple(it.first, MapHelper.distanceMeter(it.second, position), it.third) }
        .filter { it.second < range }

    override fun addMeasurement(deviceEUI: Long, position: GeoPosition?, level: PollutionLevel?) { }
}
