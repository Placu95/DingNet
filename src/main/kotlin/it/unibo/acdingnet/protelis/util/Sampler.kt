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
    private val samplingTime: Time
) {
    private val _samplings: MutableList<Sampling> = mutableListOf(Sampling.zero())

    fun getSamplings() = _samplings.toList()

    companion object {
        fun header(timeUnit: TimeUnit) = Sampling.header(timeUnit)
    }

    fun start() {
        // add trigger for sampling
        clock.addPeriodicTrigger(samplingTime, samplingTime) {
            val lastSample = _samplings.last()
            val runs = physicalNetwork
                .hosts
                .groupBy { it.type }
                .map { it.key to it.value.map { h -> h.numOfRuns }.reduce(Int::plus) }
                .toMap()
            _samplings.add(
                Sampling(
                    clock.time,
                    NetworkStatistic.countUpload,
                    NetworkStatistic.delayToTUpload,
                    NetworkStatistic.delayMaxUpload,
                    lastSample.commCountUploadTot + NetworkStatistic.countUpload,
                    lastSample.delaySumUploadTot + NetworkStatistic.delayToTUpload,
                    Utils.maxTime(lastSample.delayMaxUploadTot, NetworkStatistic.delayMaxUpload),
                    NetworkStatistic.countDownload,
                    NetworkStatistic.delayToTDownload,
                    NetworkStatistic.delayMaxDownload,
                    lastSample.commCountDownloadTot + NetworkStatistic.countDownload,
                    lastSample.delaySumDownloadTot + NetworkStatistic.delayToTDownload,
                    Utils.maxTime(
                        lastSample.delayMaxDownloadTot, NetworkStatistic.delayMaxDownload),
                    NetworkStatistic.countDownloadMax,
                    NetworkStatistic.delayToTDownloadMax,
                    NetworkStatistic.delayMaxDownloadMax,
                    lastSample.commCountDownloadMaxTot + NetworkStatistic.countDownloadMax,
                    lastSample.delaySumDownloadMaxTot + NetworkStatistic.delayToTDownloadMax,
                    Utils.maxTime(
                        lastSample.delayMaxDownloadMaxTot, NetworkStatistic.delayMaxDownloadMax),
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
    val commCountUploadPartial: Int,
    val delaySumUploadPartial: Time,
    val delayMaxUploadPartial: Time,
    val commCountUploadTot: Int,
    val delaySumUploadTot: Time,
    val delayMaxUploadTot: Time,
    val commCountDownloadPartial: Int,
    val delaySumDownloadPartial: Time,
    val delayMaxDownloadPartial: Time,
    val commCountDownloadTot: Int,
    val delaySumDownloadTot: Time,
    val delayMaxDownloadTot: Time,
    val commCountDownloadMaxPartial: Int,
    val delaySumDownloadMaxPartial: Time,
    val delayMaxDownloadMaxPartial: Time,
    val commCountDownloadMaxTot: Int,
    val delaySumDownloadMaxTot: Time,
    val delayMaxDownloadMaxTot: Time,
    val runOnCloudPartial: Int,
    val runOnEdgePartial: Int,
    val runOnSmartphonePartial: Int,
    val runOnCloudTot: Int,
    val runOnEdgeTot: Int,
    val runOnSmartphoneTot: Int
) {
    fun print(tU: TimeUnit) = "${printTime(instant, tU)} " +
        "$commCountUploadPartial ${printTime(delaySumUploadPartial, tU)} " +
        "${printTime(delayMaxUploadPartial, tU)} $commCountUploadTot " +
        "${printTime(delaySumUploadTot, tU)} ${printTime(delayMaxUploadTot, tU)} " +
        "$commCountDownloadPartial ${printTime(delaySumDownloadPartial, tU)} " +
        "${printTime(delayMaxDownloadPartial, tU)} $commCountDownloadTot " +
        "${printTime(delaySumDownloadTot, tU)} ${printTime(delayMaxDownloadTot, tU)} " +
        "$commCountDownloadMaxPartial ${printTime(delaySumDownloadMaxPartial, tU)} " +
        "${printTime(delayMaxDownloadMaxPartial, tU)} $commCountDownloadMaxTot " +
        "${printTime(delaySumDownloadMaxTot, tU)} ${printTime(delayMaxDownloadMaxTot, tU)} " +
        "$runOnCloudPartial $runOnEdgePartial $runOnSmartphonePartial " +
        "$runOnCloudTot $runOnEdgeTot $runOnSmartphoneTot"

    private fun printTime(t: Time, tU: TimeUnit) = Utils.roundToDecimal(t.getAs(tU), 2).toString()

    companion object {
        fun header(timeUnit: TimeUnit) = "instant[${timeUnit.name}] " +
            "commCountUploadPartial delaySumUploadPartial[${timeUnit.name}] " +
            "delayMaxUploadPartial[${timeUnit.name}] commCountUploadTot " +
            "delaySumUploadTot[${timeUnit.name}] delayMaxUploadTot[${timeUnit.name}] " +
            "commCountDownloadPartial delaySumDownloadPartial[${timeUnit.name}] " +
            "delayMaxDownloadPartial[${timeUnit.name}] commCountDownloadTot " +
            "delaySumDownloadTot[${timeUnit.name}] delayMaxDownloadTot[${timeUnit.name}] " +
            "commCountDownloadMaxPartial delaySumDownloadMaxPartial[${timeUnit.name}] " +
            "delayMaxDownloadMaxPartial[${timeUnit.name}] commCountDownloadMaxTot " +
            "delaySumDownloadMaxTot[${timeUnit.name}] delayMaxDownloadMaxTot[${timeUnit.name}] " +
            "runOnCloudPartial runOnEdgePartial runOnSmartphonePartial " +
            "runOnCloudTot runOnEdgeTot runOnSmartphoneTot"

        fun zero() = Sampling(
            DoubleTime.zero(), 0, DoubleTime.zero(),
            DoubleTime.zero(), 0, DoubleTime.zero(), DoubleTime.zero(),
            0, DoubleTime.zero(),
            DoubleTime.zero(), 0, DoubleTime.zero(), DoubleTime.zero(),
            0, DoubleTime.zero(),
            DoubleTime.zero(), 0, DoubleTime.zero(), DoubleTime.zero(),
            0, 0, 0, 0, 0, 0)
    }
}
