package io.rippledown.persistence.postgres

import io.rippledown.model.KBInfo
import io.rippledown.persistence.PersistentKB

abstract class PostgresStoreTest {
    val dbName = "rdr_test"
    private val kbInfo = KBInfo(dbName, dbName)
    private val postgresPersistenceProvider = PostgresPersistenceProvider()
    var postgresKB: PersistentKB

    init {
        postgresPersistenceProvider.destroyKBPersistence(kbInfo)
        postgresKB = postgresPersistenceProvider.createKBPersistence(kbInfo)
    }

    open fun reload() {
        postgresKB = postgresPersistenceProvider.kbPersistence(kbInfo.id)
    }

    fun dropTable() {
        ConnectionProvider.systemConnection().use { connection ->
            tablesInDropOrder().forEach { table ->
                connection.createStatement().executeUpdate("DROP TABLE IF EXISTS $table")
            }
        }
    }

    abstract fun tablesInDropOrder(): List<String>
}