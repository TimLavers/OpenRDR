package io.rippledown.kb

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.mockk
import io.rippledown.model.*
import io.rippledown.model.condition.CaseStructureCondition
import io.rippledown.model.condition.greaterThanOrEqualTo
import io.rippledown.model.condition.structural.IsPresentInCase
import io.rippledown.model.diff.Addition
import io.rippledown.model.diff.DerivedValueAddition
import io.rippledown.model.diff.DerivedValueRemoval
import io.rippledown.model.diff.DerivedValueReplacement
import io.rippledown.model.rule.AssignValue
import io.rippledown.model.rule.ByDefinition
import io.rippledown.model.rule.Formula
import io.rippledown.model.rule.Literal
import io.rippledown.persistence.inmemory.InMemoryKB
import io.rippledown.server.websocket.WebSocketManager
import io.rippledown.utils.defaultDate
import kotlin.test.BeforeTest
import kotlin.test.Test

class RuleSessionManagerAssignmentTest {
    private lateinit var kb: KB
    private lateinit var rsm: RuleSessionManager
    private lateinit var webSocketManager: WebSocketManager

    @BeforeTest
    fun setup() {
        val kbInfo = KBInfo("id123", "TestKB")
        kb = KB(InMemoryKB(kbInfo))
        webSocketManager = mockk(relaxed = true)
        rsm = RuleSessionManager(kb, webSocketManager)
    }

    private fun glucose(): Attribute = kb.attributeManager.getOrCreate("Glucose")
    private fun diabetesStatus(): Attribute = kb.attributeManager.getOrCreate("Diabetes status", AttributeKind.DERIVED)

    private fun createCase(name: String, value: String = "12.0"): RDRCase {
        val builder = RDRCaseBuilder()
        builder.addValue(glucose(), defaultDate, value)
        return builder.build(name)
    }

    private fun highGlucose() = greaterThanOrEqualTo(null, glucose(), 11.0)

    private fun buildAssignmentRule() {
        rsm.startRuleSessionToAssignValue(createCase("Setup"), "Diabetes status", "\"diabetic\"")
        rsm.addConditionToCurrentRuleSession(highGlucose())
        rsm.commitCurrentRuleSession()
    }

    // --- valueExpressionFor ---

    @Test
    fun `a quoted expression is a literal`() {
        rsm.valueExpressionFor("\"diabetic\"") shouldBe Literal("diabetic")
    }

    @Test
    fun `an arithmetic expression over attributes is a formula`() {
        // Given attributes referenced by an expression
        val weight = kb.attributeManager.getOrCreate("weight")
        val height = kb.attributeManager.getOrCreate("height")

        // When the expression is parsed
        val expression = rsm.valueExpressionFor("weight / (height * height)")

        // Then it is a formula that evaluates against a case
        val case = with(RDRCaseBuilder()) {
            addValue(weight, defaultDate, "93.0")
            addValue(height, defaultDate, "1.8")
            build("Case")
        }
        (expression is Formula) shouldBe true
        expression.evaluate(case) shouldBe "28.7"
    }

    @Test
    fun `text that is not arithmetic over attributes is a literal`() {
        rsm.valueExpressionFor("diabetic") shouldBe Literal("diabetic")
        rsm.valueExpressionFor("not a formula") shouldBe Literal("not a formula")
    }

    @Test
    fun `an arithmetic expression resolves attribute names case-insensitively`() {
        // Given existing attributes with capitalised names
        val weight = kb.attributeManager.getOrCreate("Weight")
        val height = kb.attributeManager.getOrCreate("Height")

        // When the expression uses lower-case names
        val expression = rsm.valueExpressionFor("weight / (height * height)")

        // Then it is a formula that references the existing Weight and Height attributes
        (expression is Formula) shouldBe true
        expression.referencedAttributes() shouldBe setOf(weight, height)

        // And evaluates correctly against a case with those attributes
        val case = with(RDRCaseBuilder()) {
            addValue(weight, defaultDate, "93.0")
            addValue(height, defaultDate, "1.8")
            build("Case")
        }
        expression.evaluate(case) shouldBe "28.7"
    }

