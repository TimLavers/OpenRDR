Feature: Knowledge Base management

  @single
  Scenario: Name of current Knowledge Base should be displayed
    Given I start the client application
    Then the displayed KB name should be Thyroids
    And stop the client application
#  Scenario: Attributes can be re-ordered by drag-and-drop
