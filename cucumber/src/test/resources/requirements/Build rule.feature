Feature: The user can make rules that change the interpretive report

  Scenario: When the user starts to build a rule, condition hints should be shown
    Given case Bondi is provided having data:
      | Wave | excellent |
      | Sun  | hot       |
    And I start the client application
    And I see the case Bondi as the current case
    When I start to build a rule to add the comment "Let's surf"
    Then the conditions showing should include:
      | Sun is "hot"        |
      | Sun is in case      |
      | Wave is "excellent" |
      | Wave is in case     |
    And stop the client application

  Scenario: A new rule should apply to any case satisfying its conditions
    Given I start the client application
    And case Bondi is provided having data:
      | Sun  | hot       |
      | Wave | excellent |
    And case Manly is provided having data:
      | Sun      | hot      |
      | Swimming | pleasant |
    And case Malabar is provided having data:
      | Swimming | pleasant |
    And I select case Bondi
    And I build a rule to add the comment "Go for a surf." with the condition "Sun is in case"
    When I select case Manly
    Then the interpretation should contain the text "Go for a surf."
    And I select case Malabar
    And the interpretation should be empty
    And stop the client application

  Scenario: The user should be able to cancel the current rule being built
    Given case Bondi is provided having data:
      | Sun  | hot       |
      | Wave | excellent |
    And case Manly is provided having data:
      | Sun      | hot      |
      | Swimming | pleasant |
    Given I start the client application
    And I should see the case Bondi as the current case
    And I start to build a rule to add the comment "Let's surf"
    And the conditions showing should include:
      | Sun is "hot"            |
      | Sun is in case          |
      | Swimming is not in case |
      | Wave is "excellent"     |
      | Wave is in case         |
    And I select the first condition
    When I cancel the rule
    And I select case Bondi
    And the interpretation should be empty
    And I select case Manly
    And the interpretation should be empty
    And stop the client application

  Scenario: The user should be able to add a rule after cancelling the previous rule building session
    Given case Bondi is provided having data:
      | Sun  | hot       |
      | Wave | excellent |
    And case Manly is provided having data:
      | Sun      | hot      |
      | Swimming | pleasant |
    Given I start the client application
    And I should see the case Bondi as the current case
    And I start to build a rule to add the comment "Let's surf"
    And the conditions showing should include:
      | Sun is "hot"            |
      | Sun is in case          |
      | Swimming is not in case |
      | Wave is "excellent"     |
      | Wave is in case         |
    And I select the first condition
    When I cancel the rule
    And the interpretation should be empty
    When I build a rule to add the comment "Go for a surf." with the condition "Sun is in case"
    Then the interpretation should be "Go for a surf."
    And stop the client application

  Scenario: The KB controls and case list should be disabled when building a rule
    Given a new case is stored on the server
    And I start the client application
    And the KB controls are shown
    When I start to build a rule to add the comment "Let's surf"
    Then the KB controls should be hidden
    And the case list should be hidden
    And cancel the rule
    And stop the client application

  Scenario: The KB controls and case list should be re-enabled after cancelling a rule
    Given a new case is stored on the server
    And I start the client application
    And I start to build a rule to add the comment "Let's surf"
    And the KB controls are hidden
    And the case list is hidden
    When I cancel the rule
    Then the KB controls should be shown
    And the case list should be shown
    And stop the client application

  Scenario: When the user starts to build a rule to add a comment, the rule action should be shown
    Given case Bondi is provided having data:
      | Wave | excellent |
      | Sun  | hot       |
    And I start the client application
    And I see the case Bondi as the current case
    When I start to build a rule to add the comment "Let's surf."
    Then the message indicating the comment "Let's surf." is being added should be shown
    And stop the client application

  Scenario: When the user starts to build a rule to remove a comment, the rule action should be shown
    Given case Bondi is provided having data:
      | Wave | excellent |
      | Sun  | hot       |
    And the interpretation of the case Bondi includes "Let's surf." because of condition "Wave is in case"
    And I start the client application
    And I see the case Bondi as the current case
    When I start to build a rule to remove the comment "Let's surf."
    Then the message indicating the comment "Let's surf." is being removed should be shown
    And stop the client application

  Scenario: When the user starts to build a rule to replace a comment, the rule action should be shown
    Given case Bondi is provided having data:
      | Wave | excellent |
      | Sun  | hot       |
    And the interpretation of the case Bondi includes "Let's surf." because of condition "Wave is in case"
    And I start the client application
    And I see the case Bondi as the current case
    When I start to build a rule to replace the comment "Let's surf." by "Let's swim."
    Then the message indicating the comment "Let's surf." is being replaced by "Let's swim." should be shown
    And stop the client application