package io.rippledown.kb

import io.rippledown.constants.chat.cannotRenameMessage
import io.rippledown.constants.chat.renamedMessage
import io.rippledown.constants.rule.CONDITION_IS_NOT_TRUE
import io.rippledown.constants.rule.DOES_NOT_CORRESPOND_TO_A_CONDITION
import io.rippledown.constants.rule.INTERPRETED_CONDITION_IS_NOT_TRUE
import io.rippledown.constants.rule.NOT_A_VALID_VALUE
import io.rippledown.hints.AttributeFor
import io.rippledown.hints.ConditionChatService
import io.rippledown.hints.ConditionGenerator
import io.rippledown.kb.chat.RuleService
import io.rippledown.kb.chat.action.didYouMeanFormulaMessage
import io.rippledown.kb.chat.action.nameClashWithExistingExternalAttributeMessage
import io.rippledown.kb.chat.action.unknownAttributeInFormulaMessage
import io.rippledown.kb.chat.resolveCommentVariables
import io.rippledown.log.lazyLogger
import io.rippledown.model.*
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.ConditionList
import io.rippledown.model.condition.ConditionParsingResult
import io.rippledown.model.condition.edit.EditableCondition
import io.rippledown.model.diff.*
import io.rippledown.model.rule.*
import io.rippledown.server.ConditionExpressionParser
import io.rippledown.server.websocket.WebSocketManager
import io.rippledown.suggestions.ConditionSuggester
import io.rippledown.suggestions.SuggestionContext
import kotlinx.coroutines.runBlocking

/**
 * The name a comment variable's marker is rendered with when the variable names
 * an attribute not in the KB.
 */
const val UNKNOWN_VARIABLE_NAME = "unknown"

/**
 * The characters that make a value expression worth offering to the formula
 * parser. Without one of these the text is a value, not arithmetic.
 */
private val FORMULA_OPERATORS = Regex("""[+\-*/()^]""")

