package io.rippledown.model

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.utils.serializeDeserialize
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test

internal class CaseListTypeTest {

    @Test
    fun `name keeps the spelling with which the list type was created`() {
        // Given a list type created with mixed-case spelling
        val listType = CaseListType("Gestational Diabetes")

        // When its name is read
        val name = listType.name

        // Then the original spelling is retained
        name shouldBe "Gestational Diabetes"
    }

    @Test
    fun `equality ignores case`() {
        // Given list types whose names differ only in case
        // When they are compared
        // Then they are equal
        CaseListType("good") shouldBe CaseListType("good")
        CaseListType("good") shouldBe CaseListType("Good")
        CaseListType("GOOD") shouldBe CaseListType("good")
        CaseListType("gOoD") shouldBe CaseListType("GoOd")
    }

    @Test
    fun `list types with different names are not equal`() {
        // Given list types with different names
        // When they are compared
        // Then they are not equal
        CaseListType("good") shouldNotBe CaseListType("bad")
        CaseListType("good") shouldNotBe CaseListType("good ")
        CaseListType("Processed") shouldNotBe CaseListType("Cornerstone")
    }

    @Test
    fun `a list type is not equal to an object of another class`() {
        // Given a list type and its name string
        val listType = CaseListType("good")

        // When the list type is compared with the string
        // Then they are not equal
        listType.equals("good").shouldBeFalse()
    }

    @Test
    fun `hash code ignores case`() {
        // Given list types whose names differ only in case
        // When their hash codes are computed
        // Then the hash codes are equal
        CaseListType("GOOD").hashCode() shouldBe CaseListType("good").hashCode()
        CaseListType("Cornerstone").hashCode() shouldBe CaseListType.Cornerstone.hashCode()
    }

    @Test
    fun `toString is the name as entered`() {
        // Given a list type with mixed-case spelling
        val listType = CaseListType("Gestational Diabetes")

        // When it is converted to a string
        // Then the original spelling is given
        "$listType" shouldBe "Gestational Diabetes"
    }

    @Test
    fun `the built-in list types are built in, whatever the case of the name`() {
        // Given the built-in list types, or copies with different case
        // When isBuiltIn is read
        // Then it is true
        CaseListType.Processed.isBuiltIn.shouldBeTrue()
        CaseListType.Cornerstone.isBuiltIn.shouldBeTrue()
        CaseListType("processed").isBuiltIn.shouldBeTrue()
        CaseListType("PROCESSED").isBuiltIn.shouldBeTrue()
        CaseListType("cornerstone").isBuiltIn.shouldBeTrue()
        CaseListType("CORNERSTONE").isBuiltIn.shouldBeTrue()
    }

    @Test
    fun `user-defined list types are not built in`() {
        // Given list types with user-defined names
        // When isBuiltIn is read
        // Then it is false
        CaseListType("good").isBuiltIn.shouldBeFalse()
        CaseListType("Borderline").isBuiltIn.shouldBeFalse()
        CaseListType("Processed Cases").isBuiltIn.shouldBeFalse()
    }

    @Test
    fun `names of the built-in lists are reserved, ignoring case and surrounding blanks`() {
        // Given names that denote the built-in lists in various spellings
        // When isReservedListName is called
        // Then it is true
        CaseListType.isReservedListName("Processed").shouldBeTrue()
        CaseListType.isReservedListName("processed").shouldBeTrue()
        CaseListType.isReservedListName("PROCESSED").shouldBeTrue()
        CaseListType.isReservedListName(" processed ").shouldBeTrue()
        CaseListType.isReservedListName("Cornerstone").shouldBeTrue()
        CaseListType.isReservedListName("cornerstones").shouldBeTrue()
        CaseListType.isReservedListName("CORNERSTONES").shouldBeTrue()
    }

    @Test
    fun `names of the built-in lists with a cases or case list suffix are reserved`() {
        // Given names that denote the built-in lists with a trailing "cases" or "case list"
        // When isReservedListName is called
        // Then it is true
        CaseListType.isReservedListName("Processed Cases").shouldBeTrue()
        CaseListType.isReservedListName("processed cases").shouldBeTrue()
        CaseListType.isReservedListName("Processed case list").shouldBeTrue()
        CaseListType.isReservedListName("Cornerstone Cases").shouldBeTrue()
        CaseListType.isReservedListName("CORNERSTONE CASES").shouldBeTrue()
        CaseListType.isReservedListName("cornerstones case list").shouldBeTrue()
    }

    @Test
    fun `other names are not reserved`() {
        // Given names that do not denote a built-in list
        // When isReservedListName is called
        // Then it is false
        CaseListType.isReservedListName("good").shouldBeFalse()
        CaseListType.isReservedListName("Borderline").shouldBeFalse()
        CaseListType.isReservedListName("my processed").shouldBeFalse()
        CaseListType.isReservedListName("cases").shouldBeFalse()
        CaseListType.isReservedListName("").shouldBeFalse()
        CaseListType.isReservedListName(" ").shouldBeFalse()
    }

    @Test
    fun `serialises as the bare name string`() {
        // Given a list type
        val listType = CaseListType("Good")

        // When it is serialised
        val serialised = Json.encodeToString(listType)

        // Then the JSON is the bare name string
        serialised shouldBe "\"Good\""
    }

    @Test
    fun `serialisation round trip preserves the name and its case`() {
        // Given a list type with mixed-case spelling
        val listType = CaseListType("GoOd")

        // When it is serialised and deserialised
        val restored = serializeDeserialize(listType)

        // Then the restored list type is equal and keeps the spelling
        restored shouldBe listType
        restored.name shouldBe "GoOd"
    }
}
