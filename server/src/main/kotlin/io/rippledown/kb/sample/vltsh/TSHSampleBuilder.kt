package io.rippledown.kb.sample.vltsh

import io.rippledown.kb.sample.SampleRuleBuilder
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.SeriesCondition
import io.rippledown.model.condition.episodic.predicate.GreaterThanOrEquals
import io.rippledown.model.condition.episodic.predicate.IsNotBlank
import io.rippledown.model.condition.episodic.predicate.IsNumeric
import io.rippledown.model.condition.episodic.predicate.NormalOrHighByAtMostSomePercentage
import io.rippledown.model.condition.episodic.signature.All
import io.rippledown.model.condition.episodic.signature.AtLeast
import io.rippledown.model.condition.episodic.signature.AtMost
import io.rippledown.model.condition.episodic.signature.Current
import io.rippledown.model.condition.series.Increasing
import io.rippledown.server.KBEndpoint

class TSHSampleBuilder(private val kbe: KBEndpoint)  {

    fun buildTSHRules() {
        setupTSHSampleCases()
        TSHRulesBuilder(kbe).buildRules()
    }

    fun setupTSHSampleCases() {
        setupTSHAttributes()
        val tshCases = TSHCases(kbe.kb.attributeManager)
        kbe.kb.addProcessedCase(tshCases.TSH1)
        kbe.kb.addProcessedCase(tshCases.TSH2)
        kbe.kb.addProcessedCase(tshCases.TSH3)
        kbe.kb.addProcessedCase(tshCases.TSH4)
        kbe.kb.addProcessedCase(tshCases.TSH5)
        kbe.kb.addProcessedCase(tshCases.TSH6)
        kbe.kb.addProcessedCase(tshCases.TSH7)
        kbe.kb.addProcessedCase(tshCases.TSH8)
        kbe.kb.addProcessedCase(tshCases.TSH9)
        kbe.kb.addProcessedCase(tshCases.TSH10)
        kbe.kb.addProcessedCase(tshCases.TSH11)
        kbe.kb.addProcessedCase(tshCases.TSH12)
        kbe.kb.addProcessedCase(tshCases.TSH13)
        kbe.kb.addProcessedCase(tshCases.TSH14)
        kbe.kb.addProcessedCase(tshCases.TSH15)
        kbe.kb.addProcessedCase(tshCases.TSH16)
        kbe.kb.addProcessedCase(tshCases.TSH17)
        kbe.kb.addProcessedCase(tshCases.TSH18)
        kbe.kb.addProcessedCase(tshCases.TSH19)
        kbe.kb.addProcessedCase(tshCases.TSH20)
        kbe.kb.addProcessedCase(tshCases.TSH21)
        kbe.kb.addProcessedCase(tshCases.TSH22)
        kbe.kb.addProcessedCase(tshCases.TSH23)
        kbe.kb.addProcessedCase(tshCases.TSH24)
        kbe.kb.addProcessedCase(tshCases.TSH25)
        kbe.kb.addProcessedCase(tshCases.TSH26)
        kbe.kb.addProcessedCase(tshCases.TSH27)
        kbe.kb.addProcessedCase(tshCases.TSH28)
        kbe.kb.addProcessedCase(tshCases.TSH29)
        kbe.kb.addProcessedCase(tshCases.TSH30)
        kbe.kb.addProcessedCase(tshCases.TSH31)
        kbe.kb.addProcessedCase(tshCases.TSH32)
        kbe.kb.addProcessedCase(tshCases.TSH33)
        kbe.kb.addProcessedCase(tshCases.TSH35)
    }

