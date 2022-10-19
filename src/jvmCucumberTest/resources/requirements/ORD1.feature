Feature: Show a list of cases

  @single
  Scenario: Should show the list of cases that have been stored by the server
    Given a list of cases with the following names is stored on the server:
      | case1 |
      | case2 |
    And I start the client application
    Then I should see the following cases in the case list:
      | case1 |
      | case2 |
    And stop the client application
