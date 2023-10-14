package io.rippledown.integration

import io.kotest.matchers.shouldBe
import io.rippledown.integration.restclient.RESTClient
import io.rippledown.model.Attribute
import io.rippledown.model.condition.*
import io.rippledown.model.condition.TabularCondition
import io.rippledown.model.condition.series.Increasing
import io.rippledown.model.condition.tabular.chain.All
import io.rippledown.model.condition.tabular.chain.AtLeast
import io.rippledown.model.condition.tabular.chain.AtMost
import io.rippledown.model.condition.tabular.chain.Current
import io.rippledown.model.condition.tabular.predicate.*
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

        selectCaseAndCheckName("1.4.18")
        checkInterpretation("Borderline TSH persists. Suggest repeat in one year with thyroid autoantibodies (TPO antibodies).")

        selectCaseAndCheckName("1.4.19")
        checkInterpretation("The suppressed TSH and high-normal FT4 may suggest hyperthyroidism. FT3 and TRAb may be useful. However, low " +
                "TSH may be seen in pregnancy which should be excluded. These results are within reference intervals for first trimester. If pregnant, " +
                "repeat TFTs in 6 weeks.")

        selectCaseAndCheckName("1.4.20")
        checkInterpretation("Clinical conditions associated with a suppressed TSH include non-toxic goitre, subclinical hyperthyroidism and glucocorticoid " +
                "therapy. Suggest repeat TFTs in six weeks’ time. Other causes of this pattern include: Excessive T4 therapy for hypothyroidism, treated " +
                "primary hyperthyroidism. Acute psychiatric illness may raise FT4 and/or lower TSH.")

        selectCaseAndCheckName("1.4.21")
        checkInterpretation("The increased FT3 and suppressed TSH are consistent with T3 toxicosis. Suggest measure TRAb.")

        selectCaseAndCheckName("1.4.22")
        checkInterpretation("The severely increased FT4 and FT3 and suppressed TSH are consistent with thyrotoxicosis. These results together with the clinical presentation may indicate thyroid storm. Suggest measure TRAb.")

        selectCaseAndCheckName("1.4.23")
        checkInterpretation("The reduced FT4 is consistent with excessive anti-thyroid treatment. The suppressed TSH may take many months to normalise " +
                "following commencement of ant-thyroid treatment.")

        selectCaseAndCheckName("1.4.24")
        checkInterpretation("Thyrogen [thyrotropin alpha (recombinant human TSH)] in blood is measured by the TSH assay.")

        selectCaseAndCheckName("1.4.25")
        checkInterpretation("Results should be interpreted in the context of serial measurement.")

        selectCaseAndCheckName("1.4.26")
        checkInterpretation("The positive anti thyroglobulin antibodies may interfere with this thyroglobulin immunometric assay and cause a falsely low result " +
                "making the thyroglobulin result unreliable. Anti-Tg Ab trends may be used as a surrogate tumour marker for monitoring.")

        selectCaseAndCheckName("1.4.27")
        checkInterpretation("The mildly increased TSH with normal FT4 and raised TPO antibodies indicate subclinical hypothyroidism due to autoimmune " +
                "thyroid disease. FT3 measurement is helpful only in diagnosing hyperthyroidism (or in monitoring FT3 supplementation).")

        selectCaseAndCheckName("1.4.28")
        checkInterpretation("Amiodarone inhibits T4 to T3 conversion as well as presenting the thyroid with a large iodine load. The suppressed TSH and raised " +
                "FT4 may suggest amiodarone-induced hyperthyroidism but should be interpreted in the light of clinical findings.")
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

    private fun isPresent(attribute: Attribute) = TabularCondition(null, attribute, IsNotBlank, Current)
    private fun isLow(attribute: Attribute) = TabularCondition(null, attribute, Low, Current)
    private fun isNormal(attribute: Attribute) = TabularCondition(null, attribute, Normal, Current)
    private fun isHigh(attribute: Attribute) = TabularCondition(null, attribute, High, Current)
    private fun isCondition(attribute: Attribute, text: String) = TabularCondition(null, attribute, Is(text), Current)
    private fun containsText(attribute: Attribute, text: String) = TabularCondition(null, attribute, Contains(text), Current)
    private fun doesNotContainText(attribute: Attribute, text: String) = TabularCondition(null, attribute, DoesNotContain(text), Current)
    private fun greaterThanOrEqualTo(attribute: Attribute, d: Double) = TabularCondition(null, attribute, GreaterThanOrEquals(d), Current)
    private fun lessThanOrEqualTo(attribute: Attribute, d: Double) = TabularCondition(null, attribute, LessThanOrEquals(d), Current)
    private fun slightlyLow(attribute: Attribute, cutoff: Int) =
        TabularCondition(null, attribute, LowByAtMostSomePercentage(cutoff), Current)

    private fun buildRules() {
        val tsh = attributeFactory.create("TSH")
        val ft4 = attributeFactory.create("Free T4")
        val ft3 = attributeFactory.create("Free T3")
        val tpo = attributeFactory.create("TPO Antibodies")
        val thyroglobulin = attributeFactory.create("Thyroglobulin")
        val antiThyroglobulin = attributeFactory.create("Anti-Thyroglobulin")
        val clinicalNotes = attributeFactory.create("Clinical Notes")
        val sex = attributeFactory.create("Sex")
        val age = attributeFactory.create("Age")

        val tshBelowDetection = conditionFactory.getOrCreate(isCondition(tsh, "<0.01"))
        val tshVeryLow = conditionFactory.getOrCreate(lessThanOrEqualTo(tsh, 0.1)) // What should this be?
        val tshLow = conditionFactory.getOrCreate(isLow(tsh))
        val tshNormal = conditionFactory.getOrCreate(isNormal(tsh))
        val borderlineHighTSH = conditionFactory.getOrCreate((TabularCondition(tsh, NormalOrHighByAtMostSomePercentage(10), All)))
        val tshModeratelyIncreased = conditionFactory.getOrCreate(greaterThanOrEqualTo(tsh, 10.0))
        val tshHigh = conditionFactory.getOrCreate(isHigh(tsh))
        val tshVeryHigh = conditionFactory.getOrCreate(greaterThanOrEqualTo( tsh, 40.0)) // What should this be?
        val tshAbove100 = conditionFactory.getOrCreate(greaterThanOrEqualTo( tsh, 100.0)) //  should this be?
        val atLeastTwoTSH = conditionFactory.getOrCreate(TabularCondition(tsh, IsNumeric, AtLeast(2)))

        val ft4VeryLow = conditionFactory.getOrCreate(isCondition(ft4, "<5"))
        val ft4SlightlyLow = conditionFactory.getOrCreate(slightlyLow(ft4, 20))
        val ft4Low = conditionFactory.getOrCreate(isLow(ft4))
        val fT4Normal = conditionFactory.getOrCreate(isNormal( ft4))
        val borderlineHighFT4 = conditionFactory.getOrCreate(TabularCondition(ft4, NormalOrHighByAtMostSomePercentage(10), All))
        val ft4High = conditionFactory.getOrCreate(isHigh(ft4))
        val severelyHighFT4 =  conditionFactory.getOrCreate(TabularCondition(ft4, GreaterThanOrEquals(40.0), Current))

        val ft3Available = conditionFactory.getOrCreate(isPresent(ft3))
        val ft3High = conditionFactory.getOrCreate(isHigh(ft3))
        val ft3Increasing = conditionFactory.getOrCreate((SeriesCondition(null, ft3, Increasing)))
        val borderlineHighFT3 = conditionFactory.getOrCreate((TabularCondition(ft3, NormalOrHighByAtMostSomePercentage(10), All)))
        val severelyHighFT3 =  conditionFactory.getOrCreate(TabularCondition(ft3, GreaterThanOrEquals(10.0), Current))

        val tpoHigh = conditionFactory.getOrCreate(isHigh(tpo))

        val thyroglobulinAvailable = conditionFactory.getOrCreate(isPresent(thyroglobulin))
        val thyroglobulinBelowDetection = conditionFactory.getOrCreate(isCondition(thyroglobulin, "<0.1"))
        val onlyOneThyroglobulin = conditionFactory.getOrCreate(TabularCondition(thyroglobulin, IsNotBlank, AtMost(2)))
        val antiThyroglobulinAvailable = conditionFactory.getOrCreate(isPresent(antiThyroglobulin))
        val antiThyroglobulinHigh = conditionFactory.getOrCreate(isHigh(antiThyroglobulin))

        val olderThan14 = conditionFactory.getOrCreate(greaterThanOrEqualTo(age, 14.0))
        val youngerThan44 = conditionFactory.getOrCreate(lessThanOrEqualTo(age, 44.0))
        val olderThan44 = conditionFactory.getOrCreate(greaterThanOrEqualTo(age, 45.0))
        val elderly = conditionFactory.getOrCreate(greaterThanOrEqualTo(age, 70.0))

        val female = conditionFactory.getOrCreate(isCondition(sex, "F"))

        val notesShowsTrimester1 = conditionFactory.getOrCreate(containsText(clinicalNotes, "12/40 weeks"))
        val notesDoesNotMentionPregnancy = conditionFactory.getOrCreate(doesNotContainText(clinicalNotes, "/40 weeks"))
        val notesShowsTryingForBaby = conditionFactory.getOrCreate(containsText( clinicalNotes, "Trying for a baby"))
        val tiredness = conditionFactory.getOrCreate(containsText(clinicalNotes, "very tired"))
        val thyroxineReplacement1Week = conditionFactory.getOrCreate(containsText(clinicalNotes, "started T4 replacement 1 week ago"))
        val t4Replacement = conditionFactory.getOrCreate(containsText(clinicalNotes, "On T4 replacement"))
        val historyThyroidCancer = conditionFactory.getOrCreate(containsText( clinicalNotes, "thyroid cancer"))
        val onThyroxine = conditionFactory.getOrCreate(containsText(clinicalNotes, "On thyroxine"))
        val onAmiodarone = conditionFactory.getOrCreate(containsText(clinicalNotes, "On amiodarone"))
        val preI131Therapy = conditionFactory.getOrCreate(containsText(clinicalNotes, "Pre I-131 Thyrogen therapy"))

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
        val report9b = "Suggest repeat TFT measurement at least 4–6 weeks after commencement of T4 replacement. "
        val report10 = "The normal TSH and FT4 are consistent with adequate thyroid hormone replacement."
        val report11 = "Increased TSH suggests inadequate thyroid hormone replacement if the dose has not been changed for at least 6 weeks and patient \n" +
                "has been taking the medication regularly. \n" +
                "Suggest review dose and repeat TFTs in 6 weeks."
        val report12 = "Suppressed TSH is consistent with excessive thyroid hormone replacement."
        val report13 = "Previous history of thyroid cancer noted. Low TSH may be appropriate depending on treatment targets for this patient."
        val report14 = "Borderline TSH persists. Suggest repeat in one year with thyroid autoantibodies (TPO antibodies)."
        val report15 = "The suppressed TSH and high-normal FT4 may suggest hyperthyroidism. FT3 and TRAb may be useful. However, low " +
                "TSH may be seen in pregnancy which should be excluded. These results are within reference intervals for first trimester. If pregnant, " +
                "repeat TFTs in 6 weeks."
        val report16 = "Clinical conditions associated with a suppressed TSH include non-toxic goitre, subclinical hyperthyroidism and glucocorticoid " +
                "therapy. Suggest repeat TFTs in six weeks’ time. Other causes of this pattern include: Excessive T4 therapy for hypothyroidism, treated " +
                "primary hyperthyroidism. Acute psychiatric illness may raise FT4 and/or lower TSH."
        val report16b = "The increased FT3 and suppressed TSH are consistent with T3 toxicosis. Suggest measure TRAb."
        val report17 = "The severely increased FT4 and FT3 and suppressed TSH are consistent with thyrotoxicosis. These results together with the clinical presentation may indicate thyroid storm. Suggest measure TRAb."
        val report18 = "The reduced FT4 is consistent with excessive anti-thyroid treatment. The suppressed TSH may take many months to normalise " +
                "following commencement of ant-thyroid treatment."
        val report19 = "Thyrogen [thyrotropin alpha (recombinant human TSH)] in blood is measured by the TSH assay."
        val report20 = "Results should be interpreted in the context of serial measurement."

        val report20b = "The positive anti thyroglobulin antibodies may interfere with this thyroglobulin immunometric assay and cause a falsely low result " +
                "making the thyroglobulin result unreliable. Anti-Tg Ab trends may be used as a surrogate tumour marker for monitoring."
        val report21 = "The mildly increased TSH with normal FT4 and raised TPO antibodies indicate subclinical hypothyroidism due to autoimmune " +
                "thyroid disease. FT3 measurement is helpful only in diagnosing hyperthyroidism (or in monitoring FT3 supplementation)."
        val report22 = "Amiodarone inhibits T4 to T3 conversion as well as presenting the thyroid with a large iodine load. The suppressed TSH and raised " +
                "FT4 may suggest amiodarone-induced hyperthyroidism but should be interpreted in the light of clinical findings."

        addCommentForCase("1.4.2", report1b, tshNormal)
        replaceCommentForCase("1.4.1", report1b, report1, fT4Normal)
        replaceCommentForCase("1.4.3", report1b, report2, ft4SlightlyLow)
        addCommentForCase("1.4.4", report3, tshHigh )
        replaceCommentForCase("1.4.5", report3, report3b, fT4Normal)
        replaceCommentForCase("1.4.6", report3b, report4, elderly)

        replaceCommentForCase("1.4.7", report3b, report5, tshModeratelyIncreased)
        addCommentForCase("1.4.8", report6, tshLow, notesShowsTrimester1)
        replaceCommentForCase("1.4.9", report3b, report7, tpoHigh, notesShowsTryingForBaby)
        addCommentForCase("1.4.10", report8, tshLow, fT4Normal, tiredness) // tiredness needed to exclude 1.4.8
        addCommentForCase("1.4.11", report8b, ft3High, tshLow, fT4Normal)
        replaceCommentForCase("1.4.12", report3, report9, tshVeryHigh, ft4VeryLow)
        replaceCommentForCase("1.4.13", report3, report9b, thyroxineReplacement1Week)
        replaceCommentForCase("1.4.14", report1, report10, t4Replacement)
        replaceCommentForCase("1.4.15", report3b, report11, t4Replacement)
        addCommentForCase("1.4.16", report12, tshVeryLow, t4Replacement)
        addCommentForCase("1.4.17", report13, tshLow, onThyroxine, historyThyroidCancer)
        replaceCommentForCase("1.4.18", report1, report14, borderlineHighTSH, atLeastTwoTSH)

        // We are assuming that the absence of "/40" means the patient is not pregnant.
        addCommentForCase("1.4.19", report15, female, olderThan14, youngerThan44, borderlineHighFT4, tshLow, notesDoesNotMentionPregnancy)

        // Not sure if olderThan44 is needed.
        addCommentForCase("1.4.20", report16, tshBelowDetection, borderlineHighFT3, fT4Normal, olderThan44)

        addCommentForCase("1.4.21", report16b, tshBelowDetection, ft3Increasing, ft3High )
        addCommentForCase("1.4.22", report17, tshBelowDetection, severelyHighFT4, severelyHighFT3 )
        addCommentForCase("1.4.23", report18, tshBelowDetection, ft4Low)
        replaceCommentForCase("1.4.24", report3, report19, tshAbove100, preI131Therapy)
        addCommentForCase("1.4.25", report20, thyroglobulinAvailable, antiThyroglobulinAvailable, onlyOneThyroglobulin)

        replaceCommentForCase("1.4.26", report20, report20b, thyroglobulinBelowDetection, antiThyroglobulinHigh)
        replaceCommentForCase("1.4.27", report3b, report21, borderlineHighTSH, fT4Normal, tpoHigh, ft3Available)
        addCommentForCase("1.4.28", report22, tshBelowDetection, ft4High, onAmiodarone)
    }

    private fun addCommentForCase(caseName: String, comment: String, vararg conditions: Condition) {
        val restClient = RESTClient()
        restClient.getCaseWithName(caseName)
        val conclusion = conclusionFactory.getOrCreate(comment)
        restClient.startSessionToAddConclusionForCurrentCase(conclusion)
        conditions.forEach {
            try {
                restClient.addConditionForCurrentSession(it)
            } catch (e: Throwable) {
                println("Could not add condition: $it")
                throw e
            }
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