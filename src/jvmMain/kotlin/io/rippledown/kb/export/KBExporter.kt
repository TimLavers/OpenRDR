package io.rippledown.kb.export

import io.rippledown.kb.KB
import java.io.File

open class KBExportImport(val destination: File) {
    val kbDetailsFile = File(destination, "Details.txt")
    val attributesFile = File(destination, "Attributes.txt")
    val caseViewFile = File(destination, "CaseView.txt")
    val cornerstoneCasesDirectory = File(destination, "CornerstoneCases")
    val processedCasesDirectory = File(destination, "ProcessedCases")
    val conclusionsDirectory = File(destination, "Conclusions")
    val conditionsDirectory = File(destination, "Conditions")
    val rulesDirectory = File(destination, "Rules")
}
class KBExporter(destination: File, val kb: KB): KBExportImport(destination) {
    init {
        checkDirectoryIsSuitableForExport(destination, "KB")
    }

    fun export() {
        // Details of the KB.
        KBInfoExporter(ExportFile(kbDetailsFile, "KBInfo"), kb.kbInfo).export()

        // Attributes.
        AttributesExporter(attributesFile, kb.attributeManager.all()).export()

        // Case view.
        CaseViewExporter(caseViewFile, kb.caseViewManager.allAttributesInOrder()).export()

        // Conclusions.
        conclusionsDirectory.mkdirs()
        IdentifiedObjectExporter(conclusionsDirectory, ConclusionSource(kb.conclusionManager)).export()

        // Conditions.
        conditionsDirectory.mkdirs()
        IdentifiedObjectExporter(conditionsDirectory, ConditionSource(kb.conditionManager)).export()

        // Cases.
        cornerstoneCasesDirectory.mkdirs()
        processedCasesDirectory.mkdirs()
        CaseExporter(cornerstoneCasesDirectory, kb.allCornerstoneCases()).export()
        CaseExporter(processedCasesDirectory, kb.allProcessedCases()).export()

        // Rules.
        rulesDirectory.mkdirs()
        IdentifiedObjectExporter(rulesDirectory, RuleSource(kb.ruleTree)).export()
    }
}