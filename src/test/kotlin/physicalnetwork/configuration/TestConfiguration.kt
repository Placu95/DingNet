package physicalnetwork.configuration

import io.kotlintest.matchers.collections.shouldContainAll
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import it.unibo.acdingnet.protelis.physicalnetwork.HostType
import it.unibo.acdingnet.protelis.physicalnetwork.configuration.BrokerHostConfig
import it.unibo.acdingnet.protelis.physicalnetwork.configuration.Configuration
import it.unibo.acdingnet.protelis.physicalnetwork.configuration.ConfigurationNetwork
import it.unibo.acdingnet.protelis.physicalnetwork.configuration.HostConfig
import util.time.DoubleTime
import util.time.TimeUnit

class TestConfiguration : StringSpec() {

    private val configFilePath = "/physicalnetwork/testFile.toml"
    private val configurationReader = Configuration(configFilePath)

    init {
        "test reading of the free variables" {
            val configurationNetwork = ConfigurationNetwork(1L, 0.1, 0.2, toTime(1.0),
                toTime(2.0), toTime(3.0), toTime(4.0), toTime(5.0))
            configurationReader.configurationNetwork shouldBe configurationNetwork
        }

        "test reading of broker host configuration" {
            val brokerHostConfig = BrokerHostConfig(HostType.CLOUD, 43e6)
            configurationReader.brokerHostConfig shouldBe brokerHostConfig
        }

        "test reading of hosts' configuration" {
            val hosts = listOf(
                HostConfig(HostType.CLOUD, id = "c000"),
                HostConfig(HostType.EDGE, dataRate = 10.0)
            )

            configurationReader.hostsConfig.size shouldBe hosts.size
            configurationReader.hostsConfig shouldContainAll hosts
        }
    }

    fun toTime(t: Double) = DoubleTime(t, TimeUnit.MILLIS)
}
