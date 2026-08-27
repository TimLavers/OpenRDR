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

  Scenario: The user should be able to build a rule to add a comment that already exists in the project
    # The first rule is conditioned so that the comment exists in the project but is not
    # given to Manly, so that the second rule genuinely adds an already-existing comment.
    Given case Bondi is provided having data:
      | Sun | hot |
    And case Manly is provided having data:
      | Sun | cold |
    And I start the client application
    And I select case Bondi
    And I build a rule to add the comment "Go to Malabar." with condition
      | Sun is hot |
    And the interpretation should be "Go to Malabar."
    And I select case Manly
    And the interpretation should be empty
    When I build a rule to add the existing comment "Go to Malabar."
    Then the interpretation should be "Go to Malabar."

  Scenario: The user should be able to build a rule to add a comment with a condition they have selected
    Given I start the client application
    And case Bondi is provided having data:
      | Sun  | hot       |
      | Wave | excellent |
    And I build a rule to add the comment "Go to the beach." with condition
      | Sun is in case |
    Then  the interpretation should be "Go to the beach."
    And the condition showing for the comment "Go to the beach." is:
      | Sun is in case |

  Scenario: The user should be able to build rules to add several comments
    Given a list of cases with the following names is stored on the server:
      | Case1 |
      | Case2 |
    And I start the client application
    And I should see the case Case1 as the current case
    When I build a rule to add the comment "Go to Bondi."
    And I add a further comment "Grow some trees." and complete the rule
    Then  the interpretation should be "Go to Bondi. Grow some trees."
    And select the case Case2
    And  the interpretation should be "Go to Bondi. Grow some trees."