package io.rippledown.kb.export

import io.rippledown.kb.AttributeManager
import io.rippledown.kb.KB
import java.io.File

class KBImporter(source: File): KBExportImport(source) {

    fun import(): KB {
        // Extract the name.
        val kbInfo = KBInfoImporter(kbDetailsFile).import()

        // Extract the attributes.
        val idToAttribute = AttributesImporter(attributesFile).import()
        val attributeManager = AttributeManager(idToAttribute.values.toSet())

        // Extract the rule tree.
        val ruleTree = RuleImporter(rulesDirectory).import()

        // Create the result.
        val result = KB(kbInfo, attributeManager, ruleTree)

        // Add the cases.
        CaseImporter(casesDirectory).import().forEach { result.addCase(it) }

        // Rebuild the case view.
        result.caseViewManager.setAttributes(CaseViewImporter(caseViewFile, idToAttribute).import())
        return result
    }
}