    @Test
    fun `an arithmetic expression referencing a missing attribute is a formula that evaluates to null`() {
        // Given an expression that references an attribute not yet in the KB
        val weight = kb.attributeManager.getOrCreate("weight")
        val expression = rsm.valueExpressionFor("weight / age")

        // Then it is parsed as a formula
        (expression is Formula) shouldBe true

        // And evaluating it against a case without the referenced attribute makes no assignment
        val case = with(RDRCaseBuilder()) {
            addValue(weight, defaultDate, "93.0")
            build("Case")
        }
        expression.evaluate(case).shouldBeNull()
    }

    // --- assign value sessions ---

    @Test
    fun `an assign value session creates a by-definition rule and stores the definition`() {
        // When an assign-value session is committed
        buildAssignmentRule()

        // Then the assignment is made for cases satisfying the condition,
        // with the rule pointing at the attribute's stored definition
        val interpretation = kb.interpret(createCase("A", "12.0"))
        interpretation.assignments() shouldBe setOf(AssignValue(diabetesStatus(), ByDefinition))
        kb.derivedDefinitionManager.definitionFor(diabetesStatus().id) shouldBe Literal("diabetic")
        kb.viewableCase(kb.addProcessedCase(createCase("A", "12.0"))).case
            .latestValue(diabetesStatus()) shouldBe "diabetic"
        kb.interpret(createCase("B", "5.0")).assignments() shouldBe emptySet()
    }

    @Test
    fun `an assign value session creates the derived attribute if necessary`() {
        // Given no attribute with the name exists
        kb.attributeManager.byName("Risk level").shouldBeNull()

        // When an assign-value session is started
        rsm.startRuleSessionToAssignValue(createCase("A"), "Risk level", "\"high\"")

        // Then the derived attribute has been created
        kb.attributeManager.byName("Risk level")?.kind shouldBe AttributeKind.DERIVED
    }

    @Test
    fun `a remove assignment session creates a stopping rule`() {
        // Given a KB with an assignment rule
        buildAssignmentRule()

        // When a remove-assignment session is committed for a very high glucose case
        val case = createCase("C", "25.0")
        rsm.startRuleSessionToRemoveAssignment(case, "Diabetes status")
        rsm.addConditionToCurrentRuleSession(greaterThanOrEqualTo(null, glucose(), 20.0))
        rsm.commitCurrentRuleSession()

        // Then the assignment is retracted for such cases only
        kb.interpret(createCase("D", "25.0")).assignments() shouldBe emptySet()
        kb.interpret(createCase("E", "12.0")).assignments() shouldBe
                setOf(AssignValue(diabetesStatus(), ByDefinition))
    }

    @Test
    fun `a replace assignment session creates a rule with the replacement`() {
        // Given a KB with an assignment rule
        buildAssignmentRule()

        // When a replace-assignment session is committed for a very high glucose case
        val case = createCase("C", "25.0")
        rsm.startRuleSessionToReplaceAssignment(case, "Diabetes status", "\"severely diabetic\"")
        rsm.addConditionToCurrentRuleSession(greaterThanOrEqualTo(null, glucose(), 20.0))
        rsm.commitCurrentRuleSession()

        // Then the replacement applies for such cases only, as a concrete
        // override, while the base rule still assigns by definition
        kb.interpret(createCase("D", "25.0")).assignments() shouldBe
                setOf(AssignValue(diabetesStatus(), Literal("severely diabetic")))
        kb.interpret(createCase("E", "12.0")).assignments() shouldBe
                setOf(AssignValue(diabetesStatus(), ByDefinition))
    }

    // --- cycle prevention ---

    private fun riskLevel(): Attribute = kb.attributeManager.getOrCreate("Risk level", AttributeKind.DERIVED)

