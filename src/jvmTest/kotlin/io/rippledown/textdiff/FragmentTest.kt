package io.rippledown.textdiff

import io.kotest.matchers.shouldBe
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test

class FragmentTest {
    @Test
    fun `should serialize and deserialize FragmentList`() {
        val fragmentList = FragmentList(
            listOf(
                UnchangedFragment("Go to Bondi Beach."),
                AddedFragment("Bring your handboard."),
                RemovedFragment("Don't forget your towel."),
                ReplacedFragment("And have fun.", "And have lots of fun.")
            )
        )
        val serialized = Json.encodeToString(fragmentList)
        val deserialized = Json.decodeFromString<FragmentList>(serialized)
        deserialized shouldBe fragmentList
    }
}