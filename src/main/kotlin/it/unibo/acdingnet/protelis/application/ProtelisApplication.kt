package it.unibo.acdingnet.protelis.application

import application.Application
import iot.GlobalClock
import iot.networkentity.Mote
import org.jxmapviewer.JXMapViewer
import org.jxmapviewer.painter.Painter

abstract class ProtelisApplication(
    val motes: List<Mote>,
    val timer: GlobalClock,
    protelisProgram: String,
    topics: List<String>
) : Application(topics) {

    val protelisProgramResource: String = getProgram(protelisProgram)

    private fun getProgram(path: String) = ProtelisApplication::class.java.getResourceAsStream(path).bufferedReader().readText()

    abstract fun getPainters(): List<Painter<JXMapViewer>>
}
