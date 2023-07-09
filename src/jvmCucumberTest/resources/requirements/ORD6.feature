Feature: Reviewing cornerstone cases

  Scenario: A user message should be shown if there are no cornerstone cases
    Given a new case with the name Case1 is stored on the server
    And I start the client application
    And I enter the text "Go to Bondi." in the interpretation field
    And I select the changes tab
    When I start to build a rule for the change on row 0
    Then The message "No cornerstone cases to review" should be shown

  Scenario: The first cornerstone case should be shown to the user
    Given a list of cases with the following names is stored on the server:
      | Case1 |
      | Case2 |
    And I start the client application
    And I see the case Case1 as the current case
    And I enter the text "Go to Bondi." in the interpretation field
    And I select the changes tab
    And I build a rule for the change on row 0
    And I select the case Case2
    And I replace the text in the interpretation field with "Go to Bondi. Grow some trees."
    And I select the changes tab
    When I start to build a rule for the change on row 1
    Then The case Case1 should be shown as the cornerstone case

  Scenario: The user should be able to switch to the next cornerstone case
    Given case Case1 is provided having data:
      | x | 1 |
    And case Case2 is provided having data:
      | x | 1 |
    And case Case3 is provided having data:
      | x | 1 |
    And the interpretation of the case Case1 includes "Comment 1." because of condition "x has a current value"
    And the interpretation of the case Case2 includes "Comment 2." because of condition "x has a current value"
    And the interpretation of the case Case3 includes "Comment 3." because of condition "x has a current value"
    And I start the client application
    And I see the case Case1 as the current case
    And I enter the text " Comment 4." in the interpretation field
    And I select the changes tab
    And I start to build a rule for the change on row 3
    And The case Case2 should be shown as the cornerstone case
    When I click the next cornerstone case button
    Then The case Case3 should be shown as the cornerstone case

  Scenario: The user should be able to switch to the previous cornerstone case
    Given case Case1 is provided having data:
      | x | 1 |
    And case Case2 is provided having data:
      | x | 1 |
    And case Case3 is provided having data:
      | x | 1 |
    And the interpretation of the case Case1 includes "Comment 1." because of condition "x has a current value"
    And the interpretation of the case Case2 includes "Comment 2." because of condition "x has a current value"
    And the interpretation of the case Case3 includes "Comment 3." because of condition "x has a current value"
    And I start the client application
    And I see the case Case1 as the current case
    And I enter the text " Comment 4." in the interpretation field
    And I select the changes tab
    And I start to build a rule for the change on row 3
    And I click the next cornerstone case button
    And The case Case3 should be shown as the cornerstone case
    When I click the previous cornerstone case button
    Then The case Case2 should be shown as the cornerstone case
