package io.rippledown.persistence.postgres

import io.kotest.matchers.shouldBe
import io.rippledown.model.KBInfo
import org.jetbrains.exposed.sql.SchemaUtils.dropDatabase
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

        val thyroidsInfo = KBInfo(thyroids_db, "Thyroids")
        val createdThyroids = createPostgresKB(thyroidsInfo)
        createdThyroids.kbInfo().id shouldBe thyroidsInfo.id
        createdThyroids.kbInfo().name shouldBe thyroidsInfo.name
    }
}