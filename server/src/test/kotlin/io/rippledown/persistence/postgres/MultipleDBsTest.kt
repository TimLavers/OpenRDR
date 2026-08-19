package io.rippledown.persistence.postgres

import io.rippledown.model.KBInfo
import io.rippledown.persistence.PersistentKB

open class MultipleDBsTest {
    lateinit var kb1: PersistentKB
    lateinit var kb2: PersistentKB

    open fun setup() {
        val kbs = TestDatabases.kbsFor(javaClass.name)
        kb1 = kbs.first
        kb2 = kbs.second
        TestDatabases.clearTables()
    }

    open fun reload() {
        val kbs = TestDatabases.reloaded()
        kb1 = kbs.first
        kb2 = kbs.second
    }
}

/**
 * The two databases used by the [MultipleDBsTest] subclasses, created once per
 * test class rather than once per test method: creating a Postgres database
 * copies template1, which dominated the runtime of these tests. Isolation
 * between the tests of a class comes from [clearTables] instead. See
 * [PostgresStoreTest] for the same arrangement for the single-database tests.
 */
private object TestDatabases {
    private val kbInfo1 = KBInfo("glucose", "Glucose")
    private val kbInfo2 = KBInfo("thyroids", "Thyroids")
    private var owner: String? = null
    private lateinit var provider: PostgresPersistenceProvider
    private lateinit var kbs: Pair<PersistentKB, PersistentKB>

    fun kbsFor(testClass: String): Pair<PersistentKB, PersistentKB> {
        if (owner != testClass) {
            // A fresh provider, as PostgresPersistenceProviderTest drops the system database it uses.
            provider = PostgresPersistenceProvider()
            provider.destroyKBPersistence(kbInfo1)
            provider.destroyKBPersistence(kbInfo2)
            provider.createKBPersistence(kbInfo1)
            provider.createKBPersistence(kbInfo2)
            kbs = reloaded()
            owner = testClass
        }
        return kbs
    }

    fun reloaded(): Pair<PersistentKB, PersistentKB> =
        provider.kbPersistence(kbInfo1.id) to provider.kbPersistence(kbInfo2.id)

    fun clearTables() {
        clearTablesOf(kbInfo1.id)
        clearTablesOf(kbInfo2.id)
    }
}