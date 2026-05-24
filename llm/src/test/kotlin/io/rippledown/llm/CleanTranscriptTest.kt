package io.rippledown.llm

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class CleanTranscriptTest {

    @Test
    fun `empty stays empty`() {
        cleanTranscript("") shouldBe ""
    }

    @Test
    fun `plain transcript is unchanged`() {
        cleanTranscript("Yes, please add the comment go to the beach.") shouldBe
                "Yes, please add the comment go to the beach."
    }

    @Test
    fun `trims surrounding whitespace`() {
        cleanTranscript("   hello world   ") shouldBe "hello world"
    }

    @Test
    fun `strips a single HH MM SS timestamp prefix`() {
        cleanTranscript("[ 00:00:03 ] Yes, please.") shouldBe "Yes, please."
    }

    @Test
    fun `strips a duration range prefix`() {
        cleanTranscript("[ 0m0s700ms - 0m1s100ms ] Yes, please.") shouldBe "Yes, please."
    }

    @Test
    fun `strips a short duration prefix`() {
        cleanTranscript("[ 0m0s ] Yes, please.") shouldBe "Yes, please."
    }

    @Test
    fun `strips multiple consecutive bracket prefixes`() {
        cleanTranscript("[ 00:00:03 ] [ 0m0s700ms - 0m1s100ms ] [ 0m0s ] Yes, please.") shouldBe
                "Yes, please."
    }

    @Test
    fun `preserves the unclear-word marker`() {
        cleanTranscript("[?] is the patient's name") shouldBe "[?] is the patient's name"
    }

    @Test
    fun `preserves unclear-word marker after stripping timestamps`() {
        cleanTranscript("[ 00:00:03 ] [?] is the patient's name") shouldBe
                "[?] is the patient's name"
    }

    @Test
    fun `does not touch bracketed text mid transcript`() {
        cleanTranscript("Yes [00:00:03] please") shouldBe "Yes [00:00:03] please"
    }

    @Test
    fun `does not strip a leading bracket that is not a timestamp`() {
        cleanTranscript("[important] Yes please") shouldBe "[important] Yes please"
    }

    @Test
    fun `strips even without spaces inside brackets`() {
        cleanTranscript("[00:00:03]Yes, please.") shouldBe "Yes, please."
    }
}
