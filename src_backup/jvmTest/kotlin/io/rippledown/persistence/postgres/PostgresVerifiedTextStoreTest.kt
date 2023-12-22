package io.rippledown.persistence.postgres

import io.kotest.matchers.shouldBe
import io.rippledown.persistence.VerifiedTextStore
import kotlin.test.BeforeTest
import kotlin.test.Test

class PostgresVerifiedTextStoreTest : PostgresStoreTest() {
    private val wineComment = "Time for a strengthening glass of cab sav!"

    private lateinit var store: VerifiedTextStore

    override fun tablesInDropOrder() = listOf(PostgresVerifiedTextStore.TABLE_NAME)

    @BeforeTest
    fun setup() {
        dropTable()
        store = postgresKB.verifiedTextStore()
    }

    override fun reload() {
        super.reload()
        store = postgresKB.verifiedTextStore()
    }

    @Test
    fun noVerifiedTextForId() {
        store.get(42) shouldBe null
    }

    @Test
    fun getVerifiedText() {
        store.put(42, wineComment)
        store.get(42) shouldBe wineComment

        reload()
        store.get(42) shouldBe wineComment
    }
}