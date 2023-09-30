package io.rippledown.kb

import io.kotest.matchers.shouldBe
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
    fun `a single new conclusion should be put at the end`() {
        // Given
        manager.insert(listOf(Conclusion(0, "A")))
        //todo
    }
}