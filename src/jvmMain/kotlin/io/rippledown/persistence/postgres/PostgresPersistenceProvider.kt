package io.rippledown.persistence.postgres

import io.rippledown.model.KBInfo
import io.rippledown.persistence.PersistenceProvider
import io.rippledown.persistence.PersistentKB
import org.jetbrains.exposed.sql.Database
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.sql.ResultSet

const val SYSTEM_DB_NAME = "open_rdr"

fun allDatabasesInSystem(): Set<String> {
    fun isSpecialPostgresDBName(name: String): Boolean {
        return when(name) {
            "postgres" -> true
            "template0" -> true
            "template1" -> true
            else -> false
        }
    }
    val result = mutableSetOf<String>()
    ConnectionProvider.systemConnection().use {
        it.createStatement().use { stmt ->
            val rs: ResultSet = stmt.executeQuery("SELECT datname FROM pg_database")
            while (rs.next()) {
                val dbName = rs.getString(1)
                if (!isSpecialPostgresDBName(dbName)) {
                    result.add(dbName)
                }
            }
        }
    }
    return result
}

fun createDatabase(name: String) {
    ConnectionProvider.systemConnection().use {
        it.createStatement().executeUpdate("CREATE DATABASE $name")
    }
}

class PostgresPersistenceProvider: PersistenceProvider {
    private val logger: Logger = LoggerFactory.getLogger("rdr")
    private val systemDB: Database
    private val idStore: PostgresKBIds

    init {
        logger.info("Initialising PostgresPersistenceProvider")
        val currentDatabases = allDatabasesInSystem()
        logger.info("All DBs in system: $currentDatabases.")
        if (currentDatabases.contains(SYSTEM_DB_NAME)) {
            logger.info("System DB already exists.")
        } else {
            createSystemDB()
            logger.info("System DB created.")
        }
        systemDB = Database.connect({ConnectionProvider.connection(SYSTEM_DB_NAME)})
        logger.info("About to create PostgresKBIds...")
        idStore = PostgresKBIds(systemDB)
        logger.info("PostgresKBIds created.")
    }

    override fun idStore() = idStore

    override fun kbPersistence(id: String): PersistentKB {
        return PostgresKB(id)
    }

    override fun createKBPersistence(kbInfo: KBInfo): PersistentKB {
        idStore.add(kbInfo.id, true)
        return createPostgresKB(kbInfo)
    }

    override fun destroyKBPersistence(kbInfo: KBInfo) {
        ConnectionProvider.systemConnection().use {
            it.createStatement().executeUpdate("DROP DATABASE IF EXISTS ${kbInfo.id}")
        }
        idStore.remove(kbInfo.id)
    }

    private fun createSystemDB()= createDatabase(SYSTEM_DB_NAME)
}