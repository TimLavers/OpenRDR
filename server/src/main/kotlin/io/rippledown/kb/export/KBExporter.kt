package io.rippledown.kb.export

import io.rippledown.kb.KB
import java.nio.file.Files
import java.nio.file.Path

open class KBExportImport(val destination: Path) {
    val kbDetailsFile = destination.resolve("Details.txt")
    val metaInfoDirectory = destination.resolve("MetaInfo")
    val attributesFile = destination.resolve("Attributes.txt")
    val caseViewFile = destination.resolve("CaseView.txt")
    val casesDirectory = destination.resolve("CornerstoneCases")
    val processedCasesDirectory = destination.resolve("ProcessedCases")
    val conclusionsDirectory = destination.resolve("Conclusions")
    val conditionsDirectory = destination.resolve("Conditions")
    val rulesDirectory = destination.resolve("Rules")
    val ruleSessionsDirectory = destination.resolve("RuleSessions")
}

class KBExporter(destination: Path, val kb: KB) : KBExportImport(destination) {
    init {
        checkDirectoryIsSuitableForExport(destination, "KB")
    }

    fun export() {
        // Details of the KB.
        KBInfoExporter(ExportFile(kbDetailsFile, "KBInfo"), kb.kbInfo).export()

        // MetaInfo
        Files.createDirectories(metaInfoDirectory)
        IdentifiedObjectExporter(metaInfoDirectory, KeyValueSource(kb.metaInfo.keyValueStore, "MetaInfo")).export()

        // Attributes.
        AttributesExporter(attributesFile, kb.attributeManager.all()).export()

        // Case view.
        CaseViewExporter(caseViewFile, kb.caseViewManager.allInOrder()).export()

        // Conclusions.
        Files.createDirectories(conclusionsDirectory)
        IdentifiedObjectExporter(conclusionsDirectory, ConclusionSource(kb.conclusionManager)).export()

        // Conditions.
        Files.createDirectories(conditionsDirectory)
        IdentifiedObjectExporter(conditionsDirectory, ConditionSource(kb.conditionManager)).export()

        // Cases.
        Files.createDirectories(casesDirectory)
        Files.createDirectories(processedCasesDirectory)
        CaseExporter(casesDirectory, kb.allCornerstoneCases()).export()
        CaseExporter(processedCasesDirectory, kb.allProcessedCases()).export()

        // Rules.
        Files.createDirectories(rulesDirectory)
        IdentifiedObjectExporter(rulesDirectory, RuleSource(kb.ruleTree)).export()

        // Rule sessions
        Files.createDirectories(ruleSessionsDirectory)
        IdentifiedObjectExporter(
            ruleSessionsDirectory,
            RuleSessionRecordsSource(kb.ruleSessionRecorder.allRuleSessionHistories())
        ).export()
    }
}