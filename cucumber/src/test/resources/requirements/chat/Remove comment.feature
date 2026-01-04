@chat
Feature: Remove comment

  Scenario: The user should be able to use the chat to remove a comment with a valid condition
    Given case Bondi is provided having data:
      | wave height | 2 |
    And the interpretation of the case Bondi consists of the following comments:
      | Go to the beach. |
      | Bring flippers.  |
    And I start the client application
    And I see the case Bondi as the current case
    And the chat is showing
    And the chatbot has asked if I want to add, remove or replace a comment
    And I enter the following text into the chat panel:
      | Remove the flippers comment |
    And the chatbot has asked for confirmation
    And I confirm
    And the chatbot has asked if I want to provide any reasons and I confirm
    And the chatbot has asked for the first reason
    And I enter the following text into the chat panel:
      | wave height is more than 0.5 |
    When the chatbot has asked if I want to provide any more reasons and I decline
    Then the report should be "Go to the beach."
    And stop the client application

