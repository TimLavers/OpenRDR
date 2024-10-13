Feature: When entering an expression to be used as a condition, the user is provided with a syntactically correct condition tip

  @single
  Scenario: Should provide a condition tip when the user types an expression
    Given case Bondi is provided having data:
      | Sun   | hot |
      | Waves | 1.5 |
    And I start the client application
    And I start to build a rule to add the comment "Beach time!"
    And I enter the expression "waves are large"
    And pause
    Then the suggested conditions should contain:
      | Waves are high |
    And stop the client application

