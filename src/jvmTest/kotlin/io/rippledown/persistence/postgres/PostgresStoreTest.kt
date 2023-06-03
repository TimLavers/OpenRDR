package io.rippledown.persistence.postgres

import io.rippledown.model.KBInfo
import io.rippledown.persistence.PersistentKB
import org.jetbrains.exposed.sql.Database

abstract class PostgresStoreTest {
    val dbName = "rdr_test"
    private val kbInfo = KBInfo(dbName, dbName)
    private val postgresPersistenceProvider = PostgresPersistenceProvider()
    var postgresKB: PersistentKB
    lateinit var db: Database

    init {
        postgresPersistenceProvider.destroyKBPersistence(kbInfo)
        postgresKB = postgresPersistenceProvider.createKBPersistence(kbInfo)
    }

    open fun reload() {
        postgresKB = postgresPersistenceProvider.kbPersistence(kbInfo.id)
    }

    fun dropTable() {
        ConnectionProvider.systemConnection().use {
            it.createStatement().executeUpdate("DROP TABLE IF EXISTS ${tableName()}")
        }
    }

    abstract fun tableName(): String
}