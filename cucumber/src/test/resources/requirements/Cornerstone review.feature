Feature: Reviewing cornerstone cases

  Scenario: A user message should be shown if there are no cornerstone cases
    Given a new case with the name Case1 is stored on the server
    And I start the client application
    And I enter the text "Go to Bondi." in the interpretation field
    And I select the changes tab
    When I start to build a rule for the change on row 0
    Then the message indicating no cornerstone cases to review should be shown

  Scenario: The first cornerstone case should be shown to the user
    Given a list of cases with the following names is stored on the server:
      | Case1 |
      | Case2 |
    And I start the client application
    And I see the case Case1 as the current case
    And I enter the text "Go to Bondi." in the interpretation field
    And I build a rule for this change
    And I select the case Case2
    And I replace the text in the interpretation field with "Go to Bondi. Grow some trees."
    When I start to build a rule for the change on row 1
    Then the case Case1 is shown as the cornerstone case

  Scenario: The user should be able to switch to the next cornerstone case
    Given case Case1 is provided having data:
      | x | 1 |
    And case Case2 is provided having data:
      | x | 2 |
    And case Case3 is provided having data:
      | x | 3 |
    And the interpretation of the case Case1 includes "Comment 1." because of condition "x is not blank"
    And the interpretation of the case Case2 includes "Comment 2." because of condition "x is not blank"
    And the interpretation of the case Case3 includes "Comment 3." because of condition "x is not blank"
    And I start the client application
    And I see the case Case1 as the current case
    And I enter the text " Comment 4." in the interpretation field
    And I select the changes tab
    And I start to build a rule for the change on row 0
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
    And the interpretation of the case Case1 includes "Comment 1." because of condition "x is not blank"
    And the interpretation of the case Case2 includes "Comment 2." because of condition "x is not blank"
    And the interpretation of the case Case3 includes "Comment 3." because of condition "x is not blank"
    And I start the client application
    And I see the case Case1 as the current case
    And I enter the text " Comment 4." in the interpretation field
    And I select the changes tab
    And I start to build a rule for the change on row 0
    And I click the next cornerstone case button
    And the case Case3 is shown as the cornerstone case
    When I click the previous cornerstone case button
    Then the case Case2 is shown as the cornerstone case

  @ignore
  Scenario: Cornerstones should vanish when the user adds a condition that excludes them
    Given case Case1 is provided having data:
      | x | 1 |
      | y | 1 |
    And case Case2 is provided having data:
      | x | 1 |
    And case Case3 is provided having data:
      | x | 1 |
    And the interpretation of the case Case1 includes "Comment 1." because of condition "x is not blank"
    And the interpretation of the case Case2 includes "Comment 2." because of condition "x is not blank"
    And the interpretation of the case Case3 includes "Comment 3." because of condition "x is not blank"
    And I start the client application
    And I see the case Case1 as the current case
    And I enter the text " Comment 4." in the interpretation field
    And I select the changes tab
    And I start to build a rule for the change on row 3
    And the case Case2 is shown as the cornerstone case
    And the conditions showing should be:
      | x is not blank |
      | y is not blank |
    When I select the condition "y is not blank"
    Then the message "No cornerstone cases to review" should be shown

  @ignore
  Scenario: The current cornerstones should remain selected if the user adds a condition that does not exclude it
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
      | CASE  | COMMENT ADDED | CONDITION             |
      | Case1 | Comment 1.    | x is not blank |
      | Case2 | Comment 2.    | x is not blank |
      | Case3 | Comment 3.    | x is not blank |
      | Case4 | Comment 4.    | x is not blank |
    And I start the client application
    And I see the case Case1 as the current case
    And I enter the text " Comment 5." in the interpretation field
    And I select the changes tab
    And I start to build a rule for the change on row 4
    And the case Case2 is shown as the cornerstone case
    And I click the next cornerstone case button
    And the case Case3 is shown as the cornerstone case
    And the conditions showing should be:
      | x is not blank |
      | y is not blank |
    When I select the condition "y is not blank"
    Then the case Case3 is still shown as the cornerstone case

