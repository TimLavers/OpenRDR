package io.rippledown.caseview

import io.kotest.matchers.shouldBe
import org.junit.Test

class UnitsCellFormatTest {

    @Test
    fun `passes through plain units unchanged`() {
        formatUnits("mmol/L").text shouldBe "mmol/L"
        formatUnits("g/L").text shouldBe "g/L"
        formatUnits("").text shouldBe ""
    }

    @Test
    fun `renders the micro prefix u as the Greek letter mu`() {
        formatUnits("umol/L").text shouldBe "μmol/L"
        formatUnits("ug/L").text shouldBe "μg/L"
        formatUnits("uIU/mL").text shouldBe "μIU/mL"
    }

    @Test
    fun `does not replace u in the middle of a word`() {
        // The 'u' in 'IU' (international units) and 'AU' must stay put.
        formatUnits("IU/L").text shouldBe "IU/L"
        formatUnits("kU/L").text shouldBe "kU/L"
    }

    @Test
    fun `renders 10^n as 10 followed by a superscript exponent`() {
        // The plain text strips the caret; the superscript styling lives in
        // the AnnotatedString spans, which a `Text` composable will render
        // using a smaller, raised exponent.
        formatUnits("10^9/L").text shouldBe "109/L"
        formatUnits("10^12/L").text shouldBe "1012/L"
        formatUnits("10^-3").text shouldBe "10-3"
    }

    @Test
    fun `combines both transformations when needed`() {
        formatUnits("umol/10^9").text shouldBe "μmol/109"
    }
}
