Feature: Reviewing the interpretation of a case

  Scenario: The user should be able to see the interpretation of a case
    Given case Bondi is provided having data:
      | Wave | excellent |
    And the interpretation of the case Bondi is "Go to Bondi."
    When I start the client application
    Then  I should see the case Bondi as the current case
    And the interpretation field should contain the text "Go to Bondi."
    And stop the client application

  Scenario: The user should be able to see different interpretations for different cases
    Given case Bondi is provided having data:
      | Wave | excellent |
    And case Manly is provided having data:
      | Swimming | excellent |
    And the interpretation of the case Bondi includes "Go to Bondi." because of condition "Wave is not blank"
    And the interpretation of the case Manly includes "Go to Manly." because of condition "Swimming is not blank"
    And I start the client application
    And I should see the case Bondi as the current case
    And the interpretation field should contain the text "Go to Bondi."
    When I select the case Manly
    Then the interpretation field should contain the text "Go to Manly."
    And stop the client application

  Scenario: The changes to an interpretation should be shown
    Given a list of cases with the following names is stored on the server:
      | Case1 |
    And I start the client application
    And I should see the case Case1 as the current case
    When I enter the text "Go to Bondi." in the interpretation field
    Then the interpretation field should contain the text "Go to Bondi."
    And stop the client application

  Scenario: The changes to an interpretation should be saved
    Given a list of cases with the following names is stored on the server:
      | Case1 |
      | Case2 |
    And I start the client application
    And I should see the case Case1 as the current case
    When I enter the text "Go to Bondi." in the interpretation field
    And select the case Case2
    And select the case Case1
    Then the interpretation field should contain the text "Go to Bondi."
    And stop the client application

  Scenario: The change to a case's interpretation should not affect other cases
    And case Bondi is provided having data:
      | Wave | excellent |
    And case Manly is provided having data:
      | Swimming | excellent |
    And the interpretation of the case Bondi includes "Go to Bondi." because of condition "Wave is not blank"
    And the interpretation of the case Manly includes "Go to Manly." because of condition "Swimming is not blank"
    And I start the client application
    And I should see the case Bondi as the current case
    And I enter the text "and bring flippers" in the interpretation field
    When I select the case Manly
    Then the interpretation field should contain the text "Go to Manly."
    And stop the client application

  Scenario: A changed to the interpretation should be able to be reverted
    And case Bondi is provided having data:
      | Wave | excellent |
    And case Manly is provided having data:
      | Swimming | excellent |
    And the interpretation of the case Bondi includes "Go to Bondi." because of condition "Wave is not blank"
    And the interpretation of the case Manly includes "Go to Manly." because of condition "Swimming is not blank"
    And I start the client application
    And I should see the case Bondi as the current case
    And I enter the text "and bring flippers" in the interpretation field
    And I select the case Manly followed by Bondi
    When I enter the text "Go to Bondi." in the interpretation field
    And I select the case Manly followed by Bondi
    Then the interpretation field should contain the text "Go to Bondi."
    And stop the client application

  Scenario: The badge indicating the number of changes to an interpretation should be shown
    Given a new case with the name Case1 is stored on the server
    And  a new case with the name Case2 is stored on the server
    And the interpretation of the case Case1 is "Go to Bondi."
    And I start the client application
    And I should see the case Case1 as the current case
    And the interpretation field should contain the text "Go to Bondi."
    When I enter the text "No, go to Malabar." in the interpretation field
    And I select the changes tab
    Then the changes badge indicates that there is 1 change
    And stop the client application

  Scenario: The badge indicating the number of changes to an interpretation should be saved for two cases
    Given a new case with the name Case1 is stored on the server
    And  a new case with the name Case2 is stored on the server
    And the interpretation of the case Case1 is "Go to Bondi."
    And I start the client application
    And I should see the case Case1 as the current case
    And the interpretation field should contain the text "Go to Bondi."
    And I enter the text "And bring your flippers." in the interpretation field
    And the changes badge indicates that there is 1 change
    And select the case Case2
    And I enter the text "Go to Manly. And bring your sunscreen" in the interpretation field
    And the changes badge indicates that there are 2 changes
    When select the case Case1
    Then the changes badge indicates that there is 1 change
    And stop the client application

  Scenario: A new comment that is entered by the user should show as a addition in the changes panel
    Given a new case with the name Case1 is stored on the server
    And the interpretation of the case Case1 is "Go to Bondi."
    And I start the client application
    And I should see the case Case1 as the current case
    And the interpretation field should contain the text "Go to Bondi."
    When I add the text "And bring your flippers." at the end of the current interpretation
    And  the interpretation field should contain the text "Go to Bondi. And bring your flippers."
    And I select the changes tab
    Then the differences table should show 2 rows
    And I should see in row 2 that the text "And bring your flippers." has been added
    And stop the client application

  Scenario: A comment that is deleted by the user should show as a removal in the changes panel
    Given a new case with the name Case1 is stored on the server
    And the interpretation of the case Case1 is "Go to Bondi."
    And I start the client application
    And I should see the case Case1 as the current case
    And the interpretation field should contain the text "Go to Bondi."
    When I delete all the text in the interpretation field
    And I select the changes tab
    Then the differences table should show 1 row
    And I should see in row 1 that the text "Go to Bondi." has been deleted
    And stop the client application

  Scenario: A comment that is replaced by the user should show as a replacement in the changes panel
    Given a new case with the name Case1 is stored on the server
    And the interpretation of the case Case1 is "Go to Bondi."
    And I start the client application
    And I should see the case Case1 as the current case
    And the interpretation field should contain the text "Go to Bondi."
    When I delete all the text in the interpretation field
    And I enter the text "Go to Manly." in the interpretation field
    And I select the changes tab
    Then the differences table should show 1 row
    And I should see in row 1 that the text "Go to Bondi." has been replaced by "Go to Manly."
    And stop the client application

  Scenario: A comment should be able to be entered very slowly
    Given a new case with the name Case1 is stored on the server
    And I start the client application
    And I should see the case Case1 as the current case
    When I slowly type the text "go to bondi." in the interpretation field
    Then the interpretation field should contain the text "go to bondi."
    And stop the client application



