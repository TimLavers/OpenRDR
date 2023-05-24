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
    And the interpretation by the project of the case Case1 is "Go to Bondi."
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
    And the interpretation by the project of the case Case1 is "Go to Bondi."
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

  Scenario: When the user starts to build a rule, condition hints should be shown
    Given I start the client application
    And case Bondi is provided having data:
      | Wave | excellent |
      | Sun  | hot       |
    And I enter the text "Let's surf" in the interpretation field
    And the changes badge indicates that there is 1 change
    And I select the changes tab
    When I start to build a rule for the change on row 0
    Then the conditions showing should be:
      | Wave has a current value |
      | Sun has a current value  |
    And stop the client application

  Scenario: The user should be able to build a rule to add a comment with a condition they have selected
    Given I start the client application
    And case Bondi is provided having data:
      | Wave | excellent |
      | Sun  | hot       |
    And I enter the text "Go to the beach." in the interpretation field
    And the changes badge indicates that there is 1 change
    And I select the changes tab
    And I start to build a rule for the change on row 0
    And the conditions showing should be:
      | Wave has a current value |
      | Sun has a current value  |
    When I select the first condition
    And I complete the rule
    And I select the interpretation tab
    And I open the conclusions dialog
    And click the comment "Go to the beach."
    Then the conditions showing are:
      | Wave has a current value |
    And close the conclusions dialog
    And stop the client application

  Scenario: The user should be able to build a rule to remove a comment with a condition they have selected
    Given case Bondi is provided having data:
      | Wave | poor    |
      | Sun  | too hot |
    And case Manly is provided having data:
      | Swimming | pleasant |
      | Sun      | hot      |
    And the interpretation by the project of the case Bondi is "Go to Bondi."
    And I start the client application
    And I select case Bondi
    And  the interpretation field should contain the text "Go to Bondi."
    And I delete all the text in the interpretation field
    And the changes badge indicates that there is 1 change
    And I select the changes tab
    And I start to build a rule for the change on row 0
    And the conditions showing should be:
      | Wave has a current value |
      | Sun has a current value  |
    And I select the first condition
    When I complete the rule
    And I select the interpretation tab
    Then  the interpretation field should be empty
    And I select case Manly
    And  the interpretation field should contain the text "Go to Bondi."
    And stop the client application

  Scenario: The user should be able to build a rule to replace a comment with a condition they have selected
    Given case Bondi is provided having data:
      | Wave | poor    |
      | Sun  | too hot |
    And case Manly is provided having data:
      | Swimming | pleasant |
      | Sun      | hot      |
    And the interpretation by the project of the case Bondi is "Go to Bondi."
    And I start the client application
    And I select case Manly
    And  the interpretation field should contain the text "Go to Bondi."
    And I replace the text in the interpretation field with "Go to Manly."
    And the changes badge indicates that there is 1 change
    And I select the changes tab
    And I start to build a rule for the change on row 0
    And the conditions showing should be:
      | Swimming has a current value |
      | Sun has a current value      |
    And I select the first condition
    When I complete the rule
    And I select the interpretation tab
    Then  the interpretation field should contain the text "Go to Manly."
    And I select case Bondi
    And  the interpretation field should contain the text "Go to Bondi."
    And stop the client application

  Scenario: A new rule should apply to any case satisfying its conditions
    Given I start the client application
    And case Bondi is provided having data:
      | Wave | excellent |
      | Sun  | hot       |
    And case Manly is provided having data:
      | Swimming | pleasant |
      | Sun      | hot      |
    And case Malabar is provided having data:
      | Swimming | pleasant |
    And I select case Bondi
    And I enter the text "Go for a surf." in the interpretation field
    And I select the changes tab
    And I start to build a rule for the change on row 0
    And the conditions showing should be:
      | Wave has a current value |
      | Sun has a current value  |
    And I select the second condition
    And I complete the rule
    When I select case Manly
    Then the interpretation field should contain the text "Go for a surf."
    And I select case Malabar
    And the interpretation field should be empty
    And stop the client application
