Feature: The user can undo rules

  @single
  Scenario: When a rule is undone, the interpretation of a case changes back to what is was prior to the rule being built
    Given case Bondi is provided having data:
      | Wave | excellent |
      | Sun  | hot       |
      | Tide  | low       |
    And I start the client application
    And I see the case Bondi as the current case
    And I build a rule to add the comment "Go to Bondi." with the condition "Sun is hot"
    Then the interpretation should be "Go to Bondi."
    And pause for 90 seconds
    And I undo the last rule
    Then the interpretation should be empty
    And stop the client application
