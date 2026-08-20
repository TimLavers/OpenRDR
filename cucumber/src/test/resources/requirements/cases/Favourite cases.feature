Feature: Show a list of favourite cases

  Scenario: The favourite cases list is initially empty
    Given I start the client application
    Then I should see no cases in the favourites case list

  Scenario: A case can be copied to the favourites case list
    Given a list of cases with the following names is stored on the server:
      | Case1 |
      | Case2 |
      | Case3 |
    And I start the client application
    And I see the case Case1 as the current case
    And I select case Case3
    And I see the case Case3 as the current case
    And I copy the current case to the favourites case list
    Then the favourites case list should contain:
      | Case3 |
    And the processed case list should contain:
      | Case1 |
      | Case2 |
      | Case3 |

  Scenario: The original case remains selected when a case is copied to the favourites case list.
    Given a list of cases with the following names is stored on the server:
      | Case1 |
      | Case2 |
      | Case3 |
    And I start the client application
    And I see the case Case1 as the current case
    And I select case Case3
    And I copy the current case to the favourites case list
    Then I see the case Case3 as the current case
    When I press the up arrow key
    And I see the case Case2 as the current case

  Scenario: A case can be copied to the favourites case list and given a new name
    Given a list of cases with the following names is stored on the server:
      | Case1 |
    And I start the client application
    And I see the case Case1 as the current case
    And I copy the current case to the favourites case list with name "An amazing case"
    Then the favourites case list should contain:
      | An amazing case |
    And the processed case list should contain:
      | Case1 |

  Scenario: A case copied to the favourites case list has the same data as the original case
    Given the configured case Case4 is stored on the server
    And I start the client application
    And I select case Case4
    And I copy the current case to the favourites case list
    Then the favourites case list should contain:
      | Case4 |
    And I select case Case4 on the favourites case list
    Then I should see these episode dates:
      | 2022-08-05 12:31 |
      | 2022-08-06 02:25 |
    And I should see these attributes:
      | TSH   |
      | Stuff |
    And I should see these values for 'TSH':
      | 0.67 |
      | 2.75 |
    And I should see '0.50 - 4.0' as reference range for 'TSH'
    And I should see these values for 'Stuff':
      | 12.4 |
      | 6.7  |
    And I should see '' as reference range for 'Stuff'

  Scenario: A case can be deleted from the favourites case list
    Given a list of cases with the following names is stored on the server:
      | Case1 |
    And I start the client application
    And I see the case Case1 as the current case
    And I copy the current case to the favourites case list with name "CopiedCase"
    Then the favourites case list should contain:
      | CopiedCase |
    And I select case CopiedCase on the favourites case list
    And I delete the current case from the favourites case list
    Then I should see no cases in the favourites case list
    And I see the case Case1 as the current case

  Scenario: Favourite cases are listed in the order in which they were added.
    Given a list of cases with the following names is stored on the server:
      | Case1 |
      | Case2 |
      | Case3 |
    And I start the client application
    And I see the case Case1 as the current case
    And I select case Case3
    And I copy the current case to the favourites case list
    Then the favourites case list should contain:
      | Case3 |
    And I select case Case1
    And I copy the current case to the favourites case list
    Then the favourites case list should contain:
      | Case3 |
      | Case1 |
    And I select case Case2
    And I copy the current case to the favourites case list
    Then the favourites case list should contain:
      | Case3 |
      | Case1 |
      | Case2 |

