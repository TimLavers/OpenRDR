Feature: The user should be able to see the conditions that are used to determine each comment in the interpretation

  Scenario: The user should be able to view the conditions for a comment
    Given case Bondi is provided having data:
      | Wave | excellent |
      | UV   | high      |
    And the interpretation of the case Bondi includes "Go to Bondi." because of condition "Wave is not blank"
    And the interpretation of the case Bondi includes "Wear sunscreen." because of condition "UV is not blank"
    When I start the client application
    And I should see the case Bondi as the current case
    Then the interpretation should contain the text "Go to Bondi. Wear sunscreen."
    And I should see the condition for each comment as follows:
      | Comment         | Condition         |
      | Go to Bondi.    | Wave is not blank |
      | Wear sunscreen. | UV is not blank   |
