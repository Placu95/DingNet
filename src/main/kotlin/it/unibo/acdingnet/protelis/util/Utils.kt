package it.unibo.acdingnet.protelis.util

import org.protelis.lang.datatype.Tuple
import org.protelis.lang.datatype.impl.ArrayTupleImpl
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
}
