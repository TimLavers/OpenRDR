@chat
Feature: Add comments without conditions

  Scenario: The user should be able to use the chat to add a comment to a blank report, with no conditions
    Given case Bondi is provided having data:
      | Wave | excellent |
      | Sun  | hot       |
    And I start the client application
    And the report is empty
    And I see the case Bondi as the current case
    And the chat is showing
    And the chatbot has asked if I would like to add a comment
    And I confirm
    And the chatbot has asked for what comment I want to add
    And I enter the following text into the chat panel:
      | Let's surf. |
    And the chatbot has asked for confirmation
    And I confirm
    And the chatbot has asked if I want to provide any reasons
    When I decline
    Then the report should be "Let's surf."
    And stop the client application

  Scenario: The user should be able to use the chat to add two comments with no conditions
    Given case Bondi is provided having data:
      | Wave | excellent |
      | Sun  | hot       |
    And I start the client application
    And I see the case Bondi as the current case
    And the chat is showing
    And the report is empty
    And  I build a rule to add an initial comment "Let's surf." using the chat with no condition
    And I enter the following text into the chat panel:
      | Add another comment "Bring flippers." |
    And the chatbot has asked for confirmation
    And I confirm
    And the chatbot has asked if I want to provide any reasons
    When I decline
    Then the report should be "Let's surf. Bring flippers."
    And stop the client application

  Scenario: The user should be able to use the chat to add comments with no conditions to two cases
    Given a list of cases with the following names is stored on the server:
      | Case1 |
      | Case2 |
    And I start the client application
    And I should see the case Case1 as the current case
    And the chat is showing
    And I build a rule to add an initial comment "Let's surf." using the chat with no condition
    And the report should be "Let's surf."
    And select the case Case2
    And the interpretation should be "Let's surf."
    When I build a rule to add another comment "Bring flippers." using the chat
    Then the report should be "Let's surf. Bring flippers."
    And stop the client application