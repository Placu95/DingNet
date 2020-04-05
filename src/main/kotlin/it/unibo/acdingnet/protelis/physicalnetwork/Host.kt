package it.unibo.acdingnet.protelis.physicalnetwork

import org.protelis.lang.datatype.DeviceUID

enum class HostType { CLOUD, EDGE, LEAF }

data class Host(
    val id: String,
    val type: HostType,
    val bandWidth: Int,
    var devices: Set<DeviceUID> = emptySet()
) {

    private val devicesRun: MutableMap<DeviceUID, Int> = mutableMapOf()

    fun getDevicesRuns() = devicesRun.toMap()

    fun addDevice(deviceUID: DeviceUID) { devices += deviceUID }

    fun removeDevice(deviceUID: DeviceUID) { devices -= deviceUID }

    fun addRun(deviceUID: DeviceUID) {
        devicesRun[deviceUID] = (devicesRun.getOrDefault(deviceUID, 0) + 1)
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
