package io.rippledown.server

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.rippledown.CaseTestUtils
import io.rippledown.model.Attribute
import io.rippledown.model.RDRCase
import io.rippledown.model.TestResult
import io.rippledown.model.beSameAs
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.GreaterThanOrEqualTo
import io.rippledown.model.condition.HasCurrentValue
import io.rippledown.model.condition.Is
import io.rippledown.model.*
import io.rippledown.model.condition.*
import io.rippledown.model.diff.Addition
import io.rippledown.model.diff.DiffList
import io.rippledown.model.diff.Unchanged
import io.rippledown.model.interpretationview.ViewableInterpretation
import io.rippledown.model.rule.ChangeTreeToAddConclusion
import io.rippledown.model.rule.CornerstoneStatus
import io.rippledown.persistence.inmemory.InMemoryPersistenceProvider
import io.rippledown.supplyCaseFromFile
import io.rippledown.util.shouldContainSameAs
import org.apache.commons.io.FileUtils
import java.io.File
import java.nio.file.Files
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

fun RDRCase.getAttribute(attributeName: String): Attribute {
    return attributes.first { attribute -> attribute.name == attributeName }
}

fun RDRCase.getLatest(attributeName: String): TestResult? {
    return getLatest(getAttribute(attributeName))
}

internal class ServerApplicationTest {

    private val persistenceProvider = InMemoryPersistenceProvider()
    private lateinit var app: ServerApplication

    @BeforeTest
    fun setup() {
        app = ServerApplication(persistenceProvider)
        FileUtils.cleanDirectory(app.casesDir)
        FileUtils.cleanDirectory(app.interpretationsDir)
    }

    @Test
    fun casesDir() {
        assertEquals(app.casesDir, File("cases"))
        assertTrue(app.casesDir.exists())
    }

    @Test
    fun interpretationsDir() {
        assertEquals(app.interpretationsDir, File("interpretations"))
        assertTrue(app.interpretationsDir.exists())
    }

    @Test
    fun case() {
        val caseId = supplyCaseFromFile("Case1", app).caseId
        val retrieved = app.case(caseId.id!!)
        assertEquals(retrieved.name, "Case1")
        assertEquals(retrieved.getLatest("TSH")!!.value.text, "0.667")
        assertEquals(retrieved.getLatest("ABC")!!.value.text, "6.7")
        assertEquals(2, retrieved.data.size)
        // No rules added.
        retrieved.interpretation.conclusions().size shouldBe 0
        // Add a rule.
        val conclusion = app.kb.conclusionManager.getOrCreate("ABC ok.")
        app.kb.startRuleSession(retrieved, ChangeTreeToAddConclusion(conclusion))
        val abc = retrieved.getAttribute("ABC")
        app.kb.addConditionToCurrentRuleSession(greaterThanOrEqualTo(null, abc, 5.0))
        app.kb.commitCurrentRuleSession()
        val retrievedAgain = app.case(caseId.id!!)
        retrievedAgain.interpretation.conclusions() shouldContainExactly setOf(conclusion)
    }

    @Test
    fun `should retrieve the cached case when using InMemoryPersistenceProvider`() {
        val id = supplyCaseFromFile("Case1", app).caseId.id!!
        val retrieved = app.case(id)
        val retrievedAgain = app.case(id)
        retrievedAgain shouldBeSameInstanceAs retrieved
    }

    @Test
    fun `should set the interpretation's DiffList to be empty when retrieving a case with a blank interpretation`() {
        val id = supplyCaseFromFile("Case1", app).caseId.id!!

        val case = app.viewableCase(id)
        case.verifiedText() shouldBe null
        case.textGivenByRules() shouldBe ""
        case.diffList() shouldBe DiffList(emptyList())
    }

    @Test
    fun `the DiffList should have an unchanged fragment when retrieving a case with a non-blank interpretation`() {
        val id = supplyCaseFromFile("Case1", app).caseId.id!!
        val case = app.case(id)
        val text = "ABC ok."
        val conclusion = app.kb.conclusionManager.getOrCreate(text)
        app.kb.startRuleSession(case, ChangeTreeToAddConclusion(conclusion))
        app.kb.commitCurrentRuleSession()

        with(app.viewableCase(id)) {
            verifiedText() shouldBe null
            textGivenByRules() shouldBe text
            diffList() shouldBe DiffList(listOf(Unchanged(text)))
        }
    }

    @Test
    fun `should update a case's verified interpretation and return an interpretation containing a DiffList`() {
        val caseId = supplyCaseFromFile("Case1", app).caseId
        val id = caseId.id!!
        val original = app.viewableCase(id)
        original.verifiedText() shouldBe null
        original.diffList() shouldBe DiffList()

        val verified = "Verified."
        val verifiedInterpretation = original.viewableInterpretation.apply { verifiedText = verified }
        val returnedInterpretation = app.saveInterpretation(verifiedInterpretation)
        val updated = app.viewableCase(id)
        updated.viewableInterpretation shouldBe returnedInterpretation
        original.verifiedText() shouldBe verified
        updated.diffList() shouldBe DiffList(listOf(Addition(verified)))
    }

