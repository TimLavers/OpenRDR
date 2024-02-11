Feature: Show a list of cases
  @single
  Scenario: Should show the list of cases that have been stored by the server
    Given a list of cases with the following names is stored on the server:
      | Case1 |
      | Case2 |
    And I start the client application
    And pause
    Then I should see the following cases in the case list:
      | Case1 |
      | Case2 |
    And stop the client application

  Scenario: Should be able to select the last in a list of cases
    Given a list of 10 cases is stored on the server
    And I start the client application
    And the count of the number of cases is 10
    When I select case Case_010
    Then I should see the case Case_010 as the current case
    And stop the client application

  Scenario: The list of cases should be updated when a new case is stored by the server
    Given a list of cases with the following names is stored on the server:
      | Case1 |
      | Case2 |
    And I start the client application
    And I see the following cases in the case list:
      | Case1 |
      | Case2 |
    When a new case with the name Case3 is stored on the server
    Then I should see the following cases in the case list:
      | Case1 |
      | Case2 |
      | Case3 |
    And stop the client application

  @ignore
  Scenario: The list of cases should be updated when a case is deleted on the server
    Given a list of cases with the following names is stored on the server:
      | Case1 |
      | Case2 |
    And I start the client application
    And I see the following cases in the case list:
      | Case1 |
      | Case2 |
    When the case with the name Case1 is deleted on the server
    And pause for 2 seconds
    Then I should see the following cases in the case list:
      | Case2 |
    And I should see the case Case2 as the current case
    And stop the client application

  @ignore
  Scenario: The list of cases should not be visible when all cases are deleted on the server
    Given a list of cases with the following names is stored on the server:
      | Case1 |
      | Case2 |
    And I start the client application

    And I see the following cases in the case list:
      | Case1 |
      | Case2 |
    When the following cases are deleted on the server:
      | Case1 |
      | Case2 |
    Then I should see no cases in the case list
    And stop the client application

  @ignore
  Scenario: The current case should not be visible when all cases are deleted on the server
    Given a list of cases with the following names is stored on the server:
      | Case1 |
      | Case2 |
    And I start the client application
    And I see the following cases in the case list:
      | Case1 |
      | Case2 |
    When the following cases are deleted on the server:
      | Case1 |
      | Case2 |
    And pause for 2 seconds
    Then I should not see any current case
    And stop the client application

  @ignore
  Scenario: Should select the first case on the list by default
    Given a list of cases with the following names is stored on the server:
      | Case1 |
      | Case2 |
    When I start the client application
    And I see the following cases in the case list:
      | Case1 |
      | Case2 |
    Then I should see the case Case1 as the current case
    And stop the client application
