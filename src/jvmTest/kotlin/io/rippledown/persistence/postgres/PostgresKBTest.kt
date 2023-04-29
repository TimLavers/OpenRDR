package io.rippledown.persistence.postgres

import io.kotest.matchers.shouldBe
import io.rippledown.model.KBInfo
import io.rippledown.model.condition.IsLow
import io.rippledown.persistence.PersistentKB
import kotlin.test.BeforeTest
import kotlin.test.Test

class PostgresKBTest {
    private val glucoseDB = "glucose_test"
    private val thyroidsDB = "thyroids_test"
    private val glucoseInfo = KBInfo( glucoseDB, "Glucose")
    private lateinit var glucoseKB: PersistentKB

    @BeforeTest
    fun setup() {
        dropDB(glucoseDB)
        dropDB(thyroidsDB)
        glucoseKB = createPostgresKB(glucoseInfo)
    }

    @Test
    fun create() {
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
        val age = glucoseKB.attributeStore().create("Age")
        val sex = glucoseKB.attributeStore().create("Sex")

        glucoseKB.attributeStore().all() shouldBe setOf(age, sex)

        // Rebuild and check.
        glucoseKB = PostgresKB(glucoseInfo.id)
        glucoseKB.attributeStore().all() shouldBe setOf(age, sex)
    }

    @Test
    fun attributeOrderStore() {
        glucoseKB.attributeOrderStore().store(1, 345)
        glucoseKB.attributeOrderStore().store(5, 99)

        glucoseKB.attributeOrderStore().idToIndex() shouldBe mapOf(1 to 345, 5 to 99)

        // Rebuild and check.
        glucoseKB = PostgresKB(glucoseInfo.id)
        glucoseKB.attributeOrderStore().idToIndex() shouldBe mapOf(1 to 345, 5 to 99)
    }

    @Test
    fun conclusionStore() {
        val text = "Raining today!"
        val conclusion = glucoseKB.conclusionStore().create(text)
        glucoseKB.conclusionStore().all() shouldBe setOf(conclusion)

        glucoseKB = PostgresKB(glucoseInfo.id)
        glucoseKB.conclusionStore().all() shouldBe setOf(conclusion)
    }

    @Test
    fun conditionStore() {
        glucoseKB.conditionStore().all() shouldBe emptySet()
        val glucose = glucoseKB.attributeStore().create("Glucose")
        val templateCondition = IsLow(null, glucose)
        val createdCondition = glucoseKB.conditionStore().create(templateCondition)
        glucoseKB.conditionStore().all() shouldBe setOf(createdCondition)

        glucoseKB = PostgresKB(glucoseInfo.id)
        glucoseKB.conditionStore().all() shouldBe setOf(createdCondition)
    }
}