package io.rippledown.kb.ai

import io.rippledown.kb.KB
import io.rippledown.kb.RuleSessionManager
import io.rippledown.kb.levenshtein
import io.rippledown.llm.aiEvaluationModel
import io.rippledown.model.Attribute
import io.rippledown.model.AttributeKind
import io.rippledown.model.RDRCase
import io.rippledown.model.chat.AiDefinitionDraftView
import io.rippledown.model.rule.*
import io.rippledown.model.rule.evaluation.DefaultValueExpressionEvaluator
import io.rippledown.model.rule.evaluation.EvaluationOutcome
import java.util.UUID

/** A user's approved meaning is kept on the server, independently of the authoring model. */
data class AiDefinitionDraft(
    val id: String,
    val attributeName: String,
    val definition: AiBoolean,
    val caseId: Long?,
    val reasons: List<String> = emptyList(),
    val editedAttributeId: Int? = null,
    val previousDefinition: ValueExpression? = null
) {
    fun description(): String = buildString {
        append("Draft: ").append(attributeName).append("\nSource: ").append(definition.sourceAttribute.name)
        append("\nCriteria: ").append(definition.instructions)
        definition.examples.forEach {
            append("\n").append(it.input).append(" → ").append(it.expected)
            it.explanation?.let { explanation -> append(" (").append(explanation).append(")") }
        }
        if (editedAttributeId != null) append("\nThis changes the default definition everywhere.")
    }
}

/** What a draft preview produced: a value with evidence, or the reason there is none. */
data class AiDraftPreview(val value: String? = null, val evidence: String? = null, val message: String? = null)

fun AiDefinitionDraft.toView(preview: AiDraftPreview = AiDraftPreview()) = AiDefinitionDraftView(
    attributeName = attributeName,
    source = definition.sourceAttribute.name,
    criteria = definition.instructions,
    examples = definition.examples,
    previewValue = preview.value,
    previewMessage = preview.message,
    evidence = preview.evidence,
    changesEverywhere = editedAttributeId != null,
    prompt = "Accept this definition?"
)

data class AcceptedAiDefinition(
    val message: String,
    val cornerstoneStatus: CornerstoneStatus? = null,
    val reasons: List<String> = emptyList()
)

