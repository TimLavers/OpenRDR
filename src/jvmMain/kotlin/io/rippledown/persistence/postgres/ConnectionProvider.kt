package io.rippledown.persistence.postgres

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import java.sql.Connection

const val OPEN_RDR_DB_URL = "OPEN_RDR_DB_URL"
const val OPEN_RDR_DB_USER = "OPEN_RDR_DB_USER"
const val OPEN_RDR_DB_PASSWORD = "OPEN_RDR_DB_PASSWORD"

object ConnectionProvider {

    fun systemConnection(): Connection = connection("postgres")


    fun database(dbName: String) = Database.connect(dataSource(dbName))


    private fun dataSource(dbName: String): HikariDataSource {
        val config = HikariConfig().apply {
            driverClassName = "org.postgresql.Driver"
            jdbcUrl = connectionString(dbName)
            username = dbUser()
            password = dbPassword()
            maximumPoolSize = 10
            isAutoCommit = true
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }
        return HikariDataSource(config)
    }

    fun connection(dbName: String) = dataSource(dbName).getConnection()

    fun dbUser() = System.getenv(OPEN_RDR_DB_USER) ?: "postgres"

    fun dbPassword() = System.getenv(OPEN_RDR_DB_PASSWORD) ?: "postgres"

    fun connectionString(dbName: String): String {
        val url = System.getenv(OPEN_RDR_DB_URL)
        return if (url != null) "$url$dbName" else "jdbc:postgresql://localhost:5432/$dbName"
    }
}