    @Test
    fun `should return condition hints for a case`() {
        val id = supplyCaseFromFile("Case1", app).caseId.id!!
        val case = app.case(id)
        val expectedConditions: List<Condition> = case.attributes
            .filter { attribute ->
                case.getLatest(attribute) != null
            }.map { attribute ->
                hasCurrentValue(1, attribute)
            }
        val hintConditions = app.conditionHintsForCase(id).conditions.toSet()

        hintConditions.size shouldBe expectedConditions.size
        expectedConditions.forEach {
            hintConditions shouldContainSameAs it
        }
    }

    @Test
    fun `condition hints should all have an id`() {
        val id = supplyCaseFromFile("Case1", app).caseId.id!!

        app.conditionHintsForCase(id).conditions.toSet().forEach { condition ->
            condition.id shouldNotBe null
        }
    }


    @Test
    fun processCase() {
        app.kb.allProcessedCases() shouldBe emptyList()
        val externalCase1 = CaseTestUtils.getCase("Case1")
        val case1 = app.processCase(externalCase1)
        case1.name shouldBe externalCase1.name
        val retrieved1 = app.kb.getProcessedCase(case1.caseId.id!!)!!
        retrieved1.caseId shouldBe case1.caseId
        // Supply it again.
        val case2 = app.processCase(externalCase1)
        val retrieved2 = app.kb.getProcessedCase(case2.caseId.id!!)!!
        app.kb.allProcessedCases() shouldBe listOf(retrieved1, retrieved2)
    }

    @Test
    fun deleteProcessedCase() {
        val externalCase1 = CaseTestUtils.getCase("Case1")
        val case1 = app.processCase(externalCase1)
        val externalCase2 = CaseTestUtils.getCase("Case2")
        val case2 = app.processCase(externalCase2)
        app.kb.allProcessedCases() shouldBe listOf(case1, case2)
        app.deleteProcessedCase(case2.name)
        app.kb.allProcessedCases() shouldBe listOf(case1)
    }

    @Test
    fun viewableCase() {
        val id = supplyCaseFromFile("Case1", app).caseId.id!!

        val retrieved = app.viewableCase(id)
        assertEquals(retrieved.name, "Case1")
        assertEquals(retrieved.case.getLatest("ABC")!!.value.text, "6.7")
        assertEquals(2, retrieved.attributes().size)
        // No rules added.
        retrieved.viewableInterpretation.interpretation.conclusions().size shouldBe 0
        // Add a rule.
        val conclusion = app.kb.conclusionManager.getOrCreate("ABC ok.")
        app.kb.startRuleSession(retrieved.case, ChangeTreeToAddConclusion(conclusion))
        val abc = retrieved.case.getAttribute("ABC")
        app.kb.addConditionToCurrentRuleSession(greaterThanOrEqualTo(null, abc, 5.0))
        app.kb.commitCurrentRuleSession()
        val retrievedAgain = app.viewableCase(id)
        retrievedAgain.viewableInterpretation.interpretation.conclusions() shouldContainExactly setOf(conclusion)
    }

    @Test
    fun getOrCreateAttribute() {
        app.kb.attributeManager.all() shouldBe emptySet()
        val attribute = app.getOrCreateAttribute("stuff")
        app.kb.attributeManager.all() shouldBe setOf(attribute)
    }

    @Test
    fun getOrCreateConclusion() {
        app.kb.conclusionManager.all() shouldBe emptySet()
        val text = "It is rainy."
        val conclusion = app.getOrCreateConclusion(text)
        conclusion.text shouldBe text
        app.kb.conclusionManager.all() shouldBe setOf(conclusion)
    }

    @Test
    fun getOrCreateCondition() {
        app.kb.conditionManager.all() shouldBe emptySet()
        val attribute = app.getOrCreateAttribute("stuff")
        val prototype = isCondition(null, attribute, "Whatever")
        val created = app.getOrCreateCondition(prototype)
        created should beSameAs(prototype)
        app.kb.conditionManager.all() shouldBe setOf(created)
    }

    @Test
    fun moveAttributeJustBelow() {
        val id = supplyCaseFromFile("Case1", app).caseId.id!!
        val retrieved = app.viewableCase(id)
        val attributesBefore = retrieved.attributes()
        attributesBefore.size shouldBe 2 // sanity
        app.moveAttributeJustBelow(attributesBefore[0].id, attributesBefore[1].id)
        // Get the case again and check that the order has been applied.
        val retrievedAfter = app.viewableCase(id)
        retrievedAfter.attributes()[0] shouldBe attributesBefore[1]
        retrievedAfter.attributes()[1] shouldBe attributesBefore[0]
    }

