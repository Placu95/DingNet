package it.unibo.acdingnet.protelis.physicalnetwork.configuration

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.ConfigSpec
import it.unibo.acdingnet.protelis.physicalnetwork.HostType
import util.time.DoubleTime
import util.time.Time
import util.time.TimeUnit

data class ConfigurationNetwork(
    val seed: Long,
    val gamma: Double,
    val beta: Double,
    val dee: Time,
    val dcc: Time,
    val dec: Time,
    val dsc: Time,
    val dLocalhost: Time
) {
    init {
        check(gamma in 0.0..1.0) { "gamma has to be in the range [0, 1], but it is: $gamma" }
        check(beta in 0.0..1.0) { "gamma has to be in the range [0, 1], but it is: $gamma" }
    }

    companion object : ConfigSpec("configurationNetwork") {
        private val seed by required<Long>()
        private val gamma by required<Double>()
        private val beta by required<Double>()
        private val dee by required<Double>()
        private val dcc by required<Double>()
        private val dec by required<Double>()
        private val dsc by required<Double>()
        private val dLocalhost by required<Double>()

        fun read(config: Config) = ConfigurationNetwork(
            config[gamma], config[beta], toTime(config[dee]), toTime(config[dcc]),
            toTime(config[dec]), toTime(config[dsc]), toTime(config[dLocalhost])
        )

        private fun toTime(t: Double) = DoubleTime(t, TimeUnit.MILLIS)
    }
}

data class BrokerHostConfig(
    val type: HostType,
    val dataRate: Double,
    val id: String? = null
) {
    companion object : ConfigSpec("") {
        val brokerHostConfig by required<BrokerHostConfig>()
    }
}

data class HostConfig(
    val type: HostType,
    val dataRate: Double? = null,
    val id: String? = null
) {
    companion object : ConfigSpec("") {
        val hostsConfig by required<List<HostConfig>>()
    }
}

class Configuration(path: String) {

    val configurationNetwork: ConfigurationNetwork
    val brokerHostConfig: BrokerHostConfig
    val hostsConfig: List<HostConfig>

    init {
        val config = Config {
            addSpec(ConfigurationNetwork)
            addSpec(BrokerHostConfig)
            addSpec(HostConfig)
        }.from().toml.inputStream(Configuration::class.java.getResourceAsStream(path))

        configurationNetwork = ConfigurationNetwork.read(config)
        brokerHostConfig = config[BrokerHostConfig.brokerHostConfig]
        hostsConfig = config[HostConfig.hostsConfig]
    }
}
