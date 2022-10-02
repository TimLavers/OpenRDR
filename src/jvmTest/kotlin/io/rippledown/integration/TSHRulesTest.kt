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

        selectCaseAndCheckName("1.4.5")
        checkInterpretation("Mildly increased TSH with a normal FT4 may be found in patients with subclinical hypothyroidism or sick euthyroid syndrome. Suggest repeat measurement with TPO antibodies in 6 weeks.")

        selectCaseAndCheckName("1.4.6")
        checkInterpretation("Mildly increased TSH with a normal FT4 can be seen in the euthyroid elderly.")

//        selectCaseAndCheckName("1.4.7")
//        checkInterpretation("A moderately increased TSH with a normal FT4 is consistent with (mild) primary hypothyroidism.")
//
//        selectCaseAndCheckName("1.4.8")
//        checkInterpretation("TSH reference intervals in pregnancy.")
/*
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
        val tshNormal = IsNormal(tsh)
        val highTSH = IsHigh(tsh)
        val tshSlightlyHigh = SlightlyHigh(tsh, 20)
        val freeT4Normal = IsNormal(freeT4)
        val freeT4NotDone = HasNoCurrentValue(freeT4)
        val notElderly = LessThanOrEqualTo(Attribute("Age"), 69.0)
        val elderly = GreaterThanOrEqualTo(Attribute("Age"), 70.0)
        val freeT4SlightlyLow = SlightlyLow(freeT4, 20)
        addCommentForCase("1.4.1", "Normal T4 and TSH are consistent with a euthyroid state.", tshNormal, freeT4Normal)
        addCommentForCase("1.4.2", "Normal TSH is consistent with a euthyroid state.", tshNormal, freeT4NotDone)
        addCommentForCase("1.4.3", "A mildly reduced FT4 with a normal TSH may be due to non-thyroidal illness or pituitary hypothyroidism.", tshNormal, freeT4SlightlyLow)
        addCommentForCase("1.4.4", "Mildly increased TSH may be found in patients with subclinical hypothyroidism or sick euthyroid syndrome. Suggest measurement of FT4, TSH and thyroperoxidase (TPO) antibodies in 6 weeks.", highTSH, freeT4NotDone, notElderly )
        addCommentForCase("1.4.5", "Mildly increased TSH with a normal FT4 may be found in patients with subclinical hypothyroidism or sick euthyroid syndrome. Suggest repeat measurement with TPO antibodies in 6 weeks.", highTSH, freeT4Normal, notElderly)
        addCommentForCase("1.4.6", "Mildly increased TSH with a normal FT4 can be seen in the euthyroid elderly.", tshSlightlyHigh, elderly, freeT4Normal)
//        addCommentForCase("1.4.", "")
//        addCommentForCase("1.4.", "")
//        addCommentForCase("1.4.", "")
//        addCommentForCase("1.4.", "")
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