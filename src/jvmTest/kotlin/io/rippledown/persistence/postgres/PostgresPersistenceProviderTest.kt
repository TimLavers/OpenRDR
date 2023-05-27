package io.rippledown.persistence.postgres

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.rippledown.model.KBInfo
import kotlin.test.BeforeTest
import kotlin.test.Test

class PostgresPersistenceProviderTest {

    private val glucoseInfo = KBInfo("glucose_test_db", "Glucose")
    private val lipidsInfo = KBInfo("lipids_test_db", "Lipids")
    private lateinit var ppp: PostgresPersistenceProvider

    @BeforeTest
    fun deleteAllDBs() {
        dropDB(glucoseInfo.id)
        dropDB(lipidsInfo.id)
        dropDB(SYSTEM_DB_NAME)
        ppp = PostgresPersistenceProvider()
    }

//    @Test
    fun cleanupAllDBs() {
        val allDBs = allDatabasesInSystem()
        println("allDBs BEFORE = ${allDBs}")
        allDBs.forEach {
            println("cleanup of :$it")
            try {
                dropDB(it)
            } catch (e: Exception) {
                println("Could not delete: $it")
            }
        }
        val allDBsAfter = allDatabasesInSystem()
        println("allDBs AFTER = $allDBsAfter")
    }

    @Test
    fun allDBS() {
        val allDBsAtStart = allDatabasesInSystem()
        allDBsAtStart shouldNotContain glucoseInfo.id
        allDBsAtStart shouldNotContain lipidsInfo.id
        ppp.createKBPersistence(glucoseInfo)
        ppp.createKBPersistence(lipidsInfo)
        allDatabasesInSystem() shouldContain  glucoseInfo.id
        allDatabasesInSystem() shouldContain  lipidsInfo.id
        allDatabasesInSystem().size shouldBe allDBsAtStart.size + 2
    }

    @Test
    fun idStore() {
        val idStore = ppp.idStore()
        idStore.data() shouldBe emptyMap()
        val drinks = mapOf("Coffee" to true, "Tea" to true, "Cocoa" to false)
        drinks.forEach{idStore.add(it.key, it.value)}

        idStore.data() shouldBe drinks

        PostgresPersistenceProvider().idStore().data() shouldBe drinks
    }

    @Test
    fun createKBPersistence() {
        ppp.idStore().data() shouldBe emptyMap()

        val glucoseKB = ppp.createKBPersistence(glucoseInfo)
        glucoseKB.kbInfo().name shouldBe glucoseInfo.name
        glucoseKB.kbInfo().id shouldBe glucoseInfo.id
        ppp.idStore().data() shouldBe mapOf(glucoseInfo.id to true)

        val lipidsKB = ppp.createKBPersistence(lipidsInfo)
        lipidsKB.kbInfo().name shouldBe lipidsInfo.name
        lipidsKB.kbInfo().id shouldBe lipidsInfo.id
        ppp.idStore().data() shouldBe mapOf(glucoseInfo.id to true, lipidsInfo.id to true)
    }

    @Test
    fun destroyKBPersistence() {
        ppp.createKBPersistence(glucoseInfo)
        ppp.createKBPersistence(lipidsInfo)
        ppp.idStore().data() shouldBe mapOf(glucoseInfo.id to true, lipidsInfo.id to true)

        ppp.destroyKBPersistence(lipidsInfo)
        ppp.idStore().data() shouldBe mapOf(glucoseInfo.id to true)

        ppp.destroyKBPersistence(glucoseInfo)
        ppp.idStore().data() shouldBe mapOf()

        PostgresPersistenceProvider().idStore().data() shouldBe mapOf()
    }

    @Test
    fun openKB() {
        ppp.createKBPersistence(glucoseInfo)
        val glucoseKB = ppp.kbPersistence(glucoseInfo.id)
        glucoseKB.kbInfo().name shouldBe glucoseInfo.name
        glucoseKB.kbInfo().id shouldBe glucoseInfo.id
    }
}