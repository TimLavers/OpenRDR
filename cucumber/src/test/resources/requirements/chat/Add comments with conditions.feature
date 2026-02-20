@chat
Feature: Add comments with conditions

  Scenario: The user should be able to use the chat to add a comment with a valid condition
    Given case Bondi is provided having data:
      | wave height | 2 |
    And I start the client application
    And I see the case Bondi as the current case
    And the chat is showing
    And I request that the comment "Let's surf." be added
    When I provide only the following reason:
      | wave height is more than 0.5 |
    Then the report should be "Let's surf."
    And the condition showing for the comment "Let's surf." is:
      | wave height > 0.5 |
    And stop the client application

  Scenario: The user should be able to use the chat to add a comment with a valid condition in French
    Given case Lindsay is provided with the following values, reference ranges and units:
      | Attribute | Value | Low | High | Units  |
      | Glucose   | 5.2   |     | 5.1  | mmol/L |
      | Pregnant  | Y     |     |      |        |
      | Age       | 21    |     |      |        |
    And I start the client application
    And I see the case Lindsay as the current case
    And the chat is showing
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
    And stop the client application

  Scenario: The user should be able to see the reason why their condition expression is invalid
    Given case Bondi is provided having data:
      | wave height | 2 |
    And I start the client application
    And I see the case Bondi as the current case
    And the chat is showing
    And I request that the comment "Let's surf." be added
    And the chatbot has asked if I want to provide any reasons and I confirm
    And the chatbot has asked for the first reason
    When I enter the following text into the chat panel:
      | wave height is more than 2 |
    Then the chatbot indicates that this reason is not true for the current case
#    todo check that the rule is not added
    And stop the client application
