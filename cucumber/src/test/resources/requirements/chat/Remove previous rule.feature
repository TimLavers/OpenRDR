@chat
Feature: Remove previous rules
  Scenario: The user should be able to remove the previous rule using the chat
    Given cases are set up as follows:
      | Case name | attribute name | value | comment    | condition    |
      | Case1     | x              | 1     | Comment 1. | x is in case |
      | Case2     | x              | 2     | Comment 2. | x is in case |
      | Case3     | x              | 3     | Comment 3. | x is in case |
    And I start the client application
    And I see the case Case1 as the current case
    And the report is "Comment 1. Comment 2. Comment 3."
    And the chat is showing
    And the chatbot has asked if I want to add, remove or replace a comment
    When I enter the following text into the chat panel:
      | Please remove the previous rule |
    Then the report should be "Comment 1. Comment 2."
    When I enter the following text into the chat panel:
      | and again please |
    Then the report should be "Comment 1."
    When I enter the following text into the chat panel:
      | and once more |
    Then the interpretation report should be blank
