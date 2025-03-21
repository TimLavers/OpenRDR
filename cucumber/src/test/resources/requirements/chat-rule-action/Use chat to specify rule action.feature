Feature: The user can make rules that change the interpretive report

  @single
  Scenario: Using chat, the user shall be able to specify that a comment should be added to the report
    Given case Bondi is provided having data:
      | Wave | excellent |
      | Sun  | hot       |
    And I start the client application
    And I see the case Bondi as the current case
    And pause
    And the chat asks me what change should be made to the report
    When I tell the chat to add a comment "Let's surf"
    Then the comment "Let's surf" shows as the rule action
    And the the building controls are showing
    And stop the client application
