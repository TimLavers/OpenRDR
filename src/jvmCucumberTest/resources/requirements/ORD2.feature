Feature: Case View

  @single
  Scenario: Attribute order can be set by the user
    Given I start the client application
    And case CaseA is provided having data:
      |A|a|
    And case CaseAB is provided having data:
      |A|a|
      |B|b|
    And case CaseABC is provided having data:
      |A|a|
      |B|b|
      |C|c|
    And if I select case CaseABC
    Then the case should show the attributes in order:
      | A |
      | B |
      | C |
    And I move attribute C below attribute A
    And I move attribute A below attribute B
    Then the case should show the attributes in order:
      | C |
      | B |
      | A |
    And case CaseABCD is provided having data:
      |A|a|
      |B|b|
      |C|c|
      |D|d|
    And if I select case CaseABCD
    Then the case should show the attributes in order:
      | C |
      | B |
      | A |
      | D |
    And pause briefly
    And stop the client application
