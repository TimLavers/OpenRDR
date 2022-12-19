package io.rippledown.kb.export

import io.rippledown.kb.KB
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter

class KBExporter(private val destination: File, val kb: KB) {
    init {
        checkDirectoryIsSuitableForExport(destination, "KB")
    }

    fun export() {
        // Details of the KB.
        val kbDetailsFile = File(destination, "Details.txt")
        val writer = BufferedWriter(FileWriter(kbDetailsFile))
        writer.write(kb.name)
        writer.newLine()
        writer.close()

        // Case view.
        val attributesFile = File(destination, "CaseView.txt")
        CaseViewExporter(attributesFile, kb.caseViewManager.allAttributesInOrder())

        // Cases.
        val casesDirectory = File(destination, "Cases")
        CaseExporter(casesDirectory, kb.allCases()).export()

        // Rules.
        val rulesDirectory = File(destination, "Rules")
        RuleExporter(rulesDirectory, kb.ruleTree)
    }
}