Feature: Application management
  Scenario: The product name should be displayed
    Given I start the client application
    Then the displayed product name is 'Open RippleDown'
    And stop the client application