Feature: When entering an expression to be used as a condition, the user is provided with a syntactically correct condition tip

  Scenario: Should provide a condition tip when the user types an expression
    Given case Bondi is provided with the following values, reference ranges and units:
      | Attribute | Value | Low | High | Units  |
      | Waves     | 2.1   | 0.5 | 2    | metres |
      | UV        | 4.5   |     | 2.6  |        |
    And I start the client application
    And I start to build a rule to add the comment "Beach time!"
    And I enter the expression "elevated waves"
    Then the suggested conditions should contain:
      | Waves is high |
    And stop the client application