    /**
     * Builds two rules: Risk level is assigned for high glucose, and
     * Diabetes status is assigned when Risk level is in the case. So
     * Diabetes status depends on Risk level.
     */
    private fun buildDependentRules() {
        rsm.startRuleSessionToAssignValue(createCase("Setup1"), "Risk level", "\"high\"")
        rsm.addConditionToCurrentRuleSession(highGlucose())
        rsm.commitCurrentRuleSession()

        rsm.startRuleSessionToAssignValue(createCase("Setup2"), "Diabetes status", "\"diabetic\"")
        rsm.addConditionToCurrentRuleSession(CaseStructureCondition(null, IsPresentInCase(riskLevel()), ""))
        rsm.commitCurrentRuleSession()
    }

    @Test
    fun `a condition that would create a dependency cycle is refused`() {
        // Given Diabetes status depends on Risk level
        buildDependentRules()

        // When a session assigning Risk level tries to use a condition on Diabetes status
        rsm.startRuleSessionToAssignValue(createCase("C", "5.0"), "Risk level", "\"low\"")

        // Then the condition is refused, with the cycle named
        shouldThrow<IllegalArgumentException> {
            rsm.addConditionToCurrentRuleSession(CaseStructureCondition(null, IsPresentInCase(diabetesStatus()), ""))
        }.message shouldBe "This condition cannot be used: it would make \"Risk level\" depend on itself " +
                "(Risk level → Diabetes status → Risk level)."

        // And a condition on an external attribute is accepted
        rsm.addConditionToCurrentRuleSession(greaterThanOrEqualTo(null, glucose(), 1.0))
    }

    @Test
    fun `an assignment whose expression references its own attribute is refused`() {
        // Given a derived attribute
        kb.attributeManager.getOrCreate("BMI", AttributeKind.DERIVED)

        // When a session assigns it a value computed from itself
        // Then the session is refused, with the cycle named
        shouldThrow<IllegalStateException> {
            rsm.startRuleSessionToAssignValue(createCase("A"), "BMI", "BMI * 2")
        }.message shouldBe "This value cannot be assigned: it would make \"BMI\" depend on itself (BMI → BMI)."
    }

    @Test
    fun `suggestions include derived attributes assigned by existing rules`() {
        // Given an assignment rule that derives Diabetes status from high glucose
        rsm.startRuleSessionToAssignValue(createCase("Setup"), "Diabetes status", "\"diabetic\"")
        rsm.addConditionToCurrentRuleSession(greaterThanOrEqualTo(null, glucose(), 11.0))
        rsm.commitCurrentRuleSession()

        // When a comment rule session is started on a high-glucose case and we ask for suggestions
        val highGlucoseCase = kb.addProcessedCase(createCase("CG", "12.0"))
        rsm.startRuleSessionToAddComment(kb.viewableCase(highGlucoseCase), "Advice given.")
        val suggestions = rsm.conditionHintsForCase(highGlucoseCase).suggestions.map { it.asText() }

        // Then a condition on the derived attribute is suggested
        suggestions.any { "Diabetes status" in it } shouldBe true
    }

    @Test
    fun `typed conditions on derived attributes assigned by existing rules are accepted`() {
        // Given an assignment rule that derives Diabetes status from high glucose
        rsm.startRuleSessionToAssignValue(createCase("Setup"), "Diabetes status", "\"diabetic\"")
        rsm.addConditionToCurrentRuleSession(greaterThanOrEqualTo(null, glucose(), 11.0))
        rsm.commitCurrentRuleSession()

        // When a comment rule session is started and a condition on the derived attribute is typed
        val highGlucoseCase = kb.addProcessedCase(createCase("CG", "12.0"))
        rsm.startRuleSessionToAddComment(kb.viewableCase(highGlucoseCase), "Advice given.")
        rsm.setConditionParser(object : ConditionParser {
            override fun parse(expression: String, attributeFor: (String) -> Attribute) =
                CaseStructureCondition(null, IsPresentInCase(diabetesStatus()), expression)
        })

        // Then the condition is accepted
        val result = rsm.conditionForExpression(highGlucoseCase, "Diabetes status is in case")
        result.condition shouldNotBe null
        result.condition?.asText() shouldBe "Diabetes status is in case"
        result.errorMessage.shouldBeNull()
    }

