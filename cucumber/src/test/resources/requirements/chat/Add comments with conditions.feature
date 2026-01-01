@chat
Feature: Add comments with conditions
  Scenario: The user should be able to use the chat to add a comment with a valid condition
    Given case Bondi is provided having data:
      | wave height | 2 |
    And I start the client application
    And I see the case Bondi as the current case
    And the chat is showing
    And the chatbot has asked if I would like to add a comment
    And I confirm
    And the chatbot has asked for what comment I want to add
    And I enter the following text into the chat panel:
      | Let's surf. |
    And the chatbot has asked for confirmation
    And I confirm
    And the chatbot has asked if I want to provide any reasons and I confirm
    And the chatbot has asked for the first reason
    And I enter the following text into the chat panel:
      | wave height is more than 0.5 |
    When the chatbot has asked if I want to provide any more reasons and I decline
    Then the report should be "Let's surf."
    And the condition showing for the comment "Let's surf." is:
      | wave height > 0.5 |
    And stop the client application

  Scenario: The user should be able to see the reason why their condition expression is invalid
    Given case Bondi is provided having data:
      | wave height | 2 |
    And I start the client application
    And I see the case Bondi as the current case
    And the chat is showing
    And the chatbot has asked if I would like to add a comment
    And I confirm
    And the chatbot has asked for what comment I want to add
    And I enter the following text into the chat panel:
      | Let's surf. |
    And the chatbot has asked for confirmation
    And I confirm
    And the chatbot has asked if I want to provide any reasons and I confirm
    And the chatbot has asked for the first reason
    When I enter the following text into the chat panel:
      | wave height is more than 2 |
    Then the chatbot indicates that this reason is not true for the current case
    And stop the client application
