package it.unibo.acdingnet.protelis.mqtt

import util.time.Time

data class IncomingMessage(
    val id: Long,
    val t_send: Time,
    val delay: Time,
    val msgSize: Int,
    var t_end: Time,
    var idTrigger: Long,
    val triggerHandler: () -> Unit
) {
    companion object {
        private var id = 0L
        fun getNextId() = id++
    }
}