    @Test
    fun `a typed expression that would create a dependency cycle is reported, not returned`() {
        // Given Diabetes status depends on Risk level, and a session assigning Risk level
        buildDependentRules()
        rsm.startRuleSessionToAssignValue(createCase("C", "5.0"), "Risk level", "\"low\"")
        rsm.setConditionParser(object : ConditionParser {
            override fun parse(expression: String, attributeFor: (String) -> Attribute) =
                CaseStructureCondition(null, IsPresentInCase(diabetesStatus()), expression)
        })

        // When a cycle-creating condition is typed, for a case with the derived value
        val caseWithDerived = with(RDRCaseBuilder()) {
            addValue(glucose(), defaultDate, "12.0")
            addValue(diabetesStatus(), defaultDate, "diabetic")
            build("D")
        }
        val result = rsm.conditionForExpression(caseWithDerived, "Diabetes status is in case")

        // Then the result carries the cycle message
        result.condition.shouldBeNull()
        result.errorMessage shouldBe "This condition cannot be used: it would make \"Risk level\" depend on itself " +
                "(Risk level → Diabetes status → Risk level)."
    }

    @Test
    fun `a remove assignment session cannot be started for an unknown attribute`() {
        shouldThrow<IllegalStateException> {
            rsm.startRuleSessionToRemoveAssignment(createCase("A"), "No such attribute")
        }.message shouldBe "No attribute with name \"No such attribute\" exists."
    }

    @Test
    fun `a remove assignment session cannot be started if the case has no assignment for the attribute`() {
        // Given a KB with an assignment rule for high glucose
        buildAssignmentRule()

        // When a remove-assignment session is started for a low glucose case
        // Then it is refused
        shouldThrow<IllegalStateException> {
            rsm.startRuleSessionToRemoveAssignment(createCase("A", "5.0"), "Diabetes status")
        }.message shouldBe "No value is assigned to \"Diabetes status\" for case A."
    }

    // --- the pending derived value change previewed by the Derived attributes panel ---

    /**
     * A case with the attributes a BMI formula references. Omitting [heightValue]
     * gives a case the formula cannot be evaluated against.
     */
    private fun bmiCase(name: String = "A", heightValue: String? = "1.8"): RDRCase {
        val weight = kb.attributeManager.getOrCreate("weight")
        val height = kb.attributeManager.getOrCreate("height")
        return with(RDRCaseBuilder()) {
            addValue(weight, defaultDate, "93.0")
            if (heightValue != null) addValue(height, defaultDate, heightValue)
            build(name)
        }
    }

    @Test
    fun `there is no pending derived value change before any session is started`() {
        rsm.currentDerivedValueChange.shouldBeNull()
    }

    @Test
    fun `assigning a formula previews the formula, not its value for the case`() {
        // Given a case with the attributes the formula references
        val case = bmiCase()

        // When an assign-value session is started
        rsm.startRuleSessionToAssignValue(case, "BMI", "weight / (height * height)")

        // Then the change previews the definition the rule will give the
        // attribute: no rule assigns a value to the case yet
        rsm.currentDerivedValueChange shouldBe DerivedValueAddition(
            attributeName = "BMI",
            formula = "weight / (height * height)"
        )
    }

    @Test
    fun `assigning a formula that cannot be evaluated previews the formula just the same`() {
        // Given a case missing an attribute the formula references
        kb.attributeManager.getOrCreate("height")
        val case = bmiCase(heightValue = null)

        // When an assign-value session is started
        rsm.startRuleSessionToAssignValue(case, "BMI", "weight / (height * height)")

        // Then the preview is the same, since it never depended on the case
        rsm.currentDerivedValueChange shouldBe DerivedValueAddition(
            attributeName = "BMI",
            formula = "weight / (height * height)"
        )
    }

