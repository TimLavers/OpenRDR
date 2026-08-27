package io.rippledown.kb

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.rippledown.model.Attribute
import io.rippledown.model.rule.*
import io.rippledown.persistence.DerivedDefinitionStore
import io.rippledown.persistence.inmemory.InMemoryAttributeStore
import io.rippledown.persistence.inmemory.InMemoryDerivedDefinitionStore
import kotlin.test.Test

class DerivedDefinitionManagerTest {
    private val weight = Attribute(1, "weight")
    private val height = Attribute(2, "height")
    private val attributeManager = AttributeManager(InMemoryAttributeStore(setOf(weight, height)))
    private val bmiFormula = Formula(
        Binary(
            Operator.DIVIDE,
            AttributeValue(weight),
            Binary(Operator.TIMES, AttributeValue(height), AttributeValue(height))
        )
    )

    @Test
    fun `definitions in the store are available at construction`() {
        // Given a store holding definitions
        val store = InMemoryDerivedDefinitionStore()
        store.store(10, bmiFormula)
        store.store(11, Literal("diabetic"))

        // When a manager is created on it
        val manager = DerivedDefinitionManager(store, attributeManager)

        // Then the definitions are available
        manager.definitionFor(10) shouldBe bmiFormula
        manager.definitionFor(11) shouldBe Literal("diabetic")
        manager.all() shouldBe mapOf<Int, ValueExpression>(10 to bmiFormula, 11 to Literal("diabetic"))
    }

    @Test
    fun `definitionFor returns null when there is no definition for the attribute`() {
        // Given a manager on an empty store
        val manager = DerivedDefinitionManager(InMemoryDerivedDefinitionStore(), attributeManager)

        // Then there is no definition for an unknown attribute
        manager.definitionFor(42).shouldBeNull()
    }

    @Test
    fun `store persists the definition and makes it available`() {
        // Given a manager on an empty store
        val store = InMemoryDerivedDefinitionStore()
        val manager = DerivedDefinitionManager(store, attributeManager)

        // When a definition is stored
        manager.store(10, bmiFormula)

        // Then it is available from the manager and persisted in the store
        manager.definitionFor(10) shouldBe bmiFormula
        store.definitionFor(10) shouldBe bmiFormula
    }

    @Test
    fun `store overwrites an existing definition`() {
        // Given a manager with a definition
        val store = InMemoryDerivedDefinitionStore()
        val manager = DerivedDefinitionManager(store, attributeManager)
        manager.store(10, bmiFormula)

        // When a new definition is stored for the same attribute
        val corrected = Literal("30")
        manager.store(10, corrected)

        // Then the new definition replaces the old, in the manager and the store
        manager.definitionFor(10) shouldBe corrected
        store.definitionFor(10) shouldBe corrected
    }

    @Test
    fun `a stored definition is aligned with the attributes held by the knowledge base`() {
        // Given a store holding a definition referring to an attribute by a name it no longer has
        val store = InMemoryDerivedDefinitionStore()
        store.store(10, Formula(AttributeValue(Attribute(1, "Wgt"))))
        val renamedManager = AttributeManager(InMemoryAttributeStore(setOf(Attribute(1, "Weight"))))

        // When a manager is created on the store
        val manager = DerivedDefinitionManager(store, renamedManager)

        // Then the definition reads with the attribute's current name
        manager.definitionFor(10)?.asText() shouldBe "Weight"
    }

    @Test
    fun `a stored definition referring to an unknown attribute is left as it was stored`() {
        // Given a store holding a definition referring to an attribute the KB does not have
        val store = InMemoryDerivedDefinitionStore()
        val definition = Formula(AttributeValue(Attribute(99, "Whatever")))
        store.store(10, definition)

        // When a manager is created on the store
        val manager = DerivedDefinitionManager(store, attributeManager)

        // Then the definition is unchanged
        manager.definitionFor(10) shouldBe definition
    }

    @Test
    fun `store delegates to the backing store`() {
        // Given a manager on a mocked store
        val store = mockk<DerivedDefinitionStore>()
        every { store.all() } returns emptyMap()
        every { store.store(any(), any()) } returns Unit
        val manager = DerivedDefinitionManager(store, attributeManager)

        // When a definition is stored
        manager.store(10, bmiFormula)

        // Then the backing store is updated
        verify { store.store(10, bmiFormula) }
    }
}
