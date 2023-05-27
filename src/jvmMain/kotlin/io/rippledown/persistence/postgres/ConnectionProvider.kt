package io.rippledown.persistence.postgres

import org.jetbrains.exposed.sql.Database
import java.sql.Connection
import java.sql.DriverManager

const val OPEN_RDR_DB_URL = "OPEN_RDR_DB_URL"
const val OPEN_RDR_DB_USER = "OPEN_RDR_DB_USER"
const val OPEN_RDR_DB_PASSWORD = "OPEN_RDR_DB_PASSWORD"

object ConnectionProvider {

    fun systemConnection(): Connection = connection("postgres")

    fun connection(dbName: String): Connection = DriverManager.getConnection(connectionString(dbName), dbUser(), dbPassword())

    fun database(dbName: String) = Database.connect(connectionString(dbName),
        driver = "org.postgresql.Driver",
        password = dbUser(),
        user = dbPassword())

    private fun dbUser() = System.getenv(OPEN_RDR_DB_USER) ?: "postgres"

    private fun dbPassword() = System.getenv(OPEN_RDR_DB_PASSWORD) ?: "postgres"

    private fun connectionString(dbName: String) = System.getenv(OPEN_RDR_DB_URL) ?: "jdbc:postgresql://localhost:5432/$dbName"
}