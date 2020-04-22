package physicalnetwork.configuration

import io.kotlintest.matchers.doubles.shouldBeGreaterThan
import io.kotlintest.matchers.doubles.shouldBeLessThan
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import iot.GlobalClock
import it.unibo.acdingnet.protelis.mqtt.MqttMockSerWithDelay
import it.unibo.acdingnet.protelis.physicalnetwork.HostType
import it.unibo.acdingnet.protelis.physicalnetwork.NetworkStatistic
import it.unibo.acdingnet.protelis.physicalnetwork.PhysicalNetwork
import it.unibo.acdingnet.protelis.physicalnetwork.configuration.Configuration
import org.protelis.lang.datatype.impl.StringUID

class TestNetworkDelay : StringSpec() {

    private val configFilePath = "/physicalnetwork/testFile.toml"
    private val configurationReader = Configuration(configFilePath)
    private val clock = GlobalClock()
    private val network = PhysicalNetwork(configurationReader, clock)

    init {
        "test network delay" {
            val id1 = StringUID("1")
            val id2 = StringUID("2")
            network.addDeviceTo(id1, HostType.EDGE)
            network.addDeviceTo(id2, HostType.CLOUD)
            val client1 = MqttMockSerWithDelay(network, clock, id1)
            val client2 = MqttMockSerWithDelay(network, clock, id2)
            client1.subscribe(client1, "test/$id2", String::class.java) { _, _ -> }
            client2.subscribe(client2, "test/$id1", String::class.java) { _, _ -> }
            client1.publish("test/$id1", generateMsg(10))
            clock.tick(1)
            client2.publish("test/$id2", generateMsg(15))
            clock.tick(100)
            NetworkStatistic.delays.forEach { (_, u) -> u.size shouldBe 2 }
            checkDelay(id1, NetworkStatistic.Type.UPLOAD, 3.0, 4.0)
            checkDelay(id1, NetworkStatistic.Type.DOWNLOAD, 3.0, 4.0)
            checkDelay(id2, NetworkStatistic.Type.UPLOAD, 4.0, 5.0)
            checkDelay(id2, NetworkStatistic.Type.DOWNLOAD, 2.0, 3.0)
        }
    }

    private fun checkDelay(
        id: StringUID,
        type: NetworkStatistic.Type,
        lowerBound: Double,
        upperBound: Double
    ) {
        NetworkStatistic.delays[id]
            ?.find { it.first == type }
            ?.second?.asMilli()
            ?.let {
                it shouldBeGreaterThan lowerBound
                it shouldBeLessThan upperBound
            }
    }

    private fun generateMsg(numChar: Int): String {
        var s = ""
        for (i in 1..numChar) {
            s += "a"
        }
        return s
    }
}
