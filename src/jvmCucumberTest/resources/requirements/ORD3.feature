
Feature: Knowledge Base management

  Scenario: Name of current Knowledge Base should be displayed
    Given I start the client application
    Then the displayed KB name is now Thyroids
    And stop the client application

  Scenario: A previously exported Knowledge Base can be imported
    Given I start the client application
    And the displayed KB name is Thyroids
    When I import the configured zipped Knowledge Base Whatever
    Then the displayed KB name is now Whatever
    And stop the client application

  Scenario: A Knowledge Base can be exported
    Given I start the client application
    And the displayed KB name is Thyroids
    And I export the current Knowledge Base
    And there is a file called Thyroids.zip in my downloads directory
    And I import the configured zipped Knowledge Base Whatever
    And the displayed KB name is Whatever
    And pause for 1 second
    When I import the exported Knowledge Base Thyroids
    Then the displayed KB name is now Thyroids
    And stop the client application

