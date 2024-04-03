Feature: The user should be able to see the conditions that are used to determine each comment in the interpretation

  @single
  Scenario: The user should be able to view the conditions for a comment
    Given case Bondi is provided having data:
      | Wave | excellent |
      | UV   | high      |
    And the interpretation of the case Bondi includes "Go to Bondi." because of condition "Wave is not blank"
    And the interpretation of the case Bondi includes "Wear sunscreen." because of condition "UV is not blank"
    And I start the client application
    And I should see the case Bondi as the current case
    And the interpretation field should contain the text "Go to Bondi. Wear sunscreen."
    And pause
    When I click on the Comments tab
    Then I should see the conditions for each comment as follows:
      | Comment         | Condition         |
      | Go to Bondi.    | Wave is not blank |
      | Wear sunscreen. | UV is not blank   |
    And stop the client application