package it.unibo.acdingnet.protelis.mqtt

import util.time.Time

data class IncomingMessage(
    val id: Long,
    val tSend: Time,
    val delay: Time,
    val msgSize: Int,
    var tEnd: Time,
    var idTrigger: Long,
    val triggerHandler: () -> Unit
) {
    companion object {
        private var id = 0L
        fun getNextId() = id++
    }
}
