Feature: When building a rule, the user is provided with candidate conditions that
  can either be directly added or modified and then added.

  @single
  Scenario: If a suggested condition is not editable, then clicking it
  adds it to the list of conditions for the rule.
    Given case Bondi is provided having data:
      | Sun   | hot |
      | Waves | 1.5 |
    And I start the client application
    And I start to build a rule to add the comment "Beach time!"
    And I click the suggested condition "Sun is \"hot\""
    Then the selected conditions should be:
      | Sun is "hot" |
    And pause
    And stop the client application

  Scenario: When a suggested condition is selected, it is removed from the list of suggestions.
    Given case Bondi is provided having data:
      | Sun | hot |
    And I start the client application
    And I start to build a rule to add the comment "Beach time!"
    And I click the suggested condition "Sun is \"hot\""
    Then the suggested conditions should not contain:
      | Sun is "hot" |
    And stop the client application

  Scenario: After removing a condition, it is reinstated in the list of suggestions.
    Given case Bondi is provided having data:
      | Sun | hot |
    And I start the client application
    And I start to build a rule to add the comment "Beach time!"
    And I click the suggested condition "Sun is \"hot\""
    And I remove the condition "Sun is \"hot\""
    Then the suggested conditions should contain:
      | Sun is "hot" |
    And stop the client application

  Scenario: Some suggested conditions can be modified before being added
    Given case Bondi is provided having data:
      | Sun   | hot |
      | Waves | 1.5 |
    And I start the client application
    And I start to build a rule to add the comment "Beach time!"
    And I click the suggested condition "Waves ≥ 1.5"
    And I set the editable value to be "1.2" and click ok
    Then the selected conditions should be:
      | Waves ≥ 1.2 |
    And stop the client application

  Scenario: When an editable condition is used, it is removed from the list of suggestions.
    Given case Bondi is provided having data:
      | Sun   | hot |
      | Waves | 1.5 |
    And I start the client application
    And I start to build a rule to add the comment "Beach time!"
    And I click the suggested condition "Waves ≥ 1.5"
    And pause for 1 seconds
    And I set the editable value to be "1.2" and click ok
    Then the suggested conditions should not contain:
      | Waves ≥ 1.5 |
    And the selected conditions should be:
      | Waves ≥ 1.2 |
    And stop the client application
