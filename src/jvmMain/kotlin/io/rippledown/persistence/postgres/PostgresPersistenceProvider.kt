package io.rippledown.persistence.postgres

import io.rippledown.model.KBInfo
import io.rippledown.persistence.PersistenceProvider
import io.rippledown.persistence.PersistentKBIds
import io.rippledown.persistence.PersistentKB
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.sql.ResultSet

const val SYSTEM_DB_NAME = "open_rdr"

class PostgresPersistenceProvider: PersistenceProvider {
    val logger: Logger = LoggerFactory.getLogger("rdr")
    private val idStore: PostgresKBIds

    init {
        logger.info("Initialising PostgresPersistenceProvider")
        val currentDatabases = allDatabasesInSystem()
        logger.info("All DBs in system: $currentDatabases.")
        if (currentDatabases.contains(SYSTEM_DB_NAME)) {
            logger.info("System DB already exists.")
        } else {
            createSystemDB()
        }
        idStore = PostgresKBIds(SYSTEM_DB_NAME)
    }

    override fun idStore(): PersistentKBIds {
        return idStore
    }

    override fun kbStore(id: String): PersistentKB {
        TODO("Not yet implemented")
    }

    override fun createKB(kbInfo: KBInfo): PersistentKB {
        TODO("Not yet implemented")
    }

    private fun allDatabasesInSystem(): Set<String> {
        val result = mutableSetOf<String>()
        ConnectionProvider.systemConnection().use {
            it.createStatement().use { stmt ->
                val rs: ResultSet = stmt.executeQuery("SELECT datname FROM pg_database")
                while (rs.next()) {
                    result.add(rs.getString(1))
                }
            }
        }
        return result
    }

    private fun createSystemDB() {
        ConnectionProvider.systemConnection().use {
            val statement = ConnectionProvider.systemConnection().createStatement()
            statement.executeUpdate("CREATE DATABASE $SYSTEM_DB_NAME")
        }
    }
}