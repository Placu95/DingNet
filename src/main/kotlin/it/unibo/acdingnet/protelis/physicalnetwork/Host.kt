package it.unibo.acdingnet.protelis.physicalnetwork

import org.protelis.lang.datatype.DeviceUID

enum class HostType { CLOUD, EDGE, SMARTPHONE }

data class Host(
    val id: String,
    val type: HostType,
    private val dataRate: Double? = null,
    var devices: Set<DeviceUID> = emptySet()
) {

    var numOfRuns: Int = 0
    private set

    fun getDataRate(): Double = checkNotNull(dataRate) { "data rate parameter is undefined" }

    fun addDevice(deviceUID: DeviceUID) { devices += deviceUID }

    fun removeDevice(deviceUID: DeviceUID) { devices -= deviceUID }

    fun addRun() {
        numOfRuns++
    }

    override fun equals(other: Any?): Boolean = when (other) {
        is Host -> id == other.id
        else -> false
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + type.hashCode()
        return result
    }
}
