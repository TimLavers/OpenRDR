package io.rippledown.kb

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.rippledown.model.*
import io.rippledown.model.condition.*
import io.rippledown.model.external.ExternalCase
import io.rippledown.model.external.MeasurementEvent
import io.rippledown.model.rule.ChangeTreeToAddConclusion
import io.rippledown.model.rule.ConditionSuggester
import io.rippledown.model.rule.CornerstoneStatus
import io.rippledown.model.rule.UpdateCornerstoneRequest
import io.rippledown.persistence.inmemory.InMemoryKB
import io.rippledown.util.shouldBeSameAs
import kotlin.test.BeforeTest
import kotlin.test.Test

class KBTest {
    private lateinit var persistentKB: InMemoryKB
    private lateinit var kb: KB

    @BeforeTest
    fun setup() {
        val kbInfo = KBInfo("id123", "Blah")
        kb = createKB(kbInfo)
    }

    @Test
    fun descriptionTest() {
        kb.description() shouldBe ""
        val newDescription = "A truly fine KB!"
        kb.setDescription(newDescription)
        kb.description() shouldBe newDescription
        kb = KB(persistentKB)
        kb.description() shouldBe newDescription
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
        processed.interpretation.conclusionTexts() shouldBe setOf(conclusionToAdd)
        kb.getProcessedCase(processed.caseId.id!!)!!.interpretation.conclusionTexts() shouldBe setOf(conclusionToAdd)
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
    fun deleteProcessedCaseWithName() {
        val externalCase1 = createExternalCase("Case1", "g1")
        kb.processCase(externalCase1)
        kb.deletedProcessedCaseWithName(externalCase1.name)
        kb.allProcessedCases() shouldBe emptyList()
    }

    @Test
    fun `only first processed case with name gets deleted`() {
        val externalCase1 = createExternalCase("Case1", "g1")
        val case1 = kb.processCase(externalCase1)
        val externalCase2 = createExternalCase("Case2", "g2")
        kb.processCase(externalCase2)
        val externalCase3 = createExternalCase("Case1", "g3")
        val case3 = kb.processCase(externalCase3)
        val externalCase4 = createExternalCase("Case1", "g4")
        kb.processCase(externalCase4)
        kb.deletedProcessedCaseWithName(externalCase1.name)

        kb.allProcessedCases().size shouldBe 3
        kb.getProcessedCase(case1.caseId.id!!) shouldBe null
        kb.getProcessedCase(case3.caseId.id!!)!!.name shouldBe case3.name
    }

    @Test
    fun `deleted processed case with unknown name does nothing`() {
        val externalCase1 = createExternalCase("Case1", "g1")
        kb.processCase(externalCase1)
        kb.deletedProcessedCaseWithName("Unknown")
        kb.allProcessedCases().size shouldBe 1
    }

    @Test
    fun allProcessedCases() {
        kb.allProcessedCases() shouldBe emptyList()

        kb.processCase(createExternalCase("A", "g1"))
        kb.processCase(createExternalCase("B", "g1"))
        kb.processCase(createExternalCase("C", "g1"))
        kb.allProcessedCases().map { it.name } shouldBe listOf("A", "B", "C")
    }

    @Test
    fun processedCaseIds() {
        kb.processedCaseIds() shouldBe emptyList()

        val idA = kb.processCase(createExternalCase("A", "g1")).caseId
        val idB = kb.processCase(createExternalCase("B", "g1")).caseId
        val idC = kb.processCase(createExternalCase("C", "g1")).caseId
        kb.processedCaseIds() shouldBe listOf(idA, idB, idC)
    }

    @Test
    fun createCase() {
        kb.attributeManager.all() shouldBe emptySet()
        val date1 = defaultDate
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
        val data = mapOf(
            eventABC1 to result1,
            eventABC2 to result2,
            eventXY1 to result3,
            eventXY2 to result4,
            eventXY3 to result5
        )
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
        kb.conclusionManager.all() shouldBe emptySet()

        val created = kb.conclusionManager.getOrCreate("Whatever")
        kb.conclusionManager.getById(created.id) shouldBeSameInstanceAs created
    }

    @Test
    fun conditionManager() {
        kb.conditionManager.all() shouldBe emptySet()
        val glucose = kb.attributeManager.getOrCreate("Glucose")
        val template = isNormal(null, glucose)
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
        val case = builder.build("Case2", 42)

        // Check that it has been interpreted.
        val viewableCase = kb.viewableCase(case)
        viewableCase.textGivenByRules() shouldBe comment

        // Check that ordering is working by getting the current ordering
        // and changing it, and then getting the case again and checking
        // that the new ordering is applied.
        val attributesInOriginalOrder = viewableCase.attributes()
        kb.caseViewManager.moveJustBelow(attributesInOriginalOrder[0], attributesInOriginalOrder[1])
        val caseAfterMove = kb.viewableCase(case)
        caseAfterMove.attributes() shouldBe listOf(attributesInOriginalOrder[1], attributesInOriginalOrder[0])
    }

    @Test
    fun `should show comments given by rules in the order specified by the InterpretationViewManager`() {
        //Given
        val coffeeComment = "Coffee time!"
        val teaComment = "Tea time!"
        val chocComment = "Chocolate time!"
        buildRuleToAddAComment(kb, coffeeComment)
        buildRuleToAddAComment(kb, teaComment)
        buildRuleToAddAComment(kb, chocComment)

        val case = RDRCase(CaseId(42, "Case"))

        //When the case is interpreted
        val viewableCase = kb.viewableCase(case)

        //Then the comments should be in the order in which they were added to the interpretation
        viewableCase.textGivenByRules() shouldBe "$coffeeComment $teaComment $chocComment"
    }

    @Test
    fun interpretCase() {
        val comment = "Whatever."
        buildRuleToAddAComment(kb, comment)
        val case = createCase("Case1", value = "1.0")
        case.interpretation.conclusions() shouldBe emptySet()
        kb.interpret(case)
        case.interpretation.conclusions().map { it.text } shouldBe listOf(comment)
    }

    private fun buildRuleToAddAComment(kb: KB, comment: String) {
        kb.addCornerstoneCase(createCase("Case1", value = "1.0"))
        val sessionCase = kb.getCornerstoneCaseByName("Case1")
        val conclusion = kb.conclusionManager.getOrCreate(comment)
        kb.startRuleSession(sessionCase, ChangeTreeToAddConclusion(conclusion))
        kb.commitCurrentRuleSession()
    }

    @Test
    fun equalsTest() {
        val kb1 = createKB(KBInfo("1", "Thyroids"))
        val kb2 = createKB(KBInfo("2", "Glucose"))
        val kb3 = createKB(KBInfo("4", "Glucose"))
        val kb4 = createKB(KBInfo("4", "Thyroids"))
        (kb1 == kb2) shouldBe false
        (kb1 == kb3) shouldBe false
        (kb3 == kb4) shouldBe true
    }

    @Test
    fun hashCodeTest() {
        val kb1 = createKB(KBInfo("id123", "Thyroids"))
        val kb2 = createKB(KBInfo("id123", "Thyroids"))
        (kb1.hashCode() == kb2.hashCode()) shouldBe true
    }

    @Test
    fun getCaseByNameWhenNoCases() {
        shouldThrow<NoSuchElementException> {
            kb.getCornerstoneCaseByName("Whatever")
        }
    }

    @Test
    fun getCaseByNameUnknownCase() {
        kb.addProcessedCase(createCase("Case1"))
        shouldThrow<NoSuchElementException> {
            kb.getProcessedCaseByName("Whatever")
        }
    }

    @Test
    fun getCase() {
        kb.addCornerstoneCase(createCase("Case1", value = "1.2"))
        kb.addCornerstoneCase(createCase("Case2"))
        val retrieved = kb.getCornerstoneCaseByName("Case1")
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
            val retrieved = kb.getCornerstoneCaseByName("Case$i")
            kb.allCornerstoneCases() shouldContain retrieved
        }
    }

