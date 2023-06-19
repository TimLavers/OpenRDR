package io.rippledown.kb

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.rippledown.model.*
import io.rippledown.model.condition.GreaterThanOrEqualTo
import io.rippledown.model.condition.IsNormal
import io.rippledown.model.condition.HasCurrentValue
import io.rippledown.model.condition.LessThanOrEqualTo
import io.rippledown.model.external.ExternalCase
import io.rippledown.model.external.MeasurementEvent
import io.rippledown.model.rule.ChangeTreeToAddConclusion
import io.rippledown.persistence.InMemoryKB
import org.junit.Before
import kotlin.test.Test

class KBTest {
    private lateinit var kb : KB

    @Before
    fun setup() {
        val kbInfo = KBInfo("id123", "Blah")
        kb = createKB(kbInfo)
    }

    @Test
    fun processCase() {
        kb.allProcessedCases() shouldBe emptyList()
        kb.attributeManager.all() shouldBe emptySet()
        val externalCase1 = createExternalCase("Case1", "g1")
        val caseId1 = kb.processCase(externalCase1).caseId
        kb.allProcessedCases() shouldHaveSize 1
        val case = kb.allProcessedCases().first()
        case.name shouldBe externalCase1.name
        case.caseId shouldBe caseId1
    }

    @Test
    fun `processed cases are interpreted`() {
        val conclusionToAdd = "Whatever"
        buildRuleToAddAComment(kb, conclusionToAdd)

        val externalCase1 = createExternalCase("Case1", "g1")
        val processed = kb.processCase(externalCase1)
        processed.interpretation.textGivenByRules() shouldBe conclusionToAdd
        kb.getProcessedCase(processed.caseId.id!!)!!.interpretation.textGivenByRules() shouldBe conclusionToAdd
    }

    @Test
    fun `processed case names need not be unique`() {
        val caseId1 = kb.processCase(createExternalCase("CaseA", "g1")).caseId
        val caseId2 = kb.processCase(createExternalCase("CaseA", "g1")).caseId
        kb.allProcessedCases() shouldHaveSize 2
        kb.allProcessedCases().map { it.name } shouldBe listOf(caseId1.name, caseId2.name)
        caseId1.name shouldBe caseId2.name
        caseId1.id shouldNotBe caseId2.id
    }

    @Test
    fun getProcessedCase() {
        repeat(10) {
            val externalCase = createExternalCase("Case$it", "$it")
            val caseId = kb.processCase(externalCase).caseId
            val retrieved = kb.getProcessedCase(caseId.id!!)!!
            retrieved.name shouldBe externalCase.name
        }
        kb.getProcessedCase(99994) shouldBe null
    }

    @Test
    fun allProcessedCases() {
        kb.allProcessedCases() shouldBe emptyList()

        kb.processCase(createExternalCase("A", "g1")).caseId
        kb.processCase(createExternalCase("B", "g1")).caseId
        kb.processCase(createExternalCase("C", "g1")).caseId
        kb.allProcessedCases().map { it.name } shouldBe listOf("A", "B", "C")
    }

    @Test
    fun createCase() {
        kb.attributeManager.all() shouldBe emptySet()
        val date1 = defaultTestDate
        val date2 = date1 - 60_000
        val date3 = date2 - 60_000
        val eventABC1 = MeasurementEvent("ABC", date1)
        val eventABC2 = MeasurementEvent("ABC", date2)
        val eventXY1 = MeasurementEvent("XY", date1)
        val eventXY2 = MeasurementEvent("XY", date2)
        val eventXY3 = MeasurementEvent("XY", date3)
        val result1 = TestResult("1.0")
        val result2 = TestResult("2.0")
        val result3 = TestResult("3.0")
        val result4 = TestResult("4.0")
        val result5 = TestResult("5.0")
        val data = mapOf(eventABC1 to result1, eventABC2 to result2, eventXY1 to result3, eventXY2 to result4, eventXY3 to result5)
        val externalCase = ExternalCase("ExternalCase", data)

        val rdrCase = kb.createRDRCase(externalCase)

        // There should be two attributes now.
        kb.attributeManager.all().size shouldBe 2
        val abc = kb.attributeManager.getOrCreate("ABC")
        val xy = kb.attributeManager.getOrCreate("XY")
        kb.attributeManager.all().size shouldBe 2 // The two calls to getOrCreate got.

        rdrCase.name shouldBe externalCase.name
        rdrCase.attributes shouldBe setOf(abc, xy)
        val abcValues = rdrCase.values(abc)!!
        abcValues.size shouldBe 3
        abcValues[0] shouldBe TestResult("")
        abcValues[1].value shouldBe result2.value
        abcValues[2].value shouldBe result1.value
        val xyValues = rdrCase.values(xy)!!
        xyValues.size shouldBe 3
        xyValues[0].value shouldBe result5.value
        xyValues[1].value shouldBe result4.value
        xyValues[2].value shouldBe result3.value

        rdrCase.dates shouldBe listOf(date3, date2, date1)
    }