    private fun setupTSHAttributes() {
        // We create the attributes ahead of time and set their order
        // so that the order in the case view is well-defined.
        val attributeNamesInOrder = listOf(
            "Sex",
            "Age",
            "TSH",
            "Free T4",
            "Free T3",
            "TPO Antibodies",
            "Thyroglobulin",
            "Anti-Thyroglobulin",
            "Sodium",
            "Potassium",
            "Bicarbonate",
            "Urea",
            "Creatinine",
            "eGFR",
            "Patient Location",
            "Tests",
            "Clinical Notes",
        )
        val attributesInOrder = attributeNamesInOrder.map {
            kbe.getOrCreateAttribute(it)
        }
        kbe.setAttributeOrder(attributesInOrder)
    }
}
class TSHRulesBuilder(kbe: KBEndpoint) : SampleRuleBuilder(kbe) {
    fun buildRules() {
        val tsh = kbe.getOrCreateAttribute("TSH")
        val ft4 = kbe.getOrCreateAttribute("Free T4")
        val ft3 = kbe.getOrCreateAttribute("Free T3")
        val tpo = kbe.getOrCreateAttribute("TPO Antibodies")
        val thyroglobulin = kbe.getOrCreateAttribute("Thyroglobulin")
        val antiThyroglobulin = kbe.getOrCreateAttribute("Anti-Thyroglobulin")
        val potassium = kbe.getOrCreateAttribute("Potassium")
        val clinicalNotes = kbe.getOrCreateAttribute("Clinical Notes")
        val sex = kbe.getOrCreateAttribute("Sex")
        val age = kbe.getOrCreateAttribute("Age")

        val tshBelowDetection = kbe.getOrCreateCondition(isCondition(tsh, "<0.01"))
        val tshVeryLow = kbe.getOrCreateCondition(lessThanOrEqualTo(tsh, 0.1)) // What should this be?
        val tshLow = kbe.getOrCreateCondition(isLow(tsh))
        val tshNormal = kbe.getOrCreateCondition(isNormal(tsh))
        val borderline10HighTSH = kbe.getOrCreateCondition((EpisodicCondition(tsh, NormalOrHighByAtMostSomePercentage(10), All)))
        val borderline20HighTSH = kbe.getOrCreateCondition((EpisodicCondition(tsh, NormalOrHighByAtMostSomePercentage(20), All)))
        val tshSlightlyIncreased = kbe.getOrCreateCondition(greaterThanOrEqualTo(tsh, 10.0))
        val tshHigh = kbe.getOrCreateCondition(isHigh(tsh))
        val tshVeryHigh = kbe.getOrCreateCondition(greaterThanOrEqualTo( tsh, 40.0)) // What should this be?
        val tshProfoundlyHigh = kbe.getOrCreateCondition(greaterThanOrEqualTo( tsh, 100.0)) //  should this be?
        val atLeastTwoTSH = kbe.getOrCreateCondition(EpisodicCondition(tsh, IsNumeric, AtLeast(2)))

        val ft4VeryLow = kbe.getOrCreateCondition(isCondition(ft4, "<5"))
        val ft4SlightlyLow = kbe.getOrCreateCondition(slightlyLow(ft4, 20))
        val ft4Low = kbe.getOrCreateCondition(isLow(ft4))
        val fT4Normal = kbe.getOrCreateCondition(isNormal( ft4))
        val borderlineHighFT4 = kbe.getOrCreateCondition(EpisodicCondition(ft4, NormalOrHighByAtMostSomePercentage(10), All))
        val ft4High = kbe.getOrCreateCondition(isHigh(ft4))
        val severelyHighFT4 =  kbe.getOrCreateCondition(EpisodicCondition(ft4, GreaterThanOrEquals(40.0), Current))

        val ft3Available = kbe.getOrCreateCondition(isPresent(ft3))
        val ft3NotAvailable = kbe.getOrCreateCondition(isNotPresent(ft3))
        val ft3High = kbe.getOrCreateCondition(isHigh(ft3))
        val ft3Increasing = kbe.getOrCreateCondition((SeriesCondition(null, ft3, Increasing, "")))
        val borderlineHighFT3 = kbe.getOrCreateCondition((EpisodicCondition(ft3, NormalOrHighByAtMostSomePercentage(10), All)))
        val severelyHighFT3 =  kbe.getOrCreateCondition(EpisodicCondition(ft3, GreaterThanOrEquals(10.0), Current))

        val tpoHigh = kbe.getOrCreateCondition(isHigh(tpo))

        val thyroglobulinAvailable = kbe.getOrCreateCondition(isPresent(thyroglobulin))
        val thyroglobulinBelowDetection = kbe.getOrCreateCondition(isCondition(thyroglobulin, "<0.1"))
        val onlyOneThyroglobulin = kbe.getOrCreateCondition(EpisodicCondition(thyroglobulin, IsNotBlank, AtMost(2)))
        val antiThyroglobulinAvailable = kbe.getOrCreateCondition(isPresent(antiThyroglobulin))
        val antiThyroglobulinHigh = kbe.getOrCreateCondition(isHigh(antiThyroglobulin))

        val lowPotassium = kbe.getOrCreateCondition(isLow(potassium))

        val olderThan14 = kbe.getOrCreateCondition(greaterThanOrEqualTo(age, 14.0))
        val youngerThan44 = kbe.getOrCreateCondition(lessThanOrEqualTo(age, 44.0))
        val olderThan44 = kbe.getOrCreateCondition(greaterThanOrEqualTo(age, 45.0))
        val elderly = kbe.getOrCreateCondition(greaterThanOrEqualTo(age, 70.0))

        val female = kbe.getOrCreateCondition(isCondition(sex, "F"))

        val notesShowsTrimester1 = kbe.getOrCreateCondition(containsText(clinicalNotes, "12/40 weeks"))
        val notesDoesNotMentionPregnancy = kbe.getOrCreateCondition(doesNotContainText(clinicalNotes, "/40 weeks"))
        val notesShowsTryingForBaby = kbe.getOrCreateCondition(containsText( clinicalNotes, "Trying for a baby"))
        val tiredness = kbe.getOrCreateCondition(containsText(clinicalNotes, "very tired"))
        val thyroxineReplacement1Week = kbe.getOrCreateCondition(containsText(clinicalNotes, "started T4 replacement 1 week ago"))
        val t4Replacement = kbe.getOrCreateCondition(containsText(clinicalNotes, "On T4 replacement"))
        val historyThyroidCancer = kbe.getOrCreateCondition(containsText( clinicalNotes, "thyroid cancer"))
        val onThyroxine = kbe.getOrCreateCondition(containsText(clinicalNotes, "On thyroxine"))
        val onT4 = kbe.getOrCreateCondition(containsText(clinicalNotes, "On T4"))
        val onAmiodarone = kbe.getOrCreateCondition(containsText(clinicalNotes, "On amiodarone"))
        val preI131Therapy = kbe.getOrCreateCondition(containsText(clinicalNotes, "Pre I-131 Thyrogen therapy"))

        val report1 = "Normal T4 and TSH are consistent with a euthyroid state."
        val report1b = "Normal TSH is consistent with a euthyroid state."
        val report2 = "A mildly reduced FT4 with a normal TSH may be due to non-thyroidal illness or pituitary hypothyroidism."
        val report3 = "Mildly increased TSH may be found in patients with subclinical hypothyroidism or sick euthyroid syndrome. " +
                "Suggest measurement of FT4, TSH and thyroperoxidase (TPO) antibodies in 6 weeks."
        val report3b = "Mildly increased TSH with a normal FT4 may be found in patients with subclinical hypothyroidism or sick euthyroid syndrome. " +
                "Suggest repeat measurement with TPO antibodies in 6 weeks."
        val report4 = "Mildly increased TSH with a normal FT4 can be seen in the euthyroid elderly."
        val report5 = "A moderately increased TSH with a normal FT4 is consistent with (mild) primary hypothyroidism."
        val report6 = "TSH reference intervals in pregnancy.\n" +
                "1st trimester                          0.02–2.5\n" +
                "2nd and 3rd trimester                  0.30–3.0"
        val report7 = "The mildly increased TSH and raised TPO antibodies indicate subclinical hypothyroidism due to autoimmune thyroid disease.\n" +
                "Suggest confirming subclinical hypothyroidism by repeat testing. Poor pregnancy outcomes have been described in women with a\n" +
                "raised TSH. If raised TSH confirmed, consider thyroxine replacement."
        val report8 = "The suppressed TSH and normal FT4 are consistent with subclinical hyperthyroidism.\n" +
                "Suggest measure free triiodothyronine (FT3)."
        val report8b = "The increased FT3 and suppressed TSH (with a normal FT4) are consistent with T3 toxicosis. " +
                "Suggest measure TSH-receptor antibodies (TRAb)."
        val report9 = "The severely increased TSH with a very low FT4 is consistent with primary hypothyroidism. Suggest measure TPO antibodies."
        val report9b = "Suggest repeat TFT measurement at least 4–6 weeks after commencement of T4 replacement."
        val report10 = "The normal TSH and FT4 are consistent with adequate thyroid hormone replacement."
        val report11 = "Increased TSH suggests inadequate thyroid hormone replacement if the dose has not been changed for at least 6 weeks and patient\n" +
                "has been taking the medication regularly.\n" +
                "Suggest review dose and repeat TFTs in 6 weeks."
        val report12 = "Suppressed TSH is consistent with excessive thyroid hormone replacement."
        val report13 = "Previous history of thyroid cancer noted. Low TSH may be appropriate depending on treatment targets for this patient."
        val report14 = "Borderline TSH persists. Suggest repeat in one year with thyroid autoantibodies (TPO antibodies)."
        val report15 = "The suppressed TSH and high-normal FT4 may suggest hyperthyroidism. FT3 and TRAb may be useful. However, low\n" +
                "TSH may be seen in pregnancy which should be excluded. These results are within reference intervals for first trimester. If pregnant,\n" +
                "repeat TFTs in 6 weeks."
        val report16 = "Clinical conditions associated with a suppressed TSH include non-toxic goitre, subclinical hyperthyroidism and glucocorticoid\n" +
                "therapy. Suggest repeat TFTs in six weeks’ time. Other causes of this pattern include: Excessive T4 therapy for hypothyroidism, treated\n" +
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
        val report23 = "Normal TSH indicates a euthyroid state. Causes of a raised FT4 with reduced T4/T3 conversion include non-thyroidal illness, drugs " +
                "(beta-blockers, amiodarone, heparin, radiocontrast) and treated thyroid disease.\n" +
                "Suggest measure FT3 if not on treatment."
//        val report23b = "FT4 and TSH results confirmed by alternative method. Heterophile antibody excluded for TSH. Consider specialist Endocrine " +
//                "referral to test for TSH secreting tumour or thyroid hormone resistance."
        val report24 = "The raised FT4 and FT3 with suppressed TSH are consistent with thyrotoxicosis. Hyperthyroidism with hypokalaemia and muscle " +
                "weakness may be consistent with thyrotoxic periodic paralysis."
        val report25 = "The presence of a low FT4 with only a marginal increase in TSH may suggest pituitary insufficiency, although these results may also " +
                "be seen in non-thyroidal illness. Suggest further pituitary investigations or Specialist Endocrine referral if abnormalities persist."
        val report26 = "FT4 should be maintained within the upper reference interval in patients on thyroxine for secondary hypothyroidism. Suggest " +
                "review T4 dose (and adherence to therapy) based on clinical assessment."
        val report27b = "The low FT4 and profoundly raised TSH are in keeping with severe hypothyroidism."

        addCommentForCase("1.4.2", report1b, tshNormal)
        replaceCommentForCase("1.4.1", report1b, report1, fT4Normal)
        replaceCommentForCase("1.4.3", report1b, report2, ft4SlightlyLow)
        addCommentForCase("1.4.4", report3, tshHigh )
        replaceCommentForCase("1.4.5", report3, report3b, fT4Normal)
        replaceCommentForCase("1.4.6", report3b, report4, elderly)

        replaceCommentForCase("1.4.7", report3b, report5, tshSlightlyIncreased)
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
        replaceCommentForCase("1.4.18", report1, report14, borderline10HighTSH, atLeastTwoTSH)

        // We are assuming that the absence of "/40" means the patient is not pregnant.
        addCommentForCase("1.4.19", report15, female, olderThan14, youngerThan44, borderlineHighFT4, tshLow, notesDoesNotMentionPregnancy)

        // Not sure if olderThan44 is needed.
        addCommentForCase("1.4.20", report16, tshBelowDetection, borderlineHighFT3, fT4Normal, olderThan44)

        addCommentForCase("1.4.21", report16b, tshBelowDetection, ft3Increasing, ft3High )
        addCommentForCase("1.4.22", report17, tshBelowDetection, severelyHighFT4, severelyHighFT3 )
        addCommentForCase("1.4.23", report18, tshBelowDetection, ft4Low)
        replaceCommentForCase("1.4.24", report3, report19, tshProfoundlyHigh, preI131Therapy)
        addCommentForCase("1.4.25", report20, thyroglobulinAvailable, antiThyroglobulinAvailable, onlyOneThyroglobulin)

        replaceCommentForCase("1.4.26", report20, report20b, thyroglobulinBelowDetection, antiThyroglobulinHigh)
        replaceCommentForCase("1.4.27", report3b, report21, borderline10HighTSH, fT4Normal, tpoHigh, ft3Available)
        addCommentForCase("1.4.28", report22, tshBelowDetection, ft4High, onAmiodarone)

        replaceCommentForCase("1.4.29", report1b, report23, tshNormal, ft4High, ft3NotAvailable)
//        replaceCommentForCase("1.4.30", report1b, report23b)
        replaceCommentForCase("1.4.31", report17, report24, severelyHighFT4, severelyHighFT3, tshBelowDetection, lowPotassium)
        replaceCommentForCase("1.4.32", report3, report25, ft4Low, borderline20HighTSH)
        addCommentForCase("1.4.33", report26, ft4Low, onT4)
        replaceCommentForCase("1.4.35", report3, report27b, ft4Low, tshProfoundlyHigh)
    }
}