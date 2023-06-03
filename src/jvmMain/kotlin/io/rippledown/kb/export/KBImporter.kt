package io.rippledown.kb.export

import io.rippledown.kb.KB
import io.rippledown.persistence.PersistenceProvider
import java.io.File

class KBImporter(source: File, private val persistenceProvider: PersistenceProvider): KBExportImport(source) {

    fun import(): KB {
        // Extract the name and id.
        val kbInfo = KBInfoImporter(kbDetailsFile).import()

        // Using the name and id, create a persistent KB.
        val persistentKB = persistenceProvider.createKBPersistence(kbInfo)

        // Extract the attributes and load them into the persistent attribute store.
        val idToAttribute = AttributesImporter(attributesFile).import()
        val attributeStore = persistentKB.attributeStore()
        attributeStore.load(idToAttribute.values.toSet())

        // Extract the case view and store it in the persistent attribute order store.
        val attributesInOrder = CaseViewImporter(caseViewFile, idToAttribute).import()
        val attributeIdToIndex = mutableMapOf<Int,Int>()
        attributesInOrder.forEachIndexed { index, attribute -> attributeIdToIndex[attribute.id] = index}
        persistentKB.attributeOrderStore().load(attributeIdToIndex)

        // Extract the conclusions and store them.
        val conclusions = DirectoryImporter(conclusionsDirectory, ConclusionExporter(), true).import()
        persistentKB.conclusionStore().load(conclusions)

        // Extract the conditions and store them.
        val conditions = DirectoryImporter(conditionsDirectory, ConditionExporter(), true).import()
        persistentKB.conditionStore().load(conditions)

        // Extract the rule tree.
        val ruleStore = persistentKB.ruleStore()
        ruleStore.load(DirectoryImporter(rulesDirectory, RuleExporter()).import())

        // Create the result KB.
        val result = KB(persistentKB)

        // Add the cases.
        CaseImporter(casesDirectory).import().forEach { result.addCase(it) }

        return result
    }
}