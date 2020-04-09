package it.unibo.acdingnet.protelis.physicalnetwork

import it.unibo.acdingnet.protelis.node.GenericNode
import it.unibo.acdingnet.protelis.physicalnetwork.configuration.Reader
import it.unibo.acdingnet.protelis.util.skip
import org.protelis.lang.datatype.DeviceUID
import util.time.DoubleTime
import util.time.Time
import util.time.TimeUnit

class PhysicalNetwork(reader: Reader) {

    private val hostBroker: Host
    private var hosts: Set<Host> = emptySet()
    private val configurationNetwork = reader.configurationNetwork

    init {
        val brokerConfig = reader.brokerHostConfig
        val brokerId = brokerConfig.id ?: "host_broker"
        hostBroker = Host(brokerId, brokerConfig.type, brokerConfig.bandwidth)

        hosts = reader.hostsConfig.mapIndexed { i, it ->
            Host(it.id ?: "${it.type}_$i", it.type, it.bandwidth)
        }.toSet()
    }

    fun addNodes(edgeNodes: Collection<GenericNode>, nodes: Collection<GenericNode>) {
        val cloud by lazy { hostByType(HostType.CLOUD) }
        val edge by lazy { hostByType(HostType.EDGE) }

        val deviceOnEdge = (configurationNetwork.gamma * edgeNodes.size).toInt()
        edgeNodes.take(deviceOnEdge).forEach { it.host = edge }
        edgeNodes.skip(deviceOnEdge).forEach { it.host = cloud }

        val deviceOnSmartphone = (configurationNetwork.beta * nodes.size).toInt()
        nodes.take(deviceOnSmartphone).forEach {
            val host = Host(
                "${HostType.SMARTPHONE.toString().toLowerCase()}_${it.deviceUID}",
                HostType.SMARTPHONE
            )
            it.host = host
            hosts += host
        }
        nodes.skip(deviceOnSmartphone).forEach { it.host = cloud }
    }

    fun addDeviceTo(deviceUID: DeviceUID, hostType: HostType) =
        hostByType(hostType).addDevice(deviceUID)

    fun addDeviceToBrokerHost(deviceUID: DeviceUID) = hostBroker.addDevice(deviceUID)

    private fun hostByType(hostType: HostType) = checkNotNull(hosts.find { it.type == hostType })

    fun delayToPublish(deviceUID: DeviceUID): Time =
        computeDelay(getHostByDevice(deviceUID), hostBroker)

    fun delayToReceive(deviceUID: DeviceUID): Time =
        computeDelay(hostBroker, getHostByDevice(deviceUID))

    private fun getHostByDevice(deviceUID: DeviceUID) =
        checkNotNull(hosts.find { it.devices.contains(deviceUID) })

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