    @Test
    fun attributeManager() {
        kb.attributeManager.all() shouldBe emptySet()
    }

    @Test
    fun conclusionManager() {
        kb.conclusionManager.all() shouldBe  emptySet()

        val created = kb.conclusionManager.getOrCreate("Whatever")
        kb.conclusionManager.getById(created.id) shouldBeSameInstanceAs created
    }

    @Test
    fun conditionManager() {
        kb.conditionManager.all() shouldBe emptySet()
        val glucose = kb.attributeManager.getOrCreate("Glucose")
        val template = IsNormal(null, glucose)
        val created = kb.conditionManager.getOrCreate(template)
        kb.conditionManager.getById(created.id!!) shouldBeSameInstanceAs created
    }

    @Test
    fun viewableInterpretedCase() {
        val comment = "Coffee time!"
        buildRuleToAddAComment(kb, comment)

        val builder = RDRCaseBuilder()
        builder.addValue(Attribute(300, "ABC"), defaultDate, "10")
        builder.addValue(Attribute(400, "DEF"), defaultDate, "20")
        val case = builder.build("Case2")

        case.interpretation.textGivenByRules() shouldBe ""
        // Check that it has been interpreted.
        val viewableCase = kb.viewableInterpretedCase(case)
        viewableCase.interpretation.textGivenByRules() shouldBe comment

        // Check that ordering is working by getting the current ordering
        // and changing it, and then getting the case again and checking
        // that the new ordering is applied.
        val attributesInOriginalOrder = viewableCase.attributes()
        kb.caseViewManager.moveJustBelow(attributesInOriginalOrder[0], attributesInOriginalOrder[1])
        val caseAfterMove = kb.viewableInterpretedCase(case)
        caseAfterMove.attributes() shouldBe listOf(attributesInOriginalOrder[1], attributesInOriginalOrder[0])
    }

    @Test
    fun interpretCase() {
        val comment = "Whatever."
        buildRuleToAddAComment(kb, comment)
        val case = createCase("Case1", "1.0")
        case.interpretation.textGivenByRules() shouldBe ""
        kb.interpret(case)
        case.interpretation.textGivenByRules() shouldBe comment
    }

    private fun buildRuleToAddAComment(kb: KB, comment: String) {
        kb.addCornerstoneCase(createCase("Case1", "1.0"))
        val sessionCase = kb.getCaseByName("Case1")
        val conclusion = kb.conclusionManager.getOrCreate(comment)
        kb.startRuleSession(sessionCase, ChangeTreeToAddConclusion(conclusion))
        kb.commitCurrentRuleSession()
    }

    @Test
    fun equalsTest() {
        val kb1 = createKB(KBInfo("1","Thyroids"))
        val kb2 = createKB(KBInfo("2","Glucose"))
        val kb3 = createKB(KBInfo("4","Glucose"))
        val kb4 = createKB(KBInfo("4","Thyroids"))
        (kb1 == kb2) shouldBe false
        (kb1 == kb3) shouldBe false
        (kb3 == kb4) shouldBe true
    }

    @Test
    fun hashCodeTest() {
        val kb1 = createKB(KBInfo("id123","Thyroids"))
        val kb2 = createKB(KBInfo("id123","Thyroids"))
        (kb1.hashCode() == kb2.hashCode()) shouldBe true
    }

