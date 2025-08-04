@single
@chat
Feature: Review cornerstone cases

  Scenario: The user should be able to see the next cornerstone case using the chat
    Given case Case1 is provided having data:
      | x | 1 |
    And case Case2 is provided having data:
      | x | 2 |
    And case Case3 is provided having data:
      | x | 3 |
    And the interpretation of the case Case1 includes "Comment 1." because of condition "x is in case"
    And the interpretation of the case Case2 includes "Comment 2." because of condition "x is in case"
    And the interpretation of the case Case3 includes "Comment 3." because of condition "x is in case"
    And I start the client application
    And I see the case Case1 as the current case
    And the chat is showing

    And I start to build a rule to add the comment "Comment 4."
    And the case Case2 is shown as the cornerstone case
    When I enter the following text into the chat panel:
      | next cornerstone case |
    And pause
    Then the case Case3 is shown as the cornerstone case