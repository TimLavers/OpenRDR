package io.rippledown.persistence.postgres

import io.rippledown.model.KBInfo
import io.rippledown.persistence.PersistentKB

open class MultipleDBsTest {
    private val postgresPersistenceProvider = PostgresPersistenceProvider()
    private val kbInfo1 = KBInfo("glucose", "Glucose")
    private val kbInfo2 = KBInfo("thyroids", "Thyroids")
    lateinit var kb1: PersistentKB
    lateinit var kb2: PersistentKB

    open fun setup() {
        kb1 = postgresPersistenceProvider.createKBPersistence(kbInfo1)
        kb2 = postgresPersistenceProvider.createKBPersistence(kbInfo2)
    }

    open fun cleanup() {
        postgresPersistenceProvider.destroyKBPersistence(kbInfo1)
        postgresPersistenceProvider.destroyKBPersistence(kbInfo2)
    }

    open fun reload() {
        kb1 = postgresPersistenceProvider.kbPersistence(kbInfo1.id)
        kb2 = postgresPersistenceProvider.kbPersistence(kbInfo2.id)
    }
}