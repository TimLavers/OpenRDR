package io.rippledown.persistence.inmemory

import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import io.rippledown.model.AttributeKind
import kotlin.test.BeforeTest
import kotlin.test.Test

class InMemoryAttributeStoreTest {
    private lateinit var store: InMemoryAttributeStore

    @BeforeTest
    fun setup() {
        store = InMemoryAttributeStore()
    }

    @Test
    fun `create without a kind creates an external attribute`() {
        // When an attribute is created without specifying a kind
        val glucose = store.create("Glucose")

        // Then it is external
        glucose.kind shouldBe AttributeKind.EXTERNAL
        store.all() shouldBe setOf(glucose)
    }

    @Test
    fun `create with a kind creates an attribute of that kind`() {
        // When attributes are created with each kind
        val glucose = store.create("Glucose", AttributeKind.EXTERNAL)
        val bmi = store.create("BMI", AttributeKind.DERIVED)
        val comment = store.create("DiabetesStatus", AttributeKind.COMMENT)

        // Then the kinds are as requested
        glucose.kind shouldBe AttributeKind.EXTERNAL
        bmi.kind shouldBe AttributeKind.DERIVED
        comment.kind shouldBe AttributeKind.COMMENT
        store.all() shouldBe setOf(glucose, bmi, comment)
    }

    @Test
    fun `load preserves kinds`() {
        // Given attributes of each kind
        val attributes = setOf(
            Attribute(1, "Glucose"),
            Attribute(2, "BMI", AttributeKind.DERIVED),
            Attribute(3, "DiabetesStatus", AttributeKind.COMMENT)
        )

        // When they are loaded
        store.load(attributes)

        // Then the kinds are unchanged
        store.all() shouldBe attributes
        store.all().map { it.kind }.toSet() shouldBe AttributeKind.entries.toSet()
    }
}
