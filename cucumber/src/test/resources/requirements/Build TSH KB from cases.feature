Feature: The TSH KB can be built with the user interface.

  @single
  Scenario: Build the TSH KB from cases
    Given I start the client application
    And I create a Knowledge Base with the name TSH based on the "Thyroid Stimulating Hormone - cases only" sample
    Then the count of the number of cases is 34

  #        addCommentForCase("1.4.2", report1b, tshNormal)
    And I select case 1.4.2
    And I build a rule to add the comment "Normal TSH is consistent with a euthyroid state." with conditions
      | TSH is normal |

#        replaceCommentForCase("1.4.1", report1b, report1, fT4Normal)
    And I select case 1.4.1
    And I build a rule to replace the comment "Normal TSH is consistent with a euthyroid state." with the comment "Normal T4 and TSH are consistent with a euthyroid state." with conditions
      | Free T4 is normal |

#        replaceCommentForCase("1.4.3", report1b, report2, ft4SlightlyLow)
    And I select case 1.4.3
    And I build a rule to replace the comment "Normal TSH is consistent with a euthyroid state." with the comment "A mildly reduced FT4 with a normal TSH may be due to non-thyroidal illness or pituitary hypothyroidism." with conditions
      | Free T4 is low by at most 20% | Free T4 is low by at most | 20 |

  #        addCommentForCase("1.4.4", report3, tshHigh )
    And I select case 1.4.4
    And I build a rule to add the comment "Mildly increased TSH may be found in patients with subclinical hypothyroidism or sick euthyroid syndrome. Suggest measurement of FT4, TSH and thyroperoxidase (TPO) antibodies in 6 weeks." with conditions
      | TSH is high |

#            replaceCommentForCase("1.4.5", report3, report3b, fT4Normal)
    And I select case 1.4.5
    And I build a rule to replace the comment "Mildly increased TSH may be found in patients with subclinical hypothyroidism or sick euthyroid syndrome. Suggest measurement of FT4, TSH and thyroperoxidase (TPO) antibodies in 6 weeks." with the comment "Mildly increased TSH with a normal FT4 may be found in patients with subclinical hypothyroidism or sick euthyroid syndrome. Suggest repeat measurement with TPO antibodies in 6 weeks." with conditions
      | Free T4 is normal |

#        replaceCommentForCase("1.4.6", report3b, report4, elderly)
    And I select case 1.4.6
    And I build a rule to replace the comment "Mildly increased TSH with a normal FT4 may be found in patients with subclinical hypothyroidism or sick euthyroid syndrome. Suggest repeat measurement with TPO antibodies in 6 weeks." with the comment "Mildly increased TSH with a normal FT4 can be seen in the euthyroid elderly." with conditions
      | Age ≥ 70.0 | Age ≥ | 70 |

#        replaceCommentForCase("1.4.7", report3b, report5, tshSlightlyIncreased)
    And I select case 1.4.7
    And I build a rule to replace the comment "Mildly increased TSH with a normal FT4 may be found in patients with subclinical hypothyroidism or sick euthyroid syndrome. Suggest repeat measurement with TPO antibodies in 6 weeks." with the comment "A moderately increased TSH with a normal FT4 is consistent with (mild) primary hypothyroidism." with conditions
      | TSH ≥ 10.0 | TSH ≥ | 10 |

  #        addCommentForCase("1.4.8", report6, tshLow, notesShowsTrimester1)
    And I select case 1.4.8
    And I build a rule to add the comment "TSH reference intervals in pregnancy: 1st trimester 0.02–2.5, 2nd and 3rd trimester 0.30–3.0" with conditions
      | Clinical Notes contains "12/40 weeks" | Clinical Notes contains | 12/40 weeks |

#        replaceCommentForCase("1.4.9", report3b, report7, tpoHigh, notesShowsTryingForBaby)
    And I select case 1.4.9
    And I build a rule to replace the comment "Mildly increased TSH with a normal FT4 may be found in patients with subclinical hypothyroidism or sick euthyroid syndrome. Suggest repeat measurement with TPO antibodies in 6 weeks." with the comment "The mildly increased TSH and raised TPO antibodies indicate subclinical hypothyroidism due to autoimmune thyroid disease. Suggest confirming subclinical hypothyroidism by repeat testing. Poor pregnancy outcomes have been described in women with a raised TSH. If raised TSH confirmed, consider thyroxine replacement." with conditions
      | Clinical Notes contains "Trying for a baby" | Clinical Notes contains | Trying for a baby |

#            addCommentForCase("1.4.10", report8, tshLow, fT4Normal, tiredness) // tiredness needed to exclude 1.4.8
    And I select case 1.4.10
    And I build a rule to add the comment "The suppressed TSH and normal FT4 are consistent with subclinical hyperthyroidism. Suggest measure free triiodothyronine (FT3)." with conditions
      | TSH is low | | |
      | Free T4 is normal | | |
      | Clinical Notes contains "very tired" | Clinical Notes contains | very tired |

