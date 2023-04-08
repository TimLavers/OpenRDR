package io.rippledown.kb.export

import io.rippledown.kb.AttributeManager
import io.rippledown.kb.KB
import io.rippledown.persistence.PersistenceProvider
import java.io.File

class KBImporter(source: File, private val persistenceProvider: PersistenceProvider): KBExportImport(source) {

    fun import(): KB {
        // Extract the name and id.
        val kbInfo = KBInfoImporter(kbDetailsFile).import()

        // Using the name and id, create a KB.
        val persistentKB = persistenceProvider.createKBPersistence(kbInfo)

        // Extract the attributes.
        val idToAttribute = AttributesImporter(attributesFile).import()
        val attributeStore = persistentKB.attributeStore()
        attributeStore.load(idToAttribute.values.toSet())
        val attributeManager = AttributeManager(attributeStore)

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