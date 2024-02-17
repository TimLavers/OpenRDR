Feature: The Knowledge Base described in the paper

  Scenario: TSH KB cases
    Given the configured case Case4 is stored on the server
    Given I start the client application
    And pause for 5 seconds
    And stop the client application
