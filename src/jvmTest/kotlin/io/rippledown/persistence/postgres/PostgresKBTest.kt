package io.rippledown.persistence.postgres

import io.kotest.matchers.shouldBe
import io.rippledown.model.KBInfo
import kotlin.test.BeforeTest
import kotlin.test.Test

class PostgresKBTest {
    private val glucoseDB = "glucose_test"
    private val thyroidsDB = "thyroids_test"

    @BeforeTest
    fun setup() {
        dropDB(glucoseDB)
        dropDB(thyroidsDB)
    }

    @Test
    fun create() {
        val glucoseInfo = KBInfo( glucoseDB, "Glucose")
        val glucoseKB = createPostgresKB(glucoseInfo)
        glucoseKB.kbInfo().id shouldBe glucoseInfo.id
        glucoseKB.kbInfo().name shouldBe glucoseInfo.name
        glucoseKB.attributeStore().all() shouldBe emptySet()
        glucoseKB.attributeOrderStore().idToIndex() shouldBe emptyMap()

        val thyroidsInfo = KBInfo(thyroidsDB, "Thyroids")
        val thyroidsKB = createPostgresKB(thyroidsInfo)
        thyroidsKB.kbInfo().id shouldBe thyroidsInfo.id
        thyroidsKB.kbInfo().name shouldBe thyroidsInfo.name
        thyroidsKB.attributeStore().all() shouldBe emptySet()
        thyroidsKB.attributeOrderStore().idToIndex() shouldBe emptyMap()
    }

    @Test
    fun attributeStore() {
        val glucoseInfo = KBInfo( glucoseDB, "Glucose")
        var kb = createPostgresKB(glucoseInfo)
        val age = kb.attributeStore().create("Age")
        val sex = kb.attributeStore().create("Sex")

        kb.attributeStore().all() shouldBe setOf(age, sex)

        // Rebuild and check.
        kb = PostgresKB(glucoseInfo.id)
        kb.attributeStore().all() shouldBe setOf(age, sex)
    }

    @Test
    fun attributeOrderStore() {
        val glucoseInfo = KBInfo( glucoseDB, "Glucose")
        var kb = createPostgresKB(glucoseInfo)
        kb.attributeOrderStore().store(1, 345)
        kb.attributeOrderStore().store(5, 99)

        kb.attributeOrderStore().idToIndex() shouldBe mapOf(1 to 345, 5 to 99)

        // Rebuild and check.
        kb = PostgresKB(glucoseInfo.id)
        kb.attributeOrderStore().idToIndex() shouldBe mapOf(1 to 345, 5 to 99)
    }
}