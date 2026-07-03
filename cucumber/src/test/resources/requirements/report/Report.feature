Feature: Report generation by the AI

  @single
  Scenario: An AI-generated report should be displayed if the corresponding panel is visible
    Given a case with name Einstein is stored on the server
    And I start the client application
    And I see the case Einstein as the current case
    And I build a rule to add the comment "MCV value of {mcv} is concerning." with conditions
      | MCV is high    |
      | MCV increasing |
    And I build another rule to append the comment "Haemoglobin is way too high." with conditions
      | HAEMOGLOBIN >=190 |
    When I click to show the report panel
    Then the report should contain the phrases:
      | MCV         |
      | 100.2       |
      | Haemoglobin |
      | 194         |
