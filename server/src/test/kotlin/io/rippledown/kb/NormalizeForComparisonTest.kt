package io.rippledown.kb

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class NormalizeForComparisonTest {

    @Test
    fun `should strip double quotes`() {
        """UV is "5.6"""".normalizeForComparison() shouldBe "uv is 5.6"
    }

    @Test
    fun `should strip single quotes`() {
        "UV is '5.6'".normalizeForComparison() shouldBe "uv is 5.6"
    }

    @Test
    fun `should collapse multiple spaces`() {
        "UV  is   5.6".normalizeForComparison() shouldBe "uv is 5.6"
    }

    @Test
    fun `should trim leading and trailing whitespace`() {
        "  UV is 5.6  ".normalizeForComparison() shouldBe "uv is 5.6"
    }

    @Test
    fun `should lowercase`() {
        "UV Is 5.6".normalizeForComparison() shouldBe "uv is 5.6"
    }

    @Test
    fun `should treat quoted and unquoted constants as equivalent`() {
        "UV is 5.6".normalizeForComparison() shouldBe """UV is "5.6"""".normalizeForComparison()
    }

    @Test
    fun `should treat expressions differing only by whitespace as equivalent`() {
        "UV  is  5.6".normalizeForComparison() shouldBe "UV is 5.6".normalizeForComparison()
    }

    @Test
    fun `should distinguish genuinely different expressions`() {
        val result = "below".normalizeForComparison() != "Waves is low".normalizeForComparison()
        result shouldBe true
    }
}
