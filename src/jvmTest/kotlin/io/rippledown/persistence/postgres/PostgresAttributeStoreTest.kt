package io.rippledown.persistence.postgres

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import kotlin.IllegalArgumentException
import kotlin.test.BeforeTest
import kotlin.test.Test

class PostgresAttributeStoreTest {
    private val dbName = "rdr_test"
    private lateinit var store: PostgresAttributeStore

    init {
        ConnectionProvider.systemConnection().use {
            it.createStatement().executeUpdate("DROP DATABASE IF EXISTS $dbName")
        }
        ConnectionProvider.systemConnection().use {
            it.createStatement().executeUpdate("CREATE DATABASE $dbName")
        }
    }

    @BeforeTest
    fun setup() {
        // Delete the table.
        ConnectionProvider.systemConnection().use {
            it.createStatement().executeUpdate("DROP TABLE IF EXISTS $KB_IDS_TABLE")
        }
        store = PostgresAttributeStore(dbName)
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
        store = PostgresAttributeStore(dbName)

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

        val updated = Attribute("Updated", a1.id)
        store.store(updated)

        store.all().map { it.name } shouldBe setOf(a2.name, updated.name)
        store.all().map { it.id } shouldBe setOf(a2.id, updated.id)

        // Rebuild and check again.
        store = PostgresAttributeStore(dbName)
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
        store = PostgresAttributeStore(dbName)
        store.all().size shouldBe  100
    }

    @Test
    fun load() {
        val a1 = Attribute("Glucose", 1)
        val a2 = Attribute("LDL", 2)
        val a3 = Attribute("HDL", 3)
        store.load(setOf(a1, a2, a3))
        store.all() shouldBe setOf(a1, a2, a3)

        // Rebuild and check again.
        store = PostgresAttributeStore(dbName)
        store.all() shouldBe setOf(a1, a2, a3)
    }

    @Test
    fun `cannot load if there are already attributes`() {
        store.create("Whatever")
        shouldThrow<IllegalArgumentException> {
            store.load(setOf())
        }.message shouldBe "Cannot load attributes if there are are some stored already."
    }
}