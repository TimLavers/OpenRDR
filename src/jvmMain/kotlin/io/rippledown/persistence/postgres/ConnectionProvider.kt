package io.rippledown.persistence.postgres

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection

const val OPEN_RDR_DB_URL = "OPEN_RDR_DB_URL"
const val OPEN_RDR_DB_USER = "OPEN_RDR_DB_USER"
const val OPEN_RDR_DB_PASSWORD = "OPEN_RDR_DB_PASSWORD"

object ConnectionProvider {
    val dbToDataSource = mutableMapOf<String, HikariDataSource>()

    fun systemConnection(): Connection = connection("postgres")

    private fun dataSource(dbName: String): HikariDataSource {
        println("creating datasource for dbName = ${dbName}")
        val config = HikariConfig().apply {
            driverClassName = "org.postgresql.Driver"
            jdbcUrl = connectionString(dbName)
            username = dbUser()
            password = dbPassword()
            maximumPoolSize = 10
            isAutoCommit = true
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            connectionTimeout = 1000
            validate()
        }
        println("********dbToDataSourcefor  = $dbName")
        return HikariDataSource(config)
    }

    fun mappedDataSource(dbName: String): HikariDataSource {
        val dataSource = dbToDataSource.getOrPut(dbName) {
            dataSource(dbName)
        }
        return dataSource
    }

    fun connection(dbName: String): Connection {
        return mappedDataSource(dbName).getConnection()
    }

    fun dbUser() = System.getenv(OPEN_RDR_DB_USER) ?: "postgres"

    fun dbPassword() = System.getenv(OPEN_RDR_DB_PASSWORD) ?: "postgres"

    fun connectionString(dbName: String): String {
        println("connection string for  ${dbName}")
        val url = System.getenv(OPEN_RDR_DB_URL)
        return if (url != null) "$url$dbName" else "jdbc:postgresql://localhost:5432/$dbName"
    }
}