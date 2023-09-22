package io.rippledown.kb

import io.kotest.matchers.shouldBe
import io.rippledown.model.Conclusion
import io.rippledown.persistence.inmemory.InMemoryConclusionStore
import io.rippledown.persistence.inmemory.InMemoryOrderStore
import kotlin.test.BeforeTest
import kotlin.test.Test

class ViewableInterpretationManagerTest {
    private lateinit var manager: InterpretationViewManager

    @BeforeTest
    fun init() {
        val conclusionManager = ConclusionManager(InMemoryConclusionStore())
        val orderStore = InMemoryOrderStore()
        manager = InterpretationViewManager(orderStore, conclusionManager)
    }

    @Test
    fun `should be no ordering when the interpretation view manager is created from an empty conclusion manager`() {
        manager.allInOrder() shouldBe emptyList()
    }

    @Test
    fun `a single new conclusion should be put at the end`() {
        // Given
        manager.insert(listOf(Conclusion(0, "A")))

    }
}