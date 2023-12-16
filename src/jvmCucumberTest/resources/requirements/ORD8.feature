Feature: Changes to a KB should be persisted.

  @database
    @single
  Scenario: Rules are persisted.
    Given a list of cases with the following names is stored on the server:
      | Case1 |
    And I start the client application
    And I should see the case Case1 as the current case
    And I select the interpretation tab
    And I build a rule to add the conclusion "Go to Bondi." with no conditions
    And I stop the client application
    When I re-start the server application
    And I start the client application
    Then I should see the case Case1 as the current case
    And the interpretation should be "Go to Bondi."

