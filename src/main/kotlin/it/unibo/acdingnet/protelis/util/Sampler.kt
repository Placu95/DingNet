package it.unibo.acdingnet.protelis.util

import iot.GlobalClock
import it.unibo.acdingnet.protelis.physicalnetwork.HostType
import it.unibo.acdingnet.protelis.physicalnetwork.NetworkStatistic
import it.unibo.acdingnet.protelis.physicalnetwork.PhysicalNetwork
import util.time.DoubleTime
import util.time.Time
import util.time.TimeUnit

data class Sampler(
    private val physicalNetwork: PhysicalNetwork,
    private val clock: GlobalClock,
    private val samplingTime: Long
) {
    private val _samplings: MutableList<Sampling> = mutableListOf(Sampling.zero())

    fun getSamplings() = _samplings.toList()

    companion object {
        fun header(timeUnit: TimeUnit) = Sampling.header(timeUnit)
    }

    fun start() {
        // add trigger for sampling
        clock.addPeriodicTrigger(
            DoubleTime(samplingTime.toDouble(), TimeUnit.SECONDS),
            samplingTime) {
            val lastSample = _samplings.last()
            val runs = physicalNetwork
                .hosts
                .groupBy { it.type }
                .map { it.key to it.value.map { h -> h.numOfRuns }.reduce(Int::plus) }
                .toMap()
            _samplings.add(
                Sampling(
                    clock.time,
                    NetworkStatistic.count,
                    NetworkStatistic.delayToT,
                    NetworkStatistic.delayMax,
                    lastSample.delayCountTot + NetworkStatistic.count,
                    lastSample.delaySumTot + NetworkStatistic.delayToT,
                    Utils.maxTime(lastSample.delayMaxTot, NetworkStatistic.delayMax),
                    (runs[HostType.CLOUD] ?: 0) - lastSample.runOnCloudTot,
                    (runs[HostType.EDGE] ?: 0) - lastSample.runOnEdgeTot,
                    (runs[HostType.SMARTPHONE] ?: 0) - lastSample.runOnSmartphoneTot,
                    (runs[HostType.CLOUD] ?: 0),
                    (runs[HostType.EDGE] ?: 0),
                    (runs[HostType.SMARTPHONE] ?: 0)
                )
            )
            NetworkStatistic.reset()
        }
    }
}

data class Sampling(
    val instant: Time,
    val delayCountPartial: Int,
    val delaySumPartial: Time,
    val delayMaxPartial: Time,
    val delayCountTot: Int,
    val delaySumTot: Time,
    val delayMaxTot: Time,
    val runOnCloudPartial: Int,
    val runOnEdgePartial: Int,
    val runOnSmartphonePartial: Int,
    val runOnCloudTot: Int,
    val runOnEdgeTot: Int,
    val runOnSmartphoneTot: Int
) {
    fun print(tU: TimeUnit) = "${printTime(instant, tU)} $delayCountPartial " +
        "${printTime(delaySumPartial, tU)} ${printTime(delayMaxPartial, tU)} $delayCountTot " +
        "${printTime(delaySumTot, tU)} ${printTime(delayMaxTot, tU)} $runOnCloudPartial " +
        "$runOnEdgePartial $runOnSmartphonePartial $runOnCloudTot $runOnEdgeTot " +
        "$runOnSmartphoneTot"

    private fun printTime(t: Time, tU: TimeUnit) = Utils.roundToDecimal(t.getAs(tU), 2).toString()

    companion object {
        fun header(timeUnit: TimeUnit) = "instant[${timeUnit.name}] delayCountPartial " +
            "delaySumPartial[${timeUnit.name}] delayMaxPartial[${timeUnit.name}] " +
            "delayCountTot delaySumTot[${timeUnit.name}] delayMaxTot[${timeUnit.name}] " +
            "runOnCloudPartial runOnEdgePartial runOnSmartphonePartial runOnCloudTot " +
            "runOnEdgeTot runOnSmartphoneTot"

        fun zero() = Sampling(
            DoubleTime.zero(), 0, DoubleTime.zero(),
            DoubleTime.zero(), 0, DoubleTime.zero(), DoubleTime.zero(),
            0, 0, 0, 0, 0, 0)
    }
}
