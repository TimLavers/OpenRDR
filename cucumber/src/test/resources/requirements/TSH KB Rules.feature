Feature: The reports for the cases described in the TSH paper can be generated using OpenRDR rules

  Scenario: TSH KB rules
    Given the TSH sample KB has been loaded
    And I start the client application
    Then the count of the number of cases is 34

    When I select case 1.4.1
    Then the interpretation should be this:
      """
      Normal T4 and TSH are consistent with a euthyroid state.
      """

    When I select case 1.4.2
    Then the interpretation should be this:
      """
      Normal TSH is consistent with a euthyroid state.
      """

    When I select case 1.4.3
    Then the interpretation should be this:
      """
      A mildly reduced FT4 with a normal TSH may be due to non-thyroidal illness or pituitary hypothyroidism.
      """

    When I select case 1.4.4
    Then the interpretation should be this:
      """
      Mildly increased TSH may be found in patients with subclinical hypothyroidism or sick euthyroid syndrome. Suggest measurement of FT4, TSH and thyroperoxidase (TPO) antibodies in 6 weeks.
      """

    When I select case 1.4.5
    Then the interpretation should be this:
      """
      Mildly increased TSH with a normal FT4 may be found in patients with subclinical hypothyroidism or sick euthyroid syndrome. Suggest repeat measurement with TPO antibodies in 6 weeks.
      """

    When I select case 1.4.6
    Then the interpretation should be this:
      """
      Mildly increased TSH with a normal FT4 can be seen in the euthyroid elderly.
      """

    When I select case 1.4.7
    Then the interpretation should be this:
      """
      A moderately increased TSH with a normal FT4 is consistent with (mild) primary hypothyroidism.
      """

    When I select case 1.4.8
    Then the interpretation should be this:
      """
      TSH reference intervals in pregnancy.
      1st trimester                          0.02–2.5
      2nd and 3rd trimester                  0.30–3.0
      """

    When I select case 1.4.9
    Then the interpretation should be this:
      """
      The mildly increased TSH and raised TPO antibodies indicate subclinical hypothyroidism due to autoimmune thyroid disease.
      Suggest confirming subclinical hypothyroidism by repeat testing. Poor pregnancy outcomes have been described in women with a
      raised TSH. If raised TSH confirmed, consider thyroxine replacement.
      """

    When I select case 1.4.10
    Then the interpretation should be this:
      """
      The suppressed TSH and normal FT4 are consistent with subclinical hyperthyroidism.
      Suggest measure free triiodothyronine (FT3).
      """

    When I select case 1.4.11
    Then the interpretation should be this:
      """
      The increased FT3 and suppressed TSH (with a normal FT4) are consistent with T3 toxicosis. Suggest measure TSH-receptor antibodies (TRAb).
      """

    When I select case 1.4.12
    Then the interpretation should be this:
      """
      The severely increased TSH with a very low FT4 is consistent with primary hypothyroidism. Suggest measure TPO antibodies.
      """

    When I select case 1.4.13
    Then the interpretation should be this:
      """
      Suggest repeat TFT measurement at least 4–6 weeks after commencement of T4 replacement.
      """

    When I select case 1.4.14
    Then the interpretation should be this:
      """
      The normal TSH and FT4 are consistent with adequate thyroid hormone replacement.
      """

    When I select case 1.4.15
    Then the interpretation should be this:
      """
      Increased TSH suggests inadequate thyroid hormone replacement if the dose has not been changed for at least 6 weeks and patient
      has been taking the medication regularly.
      Suggest review dose and repeat TFTs in 6 weeks.
      """

    When I select case 1.4.16
    Then the interpretation should be this:
      """
      Suppressed TSH is consistent with excessive thyroid hormone replacement.
      """

    When I select case 1.4.17
    Then the interpretation should be this:
      """
      Previous history of thyroid cancer noted. Low TSH may be appropriate depending on treatment targets for this patient.
      """

    When I select case 1.4.18
    Then the interpretation should be this:
      """
      Borderline TSH persists. Suggest repeat in one year with thyroid autoantibodies (TPO antibodies).
      """

    When I select case 1.4.19
    Then the interpretation should be this:
      """
      The suppressed TSH and high-normal FT4 may suggest hyperthyroidism. FT3 and TRAb may be useful. However, low
      TSH may be seen in pregnancy which should be excluded. These results are within reference intervals for first trimester. If pregnant,
      repeat TFTs in 6 weeks.
      """

    When I select case 1.4.20
    Then the interpretation should be this:
      """
      Clinical conditions associated with a suppressed TSH include non-toxic goitre, subclinical hyperthyroidism and glucocorticoid
      therapy. Suggest repeat TFTs in six weeks’ time. Other causes of this pattern include: Excessive T4 therapy for hypothyroidism, treated
      primary hyperthyroidism. Acute psychiatric illness may raise FT4 and/or lower TSH.
      """

    When I select case 1.4.21
    Then the interpretation should be this:
      """
      The increased FT3 and suppressed TSH are consistent with T3 toxicosis. Suggest measure TRAb.
      """

    When I select case 1.4.22
    Then the interpretation should be this:
      """
      The severely increased FT4 and FT3 and suppressed TSH are consistent with thyrotoxicosis. These results together with the clinical presentation may indicate thyroid storm. Suggest measure TRAb.
      """

    When I select case 1.4.23
    Then the interpretation should be this:
      """
      The reduced FT4 is consistent with excessive anti-thyroid treatment. The suppressed TSH may take many months to normalise following commencement of ant-thyroid treatment.
      """

    When I select case 1.4.24
    Then the interpretation should be this:
      """
      Thyrogen [thyrotropin alpha (recombinant human TSH)] in blood is measured by the TSH assay.
      """

    When I select case 1.4.25
    Then the interpretation should be this:
      """
      Results should be interpreted in the context of serial measurement.
      """

    When I select case 1.4.26
    Then the interpretation should be this:
      """
      The positive anti thyroglobulin antibodies may interfere with this thyroglobulin immunometric assay and cause a falsely low result making the thyroglobulin result unreliable. Anti-Tg Ab trends may be used as a surrogate tumour marker for monitoring.
      """

    When I select case 1.4.27
    Then the interpretation should be this:
      """
      The mildly increased TSH with normal FT4 and raised TPO antibodies indicate subclinical hypothyroidism due to autoimmune thyroid disease. FT3 measurement is helpful only in diagnosing hyperthyroidism (or in monitoring FT3 supplementation).
      """

    When I select case 1.4.28
    Then the interpretation should be this:
      """
      Amiodarone inhibits T4 to T3 conversion as well as presenting the thyroid with a large iodine load. The suppressed TSH and raised FT4 may suggest amiodarone-induced hyperthyroidism but should be interpreted in the light of clinical findings.
      """

    When I select case 1.4.29
    Then the interpretation should be this:
      """
      Normal TSH indicates a euthyroid state. Causes of a raised FT4 with reduced T4/T3 conversion include non-thyroidal illness, drugs (beta-blockers, amiodarone, heparin, radiocontrast) and treated thyroid disease.
      Suggest measure FT3 if not on treatment.
      """