#        addCommentForCase("1.4.11", report8b, ft3High, tshLow, fT4Normal)
    And I select case 1.4.11
    And I build a rule to add the comment "The increased FT3 and suppressed TSH (with a normal FT4) are consistent with T3 toxicosis. Suggest measure TSH-receptor antibodies (TRAb)." with conditions
      | TSH is low |
      | Free T3 is high |
      | Free T4 is normal |

  #        replaceCommentForCase("1.4.12", report3, report9, tshVeryHigh, ft4VeryLow)
    And I select case 1.4.12
    And I build a rule to replace the comment "Mildly increased TSH may be found in patients with subclinical hypothyroidism or sick euthyroid syndrome. Suggest measurement of FT4, TSH and thyroperoxidase (TPO) antibodies in 6 weeks." with the comment "The severely increased TSH with a very low FT4 is consistent with primary hypothyroidism. Suggest measure TPO antibodies." with conditions
      | TSH ≥ 40.0 | TSH ≥ | 40.0 |
      | Free T4 is "<5" | | |

#        replaceCommentForCase("1.4.13", report3, report9b, thyroxineReplacement1Week)
    And I select case 1.4.13
    And I build a rule to replace the comment "Mildly increased TSH may be found in patients with subclinical hypothyroidism or sick euthyroid syndrome. Suggest measurement of FT4, TSH and thyroperoxidase (TPO) antibodies in 6 weeks." with the comment "The severely increased TSH with a very low FT4 is consistent with primary hypothyroidism. Suggest measure TPO antibodies." with conditions
      | Clinical Notes contains "started T4 replacement 1 week ago" | Clinical Notes contains | started T4 replacement 1 week ago |

  #        replaceCommentForCase("1.4.14", report1, report10, t4Replacement)
    And I select case 1.4.14
    And I build a rule to replace the comment "Normal T4 and TSH are consistent with a euthyroid state." with the comment "The normal TSH and FT4 are consistent with adequate thyroid hormone replacement." with conditions
      | Clinical Notes contains "On T4 replacement" | Clinical Notes contains | On T4 replacement |

  #        replaceCommentForCase("1.4.15", report3b, report11, t4Replacement)
    And I select case 1.4.15
    And I build a rule to replace the comment "Mildly increased TSH with a normal FT4 may be found in patients with subclinical hypothyroidism or sick euthyroid syndrome. Suggest repeat measurement with TPO antibodies in 6 weeks." with the comment "Increased TSH suggests inadequate thyroid hormone replacement if the dose has not been changed for at least 6 weeks and patient has been taking the medication regularly. Suggest review dose and repeat TFTs in 6 weeks." with conditions
      | Clinical Notes contains "On T4 replacement" | Clinical Notes contains | On T4 replacement |

#           addCommentForCase("1.4.16", report12, tshVeryLow, t4Replacement)
    And I select case 1.4.16
    And I build a rule to add the comment "Suppressed TSH is consistent with excessive thyroid hormone replacement." with conditions
      | TSH ≤ 0.1 | TSH ≤ | 0.1 |
      | Clinical Notes contains "On T4 replacement" | Clinical Notes contains | On T4 replacement |

#        addCommentForCase("1.4.17", report13, tshLow, onThyroxine, historyThyroidCancer)
    And I select case 1.4.17
    And I build a rule to add the comment "Previous history of thyroid cancer noted. Low TSH may be appropriate depending on treatment targets for this patient." with conditions
      | Clinical Notes contains "thyroid cancer" | Clinical Notes contains | thyroid cancer |
      | Clinical Notes contains "On thyroxine" | Clinical Notes contains | On thyroxine |
      | TSH is low                             |                         |              |

#        replaceCommentForCase("1.4.18", report1, report14, borderline10HighTSH, atLeastTwoTSH)
    And I select case 1.4.18
    And I build a rule to replace the comment "Normal T4 and TSH are consistent with a euthyroid state." with the comment "Borderline TSH persists. Suggest repeat in one year with thyroid autoantibodies (TPO antibodies)." with conditions
      | TSH is normal or high by at most 10% | TSH is normal or high | 10 |
      | at least 2 TSH are numeric           |                       |    |


  #        // We are assuming that the absence of "/40" means the patient is not pregnant.
#        addCommentForCase("1.4.19", report15, female, olderThan14, youngerThan44, borderlineHighFT4, tshLow, notesDoesNotMentionPregnancy)
    And I select case 1.4.19
    And I build a rule to add the comment "The suppressed TSH and high-normal FT4 may suggest hyperthyroidism. FT3 and TRAb may be useful. However, low TSH may be seen in pregnancy which should be excluded. These results are within reference intervals for first trimester. If pregnant, repeat TFTs in 6 weeks." with conditions
      | Sex is "female" | | |
      | Age ≥ 14.0 | Age ≥ | 14 |
      | Age ≤ 44.0 | Age ≤ | 44 |
      | Free T4 is normal or high by at most 10% | Free T4 is normal or high        |  10 |
      | TSH is low                               |                                  |     |
      | Clinical Notes does not contain "/40"    |  Clinical Notes does not contain | /40 |

