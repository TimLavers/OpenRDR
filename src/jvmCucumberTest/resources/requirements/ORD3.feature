Feature: Knowledge Base management

  @single
  Scenario: Name of current Knowledge Base should be displayed
    Given I start the client application
    Then the displayed KB name should be Thyroids
    And stop the client application
  Scenario: A previously exported Knowledge Base can be imported
    Given I start the client application
    Then the displayed KB name should be Thyroids
    And I import the configured zipped Knowledge Base Whatever
    Then the displayed KB name should be Whatver
