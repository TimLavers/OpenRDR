@single
Feature: The user can use the chat to make changes the interpretive report
  Scenario: The user should be able to use the chat to add a comment with no conditions
    Given case Bondi is provided having data:
      | Wave | excellent |
      | Sun  | hot       |
    And I start the client application
    And I see the case Bondi as the current case
    And the report is empty
    And the chatbot has asked if I want to add a comment
    And I enter the following text into the chat panel:
      | The report should say "Let's surf" |
    And the chatbot has asked for confirmation
    When I confirm
    Then the report should be "Let's surf"
    And stop the client application

  Scenario: The user should be able to use the chat to add two comments with no conditions
    Given case Bondi is provided having data:
      | Wave | excellent |
      | Sun  | hot       |
    And I start the client application
    And I see the case Bondi as the current case
    And the report is empty
    And I have added a comment "Let's surf." using the chat
    And I enter the following text into the chat panel:
      | Add another comment "Bring flippers." |
    And the chatbot has asked for confirmation
    When I confirm
    Then the report should be "Let's surf. Bring flippers."
    And stop the client application
