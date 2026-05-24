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
    fun `strips trailing noise annotation`() {
        cleanTranscript("Yes, please. [noise]") shouldBe "Yes, please."
    }

    @Test
    fun `strips inline noise annotation and collapses whitespace`() {
        cleanTranscript("Yes [noise] please") shouldBe "Yes please"
    }

    @Test
    fun `strips multi-word non-speech annotation`() {
        cleanTranscript("Yes [background noise] please") shouldBe "Yes please"
    }

    @Test
    fun `strips known non-speech tags`() {
        cleanTranscript("Hello [music] world [laughter] [silence] today") shouldBe
                "Hello world today"
    }

    @Test
    fun `noise annotation immediately before punctuation does not leave a gap`() {
        cleanTranscript("Yes, please [noise].") shouldBe "Yes, please."
    }

    @Test
    fun `strips both leading timestamps and inline noise tags`() {
        cleanTranscript("[ 00:00:03 ] Yes, please. [noise]") shouldBe "Yes, please."
    }

    @Test
    fun `strips even without spaces inside brackets`() {
        cleanTranscript("[00:00:03]Yes, please.") shouldBe "Yes, please."
    }
}
