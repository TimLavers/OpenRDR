package io.rippledown.persistence.postgres

import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.sql.Database
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class MultipleDBsKBIdsStoresTest {
    private val dbOne = "db_one"
    private val dbTwo = "db_two"
    private lateinit var store1: PostgresKBIds
    private lateinit var store2: PostgresKBIds

    @BeforeTest
    fun setup() {
        cleanup()
        createDatabase(dbOne)
        createDatabase(dbTwo)
        reload()
    }

    @AfterTest
    fun cleanup() {
        dropDB(dbOne)
        dropDB(dbTwo)
    }

    fun reload() {
        val db1 = Database.connect({ ConnectionProvider.connection(dbOne) })
        val db2 = Database.connect({ ConnectionProvider.connection(dbTwo) })
        store1 = PostgresKBIds(db1)
        store2 = PostgresKBIds(db2)
    }

    @Test
    fun create() {
        store1.add("a", true)
        store2.add("a", true)
        store1.add("b", false)
        store2.add("b", true)

        store1.data() shouldBe mapOf("a" to true, "b" to false)
        store2.data() shouldBe mapOf("a" to true, "b" to true)
        reload()
        store1.data() shouldBe mapOf("a" to true, "b" to false)
        store2.data() shouldBe mapOf("a" to true, "b" to true)
    }

    @Test
    fun remove() {
        store1.add("a", true)
        store2.add("a", true)
        store1.add("b", false)
        store2.add("b", true)
        store1.remove("a")
        store2.remove("b")

        store1.data() shouldBe mapOf("b" to false)
        store2.data() shouldBe mapOf("a" to true)
        reload()
        store1.data() shouldBe mapOf("b" to false)
        store2.data() shouldBe mapOf("a" to true)
    }
}