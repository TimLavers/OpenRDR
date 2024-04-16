package io.rippledown.server

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.rippledown.CaseTestUtils
import io.rippledown.kb.KB
import io.rippledown.kb.KBManager
import io.rippledown.model.beSameAs
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.greaterThanOrEqualTo
import io.rippledown.model.condition.hasCurrentValue
import io.rippledown.model.condition.isCondition
import io.rippledown.model.diff.Addition
import io.rippledown.model.diff.DiffList
import io.rippledown.model.diff.Unchanged
import io.rippledown.model.interpretationview.ViewableInterpretation
import io.rippledown.model.rule.ChangeTreeToAddConclusion
import io.rippledown.model.rule.CornerstoneStatus
import io.rippledown.persistence.inmemory.InMemoryPersistenceProvider
import io.rippledown.supplyCaseFromFile
import io.rippledown.util.EntityRetrieval
import io.rippledown.util.shouldContainSameAs
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

    @BeforeTest
    fun setup() {
        val rootDir = File("kbe")
        val kbInfo = kbManager.createKB(kbName)
        val kb = (kbManager.openKB(kbInfo.id) as EntityRetrieval.Success<KB>).entity
        endpoint = KBEndpoint(kb, rootDir)
        FileUtils.cleanDirectory(endpoint.casesDir)
        FileUtils.cleanDirectory(endpoint.interpretationsDir)
    }

    @AfterTest
    fun cleanup() {
        kbManager.deleteKB(endpoint.kbName())
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
    fun `should set the interpretation's DiffList to be empty when retrieving a case with a blank interpretation`() {
        val id = supplyCaseFromFile("Case1", endpoint).caseId.id!!

        val case = endpoint.viewableCase(id)
        case.verifiedText() shouldBe null
        case.textGivenByRules() shouldBe ""
        case.diffList() shouldBe DiffList(emptyList())
    }

    @Test
    fun `the DiffList should have an unchanged fragment when retrieving a case with a non-blank interpretation`() {
        val id = supplyCaseFromFile("Case1", endpoint).caseId.id!!
        val case = endpoint.case(id)
        val text = "ABC ok."
        val conclusion = endpoint.kb.conclusionManager.getOrCreate(text)
        endpoint.kb.startRuleSession(case, ChangeTreeToAddConclusion(conclusion))
        endpoint.kb.commitCurrentRuleSession()

        with(endpoint.viewableCase(id)) {
            verifiedText() shouldBe null
            textGivenByRules() shouldBe text
            diffList() shouldBe DiffList(listOf(Unchanged(text)))
        }
    }

    @Test
    fun `should update a case's verified interpretation and return an interpretation containing a DiffList`() {
        val caseId = supplyCaseFromFile("Case1", endpoint).caseId
        val id = caseId.id!!
        val original = endpoint.viewableCase(id)
        original.verifiedText() shouldBe null
        original.diffList() shouldBe DiffList()

        val verified = "Verified."
        original.viewableInterpretation.apply { verifiedText = verified }
        val returnedCase = endpoint.saveInterpretation(original)
        returnedCase.id shouldBe original.id
        returnedCase.verifiedText() shouldBe verified
        returnedCase.diffList() shouldBe DiffList(listOf(Addition(verified)))
    }

    @Test
    fun `should return condition hints for a case`() {
        val id = supplyCaseFromFile("Case1", endpoint).caseId.id!!
        val case = endpoint.case(id)
        val expectedConditions: List<Condition> = case.attributes
            .filter { attribute ->
                case.getLatest(attribute) != null
            }.map { attribute ->
                hasCurrentValue(1, attribute)
            }
        val hintConditions = endpoint.conditionHintsForCase(id).conditions.toSet()

        hintConditions.size shouldBe expectedConditions.size
        expectedConditions.forEach {
            hintConditions shouldContainSameAs it
        }
    }

    @Test
    fun `condition hints should all have an id`() {
        val id = supplyCaseFromFile("Case1", endpoint).caseId.id!!

        endpoint.conditionHintsForCase(id).conditions.toSet().forEach { condition ->
            condition.id shouldNotBe null
        }
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
    fun `when saving a viewableCase, an interpretation with diffs should be returned`() {
        val id = supplyCaseFromFile("Case1", endpoint).caseId.id!!
        val conclusion = endpoint.kb.conclusionManager.getOrCreate("Go to Bondi.")
        with(endpoint) {
            startRuleSessionToAddConclusion(id, conclusion)
            commitCurrentRuleSession()
            val case = case(id)
            case.interpretation.conclusionTexts() shouldBe setOf(conclusion.text)

            val verifiedInterpretation = ViewableInterpretation(case.interpretation).apply {
                verifiedText = "Go to Bondi. Bring 2 sets of flippers. And bring sunscreen."
            }
            val viewableCase = viewableCase(id).apply {
                viewableInterpretation = verifiedInterpretation
            }
            val caseReturned = endpoint.saveInterpretation(viewableCase)
            caseReturned.diffList() shouldBe DiffList(
                listOf(
                    Unchanged("Go to Bondi."),
                    Addition("Bring 2 sets of flippers."),
                    Addition("And bring sunscreen."),
                )
            )
        }
    }

    @Test
    fun `when saving an interpretation, the conclusions corresponding to the verified text should be saved in order`() {
        // given
        val id = supplyCaseFromFile("Case1", endpoint).caseId.id!!
        val case = endpoint.viewableCase(id)
        val comment1 = "Go to Bondi."
        val comment2 = "Bring 2 sets of flippers."
        val comment3 = "And bring sunscreen."
        val interp = case.viewableInterpretation.apply {
            verifiedText = "$comment1 $comment2 $comment3"
        }
        val viewableCase = case.apply {
            viewableInterpretation = interp
        }

        // when
        val savedCase = endpoint.saveInterpretation(viewableCase)

        // then
        endpoint.kb.interpretationViewManager.allInOrder()
            .map { it.text } shouldBe setOf(comment1, comment2, comment3)

        savedCase.diffList() shouldBe DiffList(
            listOf(
                Addition(comment1),
                Addition(comment2),
                Addition(comment3),
            )
        )
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
            cornerstoneStatusForIndex(0) shouldBe CornerstoneStatus(viewableCase1, 0, 1)
            commitCurrentRuleSession()
            kb.allCornerstoneCases() shouldHaveSize 2

            val viewableCase2 = endpoint.viewableCase(kb.allCornerstoneCases()[1].id!!)
            startRuleSessionToAddConclusion(id3, conclusion3)
            cornerstoneStatusForIndex(1) shouldBe CornerstoneStatus(viewableCase2, 1, 2)
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