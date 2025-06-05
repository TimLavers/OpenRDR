Feature: The user can undo rules

  Scenario: When a rule is undone, the interpretation of a case changes back to what is was prior to the rule being built
    Given case Bondi is provided having data:
      | Wave | excellent |
      | Sun  | hot       |
      | Tide | low       |
    And I start the client application
    And I see the case Bondi as the current case
    And I build a rule to add the comment "Go to Bondi." with the condition "Sun is hot"
    Then the interpretation should be "Go to Bondi."
    And I undo the last rule
    Then the interpretation should be empty
    And stop the client application

  Scenario: Initially, there are no rules to undo
    Given I start the client application
    Then the undo last rule dialog shows that no rule is available for undoing
    And stop the client application

  Scenario: Undo rules from sample KB
    Given I start the client application

    And I create a Knowledge Base with the name ContactLenseUndo based on the "Contact Lense Prescription" sample
    Then the count of the number of cases is 24

    When I select case Case24
    Then the interpretation should be empty

    And I undo the last rule
    Then the interpretation should be "hard"

    And stop the client application
