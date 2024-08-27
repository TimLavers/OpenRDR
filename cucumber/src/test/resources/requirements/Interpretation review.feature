Feature: Reviewing the interpretation of a case

  Scenario: The user should be able to see the interpretation of a case
    Given case Bondi is provided having data:
      | Wave | excellent |
    And the interpretation of the case Bondi is "Go to Bondi."
    When I start the client application
    Then I should see the case Bondi as the current case
    And the interpretation should contain the text "Go to Bondi."

  Scenario: The user should be able to see different interpretations for different cases
    Given case Bondi is provided having data:
      | Wave | excellent |
    And case Manly is provided having data:
      | Swimming | excellent |
    And the interpretation of the case Bondi includes "Go to Bondi." because of condition "Wave is in case"
    And the interpretation of the case Manly includes "Go to Manly." because of condition "Swimming is in case"
    And I start the client application
    And I should see the case Bondi as the current case
    And the interpretation should contain the text "Go to Bondi."
    When I select the case Manly
    Then the interpretation should contain the text "Go to Manly."



