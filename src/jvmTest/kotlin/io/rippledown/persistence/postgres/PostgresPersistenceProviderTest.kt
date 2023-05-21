package io.rippledown.persistence.postgres

import io.kotest.matchers.shouldBe
import io.rippledown.model.KBInfo
import kotlin.test.BeforeTest
import kotlin.test.Test

class PostgresPersistenceProviderTest {

    private val glucoseInfo = KBInfo("glucose_test_db", "Glucose")
    private val lipidsInfo = KBInfo("lipids_test_db", "Lipids")

    @BeforeTest
    fun deleteAllDBs() {
        dropDB(glucoseInfo.id)
        dropDB(lipidsInfo.id)
        dropDB(SYSTEM_DB_NAME)
    }

//    @Test
//    fun cleanupAllDBs() {
//        val ppp = PostgresPersistenceProvider()
//        val allDBs = ppp.allDatabasesInSystem()
//        println("allDBs BEFORE = ${allDBs}")
//        allDBs.forEach {
//            println("cleanup of :$it")
//            try {
//                dropDB(it)
//            } catch (e: Exception) {
//                println("Could not delete: $it")
//            }
//        }
//        val allDBsAfter = ppp.allDatabasesInSystem()
//        println("allDBs AFTER = ${allDBsAfter}")
//    }

    @Test
    fun idStore() {
        val ppp = PostgresPersistenceProvider()
        val idStore = ppp.idStore()
        idStore.data() shouldBe emptyMap()
        val drinks = mapOf("Coffee" to true, "Tea" to true, "Cocoa" to false)
        drinks.forEach{idStore.add(it.key, it.value)}

        idStore.data() shouldBe drinks

        PostgresPersistenceProvider().idStore().data() shouldBe drinks
    }

    @Test
    fun createKB() {
        val ppp = PostgresPersistenceProvider()
        val glucoseKB = ppp.createKBPersistence(glucoseInfo)
        glucoseKB.kbInfo().name shouldBe glucoseInfo.name
        glucoseKB.kbInfo().id shouldBe glucoseInfo.id
    }

    @Test
    fun openKB() {
        val ppp = PostgresPersistenceProvider()
        ppp.createKBPersistence(glucoseInfo)
        val glucoseKB = ppp.kbPersistence(glucoseInfo.id)
        glucoseKB.kbInfo().name shouldBe glucoseInfo.name
        glucoseKB.kbInfo().id shouldBe glucoseInfo.id
    }
}