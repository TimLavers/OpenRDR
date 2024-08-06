Feature: The TSH KB can be built with the user interface.

  @single
  Scenario: Build the TSH KB from cases
    Given I start the client application
    And I create a Knowledge Base with the name TSH based on the "Thyroid Stimulating Hormone - cases only" sample
    And pause for 2 seconds
    Then the count of the number of cases is 34

    And I select case 1.4.1
    And I build a rule to add the comment "Normal TSH is consistent with a euthyroid state." with conditions
      | TSH is normal |
    And pause

    And I select case 1.4.2
    And I build a rule to replace the comment "Normal TSH is consistent with a euthyroid state." with the comment "Normal T4 and TSH are consistent with a euthyroid state." with conditions
      | FT4 is normal |


    And stop the client application
