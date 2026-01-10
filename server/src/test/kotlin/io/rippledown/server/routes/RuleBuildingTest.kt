package io.rippledown.server.routes

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.rippledown.kb.KB
import io.rippledown.kb.KBManager
import io.rippledown.model.COMMENT_SEPARATOR
import io.rippledown.model.diff.Addition
import io.rippledown.model.diff.Removal
import io.rippledown.model.rule.CornerstoneStatus
import io.rippledown.model.rule.RuleRequest
import io.rippledown.model.rule.SessionStartRequest
import io.rippledown.persistence.inmemory.InMemoryPersistenceProvider
import io.rippledown.server.KBEndpoint
import io.rippledown.supplyCaseFromFile
import io.rippledown.util.EntityRetrieval
import org.apache.commons.io.FileUtils
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class RuleBuildingTest {
    private val kbName = "KBEndpointTest"
    private val persistenceProvider = InMemoryPersistenceProvider()
    private val kbManager = KBManager(persistenceProvider, mockk())
    private lateinit var kbEndpoint: KBEndpoint

    @BeforeTest
    fun setup() {
        val kbInfo = kbManager.createKB(kbName)
        val kb = (kbManager.openKB(kbInfo.id) as EntityRetrieval.Success<KB>).entity
        val rootDir = File("kbe")
        FileUtils.cleanDirectory(rootDir)
        kbEndpoint = KBEndpoint(kb)
    }

    @Test
    fun `should return empty CornerstoneStatus when a rule session is started and there are no cornerstones`() {
        val id = supplyCaseFromFile("Case1", kbEndpoint).caseId.id!!
        val diff = Addition("Go to Bondi")
        val cornerstoneStatus = kbEndpoint.startRuleSession(SessionStartRequest(id, diff))
        cornerstoneStatus shouldBe CornerstoneStatus()
    }

    @Test
    fun `should return the first cornerstone when a rule session is started and there are cornerstones`() {
        val id1 = supplyCaseFromFile("Case1", kbEndpoint).caseId.id!!
        val id2 = supplyCaseFromFile("Case2", kbEndpoint).caseId.id!!
        val case1 = kbEndpoint.case(id1)
        val case2 = kbEndpoint.case(id2)
        val cc1 = kbEndpoint.kb.addCornerstoneCase(case1)
        kbEndpoint.kb.addCornerstoneCase(case2)
        val viewableCase = kbEndpoint.viewableCase(cc1.id!!)
        val diff = Addition("Go to Bondi")
        val cornerstoneStatus = kbEndpoint.startRuleSession(SessionStartRequest(id2, diff))
        cornerstoneStatus shouldBe CornerstoneStatus(viewableCase, 0, 1)
    }

    @Test
    fun `should build a rule and return an interpretation when a comment is removed`() {
        //Given
        val id = supplyCaseFromFile("Case1", kbEndpoint).caseId.id!!
        val comment1 = "Bondi or bust."
        val comment2 = "Bring your flippers."
        with(kbEndpoint) {
            startRuleSessionToAddConclusion(id, kbEndpoint.getOrCreateConclusion(comment1))
            commitCurrentRuleSession()
            startRuleSessionToAddConclusion(id, kbEndpoint.getOrCreateConclusion(comment2))
            commitCurrentRuleSession()
            viewableCase(id).latestText() shouldBe "$comment1${COMMENT_SEPARATOR}$comment2" //sanity check
        }

        //When
        kbEndpoint.startRuleSession(SessionStartRequest(id, Removal(comment2)))
        val ruleRequest = RuleRequest(id)
        kbEndpoint.commitRuleSession(ruleRequest)

        //Then
        val updatedInterpretation = kbEndpoint.viewableCase(id).viewableInterpretation
        updatedInterpretation.latestText() shouldBe comment1
    }

    @Test
    fun `should return the specified cornerstone when the user selects its index`() {
        val id1 = supplyCaseFromFile("Case1", kbEndpoint).caseId.id!!
        val id2 = supplyCaseFromFile("Case2", kbEndpoint).caseId.id!!
        val id3 = supplyCaseFromFile("Case3", kbEndpoint).caseId.id!!
        val id4 = supplyCaseFromFile("Case4", kbEndpoint).caseId.id!!
        val case1 = kbEndpoint.case(id1)
        val case2 = kbEndpoint.case(id2)
        val case3 = kbEndpoint.case(id3)
        val case4 = kbEndpoint.case(id4)
        kbEndpoint.kb.addCornerstoneCase(case1).id!!
        kbEndpoint.kb.addCornerstoneCase(case2).id!!
        val cc3Id = kbEndpoint.kb.addCornerstoneCase(case3).id!!
        kbEndpoint.kb.addCornerstoneCase(case4).id!!
        val ccStatus = kbEndpoint.startRuleSession(SessionStartRequest(id1, Addition("Go to Bondi")))
        withClue("sanity check. The session case is not a cornerstone") {
            ccStatus.numberOfCornerstones shouldBe 3
        }
        withClue("There are 3 cornerstones showing, so index 1 0-based corresponds to case3") {

        }
        val cornerstone = kbEndpoint.selectCornerstone(1).cornerstoneToReview //0-based index
        cornerstone shouldBe kbEndpoint.viewableCase(cc3Id)
    }
}
