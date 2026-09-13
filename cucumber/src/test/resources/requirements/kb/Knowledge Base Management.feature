Feature: Knowledge Base management

  Background:
    Given a default KB is opened

  Scenario: A previously exported Knowledge Base can be imported
    Given I start the client application
    And the displayed KB name is Thyroids
    When I import the configured zipped Knowledge Base Whatever
    Then the displayed KB name is now Whatever

  Scenario: A Knowledge Base can be exported
    Given I start the client application
    And the displayed KB name is Thyroids
    And I export the current Knowledge Base
    And I import the configured zipped Knowledge Base Whatever
    And the displayed KB name is Whatever
    When I import the previously exported Knowledge Base
    Then the displayed KB name is now Thyroids
