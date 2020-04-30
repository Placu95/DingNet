package it.unibo.acdingnet.protelis.util

import org.protelis.lang.datatype.Tuple
import org.protelis.lang.datatype.impl.ArrayTupleImpl
import util.time.Time
import util.time.TimeUnit
import java.math.RoundingMode

object Utils {

    @JvmOverloads
    @JvmStatic
    fun roundToDecimal(value: Double, numOfDecimal: Int = 1) = value.toBigDecimal()
        .setScale(numOfDecimal, RoundingMode.HALF_EVEN).toDouble()

    @JvmStatic
    fun distinctByUID(tuple: Tuple): Tuple {
        var ret: Tuple = ArrayTupleImpl()
        tuple.distinctBy { (it as ArrayTupleImpl)[0] }.forEach { ret = ret.append(it) }
        return ret
    }

    fun maxTime(t1: Time, t2: Time) = if (t1.isAfter(t2)) t1 else t2

    fun printTime(t: Time, tU: TimeUnit) = Utils.roundToDecimal(t.getAs(tU), 2).toString()
}
