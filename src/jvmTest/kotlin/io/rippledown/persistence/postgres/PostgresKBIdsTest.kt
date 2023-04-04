package io.rippledown.persistence.postgres

import io.kotest.matchers.shouldBe
import java.sql.*
import kotlin.test.BeforeTest
import kotlin.test.Test

class PostgresKBIdsTest {
    private val dbName = "rdr_test"
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
    }

    @BeforeTest
    fun setup() {
        // Delete the table.
        ConnectionProvider.systemConnection().use {
            it.createStatement().executeUpdate("DROP TABLE IF EXISTS $KB_IDS_TABLE")
        }
        postgresKBIds = PostgresKBIds(dbName)
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
        postgresKBIds = PostgresKBIds(dbName)
        postgresKBIds.data() shouldBe mapOf(k1 to true, k2 to false, k3 to true)
    }
}