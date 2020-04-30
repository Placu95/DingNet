@file:JvmName("SearchMissingFiles")
package it.unibo.gradle

import it.unibo.acdingnet.protelis.physicalnetwork.configuration.Configuration
import util.time.TimeUnit
import java.io.File

fun main(args: Array<String>) {
    val configDir = args[0]
    val outputDir = args[1]

    val results = File(outputDir)
        .listFiles()
        .map { it.name }

    File(configDir)
        .listFiles()
        .filter { it.extension == "toml" }
        .map { Pair(Configuration(it.absolutePath), it.name) }
        .map {
            Pair(
                it.first.configurationNetwork.print(TimeUnit.MILLIS) +
                    ", hostBroker = ${it.first.brokerHostConfig.type}",
                it.second)
        }
        .map { Pair("sim_" + it.first.replace(" = ", "-").replace(", ", "_"), it.second) }
        .filter { (results.find { r -> r.startsWith(it.first) } == null) }
        .forEach { println(it.second) }
}
