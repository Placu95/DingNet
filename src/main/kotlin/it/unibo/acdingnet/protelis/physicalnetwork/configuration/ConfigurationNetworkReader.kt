package it.unibo.acdingnet.protelis.physicalnetwork.configuration

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.ConfigSpec
import it.unibo.acdingnet.protelis.physicalnetwork.HostType

data class ConfigurationNetwork(
    val gamma: Double,
    val beta: Double,
    val dee: Double,
    val dcc: Double,
    val dec: Double,
    val dsc: Double,
    val dHostBroker: Double
) {
    init {
        check(gamma in 0.0..1.0) { "gamma has to be in the range [0, 1], but it is: $gamma" }
        check(beta in 0.0..1.0) { "gamma has to be in the range [0, 1], but it is: $gamma" }
    }

    companion object : ConfigSpec("") {
        val configurationNetwork by required<ConfigurationNetwork>()
    }
}

data class BrokerHostConfig(
    val type: HostType,
    val bandwidth: Double,
    val id: String? = null
) {
    companion object : ConfigSpec("") {
        val brokerHostConfig by required<BrokerHostConfig>()
    }
}

data class HostConfig(
    val type: HostType,
    val bandwidth: Double? = null,
    val id: String? = null
) {
    companion object : ConfigSpec("") {
        val hostsConfig by required<List<HostConfig>>()
    }
}

class Reader(path: String) {

    val configurationNetwork: ConfigurationNetwork
    val brokerHostConfig: BrokerHostConfig
    val hostsConfig: List<HostConfig>

    init {
        val config = Config {
            addSpec(ConfigurationNetwork)
            addSpec(BrokerHostConfig)
            addSpec(HostConfig)
        }.from().toml.inputStream(Reader::class.java.getResourceAsStream(path))

        configurationNetwork = config[ConfigurationNetwork.configurationNetwork]
        brokerHostConfig = config[BrokerHostConfig.brokerHostConfig]
        hostsConfig = config[HostConfig.hostsConfig]
    }
}
