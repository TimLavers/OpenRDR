package io.rippledown.persistence.postgres

import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.sql.Database
import kotlin.test.BeforeTest
import kotlin.test.Test

class PostgresKBIdsTest {
    private val dbName = "rdr_test"
    private val db: Database
    private lateinit var postgresKBIds: PostgresKBIds
    private val k1 = "key1"
    private val k2 = "key2"
    private val k3 = "key3"

    init {
        ConnectionProvider.systemConnection().use {
            it.createStatement().executeUpdate("DROP DATABASE IF EXISTS $dbName")
        }
        ConnectionProvider.systemConnection().use {
            it.createStatement().executeUpdate("CREATE DATABASE $dbName")
        }
        db = Database.connect({ConnectionProvider.connection(dbName)})
    }

    @BeforeTest
    fun setup() {
        // Delete the table.
        ConnectionProvider.systemConnection().use {
            it.createStatement().executeUpdate("DROP TABLE IF EXISTS $KB_IDS_TABLE")
        }
        postgresKBIds = PostgresKBIds(db)
    }

    @Test
    fun `initially empty`() {
        postgresKBIds.data() shouldBe emptyMap()
    }

    @Test
    fun put() {
        postgresKBIds.add(k1, true)
        postgresKBIds.add(k2, false)
        postgresKBIds.add(k3, true)

        postgresKBIds.data() shouldBe mapOf(k1 to true, k2 to false, k3 to true)

        // Rebuild.
        postgresKBIds = PostgresKBIds(db)
        postgresKBIds.data() shouldBe mapOf(k1 to true, k2 to false, k3 to true)
    }

    @Test
    fun remove() {
        postgresKBIds.add(k1, true)
        postgresKBIds.add(k2, false)
        postgresKBIds.add(k3, true)

        postgresKBIds.remove(k2)
        postgresKBIds.data() shouldBe mapOf(k1 to true, k3 to true)

        // Rebuild.
        postgresKBIds = PostgresKBIds(db)
        postgresKBIds.data() shouldBe mapOf(k1 to true, k3 to true)
    }

    @Test
    fun `remove and re-add`() {
        postgresKBIds.add(k1, false)
        postgresKBIds.add(k2, true)
        postgresKBIds.add(k3, true)

        postgresKBIds.remove(k2)
        postgresKBIds.data() shouldBe mapOf(k1 to false, k3 to true)
        postgresKBIds.add(k2, true)
        postgresKBIds.data() shouldBe mapOf(k1 to false, k2 to true, k3 to true)

        // Rebuild.
        postgresKBIds = PostgresKBIds(db)
        postgresKBIds.data() shouldBe mapOf(k1 to false, k2 to true, k3 to true)
    }

    @Test
    fun `attempt to remove unknown value`() {
        postgresKBIds.add(k1, false)
        postgresKBIds.add(k2, true)
        postgresKBIds.remove(k3)
        postgresKBIds.data() shouldBe mapOf(k1 to false, k2 to true)
    }
}