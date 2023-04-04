package io.rippledown.persistence.postgres

import io.kotest.matchers.shouldBe
import java.sql.*
import kotlin.test.BeforeTest
import kotlin.test.Test


class PostgresPersistenceProviderTest {

    @BeforeTest
    fun deleteAllDBs() {
        val connection = ConnectionProvider.systemConnection()
        val statement: Statement = connection.createStatement()

//        statement.("select datname from postgres")
        val query = "select datname from pg_database"
        try {
            connection.createStatement().use { stmt ->
                val rs: ResultSet = stmt.executeQuery(query)
                while (rs.next()) {
                    val name = rs.getString(1)
                    println(name)
                }
            }
        } catch (e: SQLException) {
           e.printStackTrace()
        } finally {
            connection.close()
        }
    }

    @Test
    fun idStore() {
        val ppp = PostgresPersistenceProvider()
        val idStore = ppp.idStore()
        idStore.data() shouldBe emptyMap()
    }
}