    @Test
    fun getCaseByNameWhenNoCases() {
        shouldThrow<NoSuchElementException> {
            kb.getCaseByName("Whatever")
        }
    }

    @Test
    fun getCaseByNameUnknownCase() {
        kb.addCornerstoneCase(createCase("Case1"))
        shouldThrow<NoSuchElementException> {
            kb.getCaseByName("Whatever")
        }
    }

    @Test
    fun getCase() {
        kb.addCornerstoneCase(createCase("Case1", "1.2"))
        kb.addCornerstoneCase(createCase("Case2"))
        val retrieved = kb.getCaseByName("Case1")
        retrieved.name shouldBe "Case1"
        retrieved.getLatest(glucose())!!.value.text shouldBe "1.2"
    }

    @Test
    fun allCornerstoneCases() {
        kb.allCornerstoneCases() shouldBe emptyList()
        for (i in 1..10) {
            kb.addCornerstoneCase(createCase("Case$i"))
        }
        kb.allCornerstoneCases() shouldHaveSize 10
        for (i in 1..10) {
            val retrieved = kb.getCaseByName("Case$i")
            kb.allCornerstoneCases() shouldContain retrieved
        }
    }

    @Test
    fun addCase() {
        for (i in 1..10) {
            kb.addCornerstoneCase(createCase("Case$i"))
        }
        for (i in 1..10) {
            val retrieved = kb.getCaseByName("Case$i")
            retrieved.name shouldBe "Case$i"
        }
    }

    @Test
    fun containsCaseWithName() {
        for (i in 1..10) {
            val caseName = "Case$i"
            kb.containsCaseWithName(caseName) shouldBe false
            kb.addCornerstoneCase(createCase(caseName))
            kb.containsCaseWithName(caseName) shouldBe true
        }
    }

    @Test
    fun cannotAddCaseWithSameNameAsExistingCase() {
        kb.addCornerstoneCase(createCase("Blah"))
        kb.addCornerstoneCase(createCase("Whatever"))
        shouldThrow<IllegalArgumentException> {
            kb.addCornerstoneCase(createCase("Blah"))
        }.message shouldBe "There is already a case with name Blah in the KB."
    }

    @Test
    fun `rule session must be started for rule session operations`() {
        val noSessionMessage = "Rule session not started."
        shouldThrow<IllegalStateException> {
            kb.addConditionToCurrentRuleSession(createCondition())
        }.message shouldBe noSessionMessage

        shouldThrow<IllegalStateException> {
            kb.conflictingCasesInCurrentRuleSession()
        }.message shouldBe noSessionMessage

        shouldThrow<IllegalStateException> {
            kb.commitCurrentRuleSession()
        }.message shouldBe noSessionMessage
    }

    @Test
    fun `cannot start a rule session if one is already started`() {
        kb.addCornerstoneCase(createCase("Case1"))
        val sessionCase = kb.getCaseByName("Case1")
        val conclusion = kb.conclusionManager.getOrCreate("Whatever.")
        kb.startRuleSession(sessionCase, ChangeTreeToAddConclusion(conclusion))
        shouldThrow<IllegalStateException> {
            kb.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Stuff.")))
        }.message shouldBe "Session already in progress."
    }

