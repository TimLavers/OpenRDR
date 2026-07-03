package io.rippledown.model.report

import io.kotest.matchers.shouldBe
import io.rippledown.fromJsonString
import io.rippledown.toJsonString
import kotlin.test.Test

class CaseReportTest {
    @Test
    fun construction() {
        val report = CaseReport(markdown = "Test report")
        report.markdown shouldBe "Test report"
        report.generated shouldBe true
    }

    @Test
    fun constructionWithGeneratedFalse() {
        val report = CaseReport(markdown = "", generated = false)
        report.markdown shouldBe ""
        report.generated shouldBe false
    }

    @Test
    fun serialization() {
        val report = CaseReport(markdown = "Test report")
        val json = report.toJsonString()
        val deserialized = json.fromJsonString<CaseReport>()
        deserialized.markdown shouldBe "Test report"
        deserialized.generated shouldBe true
    }
}
