Feature: Navigate cases via up and down arrow keys

  @single
  Scenario: The user should be able to navigate to the next case via the down arrow key
    Given a list of cases with the following names is stored on the server:
      | Case1 |
      | Case2 |
    And I start the client application
    And I see the case Case1 as the current case
    And I select case Case1 on the processed case list
    When I press the down arrow key
    Then I see the case Case2 as the current case

  Scenario: The user should be able to navigate to the previous case via the up arrow key
    Given a list of cases with the following names is stored on the server:
      | Case1 |
      | Case2 |
    And I start the client application
    And I see the case Case1 as the current case
    And I select case Case2 on the processed case list
    When I press the up arrow key
    And I see the case Case1 as the current case

  Scenario: The user should be able to navigate to the next cornerstone case via the down arrow key
    Given a list of cases with the following names is stored on the server:
      | Case1 |
      | Case2 |
    And a list of cornerstone cases with the following names is stored on the server:
      | CCase1 |
      | CCase2 |
    And I start the client application
    And I see the case Case1 as the current case
    And I select the case CCase1 on the cornerstone case list
    When I press the down arrow key
    And I see the case CCase2 as the current case

  Scenario: The user should be able to navigate to the previous cornerstone case via the up arrow key
    Given a list of cases with the following names is stored on the server:
      | Case1 |
      | Case2 |
    And a list of cornerstone cases with the following names is stored on the server:
      | CCase1 |
      | CCase2 |
    And I start the client application
    And I see the case Case1 as the current case
    And I select the case CCase2 on the cornerstone case list
    When I press the up arrow key
    And I see the case CCase1 as the current case




