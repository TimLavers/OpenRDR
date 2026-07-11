package io.rippledown.kb.export

import io.rippledown.kb.KB
import io.rippledown.persistence.PersistenceProvider
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Path
import kotlin.io.path.listDirectoryEntries

fun importKbFromZipFile(zipFile: Path,persistenceProvider: PersistenceProvider): KB = FileSystems.newFileSystem(zipFile, null as ClassLoader?).use { fs ->
    val root = fs.rootDirectories.first()
    val subDirectories = root.listDirectoryEntries()
    require(subDirectories.size == 1) {
        "Invalid zip for KB import."
    }
    val rootDir = subDirectories[0]
    KBImporter(rootDir, persistenceProvider).import()
}

class KBImporter(source: Path, private val persistenceProvider: PersistenceProvider): KBExportImport(source) {

    fun import(): KB {
        // Extract the name and id.
        val kbInfo = KBInfoImporter(kbDetailsFile).import()

        // Using the name and id, create a persistent KB.
        val persistentKB = persistenceProvider.createKBPersistence(kbInfo)

        // Load the metadata.
        val items = DirectoryImporter(metaInfoDirectory, KeyValueExporter(), true).import()
        persistentKB.metaDataStore().load(items)

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

        // Extract the rule sessions.
        val ruleSessionRecordStore = persistentKB.ruleSessionRecordStore()
        ruleSessionRecordStore.load(DirectoryImporter(ruleSessionsDirectory, RuleSessionRecordsExporter(), true).import())

        // Create the result KB.
        val result = KB(persistentKB)

        // Add the cases.
        result.loadCases(CaseImporter(casesDirectory).import() +  CaseImporter(processedCasesDirectory).import())

        return result
    }
}