@requirement
Feature: Reviewing the interpretation of a case

  @single
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