#  addCommentForCase("1.4.20", report16, tshBelowDetection, borderlineHighFT3, fT4Normal, olderThan44)
    And I select case 1.4.20
    And I build a rule to add the comment "Clinical conditions associated with a suppressed TSH include non-toxic goitre, subclinical hyperthyroidism and glucocorticoid therapy. Suggest repeat TFTs in six weeks’ time. Other causes of this pattern include: Excessive T4 therapy for hypothyroidism, treated primary hyperthyroidism. Acute psychiatric illness may raise FT4 and/or lower TSH." with conditions
      | TSH is "<0.01" | | |
      | Free T3 is normal or high by at most 10% | Free T3 is normal or high        |  10 |
      | Free T4 is normal |                                  |     |
      | Age ≥ 44.0 | Age ≥ | 44 |

#  addCommentForCase("1.4.21", report16b, tshBelowDetection, ft3Increasing, ft3High )
    And I select case 1.4.21
    And I build a rule to add the comment "The increased FT3 and suppressed TSH are consistent with T3 toxicosis. Suggest measure TRAb." with conditions
      | TSH is "<0.01"        |
      | Free T3 is increasing |
      | Free T3 is high       |

#  addCommentForCase("1.4.22", report17, tshBelowDetection, severelyHighFT4, severelyHighFT3 )
#  addCommentForCase("1.4.23", report18, tshBelowDetection, ft4Low)
#  replaceCommentForCase("1.4.24", report3, report19, tshProfoundlyHigh, preI131Therapy)
#  addCommentForCase("1.4.25", report20, thyroglobulinAvailable, antiThyroglobulinAvailable, onlyOneThyroglobulin)


    Then the cases should have interpretations as follows
      | 1.4.1  | Normal T4 and TSH are consistent with a euthyroid state. |
      | 1.4.2  | Normal TSH is consistent with a euthyroid state. |
      | 1.4.3  | A mildly reduced FT4 with a normal TSH may be due to non-thyroidal illness or pituitary hypothyroidism. |
      | 1.4.4  | Mildly increased TSH may be found in patients with subclinical hypothyroidism or sick euthyroid syndrome. Suggest measurement of FT4, TSH and thyroperoxidase (TPO) antibodies in 6 weeks. |
      | 1.4.5  | Mildly increased TSH with a normal FT4 may be found in patients with subclinical hypothyroidism or sick euthyroid syndrome. Suggest repeat measurement with TPO antibodies in 6 weeks. |
      | 1.4.6  | Mildly increased TSH with a normal FT4 can be seen in the euthyroid elderly. |
      | 1.4.7  | A moderately increased TSH with a normal FT4 is consistent with (mild) primary hypothyroidism. |
      | 1.4.8  | TSH reference intervals in pregnancy: 1st trimester 0.02–2.5, 2nd and 3rd trimester 0.30–3.0 |
      | 1.4.9  | The mildly increased TSH and raised TPO antibodies indicate subclinical hypothyroidism due to autoimmune thyroid disease. Suggest confirming subclinical hypothyroidism by repeat testing. Poor pregnancy outcomes have been described in women with a raised TSH. If raised TSH confirmed, consider thyroxine replacement. |
      | 1.4.10 | The suppressed TSH and normal FT4 are consistent with subclinical hyperthyroidism. Suggest measure free triiodothyronine (FT3). |
      | 1.4.11 | The increased FT3 and suppressed TSH (with a normal FT4) are consistent with T3 toxicosis. Suggest measure TSH-receptor antibodies (TRAb). |
      | 1.4.12 | The severely increased TSH with a very low FT4 is consistent with primary hypothyroidism. Suggest measure TPO antibodies. |
      | 1.4.13 | The severely increased TSH with a very low FT4 is consistent with primary hypothyroidism. Suggest measure TPO antibodies. |
      | 1.4.14 | The normal TSH and FT4 are consistent with adequate thyroid hormone replacement. |
      | 1.4.15 | Increased TSH suggests inadequate thyroid hormone replacement if the dose has not been changed for at least 6 weeks and patient has been taking the medication regularly. Suggest review dose and repeat TFTs in 6 weeks. |
      | 1.4.16 | Suppressed TSH is consistent with excessive thyroid hormone replacement. |
      | 1.4.17 | Previous history of thyroid cancer noted. Low TSH may be appropriate depending on treatment targets for this patient. |
      | 1.4.18 | Borderline TSH persists. Suggest repeat in one year with thyroid autoantibodies (TPO antibodies). |
      | 1.4.19 | The suppressed TSH and high-normal FT4 may suggest hyperthyroidism. FT3 and TRAb may be useful. However, low TSH may be seen in pregnancy which should be excluded. These results are within reference intervals for first trimester. If pregnant, repeat TFTs in 6 weeks. |
      | 1.4.20 | Clinical conditions associated with a suppressed TSH include non-toxic goitre, subclinical hyperthyroidism and glucocorticoid therapy. Suggest repeat TFTs in six weeks’ time. Other causes of this pattern include: Excessive T4 therapy for hypothyroidism, treated primary hyperthyroidism. Acute psychiatric illness may raise FT4 and/or lower TSH. |
      | 1.4.21 | The increased FT3 and suppressed TSH are consistent with T3 toxicosis. Suggest measure TRAb. |


    And stop the client application
