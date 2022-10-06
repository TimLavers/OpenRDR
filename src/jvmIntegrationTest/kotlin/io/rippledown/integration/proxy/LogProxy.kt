package io.rippledown.integration.proxy

import java.io.File
import java.nio.file.Paths

class LogProxy(val logDir: File) {
    val logFile: File = Paths.get(logDir.path, "server.log").toFile()

    fun contains(string: String) = logFile.exists() && contents().contains(string)

    fun contents() = logFile.readText()

    fun linesContaining(string: String) = logFile.readLines()
        .filter { it.contains(string) }

    fun deleteLogfiles() {
        logDir.listFiles()?.forEach { it.delete() }
    }

}