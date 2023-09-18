package io.rippledown.integration

import io.kotest.matchers.shouldBe
import io.rippledown.integration.restclient.RESTClient
import io.rippledown.model.Attribute
import io.rippledown.model.condition.*
import io.rippledown.model.condition.TabularCondition
import io.rippledown.model.condition.tabular.chain.Current
import io.rippledown.model.condition.tabular.predicate.*
import kotlin.test.Test

// ORD6
internal class TSHRulesTest : TSHTest() {

    @Test
    fun checkInterpretations() {
        stop()
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

        selectCaseAndCheckName("1.4.13")
        checkInterpretation("Suggest repeat TFT measurement at least 4–6 weeks after commencement of T4 replacement. ")

        selectCaseAndCheckName("1.4.14")
        checkInterpretation("The normal TSH and FT4 are consistent with adequate thyroid hormone replacement.")

        selectCaseAndCheckName("1.4.15")
        checkInterpretation("Increased TSH suggests inadequate thyroid hormone replacement if the dose has not been changed for at least 6 weeks and patient \n" +
                "has been taking the medication regularly. \n" +
                "Suggest review dose and repeat TFTs in 6 weeks.")

        selectCaseAndCheckName("1.4.16")
        checkInterpretation("Suppressed TSH is consistent with excessive thyroid hormone replacement.")

        selectCaseAndCheckName("1.4.17")
        checkInterpretation("Previous history of thyroid cancer noted. Low TSH may be appropriate depending on treatment targets for this patient.")

    }

    private fun checkInterpretation(comment: String) {
        interpretationViewPO.interpretationText() shouldBe comment
    }

    @Suppress("SameParameterValue")
    private fun checkInterpretationIgnoringWhitespace(comment: String) {
        interpretationViewPO.interpretationText().replace("\\s".toRegex(), "") shouldBe comment.replace(
            "\\s".toRegex(),
            ""
        )
    }

    override fun setupCases() {
        super.setupCases()
        buildRules()
    }

    private fun isLow(attribute: Attribute) = TabularCondition(null, attribute, Low, Current)
    private fun isNormal(attribute: Attribute) = TabularCondition(null, attribute, Normal, Current)
    private fun isHigh(attribute: Attribute) = TabularCondition(null, attribute, High, Current)
    private fun isCondition(attribute: Attribute, text: String) = TabularCondition(null, attribute, Is(text), Current)
    private fun containsText(attribute: Attribute, text: String) = TabularCondition(null, attribute, Contains(text), Current)
    private fun greaterThanOrEqualTo(attribute: Attribute, d: Double) = TabularCondition(null, attribute, GreaterThanOrEquals(d), Current)
    private fun lessThanOrEqualTo(attribute: Attribute, d: Double) = TabularCondition(null, attribute, LessThanOrEquals(d), Current)
    private fun slightlyLow(attribute: Attribute, cutoff: Int) =
        TabularCondition(null, attribute, AtMostPercentageLow(cutoff), Current)

    private fun buildRules() {
        val tsh = attributeFactory.create("TSH")
        val freeT4 = attributeFactory.create("Free T4")
        val ft3 = attributeFactory.create("Free T3")
        val tpo = attributeFactory.create("TPO Antibodies")
        val clinicalNotes = attributeFactory.create("Clinical Notes")

        val tshLow = conditionFactory.getOrCreate(isLow(tsh))
        val tshNormal = conditionFactory.getOrCreate(isNormal(tsh))
        val tshModeratelyIncreased = conditionFactory.getOrCreate(greaterThanOrEqualTo(tsh, 10.0))
        val tshHigh = conditionFactory.getOrCreate(isHigh(tsh))
        val tshVeryHigh = conditionFactory.getOrCreate(greaterThanOrEqualTo( tsh, 40.0)) // What should this be?
        val ft4VeryLow = conditionFactory.getOrCreate(isCondition(freeT4, "<5"))
        val t4SlightlyLow = conditionFactory.getOrCreate(slightlyLow(freeT4, 20))
        val ft3High = conditionFactory.getOrCreate(isHigh(ft3))
        val freeT4Normal = conditionFactory.getOrCreate(isNormal( freeT4))
        val tpoHigh = conditionFactory.getOrCreate(isHigh(tpo))
        val elderly = conditionFactory.getOrCreate(greaterThanOrEqualTo(attributeFactory.create("Age"), 70.0))
        val notesShowsTrimester1 = conditionFactory.getOrCreate(containsText(clinicalNotes, "12/40 weeks"))
        val notesShowsTryingForBaby = conditionFactory.getOrCreate(containsText( clinicalNotes, "Trying for a baby"))
        val tiredness = conditionFactory.getOrCreate(containsText(clinicalNotes, "very tired"))
        val thyroxineReplacement1Week = conditionFactory.getOrCreate(containsText(clinicalNotes, "started T4 replacement 1 week ago"))
        val t4Replacement = conditionFactory.getOrCreate(containsText(clinicalNotes, "On T4 replacement"))
        val tshVeryLow = conditionFactory.getOrCreate(lessThanOrEqualTo(tsh, 0.1)) // What should this be?
        val historyThyroidCancer = conditionFactory.getOrCreate(containsText( clinicalNotes, "thyroid cancer"))
        val onThyroxine = conditionFactory.getOrCreate(containsText(clinicalNotes, "On thyroxine"))

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
        val report10 = "Suggest repeat TFT measurement at least 4–6 weeks after commencement of T4 replacement. "
        val report11 = "The normal TSH and FT4 are consistent with adequate thyroid hormone replacement."
        val report12 = "Increased TSH suggests inadequate thyroid hormone replacement if the dose has not been changed for at least 6 weeks and patient \n" +
                "has been taking the medication regularly. \n" +
                "Suggest review dose and repeat TFTs in 6 weeks."
        val report13 = "Suppressed TSH is consistent with excessive thyroid hormone replacement."
        val report14 = "Previous history of thyroid cancer noted. Low TSH may be appropriate depending on treatment targets for this patient."

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
        replaceCommentForCase("1.4.13", report3, report10, thyroxineReplacement1Week)
        replaceCommentForCase("1.4.14", report1, report11, t4Replacement)
        replaceCommentForCase("1.4.15", report3b, report12, t4Replacement)
        addCommentForCase("1.4.16", report13, tshVeryLow, t4Replacement)
        addCommentForCase("1.4.17", report14, tshLow, onThyroxine, historyThyroidCancer)

    }

    private fun addCommentForCase(caseName: String, comment: String, vararg conditions: Condition) {
        val restClient = RESTClient()
        restClient.getCaseWithName(caseName)
        val conclusion = conclusionFactory.getOrCreate(comment)
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
        val conclusionToGo = conclusionFactory.getOrCreate(toGo)
        val replacementConclusion = conclusionFactory.getOrCreate(replacement)
        restClient.startSessionToReplaceConclusionForCurrentCase(conclusionToGo, replacementConclusion)
        conditions.forEach {
            restClient.addConditionForCurrentSession(it)
        }
        restClient.commitCurrentSession()
    }
}