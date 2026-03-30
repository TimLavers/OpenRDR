Feature: When entering an expression to be used in a rule, the user is provided with a syntactically correct condition
  Scenario: Should provide a condition tip when the user types an expression
    Given case Bondi is provided with the following values, reference ranges and units:
      | Attribute | Value | Low | High | Units  |
      | Waves     | 2.1   | 0.5 | 2    | metres |
      | UV        | 4.5   |     | 2.6  |        |
    And I start the client application
    And I request that the comment "Beach time!" be added
    When I enter the expression "elevated waves"
    Then the model should respond with a message containing:
      """
      Waves is high
      """

  Scenario: Should provide a warning when the user types an unknown expression
    Given case Bondi is provided with the following values, reference ranges and units:
      | Attribute | Value | Low | High | Units  |
      | Waves     | 2.1   | 0.5 | 2    | metres |
      | UV        | 4.5   |     | 2.6  |        |
    And I start the client application
    And I request that the comment "Beach time!" be added
    And the suggested conditions should contain:
      | Waves is high |
    When I enter the expression "below"
    Then the model should indicate that the expression is not a valid reason

  Scenario: Should provide a warning when the user types a valid condition but which is not true for the case
    Given case Bondi is provided with the following values, reference ranges and units:
      | Attribute | Value | Low | High | Units  |
      | Waves     | 2.1   | 0.5 | 2    | metres |
      | UV        | 4.5   |     | 2.6  |        |
    And I start the client application
    And I request that the comment "Beach time1" be added
    When I enter the expression "UV is 5.6"
    Then the model should respond with a message containing:
    """
    This condition is not true for this case. Please try again.
    """

