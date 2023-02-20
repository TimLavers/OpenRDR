Feature: Knowledge Base management

  @single
  Scenario: Name of current Knowledge Base should be displayed
    Given I start the client application
    Then the displayed KB name should be Thyroids
    And stop the client application
  Scenario: A previously exported Knowledge Base can be imported
    Given I start the client application
    Then the displayed KB name should be Thyroids
    Given I import the configured zipped Knowledge Base Whatever
    Then the displayed KB name should be Whatever
  Scenario: A Knowledge Base can be exported
    Given I start the client application
    Then the displayed KB name should be Thyroids
    And I export the current Knowledge Base
    Then there is a file called Thyroids.zip in my downloads directory
    Given I import the configured zipped Knowledge Base Whatever
    Then the displayed KB name should be Whatever
    Given I import the exported Knowledge Base Thyroids
    Then the displayed KB name should be Thyroids

