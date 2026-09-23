Feature: Store interesting cases in user-defined case lists

  Background:
    Given a default KB is opened

  Scenario: A case can be copied to a favourites case list
    Given a list of cases with the following names is stored on the server:
      | Case1 |
      | Case2 |
      | Case3 |
    And I start the client application
    And I see the case Case1 as the current case
    And I select case Case3
    And I see the case Case3 as the current case
    And I copy the current case to the "favourites" case list
    Then the "favourites" case list should contain:
      | Case3 |
    And the processed case list should contain:
      | Case1 |
      | Case2 |
      | Case3 |

  Scenario: The original case remains selected when a case is copied to.
    Given a list of cases with the following names is stored on the server:
      | Case1 |
      | Case2 |
      | Case3 |
    And I start the client application
    And I see the case Case1 as the current case
    And I select case Case3
    And I copy the current case to the "favourites" case list
    Then I see the case Case3 as the current case
    When I press the up arrow key
    And I see the case Case2 as the current case

  Scenario: A case can be copied and given a new name
    Given a list of cases with the following names is stored on the server:
      | Case1 |
    And I start the client application
    And I see the case Case1 as the current case
    And I copy the current case to the "favourites" case list with name "An amazing case"
    Then the "favourites" case list should contain:
      | An amazing case |
    And the processed case list should contain:
      | Case1 |

  Scenario: A copied case has the same data as the original case
    Given the configured case Case4 is stored on the server
    And I start the client application
    And I select case Case4
    And I copy the current case to the "favourites" case list
    Then the favourites case list should contain:
      | Case4 |
    And I select case Case4 on the "favourites" case list
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

  Scenario: A case can be deleted from a user-defined case list
    Given a list of cases with the following names is stored on the server:
      | Case1 |
    And I start the client application
    And I see the case Case1 as the current case
    And I copy the current case to the "favourites" case list with name "CopiedCase"
    Then the "favourites" case list should contain:
      | CopiedCase |
    And I select case CopiedCase on the "favourites" case list
    And I delete the current case from the "favourites" case list
    Then I should no longer see the "favourites" case list
    And I see the case Case1 as the current case

  Scenario: Copied cases are listed in the order in which they were added.
    Given a list of cases with the following names is stored on the server:
      | Case1 |
      | Case2 |
      | Case3 |
    And I start the client application
    And I see the case Case1 as the current case
    And I select case Case3
    And I copy the current case to the "favourites" case list
    Then the "favourites" case list should contain:
      | Case3 |
    And I select case Case1
    And I copy the current case to the "favourites" case list
    Then the favourites case list should contain:
      | Case3 |
      | Case1 |
    And I select case Case2
    And I copy the current case to the "favourites" case list
    Then the "favourites" case list should contain:
      | Case3 |
      | Case1 |
      | Case2 |

  Scenario: Copied cases can themselves be copied to the list they are on.
    Given a list of cases with the following names is stored on the server:
      | Case1 |
      | Case2 |
    And I start the client application
    And I see the case Case1 as the current case
    And I select case Case2
    And I copy the current case to the "favourites" case list with name "CopiedCase"
    Then the "favourites" case list should contain:
      | CopiedCase |
    And I select case CopiedCase
    And I copy the current case to the "favourites" case list with name "CopiedCopiedCase"
    Then the "favourites" case list should contain:
      | CopiedCase |
      | CopiedCopiedCase |

  Scenario: Cornerstone cases can be copied.
    Given a list of cases with the following names is stored on the server:
      | Case1 |
    And a list of cornerstone cases with the following names is stored on the server:
      | CCase1 |
      | CCase2 |
    And I start the client application
    And I see the case Case1 as the current case
    And I select the case CCase1 on the cornerstone case list
    And I copy the current case to the "favourites" case list with name "CopiedCase"
    Then the "favourites" case list should contain:
      | CopiedCase |

#  Copy same case to multiple lists
  Scenario: The user can define more than one case list.
    Given a list of cases with the following names is stored on the server:
      | Case1 |
      | Case2 |
      | Case3 |
    And I start the client application
    And I see the case Case1 as the current case
    And I copy the current case to the "good" case list
    Then the "good" case list should contain:
      | Case1 |
    And I select case Case2
    And I copy the current case to the "bad" case list
    Then the "bad" case list should contain:
      | Case2 |
    And I select case Case3
    And I copy the current case to the "ugly" case list
    Then the "ugly" case list should contain:
      | Case3 |

  Scenario: The user can copy the same case to more than one list.
    Given a list of cases with the following names is stored on the server:
      | Case1 |
      | Case2 |
    And I start the client application
    And I see the case Case1 as the current case
    And I copy the current case to the "good" case list
    Then the "good" case list should contain:
      | Case1 |
    And I copy the current case to the "bad" case list
    Then the "bad" case list should contain:
      | Case1 |
    And I copy the current case to the "ugly" case list
    Then the "ugly" case list should contain:
      | Case1 |

  Scenario: Cases cannot be copied to the cornerstone cases list.
    Given a list of cases with the following names is stored on the server:
      | Case1 |
    And a list of cornerstone cases with the following names is stored on the server:
      | CCase1 |
    And I start the client application
    And I see the case Case1 as the current case
    And I copy the current case to the "Cornerstone Cases" case list with name "CopiedCase"
    Then the chatbot response contains the following terms:
      | Cannot | Cornerstone |
    And the cornerstone case count should be 1

  Scenario: Copied cases can themselves be copied to the list they are on.
    Given a list of cases with the following names is stored on the server:
      | Case1 |
      | Case2 |
    And I start the client application
    And I see the case Case1 as the current case
    And I select case Case2
    And I copy the current case to the "favourites" case list with name "CopiedCase"
    Then the "favourites" case list should contain:
      | CopiedCase |
    And I select case CopiedCase
    And I copy the current case to the "favourites" case list with name "CopiedCopiedCase"
    Then the "favourites" case list should contain:
      | CopiedCase |
      | CopiedCopiedCase |
