@delay_after_cuke
Feature: Build rules using non-English languages

  Scenario: The user should be able to use the chat to add a comment with a valid condition in French
    Given case Lindsay is provided with the following values, reference ranges and units:
      | Attribute | Value | Low | High | Units  |
      | Glucose   | 5.2   |     | 5.1  | mmol/L |
      | Pregnant  | Y     |     |      |        |
      | Age       | 21    |     |      |        |
    And I start the client application
    And I see the case Lindsay as the current case
    And I request that the comment "La patiente présente un diabète gestationnel." be added
    When I provide only the following reasons:
      | Le taux de Glucose est élevé |
      | Moins de 50 ans              |
      | pregnant est "Y"             |
    Then the report should be "La patiente présente un diabète gestationnel."
    And the condition showing for the comment "La patiente présente un diabète gestationnel." is:
      | Glucose is high |
      | Age < 50        |
      | Pregnant is "Y" |

  Scenario: The user should be able to use the chat to add a comment with a valid condition in Spanish
    Given case Lindsay is provided with the following values, reference ranges and units:
      | Attribute | Value | Low | High | Units  |
      | Glucose   | 5.2   |     | 5.1  | mmol/L |
      | Pregnant  | Y     |     |      |        |
      | Age       | 21    |     |      |        |
    And I start the client application
    And I see the case Lindsay as the current case
    And I request that the comment "La paciente presenta diabetes gestacional." be added
    When I provide only the following reasons:
      | El nivel de Glucose es alto |
      | Menos de 50 años            |
      | pregnant es "Y"             |
    Then the report should be "La paciente presenta diabetes gestacional."
    And the condition showing for the comment "La paciente presenta diabetes gestacional." is:
      | Glucose is high |
      | Age < 50        |
      | Pregnant is "Y" |
