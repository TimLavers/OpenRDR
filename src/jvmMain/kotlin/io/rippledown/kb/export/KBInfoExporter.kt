package io.rippledown.kb.export

import io.rippledown.model.KBInfo

class KBInfoExporter(private val exportFile: ExportFile, private val kbInfo: KBInfo) {

    fun export() {
        val writer = exportFile.writer()
        writer.write(kbInfo.id)
        writer.newLine()
        writer.write(kbInfo.name)
        writer.newLine()
        writer.close()
    }
}