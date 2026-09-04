Feature: Knowledge Base management

  Background:
    Given there is a knowledge base called Thyroids

  Scenario: Name of current Knowledge Base should be displayed
    Given I start the client application
    Then the displayed KB name is now Thyroids

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

  Scenario: A Knowledge Base can be created
    Given I start the client application
    And the displayed KB name is Thyroids
    When I create a Knowledge Base with the name Glucose
    Then the displayed KB name is now Glucose

  Scenario: Available Knowledge Bases are displayed
    Given A Knowledge Base called B has been created
    And A Knowledge Base called C has been created
    And A Knowledge Base called A has been created
    And I start the client application
    And I activate the KB management control
    # The client opens the first KB by name ("A"). It acts as the dropdown
    # trigger and is therefore excluded from the switcher list, which only
    # offers the *other* available KBs to switch to.
    Then I should see this list of available KBs:
      | B |
      | C |
      | Thyroids |

  Scenario: An existing Knowledge Base can be opened
    Given A Knowledge Base called Stuff has been created
    And I start the client application
    Then I select the Knowledge Base named Stuff
    Then the displayed KB name is now Stuff

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

