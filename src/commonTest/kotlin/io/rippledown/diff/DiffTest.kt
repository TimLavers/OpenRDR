package io.rippledown.diff

import io.kotest.matchers.shouldBe
import io.rippledown.model.diff.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test

class DiffTest {
    @Test
    fun shouldSerializeAndDeserializeDiffList() {
        val diffList = DiffList(
            listOf(
                Unchanged("Go to Bondi Beach."),
                Addition("Bring your handboard."),
                Removal("Don't forget your towel."),
                Replacement("And have fun.", "And have lots of fun.")
            )
        )
        val format = Json { allowStructuredMapKeys = true }
        val serialized = format.encodeToString(diffList)
        val deserialized = format.decodeFromString<DiffList>(serialized)
        deserialized shouldBe diffList
    }

    @Test
    fun shouldNotIncludeUnchangedInTheListOfChanges() {
        val diffList = DiffList(
            listOf(
                Unchanged("Go to Bondi Beach."),
                Addition("Bring your handboard."),
                Removal("Don't forget your towel."),
                Replacement("And have fun.", "And have lots of fun.")
            )
        )
        diffList.numberOfChanges() shouldBe 3

    }
}