#@chat
Feature: Review cornerstone cases

  @single
  Scenario: The user should be able to see the next cornerstone case using the chat
    Given cases are set up as follows:
      | Case name | attribute name | value | comment    | condition    |
      | Case1     | x              | 1     | Comment 1. | x is in case |
      | Case2     | x              | 2     | Comment 2. | x is in case |
      | Case3     | x              | 3     | Comment 3. | x is in case |
    And I start the client application
    And I see the case Case1 as the current case
    And the chat is showing
    And I start to build a rule using the chat to add the comment "Comment 4."
    And pause
    And the case Case2 is shown as the cornerstone case
    When I enter the following text into the chat panel:
      | next cornerstone case |
    And pause
    Then the case Case3 is shown as the cornerstone case