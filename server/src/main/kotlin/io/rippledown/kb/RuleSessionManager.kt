package io.rippledown.kb

import io.rippledown.constants.rule.CONDITION_IS_NOT_TRUE
import io.rippledown.constants.rule.DOES_NOT_CORRESPOND_TO_A_CONDITION
import io.rippledown.constants.rule.INTERPRETED_CONDITION_IS_NOT_TRUE
import io.rippledown.hints.AttributeFor
import io.rippledown.hints.ConditionChatService
import io.rippledown.hints.ConditionGenerator
import io.rippledown.kb.chat.RuleService
import io.rippledown.kb.chat.action.nameClashWithExistingExternalAttributeMessage
import io.rippledown.log.lazyLogger
import io.rippledown.model.*
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.ConditionList
import io.rippledown.model.condition.ConditionParsingResult
import io.rippledown.model.diff.*
import io.rippledown.model.rule.*
import io.rippledown.server.ConditionExpressionParser
import io.rippledown.server.websocket.WebSocketManager
import io.rippledown.suggestions.ConditionSuggester
import io.rippledown.suggestions.SuggestionContext
import kotlinx.coroutines.runBlocking

class RuleSessionManager(
    private val kb: KB,
    private val webSocketManager: WebSocketManager? = null
) : RuleService {
    val logger = lazyLogger

    private var ruleSession: RuleBuildingSession? = null

    /**
     * The change the session in progress is about to make. A session makes one
     * change, so this is one field; [currentDiff] and [currentDerivedValueChange]
     * are read views of it for the code that handles only one kind.
     */
    internal var currentChange: PendingChange? = null

    internal val currentDiff: Diff?
        get() = currentChange as? Diff

    internal val currentDerivedValueChange: DerivedValueChange?
        get() = currentChange as? DerivedValueChange

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
        logger.info("Current conclusions are: ${case.interpretation.conclusionTexts()} ")
        check(ruleSession == null) { "Session already in progress." }
        check(action.isApplicable(kb.ruleTree, case)) { "Action $action is not applicable to case ${case.name}" }
        checkActionExpressionIsAcyclic(action)
        val alignedAction = action.alignWith(kb.conclusionManager)
        ruleSession = RuleBuildingSession(
            kb.ruleManager, kb.ruleTree, case, alignedAction, kb.allCornerstoneCases(), kb.definitionResolver
        )
        logger.info("Rule session created")
        return cornerstoneStatus(null)
    }

    override fun startRuleSessionToAddComment(
        viewableCase: ViewableCase,
        comment: String,
        variables: List<CommentVariable>
    ): CornerstoneStatus = startRuleSessionToAddComment(viewableCase.case, comment, variables)

    /**
     * Comments are comment attributes: adding one gets or creates the
     * attribute whose definition is the comment's text, and builds a rule
     * assigning it by definition. See "Phase 2 — comments become derived
     * attributes" in documentation/design/repeat_inferencing.md.
     */
    internal fun startRuleSessionToAddComment(
        case: RDRCase,
        comment: String,
        variables: List<CommentVariable> = emptyList()
    ): CornerstoneStatus {
        val template = commentTemplate(comment, variables)
        val attribute = commentAttributeFor(template)
        currentChange = Addition(template.textWithVariableNames())
        return startRuleSession(case, ChangeTreeToAddAssignment(AssignValue(attribute, ByDefinition)))
    }

    override fun startRuleSessionToRemoveComment(viewableCase: ViewableCase, comment: String): CornerstoneStatus =
        startRuleSessionToRemoveComment(viewableCase.case, comment)

    internal fun startRuleSessionToRemoveComment(case: RDRCase, comment: String): CornerstoneStatus {
        val attribute = commentAttributeForText(comment)
            ?: error("Cannot remove comment: no comment matching \"$comment\" exists.")
        currentChange = Removal(renderedComment(attribute, case))
        return startRuleSession(case, ChangeTreeToRemoveAssignment(AssignValue(attribute, ByDefinition)))
    }

    override fun startRuleSessionToReplaceComment(
        viewableCase: ViewableCase,
        replacedComment: String,
        replacementComment: String,
        variables: List<CommentVariable>
    ): CornerstoneStatus =
        startRuleSessionToReplaceComment(viewableCase.case, replacedComment, replacementComment, variables)

    /**
     * Each comment text has its own attribute, so the replacement is a new
     * (or existing) attribute for the replacement text: the replacing rule
     * assigns it, and leaf-most suppression retracts the original. See
     * "Phase 2" in documentation/design/repeat_inferencing.md.
     */
    internal fun startRuleSessionToReplaceComment(
        case: RDRCase,
        replacedComment: String,
        replacementComment: String,
        variables: List<CommentVariable> = emptyList()
    ): CornerstoneStatus {
        val replacedAttribute = commentAttributeForText(replacedComment)
            ?: error("Cannot replace comment: no comment matching \"$replacedComment\" exists.")
        val replacementTemplate = commentTemplate(replacementComment, variables)
        val replacementAttribute = commentAttributeFor(replacementTemplate)
        currentChange =
            Replacement(renderedComment(replacedAttribute, case), replacementTemplate.textWithVariableNames())
        return startRuleSession(
            case,
            ChangeTreeToReplaceAssignment(
                AssignValue(replacedAttribute, ByDefinition),
                AssignValue(replacementAttribute, ByDefinition)
            )
        )
    }

    private fun commentTemplate(comment: String, variables: List<CommentVariable>) =
        CommentTemplate(comment, variables.map { kb.attributeManager.getById(it.attributeId) })

    /**
     * The comment attribute whose definition is the given template,
     * created if necessary.
     */
    private fun commentAttributeFor(template: CommentTemplate): Attribute =
        kb.attributeManager.commentAttributes()
            .firstOrNull { kb.derivedDefinitionManager.definitionFor(it.id) == template }
            ?: kb.attributeManager.createCommentAttribute()
                .also { kb.derivedDefinitionManager.store(it.id, template) }

    /**
     * The comment attribute whose definition has the given (internal-form)
     * text, or null if none exists.
     */
    private fun commentAttributeForText(text: String): Attribute? =
        kb.attributeManager.commentAttributes().firstOrNull {
            (kb.derivedDefinitionManager.definitionFor(it.id) as? CommentTemplate)?.text == text
        }

    private fun renderedComment(attribute: Attribute, case: RDRCase): String =
        (kb.derivedDefinitionManager.definitionFor(attribute.id) as? CommentTemplate)?.render(case)?.text ?: ""

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
        val attribute = kb.attributeManager.getOrCreate(attributeName, AttributeKind.DERIVED)
        val expression = valueExpressionFor(expressionText)
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
     * in the Derived attributes panel while it is in progress. The preview is
     * set before the session starts so that the returned status carries it, and
     * rolled back if the session is refused, so that a request that never became
     * a session leaves nothing behind for the next one to show.
     */
    private fun startAssignmentSession(
        case: RDRCase,
        change: DerivedValueChange,
        action: RuleTreeChange
    ): CornerstoneStatus {
        currentChange = change
        return try {
            startRuleSession(case, action)
        } catch (e: Throwable) {
            currentChange = null
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
        val referenced = newExpression.referencedAttributes().filter { it.kind == AttributeKind.DERIVED }.toSet()
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
     * is a literal, text that parses as arithmetic over attribute names is a
     * formula, and anything else is a literal.
     */
    internal fun valueExpressionFor(expressionText: String): ValueExpression {
        val trimmed = expressionText.trim()
        if (trimmed.length >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return Literal(trimmed.substring(1, trimmed.length - 1))
        }
        // Only treat text as a formula if it contains arithmetic operators, so
        // that plain text like "diabetic" remains a literal. Formulas may
        // reference attributes that are not yet in the KB; those attributes are
        // created so the formula can be evaluated against future cases.
        val hasArithmeticOperators = trimmed.contains(Regex("""[\+\-\*/()]"""))
        val parsed = if (hasArithmeticOperators) {
            val attributeFor: (String) -> Attribute = { name ->
                kb.attributeManager.all()
                    .firstOrNull { it.name.equals(name, ignoreCase = true) }
                    ?: kb.attributeManager.getOrCreate(name)
            }
            FormulaParser(attributeFor).parse(trimmed)
        } else null
        return if (parsed != null) Formula(parsed) else Literal(trimmed)
    }

    /**
     * Render a conclusion for the given case, substituting any comment variables with their attribute
     * values so that the diff/preview shows human-readable text rather than the internal token form.
     */
    private fun renderedText(conclusion: Conclusion, case: RDRCase): String =
        conclusion.render(case) { id -> attributeById(id) }.text

    override fun attributeById(id: Int): Attribute? =
        runCatching { kb.attributeManager.getById(id) }.getOrNull()

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
        check(ruleSession != null) { "No rule session in progress." }
        val condition = kb.conditionManager.getById(conditionId)
        ruleSession!!.removeCondition(condition)
        return cornerstoneStatus(null)
    }

    override fun removeConditionByText(conditionText: String): CornerstoneStatus {
        check(ruleSession != null) { "No rule session in progress." }
        val condition = ruleSession!!.conditions.firstOrNull { it.asText() == conditionText }
            ?: throw IllegalArgumentException("Condition not found in current rule session: $conditionText")
        ruleSession!!.removeCondition(condition)
        return cornerstoneStatus(null)
    }

    fun cancelRuleSession() {
        check(ruleSession != null) { "No rule session in progress." }
        ruleSession = null
        currentChange = null
    }

    override fun cancelCurrentRuleSession() = cancelRuleSession()

    fun conflictingCasesInCurrentRuleSession(): List<RDRCase> {
        checkSession()
        return ruleSession!!.cornerstoneCases()
    }

    override fun addConditionToCurrentRuleSession(condition: Condition) {
        checkSession()
        // Align the provided condition with that in the condition manager.
        val conditionToUse = if (condition.id == null) {
            kb.conditionManager.getOrCreate(condition)
        } else {
            val existing = kb.conditionManager.getById(condition.id!!)
            // Check that there's no confusion between the condition provided
            // and the one that already exists (here we're defending against test code
            // that might have mixed things up).
            require(existing.sameAs(condition)) {
                "Condition provided does not match that in the condition manager."
            }
            existing
        }
        cycleMessageFor(conditionToUse)?.let { throw IllegalArgumentException(it) }
        ruleSession!!.addCondition(conditionToUse)
    }

    override fun commitCurrentRuleSession() {
        checkSession()
        // Internal invariant: the entry points refuse cycle-creating
        // conditions, so this should never fire.
        ruleSession!!.conditions.forEach { condition ->
            check(cycleMessageFor(condition) == null) {
                "Cannot commit rule session: ${cycleMessageFor(condition)}"
            }
        }
        val rulesAdded = ruleSession!!.commit()
        kb.ruleSessionRecorder.recordRuleSessionCommitted(rulesAdded)
        kb.addCornerstoneCaseIfNoEquivalentAlreadyPresent(ruleSession!!.case)
        ruleSession = null
        currentChange = null
        checkRuleSessionHistoryConsistency()
        sendCasesInfo()
    }

    override fun exemptCornerstoneCase() = exemptCornerstone(cornerstoneStatus().indexOfCornerstoneToReview)

    override fun selectCornerstoneCase(index: Int) = selectCornerstone(index)

    override fun descriptionOfMostRecentRule(): UndoRuleDescription {
        val record = kb.ruleSessionRecorder.idsOfRulesAddedInMostRecentSession()
            ?: return UndoRuleDescription("There are no rules to undo.", false)
        val idOfExemplar = record.idsOfRulesAddedInSession.random()
        val exemplar = kb.ruleTree.ruleForId(idOfExemplar)
        val summary = exemplar.actionSummary { conclusion ->
            conclusion.truncatedText { id -> attributeById(id)?.name ?: "unknown" }
        }
        return UndoRuleDescription(summary, true)
    }

    fun ruleSessionHistories() = kb.ruleSessionRecorder.allRuleSessionHistories()

    override fun undoLastRuleSession() {
        val record = kb.ruleSessionRecorder.idsOfRulesAddedInMostRecentSession()!!
        record.idsOfRulesAddedInSession.forEach {
            val toDelete = kb.ruleTree.ruleForId(it)
            kb.ruleManager.deleteLeafRule(toDelete)
        }
        kb.ruleSessionRecorder.delete(kb.ruleSessionRecorder.allRuleSessionHistories().last())
    }

    private fun checkSession() {
        logger.debug("checking session")
        check(ruleSession != null) { "Rule session not started." }
    }

    override fun conditionHintsForCase(case: RDRCase): ConditionList {
        // Materialise the case so that derived attributes assigned by existing
        // rules are visible to the suggestion generators. See step 8a of
        // documentation/design/repeat_inferencing.md.
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
        checkSession()

        //replace the conditions in the current session with the updated ones
        ruleSession!!.conditions = request.conditionList.conditions.toMutableSet()

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
        checkSession()

        val currentCornerstones = ruleSession!!.cornerstoneCases()
        if (index < 0 || currentCornerstones.isEmpty()) {
            selectedCornerstone = null
            return CornerstoneStatus()
        }
        val toExempt = currentCornerstones[index]
        ruleSession!!.exemptCornerstone(toExempt)

        val cornerstones = ruleSession!!.cornerstoneCases()
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
        checkSession()
        val cornerstones = ruleSession!!.cornerstoneCases()
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
        checkSession()
        val cornerstones: List<RDRCase> = ruleSession!!.cornerstoneCases()
        val conditionTexts = ruleSession!!.conditions.map { it.asText() }
        if (cornerstones.isEmpty()) return CornerstoneStatus(
            pendingChange = currentChange,
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
        return CornerstoneStatus(viewableCornerstone, index, cornerstones.size, currentChange, conditionTexts)
    }

    //Allow a mock parser to be set so we can avoid connecting to Gemini for all the tests
    fun setConditionParser(parser: ConditionParser) {
        conditionParser = parser
    }

    override fun conditionForExpression(case: RDRCase, expression: String): ConditionParsingResult {
        val attributeFor: AttributeFor = { kb.attributeManager.getOrCreate(it) }
        val condition = conditionParser.parse(expression, attributeFor)

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
        val copied = kb.copyCaseAsFavourite(case.id!!, newName)
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

    private fun checkRuleSessionHistoryConsistency() {
        val idsOfNonRootRulesInTree = kb.ruleTree.rules().filter { it.parent != null }.map { it.id }.toSet()
    }

    fun conditionForExpression(expression: String) = conditionForExpression(ruleSession!!.case, expression)

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
        currentChange = diff
        val case = kb.getProcessedCase(caseId) ?: throw IllegalArgumentException("Case with id $caseId not found")
        kb.interpret(case)
        val status = when (diff) {
            is Addition -> startRuleSessionToAddComment(case, diff.right())
            is Removal -> startRuleSessionToRemoveComment(case, diff.left())
            is Replacement -> startRuleSessionToReplaceComment(case, diff.left(), diff.right())
        }
        currentChange = diff
        return status
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
                is Addition -> startRuleSessionToAddComment(viewableCase, diff.addedText)
                is Removal -> startRuleSessionToRemoveComment(viewableCase, diff.removedText)
                is Replacement -> startRuleSessionToReplaceComment(
                    viewableCase,
                    diff.originalText,
                    diff.replacementText
                )
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
