Feature: The TSH KB can be built with the user interface.

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

#        ""
    Then the cases should have interpretations as follows
      | 1.4.1 | Normal T4 and TSH are consistent with a euthyroid state. |
      | 1.4.2 | Normal TSH is consistent with a euthyroid state. |
      | 1.4.3 | A mildly reduced FT4 with a normal TSH may be due to non-thyroidal illness or pituitary hypothyroidism. |
      | 1.4.4 | Mildly increased TSH may be found in patients with subclinical hypothyroidism or sick euthyroid syndrome. Suggest measurement of FT4, TSH and thyroperoxidase (TPO) antibodies in 6 weeks. |
      | 1.4.5 | Mildly increased TSH with a normal FT4 may be found in patients with subclinical hypothyroidism or sick euthyroid syndrome. Suggest repeat measurement with TPO antibodies in 6 weeks. |
      | 1.4.6 | Mildly increased TSH with a normal FT4 can be seen in the euthyroid elderly. |
      | 1.4.7 | A moderately increased TSH with a normal FT4 is consistent with (mild) primary hypothyroidism. |
      | 1.4.8 | TSH reference intervals in pregnancy: 1st trimester 0.02–2.5, 2nd and 3rd trimester 0.30–3.0 |
      | 1.4.9 | The mildly increased TSH and raised TPO antibodies indicate subclinical hypothyroidism due to autoimmune thyroid disease. Suggest confirming subclinical hypothyroidism by repeat testing. Poor pregnancy outcomes have been described in women with a raised TSH. If raised TSH confirmed, consider thyroxine replacement. |
#      | 1.4.9 |  |


    And stop the client application
