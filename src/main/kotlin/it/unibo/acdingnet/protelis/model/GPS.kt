package it.unibo.acdingnet.protelis.model

import util.time.Time

data class GPSPosition(val position: LatLongPosition, val time: Time) : Comparable<GPSPosition> {

    override fun compareTo(other: GPSPosition): Int = time.asMilli().compareTo(other.time.asMilli())
}

data class GPSTrace(val positions: List<GPSPosition>)
