Feature: Case View

  @single
  Scenario: Attribute order can be set by the user
    Given a KB for which the initial attribute order is A, B, C:
    And I start the client application
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
    And if I select case CaseABC2
    Then the case should show the attributes in order:
      | C |
      | B |
      | A |
    And stop the client application
