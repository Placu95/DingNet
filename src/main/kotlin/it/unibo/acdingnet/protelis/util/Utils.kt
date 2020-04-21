package it.unibo.acdingnet.protelis.util

import java.math.RoundingMode

object Utils {

    @JvmOverloads
    @JvmStatic
    fun roundToDecimal(value: Double, numOfDecimal: Int = 1) = value.toBigDecimal()
        .setScale(numOfDecimal, RoundingMode.HALF_EVEN).toDouble()
}
