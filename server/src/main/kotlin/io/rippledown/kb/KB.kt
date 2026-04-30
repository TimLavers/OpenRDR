package io.rippledown.kb

import io.rippledown.log.lazyLogger
import io.rippledown.model.CaseType
import io.rippledown.model.RDRCase
import io.rippledown.model.RDRCaseBuilder
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.external.ExternalCase
import io.rippledown.model.rule.RuleSessionRecorder
import io.rippledown.persistence.PersistentKB


class KB(persistentKB: PersistentKB) {
    val logger = lazyLogger

    val kbInfo = persistentKB.kbInfo()
    val metaInfo = MetaInfo(persistentKB.metaDataStore())
    val attributeManager = AttributeManager(persistentKB.attributeStore())
    val conclusionManager = ConclusionManager(persistentKB.conclusionStore())
    val conditionManager = ConditionManager(attributeManager, persistentKB.conditionStore())
    val interpretationViewManager = InterpretationViewManager(persistentKB.conclusionOrderStore(), conclusionManager)
    val ruleSessionRecorder = RuleSessionRecorder(persistentKB.ruleSessionRecordStore())
    internal val ruleManager = RuleManager(conclusionManager, conditionManager, persistentKB.ruleStore())
    private val caseManager = CaseManager(persistentKB.caseStore(), attributeManager)
    internal val caseViewManager = CaseViewManager(persistentKB.attributeOrderStore(), attributeManager)
    val ruleTree = ruleManager.ruleTree()

    fun attributeNames() = attributeManager.all().map { it.name }

    fun description() = metaInfo.getDescription()

    fun setDescription(description: String) {
        metaInfo.setDescription(description)
    }

    fun containsCornerstoneCaseWithName(caseName: String): Boolean {
        return caseManager.ids(CaseType.Cornerstone).find { rdrCase -> rdrCase.name == caseName } != null
    }

    fun loadCases(data: List<RDRCase>) = caseManager.load(data)

    fun addCornerstoneCase(case: RDRCase): RDRCase {
        return caseManager.add(case.copyWithoutId(CaseType.Cornerstone))
    }

    fun addCornerstoneCase(externalCase: ExternalCase): RDRCase {
        val builder = RDRCaseBuilder().apply { setCaseType(CaseType.Cornerstone) }
        externalCase.data.forEach {
            val attribute = attributeManager.getOrCreate(it.key.name)
            builder.addResult(attribute, it.key.time, it.value)
        }
        return caseManager.add(builder.build(externalCase.caseName))
    }

    fun addProcessedCase(case: RDRCase): RDRCase {
        return caseManager.add(case)
    }

    fun getCaseByName(caseName: String): RDRCase {
        return caseManager.all().first { caseName == it.name }
    }

    fun getCornerstoneCaseByName(caseName: String) = allCornerstoneCases().first { caseName == it.name }

    fun getProcessedCaseByName(caseName: String) = allProcessedCases().first { caseName == it.name }

    fun allCornerstoneCases() = caseManager.all(CaseType.Cornerstone)

    fun cornerstoneCaseIds() = caseManager.ids(CaseType.Cornerstone)

    fun processedCaseIds() = caseManager.ids(CaseType.Processed)

    fun allProcessedCases() = caseManager.all(CaseType.Processed)

    fun deletedProcessedCaseWithName(name: String) {
        val toGo = processedCaseIds().firstOrNull { it.name == name }
        if (toGo != null) {
            caseManager.delete(toGo.id!!)
        }
    }

    fun getProcessedCase(id: Long): RDRCase? = caseManager.getCase(id)

    fun getCase(id: Long): RDRCase? = caseManager.getCase(id) // todo test

    fun processCase(externalCase: ExternalCase): RDRCase {
        val case = createRDRCase(externalCase)
        val stored = caseManager.add(case)
        interpret(stored)
        return stored
    }

    fun createRDRCase(case: ExternalCase): RDRCase {
        val builder = RDRCaseBuilder()
        case.data.forEach {
            val attribute = attributeManager.getOrCreate(it.key.name)
            builder.addResult(attribute, it.key.time, it.value)
        }
        return builder.build(case.caseName)
    }

    fun interpret(case: RDRCase) = ruleTree.apply(case)

    fun viewableCase(case: RDRCase): ViewableCase {
        val interpretation = interpret(case)
        val viewableInterpretation = interpretationViewManager.viewableInterpretation(interpretation)
        return caseViewManager.getViewableCase(case, viewableInterpretation)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KB

        return kbInfo == other.kbInfo
    }

    override fun hashCode() = kbInfo.hashCode()
}

internal fun String.normalizeForComparison() =
    lowercase().replace("\"", "").replace("'", "").replace(Regex("\\s+"), " ").trim()