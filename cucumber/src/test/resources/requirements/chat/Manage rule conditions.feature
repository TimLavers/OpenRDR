@chat
Feature: Manage rule conditions

  Scenario: The user should be able to build a rule using a misspelled attribute name
    Given case Bondi is provided having data:
      |  height | 2 |
    And I start the client application
    And I see the case Bondi as the current case
    And the chat is showing
    And I request that the comment "Let's surf." be added
    When I provide only the following reason:
      |  hite > 1 |
    Then the conditions showing for the comment "Let's surf." are:
      |  height > 1.0 |
    And stop the client application

  Scenario: The user should be able to build a rule using several conditions
    Given case Bondi is provided having data:
      |  height | 2  |
      |  period | 10 |
    And I start the client application
    And I see the case Bondi as the current case
    And the chat is showing
    And I request that the comment "Let's surf." be added
    When I provide only the following reasons:
      |  height > 1 |
      |  period > 5 |
    Then the conditions showing for the comment "Let's surf." are:
      |  height > 1.0 |
      |  period > 5.0 |
    And stop the client application

  Scenario: The user should be able to see the conditions in the current rule building session
    Given case Bondi is provided having data:
      |  height | 2  |
      |  period | 10 |
    And I start the client application
    And I see the case Bondi as the current case
    And the chat is showing
    And I request that the comment "Let's surf." be added
    And I provide the following reasons:
      |  height > 1 |
      |  period > 5 |
    And the chatbot has asked if I want to provide any more reasons
    And I ask to see the reasons
    Then the chatbot lists the following reasons:
      | height > 1.0 |
      | period > 5.0 |
    And stop the client application

  Scenario: The user should be able to remove a condition from the current rule building session
    Given case Bondi is provided having data:
      |  height | 2  |
      |  period | 10 |
    And I start the client application
    And I see the case Bondi as the current case
    And the chat is showing
    And I request that the comment "Let's surf." be added
    And I provide the following reasons:
      | height > 1 |
      | period > 5 |
    And the chatbot has asked if I want to provide any more reasons
    When I request that the first reason be removed
    And the chatbot has asked if I want to provide any more reasons and I decline
    And the chatbot has completed the action
    Then the condition showing for the comment "Let's surf." is:
      |  period > 5.0 |
    And stop the client application
