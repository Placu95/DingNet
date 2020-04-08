package physicalnetwork.configuration

import io.kotlintest.matchers.collections.shouldContainAll
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import it.unibo.acdingnet.protelis.physicalnetwork.HostType
import it.unibo.acdingnet.protelis.physicalnetwork.configuration.BrokerHostConfig
import it.unibo.acdingnet.protelis.physicalnetwork.configuration.ConfigurationNetwork
import it.unibo.acdingnet.protelis.physicalnetwork.configuration.HostConfig
import it.unibo.acdingnet.protelis.physicalnetwork.configuration.Reader

class TestConfigurationReader: StringSpec() {

    private val configFilePath = "/physicalnetwork/testFile.toml"
    private val configurationReader = Reader(configFilePath)

    init {
        "test reading of the free variables" {
            val configurationNetwork = ConfigurationNetwork(0.1, 0.2, 1.0, 2.0, 3.0, 4.0, 5.0)
            configurationReader.configurationNetwork shouldBe configurationNetwork
        }

        "test reading of broker host configuration" {
            val brokerHostConfig = BrokerHostConfig(HostType.CLOUD, 125.0)
            configurationReader.brokerHostConfig shouldBe brokerHostConfig
        }

        "test reading of hosts' configuration" {
            val hosts = listOf(
                HostConfig(HostType.CLOUD, id = "c000"),
                HostConfig(HostType.EDGE, bandwidth = 10.0)
            )

            configurationReader.hostsConfig.size shouldBe hosts.size
            configurationReader.hostsConfig shouldContainAll hosts
        }
    }
}
