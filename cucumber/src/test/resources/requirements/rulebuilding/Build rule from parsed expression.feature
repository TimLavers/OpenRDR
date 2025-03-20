Feature: When entering an expression to be used in a rule, the user is provided with a syntactically correct condition

  Scenario: Should provide a condition tip when the user types an expression
    Given case Bondi is provided with the following values, reference ranges and units:
      | Attribute | Value | Low | High | Units  |
      | Waves     | 2.1   | 0.5 | 2    | metres |
      | UV        | 4.5   |     | 2.6  |        |
    And I start the client application
    And I start to build a rule to add the comment "Beach time!"
    And I enter the expression "elevated waves"
    Then the available condition and its tool tip should be:
      | elevated waves | Waves is high |
    And stop the client application

  Scenario: Should provide a warning when the user types an unknown expression
    Given case Bondi is provided with the following values, reference ranges and units:
      | Attribute | Value | Low | High | Units  |
      | Waves     | 2.1   | 0.5 | 2    | metres |
      | UV        | 4.5   |     | 2.6  |        |
    And I start the client application
    And I start to build a rule to add the comment "Beach time!"
    And I enter the expression "below"
    Then an alert should be displayed with the message:
    """
    This condition is not able to be recognised. Please try again.
    """
    And stop the client application

  Scenario: Should provide a warning when the user types a valid condition but which is not true for the case
    Given case Bondi is provided with the following values, reference ranges and units:
      | Attribute | Value | Low | High | Units  |
      | Waves     | 2.1   | 0.5 | 2    | metres |
      | UV        | 4.5   |     | 2.6  |        |
    And I start the client application
    And I start to build a rule to add the comment "Beach time!"
    And I enter the expression "UV is 5.6"
    Then an alert should be displayed with the message:
    """
    This condition is not true for this case. Please try again.
    """
    And stop the client application

