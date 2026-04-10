package io.rippledown.kb

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.*
import io.rippledown.chat.ReasonTransformation
import io.rippledown.constants.rule.CONDITION_IS_NOT_TRUE
import io.rippledown.constants.rule.DOES_NOT_CORRESPOND_TO_A_CONDITION
import io.rippledown.constants.rule.INTERPRETED_CONDITION_IS_NOT_TRUE
import io.rippledown.kb.chat.ModelResponder
import io.rippledown.kb.chat.RuleService
import io.rippledown.model.*
import io.rippledown.model.condition.*
import io.rippledown.model.condition.episodic.predicate.*
import io.rippledown.model.condition.episodic.signature.Current
import io.rippledown.model.diff.Addition
import io.rippledown.model.diff.Removal
import io.rippledown.model.diff.Replacement
import io.rippledown.model.external.ExternalCase
import io.rippledown.model.external.MeasurementEvent
import io.rippledown.model.rule.*
import io.rippledown.persistence.inmemory.InMemoryKB
import io.rippledown.server.websocket.WebSocketManager
import io.rippledown.util.shouldBeSameAs
import io.rippledown.utils.DEFAULT_GLUCOSE_VALUE
import io.rippledown.utils.createViewableCase
import io.rippledown.utils.defaultDate
import io.rippledown.utils.shouldBeSameAs
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class KBTest {
    private lateinit var persistentKB: InMemoryKB
    private lateinit var kb: KB
    private lateinit var session: KBSession
    private lateinit var rsm: RuleSessionManager
    lateinit var webSocketManager: WebSocketManager

    @BeforeTest
    fun setup() {
        val kbInfo = KBInfo("id123", "Blah")
        webSocketManager = mockk()
        kb = createKB(kbInfo)
    }

    @Test
    fun `should call web socket manager when sending cornerstone status`() = runTest {
        //Given
        val sessionCase = createCase("Case1")
        val conclusion = kb.conclusionManager.getOrCreate("Whatever.")
        val ccStatus = rsm.startRuleSession(sessionCase, ChangeTreeToAddConclusion(conclusion))

        //When
        rsm.sendCornerstoneStatus()

        //Then
        coVerify { webSocketManager.sendStatus(ccStatus) }
    }

    @Test
    fun `sendCornerstoneStatus should send the selected cornerstone after selectCornerstone`() = runTest {
        //Given
        kb.addCornerstoneCase(createCase("Case1"))
        val cc2 = kb.addCornerstoneCase(createCase("Case2"))
        kb.addCornerstoneCase(createCase("Case3"))
        val vcc2 = kb.viewableCase(cc2)
        val sessionCase = createCase("Session")
        rsm.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go to Bondi.")))

        //When
        rsm.selectCornerstone(1)
        rsm.sendCornerstoneStatus()

        //Then
        coVerify { webSocketManager.sendStatus(match { it.cornerstoneToReview == vcc2 && it.indexOfCornerstoneToReview == 1 }) }
    }

    @Test
    fun `sendCornerstoneStatus should send the last cornerstone after selecting it`() = runTest {
        //Given
        kb.addCornerstoneCase(createCase("Case1"))
        kb.addCornerstoneCase(createCase("Case2"))
        val cc3 = kb.addCornerstoneCase(createCase("Case3"))
        val vcc3 = kb.viewableCase(cc3)
        val sessionCase = createCase("Session")
        rsm.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go to Bondi.")))

        //When
        rsm.selectCornerstone(2)
        rsm.sendCornerstoneStatus()

        //Then
        coVerify { webSocketManager.sendStatus(match { it.cornerstoneToReview == vcc3 && it.indexOfCornerstoneToReview == 2 }) }
    }

    @Test
    fun `sendCornerstoneStatus should send the first cornerstone when no selection has been made`() = runTest {
        //Given
        val cc1 = kb.addCornerstoneCase(createCase("Case1"))
        kb.addCornerstoneCase(createCase("Case2"))
        kb.addCornerstoneCase(createCase("Case3"))
        val vcc1 = kb.viewableCase(cc1)
        val sessionCase = createCase("Session")
        rsm.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go to Bondi.")))

        //When
        rsm.sendCornerstoneStatus()

        //Then
        coVerify { webSocketManager.sendStatus(match { it.cornerstoneToReview == vcc1 && it.indexOfCornerstoneToReview == 0 }) }
    }

    @Test
    fun `sendCornerstoneStatus should reflect the most recent selectCornerstone call`() = runTest {
        //Given
        kb.addCornerstoneCase(createCase("Case1"))
        val cc2 = kb.addCornerstoneCase(createCase("Case2"))
        val cc3 = kb.addCornerstoneCase(createCase("Case3"))
        val vcc3 = kb.viewableCase(cc3)
        val sessionCase = createCase("Session")
        rsm.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go to Bondi.")))

        //When - select second, then select third
        rsm.selectCornerstone(1)
        rsm.selectCornerstone(2)
        rsm.sendCornerstoneStatus()

        //Then - should send the third (most recent selection)
        coVerify { webSocketManager.sendStatus(match { it.cornerstoneToReview == vcc3 && it.indexOfCornerstoneToReview == 2 }) }
    }

    @Test
    fun `cornerstoneStatus should return the correct index after selectCornerstone`() {
        //Given
        kb.addCornerstoneCase(createCase("Case1"))
        val cc2 = kb.addCornerstoneCase(createCase("Case2"))
        kb.addCornerstoneCase(createCase("Case3"))
        val vcc2 = kb.viewableCase(cc2)
        val sessionCase = createCase("Session")
        rsm.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go to Bondi.")))

        //When
        rsm.selectCornerstone(1)
        val status = rsm.cornerstoneStatus()

        //Then
        status.cornerstoneToReview shouldBe vcc2
        status.indexOfCornerstoneToReview shouldBe 1
        status.numberOfCornerstones shouldBe 3
    }

    @Test
    fun `cornerstoneStatus should return index 0 when no selection has been made`() {
        //Given
        val cc1 = kb.addCornerstoneCase(createCase("Case1"))
        kb.addCornerstoneCase(createCase("Case2"))
        val vcc1 = kb.viewableCase(cc1)
        val sessionCase = createCase("Session")
        rsm.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go to Bondi.")))

        //When
        val status = rsm.cornerstoneStatus()

        //Then
        status.cornerstoneToReview shouldBe vcc1
        status.indexOfCornerstoneToReview shouldBe 0
        status.numberOfCornerstones shouldBe 2
    }

    @Test
    fun `cornerstoneStatus should reflect the most recent selectCornerstone call`() {
        //Given
        kb.addCornerstoneCase(createCase("Case1"))
        kb.addCornerstoneCase(createCase("Case2"))
        val cc3 = kb.addCornerstoneCase(createCase("Case3"))
        val vcc3 = kb.viewableCase(cc3)
        val sessionCase = createCase("Session")
        rsm.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go to Bondi.")))

        //When
        rsm.selectCornerstone(0)
        rsm.selectCornerstone(2)
        val status = rsm.cornerstoneStatus()

        //Then
        status.cornerstoneToReview shouldBe vcc3
        status.indexOfCornerstoneToReview shouldBe 2
        status.numberOfCornerstones shouldBe 3
    }

    @Test
    fun `cornerstoneStatus should return correct index for navigating back after selecting last`() {
        //Given
        val cc1 = kb.addCornerstoneCase(createCase("Case1"))
        kb.addCornerstoneCase(createCase("Case2"))
        kb.addCornerstoneCase(createCase("Case3"))
        val vcc1 = kb.viewableCase(cc1)
        val sessionCase = createCase("Session")
        rsm.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go to Bondi.")))

        //When - select last, then select first
        rsm.selectCornerstone(2)
        rsm.selectCornerstone(0)
        val status = rsm.cornerstoneStatus()

        //Then
        status.cornerstoneToReview shouldBe vcc1
        status.indexOfCornerstoneToReview shouldBe 0
        status.numberOfCornerstones shouldBe 3
    }

    @Test
    fun `should call web socket manager when sending rule session completed`() = runTest {
        //Given
        val sessionCase = createCase("Case1")
        val conclusion = kb.conclusionManager.getOrCreate("Whatever.")
        rsm.startRuleSession(sessionCase, ChangeTreeToAddConclusion(conclusion))

        //When
        rsm.sendRuleSessionCompleted()

        //Then
        coVerify { webSocketManager.sendRuleSessionCompleted() }
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
        // Given
        val conclusionToAdd = "Whatever"
        buildRuleToAddAComment(kb, conclusionToAdd)
        val externalCase = createExternalCase("Case1", "g1")

        // When
        val processed = kb.processCase(externalCase)

        // Then
        processed.interpretation.conclusionTexts() shouldBe setOf(conclusionToAdd)
    }

    @Test
    fun `getProcessedCase should return the case with a blank interpretation`() {
        // Given
        val conclusionToAdd = "Whatever"
        buildRuleToAddAComment(kb, conclusionToAdd)
        val externalCase = createExternalCase("Case", "g")
        val processed = kb.processCase(externalCase)
        processed.interpretation.conclusionTexts() shouldBe setOf(conclusionToAdd)

        // When
        val retrieved = kb.getProcessedCase(processed.caseId.id!!)!!

        // Then
        retrieved.interpretation.conclusionTexts() shouldBe emptySet()
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
    fun `should return the attribute names in a KB`() {
        // Given
        kb.attributeNames() shouldBe emptyList()

        // When
        kb.attributeManager.getOrCreate("Glucose")
        kb.attributeManager.getOrCreate("Cholesterol")

        // Then
        kb.attributeNames() shouldBe setOf("Glucose", "Cholesterol")
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
        rsm.startRuleSession(sessionCase, ChangeTreeToAddConclusion(conclusion))
        rsm.commitCurrentRuleSession()
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
    fun `description for most recent rule when none have been built`() {
        with(rsm.descriptionOfMostRecentRule()) {
            description shouldBe "There are no rules to undo."
            canRemove shouldBe false
        }
    }

    @Test
    fun `description for rule that adds a comment`() {
        val sessionCase = createCase("Case1", value = "1.0")
        sessionCase.interpretation.conclusionTexts() shouldBe emptySet()
        val commentText = "The Brindabellas are a range of beautiful mountains to the south east of Canberra."
        rsm.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate(commentText)))
        rsm.commitCurrentRuleSession()
        with(rsm.descriptionOfMostRecentRule()) {
            description shouldBe "Rule to add comment:\n${commentText.substring(0, 20)}..."
            canRemove shouldBe true
        }
    }

    @Test
    fun `description for rule that removes a comment`() {
        val case1 = createCase("Case1", value = "1.0")
        case1.interpretation.conclusionTexts() shouldBe emptySet()
        val commentText = "Lake Burley-Griffin is not renowned as somewhere to go swimming."
        val comment = kb.conclusionManager.getOrCreate(commentText)
        rsm.startRuleSession(case1, ChangeTreeToAddConclusion(comment))
        rsm.commitCurrentRuleSession()
        val case2 = createCase("Case2", value = "2.0")
        rsm.startRuleSession(case2, ChangeTreeToRemoveConclusion(comment))
        rsm.commitCurrentRuleSession()
        with(rsm.descriptionOfMostRecentRule()) {
            description shouldBe "Rule to remove comment:\n${commentText.substring(0, 20)}..."
            canRemove shouldBe true
        }
    }

    @Test
    fun `description for rule that replaces a comment with another comment`() {
        val case1 = createCase("Case1", value = "1.0")
        case1.interpretation.conclusionTexts() shouldBe emptySet()
        val commentText = "The National Portrait Gallery is well worth a visit."
        val comment = kb.conclusionManager.getOrCreate(commentText)
        rsm.startRuleSession(case1, ChangeTreeToAddConclusion(comment))
        rsm.commitCurrentRuleSession()
        val case2 = createCase("Case2", value = "2.0")
        val replacementText = "The National Library is a good place for studying."
        val replacement = kb.conclusionManager.getOrCreate(replacementText)
        rsm.startRuleSession(case2, ChangeTreeToReplaceConclusion(comment, replacement))
        rsm.commitCurrentRuleSession()
        with(rsm.descriptionOfMostRecentRule()) {
            val expected = """
                Rule to replace comment:
                ${commentText.substring(0, 20)}...
                with:
                ${replacementText.substring(0, 20)}...
            """.trimIndent()
            description shouldBe expected
            canRemove shouldBe true
        }
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
            rsm.addConditionToCurrentRuleSession(createCondition())
        }.message shouldBe noSessionMessage

        shouldThrow<IllegalStateException> {
            rsm.conflictingCasesInCurrentRuleSession()
        }.message shouldBe noSessionMessage

        shouldThrow<IllegalStateException> {
            rsm.commitCurrentRuleSession()
        }.message shouldBe noSessionMessage
    }

    @Test
    fun `rule session must be started before it can be cancelled`() {
        val noSessionMessage = "No rule session in progress."
        shouldThrow<IllegalStateException> {
            rsm.cancelRuleSession()
        }.message shouldBe noSessionMessage
    }

    @Test
    fun `cannot start a rule session if one is already started`() {
        val sessionCase = createCase("Case1")
        val conclusion = kb.conclusionManager.getOrCreate("Whatever.")
        rsm.startRuleSession(sessionCase, ChangeTreeToAddConclusion(conclusion))
        shouldThrow<IllegalStateException> {
            rsm.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Stuff.")))
        }.message shouldBe "Session already in progress."
    }

    @Test
    fun `cannot start a rule session if action is not applicable to session case`() {
        val sessionCase = createCase("Case1", value = "1.0")
        val otherCase = createCase("Case2", value = "1.0")
        rsm.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Whatever.")))
        rsm.commitCurrentRuleSession()
        kb.interpret(otherCase)
        otherCase.interpretation.conclusionTexts() shouldBe setOf("Whatever.") // sanity

        shouldThrow<IllegalStateException> {
            rsm.startRuleSession(otherCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Whatever.")))
        }.message shouldBe "Action ChangeTreeToAddConclusion(toBeAdded=Conclusion(id=1, text=Whatever.)) is not applicable to case Case2"
    }

    @Test
    fun startRuleSession() {
        val sessionCase = createCase("Case1")
        kb.interpret(sessionCase)
        sessionCase.interpretation.conclusionTexts() shouldBe emptySet()
        rsm.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Whatever.")))
        rsm.commitCurrentRuleSession()
        kb.interpret(sessionCase)
        sessionCase.interpretation.conclusionTexts() shouldBe setOf("Whatever.")
    }

    @Test
    fun conflictingCases() {
        val sessionCase = createCase("Case1", value = "1.0")
        kb.addCornerstoneCase(createCase("Case2", value = "2.0"))
        sessionCase.interpretation.conclusionTexts() shouldBe emptySet()
        rsm.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Whatever.")))
        rsm.conflictingCasesInCurrentRuleSession().map { rdrCase -> rdrCase.name }.toSet() shouldBe setOf("Case2")
    }

    @Test
    fun addCondition() {
        val sessionCase = createCase("Case1", value = "1.0")
        kb.addCornerstoneCase(createCase("Case2", value = "2.0"))
        sessionCase.interpretation.conclusionTexts() shouldBe emptySet()
        rsm.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Whatever.")))
        rsm.addConditionToCurrentRuleSession(lessThanOrEqualTo(null, glucose(), 1.2))
        rsm.conflictingCasesInCurrentRuleSession().size shouldBe 0
    }

    @Test
    fun `should remove condition from rule session`() {
        //Given
        val sessionCase = createCase("Case1", value = "1.0")
        val cornerstoneCase = createViewableCase("Case2", caseId = 1, CaseType.Cornerstone)
        kb.addCornerstoneCase(cornerstoneCase.case)
        sessionCase.interpretation.conclusionTexts() shouldBe emptySet()
        rsm.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Whatever.")))
        val condition = lessThanOrEqualTo(null, glucose(), 1.2)
        rsm.addConditionToCurrentRuleSession(condition)
        rsm.conflictingCasesInCurrentRuleSession().size shouldBe 0

        //When
        val addedCondition = kb.conditionManager.getOrCreate(condition)
        val ccStatus = rsm.removeCondition(addedCondition.id!!)

        //Then
        rsm.conflictingCasesInCurrentRuleSession().size shouldBe 1
        ccStatus shouldBe CornerstoneStatus(cornerstoneCase, 0, 1)
    }

    @Test
    fun commitSession() {
        val sessionCase = createCase("Case1", value = "1.0")
        val otherCase = createCase("Case2", value = "2.0")
        sessionCase.interpretation.conclusionTexts() shouldBe emptySet()
        rsm.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Whatever.")))
        kb.interpret(otherCase)
        // Rule not yet added...
        otherCase.interpretation.conclusionTexts() shouldBe emptySet()
        rsm.commitCurrentRuleSession()
        // Rule now added...
        kb.interpret(otherCase)
        otherCase.interpretation.conclusionTexts() shouldBe setOf("Whatever.")
    }

    @Test
    fun undoLastRuleSession() {
        val sessionCase = createCase("Case1", value = "1.0")
        rsm.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Whatever.")))
        rsm.commitCurrentRuleSession()

        val otherCase = createCase("Case2", value = "2.0")
        kb.interpret(otherCase)
        otherCase.interpretation.conclusionTexts() shouldBe setOf("Whatever.") // Sanity

        rsm.undoLastRuleSession()
        kb.interpret(otherCase)
        otherCase.interpretation.conclusionTexts() shouldBe emptySet()
    }

    @Test
    fun `should return condition hints for case`() {
        val caseWithGlucoseAttribute = createCase("A", value = "1.0")
        val conditionList = rsm.conditionHintsForCase(caseWithGlucoseAttribute)
        conditionList.suggestions.toSet() shouldBe ConditionSuggester(
            kb.attributeManager.all(),
            caseWithGlucoseAttribute
        ).suggestions().toSet()
    }

    @Test
    fun `should return condition for matching non-editable suggestion text`() {
        // Given
        val caseWithGlucoseAttribute = createCase("A", value = "1.0")
        val hints = rsm.conditionHintsForCase(caseWithGlucoseAttribute)
        val nonEditableSuggestion = hints.suggestions.first { !it.isEditable() }
        val conditionText = nonEditableSuggestion.asText()

        // When
        val condition = rsm.conditionForSuggestionText(caseWithGlucoseAttribute, conditionText)

        // Then
        condition shouldNotBe null
        condition!!.asText() shouldBe conditionText
    }

    @Test
    fun `should return null when no suggestion matches the text`() {
        // Given
        val caseWithGlucoseAttribute = createCase("A", value = "1.0")

        // When
        val condition = rsm.conditionForSuggestionText(caseWithGlucoseAttribute, "no such condition")

        // Then
        condition shouldBe null
    }

    @Test
    fun `should not return condition for editable suggestion text`() {
        // Given
        val caseWithGlucoseAttribute = createCase("A", value = "1.0")
        val hints = rsm.conditionHintsForCase(caseWithGlucoseAttribute)
        val editableSuggestion = hints.suggestions.firstOrNull { it.isEditable() }

        // When/Then - if there is an editable suggestion, looking it up should return null
        if (editableSuggestion != null) {
            val condition = rsm.conditionForSuggestionText(caseWithGlucoseAttribute, editableSuggestion.asText())
            condition shouldBe null
        }
    }

    @Test // Conc-4
    fun `conclusions are aligned when building rules`() {
        val conclusionToAdd = kb.conclusionManager.getOrCreate("Whatever")
        val copyOfConclusion = conclusionToAdd.copy()
        kb.addCornerstoneCase(createCase("Case1", value = "1.0"))
        val sessionCase = kb.getCornerstoneCaseByName("Case1")
        rsm.startRuleSession(sessionCase, ChangeTreeToAddConclusion(copyOfConclusion))
        rsm.commitCurrentRuleSession()
        kb.interpret(sessionCase)
        sessionCase.interpretation.conclusions().single() shouldBeSameInstanceAs conclusionToAdd
    }

    @Test
    fun `the session case should be stored as the cornerstone case when the rule is committed`() {
        val sessionCase = createCase("Case1", value = "1.0")
        sessionCase.interpretation.conclusionTexts() shouldBe emptySet()
        rsm.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Whatever.")))
        kb.allCornerstoneCases() shouldHaveSize 0
        rsm.commitCurrentRuleSession()
        kb.allCornerstoneCases() shouldHaveSize 1
        kb.containsCornerstoneCaseWithName("Case1") shouldBe true
    }

    @Test
    fun `cornerstoneCaseIds should return ids of cornerstone cases only`() {
        kb.cornerstoneCaseIds() shouldHaveSize 0
        val cc = kb.addCornerstoneCase(createCase("CC1", value = "1.0"))
        kb.cornerstoneCaseIds() shouldHaveSize 1
        kb.cornerstoneCaseIds().first().name shouldBe "CC1"
    }

    @Test
    fun `cornerstoneCaseIds should not include processed cases`() {
        val externalCase = createExternalCase("Processed1")
        kb.processCase(externalCase)
        kb.processedCaseIds() shouldHaveSize 1
        kb.cornerstoneCaseIds() shouldHaveSize 0
    }

    @Test
    fun `getCase should return a processed case by id`() {
        val externalCase = createExternalCase("Processed1")
        val processed = kb.processCase(externalCase)
        val retrieved = kb.getCase(processed.caseId.id!!)
        retrieved shouldNotBe null
        retrieved!!.name shouldBe "Processed1"
    }

    @Test
    fun `getCase should return a cornerstone case by id`() {
        val cc = kb.addCornerstoneCase(createCase("CC1", value = "1.0"))
        val retrieved = kb.getCase(cc.caseId.id!!)
        retrieved shouldNotBe null
        retrieved!!.name shouldBe "CC1"
    }

    @Test
    fun `getCase should return null for unknown id`() {
        kb.getCase(999L) shouldBe null
    }

    @Test
    fun `addCornerstoneCase from ExternalCase should store the case as a cornerstone`() {
        kb.cornerstoneCaseIds() shouldHaveSize 0
        val externalCase = createExternalCase("CC1")
        val stored = kb.addCornerstoneCase(externalCase)
        stored.name shouldBe "CC1"
        stored.caseId.id shouldNotBe null
        kb.cornerstoneCaseIds() shouldHaveSize 1
        kb.cornerstoneCaseIds().first().name shouldBe "CC1"
    }

    @Test
    fun `addCornerstoneCase from ExternalCase should not create a processed case`() {
        val externalCase = createExternalCase("CC1")
        kb.addCornerstoneCase(externalCase)
        kb.processedCaseIds() shouldHaveSize 0
        kb.cornerstoneCaseIds() shouldHaveSize 1
    }

    @Test
    fun `addCornerstoneCase from ExternalCase should preserve the case data`() {
        val externalCase = createExternalCase("CC1", glucoseValue = "5.5")
        val stored = kb.addCornerstoneCase(externalCase)
        val retrieved = kb.getCase(stored.caseId.id!!)
        retrieved shouldNotBe null
        retrieved!!.name shouldBe "CC1"
        retrieved.getLatest(kb.attributeManager.getOrCreate("Glucose"))!!.value.text shouldBe "5.5"
    }

    @Test
    fun `addCornerstoneCase from ExternalCase should allow multiple cornerstone cases`() {
        kb.addCornerstoneCase(createExternalCase("CC1"))
        kb.addCornerstoneCase(createExternalCase("CC2"))
        kb.cornerstoneCaseIds() shouldHaveSize 2
        kb.cornerstoneCaseIds().map { it.name } shouldBe listOf("CC1", "CC2")
    }

    @Test
    fun `addCornerstoneCase from ExternalCase should not affect existing processed cases`() {
        val processed = kb.processCase(createExternalCase("P1"))
        kb.addCornerstoneCase(createExternalCase("CC1"))
        kb.processedCaseIds() shouldHaveSize 1
        kb.processedCaseIds().first().name shouldBe "P1"
        kb.cornerstoneCaseIds() shouldHaveSize 1
        kb.cornerstoneCaseIds().first().name shouldBe "CC1"
    }

    @Test
    fun `should update the cornerstone status when the conditions change`() {
        val cc1 = kb.addCornerstoneCase(createCase("Case1", value = "1.0"))
        val vcc1 = kb.viewableCase(cc1)
        val sessionCase = createCase("Case3", value = "3.0")

        rsm.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go to Bondi.")))
        val currentCCStatus = CornerstoneStatus(vcc1, 0, 1)
        withClue("sanity check") {
            rsm.cornerstoneStatus(vcc1) shouldBe currentCCStatus
        }
        val condition = lessThanOrEqualTo(null, glucose(), 0.5) //false for the cornerstone
        val updateRequest = UpdateCornerstoneRequest(currentCCStatus, RuleConditionList(listOf(condition)))
        rsm.updateCornerstone(updateRequest) shouldBe CornerstoneStatus(ruleConditions = listOf(condition.asText()))
    }

    @Test
    fun `should not update the cornerstone status if no cornerstones are removed by the condition change`() {
        val cc1 = kb.addCornerstoneCase(createCase("Case1", value = "1.0"))
        val vcc1 = kb.viewableCase(cc1)
        val sessionCase = createCase("Case3", value = "3.0")

        rsm.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go to Bondi.")))
        val currentCCStatus = CornerstoneStatus(vcc1, 0, 1)
        withClue("sanity check") {
            rsm.cornerstoneStatus(vcc1) shouldBe currentCCStatus
        }
        val condition = lessThanOrEqualTo(null, glucose(), 1.0) //true for the cornerstone
        val updateRequest = UpdateCornerstoneRequest(currentCCStatus, RuleConditionList(listOf(condition)))
        rsm.updateCornerstone(updateRequest) shouldBe currentCCStatus.copy(ruleConditions = listOf(condition.asText()))
    }

    @Test
    fun `should reset the first cornerstone case if the current cornerstone has been removed by the condition change`() {
        val cc1 = kb.addCornerstoneCase(createCase("Case1", value = "1.0"))
        val cc2 = kb.addCornerstoneCase(createCase("Case2", value = "2.0"))
        kb.addCornerstoneCase(createCase("Case3", value = "3.0"))
        val vcc1 = kb.viewableCase(cc1)
        val vcc2 = kb.viewableCase(cc2)
        val sessionCase = createCase("Case4", value = "4.0")

        rsm.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go to Bondi.")))
        val currentCCStatus = CornerstoneStatus(vcc1, 0, 3)
        withClue("sanity check") {
            rsm.cornerstoneStatus(vcc1) shouldBe currentCCStatus
        }
        val condition = greaterThanOrEqualTo(null, glucose(), 1.5) //false for the current cornerstone
        val updateRequest = UpdateCornerstoneRequest(currentCCStatus, RuleConditionList(listOf(condition)))
        val expected = CornerstoneStatus(vcc2, 0, 2, ruleConditions = listOf(condition.asText()))
        rsm.updateCornerstone(updateRequest) shouldBe expected
    }

    @Test
    fun `should remain on the current cornerstone case if it has not been removed by the condition change`() {
        val cc1 = kb.addCornerstoneCase(createCase("Case1", value = "1.0"))
        kb.addCornerstoneCase(createCase("Case2", value = "2.0"))
        kb.addCornerstoneCase(createCase("Case3", value = "3.0"))
        val vcc1 = kb.viewableCase(cc1)
        val sessionCase = createCase("Case4", value = "4.0")

        rsm.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go to Bondi.")))
        val currentCCStatus = CornerstoneStatus(vcc1, 0, 3)
        withClue("sanity check") {
            rsm.cornerstoneStatus(vcc1) shouldBe currentCCStatus
        }
        val condition = lessThanOrEqualTo(null, glucose(), 2.5) //true for the current cornerstone and cc2
        val updateRequest = UpdateCornerstoneRequest(currentCCStatus, RuleConditionList(listOf(condition)))
        val expected = CornerstoneStatus(vcc1, 0, 2, ruleConditions = listOf(condition.asText()))
        rsm.updateCornerstone(updateRequest) shouldBe expected
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
        rsm.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go to Bondi.")))

        //Assume that the user has skipped to the 2nd cornerstone
        val originalCCStatus = CornerstoneStatus(vcc2, 1, 3)
        withClue("sanity check") {
            rsm.cornerstoneStatus(vcc2) shouldBe originalCCStatus
        }
        val condition = lessThanOrEqualTo(null, glucose(), 2.5) //true for cc1 and the current cornerstone cc2
        var updateRequest = UpdateCornerstoneRequest(originalCCStatus, RuleConditionList(listOf(condition)))
        val expected = CornerstoneStatus(vcc2, 1, 2, ruleConditions = listOf(condition.asText()))
        rsm.updateCornerstone(updateRequest) shouldBe expected
        rsm.cornerstoneStatus(vcc2) shouldBe expected

        //Remove the condition that was added
        updateRequest = UpdateCornerstoneRequest(expected, RuleConditionList(emptyList()))

        //Then
        rsm.updateCornerstone(updateRequest) shouldBe originalCCStatus
        rsm.cornerstoneStatus(vcc2) shouldBe originalCCStatus
    }

    @Test
    fun `should remain on the current cornerstone case if it has not been removed by the condition change and it is not the first one`() {
        val cc1 = kb.addCornerstoneCase(createCase("Case1", value = "1.0"))
        val cc2 = kb.addCornerstoneCase(createCase("Case2", value = "2.0"))
        kb.addCornerstoneCase(createCase("Case3", value = "3.0"))
        kb.viewableCase(cc1)
        val vcc2 = kb.viewableCase(cc2)
        val sessionCase = createCase("Case4", value = "4.0")

        rsm.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go to Bondi.")))
        val currentCCStatus = CornerstoneStatus(vcc2, 1, 3) //the user has skipped to the 2nd cornerstone
        withClue("sanity check") {
            rsm.cornerstoneStatus(vcc2) shouldBe currentCCStatus
        }

        val condition =
            lessThanOrEqualTo(null, glucose(), 2.5) //true for cc1 and the current cornerstone. false for cc3
        val updateRequest = UpdateCornerstoneRequest(currentCCStatus, RuleConditionList(listOf(condition)))
        val expected = CornerstoneStatus(vcc2, 1, 2, ruleConditions = listOf(condition.asText()))
        rsm.updateCornerstone(updateRequest) shouldBe expected
    }

    @Test
    fun `should return no cornerstones when the rule session has just started if there aren't any cornerstones`() {
        val sessionCase = createCase("Case4", value = "4.0")
        rsm.startRuleSession(
            sessionCase,
            ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go to Bondi."))
        )
        val ccStatus = rsm.cornerstoneStatus(null)
        ccStatus shouldBe CornerstoneStatus()
    }

    @Test
    fun `should return all cornerstones when the rule session has just started and no cornerstone has been selected`() {
        val cc1 = kb.addCornerstoneCase(createCase("Case1", value = "1.0"))
        kb.addCornerstoneCase(createCase("Case2", value = "2.0"))
        kb.addCornerstoneCase(createCase("Case3", value = "3.0"))
        val vcc1 = kb.viewableCase(cc1)

        val sessionCase = createCase("Case4", value = "4.0")
        rsm.startRuleSession(
            sessionCase,
            ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go to Bondi."))
        )
        val ccStatus = rsm.cornerstoneStatus(null)
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
        rsm.startRuleSession(
            sessionCase,
            ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go to Bondi."))
        )
        val ccStatus = rsm.cornerstoneStatus(vcc2)
        ccStatus shouldBe CornerstoneStatus(vcc2, 1, 3)
    }

    @Test
    fun `should not set the user expression for a condition parsed from that expression if the condition already exists`() {
        //Given
        val waves = kb.attributeManager.getOrCreate("Waves")
        val height = "2" //greater than 1
        val sessionCase = createCase("Case", attribute = waves, value = height)
        val conditionParser = mockk<ConditionParser>()
        val userExpression = "waves seem to be at least one metre"
        val parsedCondition = EpisodicCondition(null, waves, GreaterThanOrEquals(1.0), Current, userExpression)
        every { conditionParser.parse(any(), any()) } returns parsedCondition
        rsm.setConditionParser(conditionParser)

        rsm.startRuleSession(
            sessionCase,
            ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go to Bondi."))
        )
        parsedCondition.userExpression() shouldBe userExpression

        //When
        val existingCondition = kb.conditionManager.getOrCreate(greaterThanOrEqualTo(null, waves, 1.0))
        val returnedCondition = rsm.conditionForExpression(userExpression).condition!!

        //Then
        verify { conditionParser.parse(userExpression, any()) }
        returnedCondition shouldBe existingCondition
        withClue("The user expression should not be set for an existing condition") {
            returnedCondition.userExpression() shouldBe ""
        }
    }

    @Test
    fun `should set the user expression for a condition parsed from that expression if the condition does not already exist`() {
        //Given
        val waves = kb.attributeManager.getOrCreate("Waves")
        val height = "2" //greater than 1
        val sessionCase = createCase("Case", attribute = waves, value = height)
        val conditionParser = mockk<ConditionParser>()
        val userExpression = "waves seem to be at least one metre"
        val parsedCondition = EpisodicCondition(null, waves, GreaterThanOrEquals(1.0), Current, userExpression)
        every { conditionParser.parse(any(), any()) } returns parsedCondition
        rsm.setConditionParser(conditionParser)

        rsm.startRuleSession(
            sessionCase,
            ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go to Bondi."))
        )
        parsedCondition.userExpression() shouldBe userExpression

        //When
        val conditionForExpression = rsm.conditionForExpression(userExpression).condition!!
        val returnedCondition = conditionForExpression

        //Then
        verify { conditionParser.parse(userExpression, any()) }
        returnedCondition shouldBeSameAs parsedCondition
        withClue("The user expression should be set for new condition") {
            returnedCondition.userExpression() shouldBe userExpression
        }
    }

    @Test
    fun `should return the condition for a user expression`() = runTest {
        //Given
        val x = kb.attributeManager.getOrCreate("x")
        val value = "42"
        val case = createCase("Case", attribute = x, value = value)
        val userExpression = "x equates to $value"

        rsm.startRuleSession(
            case,
            ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Whatever."))
        )

        //When
        val conditionParsingResult = rsm.conditionForExpression(userExpression)

        //Then
        val expectedCondition = EpisodicCondition(
            null,
            x,
            Is(value),
            Current,
            userExpression
        )
        conditionParsingResult.isFailure shouldBe false
        conditionParsingResult.condition shouldBeSameAs expectedCondition
    }

    @Test
    fun `should return the condition for a user expression involving contains`() = runTest {
        //Given
        val x = kb.attributeManager.getOrCreate("x")
        val value = "ab"
        val case = createCase("Case", attribute = x, value = value)
        val userExpression = "x contains b"

        rsm.startRuleSession(
            case,
            ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Whatever."))
        )

        //When
        val conditionParsingResult = rsm.conditionForExpression(userExpression)

        //Then
        val expectedCondition = EpisodicCondition(
            null,
            x,
            Contains("\"b\""),
            Current,
            userExpression
        )
        conditionParsingResult.isFailure shouldBe false
        conditionParsingResult.condition shouldBeSameAs expectedCondition
    }

    @Test
    fun `should create a condition using Gemini`() {
        //Given
        val waves = kb.attributeManager.getOrCreate("Waves")
        val height = "2.5" //high
        val sessionCase = createCase("Case", attribute = waves, value = height, range = ReferenceRange("1", "2"))
        val userExpression = "elevated waves"

        rsm.startRuleSession(
            sessionCase,
            ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go to Bondi."))
        )

        //When
        val returnedCondition = rsm.conditionForExpression(userExpression).condition!!

        //Then
        returnedCondition shouldBeSameAs EpisodicCondition(null, waves, High, Current, userExpression)
    }

    @Test
    fun `should return error if the parsed condition references an attribute not in the case`() {
        //Given
        val waves = kb.attributeManager.getOrCreate("Waves")
        val unknownAttribute = kb.attributeManager.getOrCreate("x")
        val height = "0.5"
        val sessionCase = createCase("Case", attribute = waves, value = height)
        val conditionParser = mockk<ConditionParser>()
        val userExpression = "below"
        val parsedCondition = EpisodicCondition(null, unknownAttribute, High, Current, userExpression)
        every { conditionParser.parse(any(), any()) } returns parsedCondition
        rsm.setConditionParser(conditionParser)

        rsm.startRuleSession(
            sessionCase,
            ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go to Bondi."))
        )

        //When
        val result = rsm.conditionForExpression(userExpression)

        //Then
        result.condition shouldBe null
        result.errorMessage shouldBe DOES_NOT_CORRESPOND_TO_A_CONDITION
    }

    @Test
    fun `should return null if the parsed condition is not true for the session case`() {
        //Given
        val waves = kb.attributeManager.getOrCreate("Waves")
        val height = "0.5" //less than 1.0
        val sessionCase = createCase("Case", attribute = waves, value = height)
        val conditionParser = mockk<ConditionParser>()
        val userExpression = "waves seem to be at least one metre"
        val parsedCondition = EpisodicCondition(null, waves, GreaterThanOrEquals(1.0), Current, userExpression)
        every { conditionParser.parse(any(), any()) } returns parsedCondition
        rsm.setConditionParser(conditionParser)

        rsm.startRuleSession(
            sessionCase,
            ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go to Bondi."))
        )

        //When
        val returnedCondition = rsm.conditionForExpression(userExpression).condition

        //Then
        verify { conditionParser.parse(userExpression, any()) }
        returnedCondition shouldBe null
    }

    @Test
    fun `should return interpreted error message when expression differs from condition text and condition is not true`() {
        //Given
        val waves = kb.attributeManager.getOrCreate("Waves")
        val height = "2.1"
        val sessionCase = createCase("Case", attribute = waves, value = height)
        val conditionParser = mockk<ConditionParser>()
        val userExpression = "below"
        val parsedCondition = EpisodicCondition(null, waves, Low, Current, userExpression)
        every { conditionParser.parse(any(), any()) } returns parsedCondition
        rsm.setConditionParser(conditionParser)

        rsm.startRuleSession(
            sessionCase,
            ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go to Bondi."))
        )

        //When
        val result = rsm.conditionForExpression(userExpression)

        //Then
        result.condition shouldBe null
        result.errorMessage shouldBe INTERPRETED_CONDITION_IS_NOT_TRUE.format(userExpression, parsedCondition.asText())
    }

    @Test
    fun `should return standard error message when expression matches condition text and condition is not true`() {
        //Given
        val waves = kb.attributeManager.getOrCreate("Waves")
        val height = "0.5" //less than 1.0
        val sessionCase = createCase("Case", attribute = waves, value = height)
        val conditionParser = mockk<ConditionParser>()
        val userExpression = "Waves ≥ 1.0"
        val parsedCondition = EpisodicCondition(null, waves, GreaterThanOrEquals(1.0), Current, userExpression)
        every { conditionParser.parse(any(), any()) } returns parsedCondition
        rsm.setConditionParser(conditionParser)

        rsm.startRuleSession(
            sessionCase,
            ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go to Bondi."))
        )

        //When
        val result = rsm.conditionForExpression(userExpression)

        //Then
        result.condition shouldBe null
        result.errorMessage shouldBe CONDITION_IS_NOT_TRUE
    }

    @Test
    fun `should return standard error message when expression differs from condition text only by quoted constants`() {
        //Given
        val uv = kb.attributeManager.getOrCreate("UV")
        val sessionCase = createCase("Case", attribute = uv, value = "4.5")
        val conditionParser = mockk<ConditionParser>()
        val userExpression = "UV is 5.6"
        // The parsed condition text would be: UV is "5.6"
        val parsedCondition = EpisodicCondition(null, uv, Is("5.6"), Current, userExpression)
        every { conditionParser.parse(any(), any()) } returns parsedCondition
        rsm.setConditionParser(conditionParser)

        rsm.startRuleSession(
            sessionCase,
            ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go to Bondi."))
        )

        //When
        val result = rsm.conditionForExpression(userExpression)

        //Then
        result.condition shouldBe null
        result.errorMessage shouldBe CONDITION_IS_NOT_TRUE
    }

    @Test
    fun `should return null if no condition can be parsed from the user expression`() {
        //Given
        val waves = kb.attributeManager.getOrCreate("Waves")
        val height = "0.5"
        val sessionCase = createCase("Case", attribute = waves, value = height)
        val conditionParser = mockk<ConditionParser>()
        val userExpression = "waves are all over the place"
        every { conditionParser.parse(any(), any()) } returns null
        rsm.setConditionParser(conditionParser)

        rsm.startRuleSession(
            sessionCase,
            ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go to Bondi."))
        )

        //When
        val returnedCondition = rsm.conditionForExpression(userExpression).condition

        //Then
        verify { conditionParser.parse(userExpression, any()) }
        returnedCondition shouldBe null
    }

    @Test
    fun `should create ReasonTransformer`() = runTest {
        //Given
        val viewableCase = createViewableCase()
        val ruleService = mockk<RuleService>()
        val conditionParser = mockk<ConditionParser>()
        rsm.setConditionParser(conditionParser)
        val reason = "elevated glucose value"
        val condition = greaterThanOrEqualTo(null, glucose(), DEFAULT_GLUCOSE_VALUE)
        every { conditionParser.parse(reason, any()) } returns condition
        every {
            ruleService.conditionForExpression(
                viewableCase.case,
                reason
            )
        } returns ConditionParsingResult(condition)
        every { ruleService.cornerstoneStatus() } returns CornerstoneStatus()

        //When
        session.startConversation(viewableCase)
        rsm.startRuleSession(
            viewableCase.case,
            ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go to Bondi."))
        )
        val modelResponder = mockk<ModelResponder>(relaxed = true)
        val reasonTransformer =
            session.chatSessionManager.createReasonTransformer(viewableCase, ruleService, modelResponder)
        val reasonTransformation = reasonTransformer.transform(reason)

        //Then
        val expected = ReasonTransformation.TRANSFORMATION_MESSAGE.format("Glucose ≥ 5.1")
        reasonTransformation.message shouldBe expected
        val slot = slot<Condition>()
        verify { ruleService.addConditionToCurrentRuleSession(capture(slot)) }
        slot.captured shouldBeSameAs condition
    }


    @Test
    fun `currentRuleSessionConditionTexts should return empty set when no rule session is active`() {
        rsm.currentRuleSessionConditionTexts() shouldBe emptySet()
    }

    @Test
    fun `currentRuleSessionConditionTexts should return empty set when rule session has no conditions`() {
        val sessionCase = createCase("Case1", value = "1.0")
        rsm.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Whatever.")))
        rsm.currentRuleSessionConditionTexts() shouldBe emptySet()
    }

    @Test
    fun `currentRuleSessionConditionTexts should return condition texts after adding conditions`() {
        val sessionCase = createCase("Case1", value = "1.0")
        rsm.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Whatever.")))
        val condition = lessThanOrEqualTo(null, glucose(), 1.2)
        rsm.addConditionToCurrentRuleSession(condition)
        rsm.currentRuleSessionConditionTexts() shouldBe setOf(condition.asText())
    }

    @Test
    fun `currentRuleSessionConditionTexts should return texts of all added conditions`() {
        val sessionCase = createCase("Case1", value = "1.0")
        rsm.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Whatever.")))
        val condition1 = lessThanOrEqualTo(null, glucose(), 1.2)
        val condition2 = greaterThanOrEqualTo(null, glucose(), 0.5)
        rsm.addConditionToCurrentRuleSession(condition1)
        rsm.addConditionToCurrentRuleSession(condition2)
        rsm.currentRuleSessionConditionTexts() shouldBe setOf(condition1.asText(), condition2.asText())
    }

    @Test
    fun `should set currentDiff to Addition when starting a rule session to add a comment`() {
        //Given
        val sessionCase = createCase("Case1", value = "1.0", id = 1)
        val viewableCase = kb.viewableCase(sessionCase)
        val comment = "Go to Bondi."

        //When
        rsm.startRuleSessionToAddComment(viewableCase, comment)

        //Then
        rsm.currentDiff shouldBe Addition(comment)
    }

    @Test
    fun `should set currentDiff to Removal when starting a rule session to remove a comment`() {
        //Given
        val sessionCase = createCase("Case1", value = "1.0", id = 1)
        val viewableCase = kb.viewableCase(sessionCase)
        val comment = "Go to Bondi."
        rsm.startRuleSessionToAddComment(viewableCase, comment)
        rsm.commitCurrentRuleSession()

        //When
        rsm.startRuleSessionToRemoveComment(viewableCase, comment)

        //Then
        rsm.currentDiff shouldBe Removal(comment)
    }

    @Test
    fun `should set currentDiff to Replacement when starting a rule session to replace a comment`() {
        //Given
        val sessionCase = createCase("Case1", value = "1.0", id = 1)
        val viewableCase = kb.viewableCase(sessionCase)
        val original = "Go to Bondi."
        val replacement = "Go to Maroubra."
        rsm.startRuleSessionToAddComment(viewableCase, original)
        rsm.commitCurrentRuleSession()

        //When
        rsm.startRuleSessionToReplaceComment(viewableCase, original, replacement)

        //Then
        rsm.currentDiff shouldBe Replacement(original, replacement)
    }

    @Test
    fun `should clear currentDiff when cancelling a rule session`() {
        //Given
        val sessionCase = createCase("Case1", value = "1.0", id = 1)
        val viewableCase = kb.viewableCase(sessionCase)
        rsm.startRuleSessionToAddComment(viewableCase, "Go to Bondi.")
        rsm.currentDiff shouldNotBe null

        //When
        rsm.cancelRuleSession()

        //Then
        rsm.currentDiff shouldBe null
    }

    @Test
    fun `should clear currentDiff when committing a rule session`() {
        //Given
        val sessionCase = createCase("Case1", value = "1.0", id = 1)
        val viewableCase = kb.viewableCase(sessionCase)
        rsm.startRuleSessionToAddComment(viewableCase, "Go to Bondi.")
        rsm.currentDiff shouldNotBe null

        //When
        rsm.commitCurrentRuleSession()

        //Then
        rsm.currentDiff shouldBe null
    }

    @Test
    fun `should include the diff in the cornerstone status when cornerstones exist`() {
        //Given
        kb.addCornerstoneCase(createCase("Case2", value = "2.0"))
        val sessionCase = createCase("Case1", value = "1.0", id = 1)
        val viewableCase = kb.viewableCase(sessionCase)
        val comment = "Go to Bondi."

        //When
        val status = rsm.startRuleSessionToAddComment(viewableCase, comment)

        //Then
        status.diff shouldBe Addition(comment)
    }

    @Test
    fun `should include the diff in the cornerstone status when no cornerstones exist`() {
        //Given
        val sessionCase = createCase("Case1", value = "1.0", id = 1)
        val viewableCase = kb.viewableCase(sessionCase)
        val comment = "Go to Bondi."

        //When
        val status = rsm.startRuleSessionToAddComment(viewableCase, comment)

        //Then
        status.diff shouldBe Addition(comment)
    }

    @Test
    fun `cornerstoneStatus should have empty ruleConditions when no conditions have been added and there are no cornerstones`() {
        val sessionCase = createCase("Case1", value = "1.0")
        rsm.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go to Bondi.")))
        val ccStatus = rsm.cornerstoneStatus(null)
        ccStatus.ruleConditions shouldBe emptyList()
    }

    @Test
    fun `cornerstoneStatus should have empty ruleConditions when no conditions have been added and there are cornerstones`() {
        val cc1 = kb.addCornerstoneCase(createCase("Case1", value = "1.0"))
        kb.viewableCase(cc1)
        val sessionCase = createCase("Case2", value = "2.0")
        rsm.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go to Bondi.")))
        val ccStatus = rsm.cornerstoneStatus(null)
        ccStatus.ruleConditions shouldBe emptyList()
    }

    @Test
    fun `cornerstoneStatus should include ruleConditions after conditions have been added and there are no cornerstones`() {
        val sessionCase = createCase("Case1", value = "1.0")
        rsm.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go to Bondi.")))
        val condition = lessThanOrEqualTo(null, glucose(), 1.2)
        rsm.addConditionToCurrentRuleSession(condition)
        val ccStatus = rsm.cornerstoneStatus(null)
        ccStatus.ruleConditions shouldBe listOf(condition.asText())
    }

    @Test
    fun `cornerstoneStatus should include ruleConditions after conditions have been added and there are cornerstones`() {
        kb.addCornerstoneCase(createCase("Case1", value = "1.0"))
        val sessionCase = createCase("Case2", value = "2.0")
        rsm.startRuleSession(sessionCase, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go to Bondi.")))
        val condition = lessThanOrEqualTo(null, glucose(), 2.5) //true for session case, true for cornerstone
        rsm.addConditionToCurrentRuleSession(condition)
        val ccStatus = rsm.cornerstoneStatus(null)
        ccStatus.ruleConditions shouldBe listOf(condition.asText())
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
        val newKb = KB(persistentKB)
        session = KBSession(newKb, webSocketManager)
        rsm = session.ruleSessionManager
        return newKb
    }
}