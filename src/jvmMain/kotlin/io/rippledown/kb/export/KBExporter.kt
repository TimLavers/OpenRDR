package io.rippledown.kb.export

import io.rippledown.kb.KB
import java.io.File

open class KBExportImport(val destination: File) {
    val kbDetailsFile = File(destination, "Details.txt")
    val attributesFile = File(destination, "Attributes.txt")
    val caseViewFile = File(destination, "CaseView.txt")
    val casesDirectory = File(destination, "Cases")
    val rulesDirectory = File(destination, "Rules")
}
class KBExporter(destination: File, val kb: KB): KBExportImport(destination) {
    init {
        checkDirectoryIsSuitableForExport(destination, "KB")
    }

    fun export() {
        // Details of the KB.
        KBInfoExporter(ExportFile(kbDetailsFile, "KBInfo"), kb.name).export()

        // Attributes.
        AttributesExporter(attributesFile, kb.attributeManager.all()).export()

        // Case view.
        CaseViewExporter(caseViewFile, kb.caseViewManager.allAttributesInOrder()).export()

        // Cases.
        casesDirectory.mkdirs()
        CaseExporter(casesDirectory, kb.allCases()).export()

        // Rules.
        rulesDirectory.mkdirs()
        RuleExporter(rulesDirectory, kb.ruleTree).export()
    }
}