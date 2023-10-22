package io.rippledown.kb

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.rippledown.model.CaseId
import io.rippledown.model.Conclusion
import io.rippledown.model.Interpretation
import io.rippledown.model.diff.Addition
import io.rippledown.model.diff.DiffList
import io.rippledown.model.diff.Unchanged
import io.rippledown.persistence.inmemory.InMemoryConclusionStore
import io.rippledown.persistence.inmemory.InMemoryOrderStore
import io.rippledown.persistence.inmemory.InMemoryVerifiedTextStore
import kotlin.test.BeforeTest
import kotlin.test.Test

class InterpretationViewManagerTest {
    private lateinit var manager: InterpretationViewManager
    private lateinit var verifiedTextStore: InMemoryVerifiedTextStore

    @BeforeTest
    fun init() {
        val conclusionManager = ConclusionManager(InMemoryConclusionStore())
        val orderStore = InMemoryOrderStore()
        verifiedTextStore = InMemoryVerifiedTextStore()
        manager = InterpretationViewManager(orderStore, conclusionManager, verifiedTextStore)
    }

    @Test
    fun `should retrieve a verified text when creating a viewable interpretation`() {
        val id: Long = 42
        val verifiedText = "Bondi"
        verifiedTextStore.put(id, verifiedText)
        val interpretation = Interpretation(CaseId(id))
        val viewableInterpretation = manager.viewableInterpretation(interpretation)
        viewableInterpretation.verifiedText shouldBe verifiedText
    }

    @Test
    fun `should not retrieve verified text when creating a viewable interpretation if there is none`() {
        val interpretation = Interpretation(CaseId(42))
        val viewableInterpretation = manager.viewableInterpretation(interpretation)
        viewableInterpretation.verifiedText shouldBe null
    }

    @Test
    fun `should be no ordering when the interpretation view manager is created from an empty conclusion manager`() {
        manager.allInOrder() shouldBe emptyList()
    }

    @Test
    fun `should set the text given by rules according to the conclusion ordering`() {
        //Given
        val conclusion1 = Conclusion(1, "a")
        val conclusion2 = Conclusion(2, "b")
        val conclusion3 = Conclusion(3, "c")
        val interpretation = mockk<Interpretation>()
        every { interpretation.conclusions() } returns setOf(conclusion1, conclusion2, conclusion3)
        every { interpretation.caseId } returns CaseId(42, "Hitch")
        manager.insert(listOf(conclusion3, conclusion1, conclusion2))

        //When
        val viewableInterpretation = manager.viewableInterpretation(interpretation)

        //Then
        viewableInterpretation.textGivenByRules shouldBe "c a b"
    }

    @Test
    fun `should calculate the diff list using the conclusion ordering`() {
        //Given
        val conclusion1 = Conclusion(1, "a.")
        val conclusion2 = Conclusion(2, "b.")
        val conclusion3 = Conclusion(3, "c.")
        val interpretation = mockk<Interpretation>()
        every { interpretation.conclusions() } returns setOf(conclusion1, conclusion2, conclusion3)
        every { interpretation.conclusionTexts() } returns setOf("a.", "b.", "c.")
        every { interpretation.caseId } returns CaseId(42, "Hitch")
        manager.insert(listOf(conclusion3, conclusion1, conclusion2))
        verifiedTextStore.put(42, "c. d. a. b.")

        //When
        val viewableInterpretation = manager.viewableInterpretation(interpretation)

        //Then
        viewableInterpretation.diffList shouldBe DiffList(
            listOf(
                Unchanged("c."),
                Addition("d."),
                Unchanged("a."),
                Unchanged("b.")
            )
        )
    }



}