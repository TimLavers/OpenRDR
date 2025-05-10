package io.rippledown.server

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.rippledown.CaseTestUtils
import io.rippledown.kb.ConditionParser
import io.rippledown.kb.KB
import io.rippledown.kb.KBManager
import io.rippledown.model.beSameAs
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.greaterThanOrEqualTo
import io.rippledown.model.condition.isCondition
import io.rippledown.model.rule.ChangeTreeToAddConclusion
import io.rippledown.model.rule.UndoRuleDescription
import io.rippledown.persistence.inmemory.InMemoryPersistenceProvider
import io.rippledown.supplyCaseFromFile
import io.rippledown.util.EntityRetrieval
import org.apache.commons.io.FileUtils
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
        kb.setConditionParser(conditionParser)
        endpoint = KBEndpoint(kb, rootDir)
        FileUtils.cleanDirectory(endpoint.casesDir)
        FileUtils.cleanDirectory(endpoint.interpretationsDir)
    }

    @AfterTest
    fun cleanup() {
        kbManager.deleteKB(endpoint.kbName())
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
        val kb = mockk<KB>(relaxed = true)
        every { kb.descriptionOfMostRecentRule() } returns undoDescription
        KBEndpoint(kb, File("kbe")).descriptionOfMostRecentRule() shouldBe undoDescription
    }

    @Test
    fun undoLastRuleTest() {
        val kb = mockk<KB>(relaxed = true)
        KBEndpoint(kb, File("kbe")).undoLastRule()
        verify { kb.undoLastRuleSession() }
    }

    @Test
    fun `should delegate parsing a condition to the KB`() {
        // Given
        val kb = mockk<KB>(relaxed = true)
        val condition = mockk<Condition>(relaxed = true)
        every { kb.conditionForExpression(any(), any()) } returns condition
        val endpoint = KBEndpoint(kb, File("kbe"))
        val userExpression = "TSH is depressed"
        val attributeNames = listOf("TSH")

        // When
        val parsed = endpoint.conditionForExpression(userExpression, attributeNames)

        // Then
        verify { kb.conditionForExpression(userExpression, attributeNames) }
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
        endpoint.kb.startRuleSession(retrieved, ChangeTreeToAddConclusion(conclusion))
        val abc = retrieved.getAttribute("ABC")
        endpoint.kb.addConditionToCurrentRuleSession(greaterThanOrEqualTo(null, abc, 5.0))
        endpoint.kb.commitCurrentRuleSession()
        val retrievedAgain = endpoint.case(caseId.id!!)
        retrievedAgain.interpretation.conclusions() shouldContainExactly setOf(conclusion)
    }

    @Test
    fun `should retrieve the cached case when using InMemoryPersistenceProvider`() {
        val id = supplyCaseFromFile("Case1", endpoint).caseId.id!!
        val retrieved = endpoint.case(id)
        val retrievedAgain = endpoint.case(id)
        retrievedAgain shouldBeSameInstanceAs retrieved
    }

    @Test
    fun `should return condition hints for a case`() {
        val id = supplyCaseFromFile("Case1", endpoint).caseId.id!!
        val case = endpoint.case(id)
        val hintConditions = endpoint.conditionHintsForCase(id)
        hintConditions shouldBe endpoint.kb.conditionHintsForCase(case)
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
        endpoint.kb.startRuleSession(retrieved.case, ChangeTreeToAddConclusion(conclusion))
        val abc = retrieved.case.getAttribute("ABC")
        endpoint.kb.addConditionToCurrentRuleSession(greaterThanOrEqualTo(null, abc, 5.0))
        endpoint.kb.commitCurrentRuleSession()
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
    fun waitingCasesInfo() {
        FileUtils.cleanDirectory(endpoint.casesDir)
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
    fun kbName() {
        endpoint.kbName().name shouldBe kbName
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
            endpoint.kb.conflictingCasesInCurrentRuleSession()
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
        val serverApplication = ServerApplication(persistenceProvider)
        serverApplication.importKBFromZip(exported.readBytes())
        endpoint.kb.allCornerstoneCases().size shouldBe 1
        endpoint.kb.ruleTree.size() shouldBe 2
        val rule = endpoint.kb.ruleTree.root.childRules().single()
        val conditions = rule.conditions
        conditions.size shouldBe 1
        conditions.single().sameAs(tshCondition) shouldBe true
        rule.conclusion shouldBe conclusion1
    }
}