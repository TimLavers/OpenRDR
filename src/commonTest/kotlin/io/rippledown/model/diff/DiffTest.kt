package io.rippledown.model.diff

import io.kotest.matchers.shouldBe
import kotlinx.serialization.decodeFromString
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
        val serialized = Json.encodeToString(diffList)
        val deserialized = Json.decodeFromString<DiffList>(serialized)
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