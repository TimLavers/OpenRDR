package io.rippledown.textdiff

import io.kotest.matchers.shouldBe
import io.rippledown.model.diff.Addition
import io.rippledown.model.diff.Removal
import io.rippledown.model.diff.Replacement
import io.rippledown.model.diff.Unchanged
import org.junit.Test

class DiffGeneratorTest {

    @Test
    fun add() {
        diffs("A", "AB") shouldBe listOf(
            Unchanged("A"),
            Addition("B")
        )
    }

    @Test
    fun `consecutive adds`() {
        diffs("A", "ABC") shouldBe listOf(
            Unchanged("A"),
            Addition("B"),
            Addition("C")
        )
    }

    @Test
    fun `non-consecutive adds`() {
        diffs("B", "ABC") shouldBe listOf(
            Addition("A"),
            Unchanged("B"),
            Addition("C")
        )
    }

    @Test
    fun `several non-consecutive adds`() {
        diffs("C", "ABCDE") shouldBe listOf(
            Addition("A"),
            Addition("B"),
            Unchanged("C"),
            Addition("D"),
            Addition("E"),

            )
    }

    @Test
    fun remove() {
        diffs("AB", "A") shouldBe listOf(
            Unchanged("A"),
            Removal("B")
        )
    }

    @Test
    fun `consecutive removes`() {
        diffs("ABC", "A") shouldBe listOf(
            Unchanged("A"),
            Removal("B"),
            Removal("C"),
        )
    }

    @Test
    fun `non-consecutively removes`() {
        diffs("ABC", "B") shouldBe listOf(
            Removal("A"),
            Unchanged("B"),
            Removal("C"),
        )
    }

    @Test
    fun `several non-consecutively removes`() {
        diffs("ABCDE", "C") shouldBe listOf(
            Removal("A"),
            Removal("B"),
            Unchanged("C"),
            Removal("D"),
            Removal("E"),
        )
    }

    @Test
    fun replace() {
        diffs("A", "B") shouldBe listOf(
            Replacement("A", "B")
        )
    }

    @Test
    fun `replacement then addition`() {
        diffs("A", "BC") shouldBe listOf(
            Replacement("A", "B"),
            Addition("C")
        )
    }

    @Test
    fun `consecutive replacements`() {
        diffs("AB", "CD") shouldBe listOf(
            Replacement("A", "C"),
            Replacement("B", "D"),
        )
    }

    @Test
    fun `non-consecutive replacements`() {
        diffs("ABC", "DBE") shouldBe listOf(
            Replacement("A", "D"),
            Unchanged("B"),
            Replacement("C", "E"),
        )
    }

    @Test
    fun unchanged() {
        diffs("A", "A") shouldBe listOf(
            Unchanged("A")
        )
    }

    @Test
    fun `no revision`() {
        diffs("", "") shouldBe emptyList()
    }

    @Test
    fun `replace and unchange`() {
        diffs("AB", "CB") shouldBe listOf(
            Replacement("A", "C"),
            Unchanged("B")
        )
    }

    @Test
    fun `replace and add`() {
        diffs("A", "BC") shouldBe listOf(
            Replacement("A", "B"),
            Addition("C")
        )
    }

    @Test
    fun `unchange and replace`() {
        diffs("AB", "AC") shouldBe listOf(
            Unchanged("A"),
            Replacement("B", "C")
        )
    }

    @Test
    fun `remove, several consecutive unchanges, add`() {
        diffs("ABC", "BCD") shouldBe listOf(
            Removal("A"),
            Unchanged("B"),
            Unchanged("C"),
            Addition("D"),
        )
    }

    @Test
    fun `non-consecutive unchanges, remove and add`() {
        diffs("ABC", "ACD") shouldBe listOf(
            Unchanged("A"),
            Removal("B"),
            Unchanged("C"),
            Addition("D"),
        )
    }

    @Test
    fun `unchanged, removed, removed, unchanged, added`() {
        diffs("ABCD", "ADE") shouldBe listOf(
            Unchanged("A"),
            Removal("B"),
            Removal("C"),
            Unchanged("D"),
            Addition("E"),
        )
    }

    @Test
    fun `remove, consecutive unchanges, add`() {
        diffs("ABC", "BCD") shouldBe listOf(
            Removal("A"),
            Unchanged("B"),
            Unchanged("C"),
            Addition("D"),
        )
    }

    @Test
    fun `several unchanged`() {
        diffs("ABC", "ABC") shouldBe listOf(
            Unchanged("A"),
            Unchanged("B"),
            Unchanged("C"),
        )
    }

    @Test
    fun `all types of revisions`() {
        diffs("ABACDEAF", "ACGEH") shouldBe listOf(
            Unchanged("A"),
            Removal("B"),
            Removal("A"),
            Unchanged("C"),
            Replacement("D", "G"),
            Unchanged("E"),
            Replacement("A", "H"),
            Removal("F"),
        )
    }
}