    @Test
    fun `assigning a literal previews its quoted formula`() {
        // When an assign-value session is started with a literal
        rsm.startRuleSessionToAssignValue(createCase("A"), "Diabetes status", "\"diabetic\"")

        // Then the definition is the quoted literal
        rsm.currentDerivedValueChange shouldBe DerivedValueAddition(
            attributeName = "Diabetes status",
            formula = "\"diabetic\""
        )
    }

    @Test
    fun `removing an assignment previews a removal`() {
        // Given a KB with an assignment rule
        buildAssignmentRule()

        // When a remove-assignment session is started
        rsm.startRuleSessionToRemoveAssignment(createCase("C", "25.0"), "Diabetes status")

        // Then the change names the attribute whose value will be retracted
        rsm.currentDerivedValueChange shouldBe DerivedValueRemoval("Diabetes status")
    }

    @Test
    fun `replacing an assignment previews the new formula`() {
        // Given a KB with an assignment rule
        buildAssignmentRule()

        // When a replace-assignment session is started
        rsm.startRuleSessionToReplaceAssignment(createCase("C", "25.0"), "Diabetes status", "\"severely diabetic\"")

        // Then the change previews the definition the rule would give instead
        rsm.currentDerivedValueChange shouldBe DerivedValueReplacement(
            attributeName = "Diabetes status",
            newFormula = "\"severely diabetic\""
        )
    }

    @Test
    fun `replacing with a formula previews the new formula`() {
        // Given a rule assigning BMI by formula
        val case = bmiCase()
        rsm.startRuleSessionToAssignValue(case, "BMI", "weight / (height * height)")
        rsm.addConditionToCurrentRuleSession(
            greaterThanOrEqualTo(null, kb.attributeManager.getOrCreate("weight"), 1.0)
        )
        rsm.commitCurrentRuleSession()
        val caseB = bmiCase("B")
        kb.interpret(caseB)

        // When the assignment is replaced by another formula
        rsm.startRuleSessionToReplaceAssignment(caseB, "BMI", "weight / height")

        // Then the preview carries the new formula, unevaluated
        rsm.currentDerivedValueChange shouldBe DerivedValueReplacement(
            attributeName = "BMI",
            newFormula = "weight / height"
        )
    }

    @Test
    fun `the pending derived value change is cleared when the session is cancelled`() {
        // Given an assign-value session in progress
        rsm.startRuleSessionToAssignValue(createCase("A"), "Diabetes status", "\"diabetic\"")
        rsm.currentDerivedValueChange shouldNotBe null

        // When the session is cancelled
        rsm.cancelRuleSession()

        // Then the preview is gone
        rsm.currentDerivedValueChange.shouldBeNull()
    }

    @Test
    fun `the pending derived value change is cleared when the session is committed`() {
        // Given an assign-value session in progress
        rsm.startRuleSessionToAssignValue(createCase("A"), "Diabetes status", "\"diabetic\"")
        rsm.addConditionToCurrentRuleSession(highGlucose())

        // When the session is committed
        rsm.commitCurrentRuleSession()

        // Then the preview is gone
        rsm.currentDerivedValueChange.shouldBeNull()
    }

    @Test
    fun `the pending removal is cleared when its session is cancelled`() {
        // Given a remove-assignment session in progress
        buildAssignmentRule()
        rsm.startRuleSessionToRemoveAssignment(createCase("C", "25.0"), "Diabetes status")

        // When the session is cancelled
        rsm.cancelRuleSession()

        // Then the preview is gone
        rsm.currentDerivedValueChange.shouldBeNull()
    }

    @Test
    fun `the pending replacement is cleared when its session is cancelled`() {
        // Given a replace-assignment session in progress
        buildAssignmentRule()
        rsm.startRuleSessionToReplaceAssignment(createCase("C", "25.0"), "Diabetes status", "\"severe\"")

        // When the session is cancelled
        rsm.cancelRuleSession()

        // Then the preview is gone
        rsm.currentDerivedValueChange.shouldBeNull()
    }