# The comment and explanation fpr case 1.4.30 don't really make sense unless the case
# is a later episode for the same patient as in 1.4.29, but the FT4 and THS values are
# identical in the cases, which seems unlikely. So it's not clear what is going on.

    When I select case 1.4.31
    Then the interpretation should be this:
      """
      The raised FT4 and FT3 with suppressed TSH are consistent with thyrotoxicosis. Hyperthyroidism with hypokalaemia and muscle weakness may be consistent with thyrotoxic periodic paralysis.
      """

    When I select case 1.4.32
    Then the interpretation should be this:
      """
      The presence of a low FT4 with only a marginal increase in TSH may suggest pituitary insufficiency, although these results may also be seen in non-thyroidal illness. Suggest further pituitary investigations or Specialist Endocrine referral if abnormalities persist.
      """

    When I select case 1.4.33
    Then the interpretation should be this:
      """
      FT4 should be maintained within the upper reference interval in patients on thyroxine for secondary hypothyroidism. Suggest review T4 dose (and adherence to therapy) based on clinical assessment.
      """

# We haven't done case 1.4.34 as it is a chemistry panel.

    When I select case 1.4.35
    Then the interpretation should be this:
      """
      The low FT4 and profoundly raised TSH are in keeping with severe hypothyroidism.
      """

    And stop the client application
