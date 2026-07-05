Feature: Report generation by the AI

  Scenario: An AI-generated report should be displayed if the corresponding panel is visible
    Given a case with name Einstein is stored on the server
    And I start the client application
    And I see the case Einstein as the current case
    And I build a rule to add the comment "MCV value of {mcv} is concerning." with condition
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

  Scenario: Report should update when a different case is selected
    Given case Mary is provided having data:
      | MCV | 202 |
    And case Jane is provided having data:
      | HBA1c | 16 |
    And I start the client application
    And a backdoor rule is built for case Mary to add the comment "MCV is fine." with conditions:
      | MCV is "202" |
    And a backdoor rule is built for case Jane to add the comment "Diabetic" with conditions:
      | HBA1c is "16" |
    And I select the case Mary
    And I click to show the report panel
    And the report contains the phrase:
      | MCV |
    And I select the case Jane
    Then the report should contain the phrase:
      | diabetic |
