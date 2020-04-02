package it.unibo.acdingnet.protelis.physicalnetwork

import org.protelis.lang.datatype.DeviceUID
import util.time.DoubleTime
import util.time.Time
import util.time.TimeUnit

class PhysicalNetwork(val devices: Set<DeviceUID> = emptySet()) {

    private var hostBroker: Host = Host("1", HostType.EDGE, 125)
    private var devicesHost: Map<DeviceUID, Host> = emptyMap()

    fun delayToPublish(deviceUID: DeviceUID): Time =
        computeDelay(checkNotNull(devicesHost[deviceUID]), hostBroker)

    fun delayToReceive(deviceUID: DeviceUID): Time =
        computeDelay(hostBroker, checkNotNull(devicesHost[deviceUID]))

    private fun computeDelay(from: Host, to: Host): Time {
        if (from == to) {
            return DoubleTime(0.5, TimeUnit.MILLIS)
        }
        var delay = computeDelay(from)

        // TODO finish to compute delay
        return delay
    }

    private fun computeDelay(from: Host): Time = TODO()

}