    @Test
    fun `add cornerstone case resets id`() {
        val case1 = createCase("Case1", value = "1.2", id = 123)
        val added = kb.addCornerstoneCase(case1)
        added.id shouldNotBe case1.id
        added.name shouldBe case1.name
        added.data shouldBe case1.data

        with(kb.getCase(added.id!!)!!) {
            id shouldNotBe case1.id
            name shouldBe case1.name
            data shouldBe case1.data
        }
    }

    @Test
    fun `add cornerstone case resets type`() {
        val case1 = createCase("Case1", value = "1.2").copy(caseId = CaseId(123, "Case1_CC", CaseType.Processed))
        val added = kb.addCornerstoneCase(case1)
        added.caseId.type shouldBe CaseType.Cornerstone
        kb.getCase(added.id!!)!!.caseId.type shouldBe CaseType.Cornerstone
    }

    @Test
    fun getCornerstoneCase() {
        kb.getCase(9099999) shouldBe null

        val id1 = kb.addCornerstoneCase(createCase("Case1", value = "1.2")).caseId.id!!
        kb.addCornerstoneCase(createCase("Case2"))

        kb.getCase(id1)!!.name shouldBe "Case1"
    }

    @Test
    fun addCornerstoneCases() {
        for (i in 1..10) {
            kb.addCornerstoneCase(createCase("Case$i"))
        }
        for (i in 1..10) {
            val retrieved = kb.getCornerstoneCaseByName("Case$i")
            retrieved.name shouldBe "Case$i"
        }
    }

