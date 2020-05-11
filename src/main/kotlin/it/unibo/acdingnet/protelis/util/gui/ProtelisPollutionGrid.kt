package it.unibo.acdingnet.protelis.util.gui

import application.pollution.PollutionGrid
import application.pollution.PollutionLevel
import org.jxmapviewer.viewer.GeoPosition
import util.MapHelper

/**
 * Pollution grid implementation based on the idw of the sensed value with `range` maximum distance
 */
open class ProtelisPollutionGrid(
    val sensors: List<Pair<GeoPosition, Double>>,
    val range: Double,
    val defaultValue: Double
) : PollutionGrid {

    override fun clean() { }

    override fun getPollutionLevel(position: GeoPosition): Double {
        val closeSensors = getCloseSensors(position)
        if (closeSensors.isEmpty()) {
            return defaultValue
        }
        closeSensors.find { it.first < MapHelper.DISTANCE_THRESHOLD_ROUNDING_ERROR }
            ?.let { return it.second }
        return closeSensors.map { it.second / it.first }.reduce { r1, r2 -> r1 + r2 } /
            closeSensors.map { 1 / it.first }.reduce { r1, r2 -> r1 + r2 }
    }

    protected open fun getCloseSensors(position: GeoPosition) = sensors
        .map { Pair(MapHelper.distanceMeter(it.first, position), it.second) }
        .filter { it.first < range }

    override fun addMeasurement(deviceEUI: Long, position: GeoPosition?, level: PollutionLevel?) { }
}
