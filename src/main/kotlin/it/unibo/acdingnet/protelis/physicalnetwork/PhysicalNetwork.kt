package it.unibo.acdingnet.protelis.physicalnetwork

import it.unibo.acdingnet.protelis.mqtt.IncomingMessage
import it.unibo.acdingnet.protelis.node.GenericNode
import it.unibo.acdingnet.protelis.physicalnetwork.configuration.Configuration
import it.unibo.acdingnet.protelis.util.skip
import org.protelis.lang.datatype.DeviceUID
import util.time.DoubleTime
import util.time.Time

class PhysicalNetwork(configuration: Configuration) {

    val hostBroker: Host
    val incomingQueue: MutableList<IncomingMessage> = mutableListOf()
    var sendingQueueFreeFrom: Time = DoubleTime.zero()
    var hosts: Set<Host>
    private set
    private val configurationNetwork = configuration.configurationNetwork

    init {
        val brokerConfig = configuration.brokerHostConfig
        val brokerId = brokerConfig.id ?: "host_broker"
        hostBroker = Host(brokerId, brokerConfig.type, brokerConfig.dataRate)

        hosts = configuration.hostsConfig.mapIndexed { i, it ->
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

    fun computeDelayPhysicalSmartphoneToProtelisNode(deviceUID: DeviceUID): Time {
        val delay = when (getHostByDevice(deviceUID).type) {
            HostType.SMARTPHONE -> configurationNetwork.dLocalhost
            HostType.CLOUD -> configurationNetwork.dsc
            else -> throw IllegalStateException()
        }
        // type is irrelevant
        NetworkStatistic.addDelay(delay, NetworkStatistic.Type.DOWNLOAD)
        return delay
    }

    fun computeRTTwithBrokerHost(deviceUID: DeviceUID) =
        computeRTTBetweenHosts(getHostByDevice(deviceUID), hostBroker)

    private fun getHostByDevice(deviceUID: DeviceUID): Host {
        val h = hosts.find { it.devices.contains(deviceUID) }
        h?.let { return it }
        if (hostBroker.devices.contains(deviceUID)) {
            return hostBroker
        } else {
            throw IllegalStateException("$deviceUID not found")
        }
    }

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

    enum class Type { UPLOAD, DOWNLOAD, DOWNLOAD_MAX }

    var delays: MutableMap<DeviceUID, MutableList<Pair<Type, Time>>> = mutableMapOf()
    private set

    var countUpload = 0
    var delayToTUpload: Time = DoubleTime.zero()
    var delayMaxUpload: Time = DoubleTime.zero()

    var countDownload = 0
    var delayToTDownload: Time = DoubleTime.zero()
    var delayMaxDownload: Time = DoubleTime.zero()

    var countDownloadMax = 0
    var delayToTDownloadMax: Time = DoubleTime.zero()
    var delayMaxDownloadMax: Time = DoubleTime.zero()

    fun addDelay(delay: Time, type: Type) {
/*
        delays[deviceUID] = (delays.getOrDefault(deviceUID, mutableListOf())
            .also { it.add(Pair(type, delay)) })
*/
        when (type) {
            Type.UPLOAD -> {
                countUpload++
                delayToTUpload += delay
                if (delay.isAfter(delayMaxUpload)) {
                    delayMaxUpload = delay
                }
            }
            Type.DOWNLOAD -> {
                countDownload++
                delayToTDownload += delay
                if (delay.isAfter(delayMaxDownload)) {
                    delayMaxDownload = delay
                }
            }
            Type.DOWNLOAD_MAX -> {
                countDownloadMax++
                delayToTDownloadMax += delay
                if (delay.isAfter(delayMaxDownloadMax)) {
                    delayMaxDownloadMax = delay
                }
            }
        }
    }

    fun reset() {
        countUpload = 0
        delayToTUpload = DoubleTime.zero()
        delayMaxUpload = DoubleTime.zero()
        countDownload = 0
        delayToTDownload = DoubleTime.zero()
        delayMaxDownload = DoubleTime.zero()
        countDownloadMax = 0
        delayToTDownloadMax = DoubleTime.zero()
        delayMaxDownloadMax = DoubleTime.zero()
    }
}
