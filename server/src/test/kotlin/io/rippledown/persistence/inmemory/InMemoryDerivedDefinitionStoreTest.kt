package io.rippledown.persistence.inmemory

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import io.rippledown.model.rule.*
import kotlin.test.BeforeTest
import kotlin.test.Test

class InMemoryDerivedDefinitionStoreTest {
    private val weight = Attribute(1, "weight")
    private val height = Attribute(2, "height")
    private val bmiFormula = Formula(
        Binary(
            Operator.DIVIDE,
            AttributeValue(weight),
            Binary(Operator.TIMES, AttributeValue(height), AttributeValue(height))
        )
    )
    private lateinit var store: InMemoryDerivedDefinitionStore

    @BeforeTest
    fun setup() {
        store = InMemoryDerivedDefinitionStore()
    }

    @Test
    fun `initially empty`() {
        store.all() shouldBe emptyMap()
    }

    @Test
    fun `definitionFor returns null when there is no definition for the attribute`() {
        store.definitionFor(42).shouldBeNull()
    }

    @Test
    fun store() {
        // When definitions are stored for two attributes
        store.store(10, bmiFormula)
        store.store(11, Literal("diabetic"))

        // Then they can be retrieved
        store.definitionFor(10) shouldBe bmiFormula
        store.definitionFor(11) shouldBe Literal("diabetic")
        store.all() shouldBe mapOf(10 to bmiFormula, 11 to Literal("diabetic"))
    }

    @Test
    fun `store overwrites an existing definition`() {
        // Given a stored definition
        store.store(10, bmiFormula)

        // When a new definition is stored for the same attribute
        val corrected = Literal("30")
        store.store(10, corrected)

        // Then the new definition replaces the old
        store.definitionFor(10) shouldBe corrected
        store.all() shouldBe mapOf(10 to corrected)
    }

    @Test
    fun load() {
        // Given definitions to load
        val definitions = mapOf<Int, ValueExpression>(10 to bmiFormula, 11 to Literal("diabetic"))

        // When they are loaded
        store.load(definitions)

        // Then they are all present
        store.all() shouldBe definitions
    }

    @Test
    fun `load not allowed if non-empty`() {
        // Given a non-empty store
        store.store(10, bmiFormula)

        // When a load is attempted
        // Then it is refused
        shouldThrow<IllegalArgumentException> {
            store.load(mapOf(11 to Literal("diabetic")))
        }.message shouldBe "Cannot load definitions into a non-empty derived definition store."
    }
}
