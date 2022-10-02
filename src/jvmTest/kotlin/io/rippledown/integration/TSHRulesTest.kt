package io.rippledown.integration

import io.kotest.matchers.shouldBe
import io.rippledown.integration.restclient.RESTClient
import io.rippledown.model.Attribute
import io.rippledown.model.Conclusion
import io.rippledown.model.condition.*
import kotlin.test.Test

// ORD6
internal class TSHRulesTest: TSHTest() {

    @Test
    fun checkInterpretations() {
        selectCaseAndCheckName("1.4.1")
        checkInterpretation("Normal T4 and TSH are consistent with a euthyroid state.")

        selectCaseAndCheckName("1.4.2")
        checkInterpretation("Normal TSH is consistent with a euthyroid state.")

        selectCaseAndCheckName("1.4.3")
        checkInterpretation("A mildly reduced FT4 with a normal TSH may be due to non-thyroidal illness or pituitary hypothyroidism.")

        selectCaseAndCheckName("1.4.4")
        checkInterpretation("Mildly increased TSH may be found in patients with subclinical hypothyroidism or sick euthyroid syndrome. Suggest measurement of FT4, TSH and thyroperoxidase (TPO) antibodies in 6 weeks.")
        /*
        selectCaseAndCheckName("1.4.5")

        selectCaseAndCheckName("1.4.6")

        selectCaseAndCheckName("1.4.7")

        selectCaseAndCheckName("1.4.8")

        selectCaseAndCheckName("1.4.9")

        selectCaseAndCheckName("1.4.10")

        selectCaseAndCheckName("1.4.11")

        selectCaseAndCheckName("1.4.12")

        selectCaseAndCheckName("1.4.13")

        selectCaseAndCheckName("1.4.14")

        selectCaseAndCheckName("1.4.15")

        selectCaseAndCheckName("1.4.16")

        selectCaseAndCheckName("1.4.17")
*/
    }

    fun checkInterpretation(comment: String) {
        caseViewPO.interpretationText() shouldBe comment
    }

    override fun setupCases() {
        super.setupCases()
        buildRules()
    }

    private fun buildRules() {
        val tsh = Attribute("TSH")
        val freeT4 = Attribute("Free T4")
        val normalTSH = IsNormal(tsh)
        val normalFreeT4 = IsNormal(freeT4)
        val freeT4NotDone = HasNoCurrentValue(freeT4)
        val freeT4SlightlyLow = SlightlyLow(freeT4, 20)
        val highTSH = IsHigh(tsh)
        addCommentForCase("1.4.1", "Normal T4 and TSH are consistent with a euthyroid state.", normalTSH, normalFreeT4)
        addCommentForCase("1.4.2", "Normal TSH is consistent with a euthyroid state.", normalTSH, freeT4NotDone)
        addCommentForCase("1.4.3", "A mildly reduced FT4 with a normal TSH may be due to non-thyroidal illness or pituitary hypothyroidism.", normalTSH, freeT4SlightlyLow)
        addCommentForCase("1.4.4", "Mildly increased TSH may be found in patients with subclinical hypothyroidism or sick euthyroid syndrome. Suggest measurement of FT4, TSH and thyroperoxidase (TPO) antibodies in 6 weeks.", highTSH, freeT4NotDone )
    }

    private fun addCommentForCase(caseName: String, comment: String, vararg conditions: Condition) {
        val restClient = RESTClient()
        restClient.getCaseWithName(caseName)
        restClient.startSessionToAddConclusionForCurrentCase(Conclusion(comment))
        conditions.forEach {
            restClient.addConditionForCurrentSession(it)
        }
        restClient.commitCurrentSession()
    }

}