@chat
Feature: Show cornerstones

  Scenario: The user should be able to review cornerstones when adding a comment using the chat
    Given cases are set up as follows:
      | Case name | attribute name | value | comment    | condition    |
      | Case1     | x              | 1     | Comment 1. | x is in case |
      | Case2     | x              | 2     | Comment 2. | x is in case |
      | Case3     | x              | 3     | Comment 3. | x is in case |
    And I start the client application
    And I see the case Case1 as the current case
    And the chat is showing
    And the chatbot has asked if I want to add, remove or replace a comment
    And I enter the following text into the chat panel:
      | Add the comment "Comment 4." |
    And the chatbot has asked for confirmation
    And I confirm
    And the case Case2 is shown as the cornerstone case
    And the chatbot has asked if I want to provide any reasons
    When I enter the following text into the chat panel:
      | No, I'm done. |
    Then there are no cornerstone cases showing
    And the chatbot has completed the action
    And the report should be "Comment 1. Comment 2. Comment 3. Comment 4."
    And stop the client application

  Scenario: The user should be able to review cornerstones when removing a comment using the chat
    Given cases are set up as follows:
      | Case name | attribute name | value | comment    | condition    |
      | Case1     | x              | a     | Comment 1. | x contains a |
      | Case2     | x              | a     |            |              |
    And I start the client application
    And select the case Case2
    And I see the case Case2 as the current case
    And the chat is showing
    And the chatbot has asked if I want to add, remove or replace a comment
    And I enter the following text into the chat panel:
      | Remove the comment |
    And the chatbot has asked for confirmation
    And I confirm
    And the case Case1 is shown as the cornerstone case
    And the chatbot has asked if I want to provide any reasons
    When I enter the following text into the chat panel:
      | No, I'm done. |
    Then there are no cornerstone cases showing
    And the chatbot has completed the action
    And the report should be empty
    And stop the client application

  Scenario: The user should be able to review cornerstones when replacing a comment using the chat
    Given cases are set up as follows:
      | Case name | attribute name | value | comment    | condition    |
      | Case1     | x              | a     | Comment 1. | x contains a |
      | Case2     | x              | a     |            |              |
    And I start the client application
    And select the case Case2
    And I see the case Case2 as the current case
    And the chat is showing
    And the chatbot has asked if I want to add, remove or replace a comment
    And I enter the following text into the chat panel:
      | Replace the comment with "Comment 2." |
    And the chatbot has asked for confirmation
    And I confirm
    And the case Case1 is shown as the cornerstone case
    And the chatbot has asked if I want to provide any reasons
    When I enter the following text into the chat panel:
      | No, I'm done. |
    Then there are no cornerstone cases showing
    And the chatbot has completed the action
    And the report should be "Comment 2."
    And stop the client application