Feature: Knowledge Base management

#  @single
  Scenario: Name of current Knowledge Base should be displayed
    Given I start the client application
    Then the displayed KB name is now Thyroids
    And stop the client application

#  Scenario: A previously exported Knowledge Base can be imported
#    Given I start the client application
#    And the displayed KB name is Thyroids
#    When I import the configured zipped Knowledge Base Whatever
#    Then the displayed KB name is now Whatever
#    And stop the client application
#
#  Scenario: A Knowledge Base can be exported
#    Given I start the client application
#    And the displayed KB name is Thyroids
#    And I export the current Knowledge Base
#    And there is a file called Thyroids.zip in my downloads directory
#    The new version of chrome shows a popup explaining
#    where the downloads are. If we don't wait for this to
#    disappear, it interferes with the test.
#    And pause for 20 seconds
#    And I import the configured zipped Knowledge Base Whatever
#    And the displayed KB name is Whatever
#    And pause for 1 second
#    When I import the exported Knowledge Base Thyroids
#    Then the displayed KB name is now Thyroids
#    And stop the client application

    @single
  Scenario: A Knowledge Base can be created
    Given I start the client application
    And the displayed KB name is Thyroids
    When I create a Knowledge Base with the name Glucose
#    Then the displayed KB name is now Glucose
    And stop the client application

  Scenario: Available Knowledge Bases are displayed
    Given A Knowledge Base called 'B' has been created
    And A Knowledge Base called 'C' has been created
    And A Knowledge Base called 'A' has been created
    And I start the client application
    And I activate the KB management control
    Then pause for 5 seconds
    Then I should see this list of available KBs:
      | A |
      | B |
      | C |
    And stop the client application
