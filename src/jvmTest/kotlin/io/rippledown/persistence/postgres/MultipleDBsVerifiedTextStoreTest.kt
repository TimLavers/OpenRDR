package io.rippledown.persistence.postgres

import io.kotest.matchers.shouldBe
import io.rippledown.persistence.VerifiedTextStore
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class MultipleDBsVerifiedTextStoreTest : MultipleDBsTest() {
    private val wineComment = "Time for a strengthening glass of cab sav!"

    private lateinit var store1: VerifiedTextStore
    private lateinit var store2: VerifiedTextStore

    @BeforeTest
    override fun setup() {
        cleanup()
        super.setup()
        store1 = kb1.verifiedTextStore()
        store2 = kb2.verifiedTextStore()
    }

    override fun reload() {
        super.reload()
        store1 = kb1.verifiedTextStore()
        store2 = kb2.verifiedTextStore()
    }

    @AfterTest
    override fun cleanup() {
        super.cleanup()
    }

    @Test
    fun noVerifiedTextForId() {
        store1.get(42) shouldBe null
        store2.get(42) shouldBe null
    }

    @Test
    fun getVerifiedText() {
        store1.put(42, wineComment)
        store1.get(42) shouldBe wineComment
        store2.get(42) shouldBe null

        reload()
        store1.get(42) shouldBe wineComment
        store2.get(42) shouldBe null
    }
}