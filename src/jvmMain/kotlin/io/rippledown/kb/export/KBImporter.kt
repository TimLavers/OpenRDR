package io.rippledown.kb.export

import io.rippledown.kb.KB
import java.io.File
import java.nio.file.Files
import kotlin.text.Charsets.UTF_8

class KBImporter(source: File): KBExportImport(source) {

    fun import(): KB {
        // Extract the name.
        val name = String(Files.readAllBytes(kbDetailsFile.toPath()), UTF_8).trim()

        // Extract the rule tree.
        val ruleTree = RuleImporter(rulesDirectory).import()

        // Create the result.
        val result = KB(name, ruleTree)

        // Add the cases.
        CaseImporter(casesDirectory).import().forEach { result.addCase(it) }

        // Rebuild the case view.
        result.caseViewManager.setAttributes(CaseViewImporter(caseViewFile).import())
        return result
    }
}