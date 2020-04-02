package it.unibo.acdingnet.protelis.application

import application.Application
import iot.GlobalClock
import iot.networkentity.Mote
import org.jxmapviewer.JXMapViewer
import org.jxmapviewer.painter.Painter

abstract class ProtelisApplication(
    val motes: List<Mote>,
    val timer: GlobalClock,
    val protelisProgram: String,
    topics: List<String>
) : Application(topics) {

    abstract fun getPainters(): List<Painter<JXMapViewer>>
}
