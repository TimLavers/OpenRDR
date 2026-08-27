package io.rippledown.persistence.postgres

import io.rippledown.model.KBInfo
import io.rippledown.persistence.PersistentKB

const val TEST_DATABASE_NAME = "rdr_test"

abstract class PostgresStoreTest {
    val dbName = TEST_DATABASE_NAME
    var postgresKB: PersistentKB = TestDatabase.kbFor(javaClass.name)

    open fun reload() {
        postgresKB = TestDatabase.reloaded()
    }

    fun clearTables() = clearTablesOf(dbName)
}

/**
 * The database used by the [PostgresStoreTest] subclasses, created once per test
 * class rather than once per test method: creating a Postgres database copies
 * template1, which dominated the runtime of these tests. Isolation between the
 * tests of a class comes from [PostgresStoreTest.clearTables] instead. The
 * stores hold no state of their own, so one [PersistentKB] serves every test of
 * a class.
 *
 * JUnit runs test classes one at a time, so keying on the test class gives
 * precisely one creation per class. A fresh provider is built each time, as
 * PostgresPersistenceProviderTest drops the system database that the provider's
 * id store lives in; constructing one recreates it.
 */
private object TestDatabase {
    private val kbInfo = KBInfo(TEST_DATABASE_NAME, TEST_DATABASE_NAME)
    private var owner: String? = null
    private lateinit var provider: PostgresPersistenceProvider
    private lateinit var kb: PersistentKB

    fun kbFor(testClass: String): PersistentKB {
        if (owner != testClass) {
            provider = PostgresPersistenceProvider()
            provider.destroyKBPersistence(kbInfo)
            provider.createKBPersistence(kbInfo)
            kb = provider.kbPersistence(kbInfo.id)
            owner = testClass
        }
        return kb
    }

    fun reloaded(): PersistentKB = provider.kbPersistence(kbInfo.id)
}