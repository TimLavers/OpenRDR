package io.rippledown.server

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.rippledown.CaseTestUtils
import io.rippledown.kb.*
import io.rippledown.model.Conclusion
import io.rippledown.model.RDRCase
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.ConditionParsingResult
import io.rippledown.model.condition.greaterThanOrEqualTo
import io.rippledown.model.condition.isCondition
import io.rippledown.model.diff.Addition
import io.rippledown.model.diff.Removal
import io.rippledown.model.diff.Replacement
import io.rippledown.model.rule.*
import io.rippledown.persistence.inmemory.InMemoryPersistenceProvider
import io.rippledown.supplyCaseFromFile
import io.rippledown.toJsonString
import io.rippledown.util.EntityRetrieval
import io.rippledown.utils.beSameAs
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class KBEndpointTest {

    private val kbName = "KBEndpointTest"
    private val persistenceProvider = InMemoryPersistenceProvider()
    private val kbManager = KBManager(persistenceProvider)

    private lateinit var endpoint: KBEndpoint
    private lateinit var conditionParser: ConditionParser

    @BeforeTest
    fun setup() {
        val rootDir = File("kbe")
        val kbInfo = kbManager.createKB(kbName)
        val kb = (kbManager.openKB(kbInfo.id) as EntityRetrieval.Success<KB>).entity
        conditionParser = mockk()
        val session = KBSession(kb)
        session.ruleSessionManager.setConditionParser(conditionParser)
        endpoint = KBEndpoint(session)
//        FileUtils.cleanDirectory(endpoint.casesDir)
//        FileUtils.cleanDirectory(endpoint.interpretationsDir)
    }

    @AfterTest
    fun cleanup() {
        kbManager.deleteKB(endpoint.kbInfo())
    }

    @Test
    fun descriptionTest() {
        endpoint.description() shouldBe ""
        val newDescription = "lots of rules"
        endpoint.setDescription(newDescription)
        endpoint.description() shouldBe newDescription
    }

    @Test
    fun descriptionOfMostRecentRuleTest() {
        val undoDescription = UndoRuleDescription("Cool rule!", false)
        val rsm = mockk<RuleSessionManager>()
        every { rsm.descriptionOfMostRecentRule() } returns undoDescription
        val session = mockk<KBSession>()
        every { session.ruleSessionManager } returns rsm
        KBEndpoint(session).descriptionOfMostRecentRule() shouldBe undoDescription
    }

    @Test
    fun undoLastRuleTest() {
        val rsm = mockk<RuleSessionManager>()
        val session = mockk<KBSession>()
        every { session.ruleSessionManager } returns rsm
        KBEndpoint(session).undoLastRule()
        verify { rsm.undoLastRuleSession() }
    }

    @Test
    fun `should delegate parsing a condition to the RuleSessionManager`() {
        // Given
        val rsm = mockk<RuleSessionManager>()
        val condition = mockk<Condition>()
        every { rsm.conditionForExpression(any<String>()) } returns ConditionParsingResult(condition)
        val session = mockk<KBSession>()
        every { session.ruleSessionManager } returns rsm
        val endpoint = KBEndpoint(session)
        val userExpression = "TSH is depressed"

        // When
        val parsed = endpoint.conditionForExpression(userExpression).condition

        // Then
        verify { rsm.conditionForExpression(userExpression) }
        parsed shouldBe condition
    }

    @Test
    fun case() {
        val caseId = supplyCaseFromFile("Case1", endpoint).caseId
        val retrieved = endpoint.case(caseId.id!!)
        assertEquals(retrieved.name, "Case1")
        assertEquals(retrieved.getLatest("TSH")!!.value.text, "0.667")
        assertEquals(retrieved.getLatest("ABC")!!.value.text, "6.7")
        assertEquals(2, retrieved.data.size)
        // No rules added.
        retrieved.interpretation.conclusions().size shouldBe 0
        // Add a rule.
        val conclusion = endpoint.kb.conclusionManager.getOrCreate("ABC ok.")
        endpoint.session.ruleSessionManager.startRuleSession(retrieved, ChangeTreeToAddConclusion(conclusion))
        val abc = retrieved.getAttribute("ABC")
        endpoint.session.ruleSessionManager.addConditionToCurrentRuleSession(greaterThanOrEqualTo(null, abc, 5.0))
        endpoint.session.ruleSessionManager.commitCurrentRuleSession()
        val retrievedAgain = endpoint.case(caseId.id!!)
        retrievedAgain.interpretation.conclusions() shouldContainExactly setOf(conclusion)
    }

    @Test
    fun `should retrieve a copy of the cached case when using InMemoryPersistenceProvider`() {
        //Given
        val id = supplyCaseFromFile("Case1", endpoint).caseId.id!!
        val retrieved = endpoint.case(id)

        //When
        val retrievedAgain = endpoint.case(id)

        //Then
        retrievedAgain shouldNotBeSameInstanceAs retrieved
        retrievedAgain.toJsonString() shouldBe retrieved.toJsonString()
    }

    @Test
    fun `should return condition hints for a case`() {
        val id = supplyCaseFromFile("Case1", endpoint).caseId.id!!
        val case = endpoint.case(id)
        val hintConditions = endpoint.conditionHintsForCase(id)
        hintConditions shouldBe endpoint.session.ruleSessionManager.conditionHintsForCase(case)
    }

    @Test
    fun processCase() {
        endpoint.kb.allProcessedCases() shouldBe emptyList()
        val externalCase1 = CaseTestUtils.getCase("Case1")
        val case1 = endpoint.processCase(externalCase1)
        case1.name shouldBe externalCase1.name
        val retrieved1 = endpoint.kb.getProcessedCase(case1.caseId.id!!)!!
        retrieved1.caseId shouldBe case1.caseId
        // Supply it again.
        val case2 = endpoint.processCase(externalCase1)
        val retrieved2 = endpoint.kb.getProcessedCase(case2.caseId.id!!)!!
        endpoint.kb.allProcessedCases() shouldBe listOf(retrieved1, retrieved2)
    }

    @Test
    fun `addCornerstoneCase should store the case as a cornerstone`() {
        endpoint.kb.allCornerstoneCases() shouldHaveSize 0
        val externalCase = CaseTestUtils.getCase("Case1")
        val stored = endpoint.addCornerstoneCase(externalCase)
        stored.name shouldBe externalCase.name
        stored.caseId.id shouldNotBe null
        endpoint.kb.allCornerstoneCases() shouldHaveSize 1
        endpoint.kb.allCornerstoneCases().first().name shouldBe externalCase.name
    }

    @Test
    fun `addCornerstoneCase should not create a processed case`() {
        val externalCase = CaseTestUtils.getCase("Case1")
        endpoint.addCornerstoneCase(externalCase)
        endpoint.kb.allProcessedCases() shouldHaveSize 0
        endpoint.kb.allCornerstoneCases() shouldHaveSize 1
    }

    @Test
    fun `addCornerstoneCase should be retrievable by id`() {
        val externalCase = CaseTestUtils.getCase("Case1")
        val stored = endpoint.addCornerstoneCase(externalCase)
        val retrieved = endpoint.kb.getCase(stored.caseId.id!!)
        retrieved shouldNotBe null
        retrieved!!.name shouldBe externalCase.name
    }

    @Test
    fun `addCornerstoneCase should allow multiple cornerstone cases`() {
        endpoint.addCornerstoneCase(CaseTestUtils.getCase("Case1"))
        endpoint.addCornerstoneCase(CaseTestUtils.getCase("Case2"))
        endpoint.kb.allCornerstoneCases() shouldHaveSize 2
    }

    @Test
    fun `addCornerstoneCase should not affect existing processed cases`() {
        val processed = endpoint.processCase(CaseTestUtils.getCase("Case1"))
        endpoint.addCornerstoneCase(CaseTestUtils.getCase("Case2"))
        endpoint.kb.allProcessedCases() shouldHaveSize 1
        endpoint.kb.allProcessedCases().first().name shouldBe "Case1"
        endpoint.kb.allCornerstoneCases() shouldHaveSize 1
        endpoint.kb.allCornerstoneCases().first().name shouldBe "Case2"
    }

    @Test
    fun deleteProcessedCase() {
        val externalCase1 = CaseTestUtils.getCase("Case1")
        val case1 = endpoint.processCase(externalCase1)
        val externalCase2 = CaseTestUtils.getCase("Case2")
        val case2 = endpoint.processCase(externalCase2)
        endpoint.kb.allProcessedCases() shouldBe listOf(case1, case2)
        endpoint.deleteCase(case2.name)
        endpoint.kb.allProcessedCases() shouldBe listOf(case1)
    }

    @Test
    fun viewableCase() {
        val id = supplyCaseFromFile("Case1", endpoint).caseId.id!!

        val retrieved = endpoint.viewableCase(id)
        assertEquals(retrieved.name, "Case1")
        assertEquals(retrieved.case.getLatest("ABC")!!.value.text, "6.7")
        assertEquals(2, retrieved.attributes().size)
        // No rules added.
        retrieved.viewableInterpretation.interpretation.conclusions().size shouldBe 0
        // Add a rule.
        val conclusion = endpoint.kb.conclusionManager.getOrCreate("ABC ok.")
        endpoint.session.ruleSessionManager.startRuleSession(retrieved.case, ChangeTreeToAddConclusion(conclusion))
        val abc = retrieved.case.getAttribute("ABC")
        endpoint.session.ruleSessionManager.addConditionToCurrentRuleSession(greaterThanOrEqualTo(null, abc, 5.0))
        endpoint.session.ruleSessionManager.commitCurrentRuleSession()
        val retrievedAgain = endpoint.viewableCase(id)
        retrievedAgain.viewableInterpretation.interpretation.conclusions() shouldContainExactly setOf(conclusion)
    }

    @Test
    fun getOrCreateAttribute() {
        endpoint.kb.attributeManager.all() shouldBe emptySet()
        val attribute = endpoint.getOrCreateAttribute("stuff")
        endpoint.kb.attributeManager.all() shouldBe setOf(attribute)
    }

    @Test
    fun getOrCreateConclusion() {
        endpoint.kb.conclusionManager.all() shouldBe emptySet()
        val text = "It is rainy."
        val conclusion = endpoint.getOrCreateConclusion(text)
        conclusion.text shouldBe text
        endpoint.kb.conclusionManager.all() shouldBe setOf(conclusion)
    }

    @Test
    fun getOrCreateCondition() {
        endpoint.kb.conditionManager.all() shouldBe emptySet()
        val attribute = endpoint.getOrCreateAttribute("stuff")
        val prototype = isCondition(null, attribute, "Whatever")
        val created = endpoint.getOrCreateCondition(prototype)
        created should beSameAs(prototype)
        endpoint.kb.conditionManager.all() shouldBe setOf(created)
    }

    @Test
    fun moveAttributeJustBelow() {
        val id = supplyCaseFromFile("Case1", endpoint).caseId.id!!
        val retrieved = endpoint.viewableCase(id)
        val attributesBefore = retrieved.attributes()
        attributesBefore.size shouldBe 2 // sanity
        endpoint.moveAttribute(attributesBefore[0].id, attributesBefore[1].id)
        // Get the case again and check that the order has been applied.
        val retrievedAfter = endpoint.viewableCase(id)
        retrievedAfter.attributes()[0] shouldBe attributesBefore[1]
        retrievedAfter.attributes()[1] shouldBe attributesBefore[0]
    }

    @Test
    fun setAttributesOrder() {
        val id = supplyCaseFromFile("Case5", endpoint).caseId.id!!
        val retrieved = endpoint.viewableCase(id)
        val attributesBefore = retrieved.attributes()
        attributesBefore.size shouldBe 4 // sanity
        val reordered = attributesBefore.reversed()
        endpoint.setAttributeOrder(reordered)
        // Get the case again and check that the order has been applied.
        val retrievedAfter = endpoint.viewableCase(id)
        retrievedAfter.attributes()[0] shouldBe attributesBefore[3]
        retrievedAfter.attributes()[1] shouldBe attributesBefore[2]
        retrievedAfter.attributes()[2] shouldBe attributesBefore[1]
        retrievedAfter.attributes()[3] shouldBe attributesBefore[0]
    }

    @Test
    fun setAttributesOrderDuringRuleSession() {
        // Get a case with multiple attributes.
        val case5Id = supplyCaseFromFile("Case5", endpoint).caseId.id!!
        val retrieved = endpoint.viewableCase(case5Id)
        val attributesBefore = retrieved.attributes()
        attributesBefore.size shouldBe 4 // sanity

        // Get another case, with which to start a rule session.
        val case1Id = supplyCaseFromFile("Case1", endpoint).caseId.id!!
        endpoint.case(case1Id).interpretation.conclusions() shouldBe emptySet()
        endpoint.startRuleSessionToAddConclusion(case1Id, endpoint.kb.conclusionManager.getOrCreate("Whatever"))

        // Re-order the attributes in the first case.
        val reordered = attributesBefore.reversed()
        endpoint.setAttributeOrder(reordered)
        // Get the case again and check that the order has been applied.
        val retrievedAfter = endpoint.viewableCase(case5Id)
        retrievedAfter.attributes()[0] shouldBe attributesBefore[3]
        retrievedAfter.attributes()[1] shouldBe attributesBefore[2]
        retrievedAfter.attributes()[2] shouldBe attributesBefore[1]
        retrievedAfter.attributes()[3] shouldBe attributesBefore[0]

        // Commit the rule session.
        endpoint.commitCurrentRuleSession()
        endpoint.case(case1Id).interpretation.conclusionTexts() shouldBe setOf("Whatever")

        // Get the case again and check that the order has been applied.
        val retrievedAfterRuleSession = endpoint.viewableCase(case5Id)
        retrievedAfterRuleSession.attributes()[0] shouldBe attributesBefore[3]
        retrievedAfterRuleSession.attributes()[1] shouldBe attributesBefore[2]
        retrievedAfterRuleSession.attributes()[2] shouldBe attributesBefore[1]
        retrievedAfterRuleSession.attributes()[3] shouldBe attributesBefore[0]
    }

    @Test
    fun setAttributesOrderOfRuleSessionCase() {
        // Get a case with which to start a rule session.
        val caseId = supplyCaseFromFile("Case5", endpoint).caseId.id!!
        val viewableCase5 = endpoint.viewableCase(caseId)
        val attributesBefore = viewableCase5.attributes()
        attributesBefore.size shouldBe 4

        viewableCase5.case.interpretation.conclusions() shouldBe emptySet()
        endpoint.startRuleSessionToAddConclusion(caseId, endpoint.kb.conclusionManager.getOrCreate("Whatever"))

        endpoint.moveAttribute(attributesBefore[0].id, attributesBefore[3].id)
        val attributesAfterMove = endpoint.viewableCase(caseId).attributes()
        attributesAfterMove.size shouldBe 4
        attributesAfterMove[0] shouldBe attributesBefore[1]
        attributesAfterMove[1] shouldBe attributesBefore[2]
        attributesAfterMove[2] shouldBe attributesBefore[3]
        attributesAfterMove[3] shouldBe attributesBefore[0]

        // Commit the rule session.
        endpoint.commitCurrentRuleSession()
        endpoint.viewableCase(caseId).case.interpretation.conclusionTexts() shouldBe setOf("Whatever")

        val attributesAfterRule = endpoint.viewableCase(caseId).attributes()
        attributesAfterRule.size shouldBe 4
        attributesAfterRule[0] shouldBe attributesBefore[1]
        attributesAfterRule[1] shouldBe attributesBefore[2]
        attributesAfterRule[2] shouldBe attributesBefore[3]
        attributesAfterRule[3] shouldBe attributesBefore[0]
    }

    @Test
    fun waitingCasesInfo() {
//        FileUtils.cleanDirectory(endpoint.casesDir)
        assertEquals(endpoint.waitingCasesInfo().kbName, endpoint.kb.kbInfo.name)
        assertEquals(endpoint.waitingCasesInfo().count, 0)

        // Provide some cases.
        supplyCaseFromFile("Case2", endpoint)
        val ci1 = endpoint.waitingCasesInfo()
        assertEquals(ci1.count, 1)
        assertEquals(ci1.caseIds[0].name, "Case2")

        supplyCaseFromFile("Case3", endpoint)
        val ci2 = endpoint.waitingCasesInfo()
        assertEquals(ci2.count, 2)
        assertEquals(ci2.caseIds[0].name, "Case2")
        assertEquals(ci2.caseIds[1].name, "Case3")
    }

    @Test
    fun `waitingCasesInfo should include cornerstone cases`() {
        val id = supplyCaseFromFile("Case1", endpoint).caseId.id!!
        endpoint.waitingCasesInfo().cornerstoneCaseIds shouldHaveSize 0

        val conclusion = endpoint.kb.conclusionManager.getOrCreate("Whatever")
        endpoint.startRuleSessionToAddConclusion(id, conclusion)
        endpoint.commitCurrentRuleSession()

        val info = endpoint.waitingCasesInfo()
        info.cornerstoneCaseIds shouldHaveSize 1
        info.cornerstoneCaseIds[0].name shouldBe "Case1"
        info.count shouldBe info.caseIds.size + info.cornerstoneCaseIds.size
    }

    @Test
    fun kbName() {
        endpoint.kbInfo().name shouldBe kbName
    }

    @Test
    fun startRuleSessionToAddConclusion() {
        val id = supplyCaseFromFile("Case1", endpoint).caseId.id!!
        endpoint.case(id).interpretation.conclusions() shouldBe emptySet()
        endpoint.startRuleSessionToAddConclusion(id, endpoint.kb.conclusionManager.getOrCreate("Whatever"))
        endpoint.commitCurrentRuleSession()
        endpoint.case(id).interpretation.conclusionTexts() shouldBe setOf("Whatever")
    }

    @Test
    fun startSessionToRemoveConclusion() {
        val caseId = supplyCaseFromFile("Case1", endpoint).caseId
        val id = caseId.id!!
        val conclusion1 = endpoint.kb.conclusionManager.getOrCreate("Whatever")
        endpoint.startRuleSessionToAddConclusion(id, conclusion1)
        endpoint.commitCurrentRuleSession()
        endpoint.case(id).interpretation.conclusionTexts() shouldBe setOf(conclusion1.text)
        endpoint.startRuleSessionToRemoveConclusion(id, conclusion1)
        endpoint.commitCurrentRuleSession()
        endpoint.case(id).interpretation.conclusionTexts() shouldBe emptySet()
    }

    @Test
    fun startSessionToReplaceConclusion() {
        val caseId = supplyCaseFromFile("Case1", endpoint).caseId
        val id = caseId.id!!
        val conclusion1 = endpoint.kb.conclusionManager.getOrCreate("Whatever")
        endpoint.startRuleSessionToAddConclusion(id, conclusion1)
        endpoint.commitCurrentRuleSession()
        endpoint.case(id).interpretation.conclusionTexts() shouldBe setOf(conclusion1.text)
        val conclusion2 = endpoint.kb.conclusionManager.getOrCreate("Blah")
        endpoint.startRuleSessionToReplaceConclusion(id, conclusion1, conclusion2)
        endpoint.commitCurrentRuleSession()
        endpoint.case(id).interpretation.conclusionTexts() shouldBe setOf(conclusion2.text)
    }

    @Test
    fun `should be able to cancel a rule session after it is started`() {
        //Given
        val id = supplyCaseFromFile("Case1", endpoint).caseId.id!!
        val conclusion = endpoint.kb.conclusionManager.getOrCreate("Whatever")
        endpoint.startRuleSessionToAddConclusion(id, conclusion)

        //When
        endpoint.cancelRuleSession()

        //Then
        shouldThrow<IllegalStateException> {
            endpoint.session.ruleSessionManager.conflictingCasesInCurrentRuleSession()
        }.message shouldBe "Rule session not started."
    }

    @Test
    fun `After committing a rule the session case should be a cornerstone`() {
        val id = supplyCaseFromFile("Case1", endpoint).caseId.id!!
        val conclusion = endpoint.kb.conclusionManager.getOrCreate("Whatever")
        endpoint.kb.allCornerstoneCases() shouldHaveSize 0
        endpoint.startRuleSessionToAddConclusion(id, conclusion)
        endpoint.commitCurrentRuleSession()
        endpoint.kb.allCornerstoneCases() shouldHaveSize 1
    }

    @Test
    fun `Should select a cornerstone by its index`() {
        val id1 = supplyCaseFromFile("Case1", endpoint).caseId.id!!
        val id2 = supplyCaseFromFile("Case2", endpoint).caseId.id!!
        val id3 = supplyCaseFromFile("Case3", endpoint).caseId.id!!
        val conclusion1 = endpoint.kb.conclusionManager.getOrCreate("Whatever 1")
        val conclusion2 = endpoint.kb.conclusionManager.getOrCreate("Whatever 2")
        val conclusion3 = endpoint.kb.conclusionManager.getOrCreate("Whatever 3")
        with(endpoint) {
            kb.allCornerstoneCases() shouldHaveSize 0
            startRuleSessionToAddConclusion(id1, conclusion1)
            commitCurrentRuleSession()
            kb.allCornerstoneCases() shouldHaveSize 1

            val viewableCase1 = endpoint.viewableCase(kb.allCornerstoneCases().first().id!!)
            startRuleSessionToAddConclusion(id2, conclusion2)
            selectCornerstone(0).cornerstoneToReview shouldBe viewableCase1
            commitCurrentRuleSession()
            kb.allCornerstoneCases() shouldHaveSize 2

            val viewableCase2 = endpoint.viewableCase(kb.allCornerstoneCases()[1].id!!)
            startRuleSessionToAddConclusion(id3, conclusion3)
            selectCornerstone(1).cornerstoneToReview shouldBe viewableCase2
        }
    }

    @Test
    fun exportKBToZip() {
        // Add a case and a rule to the KB.
        val id = supplyCaseFromFile("Case1", endpoint).caseId.id!!
        val conclusion1 = endpoint.kb.conclusionManager.getOrCreate("Whatever")
        val tsh = endpoint.kb.attributeManager.getOrCreate("TSH")
        endpoint.startRuleSessionToAddConclusion(id, conclusion1)
        val tshCondition = greaterThanOrEqualTo(null, tsh, 0.6)
        endpoint.addConditionToCurrentRuleBuildingSession(tshCondition)
        endpoint.commitCurrentRuleSession()
        endpoint.case(id).interpretation.conclusionTexts() shouldBe setOf(conclusion1.text)

        // Get the exported KB.
        val exported = endpoint.exportKBToZip()
        exported.name shouldBe "${endpoint.kb.kbInfo}.zip"

        // Import the exported KB.
        val persistenceProvider = InMemoryPersistenceProvider()
        val serverApplication = ServerApplication(persistenceProvider, mockk())
        serverApplication.importKBFromZip(exported.readBytes())
        endpoint.kb.allCornerstoneCases().size shouldBe 1
        endpoint.kb.ruleTree.size() shouldBe 2
        val rule = endpoint.kb.ruleTree.root.childRules().single()
        val conditions = rule.conditions
        conditions.size shouldBe 1
        conditions.single().sameAs(tshCondition) shouldBe true
        rule.conclusion shouldBe conclusion1
    }

    @Test
    fun `should set currentDiff on ruleSessionManager when starting a rule session via SessionStartRequest`() {
        //Given
        val id = supplyCaseFromFile("Case1", endpoint).caseId.id!!
        val diff = Addition("Go to Bondi.")
        val sessionStartRequest = SessionStartRequest(id, diff)

        //When
        endpoint.startRuleSession(sessionStartRequest)

        //Then
        endpoint.session.ruleSessionManager.currentDiff shouldBe diff
    }

    @Test
    fun `should set currentDiff to Replacement when starting a rule session via SessionStartRequest`() {
        //Given
        val id = supplyCaseFromFile("Case1", endpoint).caseId.id!!
        val conclusion = endpoint.kb.conclusionManager.getOrCreate("Go to Bondi.")
        endpoint.startRuleSessionToAddConclusion(id, conclusion)
        endpoint.commitCurrentRuleSession()
        val diff = Replacement("Go to Bondi.", "Go to Maroubra.")
        val sessionStartRequest = SessionStartRequest(id, diff)

        //When
        endpoint.startRuleSession(sessionStartRequest)

        //Then
        endpoint.session.ruleSessionManager.currentDiff shouldBe diff
    }

    @Test
    fun `should include the diff in the cornerstone status returned by startRuleSession`() {
        //Given
        val id = supplyCaseFromFile("Case1", endpoint).caseId.id!!
        val diff = Addition("Go to Bondi.")
        val sessionStartRequest = SessionStartRequest(id, diff)

        //When
        val status = endpoint.startRuleSession(sessionStartRequest)

        //Then
        status.diff shouldBe diff
    }

    @Test
    fun `startRuleSessionToAddConclusion should call startRuleSession`() {
        // Given
        val kb = mockk<KB>()
        val rsm = mockk<RuleSessionManager>()
        val case = mockk<RDRCase>()
        val conclusion = mockk<Conclusion>()
        every { kb.getCase(any()) } returns case
        every { kb.interpret(any()) } returns mockk()
        val session = mockk<KBSession>()
        every { session.kb } returns kb
        every { session.ruleSessionManager } returns rsm
        val endpoint = KBEndpoint(session)

        // When
        endpoint.startRuleSessionToAddConclusion(1L, conclusion)

        // Then
        verify { rsm.startRuleSession(case, any<ChangeTreeToAddConclusion>()) }
    }

    @Test
    fun `startRuleSessionToRemoveConclusion should call startRuleSession`() {
        // Given
        val kb = mockk<KB>()
        val rsm = mockk<RuleSessionManager>()
        val case = mockk<RDRCase>()
        val conclusion = mockk<Conclusion>()
        every { kb.getCase(any()) } returns case
        every { kb.interpret(any()) } returns mockk()
        val session = mockk<KBSession>()
        every { session.kb } returns kb
        every { session.ruleSessionManager } returns rsm
        val endpoint = KBEndpoint(session)

        // When
        endpoint.startRuleSessionToRemoveConclusion(1L, conclusion)

        // Then
        verify { rsm.startRuleSession(case, any<ChangeTreeToRemoveConclusion>()) }
    }

    @Test
    fun `startRuleSessionToReplaceConclusion should call startRuleSession`() {
        // Given
        val kb = mockk<KB>()
        val rsm = mockk<RuleSessionManager>()
        val case = mockk<RDRCase>()
        val toGo = mockk<Conclusion>()
        val replacement = mockk<Conclusion>()
        every { kb.getCase(any()) } returns case
        every { kb.interpret(any()) } returns mockk()
        val session = mockk<KBSession>()
        every { session.kb } returns kb
        every { session.ruleSessionManager } returns rsm
        val endpoint = KBEndpoint(session)

        // When
        endpoint.startRuleSessionToReplaceConclusion(1L, toGo, replacement)

        // Then
        verify { rsm.startRuleSession(case, any<ChangeTreeToReplaceConclusion>()) }
    }

    @Test
    fun `buildRule should add a comment with Is conditions`() {
        // Given
        val case1 = supplyCaseFromFile("Case1", endpoint)
        val request = BuildRuleRequest(
            caseName = "Case1",
            diff = Addition("TSH ok."),
            conditions = listOf("""TSH is "0.667"""")
        )

        // When
        endpoint.buildRule(request)

        // Then
        endpoint.case(case1.caseId.id!!).interpretation.conclusionTexts() shouldBe setOf("TSH ok.")
    }

    @Test
    fun `buildRule should remove a comment with Is conditions`() {
        // Given
        val case1 = supplyCaseFromFile("Case1", endpoint)
        val id = case1.caseId.id!!
        // First add a comment
        endpoint.buildRule(
            BuildRuleRequest("Case1", Addition("TSH ok."), listOf("""TSH is "0.667""""))
        )
        endpoint.case(id).interpretation.conclusionTexts() shouldBe setOf("TSH ok.")

        // When - remove it
        endpoint.buildRule(
            BuildRuleRequest("Case1", Removal("TSH ok."), listOf("""ABC is "6.7""""))
        )

        // Then
        endpoint.case(id).interpretation.conclusionTexts() shouldBe emptySet()
    }

    @Test
    fun `buildRule should replace a comment with Is conditions`() {
        // Given
        val case1 = supplyCaseFromFile("Case1", endpoint)
        val id = case1.caseId.id!!
        // First add a comment
        endpoint.buildRule(
            BuildRuleRequest("Case1", Addition("TSH ok."), listOf("""TSH is "0.667""""))
        )
        endpoint.case(id).interpretation.conclusionTexts() shouldBe setOf("TSH ok.")

        // When - replace it
        endpoint.buildRule(
            BuildRuleRequest(
                "Case1",
                Replacement("TSH ok.", "TSH normal."),
                listOf("""ABC is "6.7"""")
            )
        )

        // Then
        endpoint.case(id).interpretation.conclusionTexts() shouldBe setOf("TSH normal.")
    }

    @Test
    fun `buildRule should handle multiple conditions`() {
        // Given
        val case1 = supplyCaseFromFile("Case1", endpoint)
        val request = BuildRuleRequest(
            caseName = "Case1",
            diff = Addition("Both ok."),
            conditions = listOf(
                """TSH is "0.667"""",
                """ABC is "6.7""""
            )
        )

        // When
        endpoint.buildRule(request)

        // Then
        endpoint.case(case1.caseId.id!!).interpretation.conclusionTexts() shouldBe setOf("Both ok.")
    }
}