    @Test
    fun `a refused assign value session leaves no stale preview behind`() {
        // Given a derived attribute
        kb.attributeManager.getOrCreate("BMI", AttributeKind.DERIVED)

        // When a session that would create a cycle is refused
        shouldThrow<IllegalStateException> {
            rsm.startRuleSessionToAssignValue(createCase("A"), "BMI", "BMI * 2")
        }

        // Then no preview is left for a session that never started
        rsm.currentDerivedValueChange.shouldBeNull()
    }

    @Test
    fun `a comment session sets a diff and no derived value change`() {
        // Given a processed case
        val case = kb.addProcessedCase(createCase("A"))

        // When a comment session is started
        rsm.startRuleSessionToAddComment(kb.viewableCase(case), "Go to Bondi.")

        // Then only the comment preview is set, so the Derived attributes panel
        // shows nothing pending
        rsm.currentDiff shouldBe Addition("Go to Bondi.")
        rsm.currentDerivedValueChange.shouldBeNull()
    }

    @Test
    fun `an assign value session sets a derived value change and no diff`() {
        // When an assign-value session is started
        rsm.startRuleSessionToAssignValue(createCase("A"), "Diabetes status", "\"diabetic\"")

        // Then only the derived value preview is set, so the Comments panel shows
        // nothing pending
        rsm.currentDiff.shouldBeNull()
        rsm.currentDerivedValueChange shouldNotBe null
    }

    @Test
    fun `a remove assignment session sets no diff`() {
        // Given a KB with an assignment rule
        buildAssignmentRule()

        // When a remove-assignment session is started
        rsm.startRuleSessionToRemoveAssignment(createCase("C", "25.0"), "Diabetes status")

        // Then the Comments panel is given nothing to preview
        rsm.currentDiff.shouldBeNull()
    }

    @Test
    fun `a replace assignment session sets no diff`() {
        // Given a KB with an assignment rule
        buildAssignmentRule()

        // When a replace-assignment session is started
        rsm.startRuleSessionToReplaceAssignment(createCase("C", "25.0"), "Diabetes status", "\"severe\"")

        // Then the Comments panel is given nothing to preview
        rsm.currentDiff.shouldBeNull()
    }

    // --- the change as carried to the client ---

    @Test
    fun `the cornerstone status carries the pending derived value change when there are no cornerstones`() {
        // Given an assign-value session on a KB with no cornerstones
        rsm.startRuleSessionToAssignValue(createCase("A"), "Diabetes status", "\"diabetic\"")

        // Then the status sent to the client carries the preview
        val status = rsm.cornerstoneStatus()
        status.cornerstoneToReview.shouldBeNull()
        status.derivedValueDiff shouldBe DerivedValueAddition(
            attributeName = "Diabetes status",
            formula = "\"diabetic\""
        )
    }

    @Test
    fun `the cornerstone status carries the pending derived value change alongside a cornerstone`() {
        // Given a committed rule, so there is a cornerstone to review
        buildAssignmentRule()
        kb.addProcessedCase(createCase("Cornerstone", "12.0"))
        rsm.startRuleSessionToAssignValue(createCase("A", "13.0"), "Risk level", "\"high\"")

        // Then the status carries the preview along with the cornerstone
        rsm.cornerstoneStatus().derivedValueDiff shouldBe DerivedValueAddition(
            attributeName = "Risk level",
            formula = "\"high\""
        )
    }

    @Test
    fun `the cornerstone status carries the rule conditions for the pending row tooltip`() {
        // Given an assign-value session with a condition added
        rsm.startRuleSessionToAssignValue(createCase("A"), "Diabetes status", "\"diabetic\"")
        rsm.addConditionToCurrentRuleSession(highGlucose())

        // Then the status carries both, which is what the pending row's tooltip needs
        val status = rsm.cornerstoneStatus()
        status.derivedValueDiff shouldNotBe null
        status.ruleConditions shouldBe listOf(highGlucose().asText())
    }
}
