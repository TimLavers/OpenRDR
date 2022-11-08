Feature: Case View

  @single
  Scenario: Attribute order can be set by the user
    Given a KB for which the initial attribute order is A, B, C:
    And I start the client application
    And I select a case with all three attributes
    And I move C below A
#    And pause
#    And I move A below B
#    Then the case should show the attributes in order:
#      | C |
#      | B |
#      | A |
#    And if I select another case having these three attributes
#    Then the case should show the attributes in order:
#      | GHI |
#      | DEF |
#      | ABC |
    And stop the client application
