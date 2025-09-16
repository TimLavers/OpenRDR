Feature: Knowledge Base import and export

  Scenario: A previously exported Knowledge Base can be imported
    Given A Knowledge Base called Thyroids has been created
    And I start the client application
    And the displayed KB name is Thyroids
    When I import the configured zipped Knowledge Base Whatever
    And pause for 2 seconds
    Then the displayed KB name is now Whatever
    And stop the client application

  Scenario: A Knowledge Base can be exported
    Given I start the client application
    And the displayed KB name is Thyroids
    And I export the current Knowledge Base
    And pause for 2 seconds
    And I import the configured zipped Knowledge Base Whatever
    And pause for 2 seconds
    And the displayed KB name is Whatever
    And pause for 2 seconds
    When I import the previously exported Knowledge Base
    Then the displayed KB name is now Thyroids
    And stop the client application
