package io.rippledown.kb.export

import io.rippledown.kb.KB
import io.rippledown.kb.requireUnreservedKbName
import io.rippledown.kb.ai.validateAiBoolean
import io.rippledown.model.rule.AiBoolean
import io.rippledown.persistence.PersistenceProvider
import java.io.File

class KBImporter(
    source: File,
    private val persistenceProvider: PersistenceProvider,
    private val requireNameUnused: (String) -> Unit = {}
) : KBExportImport(source) {

    fun import(): KB {
        // Extract the name and id.
        val kbInfo = KBInfoImporter(kbDetailsFile).import()
        requireUnreservedKbName(kbInfo.name)
        requireNameUnused(kbInfo.name)

        // Comments are comment attributes, so an export that still has
        // conclusions predates that and cannot be imported. Reject it before
        // creating or populating any persistent state.
        check(conclusionsDirectory.listFiles().isNullOrEmpty()) {
            "This knowledge base was exported with conclusions, which are no longer supported."
        }

        // Validate AI semantics and archive records before any persistent state is created.
        val idToAttribute = AttributesImporter(attributesFile).import()
        val definitions = if (definitionsDirectory.isDirectory)
            DirectoryImporter(definitionsDirectory, DefinitionExporter(), true).import() else emptySet()
        definitions.forEach { definition ->
            require(idToAttribute.containsKey(definition.attributeId)) { "A definition refers to a missing target attribute." }
            (definition.expression as? AiBoolean)?.let { validateAiBoolean(it, idToAttribute.values.toSet()) }
        }
        definitions.groupBy { it.attributeId }.forEach { (_, definitionsForAttribute) ->
            require(definitionsForAttribute.distinct().size == 1) { "Conflicting definitions for one attribute." }
        }
        val evaluations = AiEvaluationArchive.read(evaluationsDirectory, idToAttribute.values.toSet())

        // Using the name and id, create a persistent KB.
        val persistentKB = persistenceProvider.createKBPersistence(kbInfo)

        // Load the metadata.
        val items = DirectoryImporter(metaInfoDirectory, KeyValueExporter(), true).import()
        persistentKB.metaDataStore().load(items)

        // Extract the attributes and load them into the persistent attribute store.
        val attributeStore = persistentKB.attributeStore()
        attributeStore.load(idToAttribute.values.toSet())

        // Extract the case view and store it in the persistent attribute order store.
        val attributesInOrder = CaseViewImporter(caseViewFile, idToAttribute).import()
        val attributeIdToIndex = mutableMapOf<Int,Int>()
        attributesInOrder.forEachIndexed { index, attribute -> attributeIdToIndex[attribute.id] = index}
        persistentKB.attributeOrderStore().load(attributeIdToIndex)

        // Extract the definitions of the derived and comment attributes and
        // store them. An export made before definitions were exported has no
        // such directory.
        persistentKB.derivedDefinitionStore().load(definitions.associate { it.attributeId to it.expression })
        evaluations.forEach { persistentKB.aiEvaluationStore().insertIfAbsent(it) }

        // Extract the conditions and store them.
        val conditions = DirectoryImporter(conditionsDirectory, ConditionExporter(), true).import()
        persistentKB.conditionStore().load(conditions)

        // Extract the rule tree.
        val ruleStore = persistentKB.ruleStore()
        ruleStore.load(DirectoryImporter(rulesDirectory, RuleExporter()).import())

        // Extract the rule sessions.
        val ruleSessionRecordStore = persistentKB.ruleSessionRecordStore()
        ruleSessionRecordStore.load(DirectoryImporter(ruleSessionsDirectory, RuleSessionRecordsExporter(), true).import())

        // Create the result KB.
        val result = KB(persistentKB)

        // Add the cases.
        result.loadCases(listOf(casesDirectory, processedCasesDirectory, userDefinedCasesDirectory).flatMap { CaseImporter(it).import() })

        return result
    }
}
