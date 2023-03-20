package io.rippledown.kb.export

import io.rippledown.kb.AttributeManager
import io.rippledown.kb.KB
import io.rippledown.model.Attribute
import java.io.File
import java.nio.file.Files
import kotlin.text.Charsets.UTF_8

class KBImporter(source: File): KBExportImport(source) {

    fun import(): KB {
        // Extract the name.
        val name = String(Files.readAllBytes(kbDetailsFile.toPath()), UTF_8).trim()

        // Extract the attributes.
        val idToAttribute = AttributesImporter(attributesFile).import()
        val attributeManager = AttributeManager(idToAttribute.values.toSet())

        // Extract the rule tree.
        val ruleTree = RuleImporter(rulesDirectory).import()

        // Create the result.
        val result = KB(name, attributeManager, ruleTree)

        // Add the cases.
        CaseImporter(casesDirectory).import().forEach { result.addCase(it) }

        // Rebuild the case view.
        result.caseViewManager.setAttributes(CaseViewImporter(caseViewFile, idToAttribute).import())
        return result
    }
}