class RuleSessionManager(
    private val kb: KB,
    private val webSocketManager: WebSocketManager? = null
) : RuleService {
    val logger = lazyLogger

    private var ruleSession: RuleBuildingSession? = null

    /**
     * The change the session in progress is about to make. A session makes one
     * change, so this is one field; [pendingChange] is the read view of it, and
     * [currentDiff] and [currentDerivedValueChange] narrow that to the code
     * that handles only one kind.
     */
    internal var currentChange: PendingChange? = null

    /**
     * The change the session in progress is about to make, with the name of the
     * comment attribute it concerns as that attribute is named *now*: the user
     * can rename a comment while its rule is being built, and the panel showing
     * the pending change must show the new name.
     */
    internal val pendingChange: PendingChange?
        get() = when (val change = currentChange) {
            is Addition -> change.copy(attributeName = currentAttributeName(change.attributeName))
            is Removal -> change.copy(attributeName = currentAttributeName(change.attributeName))
            is Replacement -> change.copy(
                attributeName = currentAttributeName(change.attributeName),
                replacedAttributeName = replacedDiffAttribute?.name ?: change.replacedAttributeName
            )
            else -> change
        }

    /**
     * The current name of [diffAttribute], falling back to the name the change
     * was made with when the change does not concern a comment attribute.
     */
    private fun currentAttributeName(nameWhenChangeWasMade: String) =
        diffAttribute?.name ?: nameWhenChangeWasMade

    internal val currentDiff: Diff?
        get() = pendingChange as? Diff

    internal val currentDerivedValueChange: DerivedValueChange?
        get() = pendingChange as? DerivedValueChange

    /**
     * The comment attribute that the session in progress will assign, held so
     * that the user can be told its name when the comment is accepted.
     */
    private var commentAttributeInSession: Attribute? = null

    /**
     * The comment attribute whose name the pending diff shows: the attribute
     * being assigned for an addition or a replacement, and the one being
     * retracted for a removal. Held so that the name is read from the attribute
     * itself, which a rename updates in place, rather than being a snapshot
     * taken when the session started.
     */
    private var diffAttribute: Attribute? = null

    /**
     * The comment attribute a pending replacement is replacing, held for the same
     * reason as [diffAttribute]: the replacement names it so that the panel can
     * find the row to preview, and a rename during the session must not leave
     * that name stale.
     */
    private var replacedDiffAttribute: Attribute? = null

    private var selectedCornerstone: ViewableCase? = null
    private val conditionChatService = ConditionChatService()
    private var conditionParser: ConditionParser

    init {
        conditionParser = object : ConditionParser {
            override fun parse(expression: String, attributeFor: AttributeFor) =
                ConditionGenerator(attributeFor, conditionChatService, kb.attributeNames()).conditionFor(expression)
        }
    }

    fun startRuleSession(
        case: RDRCase,
        action: RuleTreeChange
    ): CornerstoneStatus {
        logger.info("Starting rule session for case ${case.name} and action $action")
        logger.info("Current comments are: ${case.interpretation.commentTexts(case)} ")
        check(ruleSession == null) { "Session already in progress." }
        check(action.isApplicable(kb.ruleTree, case)) { "Action $action is not applicable to case ${case.name}" }
        checkActionExpressionIsAcyclic(action)
        ruleSession = RuleBuildingSession(
            kb.ruleManager, kb.ruleTree, case, action, kb.allCornerstoneCases(), kb.definitionResolver
        )
        logger.info("Rule session created")
        return cornerstoneStatus(null)
    }

    override fun startRuleSessionToAddComment(
        viewableCase: ViewableCase,
        comment: String,
        variables: List<CommentVariable>,
    ): CornerstoneStatus = startRuleSessionToAddComment(viewableCase.case, comment, variables)

    /**
     * Comments are comment attributes: adding one gets or creates the
     * attribute whose definition is the comment's text, and builds a rule
     * assigning it by definition. A new attribute is auto-named (C1, C2, …)
     * and can be renamed by the user later.
     */
    internal fun startRuleSessionToAddComment(
        case: RDRCase,
        comment: String,
        variables: List<CommentVariable> = emptyList(),
    ): CornerstoneStatus {
        val template = commentTemplate(comment, variables)
        val attribute = commentAttributeFor(template)
        return startCommentSession(
            case,
            Addition(template.textWithVariableNames(), attribute.name, attribute.id),
            attribute,
            attribute,
            ChangeTreeToAddAssignment(AssignValue(attribute, ByDefinition))
        )
    }

    override fun startRuleSessionToRemoveComment(viewableCase: ViewableCase, comment: String): CornerstoneStatus =
        startRuleSessionToRemoveComment(viewableCase.case, comment)

    internal fun startRuleSessionToRemoveComment(case: RDRCase, comment: String): CornerstoneStatus {
        val attribute = commentAttributeForText(comment)
            ?: error("Cannot remove comment: no comment matching \"$comment\" exists.")
        return startCommentSession(
            case,
            Removal(commentTemplateText(attribute), attribute.name),
            null, //a removal assigns no comment
            attribute,
            ChangeTreeToRemoveAssignment(AssignValue(attribute, ByDefinition))
        )
    }

    override fun startRuleSessionToReplaceComment(
        viewableCase: ViewableCase,
        replacedComment: String,
        replacementComment: String,
        variables: List<CommentVariable>,
    ): CornerstoneStatus =
        startRuleSessionToReplaceComment(
            viewableCase.case,
            replacedComment,
            replacementComment,
            variables,
        )

    /**
     * Each comment text has its own attribute, so the replacement is a new
     * (or existing) attribute for the replacement text: the replacing rule
     * assigns it, and leaf-most suppression retracts the original.
     */
    internal fun startRuleSessionToReplaceComment(
        case: RDRCase,
        replacedComment: String,
        replacementComment: String,
        variables: List<CommentVariable> = emptyList(),
    ): CornerstoneStatus {
        val replacedAttribute = commentAttributeForText(replacedComment)
            ?: error("Cannot replace comment: no comment matching \"$replacedComment\" exists.")
        val replacementTemplate = commentTemplate(replacementComment, variables)
        val replacementAttribute = commentAttributeFor(replacementTemplate)
        return startCommentSession(
            case,
            Replacement(
                commentTemplateText(replacedAttribute),
                replacementTemplate.textWithVariableNames(),
                replacementAttribute.name,
                replacedAttribute.name
            ),
            replacementAttribute,
            replacementAttribute,
            ChangeTreeToReplaceAssignment(
                AssignValue(replacedAttribute, ByDefinition),
                AssignValue(replacementAttribute, ByDefinition)
            ),
            replacedAttribute
        )
    }

    /**
     * The internal form of a comment written by a client of the rule-building
     * API, whose variables are given as `{attributeName}` placeholders. That is
     * the form the server itself writes a comment in when it reports a change,
     * so a comment handed back to it has to be read the same way; without this a
     * placeholder would be stored as literal text, and a removal or replacement
     * naming one would match no comment at all.
     */
    private fun internalForm(comment: String) = resolveCommentVariables(comment, emptyList(), this)

    /**
     * The definition of a comment with variables. A variable naming no attribute
     * in this KB is kept, so that its token still renders as an unresolved
     * marker rather than being dropped, which would leave the raw token in the
     * comment and misalign the variables that follow it.
     */
    private fun commentTemplate(comment: String, variables: List<CommentVariable>) =
        CommentTemplate(comment, variables.map { variable ->
            attributeById(variable.attributeId) ?: Attribute(variable.attributeId, UNKNOWN_VARIABLE_NAME)
        })

    /**
     * The comment attribute whose definition is the given template,
     * created if necessary. A new attribute is auto-named (C1, C2, …) and
     * can be renamed by the user later. An existing attribute keeps the
     * name it has.
     */
    private fun commentAttributeFor(template: CommentTemplate): Attribute =
        kb.attributeManager.commentAttributes()
            .firstOrNull { kb.derivedDefinitionManager.definitionFor(it.id) == template }
            ?: kb.attributeManager.createCommentAttribute()
                .also { kb.derivedDefinitionManager.store(it.id, template) }

    override fun nameOfCommentAttributeInSession(): String? =
        if (ruleSession == null) null else commentAttributeInSession?.name

    /**
     * The comment attribute whose definition has the given (internal-form)
     * text, or null if none exists.
     */
    private fun commentAttributeForText(text: String): Attribute? =
        kb.attributeManager.commentAttributes().firstOrNull {
            (kb.derivedDefinitionManager.definitionFor(it.id) as? CommentTemplate)?.text == text
        }

    /**
     * The comment of the given attribute as its rule defines it, each variable
     * shown as `{attributeName}`. A change to a comment is previewed in this
     * form, rather than as the comment renders for the case the rule is being
     * built on, because it is the rule that the user is reviewing.
     */
    private fun commentTemplateText(attribute: Attribute): String =
        (kb.derivedDefinitionManager.definitionFor(attribute.id) as? CommentTemplate)
            ?.textWithVariableNames { id -> attributeById(id) } ?: ""

    /**
     * Starts a session for a rule assigning the attribute by its definition:
     * the expression is stored as the attribute's definition, and the rule
     * simply points at the attribute, so that a later edit of the definition
     * applies without any rule change. See
     * documentation/design/editing_derived_attribute_definitions.md.
     */
    fun startRuleSessionToAssignValue(
        case: RDRCase,
        attributeName: String,
        expressionText: String
    ): CornerstoneStatus {
        val existingAttribute = kb.attributeManager.byName(attributeName)
        if (existingAttribute != null && existingAttribute.kind == AttributeKind.EXTERNAL) {
            error(nameClashWithExistingExternalAttributeMessage(attributeName))
        }
        // Everything that can refuse the request is done before the attribute is
        // created, so that a refusal leaves nothing behind: an attribute with no
        // rule and no definition would be litter, and worse, the name would then
        // clash with the corrected request the user is being asked to confirm.
        val expression = valueExpressionFor(expressionText, attributeName)
        val attribute = kb.attributeManager.getOrCreate(attributeName, AttributeKind.DERIVED)
        cycleForDefinition(attribute, expression)?.let {
            error("This value cannot be assigned: ${cycleMessage(it)}.")
        }
        kb.derivedDefinitionManager.store(attribute.id, expression)
        val assignment = AssignValue(attribute, ByDefinition)
        return startAssignmentSession(
            case,
            DerivedValueAddition(attributeName = attributeName, formula = expression.asText()),
            ChangeTreeToAddAssignment(assignment)
        )
    }

    fun startRuleSessionToRemoveAssignment(case: RDRCase, attributeName: String): CornerstoneStatus {
        val assignment = currentAssignmentFor(case, attributeName)
        return startAssignmentSession(
            case,
            DerivedValueRemoval(attributeName),
            ChangeTreeToRemoveAssignment(assignment)
        )
    }

    fun startRuleSessionToReplaceAssignment(
        case: RDRCase,
        attributeName: String,
        replacementExpressionText: String
    ): CornerstoneStatus {
        val toBeReplaced = currentAssignmentFor(case, attributeName)
        val replacementExpression = valueExpressionFor(replacementExpressionText)
        val replacement = AssignValue(toBeReplaced.attribute, replacementExpression)
        return startAssignmentSession(
            case,
            DerivedValueReplacement(attributeName = attributeName, newFormula = replacementExpression.asText()),
            ChangeTreeToReplaceAssignment(toBeReplaced, replacement)
        )
    }

    override fun startRuleSessionToAssignValue(
        viewableCase: ViewableCase,
        attributeName: String,
        valueExpression: String
    ): CornerstoneStatus = startRuleSessionToAssignValue(viewableCase.case, attributeName, valueExpression)

    override fun startRuleSessionToRemoveAssignment(
        viewableCase: ViewableCase,
        attributeName: String
    ): CornerstoneStatus =
        startRuleSessionToRemoveAssignment(viewableCase.case, attributeName)

    override fun startRuleSessionToReplaceAssignment(
        viewableCase: ViewableCase,
        attributeName: String,
        replacementValueExpression: String
    ): CornerstoneStatus =
        startRuleSessionToReplaceAssignment(viewableCase.case, attributeName, replacementValueExpression)

    /**
     * Starts a session that will change a derived attribute, previewing [change]
     * in the Derived attributes panel while it is in progress. No comment
     * attribute is in session, so the preview shows the name the change carries.
     */
    private fun startAssignmentSession(
        case: RDRCase,
        change: DerivedValueChange,
        action: RuleTreeChange
    ): CornerstoneStatus = startSession(case, change, null, null, action, null)

    /**
     * Starts a session that will change the comments, previewing [change] in the
     * Comments panel while it is in progress.
     */
    private fun startCommentSession(
        case: RDRCase,
        change: Diff,
        commentAttribute: Attribute?,
        attributeNamingTheChange: Attribute,
        action: RuleTreeChange,
        replacedAttribute: Attribute? = null
    ): CornerstoneStatus =
        startSession(case, change, commentAttribute, attributeNamingTheChange, action, replacedAttribute)

    /**
     * Starts a session, previewing the change it is about to make. The preview is
     * set before the session starts so that the returned status carries it, and
     * put back as it was if the session is refused, so that a request which never
     * became a session leaves nothing behind for the next one to show.
     *
     * The previous preview is restored rather than cleared because one of the
     * ways a session is refused is that another is already in progress, and that
     * session's preview has to survive the refusal of the request it turned away.
     */
    private fun startSession(
        case: RDRCase,
        change: PendingChange,
        commentAttribute: Attribute?,
        attributeNamingTheChange: Attribute?,
        action: RuleTreeChange,
        replacedAttribute: Attribute?
    ): CornerstoneStatus {
        val previousChange = currentChange
        val previousCommentAttribute = commentAttributeInSession
        val previousDiffAttribute = diffAttribute
        val previousReplacedDiffAttribute = replacedDiffAttribute
        currentChange = change
        commentAttributeInSession = commentAttribute
        diffAttribute = attributeNamingTheChange
        replacedDiffAttribute = replacedAttribute
        return try {
            startRuleSession(case, action)
        } catch (e: Throwable) {
            currentChange = previousChange
            commentAttributeInSession = previousCommentAttribute
            diffAttribute = previousDiffAttribute
            replacedDiffAttribute = previousReplacedDiffAttribute
            throw e
        }
    }

    private fun dependencyGraph() =
        DerivedAttributeDependencyGraph(kb.ruleTree, kb.attributeManager.all(), kb.definitionResolver)

    /**
     * The message explaining why the given condition cannot be added to the
     * current rule session, or null if it can: a condition that would make
     * a derived attribute depend on itself is refused. See "Stratification"
     * in documentation/design/repeat_inferencing.md.
     */
    private fun cycleMessageFor(condition: Condition): String? {
        val session = ruleSession ?: return null
        val cycle = dependencyGraph().cycleCreatedBy(session.action, condition) ?: return null
        return "This condition cannot be used: ${cycleMessage(cycle)}."
    }

    /**
     * Refuses an assignment whose value expression alone would create a
     * dependency cycle, e.g. assigning BMI the value `BMI * 2`.
     */
    private fun checkActionExpressionIsAcyclic(action: RuleTreeChange) {
        val cycle = dependencyGraph().cycleCreatedBy(action, null) ?: return
        error("This value cannot be assigned: ${cycleMessage(cycle)}.")
    }

    /**
     * Edits the stored definition of a derived attribute in place — the
     * comment-editing pattern applied to data attributes. Every ByDefinition
     * rule picks up the change on the next interpretation; no rule is
     * mutated and no cornerstone review is run. See
     * documentation/design/editing_derived_attribute_definitions.md.
     */
    override fun editDerivedAttributeDefinition(attributeName: String, valueExpression: String): String {
        check(ruleSession == null) { "Session already in progress." }
        val attribute = attributeForName(attributeName)
            ?: error("No attribute with name \"$attributeName\" exists.")
        check(attribute.kind == AttributeKind.DERIVED) {
            "\"${attribute.name}\" is not a derived attribute, so it does not have a definition to edit."
        }
        val newExpression = valueExpressionFor(valueExpression)
        checkDefinitionEditIsAcyclic(attribute, newExpression)
        val oldExpression = kb.derivedDefinitionManager.definitionFor(attribute.id)
        kb.derivedDefinitionManager.store(attribute.id, newExpression)
        return if (oldExpression == null) {
            "Defined \"${attribute.name}\" as ${newExpression.asText()}."
        } else {
            "Changed the definition of \"${attribute.name}\" from ${oldExpression.asText()} to ${newExpression.asText()}."
        }
    }

    /**
     * Renames a comment or derived attribute. Renaming is not part of rule
     * building — it is allowed whether or not a session is in progress — and
     * changes the attribute's name only, since everything refers to it by id.
     * External attributes cannot be renamed until persisted aliases map the
     * names sent by the external system to renamed attributes; otherwise a
     * case arriving with the old name would create a new attribute.
     */
    override fun renameAttribute(currentName: String, newName: String): String {
        val attribute = attributeForName(currentName)
            ?: error("No attribute with name \"$currentName\" exists.")
        check(attribute.kind.isAssignedByKB()) {
            cannotRenameMessage(attribute.name)
        }
        val oldName = attribute.name
        val renamed = kb.attributeManager.rename(attribute, newName)
        return renamedMessage(oldName, renamed.name)
    }

    /**
     * Refuses a definition edit that would make the attribute depend on
     * itself. The graph is built as if the edit had been made, so cycles
     * through other by-definition rules are detected too.
     */
    internal fun checkDefinitionEditIsAcyclic(attribute: Attribute, newExpression: ValueExpression) {
        val cycle = cycleForDefinition(attribute, newExpression) ?: return
        error("This definition cannot be used: ${cycleMessage(cycle)}.")
    }

    /**
     * The cycle that giving [attribute] the definition [newExpression] would
     * create, or null if there would be none. The graph is built as if the
     * definition were already stored.
     */
    private fun cycleForDefinition(attribute: Attribute, newExpression: ValueExpression): List<Attribute>? {
        val editedResolver: DefinitionResolver = {
            if (it.id == attribute.id) newExpression else kb.definitionResolver(it)
        }
        val graph = DerivedAttributeDependencyGraph(kb.ruleTree, kb.attributeManager.all(), editedResolver)
        val referenced = newExpression.referencedAttributes().filter { it.kind.isAssignedByKB() }.toSet()
        return graph.cycleCreatedBy(attribute, referenced)
    }

    private fun currentAssignmentFor(case: RDRCase, attributeName: String): AssignValue {
        val attribute = kb.attributeManager.byName(attributeName)
            ?: error("No attribute with name \"$attributeName\" exists.")
        kb.interpret(case)
        return case.interpretation.assignments().firstOrNull { it.attribute == attribute }
            ?: error("No value is assigned to \"$attributeName\" for case ${case.name}.")
    }

    /**
     * The value expression for the given user-entered text: a quoted string
     * is a literal, text that parses as arithmetic over the names of existing
     * attributes is a formula, and anything else is a literal.
     *
     * Names must resolve exactly, by [attributeNamedExactly], and no attribute
     * is ever invented to make a formula parse: a name that is no attribute is
     * far more often a typo than an attribute the user means to fill in later,
     * and inventing it yields a formula that can never evaluate, with nothing
     * to tell the user why.
     *
     * Text that names no attribute at all was never meant as a formula, so it
     * is simply a literal — `non-diabetic` is a value, not a subtraction. But
     * text that names some attributes and one non-attribute is genuinely
     * ambiguous, and both readings would mislead if guessed at, so the reading
     * is put back to the user to confirm.
     *
     * [nameBeingDefined] is the attribute this expression is to define, given
     * when it is being defined by an assignment. It may not exist yet, and an
     * expression naming it is then the one unresolved name that is neither a
     * typo nor a literal but a self-reference, which is refused as such.
     */
    internal fun valueExpressionFor(expressionText: String, nameBeingDefined: String? = null): ValueExpression {
        val trimmed = expressionText.trim()
        if (trimmed.length >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return Literal(trimmed.substring(1, trimmed.length - 1))
        }
        // Only treat text as a formula if it contains arithmetic operators, so
        // that plain text, and a bare attribute name, remain literals.
        if (!trimmed.contains(FORMULA_OPERATORS)) return Literal(trimmed)
        val parsed = FormulaParser(::attributeNamedExactly).parse(trimmed)
        if (parsed != null) return Formula(parsed)
        // Asked of every name the text uses, not of the ones the parse reached:
        // it stops at the first name that does not resolve, so "age / weight"
        // and "weight / age" would otherwise be read differently.
        val names = namesInFormula(trimmed)
        val unresolved = names.firstOrNull { attributeNamedExactly(it) == null }
        // The one name that fails to resolve for a good reason: the attribute
        // being defined by this very expression, which does not exist yet. Its
        // definition names it, so the cycle is certain without any graph.
        if (unresolved != null && nameBeingDefined != null && unresolved.equals(nameBeingDefined, ignoreCase = true)) {
            error("This value cannot be assigned: ${cycleMessageForNames(listOf(nameBeingDefined, nameBeingDefined))}.")
        }
        formulaQuestionFor(trimmed)?.let { error(it.message) }
        return Literal(trimmed)
    }

    /**
     * The question put to the user about [expressionText], paired with the
     * expression they accept by answering yes, or null if the text raises no
     * question.
     *
     * The two travel together so that the answer to a question the server asked
     * can be acted on by the server: the model is told to re-send the offered
     * expression when the user accepts, but it re-sends the original often
     * enough, and the two of them then ask and refuse the same thing for ever.
     */
    internal fun formulaQuestionFor(expressionText: String): FormulaQuestion? {
        val trimmed = expressionText.trim()
        if (trimmed.length >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) return null
        if (!trimmed.contains(FORMULA_OPERATORS)) return null
        if (FormulaParser(::attributeNamedExactly).parse(trimmed) != null) return null
        val names = namesInFormula(trimmed)
        val unresolved = names.firstOrNull { attributeNamedExactly(it) == null } ?: return null
        if (names.none { attributeNamedExactly(it) != null }) return null
        val nearest = nearestAttributeName(unresolved)
            ?: return FormulaQuestion(
                unknownAttributeInFormulaMessage(unresolved, trimmed),
                // The offer is of the text as a value, so it is accepted as a
                // literal, which is what quoting it makes it.
                "\"$trimmed\""
            )
        val corrected = trimmed.replace(unresolved, nearest)
        return FormulaQuestion(didYouMeanFormulaMessage(unresolved, corrected), corrected)
    }

    override fun offeredValueExpressionFor(valueExpression: String): String? =
        formulaQuestionFor(valueExpression)?.offeredExpression

    /**
     * The attribute of exactly this name, differing at most in case or in
     * punctuation and whitespace. Unlike [attributeForName] this tolerates no
     * misspelling at all, because a formula is not a passing remark: it is
     * stored as a definition, applied to every later case, and its text is
     * never put in front of the user again. Attribute names one edit apart are
     * commonplace — "weight" and "height" differ by a single character — so a
     * silent near-match would quietly compute a plausible number from the wrong
     * attribute for ever. A name that does not resolve exactly is asked about.
     */
    private fun attributeNamedExactly(name: String): Attribute? {
        val attributes = kb.attributeManager.all()
        attributes.firstOrNull { it.name.equals(name, ignoreCase = true) }?.let { return it }
        val target = name.normalizeForComparison()
        return attributes.firstOrNull { it.name.normalizeForComparison() == target }
    }

    /**
     * The name of the attribute nearest [name], for suggesting the correction
     * the user is likely to have meant, or null if nothing is close. This one
     * merely asks, so it can afford the distance of two that classic
     * Levenshtein scores a transposition such as "hieght".
     */
    private fun nearestAttributeName(name: String): String? =
        kb.attributeManager.all()
            .map { it to levenshtein(it.name.lowercase(), name.lowercase()) }
            .filter { (attribute, distance) -> distance <= maxOf(2, attribute.name.length / 3) }
            .minByOrNull { it.second }
            ?.first?.name

    /**
     * How an assignment reads to the user: a comment as its (truncated) text,
     * resolved through the attribute's definition when the rule assigns it by
     * definition, and anything else as the assignment itself.
     */
    private fun describe(assignment: AssignValue): String {
        if (assignment.attribute.kind != AttributeKind.COMMENT) return assignment.asText()
        val expression = assignment.expression.resolvedFor(assignment.attribute, kb.definitionResolver)
        val text = when (expression) {
            is CommentTemplate -> expression.textWithVariableNames { id -> attributeById(id) }
            is Literal -> expression.value
            else -> assignment.attribute.name
        }
        return text.truncatedComment()
    }

    override fun attributeById(id: Int): Attribute? =
        runCatching { kb.attributeManager.getById(id) }.getOrNull()

    override fun allAttributes(): Set<Attribute> = kb.attributeManager.all()

    override fun attributeForName(name: String): Attribute? {
        val attributes = kb.attributeManager.all()
        // Exact, case-insensitive match.
        attributes.firstOrNull { it.name.equals(name, ignoreCase = true) }?.let { return it }
        // Tolerate differences in punctuation/whitespace.
        val target = name.normalizeForComparison()
        attributes.firstOrNull { it.name.normalizeForComparison() == target }?.let { return it }
        // Tolerate small misspellings via edit distance, scaled to the attribute name length.
        return attributes
            .map { it to levenshtein(it.name.lowercase(), name.lowercase()) }
            .filter { (attribute, distance) -> distance <= maxOf(1, attribute.name.length / 4) }
            .minByOrNull { it.second }
            ?.first
    }

    override fun sendCornerstoneStatus() {
        val cornerstoneStatus = cornerstoneStatus(selectedCornerstone)
        runBlocking { webSocketManager?.sendStatus(cornerstoneStatus) }
    }

    override fun sendRuleSessionCompleted() {
        runBlocking { webSocketManager?.sendRuleSessionCompleted() }
    }

    override fun removeCondition(conditionId: Int): CornerstoneStatus {
        val session = activeRuleSession("No rule session in progress.")
        val condition = kb.conditionManager.getById(conditionId)
        session.removeCondition(condition)
        return cornerstoneStatus(null)
    }

    override fun removeConditionByText(conditionText: String): CornerstoneStatus {
        val session = activeRuleSession("No rule session in progress.")
        val condition = session.conditions.firstOrNull { it.asText() == conditionText }
            ?: throw IllegalArgumentException("Condition not found in current rule session: $conditionText")
        session.removeCondition(condition)
        return cornerstoneStatus(null)
    }

    fun cancelRuleSession() {
        check(ruleSession != null) { "No rule session in progress." }
        ruleSession = null
        currentChange = null
        commentAttributeInSession = null
        diffAttribute = null
        replacedDiffAttribute = null
    }

    override fun cancelCurrentRuleSession() = cancelRuleSession()

    fun conflictingCasesInCurrentRuleSession(): List<RDRCase> = activeRuleSession().cornerstoneCases()

    override fun addConditionToCurrentRuleSession(condition: Condition) {
        val session = activeRuleSession()
        // Align the provided condition with that in the condition manager.
        val conditionId = condition.id
        val conditionToUse = if (conditionId == null) {
            kb.conditionManager.getOrCreate(condition)
        } else {
            val existing = kb.conditionManager.getById(conditionId)
            // Check that there's no confusion between the condition provided
            // and the one that already exists (here we're defending against test code
            // that might have mixed things up).
            require(existing.sameAs(condition)) {
                "Condition provided does not match that in the condition manager."
            }
            existing
        }
        cycleMessageFor(conditionToUse)?.let { throw IllegalArgumentException(it) }
        session.addCondition(conditionToUse)
    }

    override fun commitCurrentRuleSession() {
        val session = activeRuleSession()
        // Internal invariant: the entry points refuse cycle-creating
        // conditions, so this should never fire.
        session.conditions.forEach { condition ->
            check(cycleMessageFor(condition) == null) {
                "Cannot commit rule session: ${cycleMessageFor(condition)}"
            }
        }
        val rulesAdded = session.commit()
        kb.ruleSessionRecorder.recordRuleSessionCommitted(rulesAdded)
        kb.addCornerstoneCaseIfNoEquivalentAlreadyPresent(session.case)
        ruleSession = null
        currentChange = null
        commentAttributeInSession = null
        diffAttribute = null
        replacedDiffAttribute = null
        sendCasesInfo()
    }

    override fun exemptCornerstoneCase() = exemptCornerstone(cornerstoneStatus().indexOfCornerstoneToReview)

    override fun selectCornerstoneCase(index: Int) = selectCornerstone(index)

    override fun descriptionOfMostRecentRule(): UndoRuleDescription {
        val record = kb.ruleSessionRecorder.idsOfRulesAddedInMostRecentSession()
            ?: return UndoRuleDescription("There are no rules to undo.", false)
        val idOfExemplar = record.idsOfRulesAddedInSession.random()
        val exemplar = kb.ruleTree.ruleForId(idOfExemplar)
        return UndoRuleDescription(exemplar.actionSummary { describe(it) }, true)
    }

    fun ruleSessionHistories() = kb.ruleSessionRecorder.allRuleSessionHistories()

    override fun undoLastRuleSession() {
        val record = checkNotNull(kb.ruleSessionRecorder.idsOfRulesAddedInMostRecentSession()) {
            "There are no rules to undo."
        }
        record.idsOfRulesAddedInSession.forEach {
            val toDelete = kb.ruleTree.ruleForId(it)
            kb.ruleManager.deleteLeafRule(toDelete)
        }
        kb.ruleSessionRecorder.delete(kb.ruleSessionRecorder.allRuleSessionHistories().last())
    }

    private fun activeRuleSession(message: String = "Rule session not started."): RuleBuildingSession {
        logger.debug("checking session")
        return checkNotNull(ruleSession) { message }
    }

    override fun conditionHintsForCase(case: RDRCase): ConditionList {
        // Materialise the case so that derived attributes assigned by existing
        // rules are visible to the suggestion generators.
        val materialisedCase = kb.ruleTree.materialise(case, kb.definitionResolver)
        val ctx = SuggestionContext(
            sessionCase = materialisedCase,
            attributes = kb.attributeManager.all(),
            action = ruleSession?.action,
            cornerstones = ruleSession?.cornerstoneCases().orEmpty(),
            ruleTree = kb.ruleTree,
            definitionResolver = kb.definitionResolver,
        )
        return ConditionList(ConditionSuggester(ctx).suggestions())
    }

    override fun conditionForSuggestionText(case: RDRCase, conditionText: String): Condition? {
        return conditionHintsForCase(case).suggestions
            .firstOrNull { !it.isEditable() && it.asText() == conditionText }
            ?.initialSuggestion()
    }

    override fun currentRuleSessionConditionTexts(): Set<String> {
        return ruleSession?.conditions?.map { it.asText() }?.toSet() ?: emptySet()
    }

    override fun isRuleSessionActive(): Boolean = ruleSession != null

    /**
     * @param request the request containing the currently selected cornerstone and an updated list of conditions
     *
     * @return the CornerstoneStatus for the current session where the cornerstone specified in the request should remain selected if it is still in the list of cornerstones
     * after the new set of conditions have been applied
     */
    fun updateCornerstone(request: UpdateCornerstoneRequest): CornerstoneStatus {
        val session = activeRuleSession()

        //replace the conditions in the current session with the updated ones
        session.conditions = request.conditionList.conditions.toMutableSet()

        //update the cornerstone status
        val currentCC = request.cornerstoneStatus.cornerstoneToReview
        return cornerstoneStatus(currentCC)
    }

    /**
     * @param index the index of the cornerstone to be exempted
     *
     * @return the CornerstoneStatus for the current session after the specified cornerstone has been exempted
     */
    fun exemptCornerstone(index: Int): CornerstoneStatus {
        val session = activeRuleSession()

        val currentCornerstones = session.cornerstoneCases()
        if (index < 0 || currentCornerstones.isEmpty()) {
            selectedCornerstone = null
            return CornerstoneStatus()
        }
        val toExempt = currentCornerstones[index]
        session.exemptCornerstone(toExempt)

        val cornerstones = session.cornerstoneCases()
        return if (cornerstones.isEmpty()) {
            selectedCornerstone = null
            CornerstoneStatus()
        } else {
            val newCC = cornerstones[index.coerceAtMost(cornerstones.size - 1)]
            val viewable = viewableCase(newCC)
            selectedCornerstone = viewable
            cornerstoneStatus(viewable)
        }
    }

    /**
     * @param index the index of the cornerstone to be selected
     * @return the CornerstoneStatus for the current session after the specified cornerstone has been selected
     */
    fun selectCornerstone(index: Int): CornerstoneStatus {
        val cornerstones = activeRuleSession().cornerstoneCases()
        val caseInstance = cornerstones[index]
        // Because Interpretation is not immutable, we need to copy
        // the case with a new interpretation (copy is not deep)
        // to make this thread safe.
        val newCC = caseInstance.copy(interpretation = Interpretation(caseInstance.caseId))
        val viewable = viewableCase(newCC)
        selectedCornerstone = viewable
        return CornerstoneStatus(viewable, index, cornerstones.size)
    }

    override fun cornerstoneStatus(): CornerstoneStatus = cornerstoneStatus(selectedCornerstone)

    /**
     * @return the CornerstoneStatus for the current session where the specified cornerstone should remain selected if it is still in the list of cornerstones
     */
    internal fun cornerstoneStatus(currentCornerstone: ViewableCase?): CornerstoneStatus {
        val session = activeRuleSession()
        val cornerstones: List<RDRCase> = session.cornerstoneCases()
        val conditionTexts = session.conditions.map { it.asText() }
        if (cornerstones.isEmpty()) return CornerstoneStatus(
            pendingChange = pendingChange,
            ruleConditions = conditionTexts
        )

        //if no cornerstone has been selected yet, or the selected cornerstone is no longer in the list of cornerstones, return the first one
        var index = 0
        if (currentCornerstone != null) {
            // Match by case id: the selected cornerstone is a viewable copy with
            // materialised derived values, so whole-case equality does not hold
            // against the raw cornerstone cases.
            index = cornerstones.indexOfFirst { it.caseId == currentCornerstone.case.caseId }
        }
        index = if (index >= 0) index else 0
        val cornerstone = cornerstones[index]
        val viewableCornerstone = kb.viewableCase(cornerstone)
        return CornerstoneStatus(viewableCornerstone, index, cornerstones.size, pendingChange, conditionTexts)
    }

    //Allow a mock parser to be set so we can avoid connecting to Gemini for all the tests
    fun setConditionParser(parser: ConditionParser) {
        conditionParser = parser
    }

    override fun conditionForExpression(case: RDRCase, expression: String): ConditionParsingResult {
        val attributeFor: AttributeFor = { kb.attributeManager.getOrCreate(it) }
        val condition = conditionParser.parse(expression, attributeFor)
        return validated(condition, case, expression)
    }

    override fun conditionForEditedSuggestion(
        case: RDRCase,
        editableCondition: EditableCondition,
        value: String
    ): ConditionParsingResult {
        val editableValue = editableCondition.editableValue()
        if (!editableValue.type.valid(value)) {
            val asItStands = editableCondition.condition(editableValue.value).asText()
            return ConditionParsingResult(errorMessage = NOT_A_VALID_VALUE.format(value, asItStands))
        }
        val condition = editableCondition.condition(value)
        return validated(condition, case, condition.asText())
    }

    /**
     * The stored form of [condition], or the reason it cannot be used as a
     * condition of the rule being built. [expression] is what the user gave, so
     * that a condition that does not hold can be reported in their own words.
     */
    private fun validated(
        condition: Condition?,
        case: RDRCase,
        expression: String
    ): ConditionParsingResult {
        // Materialise the case so that derived attributes assigned by existing
        // rules are visible when validating the typed expression.
        val materialisedCase = kb.ruleTree.materialise(case, kb.definitionResolver)
        //Only return the condition if non-null and holds for the case
        val caseAttributeNames = materialisedCase.attributes.map { it.name }.toSet()
        return if (condition == null) {
            ConditionParsingResult(errorMessage = DOES_NOT_CORRESPOND_TO_A_CONDITION)
        } else if (condition.attributeNames().any { it !in caseAttributeNames }) {
            ConditionParsingResult(errorMessage = DOES_NOT_CORRESPOND_TO_A_CONDITION)
        } else if (!condition.holds(materialisedCase)) {
            val message = if (expression.normalizeForComparison() != condition.asText().normalizeForComparison()) {
                INTERPRETED_CONDITION_IS_NOT_TRUE.format(expression, condition.asText())
            } else {
                CONDITION_IS_NOT_TRUE
            }
            ConditionParsingResult(errorMessage = message)
        } else {
            val cycleError = cycleMessageFor(condition)
            if (cycleError != null) {
                ConditionParsingResult(errorMessage = cycleError)
            } else {
                //if this a new condition, the following will store it with its user expression, else the existing condition will be returned
                ConditionParsingResult(kb.conditionManager.getOrCreate(condition))
            }
        }
    }

    override fun copyCaseToFavourites(case: ViewableCase, newName: String?): RDRCase {
        val caseId = requireNotNull(case.id) { "Cannot copy a case that has no persisted id." }
        val copied = kb.copyCaseAsFavourite(caseId, newName)
        sendCasesInfo()
        return copied
    }

    override fun deleteCaseFromFavourites(case: ViewableCase) {
        kb.deleteCaseFromFavourites(case.case)
        sendCasesInfo()
    }

    private fun casesInfo() = CasesInfo(
        caseIds = kb.processedCaseIds(),
        cornerstoneCaseIds = kb.cornerstoneCaseIds(),
        favouriteCaseIds = kb.favouriteCaseIds(),
        kbName = kb.kbInfo.name
    )

    private fun sendCasesInfo() {
        runBlocking { webSocketManager?.sendCasesInfo(casesInfo()) }
    }

    fun conditionForExpression(expression: String) = conditionForExpression(activeRuleSession().case, expression)

    private fun viewableCase(case: RDRCase): ViewableCase {
        return kb.viewableCase(case)
    }

    override fun moveAttributeTo(moved: String, destination: String) {
        val attributeMoved = kb.attributeManager.all().first { it.name.equals(moved) }
        val attributeDestination = kb.attributeManager.all().first { it.name.equals(destination) }
        kb.caseViewManager.move(attributeMoved, attributeDestination)
    }

    fun startRuleSession(sessionStartRequest: SessionStartRequest): CornerstoneStatus {
        logger.info("startRuleSession with data $sessionStartRequest")
        val caseId = sessionStartRequest.caseId
        val diff = sessionStartRequest.diff
        val case = kb.getProcessedCase(caseId) ?: throw IllegalArgumentException("Case with id $caseId not found")
        kb.interpret(case)
        // Each of these records the change itself, naming the comment attribute
        // it concerns. That named change is kept, rather than the client's
        // unnamed one, because every CornerstoneStatus built during the session
        // carries it to the Comments panel.
        return when (diff) {
            is Addition -> {
                val (comment, variables) = internalForm(diff.right())
                startRuleSessionToAddComment(case, comment, variables)
            }

            is Removal -> startRuleSessionToRemoveComment(case, internalForm(diff.left()).first)

            is Replacement -> {
                val (replacement, variables) = internalForm(diff.right())
                startRuleSessionToReplaceComment(case, internalForm(diff.left()).first, replacement, variables)
            }
        }
    }

    fun commitRuleSession(ruleRequest: RuleRequest): ViewableCase {
        logger.info("Committing rule session for $ruleRequest")
        val caseId = ruleRequest.caseId
        val case = kb.viewableCase(
            kb.getProcessedCase(caseId) ?: throw IllegalArgumentException("Case with id $caseId not found")
        )
        ruleRequest.conditions.conditions.forEach { condition ->
            logger.info("adding condition: $condition")
            addConditionToCurrentRuleSession(condition)
        }
        commitCurrentRuleSession()
        logger.info("rule session committed")
        // Rebuild the viewable case so that the new rule's effects are shown,
        // with by-definition assignments resolved for display.
        val updated = kb.viewableCase(case.case)
        logger.info("Updated interpretation after committing the rule: ${updated.viewableInterpretation}")
        return updated
    }

    /**
     * Build a complete rule in one call, without using the UI.
     * Condition expressions are parsed deterministically from human-readable text.
     */
    fun buildRule(request: BuildRuleRequest) {
        logger.info("buildRule: case='${request.caseName}', diff=${request.diff}, conditions=${request.conditions}, assignAttribute=${request.assignAttribute}")
        val case = kb.getProcessedCaseByName(request.caseName)
        kb.interpret(case)
        val viewableCase = kb.viewableCase(case)
        val assignAttribute = request.assignAttribute
        if (assignAttribute != null) {
            startRuleSessionToAssignValue(viewableCase.case, assignAttribute, request.assignExpression ?: "")
        } else {
            when (val diff = request.diff) {
                is Addition -> {
                    val (comment, variables) = internalForm(diff.addedText)
                    startRuleSessionToAddComment(viewableCase, comment, variables)
                }

                is Removal -> startRuleSessionToRemoveComment(viewableCase, internalForm(diff.removedText).first)

                is Replacement -> {
                    val (replacement, variables) = internalForm(diff.replacementText)
                    startRuleSessionToReplaceComment(
                        viewableCase,
                        internalForm(diff.originalText).first,
                        replacement,
                        variables
                    )
                }
            }
        }
        try {
            val parser = ConditionExpressionParser { kb.attributeManager.getOrCreate(it) }
            request.conditions.forEach { expression ->
                val condition = parser.parse(expression)
                addConditionToCurrentRuleSession(condition)
            }
            commitCurrentRuleSession()
            logger.info("buildRule: completed for case='${request.caseName}'")
        } catch (e: Exception) {
            logger.error("buildRule: failed for case='${request.caseName}': ${e.message}")
            cancelRuleSession()
            throw e
        }
    }
}

/**
 * A question the server puts to the user about a value expression, with the
 * expression they are accepting by answering yes.
 */
internal data class FormulaQuestion(val message: String, val offeredExpression: String)

/**
 * Classic Levenshtein edit distance, used to tolerate small misspellings when matching a
 * user-supplied attribute name to a known attribute.
 */
internal fun levenshtein(lhs: CharSequence, rhs: CharSequence): Int {
    val lhsLength = lhs.length
    val rhsLength = rhs.length
    if (lhsLength == 0) return rhsLength
    if (rhsLength == 0) return lhsLength

    var previousRow = IntArray(rhsLength + 1) { it }
    var currentRow = IntArray(rhsLength + 1)
    for (i in 1..lhsLength) {
        currentRow[0] = i
        for (j in 1..rhsLength) {
            val substitutionCost = if (lhs[i - 1] == rhs[j - 1]) 0 else 1
            currentRow[j] = minOf(
                currentRow[j - 1] + 1,          // insertion
                previousRow[j] + 1,             // deletion
                previousRow[j - 1] + substitutionCost // substitution
            )
        }
        val swap = previousRow
        previousRow = currentRow
        currentRow = swap
    }
    return previousRow[rhsLength]
}
