package io.rippledown.kb

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.rippledown.model.CaseId
import io.rippledown.model.Conclusion
import io.rippledown.model.Interpretation
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
        manager = InterpretationViewManager(orderStore, conclusionManager)
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
}