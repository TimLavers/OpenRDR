package io.rippledown.server

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.rippledown.CaseTestUtils
import io.rippledown.model.*
import io.rippledown.model.condition.*
import io.rippledown.model.diff.Addition
import io.rippledown.model.diff.DiffList
import io.rippledown.model.diff.Unchanged
import io.rippledown.model.rule.ChangeTreeToAddConclusion
import io.rippledown.persistence.InMemoryPersistenceProvider
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
        setUpCaseFromFile("Case1", app)
        val retrieved = app.case("Case1")
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
        app.kb.addConditionToCurrentRuleSession(GreaterThanOrEqualTo(null, abc, 5.0))
        app.kb.commitCurrentRuleSession()
        val retrievedAgain = app.case("Case1")
        retrievedAgain.interpretation.conclusions() shouldContainExactly setOf(conclusion)
    }

    @Test
    fun `should retrieve cached case`() {
        val id = "Case1"
        setUpCaseFromFile(id, app)
        val retrieved = app.case(id)
        val retrievedAgain = app.case(id)
        retrievedAgain shouldBeSameInstanceAs retrieved
    }

    @Test
    fun `should set the interpretation's DiffList to be empty when retrieving a case with a blank interpretation`() {
        val id = "Case1"
        setUpCaseFromFile(id, app)

        val case = app.case(id)
        case.interpretation.verifiedText shouldBe null
        case.interpretation.textGivenByRules() shouldBe ""
        case.interpretation.diffList shouldBe DiffList(emptyList())
    }

    @Test
    fun `the DiffList should have an unchanged fragment when retrieving a case with a non-blank interpretation`() {
        val id = "Case1"
        setUpCaseFromFile(id, app)

        val case = app.case(id)
        val text = "ABC ok."
        val conclusion = app.kb.conclusionManager.getOrCreate(text)
        app.kb.startRuleSession(case, ChangeTreeToAddConclusion(conclusion))
        app.kb.commitCurrentRuleSession()

        with(app.viewableCase(id)) {
            interpretation.verifiedText shouldBe null
            interpretation.textGivenByRules() shouldBe text
            interpretation.diffList shouldBe DiffList(listOf(Unchanged(text)))
        }
    }

    @Test
    fun `should update a case's verified interpretation and return an interpretation containing a DiffList`() {
        val id = "Case1"
        val caseId = CaseId(id, id)
        setUpCaseFromFile(id, app)

        val original = app.case(id)
        original.interpretation.verifiedText shouldBe null
        original.interpretation.textGivenByRules() shouldBe ""

        val verifiedInterpretation = Interpretation(caseId, "Verified.")
        val returnedInterpretation = app.saveInterpretation(verifiedInterpretation)
        val updated = app.case(id)
        updated.interpretation shouldBe returnedInterpretation
        returnedInterpretation.diffList shouldBe DiffList(listOf(Addition("Verified.")))
    }

    @Test
    fun `should return condition hints for a case`() {
        val id = "Case1"
        setUpCaseFromFile(id, app)
        val case = app.case(id)
        val expectedConditions: List<Condition> = case.attributes
            .filter { attribute ->
                case.getLatest(attribute) != null
            }.map { attribute ->
                HasCurrentValue(1, attribute)
            }
        val hintConditions = app.conditionHintsForCase(id).conditions.toSet()
        hintConditions.size shouldBe expectedConditions.size
        expectedConditions.forEach{
            hintConditions shouldContainSameAs it
        }
    }

    @Test
    fun viewableCase() {
        setUpCaseFromFile("Case1", app)
        val retrieved = app.viewableCase("Case1")
        assertEquals(retrieved.name, "Case1")
        assertEquals(retrieved.rdrCase.getLatest("ABC")!!.value.text, "6.7")
        assertEquals(2, retrieved.attributes().size)
        // No rules added.
        retrieved.interpretation.conclusions().size shouldBe 0
        // Add a rule.
        val conclusion = app.kb.conclusionManager.getOrCreate( "ABC ok.")
        app.kb.startRuleSession(retrieved.rdrCase, ChangeTreeToAddConclusion(conclusion))
        val abc = retrieved.rdrCase.getAttribute("ABC")
        app.kb.addConditionToCurrentRuleSession(GreaterThanOrEqualTo(null, abc, 5.0))
        app.kb.commitCurrentRuleSession()
        val retrievedAgain = app.viewableCase("Case1")
        retrievedAgain.interpretation.conclusions() shouldContainExactly setOf(conclusion)
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
        val prototype = Is(null, attribute, "Whatever")
        val created = app.getOrCreateCondition(prototype)
        created should beSameAs(prototype)
        app.kb.conditionManager.all() shouldBe setOf(created)
    }

    @Test
    fun moveAttributeJustBelow() {
        setUpCaseFromFile("Case1", app)
        val retrieved = app.viewableCase("Case1")
        val attributesBefore = retrieved.attributes()
        attributesBefore.size shouldBe 2 // sanity
        app.moveAttributeJustBelow(attributesBefore[0].id, attributesBefore[1].id)
        // Get the case again and check that the order has been applied.
        val retrievedAfter = app.viewableCase("Case1")
        retrievedAfter.attributes()[0] shouldBe attributesBefore[1]
        retrievedAfter.attributes()[1] shouldBe attributesBefore[0]
    }

    @Test
    fun waitingCasesInfo() {
        FileUtils.cleanDirectory(app.casesDir)
        assertEquals(app.waitingCasesInfo().resourcePath, File("cases").absolutePath)
        assertEquals(app.waitingCasesInfo().count, 0)

        // Move some cases into the directory.
        setUpCaseFromFile("Case3", app)
        val ci1 = app.waitingCasesInfo()
        assertEquals(ci1.count, 1)
        assertEquals(ci1.caseIds[0].name, "Case3")

        setUpCaseFromFile("Case2", app)
        val ci2 = app.waitingCasesInfo()
        assertEquals(ci2.count, 2)
        assertEquals(ci2.caseIds[0].name, "Case2")
        assertEquals(ci2.caseIds[1].name, "Case3")
    }

    @Test
    fun createKB() {
        app.kb.kbInfo.name shouldBe "Thyroids"
        val kbIdsBefore = persistenceProvider.idStore().data().keys
        app.kb.containsCaseWithName("Case1") shouldBe false //sanity
        app.kb.addCase(createCase("Case1"))
        app.kb.containsCaseWithName("Case1") shouldBe true

        app.createKB()
        app.kb.kbInfo.name shouldBe "Thyroids"
        app.kb.containsCaseWithName("Case1") shouldBe false //kb rebuilt
        // Check that all of the other KBs are still there.
        persistenceProvider.idStore().data().keys shouldBe setOf(app.kbName().id).union(kbIdsBefore) }

    @Test
    fun reCreateKB() {
        app.kb.kbInfo.name shouldBe "Thyroids"
        val kbIdsBefore = persistenceProvider.idStore().data().keys
        val oldKBId = app.kbName().id
        app.kb.addCase(createCase("Case1"))
        app.kb.containsCaseWithName("Case1") shouldBe true

        app.reCreateKB()
        app.kb.kbInfo.name shouldBe "Thyroids"
        app.kb.containsCaseWithName("Case1") shouldBe false //kb rebuilt
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
        val id = "Case1"
        setUpCaseFromFile(id, app)
        app.kb.addCase(createCase(id))
        app.case(id).interpretation.conclusions() shouldBe emptySet()
        app.startRuleSessionToAddConclusion(id, app.kb.conclusionManager.getOrCreate("Whatever"))
        app.commitCurrentRuleSession()
        app.case(id).interpretation.textGivenByRules() shouldBe "Whatever"
    }

    @Test
    fun exportKBToZip() {
        // Add a case and a rule to the KB.
        val id = "Case1"
        setUpCaseFromFile(id, app)
        app.kb.addCase(createCase(id))
        val conclusion1 = app.kb.conclusionManager.getOrCreate( "Whatever")
        val tsh = app.kb.attributeManager.getOrCreate("TSH")
        app.startRuleSessionToAddConclusion(id, conclusion1)
        val tshCondition = GreaterThanOrEqualTo(null, tsh, 0.6)
        app.addConditionToCurrentRuleBuildingSession(tshCondition)
        app.commitCurrentRuleSession()
        app.case(id).interpretation.textGivenByRules() shouldBe conclusion1.text

        // Get the exported KB.
        val exported = app.exportKBToZip()
        exported.name shouldBe "${app.kb.kbInfo}.zip"

        // Clear the KB
        app.createKB()
        app.kb.allCases() shouldBe emptySet()
        app.kb.ruleTree.size() shouldBe 1

        // Import the exported KB.
        app.importKBFromZip(exported.readBytes())
        app.kb.allCases().size shouldBe 1
        app.kb.ruleTree.size() shouldBe 2
        val rule = app.kb.ruleTree.root.childRules().single()
        val conditions = rule.conditions
        conditions.size shouldBe 1
        conditions.single().sameAs(tshCondition) shouldBe true
        rule.conclusion shouldBe conclusion1
        app.case(id).interpretation.textGivenByRules() shouldBe conclusion1.text
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

    private fun setUpCaseFromFile(id: String, app: ServerApplication) {
        FileUtils.copyFileToDirectory(CaseTestUtils.caseFile(id), app.casesDir)
    }

    @Test
    fun startSessionToReplaceConclusion() {
        val id = "Case1"
        setUpCaseFromFile(id, app)
        app.kb.addCase(createCase(id))
        val conclusion1 = app.kb.conclusionManager.getOrCreate( "Whatever")
        app.startRuleSessionToAddConclusion(id, conclusion1)
        app.commitCurrentRuleSession()
        app.case(id).interpretation.textGivenByRules() shouldBe conclusion1.text
        val conclusion2 = app.kb.conclusionManager.getOrCreate("Blah")
        app.startRuleSessionToReplaceConclusion(id, conclusion1, conclusion2)
        app.commitCurrentRuleSession()
        app.case(id).interpretation.textGivenByRules() shouldBe conclusion2.text
    }

    @Test
    fun `when saving an interpretation, an interpretation with diffs should be returned`() {
        val id = "Case1"
        setUpCaseFromFile(id, app)
        val conclusion = app.kb.conclusionManager.getOrCreate("Go to Bondi.")
        with(app) {
            kb.addCase(createCase(id))
            startRuleSessionToAddConclusion(id, conclusion)
            commitCurrentRuleSession()
            val case = case(id)
            case.interpretation.latestText() shouldBe conclusion.text

            val verifiedInterpretation = case.interpretation.apply {
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

    private fun createCase(caseName: String) = CaseTestUtils.createCase(caseName)
}