    @Test
    fun waitingCasesInfo() {
        FileUtils.cleanDirectory(app.casesDir)
        assertEquals(app.waitingCasesInfo().kbName, app.kb.kbInfo.name)
        assertEquals(app.waitingCasesInfo().count, 0)

        // Provide some cases.
        supplyCaseFromFile("Case2", app)
        val ci1 = app.waitingCasesInfo()
        assertEquals(ci1.count, 1)
        assertEquals(ci1.caseIds[0].name, "Case2")

        supplyCaseFromFile("Case3", app)
        val ci2 = app.waitingCasesInfo()
        assertEquals(ci2.count, 2)
        assertEquals(ci2.caseIds[0].name, "Case2")
        assertEquals(ci2.caseIds[1].name, "Case3")
    }

    @Test
    fun createKB() {
        app.kb.kbInfo.name shouldBe "Thyroids"
        val kbIdsBefore = persistenceProvider.idStore().data().keys
        app.kb.containsCornerstoneCaseWithName("Case1") shouldBe false //sanity
        app.kb.addCornerstoneCase(createCase("Case1"))
        app.kb.containsCornerstoneCaseWithName("Case1") shouldBe true

        app.createKB()
        app.kb.kbInfo.name shouldBe "Thyroids"
        app.kb.containsCornerstoneCaseWithName("Case1") shouldBe false //kb rebuilt
        // Check that all of the other KBs are still there.
        persistenceProvider.idStore().data().keys shouldBe setOf(app.kbName().id).union(kbIdsBefore)
    }

    @Test
    fun reCreateKB() {
        app.kb.kbInfo.name shouldBe "Thyroids"
        val kbIdsBefore = persistenceProvider.idStore().data().keys
        val oldKBId = app.kbName().id
        val caseName = "Case A"
        app.kb.addCornerstoneCase(createCase(caseName))
        app.kb.containsCornerstoneCaseWithName(caseName) shouldBe true

        app.reCreateKB()
        app.kb.kbInfo.name shouldBe "Thyroids"
        app.kb.containsCornerstoneCaseWithName(caseName) shouldBe false //kb rebuilt
        // Check that the old KB has been deleted.
        val expectedKBIdsAfter = kbIdsBefore.minus(oldKBId).plus(app.kbName().id)
        persistenceProvider.idStore().data().keys shouldBe expectedKBIdsAfter
    }

    @Test
    fun kbName() {
        app.kbName().name shouldBe "Thyroids"
    }

    @Test
    fun startRuleSessionToAddConclusion() {
        val id = supplyCaseFromFile("Case1", app).caseId.id!!
        app.case(id).interpretation.conclusions() shouldBe emptySet()
        app.startRuleSessionToAddConclusion(id, app.kb.conclusionManager.getOrCreate("Whatever"))
        app.commitCurrentRuleSession()
        app.case(id).interpretation.conclusionTexts() shouldBe setOf("Whatever")
    }

    @Test
    fun exportKBToZip() {
        // Add a case and a rule to the KB.
        val id = supplyCaseFromFile("Case1", app).caseId.id!!
        val conclusion1 = app.kb.conclusionManager.getOrCreate("Whatever")
        val tsh = app.kb.attributeManager.getOrCreate("TSH")
        app.startRuleSessionToAddConclusion(id, conclusion1)
        val tshCondition = greaterThanOrEqualTo(null, tsh, 0.6)
        app.addConditionToCurrentRuleBuildingSession(tshCondition)
        app.commitCurrentRuleSession()
        app.case(id).interpretation.conclusionTexts() shouldBe setOf(conclusion1.text)

        // Get the exported KB.
        val exported = app.exportKBToZip()
        exported.name shouldBe "${app.kb.kbInfo}.zip"

        // Clear the KB
        app.createKB()
        app.kb.allCornerstoneCases() shouldBe emptyList()
        app.kb.ruleTree.size() shouldBe 1

        // Import the exported KB.
        app.importKBFromZip(exported.readBytes())
        app.kb.allCornerstoneCases().size shouldBe 1
        app.kb.ruleTree.size() shouldBe 2
        val rule = app.kb.ruleTree.root.childRules().single()
        val conditions = rule.conditions
        conditions.size shouldBe 1
        conditions.single().sameAs(tshCondition) shouldBe true
        rule.conclusion shouldBe conclusion1
    }

    @Test
    fun `handle zip in bad format`() {
        val zipFile = File("src/jvmTest/resources/export/NoRootDir.zip").toPath()
        shouldThrow<IllegalArgumentException> {
            app.importKBFromZip(Files.readAllBytes(zipFile))
        }.message shouldBe "Invalid zip for KB import."
    }

