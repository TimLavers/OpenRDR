@chat
Feature: Manage rule conditions

  Scenario: The user should be able to build a rule using several conditions
    Given case Bondi is provided having data:
      | wave height | 2  |
      | wave period | 10 |
    And I start the client application
    And I see the case Bondi as the current case
    And the chat is showing
    And I request that the comment "Let's surf." be added
    When I provide only the following reasons:
      | wave height is more than 1 |
      | wave period is more than 5 |
    Then the conditions showing for the comment "Let's surf." are:
      | wave height > 1.0 |
      | wave period > 5.0 |
    And stop the client application

  Scenario: The user should be able to see the conditions in the current rule building session
    Given case Bondi is provided having data:
      | wave height | 2  |
      | wave period | 10 |
    And I start the client application
    And I see the case Bondi as the current case
    And the chat is showing
    And I request that the comment "Let's surf." be added
    And I provide the following reasons:
      | wave height is more than 1 |
      | wave period is more than 5 |
    And the chatbot has asked if I want to provide any more reasons
    And I ask to see the reasons
    Then the chatbot lists the following reasons:
      | 1. wave height > 1.0 |
      | 2. wave period > 5.0 |
    And stop the client application

  Scenario: The user should be able to remove a condition from the current rule building session
    Given case Bondi is provided having data:
      | wave height | 2  |
      | wave period | 10 |
    And I start the client application
    And I see the case Bondi as the current case
    And the chat is showing
    And I request that the comment "Let's surf." be added
    And I provide the following reasons:
      | wave height is more than 1 |
      | wave period is more than 5 |
    And the chatbot has asked if I want to provide any more reasons
    When I request that the first reason be removed
    And the chatbot has asked if I want to provide any more reasons and I decline
    Then the condition showing for the comment "Let's surf." is:
      | wave period > 5.0 |
    And stop the client application
