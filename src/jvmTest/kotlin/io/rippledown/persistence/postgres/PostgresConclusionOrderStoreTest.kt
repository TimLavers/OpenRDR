package io.rippledown.persistence.postgres

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.rippledown.persistence.OrderStore
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class PostgresConclusionOrderStoreTest : PostgresStoreTest() {
    private lateinit var store: OrderStore

    override fun tablesInDropOrder() = listOf(PostgresConclusionOrderStore.TABLE_NAME)

    @BeforeTest
    fun setup() {
        dropTable()
        store = postgresKB.conclusionOrderStore()
    }

    @AfterTest
    fun cleanup() {
        dropDB(dbName)
    }

    override fun reload() {
        super.reload()
        store = postgresKB.conclusionOrderStore()
    }

    @Test
    fun `initially empty`() {
        store.idToIndex() shouldBe emptyMap()
    }

    @Test
    fun store() {
        store.store(1, 10)
        store.store(2, 11)
        store.store(3, 12)

        store.idToIndex() shouldBe mapOf(1 to 10, 2 to 11, 3 to 12)

        // Rebuild and check.
        reload()
        store.idToIndex() shouldBe mapOf(1 to 10, 2 to 11, 3 to 12)
    }

    @Test
    fun `load not allowed if non-empty`() {
        store.store(10, 10)

        shouldThrow<IllegalArgumentException> {
            store.load(mapOf(1 to 1))
        }.message shouldBe "Cannot load conclusion order store if it is non-empty."
    }

    @Test
    fun load() {
        val loaded = mapOf(1 to 5, 2 to 4, 3 to 3, 4 to 2, 5 to 1)
        store.load(loaded)
        store.idToIndex() shouldBe loaded

        // Rebuild and check.
        reload()
        store.idToIndex() shouldBe loaded
    }
}