package io.rippledown.server

import io.rippledown.model.CasesInfo
import java.io.File

class ServerApplication {
    val casesDir = File("temp/cases")

    init {
        casesDir.mkdirs()
    }

    fun waitingCasesInfo(): CasesInfo {
        val caseFiles = casesDir.listFiles()
        val casesWaiting = caseFiles?.size ?: 0
        return CasesInfo(casesWaiting, casesDir.absolutePath)
    }
}