    @Test
    fun `cannot start a rule session if action is not applicable to session case`() {
        kb.addCornerstoneCase(createCase("Case1", "1.0"))
        kb.addCornerstoneCase(createCase("Case2", "1.0"))
        val sessionCase = kb.getCaseByName("Case1")
        val otherCase = kb.getCaseByName("Case1")
        kb.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Whatever.")))
        kb.commitCurrentRuleSession()
        kb.interpret(otherCase)
        otherCase.interpretation.textGivenByRules() shouldBe "Whatever." // sanity

        shouldThrow<IllegalStateException> {
            kb.startRuleSession(otherCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Whatever.")))
        }.message shouldBe "Action ChangeTreeToAddConclusion(toBeAdded=Conclusion(id=1, text=Whatever.)) is not applicable to case Case1"
    }

    @Test
    fun startRuleSession() {
        kb.addCornerstoneCase(createCase("Case1"))
        val sessionCase = kb.getCaseByName("Case1")
        kb.interpret(sessionCase)
        sessionCase.interpretation.textGivenByRules() shouldBe ""
        kb.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Whatever.")))
        kb.commitCurrentRuleSession()
        kb.interpret(sessionCase)
        sessionCase.interpretation.textGivenByRules() shouldBe "Whatever."
    }

    @Test
    fun conflictingCases() {
        kb.addCornerstoneCase(createCase("Case1", "1.0"))
        kb.addCornerstoneCase(createCase("Case2", "2.0"))
        val sessionCase = kb.getCaseByName("Case1")
        sessionCase.interpretation.textGivenByRules() shouldBe ""
        kb.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Whatever.")))
        kb.conflictingCasesInCurrentRuleSession().map { rdrCase -> rdrCase.name }.toSet() shouldBe setOf("Case2")
    }

    @Test
    fun addCondition() {
        kb.addCornerstoneCase(createCase("Case1", "1.0"))
        kb.addCornerstoneCase(createCase("Case2", "2.0"))
        val sessionCase = kb.getCaseByName("Case1")
        sessionCase.interpretation.textGivenByRules() shouldBe ""
        kb.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate( "Whatever.")))
        kb.addConditionToCurrentRuleSession(LessThanOrEqualTo(null, glucose(), 1.2))
        kb.conflictingCasesInCurrentRuleSession().size shouldBe 0
    }

    @Test
    fun commitSession() {
        kb.addCornerstoneCase(createCase("Case1", "1.0"))
        kb.addCornerstoneCase(createCase("Case2", "2.0"))
        val sessionCase = kb.getCaseByName("Case1")
        val otherCase = kb.getCaseByName("Case2")
        sessionCase.interpretation.textGivenByRules() shouldBe ""
        kb.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate( "Whatever.")))
        kb.interpret(otherCase)
        // Rule not yet added...
        otherCase.interpretation.textGivenByRules() shouldBe ""
        kb.commitCurrentRuleSession()
        // Rule now added...
        kb.interpret(otherCase)
        otherCase.interpretation.textGivenByRules() shouldBe "Whatever."
    }

    @Test
    fun `should return condition hints for case`() {
        val caseWithGlucoseAttribute = createCase("A", "1.0")
        val expectedCondition = kb.conditionManager.getOrCreate(HasCurrentValue(null, glucose()))
        val conditionList = kb.conditionHintsForCase(caseWithGlucoseAttribute)
        conditionList.conditions.size shouldBe 1
        conditionList.conditions[0] should beSameAs(expectedCondition)
    }

    @Test // Conc-4
    fun `conclusions are aligned when building rules`() {
        val conclusionToAdd = kb.conclusionManager.getOrCreate("Whatever")
        val copyOfConclusion = conclusionToAdd.copy()
        kb.addCornerstoneCase(createCase("Case1", "1.0"))
        val sessionCase = kb.getCaseByName("Case1")
        kb.startRuleSession(sessionCase, ChangeTreeToAddConclusion(copyOfConclusion))
        kb.commitCurrentRuleSession()
        kb.interpret(sessionCase)
        sessionCase.interpretation.conclusions().single() shouldBeSameInstanceAs  conclusionToAdd
    }

    private fun glucose() = kb.attributeManager.getOrCreate("Glucose")

    private fun createCondition(): GreaterThanOrEqualTo {
        return GreaterThanOrEqualTo(null, Attribute(4567, "ABC"), 5.0)
    }

    private fun createCase(caseName: String, glucoseValue: String = "0.667"): RDRCase {
        val builder1 = RDRCaseBuilder()
        builder1.addValue(glucose(), defaultDate, glucoseValue)
        return builder1.build(caseName)
    }

    private fun createExternalCase(caseName: String,glucoseValue: String = "0.667"): ExternalCase {
        val regularCase = createCase(caseName, glucoseValue)
        val data = regularCase.data.mapKeys{ MeasurementEvent(it.key.attribute.name, it.key.date) }
        return ExternalCase(caseName, data)
    }

    private fun createKB(kbInfo: KBInfo) = KB(InMemoryKB(kbInfo))
}