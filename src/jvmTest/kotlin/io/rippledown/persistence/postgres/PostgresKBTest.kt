package io.rippledown.persistence.postgres

import io.kotest.matchers.shouldBe
import io.rippledown.model.KBInfo
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test

class PostgresKBTest {
    private val glucose = "glucose_test"
    private val thyroids = "thyroids_test"

    @BeforeTest
    fun setup() {
        refreshDatabase(glucose)
        refreshDatabase(thyroids)
    }

    @Test
    fun create() {
        val glucoseInfo = KBInfo(UUID.randomUUID().toString(), glucose)
        val created = createPostgresKb(glucose, glucoseInfo)
        created.kbInfo().id shouldBe glucoseInfo.id
        created.kbInfo().name shouldBe glucoseInfo.name

        val thyroidsInfo = KBInfo(UUID.randomUUID().toString(), thyroids)
        val createdThyroids = createPostgresKb(thyroids, thyroidsInfo)
        createdThyroids.kbInfo().id shouldBe thyroidsInfo.id
        createdThyroids.kbInfo().name shouldBe thyroidsInfo.name
    }
}