    @Test
    fun loadCases() {
        kb.allCornerstoneCases() shouldBe emptyList()

        for (i in 1..10) {
            kb.addCornerstoneCase(createCase("Case$i"))
        }
        val allCCs = kb.allCornerstoneCases()

        val kbInfo = KBInfo("refreshed", "Blah")
        kb = createKB(kbInfo)
        kb.allCornerstoneCases() shouldBe emptyList()

        kb.loadCases(allCCs)
        kb.allCornerstoneCases() shouldBe allCCs
    }

    @Test
    fun containsCaseWithName() {
        for (i in 1..10) {
            val caseName = "Case$i"
            kb.containsCornerstoneCaseWithName(caseName) shouldBe false
            kb.addCornerstoneCase(createCase(caseName))
            kb.containsCornerstoneCaseWithName(caseName) shouldBe true
        }
    }

    @Test
    fun `should be able to add a cornerstone case a second time`() {
        kb.allCornerstoneCases() shouldBe emptyList()
        val case = createCase("Blah")
        kb.addCornerstoneCase(case)
        kb.allCornerstoneCases().size shouldBe 1
        kb.containsCornerstoneCaseWithName("Blah") shouldBe true
        kb.addCornerstoneCase(case)
        kb.allCornerstoneCases().size shouldBe 2
    }

