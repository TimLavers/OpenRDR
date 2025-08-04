@chat
Feature: Replace comment

  Scenario: The user should be able to use the chat to replace a comment with a valid condition
    Given case Bondi is provided having data:
      | wave height | 2   |
      | UV          | 7.0 |
    And the interpretation of the case Bondi consists of the following comments:
      | Go to the beach. |
      | Bring flippers.  |
    And I start the client application
    And I see the case Bondi as the current case
    And the chat is showing
    And the chatbot has asked if I want to add, remove or replace a comment
    And I enter the following text into the chat panel:
      | Replace the flippers comment by "Don't forget sunscreen." |
    And the chatbot has asked for confirmation
    And I confirm
    And the chatbot has asked if I want to provide any reasons
    And I confirm
    And the chatbot has asked for the first reason
    And I enter the following text into the chat panel:
      | UV more than 4 |
    And the chatbot has asked if I want to provide any more reasons
    When I decline
    Then the report should be "Go to the beach. Don't forget sunscreen."
    And stop the client application

