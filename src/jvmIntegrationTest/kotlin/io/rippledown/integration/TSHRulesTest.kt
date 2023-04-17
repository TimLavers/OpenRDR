package io.rippledown.integration

import io.kotest.matchers.shouldBe
import io.rippledown.integration.restclient.RESTClient
import io.rippledown.model.condition.*
import kotlin.test.Test

// ORD6
internal class TSHRulesTest : TSHTest() {

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

        selectCaseAndCheckName("1.4.7")
        checkInterpretation("A moderately increased TSH with a normal FT4 is consistent with (mild) primary hypothyroidism.")

        selectCaseAndCheckName("1.4.8")
        checkInterpretationIgnoringWhitespace("TSH reference intervals in pregnancy. \n" +
                "1st trimester 0.02–2.5 \n" +
                "2nd and 3rd trimester 0.30–3.0")

        selectCaseAndCheckName("1.4.9")
        checkInterpretation("The mildly increased TSH and raised TPO antibodies indicate subclinical hypothyroidism due to autoimmune thyroid disease. \n" +
                "Suggest confirming subclinical hypothyroidism by repeat testing. Poor pregnancy outcomes have been described in women with a \n" +
                "raised TSH. If raised TSH confirmed, consider thyroxine replacement.")

        selectCaseAndCheckName("1.4.10")
        checkInterpretation("The suppressed TSH and normal FT4 are consistent with subclinical hyperthyroidism. \n" +
                "Suggest measure free triiodothyronine (FT3).")
        selectCaseAndCheckName("1.4.11")
        checkInterpretation("The increased FT3 and suppressed TSH (with a normal FT4) are consistent with T3 toxicosis. Suggest measure TSH-receptor antibodies (TRAb).")
        selectCaseAndCheckName("1.4.12")
        checkInterpretation("The severely increased TSH with a very low FT4 is consistent with primary hypothyroidism. Suggest measure TPO antibodies.")
        /*

                        selectCaseAndCheckName("1.4.13")

                        selectCaseAndCheckName("1.4.14")

                        selectCaseAndCheckName("1.4.15")

                        selectCaseAndCheckName("1.4.16")

                        selectCaseAndCheckName("1.4.17")
                */
    }

    private fun checkInterpretation(comment: String) {
        caseViewPO.interpretationText() shouldBe comment
    }

    private fun checkInterpretationIgnoringWhitespace(comment: String) {
        caseViewPO.interpretationText().replace("\\s".toRegex(), "") shouldBe comment.replace("\\s".toRegex(), "")
    }

    override fun setupCases() {
        super.setupCases()
        buildRules()
    }

    private fun buildRules() {
        val tsh = attributeFactory.create("TSH")
        val freeT4 = attributeFactory.create("Free T4")
        val ft3 = attributeFactory.create("Free T3")
        val tpo = attributeFactory.create("TPO Antibodies")
        val clinicalNotes = attributeFactory.create("Clinical Notes")

        val tshLow = IsLow(tsh)
        val tshNormal = IsNormal(tsh)
        val tshModeratelyIncreased = GreaterThanOrEqualTo(tsh, 10.0)
        val tshHigh = IsHigh(tsh)
        val tshVeryHigh = GreaterThanOrEqualTo(tsh, 40.0) // What should this be?
        val ft4VeryLow = Is(freeT4, "<5")
        val t4SlightlyLow = SlightlyLow(freeT4, 20)
        val ft3High = IsHigh(ft3)
        val freeT4Normal = IsNormal(freeT4)
        val tpoHigh = IsHigh(tpo)
        val elderly = GreaterThanOrEqualTo(attributeFactory.create("Age"), 70.0)
        val notesShowsTrimester1 = ContainsText(clinicalNotes, "12/40 weeks")
        val notesShowsTryingForBaby = ContainsText(clinicalNotes, "Trying for a baby")
        val tiredness = ContainsText(clinicalNotes, "very tired")

        val report1 = "Normal T4 and TSH are consistent with a euthyroid state."
        val report1b = "Normal TSH is consistent with a euthyroid state."
        val report2 = "A mildly reduced FT4 with a normal TSH may be due to non-thyroidal illness or pituitary hypothyroidism."
        val report3 = "Mildly increased TSH may be found in patients with subclinical hypothyroidism or sick euthyroid syndrome. " +
                "Suggest measurement of FT4, TSH and thyroperoxidase (TPO) antibodies in 6 weeks."
        val report3b = "Mildly increased TSH with a normal FT4 may be found in patients with subclinical hypothyroidism or sick euthyroid syndrome. " +
                "Suggest repeat measurement with TPO antibodies in 6 weeks."
        val report4 = "Mildly increased TSH with a normal FT4 can be seen in the euthyroid elderly."
        val report5 = "A moderately increased TSH with a normal FT4 is consistent with (mild) primary hypothyroidism."
        val report6 = "TSH reference intervals in pregnancy. \n" +
                "1st trimester                          0.02–2.5 \n" +
                "2nd and 3rd trimester                  0.30–3.0"
        val report7 = "The mildly increased TSH and raised TPO antibodies indicate subclinical hypothyroidism due to autoimmune thyroid disease. \n" +
                "Suggest confirming subclinical hypothyroidism by repeat testing. Poor pregnancy outcomes have been described in women with a \n" +
                "raised TSH. If raised TSH confirmed, consider thyroxine replacement."
        val report8 = "The suppressed TSH and normal FT4 are consistent with subclinical hyperthyroidism. \n" +
                "Suggest measure free triiodothyronine (FT3)."
        val report8b = "The increased FT3 and suppressed TSH (with a normal FT4) are consistent with T3 toxicosis. " +
                "Suggest measure TSH-receptor antibodies (TRAb)."
        val report9 = "The severely increased TSH with a very low FT4 is consistent with primary hypothyroidism. Suggest measure TPO antibodies."

        addCommentForCase("1.4.2", report1b, tshNormal)
        replaceCommentForCase("1.4.1", report1b, report1, freeT4Normal)
        replaceCommentForCase("1.4.3", report1b, report2, t4SlightlyLow)
        addCommentForCase("1.4.4", report3, tshHigh )
        replaceCommentForCase("1.4.5", report3, report3b, freeT4Normal)
        replaceCommentForCase("1.4.6", report3b, report4, elderly)

        replaceCommentForCase("1.4.7", report3b, report5, tshModeratelyIncreased)
        addCommentForCase("1.4.8", report6, tshLow, notesShowsTrimester1)
        replaceCommentForCase("1.4.9", report3b, report7, tpoHigh, notesShowsTryingForBaby)
        addCommentForCase("1.4.10", report8, tshLow, freeT4Normal, tiredness) // tiredness needed to exclude 1.4.8
        addCommentForCase("1.4.11", report8b, ft3High, tshLow, freeT4Normal)
        replaceCommentForCase("1.4.12", report3, report9, tshVeryHigh, ft4VeryLow)
    }

    private fun addCommentForCase(caseName: String, comment: String, vararg conditions: Condition) {
        val restClient = RESTClient()
        restClient.getCaseWithName(caseName)
        val conclusion = conclusionFactory.create(comment)
        restClient.startSessionToAddConclusionForCurrentCase(conclusion)
        conditions.forEach {
            restClient.addConditionForCurrentSession(it)
        }
        restClient.commitCurrentSession()
    }

    private fun replaceCommentForCase(
        caseName: String,
        toGo: String,
        replacement: String,
        vararg conditions: Condition
    ) {
        val restClient = RESTClient()
        restClient.getCaseWithName(caseName)
        val conclusionToGo = conclusionFactory.create(toGo)
        val replacementConclusion = conclusionFactory.create(replacement)
        restClient.startSessionToReplaceConclusionForCurrentCase(conclusionToGo, replacementConclusion)
        conditions.forEach {
            restClient.addConditionForCurrentSession(it)
        }
        restClient.commitCurrentSession()
    }

}