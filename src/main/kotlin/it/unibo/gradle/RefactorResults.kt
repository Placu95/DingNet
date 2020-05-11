@file:JvmName("RefactorResults")
package it.unibo.gradle

import java.io.File

fun main(args: Array<String>) {
    val outputDir = args[0]

    File(outputDir)
        .listFiles()
        .forEach {
            val newText = it.readText().replace("[MILLIS]", "")
            it.printWriter()
                .use { w -> w.println(newText) }
        }
}
