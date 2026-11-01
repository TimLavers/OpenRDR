package steps

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.*
import io.rippledown.integration.pageobjects.CaseViewPO
import io.rippledown.integration.restclient.RESTClient
import io.rippledown.main.Api
import io.rippledown.model.CaseId
import io.rippledown.model.CasesInfo
import io.rippledown.model.KBInfo
import io.rippledown.model.caseview.AiProvenance
import io.rippledown.model.caseview.DerivedValueInfo
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.chat.ChatContextInfo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DerivedValueStepDefsTest {
    private val original = KBInfo("thyroids_original", "Thyroids")
    private var activeKB = original
    private var restSelection = original
    private val caseId = CaseId(1L, "Export01")
    private val saved = AiProvenance("clinical_notes", "Has diabetes?", "T2 Diab", 123L, "model", "revision")
    private var provenance = saved
    private val api = mockk<Api>()
    private lateinit var client: RESTClient
    private lateinit var steps: DerivedValueStepDefs

    @BeforeEach
    fun setUp() {
        val page = mockk<CaseViewPO>()
        val case = mockk<ViewableCase>()
        coEvery { api.chatContext() } answers {
            restSelection = activeKB
            ChatContextInfo(activeKB, caseId.id)
        }
        coEvery { api.waitingCasesInfo() } answers {
            check(restSelection == activeKB) { "Request targets the deleted KB: ${restSelection.id}" }
            CasesInfo(caseIds = listOf(caseId))
        }
        coEvery { api.getCase(1L) } returns case
        every { case.derivedValues() } answers {
            listOf(DerivedValueInfo("hasDiabetes", "true", "AI", emptyList(), provenance = provenance))
        }
        client = RESTClient(api)
        mockkStatic(::restClient, ::caseViewPO)
        every { restClient() } returns client
        every { caseViewPO() } returns page
        every { page.nameShown() } returns caseId.name
        steps = DerivedValueStepDefs(mockk())
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(::restClient, ::caseViewPO)
        client.close()
    }

    @Test
    fun `provenance checks follow the imported KB instead of requesting the deleted KB`() {
        // Given saved provenance in a KB that is subsequently deleted and re-imported.
        steps.rememberAiProvenance("hasDiabetes")
        activeKB = KBInfo("thyroids_imported", "Thyroids")

        // When the imported case is checked.
        steps.requireSameAiProvenance("hasDiabetes")

        // Then both reads refresh the REST selection before querying the case.
        restSelection shouldBe activeKB
        coVerifySequence {
            api.chatContext()
            api.waitingCasesInfo()
            api.getCase(1L)
            api.chatContext()
            api.waitingCasesInfo()
            api.getCase(1L)
        }
    }

    @Test
    fun `remembering provenance waits for the case name to finish rendering`() {
        // Given the label is temporarily absent during a case-view update.
        val page = caseViewPO()
        every { page.nameShown() } returnsMany listOf(null, caseId.name)

        // When
        steps.rememberAiProvenance("hasDiabetes")
        steps.requireSameAiProvenance("hasDiabetes")

        // Then the transient absence is retried and provenance is saved for the displayed case.
        verify(atLeast = 3) { page.nameShown() }
    }

    @Test
    fun `provenance checks still reject a changed evaluation after import`() {
        // Given an imported case whose evaluation timestamp changed.
        steps.rememberAiProvenance("hasDiabetes")
        activeKB = KBInfo("thyroids_imported", "Thyroids")
        provenance = saved.copy(evaluatedAt = saved.evaluatedAt + 1)

        // When / Then the comparison fails rather than accepting the new evaluation.
        shouldThrow<AssertionError> { steps.requireSameAiProvenance("hasDiabetes") }
    }
}
