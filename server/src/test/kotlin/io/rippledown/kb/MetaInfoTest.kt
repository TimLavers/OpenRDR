package io.rippledown.kb

import io.kotest.matchers.shouldBe
import io.rippledown.model.*
import io.rippledown.model.condition.*
import kotlin.test.BeforeTest
import kotlin.test.Test

class MetaInfoTest {
    private lateinit var metaInfo: MetaInfo

    @BeforeTest
    fun setup() {
        metaInfo = MetaInfo()
    }

    @Test
    fun descriptionTest() {
        metaInfo.getDescription() shouldBe ""
        val newDescription = "A truly fine KB!"
        metaInfo.setDescription(newDescription)
        metaInfo.getDescription() shouldBe newDescription
    }
}