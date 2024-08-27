Feature: Reviewing cornerstone cases

  Scenario: A user message should be shown if there are no cornerstone cases
    Given a new case with the name Case1 is stored on the server
    And I start the client application
    And I see the case Case1 as the current case
    When I start to build a rule to add the comment "Go to Bondi."
    Then the message indicating no cornerstone cases to review should be shown

  Scenario: The first cornerstone case should be shown to the user
    Given a list of cases with the following names is stored on the server:
      | Case1 |
      | Case2 |
    And I start the client application
    And I see the case Case1 as the current case
    And I build a rule to add the comment "Go to Bondi."
    And I select the case Case2
    When I start to build a rule to replace the comment "Go to Bondi." by "Go to Maroubra."
    Then the case Case1 is shown as the cornerstone case

  Scenario: The user should be able to switch to the next cornerstone case
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
    And I start to build a rule to add the comment "Comment 4."
    And the case Case2 is shown as the cornerstone case
    When I click the next cornerstone case button
    Then the case Case3 is shown as the cornerstone case

  Scenario: The user should be able to switch to the previous cornerstone case
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
    And I start to build a rule to add the comment "Comment 4."
    And the cornerstone case indicator shows 1 of 2
    And I click the next cornerstone case button
    And the case Case3 is shown as the cornerstone case
    When I click the previous cornerstone case button
    Then the case Case2 is shown as the cornerstone case

  Scenario: Cornerstones should vanish when the user adds a condition that excludes them
    Given case Case1 is provided having data:
      | x | 1 |
      | y | 1 |
    And case Case2 is provided having data:
      | x | 1 |
    And case Case3 is provided having data:
      | x | 1 |
    And the interpretation of the case Case1 includes "Comment 1." because of condition "x is in case"
    And the interpretation of the case Case2 includes "Comment 2." because of condition "x is in case"
    And the interpretation of the case Case3 includes "Comment 3." because of condition "x is in case"
    And I start the client application
    And I see the case Case1 as the current case
    And I start to build a rule to add the comment "Comment 4."
    And the case Case2 is shown as the cornerstone case
    When I add the condition "y is in case"
    Then the message "No cornerstone cases to review" should be shown

  Scenario: The current cornerstone should remain selected if the user adds a condition that does not exclude it
    Given case Case1 is provided having data:
      | x | 1 |
      | y | 1 |
    And case Case2 is provided having data:
      | x | 1 |
    And case Case3 is provided having data:
      | x | 1 |
      | y | 1 |
    And case Case4 is provided having data:
      | x | 1 |
    And the following rules have been defined:
      | CASE  | COMMENT ADDED | CONDITION      |
      | Case1 | Comment 1.    | x is in case |
      | Case2 | Comment 2.    | x is in case |
      | Case3 | Comment 3.    | x is in case |
      | Case4 | Comment 4.    | x is in case |
    And I start the client application
    And I see the case Case1 as the current case
    And I start to build a rule to add the comment "Comment 5."
    And I click the next cornerstone case button
    And the case Case3 is shown as the cornerstone case
    And the cornerstone case indicator should show 2 of 3
    And I add the condition "y is in case"
    And the cornerstone case indicator should show 1 of 1
    And the case Case3 is still shown as the cornerstone case

  Scenario: The current cornerstone should remain selected if the user removes a condition
    Given case Case1 is provided having data:
      | x | 1 |
      | y | 1 |
    And case Case2 is provided having data:
      | x | 1 |
    And case Case3 is provided having data:
      | x | 1 |
      | y | 1 |
    And case Case4 is provided having data:
      | x | 1 |
    And the following rules have been defined:
      | CASE  | COMMENT ADDED | CONDITION      |
      | Case1 | Comment 1.    | x is in case |
      | Case2 | Comment 2.    | x is in case |
      | Case3 | Comment 3.    | x is in case |
      | Case4 | Comment 4.    | x is in case |
    And I start the client application
    And I see the case Case1 as the current case
    And I start to build a rule to add the comment "Comment 5."
    And I click the next cornerstone case button
    And the cornerstone case indicator should show 2 of 3
    And the case Case3 is shown as the cornerstone case
    And I add the condition "y is in case"
    And the cornerstone case indicator should show 1 of 1
    When I remove the condition "y is in case"
    Then the cornerstone case indicator should show 2 of 3
    And the case Case3 is still shown as the cornerstone case

  Scenario: Ignoring a cornerstone case should remove it from the list of cornerstone cases to review
    Given a list of cases with the following names is stored on the server:
      | Case1 |
      | Case2 |
    And I start the client application
    And I see the case Case1 as the current case
    And I build a rule to add the comment "Comment 1." for case Case1
    And I start to build a rule to add the comment "Comment 2." for case Case2
    And the case Case1 is shown as the cornerstone case
    When I approve the cornerstone case
    Then the case Case1 is no longer shown as the cornerstone case
    And the message "No cornerstone cases to review" should be shown

