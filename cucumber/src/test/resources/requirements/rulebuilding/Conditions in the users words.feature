@delay_after_cuke
Feature: Conditions are shown in the user's own words

  Background:
    Given a default KB is opened
    And case GlucoseCase is provided with the following values, reference ranges and units:
      | Attribute | Value | Low | High | Units  |
      | Glucose   | 12    | 3   | 6    | mmol/L |
    And I start the client application
    And I see the case GlucoseCase as the current case

  Scenario: A comment tooltip shows the user's phrase alongside the formal condition
    Given I request that the comment "Review glucose." be added
    When I provide only the following reason:
      | elevated glucose |
    Then the report should be "Review glucose."
    And the condition showing for the comment "Review glucose." is:
      | Glucose is high |
    And the condition phrases shown should be:
      | elevated glucose |

  Scenario: Reusing a condition keeps its first phrase and tells the user
    Given I request that the comment "Review glucose." be added
    And I provide only the following reason:
      | elevated glucose |
    And the report should be "Review glucose."
    And I request that the comment "Repeat glucose measurement." be added
    When I provide only the following reason:
      | raised glucose |
    Then the chat history contains "previously called this 'elevated glucose'"
    And the condition showing for the comment "Repeat glucose measurement." is:
      | Glucose is high |
    And the condition phrases shown should be:
      | elevated glucose |

  Scenario: Renaming a shared condition updates both comment tooltips
    Given I request that the comment "Review glucose." be added
    And I provide only the following reason:
      | elevated glucose |
    And the report should be "Review glucose."
    And I request that the comment "Repeat glucose measurement." be added
    And I provide only the following reason:
      | elevated glucose |
    And the condition showing for the comment "Review glucose." is:
      | Glucose is high |
    And the condition phrases shown should be:
      | elevated glucose |
    And the condition showing for the comment "Repeat glucose measurement." is:
      | Glucose is high |
    And the condition phrases shown should be:
      | elevated glucose |
    When I enter the following text into the chat panel:
      """
      call the condition "Glucose is high" "raised glucose"
      """
    Then the chatbot has completed the action
    And the condition showing for the comment "Review glucose." is:
      | Glucose is high |
    And the condition phrases shown should be:
      | raised glucose |
    And the condition showing for the comment "Repeat glucose measurement." is:
      | Glucose is high |
    And the condition phrases shown should be:
      | raised glucose |
