Feature: The TSH KB can be built with the user interface.

  @single
  Scenario: Build the TSH KB from cases
    Given I start the client application
    And I create a Knowledge Base with the name TSH based on the "Thyroid Stimulating Hormone - cases only" sample
    Then the count of the number of cases is 34
#        addCommentForCase("1.4.4", report3, tshHigh )
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
    And pause
    And I build a rule to replace the comment "Normal TSH is consistent with a euthyroid state." with the comment "A mildly reduced FT4 with a normal TSH may be due to non-thyroidal illness or pituitary hypothyroidism." with conditions
      | Free T4 is normal |

    And pause


    And stop the client application
