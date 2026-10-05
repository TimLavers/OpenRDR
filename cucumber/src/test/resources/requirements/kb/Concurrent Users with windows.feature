Feature: Two users work in the one knowledge base, each in their own window

  What one user sees of another's work is decided by which pushes the server sends to whom, and a window is the only
  place that can be observed. The chat panel in each window is driven by the real model, so this is a slow scenario.

  Background:
    Given a default KB is opened

  Scenario: A rule session is private to the user building it
    Given case Case1 is provided having data:
      | x | 1 |
    And case Case2 is provided having data:
      | x | 2 |
    And the interpretation of the case Case2 includes "Comment 2." because of condition "x is in case"
    And Alice starts the client application
    And Bob starts the client application
    And I see the case Case1 as the current case
    And the interpretation should be "Comment 2."
    When I switch to Alice's window
    And I see the case Case1 as the current case
    And I request that the comment "Comment 4." be added
    And the case Case2 is shown as the cornerstone case
    And I switch to Bob's window
    Then there are no cornerstone cases showing
    And the interpretation should be "Comment 2."
    When I switch to Alice's window
    And the chatbot has asked if I want to provide any reasons and I decline
    And the chatbot has asked if want to allow the report change to the cornerstone case and I confirm
    And the chatbot has completed the action
    Then the interpretation should be "Comment 2. Comment 4."
    When I switch to Bob's window
    Then the interpretation should be "Comment 2."
    When I select the case Case2
    Then the interpretation should be "Comment 2. Comment 4."
