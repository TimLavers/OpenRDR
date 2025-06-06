Feature: Knowledge Base management

  Scenario: Name of current Knowledge Base should be displayed
    Given A Knowledge Base called Thyroids has been created
    And I start the client application
    Then the displayed KB name is now Thyroids
    And stop the client application

  @single
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

  Scenario: A Knowledge Base can be created
    Given A Knowledge Base called Thyroids has been created
    And I start the client application
    And the displayed KB name is Thyroids
    When I create a Knowledge Base with the name Glucose
    Then the displayed KB name is now Glucose
    And stop the client application

  Scenario: Available Knowledge Bases are displayed
    Given A Knowledge Base called B has been created
    And A Knowledge Base called C has been created
    And A Knowledge Base called A has been created
    And I start the client application
    And I activate the KB management control
    Then pause for 5 seconds
    Then I should see this list of available KBs:
      | A |
      | B |
      | C |
      | Thyroids |
    And stop the client application

  Scenario: An existing Knowledge Base can be opened
    Given A Knowledge Base called Stuff has been created
    And I start the client application
    Then I select the Knowledge Base named Stuff
    Then the displayed KB name is now Stuff
    And stop the client application

  Scenario: The description for a KB can be edited
    Given A Knowledge Base called Irons has been created
    And A Knowledge Base called Glucose has been created
    And I start the client application
    And I select the Knowledge Base named Glucose
    Then the KB description is:
    """
    """
    Given I set the KB description to:
    """
# Glucose
A basic Glucose management KB
See: https://glucose.rules.info/basic
    """
    Then the KB description is:
    """
# Glucose
A basic Glucose management KB
See: https://glucose.rules.info/basic
    """
    And I select the Knowledge Base named Irons
    And pause for 5 seconds
    Then the KB description is:
    """
    """
    And I select the Knowledge Base named Glucose
    And pause for 5 seconds
    Then the KB description is:
    """
# Glucose
A basic Glucose management KB
See: https://glucose.rules.info/basic
    """
    And stop the client application

