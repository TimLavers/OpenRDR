Feature: Building rules

  Scenario: The user should be able to build a rule to add a comment
    Given a list of cases with the following names is stored on the server:
      | Case1 |
      | Case2 |
    And I start the client application
    And I should see the case Case1 as the current case
    And I enter the text "Go to Bondi." in the interpretation field
    And the changes badge indicates that there is 1 change
    And I select the changes tab
    When I build a rule for the change on row 0
    Then the changes badge indicates that there is no change
    And I select the interpretation tab
    And  the interpretation field should contain the text "Go to Bondi."
    And select the case Case2
    And  the interpretation field should contain the text "Go to Bondi."
    And stop the client application

  Scenario: The user should be able to build rules to add several comments
    Given a list of cases with the following names is stored on the server:
      | Case1 |
      | Case2 |
    And I start the client application
    And I should see the case Case1 as the current case
    And I enter the text "Go to Bondi." in the interpretation field
    And I select the changes tab
    And I build a rule for the change on row 0
    And I select the interpretation tab
    And I replace the text in the interpretation field with "Go to Bondi. Grow some trees."
    And the changes badge indicates that there is 1 change
    And I select the changes tab
    When I build a rule for the change on row 1
    And I select the interpretation tab
    Then  the interpretation field should contain the text "Go to Bondi. Grow some trees."
    And select the case Case2
    And  the interpretation field should contain the text "Go to Bondi. Grow some trees."
    And stop the client application

  Scenario: The user should be able to build a rule to remove a comment
    Given a list of cases with the following names is stored on the server:
      | Case1 |
      | Case2 |
    And the interpretation by the project of the case "Case1" is "Go to Bondi."
    And I start the client application
    And I should see the case Case1 as the current case
    And  the interpretation field should contain the text "Go to Bondi."
    And I delete all the text in the interpretation field
    And the changes badge indicates that there is 1 change
    And I select the changes tab
    When I build a rule for the change on row 0
    Then the changes badge indicates that there is no change
    And I select the interpretation tab
    And  the interpretation field should be empty
    And select the case Case2
    And  the interpretation field should be empty
    And stop the client application

  Scenario: The user should be able to build a rule to replace a comment
    Given a list of cases with the following names is stored on the server:
      | Case1 |
      | Case2 |
    And the interpretation by the project of the case "Case1" is "Go to Bondi."
    And I start the client application
    And I should see the case Case1 as the current case
    And  the interpretation field should contain the text "Go to Bondi."
    And I replace the text in the interpretation field with "Go to Maroubra."
    And the changes badge indicates that there is 1 change
    And I select the changes tab
    When I build a rule for the change on row 0
    Then the changes badge indicates that there is no change
    And I select the interpretation tab
    And  the interpretation field should contain the text "Go to Maroubra."
    And select the case Case2
    And  the interpretation field should contain the text "Go to Maroubra."
    And stop the client application
