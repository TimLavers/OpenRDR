package io.rippledown.kb

import io.kotest.matchers.shouldBe
import io.rippledown.persistence.KeyValueStore
import io.rippledown.persistence.inmemory.InMemoryKeyValueStore
import kotlin.test.BeforeTest
import kotlin.test.Test

class MetaInfoTest {
    private lateinit var metaInfo: MetaInfo
    private lateinit var keyValueStore: KeyValueStore

    @BeforeTest
    fun setup() {
        keyValueStore = InMemoryKeyValueStore()
        metaInfo = MetaInfo(keyValueStore)
    }

    @Test
    fun `description set to blank if not in store`() {
        metaInfo.getDescription() shouldBe ""
        keyValueStore.all().size shouldBe 1
        keyValueStore.all().first().value shouldBe ""
    }

    @Test
    fun descriptionTest() {
        val newDescription = "A truly fine KB!"
        metaInfo.setDescription(newDescription)
        metaInfo.getDescription() shouldBe newDescription
        metaInfo = MetaInfo(keyValueStore)
        metaInfo.getDescription() shouldBe newDescription
    }

    @Test
    fun `all data`() {
        val newDescription = "A truly fine KB!"
        metaInfo.setDescription(newDescription)
        with(metaInfo.allMetaInfo()) {
            size shouldBe 1
            first().key shouldBe DESCRIPTION_KEY
            first().value shouldBe newDescription
        }
    }
}