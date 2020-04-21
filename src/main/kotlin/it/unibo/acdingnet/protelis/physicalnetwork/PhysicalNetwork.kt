package it.unibo.acdingnet.protelis.physicalnetwork

import iot.GlobalClock
import it.unibo.acdingnet.protelis.node.GenericNode
import it.unibo.acdingnet.protelis.physicalnetwork.configuration.Reader
import it.unibo.acdingnet.protelis.util.skip
import org.protelis.lang.datatype.DeviceUID
import util.time.DoubleTime
import util.time.Time
import util.time.TimeUnit

class PhysicalNetwork(reader: Reader, private val clock: GlobalClock) {

    private val hostBroker: Host
    private var receivingQueueFreeFrom: Time = DoubleTime.zero()
    private var sendingQueueFreeFrom: Time = DoubleTime.zero()
    private var hosts: Set<Host>
    private val configurationNetwork = reader.configurationNetwork

    init {
        val brokerConfig = reader.brokerHostConfig
        val brokerId = brokerConfig.id ?: "host_broker"
        hostBroker = Host(brokerId, brokerConfig.type, brokerConfig.dataRate)

        hosts = reader.hostsConfig.mapIndexed { i, it ->
            Host(it.id ?: "${it.type}_$i", it.type, it.dataRate)
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

    fun arrivalTimeToBroker(deviceUID: DeviceUID, messageLenght: Int): Time {
        val time = computeArrivalTime(getHostByDevice(deviceUID), hostBroker,
            receivingQueueFreeFrom, messageLenght)
        receivingQueueFreeFrom = time
        NetworkStatistic.addDelay(deviceUID, time - clock.time, NetworkStatistic.Type.UPLOAD)
        return time
    }

    fun arrivalTimeToSubscriber(deviceUID: DeviceUID, messageLenght: Int): Time {
        val time = computeArrivalTime(hostBroker, getHostByDevice(deviceUID),
            sendingQueueFreeFrom, messageLenght)
        sendingQueueFreeFrom = time
        NetworkStatistic.addDelay(deviceUID, time - clock.time, NetworkStatistic.Type.DOWNLOAD)
        return time
    }

    fun computeDelayPhysicalSmartphoneToProtelisNode(deviceUID: DeviceUID): Time {
        val delay = when (getHostByDevice(deviceUID).type) {
            HostType.SMARTPHONE -> configurationNetwork.dLocalhost
            HostType.CLOUD -> configurationNetwork.dsc
            else -> throw IllegalStateException()
        }
        // type is irrelevant
        NetworkStatistic.addDelay(deviceUID, delay, NetworkStatistic.Type.DOWNLOAD)
        return delay
    }

    private fun getHostByDevice(deviceUID: DeviceUID): Host {
        val h = hosts.find { it.devices.contains(deviceUID) }
        h?.let { return it }
        if (hostBroker.devices.contains(deviceUID)) {
            return hostBroker
        } else {
            throw IllegalStateException("$deviceUID not found")
        }
    }

    private fun computeArrivalTime(h1: Host, h2: Host, queueTime: Time, messageLength: Int): Time =
        maxTime(queueTime, clock.time) +
            DoubleTime(messageLength / hostBroker.getDataRate(), TimeUnit.SECONDS) +
            (computeRTTBetweenHosts(h1, h2))

    private fun maxTime(t1: Time, t2: Time) = if (t1.isAfter(t2)) t1 else t2

    private fun computeRTTBetweenHosts(h1: Host, h2: Host): Time {
        if (h1 == h2) {
            return configurationNetwork.dLocalhost
        }
        return when (h1.type) {
            HostType.CLOUD -> when (h2.type) {
                HostType.CLOUD -> configurationNetwork.dcc
                HostType.EDGE -> configurationNetwork.dec
                HostType.SMARTPHONE -> configurationNetwork.dsc
            }
            HostType.EDGE -> when (h2.type) {
                HostType.CLOUD -> configurationNetwork.dec
                HostType.EDGE -> configurationNetwork.dee
                HostType.SMARTPHONE -> configurationNetwork.dsc + configurationNetwork.dec
            }
            HostType.SMARTPHONE -> when (h2.type) {
                HostType.CLOUD -> configurationNetwork.dsc
                HostType.EDGE -> configurationNetwork.dsc + configurationNetwork.dec
                else -> throw IllegalStateException()
            }
        }
    }
}

object NetworkStatistic {

    enum class Type { UPLOAD, DOWNLOAD }

    var delays: Map<DeviceUID, List<Pair<Type, Time>>> = emptyMap()
    private set

    fun addDelay(deviceUID: DeviceUID, delay: Time, type: Type) {
        delays += deviceUID to (delays.getOrDefault(deviceUID, emptyList()) + Pair(type, delay))
    }
}
