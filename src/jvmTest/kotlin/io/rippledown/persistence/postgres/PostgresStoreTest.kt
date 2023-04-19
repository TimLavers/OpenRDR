package io.rippledown.persistence.postgres

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import kotlin.IllegalArgumentException
import kotlin.test.BeforeTest
import kotlin.test.Test

abstract class PostgresStoreTest {
    val dbName = "rdr_test"
    private lateinit var store: PostgresAttributeStore

    init {
        ConnectionProvider.systemConnection().use {
            it.createStatement().executeUpdate("DROP DATABASE IF EXISTS $dbName")
        }
        ConnectionProvider.systemConnection().use {
            it.createStatement().executeUpdate("CREATE DATABASE $dbName")
        }
    }

    fun dropTable() {
        ConnectionProvider.systemConnection().use {
            it.createStatement().executeUpdate("DROP TABLE IF EXISTS ${tableName()}")
        }
    }

    abstract fun tableName(): String
}