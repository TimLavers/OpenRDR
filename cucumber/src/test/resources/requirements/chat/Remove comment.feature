Feature: Remove comment

  Scenario: The user should be able to use the chat to remove a comment with a valid condition
    Given case Bondi is provided having data:
      | wave height | 2 |
    And the interpretation of the case Bondi consists of the following comments:
      | Go to the beach. |
      | Bring flippers.  |
    And I start the client application
    And I see the case Bondi as the current case
    And the chatbot has asked if I want to add, remove or replace a comment
    And I request that the following comment be removed:
      | Bring flippers. |
    And I provide only the following reason:
      | wave height is more than 0.5 |
    Then the report should be "Go to the beach."

