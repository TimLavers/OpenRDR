package io.rippledown.persistence.postgres

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import io.rippledown.model.AttributeKind
import io.rippledown.persistence.AttributeStore
import kotlin.test.BeforeTest
import kotlin.test.Test

class PostgresAttributeStoreTest: PostgresStoreTest() {
    private lateinit var store: AttributeStore

    override fun reload() {
        super.reload()
        store = postgresKB.attributeStore()
    }

    @BeforeTest
    fun setup() {
        clearTables()
        store = postgresKB.attributeStore()
    }

    @Test
    fun `initially empty`() {
        store.all() shouldBe emptySet()
    }

    @Test
    fun create() {
        val a1 = store.create("A1")
        store.all() shouldContain a1
        a1.name shouldBe "A1"

        // Rebuild and check it's there.
        reload()

        store.all() shouldContain a1
        store.all().size shouldBe 1
    }

    @Test
    fun `cannot create attribute with existing name`() {
        val name = "Glucose"
        store.create(name)
        shouldThrow<IllegalArgumentException> {
            store.create(name)
        }.message shouldBe "An attribute with name $name already exists."
    }

    @Test
    fun store() {
        val a1 = store.create("A1")
        val a2 = store.create("A2")

        val updated = Attribute(a1.id, "Updated")
        store.store(updated)

        store.all().map { it.name } shouldBe setOf(a2.name, updated.name)
        store.all().map { it.id } shouldBe setOf(a2.id, updated.id)

        // Rebuild and check again.
        reload()
        store.all().map { it.name } shouldBe setOf(a2.name, updated.name)
        store.all().map { it.id } shouldBe setOf(a2.id, updated.id)
    }

    @Test
    fun all() {
        repeat(100) {
            store.all().size shouldBe it
            val newAttribute = store.create("A$it")
            store.all() shouldContain newAttribute
        }

        // Rebuild and check again.
        reload()
        store.all().size shouldBe  100
    }

    @Test
    fun load() {
        val a1 = Attribute(1, "Glucose")
        val a2 = Attribute(2, "LDL")
        val a3 = Attribute(3, "HDL")
        store.load(setOf(a1, a2, a3))
        store.all() shouldBe setOf(a1, a2, a3)

        // Rebuild and check again.
        reload()
        store.all() shouldBe setOf(a1, a2, a3)
    }

    @Test
    fun `cannot load if there are already attributes`() {
        store.create("Whatever")
        shouldThrow<IllegalArgumentException> {
            store.load(setOf())
        }.message shouldBe "Cannot load attributes if there are are some stored already."
    }

    @Test
    fun `create without a kind creates an external attribute`() {
        // When an attribute is created without specifying a kind
        val glucose = store.create("Glucose")

        // Then it is external, also after a rebuild
        glucose.kind shouldBe AttributeKind.EXTERNAL
        reload()
        store.all().first { it.id == glucose.id }.kind shouldBe AttributeKind.EXTERNAL
    }

    @Test
    fun `kind is persisted`() {
        // Given attributes of each kind
        val glucose = store.create("Glucose", AttributeKind.EXTERNAL)
        val bmi = store.create("BMI", AttributeKind.DERIVED)
        val comment = store.create("DiabetesStatus", AttributeKind.COMMENT)

        // When the store is rebuilt
        reload()

        // Then the kinds are unchanged
        store.all().first { it.id == glucose.id }.kind shouldBe AttributeKind.EXTERNAL
        store.all().first { it.id == bmi.id }.kind shouldBe AttributeKind.DERIVED
        store.all().first { it.id == comment.id }.kind shouldBe AttributeKind.COMMENT
    }

    @Test
    fun `load preserves kinds`() {
        // Given attributes of each kind
        val attributes = setOf(
            Attribute(1, "Glucose"),
            Attribute(2, "BMI", AttributeKind.DERIVED),
            Attribute(3, "DiabetesStatus", AttributeKind.COMMENT)
        )

        // When they are loaded and the store is rebuilt
        store.load(attributes)
        reload()

        // Then the kinds are unchanged
        store.all() shouldBe attributes
        store.all().map { it.kind }.toSet() shouldBe AttributeKind.entries.toSet()
    }
}