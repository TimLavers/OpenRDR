Feature: The user can make a rule that removes a comment from the interpretive report
  Scenario: The user should be able to build a rule to remove a comment
    Given a list of cases with the following names is stored on the server:
      | Case1 |
      | Case2 |
    And the interpretation of the case Case1 is "Go to Bondi."
    And I start the client application
    And I should see the case Case1 as the current case
    And  the interpretation should contain the text "Go to Bondi."
    When I build a rule to remove the comment "Go to Bondi."
    Then  the interpretation should be empty
    And select the case Case2
    And  the interpretation should be empty
    And stop the client application

  Scenario: The user should be able to build a rule to remove a comment with a condition they have selected
    Given case Bondi is provided having data:
      | Sun  | too hot |
      | Wave | poor    |
    And case Manly is provided having data:
      | Swimming | pleasant |
    And the interpretation of the case Bondi is "Go to the beach."
    And I start the client application
    And I select case Bondi
    When I build a rule to remove the comment "Go to the beach." with the condition "Sun is in case"
    And  the interpretation should be empty
    And select the case Manly
    And  the interpretation should contain the text "Go to the beach."
    And stop the client application

  Scenario: A comment given for the case must be selected before the user can start a rule to remove it
    Given a case with name Case1 is stored on the server
    And the interpretation of the case Case1 is "Go to Bondi."
    And I start the client application
    And I should see the case Case1 as the current case
    And I start to build a rule to remove a comment
    When I enter "Maroubra" as the filter to select a comment to remove
    Then the OK button to start the rule to remove the comment should be disabled
    And stop the client application