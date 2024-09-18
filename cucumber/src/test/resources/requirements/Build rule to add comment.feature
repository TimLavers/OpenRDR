Feature: The user can make a rule that adds a comment to the interpretive report

  Scenario: The user should be able to build a rule to add a new comment
    Given a list of cases with the following names is stored on the server:
      | Case1 |
      | Case2 |
    And I start the client application
    And I should see the case Case1 as the current case
    When I build a rule to add the comment "Go to Bondi."
    Then the interpretation should be "Go to Bondi."
    And select the case Case2
    And the interpretation should be "Go to Bondi."
    And stop the client application

  Scenario: The user should be able to build a rule to add an existing comment
    Given the configured case Case1 is stored on the server
    And the following comments have been defined in the project:
      | Go to Bondi.   |
      | Go to Malabar. |
      | Go to Coogee.  |
    And I start the client application
    And I should see the case Case1 as the current case
    And I build a rule to add the existing comment "Go to Malabar."
    And the interpretation should be "Go to Malabar."

  Scenario: The user should be able to build a rule to add a comment with a condition they have selected
    Given I start the client application
    And case Bondi is provided having data:
      | Sun  | hot       |
      | Wave | excellent |
    And I build a rule to add the comment "Go to the beach." with the condition "Sun is in case"
    Then  the interpretation should be "Go to the beach."
    And the condition showing for the comment "Go to the beach." is:
      | Sun is in case |
    And stop the client application

  Scenario: The user should be able to build rules to add several comments
    Given a list of cases with the following names is stored on the server:
      | Case1 |
      | Case2 |
    And I start the client application
    And I should see the case Case1 as the current case
    When I build a rule to add the comment "Go to Bondi."
    And I build a rule to add the comment "Grow some trees."
    Then  the interpretation should be "Go to Bondi. Grow some trees."
    And select the case Case2
    And  the interpretation should be "Go to Bondi. Grow some trees."
    And stop the client application

