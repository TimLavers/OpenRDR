package io.rippledown.persistence.postgres

import io.kotest.matchers.shouldBe
import io.rippledown.model.KBInfo
import kotlin.test.BeforeTest
import kotlin.test.Test

class PostgresKBTest {
    private val glucose_db = "glucose_test"
    private val thyroids_db = "thyroids_test"

    @BeforeTest
    fun setup() {
        dropDB(glucose_db)
        dropDB(thyroids_db)
    }

    @Test
    fun create() {
        val glucoseInfo = KBInfo( glucose_db, "Glucose")
        val created = createPostgresKB(glucoseInfo)
        created.kbInfo().id shouldBe glucoseInfo.id
        created.kbInfo().name shouldBe glucoseInfo.name
        created.attributeStore().all() shouldBe emptySet()

        val thyroidsInfo = KBInfo(thyroids_db, "Thyroids")
        val createdThyroids = createPostgresKB(thyroidsInfo)
        createdThyroids.kbInfo().id shouldBe thyroidsInfo.id
        createdThyroids.kbInfo().name shouldBe thyroidsInfo.name
        createdThyroids.attributeStore().all() shouldBe emptySet()
    }

    @Test
    fun attributeStore() {
        val glucoseInfo = KBInfo( glucose_db, "Glucose")
        var kb = createPostgresKB(glucoseInfo)
        val age = kb.attributeStore().create("Age")
        val sex = kb.attributeStore().create("Sex")

        kb.attributeStore().all() shouldBe setOf(age, sex)

        // Rebuild and check.
        kb = PostgresKB(glucoseInfo.id)
        kb.attributeStore().all() shouldBe setOf(age, sex)
    }
}