    @Test
    fun `handle empty zip`() {
        val zipFile = File("src/jvmTest/resources/export/Empty.zip").toPath()
        shouldThrow<IllegalArgumentException> {
            app.importKBFromZip(Files.readAllBytes(zipFile))
        }.message shouldBe "Invalid zip for KB import."
    }

    @Test
    fun startSessionToReplaceConclusion() {
        val caseId = supplyCaseFromFile("Case1", app).caseId
        val id = caseId.id!!
        val conclusion1 = app.kb.conclusionManager.getOrCreate("Whatever")
        app.startRuleSessionToAddConclusion(id, conclusion1)
        app.commitCurrentRuleSession()
        app.case(id).interpretation.conclusionTexts() shouldBe setOf(conclusion1.text)
        val conclusion2 = app.kb.conclusionManager.getOrCreate("Blah")
        app.startRuleSessionToReplaceConclusion(id, conclusion1, conclusion2)
        app.commitCurrentRuleSession()
        app.case(id).interpretation.conclusionTexts() shouldBe setOf(conclusion2.text)
    }

    @Test
    fun `when saving an interpretation, an interpretation with diffs should be returned`() {
        val id = supplyCaseFromFile("Case1", app).caseId.id!!
        val conclusion = app.kb.conclusionManager.getOrCreate("Go to Bondi.")
        with(app) {
            startRuleSessionToAddConclusion(id, conclusion)
            commitCurrentRuleSession()
            val case = case(id)
            case.interpretation.conclusionTexts() shouldBe setOf(conclusion.text)

            val verifiedInterpretation = ViewableInterpretation(case.interpretation).apply {
                verifiedText = "Go to Bondi. Bring 2 sets of flippers. And bring sunscreen."
            }
            val interpReturned = app.saveInterpretation(verifiedInterpretation)
            interpReturned.diffList shouldBe DiffList(
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
        val id = supplyCaseFromFile("Case1", app).caseId.id!!
        val case = app.viewableCase(id)
        val comment1 = "Go to Bondi."
        val comment2 = "Bring 2 sets of flippers."
        val comment3 = "And bring sunscreen."
        val interp = case.viewableInterpretation.apply {
            verifiedText = "$comment1 $comment2 $comment3"
        }

        // when
        val savedInterpretation = app.saveInterpretation(interp)

        // then
        app.kb.interpretationViewManager.allInOrder()
            .map { it.text } shouldBe setOf(comment1, comment2, comment3)

        savedInterpretation.diffList shouldBe DiffList(
            listOf(
                Addition(comment1),
                Addition(comment2),
                Addition(comment3),
            )
        )
    }

    @Test
    fun `After committing a rule the session case should be a cornerstone`() {
        val id = supplyCaseFromFile("Case1", app).caseId.id!!
        val conclusion = app.kb.conclusionManager.getOrCreate("Whatever")
        app.kb.allCornerstoneCases() shouldHaveSize 0
        app.startRuleSessionToAddConclusion(id, conclusion)
        app.commitCurrentRuleSession()
        app.kb.allCornerstoneCases() shouldHaveSize 1
    }

    @Test
    fun `Should select a cornerstone by its index`() {
        val id1 = supplyCaseFromFile("Case1", app).caseId.id!!
        val id2 = supplyCaseFromFile("Case2", app).caseId.id!!
        val id3 = supplyCaseFromFile("Case3", app).caseId.id!!
        val conclusion1 = app.kb.conclusionManager.getOrCreate("Whatever 1")
        val conclusion2 = app.kb.conclusionManager.getOrCreate("Whatever 2")
        val conclusion3 = app.kb.conclusionManager.getOrCreate("Whatever 3")
        with(app) {
            kb.allCornerstoneCases() shouldHaveSize 0
            startRuleSessionToAddConclusion(id1, conclusion1)
            commitCurrentRuleSession()
            kb.allCornerstoneCases() shouldHaveSize 1

            val viewableCase1 = app.viewableCase(kb.allCornerstoneCases().first().id!!)
            startRuleSessionToAddConclusion(id2, conclusion2)
            cornerstoneStatusForIndex(0) shouldBe CornerstoneStatus(viewableCase1, 0, 1)
            commitCurrentRuleSession()
            kb.allCornerstoneCases() shouldHaveSize 2

            val viewableCase2 = app.viewableCase(kb.allCornerstoneCases()[1].id!!)
            startRuleSessionToAddConclusion(id3, conclusion3)
            cornerstoneStatusForIndex(1) shouldBe CornerstoneStatus(viewableCase2, 1, 2)
        }
    }

    private fun createCase(caseName: String) = CaseTestUtils.createCase(caseName)
}