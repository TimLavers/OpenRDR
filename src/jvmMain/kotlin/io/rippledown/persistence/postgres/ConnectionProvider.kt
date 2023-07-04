package io.rippledown.persistence.postgres

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection

const val OPEN_RDR_DB_URL = "OPEN_RDR_DB_URL"
const val OPEN_RDR_DB_USER = "OPEN_RDR_DB_USER"
const val OPEN_RDR_DB_PASSWORD = "OPEN_RDR_DB_PASSWORD"

object ConnectionProvider {

    fun systemConnection(): Connection = connection("postgres")

    /*
        fun connection(dbName: String): Connection = DriverManager.getConnection(connectionString(dbName), dbUser(), dbPassword())

        fun database(dbName: String) = Database.connect(connectionString(dbName),
            driver = "org.postgresql.Driver",
            password = dbUser(),
            user = dbPassword())
    */

    private val dbNameToDataSource = mutableMapOf<String, HikariDataSource>()

    fun dataSource(dbName: String): HikariDataSource {
        if (!dbNameToDataSource.containsKey(dbName)) {
            logger.info("Creating connection to $dbName")
            createDataSource(dbName)
        }
        return dbNameToDataSource[dbName]!!
    }

    private fun createDataSource(dbName: String) {
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
        dbNameToDataSource.put(dbName, HikariDataSource(config))
    }

    fun connection(dbName: String) = dataSource(dbName).getConnection()

    fun dbUser() = System.getenv(OPEN_RDR_DB_USER) ?: "postgres"

    fun dbPassword() = System.getenv(OPEN_RDR_DB_PASSWORD) ?: "postgres"

    fun connectionString(dbName: String): String {
        val url = System.getenv(OPEN_RDR_DB_URL)
        return if (url != null) "$url$dbName" else "jdbc:postgresql://localhost:5432/$dbName"
    }

    fun closeConnection(dbName: String) {
//        if (dbNameToDataSource.containsKey(dbName)) {
//            logger.info("Closing connection to $dbName")
//            dbNameToDataSource[dbName]!!.close()
//            dbNameToDataSource.remove(dbName)
//        }
    }
}