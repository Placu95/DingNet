package it.unibo.acdingnet.protelis.util

import iot.GlobalClock
import it.unibo.acdingnet.protelis.executioncontext.SensorExecutionContext
import it.unibo.acdingnet.protelis.physicalnetwork.HostType
import it.unibo.acdingnet.protelis.physicalnetwork.NetworkStatistic
import it.unibo.acdingnet.protelis.physicalnetwork.PhysicalNetwork
import it.unibo.acdingnet.protelis.util.Utils.printTime
import org.apache.commons.math3.stat.descriptive.moment.Variance
import util.time.DoubleTime
import util.time.Time
import util.time.TimeUnit
import java.lang.Double.max
import java.lang.Double.min

data class Sampler(
    private val physicalNetwork: PhysicalNetwork,
    private val contexts: List<SensorExecutionContext>,
    private val clock: GlobalClock,
    private val samplingTime: Time
) {
    private val _samplings: MutableList<Sampling> = mutableListOf(Sampling.zero())
    private val variance = Variance()

    fun getSamplings() = _samplings.toList()

    companion object {
        fun header(timeUnit: TimeUnit) = Sampling.header(timeUnit)
    }

    fun start() {
        // add trigger for sampling
        clock.addPeriodicTrigger(samplingTime, samplingTime) {
            var maxTemp = Const.MIN_TEMP
            var minTemp = Const.MAX_TEMP
            var sumTemp = 0.0
            val temps = contexts.asSequence()
                .map { it.maxTemperatureAllowed }
                .onEach {
                    maxTemp = max(maxTemp, it)
                    minTemp = min(minTemp, it)
                    sumTemp += it
                }
                .toList().toDoubleArray()
            val avgTemp = Utils.roundToDecimal(sumTemp / contexts.size, 2)
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
                    (runs[HostType.SMARTPHONE] ?: 0),
                    minTemp,
                    maxTemp,
                    avgTemp,
                    Utils.roundToDecimal(variance.evaluate(temps, avgTemp), 2)
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
    val runOnSmartphoneTot: Int,
    val minTemp: Double,
    val maxTemp: Double,
    val avgTemp: Double,
    val varianceTemp: Double
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
        "$runOnCloudTot $runOnEdgeTot $runOnSmartphoneTot $minTemp $maxTemp $avgTemp $varianceTemp"

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
            "runOnCloudTot runOnEdgeTot runOnSmartphoneTot minTemp maxTemp avgTemp varianceTemp"

        fun zero() = Sampling(
            DoubleTime.zero(), 0, DoubleTime.zero(),
            DoubleTime.zero(), 0, DoubleTime.zero(), DoubleTime.zero(),
            0, DoubleTime.zero(),
            DoubleTime.zero(), 0, DoubleTime.zero(), DoubleTime.zero(),
            0, DoubleTime.zero(),
            DoubleTime.zero(), 0, DoubleTime.zero(), DoubleTime.zero(),
            0, 0, 0, 0, 0, 0, Const.MIN_TEMP, Const.MAX_TEMP, (Const.MIN_TEMP + Const.MAX_TEMP) / 2, 0.0)
    }
}
