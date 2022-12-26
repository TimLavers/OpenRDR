Feature: Order of Attributes in Case View

  @single
  Scenario: Default Attribute order is by order created
    Given I start the client application
    And case CaseA is provided having data:
      |A|a|
    And case CaseAB is provided having data:
      |B|b|
      |A|a|
    And case CaseABC is provided having data:
      |C|c|
      |B|b|
      |A|a|
    And pause
    And if I select case CaseA
    Then the case should show the attributes in order:
      | A |
    And if I select case CaseAB
    Then the case should show the attributes in order:
      | A |
      | B |
    And if I select case CaseABC
    Then the case should show the attributes in order:
      | A |
      | B |
      | C |
    And stop the client application

  Scenario: Attributes can be re-ordered by drag-and-drop
    Given I start the application and the initial Attribute order is A, B, C
    And case CaseABC is provided having data:
        |A|a|
        |B|b|
        |C|c|
    And if I select case CaseABC
    And I move attribute C below attribute A
    And I move attribute A below attribute B
    Then the case should show the attributes in order:
      | C |
      | B |
      | A |
    And stop the client application

  Scenario: New Attributes can be created after an Attribute re-ordering
    Given I start the application and the initial Attribute order is A, B, C
    And case CaseABC is provided having data:
        |A|a|
        |B|b|
        |C|c|
    And if I select case CaseABC
    And I move attribute C below attribute A
    And I move attribute A below attribute B
    And case CaseABCD is provided having data:
      |A|a|
      |B|b|
      |C|c|
      |D|d|
    When I select case CaseABCD
    Then the case should show the attributes in order:
      | C |
      | B |
      | A |
      | D |
    And stop the client application