    @Test
    fun canAddCornerstoneCaseWithSameName() {
        kb.addCornerstoneCase(createCase("Blah"))
        kb.addCornerstoneCase(createCase("Whatever"))
        kb.addCornerstoneCase(createCase("Blah"))
        kb.allCornerstoneCases().size shouldBe 3
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
    fun `rule session must be started before it can be cancelled`() {
        val noSessionMessage = "No rule session in progress."
        shouldThrow<IllegalStateException> {
            kb.cancelRuleSession()
        }.message shouldBe noSessionMessage
    }

    @Test
    fun `cannot start a rule session if one is already started`() {
        val sessionCase = createCase("Case1")
        val conclusion = kb.conclusionManager.getOrCreate("Whatever.")
        kb.startRuleSession(sessionCase, ChangeTreeToAddConclusion(conclusion))
        shouldThrow<IllegalStateException> {
            kb.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Stuff.")))
        }.message shouldBe "Session already in progress."
    }

    @Test
    fun `cannot start a rule session if action is not applicable to session case`() {
        val sessionCase = createCase("Case1", value = "1.0")
        val otherCase = createCase("Case2", value = "1.0")
        kb.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Whatever.")))
        kb.commitCurrentRuleSession()
        kb.interpret(otherCase)
        otherCase.interpretation.conclusionTexts() shouldBe setOf("Whatever.") // sanity

        shouldThrow<IllegalStateException> {
            kb.startRuleSession(otherCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Whatever.")))
        }.message shouldBe "Action ChangeTreeToAddConclusion(toBeAdded=Conclusion(id=1, text=Whatever.)) is not applicable to case Case2"
    }

    @Test
    fun startRuleSession() {
        val sessionCase = createCase("Case1")
        kb.interpret(sessionCase)
        sessionCase.interpretation.conclusionTexts() shouldBe emptySet()
        kb.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Whatever.")))
        kb.commitCurrentRuleSession()
        kb.interpret(sessionCase)
        sessionCase.interpretation.conclusionTexts() shouldBe setOf("Whatever.")
    }

    @Test
    fun conflictingCases() {
        val sessionCase = createCase("Case1", value = "1.0")
        kb.addCornerstoneCase(createCase("Case2", value = "2.0"))
        sessionCase.interpretation.conclusionTexts() shouldBe emptySet()
        kb.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Whatever.")))
        kb.conflictingCasesInCurrentRuleSession().map { rdrCase -> rdrCase.name }.toSet() shouldBe setOf("Case2")
    }

    @Test
    fun addCondition() {
        val sessionCase = createCase("Case1", value = "1.0")
        kb.addCornerstoneCase(createCase("Case2", value = "2.0"))
        sessionCase.interpretation.conclusionTexts() shouldBe emptySet()
        kb.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Whatever.")))
        kb.addConditionToCurrentRuleSession(lessThanOrEqualTo(null, glucose(), 1.2))
        kb.conflictingCasesInCurrentRuleSession().size shouldBe 0
    }

    @Test
    fun commitSession() {
        val sessionCase = createCase("Case1", value = "1.0")
        val otherCase = createCase("Case2", value = "2.0")
        sessionCase.interpretation.conclusionTexts() shouldBe emptySet()
        kb.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Whatever.")))
        kb.interpret(otherCase)
        // Rule not yet added...
        otherCase.interpretation.conclusionTexts() shouldBe emptySet()
        kb.commitCurrentRuleSession()
        // Rule now added...
        kb.interpret(otherCase)
        otherCase.interpretation.conclusionTexts() shouldBe setOf("Whatever.")
    }

    @Test
    fun `should return condition hints for case`() {
        val caseWithGlucoseAttribute = createCase("A", value = "1.0")
        val conditionList = kb.conditionHintsForCase(caseWithGlucoseAttribute)
        conditionList.suggestions.toSet() shouldBe ConditionSuggester(
            kb.attributeManager.all(),
            caseWithGlucoseAttribute
        ).suggestions().toSet()
    }

    @Test // Conc-4
    fun `conclusions are aligned when building rules`() {
        val conclusionToAdd = kb.conclusionManager.getOrCreate("Whatever")
        val copyOfConclusion = conclusionToAdd.copy()
        kb.addCornerstoneCase(createCase("Case1", value = "1.0"))
        val sessionCase = kb.getCornerstoneCaseByName("Case1")
        kb.startRuleSession(sessionCase, ChangeTreeToAddConclusion(copyOfConclusion))
        kb.commitCurrentRuleSession()
        kb.interpret(sessionCase)
        sessionCase.interpretation.conclusions().single() shouldBeSameInstanceAs conclusionToAdd
    }

    @Test
    fun `the session case should be stored as the cornerstone case when the rule is committed`() {
        val sessionCase = createCase("Case1", value = "1.0")
        sessionCase.interpretation.conclusionTexts() shouldBe emptySet()
        kb.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Whatever.")))
        kb.allCornerstoneCases() shouldHaveSize 0
        kb.commitCurrentRuleSession()
        kb.allCornerstoneCases() shouldHaveSize 1
        kb.containsCornerstoneCaseWithName("Case1") shouldBe true
    }

    @Test
    fun `should update the cornerstone status when the conditions change`() {
        val cc1 = kb.addCornerstoneCase(createCase("Case1", value = "1.0"))
        val vcc1 = kb.viewableCase(cc1)
        val sessionCase = createCase("Case3", value = "3.0")

        kb.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go to Bondi.")))
        val currentCCStatus = CornerstoneStatus(vcc1, 0, 1)
        withClue("sanity check") {
            kb.cornerstoneStatus(vcc1) shouldBe currentCCStatus
        }
        val condition = lessThanOrEqualTo(null, glucose(), 0.5) //false for the cornerstone
        val updateRequest = UpdateCornerstoneRequest(currentCCStatus, RuleConditionList(listOf(condition)))
        kb.updateCornerstone(updateRequest) shouldBe CornerstoneStatus()
    }

    @Test
    fun `should not update the cornerstone status if no cornerstones are removed by the condition change`() {
        val cc1 = kb.addCornerstoneCase(createCase("Case1", value = "1.0"))
        val vcc1 = kb.viewableCase(cc1)
        val sessionCase = createCase("Case3", value = "3.0")

        kb.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go to Bondi.")))
        val currentCCStatus = CornerstoneStatus(vcc1, 0, 1)
        withClue("sanity check") {
            kb.cornerstoneStatus(vcc1) shouldBe currentCCStatus
        }
        val condition = lessThanOrEqualTo(null, glucose(), 1.0) //true for the cornerstone
        val updateRequest = UpdateCornerstoneRequest(currentCCStatus, RuleConditionList(listOf(condition)))
        kb.updateCornerstone(updateRequest) shouldBe currentCCStatus
    }

    @Test
    fun `should reset the first cornerstone case if the current cornerstone has been removed by the condition change`() {
        val cc1 = kb.addCornerstoneCase(createCase("Case1", value = "1.0"))
        val cc2 = kb.addCornerstoneCase(createCase("Case2", value = "2.0"))
        kb.addCornerstoneCase(createCase("Case3", value = "3.0"))
        val vcc1 = kb.viewableCase(cc1)
        val vcc2 = kb.viewableCase(cc2)
        val sessionCase = createCase("Case4", value = "4.0")

        kb.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go to Bondi.")))
        val currentCCStatus = CornerstoneStatus(vcc1, 0, 3)
        withClue("sanity check") {
            kb.cornerstoneStatus(vcc1) shouldBe currentCCStatus
        }
        val condition = greaterThanOrEqualTo(null, glucose(), 1.5) //false for the current cornerstone
        val updateRequest = UpdateCornerstoneRequest(currentCCStatus, RuleConditionList(listOf(condition)))
        val expected = CornerstoneStatus(vcc2, 0, 2)
        kb.updateCornerstone(updateRequest) shouldBe expected
    }

    @Test
    fun `should remain on the current cornerstone case if it has not been removed by the condition change`() {
        val cc1 = kb.addCornerstoneCase(createCase("Case1", value = "1.0"))
        kb.addCornerstoneCase(createCase("Case2", value = "2.0"))
        kb.addCornerstoneCase(createCase("Case3", value = "3.0"))
        val vcc1 = kb.viewableCase(cc1)
        val sessionCase = createCase("Case4", value = "4.0")

        kb.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go to Bondi.")))
        val currentCCStatus = CornerstoneStatus(vcc1, 0, 3)
        withClue("sanity check") {
            kb.cornerstoneStatus(vcc1) shouldBe currentCCStatus
        }
        val condition = lessThanOrEqualTo(null, glucose(), 2.5) //true for the current cornerstone and cc2
        val updateRequest = UpdateCornerstoneRequest(currentCCStatus, RuleConditionList(listOf(condition)))
        val expected = CornerstoneStatus(vcc1, 0, 2)
        kb.updateCornerstone(updateRequest) shouldBe expected
    }

    @Test
    fun `should restore the index of the current cornerstone case if it has not been removed by the condition change`() {
        //Given
        kb.addCornerstoneCase(createCase("Case1", value = "1.0"))
        val cc2 = kb.addCornerstoneCase(createCase("Case2", value = "2.0"))
        kb.addCornerstoneCase(createCase("Case3", value = "3.0"))
        val vcc2 = kb.viewableCase(cc2)
        val sessionCase = createCase("Case4", value = "4.0")

        //When add a condition that is true for cc1 and the current cornerstone cc2
        kb.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go to Bondi.")))

        //Assume that the user has skipped to the 2nd cornerstone
        val originalCCStatus = CornerstoneStatus(vcc2, 1, 3)
        withClue("sanity check") {
            kb.cornerstoneStatus(vcc2) shouldBe originalCCStatus
        }
        val condition = lessThanOrEqualTo(null, glucose(), 2.5) //true for cc1 and the current cornerstone cc2
        var updateRequest = UpdateCornerstoneRequest(originalCCStatus, RuleConditionList(listOf(condition)))
        val expected = CornerstoneStatus(vcc2, 1, 2)
        kb.updateCornerstone(updateRequest) shouldBe expected
        kb.cornerstoneStatus(vcc2) shouldBe expected

        //Remove the condition that was added
        updateRequest = UpdateCornerstoneRequest(expected, RuleConditionList(emptyList()))

        //Then
        kb.updateCornerstone(updateRequest) shouldBe originalCCStatus
        kb.cornerstoneStatus(vcc2) shouldBe originalCCStatus
    }

    @Test
    fun `should remain on the current cornerstone case if it has not been removed by the condition change and it is not the first one`() {
        val cc1 = kb.addCornerstoneCase(createCase("Case1", value = "1.0"))
        val cc2 = kb.addCornerstoneCase(createCase("Case2", value = "2.0"))
        kb.addCornerstoneCase(createCase("Case3", value = "3.0"))
        kb.viewableCase(cc1)
        val vcc2 = kb.viewableCase(cc2)
        val sessionCase = createCase("Case4", value = "4.0")

        kb.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go to Bondi.")))
        val currentCCStatus = CornerstoneStatus(vcc2, 1, 3) //the user has skipped to the 2nd cornerstone
        withClue("sanity check") {
            kb.cornerstoneStatus(vcc2) shouldBe currentCCStatus
        }

        val condition =
            lessThanOrEqualTo(null, glucose(), 2.5) //true for cc1 and the current cornerstone. false for cc3
        val updateRequest = UpdateCornerstoneRequest(currentCCStatus, RuleConditionList(listOf(condition)))
        val expected = CornerstoneStatus(vcc2, 1, 2)
        kb.updateCornerstone(updateRequest) shouldBe expected
    }

    @Test
    fun `should return no cornerstones when the rule session has just started if there aren't any cornerstones`() {
        val sessionCase = createCase("Case4", value = "4.0")
        kb.startRuleSession(
            sessionCase,
            ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go to Bondi."))
        )
        val ccStatus = kb.cornerstoneStatus(null)
        ccStatus shouldBe CornerstoneStatus()
    }

    @Test
    fun `should return all cornerstones when the rule session has just started and no cornerstone has been selected`() {
        val cc1 = kb.addCornerstoneCase(createCase("Case1", value = "1.0"))
        kb.addCornerstoneCase(createCase("Case2", value = "2.0"))
        kb.addCornerstoneCase(createCase("Case3", value = "3.0"))
        val vcc1 = kb.viewableCase(cc1)

        val sessionCase = createCase("Case4", value = "4.0")
        kb.startRuleSession(
            sessionCase,
            ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go to Bondi."))
        )
        val ccStatus = kb.cornerstoneStatus(null)
        ccStatus shouldBe CornerstoneStatus(vcc1, 0, 3)
    }

    @Test
    fun `should not change the index of the current cornerstone if it has not been removed`() {
        val cc1 = kb.addCornerstoneCase(createCase("Case1", value = "1.0"))
        val cc2 = kb.addCornerstoneCase(createCase("Case2", value = "2.0"))
        kb.addCornerstoneCase(createCase("Case3", value = "3.0"))
        kb.viewableCase(cc1)
        val vcc2 = kb.viewableCase(cc2)

        val sessionCase = createCase("Case4", value = "4.0")
        kb.startRuleSession(
            sessionCase,
            ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go to Bondi."))
        )
        val ccStatus = kb.cornerstoneStatus(vcc2)
        ccStatus shouldBe CornerstoneStatus(vcc2, 1, 3)
    }

    @Test
    fun `should not set the user expression for a condition parsed from that expression if the condition already exists`() {
        //Given
        val waves = kb.attributeManager.getOrCreate("Waves")
        val attributeNames = listOf(waves.name)
        val height = "2" //greater than 1
        val sessionCase = createCase("Case", attribute = waves, value = height)
        val conditionParser = mockk<ConditionParser>()
        val userExpression = "waves seem to be at least one metre"
        val parsedCondition = ConditionConstructors().GreaterThanOrEqualTo(waves, userExpression, "1.0")
        every { conditionParser.parse(any(), any(), any()) } returns parsedCondition
        kb.setConditionParser(conditionParser)

        kb.startRuleSession(
            sessionCase,
            ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go to Bondi."))
        )
        kb.holdsForSessionCase(parsedCondition) shouldBe true
        parsedCondition.userExpression() shouldBe userExpression

        //When
        val existingCondition = kb.conditionManager.getOrCreate(greaterThanOrEqualTo(null, waves, 1.0))
        val returnedCondition = kb.conditionForExpression(userExpression, attributeNames)!!

        //Then
        verify { conditionParser.parse(userExpression, attributeNames, any()) }
        returnedCondition shouldBe existingCondition
        withClue("The user expression should not be set for an existing condition") {
            returnedCondition.userExpression() shouldBe ""
        }
    }

    @Test
    fun `should set the user expression for a condition parsed from that expression if the condition does not already exist`() {
        //Given
        val waves = kb.attributeManager.getOrCreate("Waves")
        val attributeNames = listOf(waves.name)
        val height = "2" //greater than 1
        val sessionCase = createCase("Case", attribute = waves, value = height)
        val conditionParser = mockk<ConditionParser>()
        val userExpression = "waves seem to be at least one metre"
        val parsedCondition = ConditionConstructors().GreaterThanOrEqualTo(waves, userExpression, "1.0")
        every { conditionParser.parse(any(), any(), any()) } returns parsedCondition
        kb.setConditionParser(conditionParser)

        kb.startRuleSession(
            sessionCase,
            ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go to Bondi."))
        )
        kb.holdsForSessionCase(parsedCondition) shouldBe true
        parsedCondition.userExpression() shouldBe userExpression

        //When
        val conditionForExpression = kb.conditionForExpression(userExpression, attributeNames)!!
        val returnedCondition = conditionForExpression

        //Then
        verify { conditionParser.parse(userExpression, attributeNames, any()) }
        returnedCondition shouldBeSameAs parsedCondition
        withClue("The user expression should be set for new condition") {
            returnedCondition.userExpression() shouldBe userExpression
        }
    }

    @Test
    fun `should create a condition using Gemini`() {
        //Given
        val waves = kb.attributeManager.getOrCreate("Waves")
        val attributeNames = listOf(waves.name)
        val height = "2.5" //high
        val sessionCase = createCase("Case", attribute = waves, value = height, range = ReferenceRange("1", "2"))
        val userExpression = "elevated waves"

        kb.startRuleSession(
            sessionCase,
            ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go to Bondi."))
        )

        //When
        val returnedCondition = kb.conditionForExpression(userExpression, attributeNames)!!

        //Then
        kb.holdsForSessionCase(returnedCondition) shouldBe true
        returnedCondition shouldBeSameAs ConditionConstructors().High(waves, userExpression)
        returnedCondition.userExpression() shouldBe userExpression
    }

    @Test
    fun `should return null if the parsed condition is not true for the session case`() {
        //Given
        val waves = kb.attributeManager.getOrCreate("Waves")
        val attributeNames = listOf(waves.name)
        val height = "0.5" //less than 1.0
        val sessionCase = createCase("Case", attribute = waves, value = height)
        val conditionParser = mockk<ConditionParser>()
        val userExpression = "waves seem to be at least one metre"
        val parsedCondition = ConditionConstructors().GreaterThanOrEqualTo(waves, userExpression, "1.0")
        every { conditionParser.parse(any(), any(), any()) } returns parsedCondition
        kb.setConditionParser(conditionParser)

        kb.startRuleSession(
            sessionCase,
            ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go to Bondi."))
        )
        kb.holdsForSessionCase(parsedCondition) shouldBe false

        //When
        val returnedCondition = kb.conditionForExpression(userExpression, attributeNames)

        //Then
        verify { conditionParser.parse(userExpression, attributeNames, any()) }
        returnedCondition shouldBe null
    }

    @Test
    fun `should return null if no condition can be parsed from the user expression`() {
        //Given
        val waves = kb.attributeManager.getOrCreate("Waves")
        val attributeNames = listOf(waves.name)
        val height = "0.5"
        val sessionCase = createCase("Case", attribute = waves, value = height)
        val conditionParser = mockk<ConditionParser>()
        val userExpression = "waves are all over the place"
        every { conditionParser.parse(any(), any(), any()) } returns null
        kb.setConditionParser(conditionParser)

        kb.startRuleSession(
            sessionCase,
            ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go to Bondi."))
        )

        //When
        val returnedCondition = kb.conditionForExpression(userExpression, attributeNames)

        //Then
        verify { conditionParser.parse(userExpression, attributeNames, any()) }
        returnedCondition shouldBe null
    }

    private fun glucose() = kb.attributeManager.getOrCreate("Glucose")

    private fun createCondition(): Condition {
        return greaterThanOrEqualTo(null, Attribute(4567, "ABC"), 5.0)
    }

    private fun createCase(
        caseName: String,
        attribute: Attribute = glucose(),
        value: String = "0.667",
        range: ReferenceRange? = null,
        id: Long? = null
    ): RDRCase {
        with(RDRCaseBuilder()) {
            val testResult = TestResult(value, range)
            addResult(attribute, defaultDate, testResult)
            return build(caseName, id)
        }
    }

    private fun createExternalCase(caseName: String, glucoseValue: String = "0.667"): ExternalCase {
        val regularCase = createCase(caseName, value = glucoseValue)
        val data = regularCase.data.mapKeys { MeasurementEvent(it.key.attribute.name, it.key.date) }
        return ExternalCase(caseName, data)
    }

    private fun createKB(kbInfo: KBInfo): KB {
        persistentKB = InMemoryKB(kbInfo)
        return KB(persistentKB)
    }
}