# Specifies the "search by conditions" facility:
# documentation/requirements/search_cases_by_condition.md
#
# The facility is not implemented yet, so every scenario here is expected to
# fail until the server, chat instructions and UI support searching. Where the
# requirements make no guarantee (the order of cases added to the destination
# list; which cases are found first when a limit stops the search early) the
# assertions avoid pinning it.

Feature: Search for cases by conditions

  Background:
    Given a default KB is opened

  Scenario: Matching cases are copied to the destination list, and the originals stay where they were.
    Given case Case1 is provided having data:
      | TSH | 2.1 |
    And case Case2 is provided having data:
      | TSH | 6.7 |
    And case Case3 is provided having data:
      | TSH | 8.9 |
    And I start the client application
    And I see the case Case1 as the current case
    When I search the "processed" case list for all cases where "TSH is greater than 4" and put the results in the highTSH case list
    And I confirm the search
    Then the user-defined highTSH case list should contain the following cases in any order:
      | Case2 |
      | Case3 |
    And the processed case list should contain:
      | Case1 |
      | Case2 |
      | Case3 |
    And the chatbot response contains the following terms:
      | found | 2 | processed | highTSH |
      | TSH is greater than 4 |

  Scenario: The user is prompted for the search parameters, and conditions can be and-ed.
    Given case Case1 is provided having data:
      | TSH | 6.7 |
      | Age | 30 |
    And case Case2 is provided having data:
      | TSH | 8.9 |
      | Age | 60 |
    And case Case3 is provided having data:
      | TSH | 2.1 |
      | Age | 70 |
    And I start the client application
    And I see the case Case1 as the current case
    When I ask to search for cases by conditions
    And I give the search conditions:
      | TSH is greater than 4 |
      | Age is greater than 50 |
    And I say that the conditions are to be joined by and
    And I say that at most 10 cases are to be found
    And I say that the following case lists are to be searched:
      | processed |
    And I say that the cases found are to go in the highOldTSH case list
    And I confirm the search
    Then the user-defined highOldTSH case list should contain:
      | Case2 |

  Scenario: Conditions can be or-ed.
    Given case Case1 is provided having data:
      | TSH | 6.7 |
      | Age | 30 |
    And case Case2 is provided having data:
      | TSH | 8.9 |
      | Age | 60 |
    And case Case3 is provided having data:
      | TSH | 2.1 |
      | Age | 70 |
    And case Case4 is provided having data:
      | TSH | 2.1 |
      | Age | 40 |
    And I start the client application
    And I see the case Case1 as the current case
    When I search the "processed" case list for all cases where "TSH is greater than 4 or Age is greater than 50" and put the results in the highOrOld case list
    And I confirm the search
    Then the user-defined highOrOld case count should be 3
    And the user-defined highOrOld case list should contain the following cases in any order:
      | Case1 |
      | Case2 |
      | Case3 |

  Scenario: At most as many cases as were asked for are found.
    Given case Case1 is provided having data:
      | TSH | 2.1 |
    And case Case2 is provided having data:
      | TSH | 6.7 |
    And case Case3 is provided having data:
      | TSH | 8.9 |
    And case Case4 is provided having data:
      | TSH | 9.9 |
    And I start the client application
    And I see the case Case1 as the current case
    When I search the "processed" case list for cases where "TSH is greater than 4" and put at most 2 results in the firstTwo case list
    And I confirm the search
    Then the user-defined firstTwo case count should be 2
    And the chatbot response contains the following terms:
      | found | 2 |

  Scenario: All cases are tried when fewer match than were asked for.
    Given case Case1 is provided having data:
      | TSH | 2.1 |
    And case Case2 is provided having data:
      | TSH | 6.7 |
    And I start the client application
    And I see the case Case1 as the current case
    When I search the "processed" case list for cases where "TSH is greater than 4" and put at most 3 results in the HighTSH case list
    And I confirm the search
    Then the user-defined HighTSH case list should contain:
      | Case2 |
    And the chatbot response contains the following terms:
      | found | 1 |

  Scenario: A search can be restricted to the cornerstone case list.
    Given case Case1 is provided having data:
      | TSH | 9.9 |
    And cornerstone case CCase1 is provided having data:
      | TSH | 6.7 |
    And cornerstone case CCase2 is provided having data:
      | TSH | 2.1 |
    And I start the client application
    And I see the case Case1 as the current case
    When I search the "cornerstones" case list for all cases where "TSH is greater than 4" and put the results in the HighTSH case list
    And I confirm the search
    Then the user-defined HighTSH case list should contain:
      | CCase1 |

  Scenario: A search can cover several case lists, without de-duplication.
    Given case Case1 is provided having data:
      | TSH | 6.7 |
    And case Case2 is provided having data:
      | TSH | 2.1 |
    And I start the client application
    And I see the case Case1 as the current case
    And I copy the current case to the Good case list
    When I search the "processed and Good" case lists for all cases where "TSH is greater than 4" and put the results in the HighTSH case list
    And I confirm the search
    Then the user-defined HighTSH case count should be 2
    And the user-defined HighTSH case list should contain:
      | Case1 |
      | Case1 |

  Scenario: Results can be merged into an existing case list, regardless of the case of its name.
    Given case Case1 is provided having data:
      | TSH | 6.7 |
    And case Case2 is provided having data:
      | TSH | 2.1 |
    And case Case3 is provided having data:
      | TSH | 8.9 |
    And I start the client application
    And I see the case Case1 as the current case
    And I select case Case2
    And I copy the current case to the Good case list
    When I search the "processed" case list for all cases where "TSH is greater than 4" and put the results in the good case list
    And I confirm the search
    Then the user-defined Good case list should contain the following cases in any order:
      | Case2 |
      | Case1 |
      | Case3 |

  Scenario: Conditions can be about comments, so cases are interpreted before being checked.
    Given case Case1 is provided having data:
      | TSH | 6.7 |
    And case Case2 is provided having data:
      | TSH | 2.1 |
    And a rule exists to add the comment "Borderline" to case Case1 for the following conditions:
      | TSH is greater than 4 |
    And I start the client application
    And I see the case Case2 as the current case
    When I search the "processed and cornerstones" case lists for all cases where "Borderline is in case" and put the results in the borderline case list
    And I confirm the search
    Then the user-defined borderline case list should contain:
      | Case1 |

  Scenario: Search results cannot be put into the processed case list.
    Given case Case1 is provided having data:
      | TSH | 6.7 |
    And I start the client application
    And I see the case Case1 as the current case
    When I search the "processed" case list for all cases where "TSH is greater than 4" and put the results in the processed case list
    Then the chatbot response contains the following terms:
      | Cannot | Processed |

  Scenario: A condition that names an attribute not in the KB is refused.
    Given case Case1 is provided having data:
      | TSH | 6.7 |
    And I start the client application
    And I see the case Case1 as the current case
    When I search the "processed" case list for all cases where "BMI is greater than 5" and put the results in the nowhere case list
    Then the chatbot response contains the following terms:
      | BMI |

  Scenario: No destination list is created when no cases are found.
    Given case Case1 is provided having data:
      | TSH | 2.1 |
    And I start the client application
    And I see the case Case1 as the current case
    When I search the "processed" case list for all cases where "TSH is greater than 4" and put the results in the noneFound case list
    And I confirm the search
    Then the chatbot response contains the following terms:
      | found |
    And the noneFound case list is not shown

  Scenario: A search can be started when there is no currently selected case.
    Given I start the client application
    When I search the "processed" case list for all cases where "TSH is greater than 4" and put the results in the anyFound case list
    And I confirm the search
    Then the chatbot response contains the following terms:
      | found |
    And the anyFound case list is not shown

  Scenario: A search cannot be started while a rule is being built.
    Given case Case1 is provided having data:
      | TSH | 6.7 |
    And I start the client application
    And I see the case Case1 as the current case
    When I request that the comment "Tea is good." be added
    And the chatbot has asked if I want to provide any reasons
    When I search the "processed" case list for all cases where "TSH is greater than 4" and put the results in the midRule case list
    Then the chatbot response contains the following terms:
      | Please finish or cancel the current rule | search |

  Scenario: The user can abandon the search setup.
    Given case Case1 is provided having data:
      | TSH | 6.7 |
    And I start the client application
    And I see the case Case1 as the current case
    When I ask to search for cases by conditions
    And I give the sezarch conditions:
      | TSH is greater than 4 |
    And I abandon the search setup
    When I ask what capabilities are available
    Then the capabilities shown include:
      | Knowledge bases |

  # Ignored until the facility exists: stopping a search deterministically
  # needs one slow enough to interrupt, which needs a large case set and a
  # controllable search pace. The requirements promise that the cases found
  # before termination are kept, and that the chatbot simply states that the
  # search was terminated.
  @ignore
  Scenario: A search can be stopped, and the cases found so far are retained.
    Given a list of 100 cases is stored on the server
    And I start the client application
    And I see the case Case_001 as the current case
    When I search the "processed" case list for all cases where "TSH is greater than 0.1" and put the results in the partial case list
    And I confirm the search
    And I ask to stop the search
    Then the chatbot response contains the following terms:
      | terminated |