/** Called under the KB lock. Preview demands are resolved by the orchestration boundary. */
class AiDefinitionAuthoring(
    private val kb: KB,
    private val sessions: RuleSessionManager,
    private val profile: () -> AiEvaluationProfile = {
        AiEvaluationPolicy.defaultProfile(aiEvaluationModel())
    }
) {
    var draft: AiDefinitionDraft? = null
        private set

    /**
     * The single near-match offered when a draft was refused because its
     * source could not be resolved. A bare confirmation applies it; anything
     * else leaves the draft's source unresolved.
     */
    var pendingSourceCorrection: String? = null
        private set

    fun propose(
        attributeName: String, sourceName: String, instructions: String, examples: List<AiBooleanExample>,
        caseId: Long?, reasons: List<String>, edit: Boolean
    ): AiDefinitionDraft {
        check(!sessions.isRuleSessionActive()) { "Please finish or cancel the current rule before starting a new one." }
        cancel()
        val attributes = kb.attributeManager.all()
        val name = attributeName.trim()
        require(name.length < 256) { "An attribute name must have fewer than 256 characters." }
        val target = if (edit) {
            attributes.singleOrNull { it.name.equals(name, ignoreCase = true) }
                ?.also { require(it.kind == AttributeKind.DERIVED) { "Only a derived attribute has a definition to edit." } }
                ?: throw IllegalArgumentException("No derived attribute named \"$name\" exists.")
        } else {
            validateNewAiAttributeName(name, attributes)
            null
        }
        val source = attributes.singleOrNull { it.name.equals(sourceName.trim(), ignoreCase = true) }
            ?: throw IllegalArgumentException(sourceQuestion(sourceName, attributes).also {
                pendingSourceCorrection = sourceCandidates(sourceName, attributes).singleOrNull()
            })
        val previous = target?.let { kb.derivedDefinitionManager.definitionFor(it.id) }
        val definition = AiBoolean(source, instructions, examples.toList(), UUID.randomUUID().toString(),
            (previous as? AiBoolean)?.evaluationProfile ?: profile())
        validateAiBoolean(definition, attributes)
        return AiDefinitionDraft(UUID.randomUUID().toString(), target?.name ?: name, definition, caseId,
            reasons.toList(), target?.id, previous).also {
            draft = it
            sessions.invalidatePendingOperations()
        }
    }

    private fun sourceCandidates(name: String, attributes: Set<Attribute>) =
        attributes.filter { it.kind == AttributeKind.EXTERNAL }
            .filter { levenshtein(it.name.lowercase(), name.lowercase()) <= maxOf(2, name.length / 3) }
            .map { it.name }.sorted()

    private fun sourceQuestion(name: String, attributes: Set<Attribute>): String {
        val near = sourceCandidates(name, attributes)
        return "No primary attribute named \"$name\" exists." +
                if (near.isEmpty()) " Please specify an existing primary attribute."
                else " Please confirm the source: ${near.joinToString { "\"$it\"" }}."
    }

    fun preview(id: String, case: RDRCase?): AiDefinitionDraftView {
        val current = current(id, case)
        if (case == null) return current.toView(AiDraftPreview(message = "Select a case to preview and accept this draft."))
        if (current.caseId == null) draft = current.copy(caseId = case.id)
        val live = liveCase(case)
        val preview = when (val result =
            DefaultValueExpressionEvaluator().evaluate(current.definition, live, kb.evaluationContext())) {
            is EvaluationOutcome.Value -> AiDraftPreview("${result.value}", result.provenance?.evidence)
            EvaluationOutcome.NoInput ->
                AiDraftPreview(message = "No ${current.definition.sourceAttribute.name} value; not evaluated.")
            is EvaluationOutcome.NeedsEvaluation -> throw IncompleteInferenceException(
                InferenceOutcome.Demand(Attribute(-1, current.attributeName, AttributeKind.DERIVED), result.request))
            is EvaluationOutcome.Failed -> throw IncompleteInferenceException(
                InferenceOutcome.Incomplete(Attribute(-1, current.attributeName, AttributeKind.DERIVED), result.reason))
        }
        return current.toView(preview)
    }

    fun accept(id: String, case: RDRCase?): AcceptedAiDefinition {
        val current = current(id, case)
        val edited = current.editedAttributeId
        val accepted = if (edited != null) {
            check(kb.derivedDefinitionManager.definitionFor(edited) == current.previousDefinition) {
                "The definition changed after this draft was shown. Please review a new draft."
            }
            val attribute = kb.attributeManager.getById(edited)
            AcceptedAiDefinition(sessions.editDerivedAttributeDefinition(attribute.name, current.definition))
        } else {
            val selected = requireNotNull(case) { "Select a case before accepting this draft." }
            validateNewAiAttributeName(current.attributeName, kb.attributeManager.all())
            val status = sessions.startRuleSessionToAssignValue(liveCase(selected), current.attributeName, current.definition)
            AcceptedAiDefinition("Accepted the definition of \"${current.attributeName}\".", status, current.reasons)
        }
        draft = null
        return accepted
    }

    fun cancel() {
        draft = null
        pendingSourceCorrection = null
        sessions.invalidatePendingOperations()
    }

    private fun current(id: String, case: RDRCase?): AiDefinitionDraft {
        val current = checkNotNull(draft?.takeIf { it.id == id }) { "This draft is no longer current. Please review a new draft." }
        check(current.caseId == null || current.caseId == case?.id) {
            "The selected case changed after this draft was shown. Please preview it on the selected case."
        }
        return current
    }

    private fun liveCase(case: RDRCase): RDRCase = case.id?.let {
        checkNotNull(kb.getCase(it)) { "The selected case no longer exists." }
    } ?: case
}
