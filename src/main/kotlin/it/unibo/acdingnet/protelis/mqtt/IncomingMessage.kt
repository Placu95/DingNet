package it.unibo.acdingnet.protelis.mqtt

import util.time.Time

data class IncomingMessage(
    val id: Long,
    val t_send: Time,
    val delay: Time,
    val msgSize: Int,
    var t_end: Time,
    val idTrigger: Long
) {
    companion object {
        private var id = 0L
        fun getNextId() = id++
    }
}
