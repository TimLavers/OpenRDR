package io.rippledown.kb

import io.rippledown.log.lazyLogger
import io.rippledown.model.*
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.external.ExternalCase
import io.rippledown.model.rule.AssignValue
import io.rippledown.model.rule.DefinitionResolver
import io.rippledown.model.rule.RuleSessionRecorder
import io.rippledown.model.rule.resolvedFor
import io.rippledown.persistence.PersistentKB


class KB(persistentKB: PersistentKB) {
    val logger = lazyLogger

    val kbInfo = persistentKB.kbInfo()
    val metaInfo = MetaInfo(persistentKB.metaDataStore())
    val attributeManager = AttributeManager(persistentKB.attributeStore())
    val conclusionManager = ConclusionManager(persistentKB.conclusionStore())
    val derivedDefinitionManager = DerivedDefinitionManager(persistentKB.derivedDefinitionStore())

    /**
     * Resolves a derived attribute to its stored definition, so that
     * ByDefinition rule actions evaluate against the definition store. See
     * documentation/design/editing_derived_attribute_definitions.md.
     */
    val definitionResolver: DefinitionResolver = { attribute -> derivedDefinitionManager.definitionFor(attribute.id) }
    val conditionManager = ConditionManager(attributeManager, persistentKB.conditionStore())
    val interpretationViewManager =
        InterpretationViewManager(persistentKB.conclusionOrderStore(), conclusionManager, attributeManager)
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

    fun addCornerstoneCaseIfNoEquivalentAlreadyPresent(case: RDRCase): RDRCase {
        val existing = caseManager.all(CaseType.Cornerstone).any { it.hasSameDataAs(case) }
        if (!existing) {
            return caseManager.add(case.copyWithoutId(CaseType.Cornerstone))
        }
        return case
    }

    fun addCornerstoneCase(externalCase: ExternalCase): RDRCase {
        val builder = RDRCaseBuilder().apply { setCaseType(CaseType.Cornerstone) }
        externalCase.data.forEach {
            val attribute = externalAttributeFor(it.key.name)
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

    fun favouriteCaseIds() = caseManager.ids(CaseType.Favourite)

    fun copyCaseAsFavourite(id: Long, newName: String?): RDRCase {
        val case = caseManager.getCase(id) ?: throw NoSuchElementException("No case with id $id")
        if (newName ==  null || newName.trim().isEmpty()) {
            return caseManager.add(case.copyWithoutId(CaseType.Favourite))
        } else {
            return caseManager.add(case.copyWithNewNameAndNoId(CaseType.Favourite, newName))
        }
    }

    fun deleteCaseFromFavourites(case: RDRCase) {
        if (case.caseId.type != CaseType.Favourite) throw IllegalArgumentException("Case is not a favourite")
        caseManager.delete(case.id!!)
    }

    fun allProcessedCases() = caseManager.all(CaseType.Processed)

    fun deletedProcessedCaseWithName(name: String) {
        val toGo = processedCaseIds().firstOrNull { it.name == name }
        if (toGo != null) {
            caseManager.delete(toGo.id!!)
        }
    }

    fun getProcessedCase(id: Long): RDRCase? = caseManager.getCase(id)

    fun getCase(id: Long): RDRCase? = caseManager.getCase(id)

    fun processCase(externalCase: ExternalCase): RDRCase {
        val case = createRDRCase(externalCase)
        val stored = caseManager.add(case)
        interpret(stored)
        // Resolve ByDefinition assignments to their stored definitions so that
        // callers reading the interpretation directly (e.g. the interpreter API
        // endpoint) see CommentTemplate/Literal expressions, not sentinels.
        stored.interpretation.resolveDefinitions(definitionResolver)
        return stored
    }

    fun createRDRCase(case: ExternalCase): RDRCase {
        val builder = RDRCaseBuilder()
        case.data.forEach {
            val attribute = externalAttributeFor(it.key.name)
            builder.addResult(attribute, it.key.time, it.value)
        }
        return builder.build(case.caseName)
    }

    /**
     * The external attribute for the given externally supplied name. If the
     * name is taken by a KB-assigned (derived or comment) attribute, the
     * external data is mapped to a deterministically mangled attribute
     * instead, so that case ingestion never fails and external data is never
     * silently dropped. The same external name maps to the same mangled
     * attribute on every case.
     */
    internal fun externalAttributeFor(name: String): Attribute {
        val existing = attributeManager.byName(name)
        return if (existing == null || existing.kind == AttributeKind.EXTERNAL) {
            attributeManager.getOrCreate(name, AttributeKind.EXTERNAL)
        } else {
            val mangledName = mangledExternalName(name)
            logger.warn(
                "Externally supplied attribute name '$name' collides with a ${existing.kind} attribute. " +
                        "Its values will be stored under '$mangledName'."
            )
            attributeManager.getOrCreate(mangledName, AttributeKind.EXTERNAL)
        }
    }

    fun interpret(case: RDRCase) = ruleTree.apply(case, definitionResolver)

    fun viewableCase(case: RDRCase): ViewableCase {
        val materialised = ruleTree.materialise(case, definitionResolver)
        // For display, a ByDefinition assignment is shown as the attribute's
        // stored definition text, so the panel and tooltip show the formula
        // rather than the sentinel. Inference structures are untouched.
        val resolvedInterpretation = materialised.interpretation.withResolvedDefinitions(definitionResolver)
        val viewableInterpretation =
            interpretationViewManager.viewableInterpretation(resolvedInterpretation, materialised)
        return caseViewManager.getViewableCase(materialised, viewableInterpretation)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KB

        return kbInfo == other.kbInfo
    }

    override fun hashCode() = kbInfo.hashCode()
}

/**
 * A copy of this interpretation for display, with each ByDefinition
 * assignment replaced by the attribute's stored definition. An assignment
 * whose attribute has no stored definition is left as is.
 */
internal fun Interpretation.withResolvedDefinitions(resolver: DefinitionResolver): Interpretation {
    val result = Interpretation(caseId)
    ruleSummaries.forEach { summary ->
        val assignment = summary.assignment
        val resolved = assignment?.expression?.resolvedFor(assignment.attribute, resolver)
        val resolvedSummary = if (resolved != null && resolved != assignment.expression) {
            summary.copy(assignment = AssignValue(assignment.attribute, resolved))
        } else {
            summary
        }
        result.add(resolvedSummary)
    }
    return result
}

internal fun String.normalizeForComparison() =
    lowercase().replace("\"", "").replace("'", "").replace(Regex("\\s+"), " ").trim()

const val EXTERNAL_NAME_MANGLING_SUFFIX = " (external)"

/**
 * The deterministic name under which externally supplied data is stored when
 * its name collides with a KB-assigned attribute.
 */
fun mangledExternalName(name: String) = "$name$EXTERNAL_NAME_MANGLING_SUFFIX"