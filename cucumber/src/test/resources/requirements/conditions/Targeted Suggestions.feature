Feature: Suggested conditions should be targeted to the rule action.
#@single
  Scenario: When adding a comment to a case with many attributes, the user should be able to add a condition from a large list of suggestions.
    Given a case with name Einstein is stored on the server
    And a cornerstone case with name Planck is stored on the server
    And I start the client application
    And I request that the comment "Abnormal haemoglobin" be added
    And the case Planck is shown as the cornerstone case
    And pause
    When I provide only the following reason:
      | haemoglobin is abnormal |
    Then there are no cornerstone cases showing
    And the interpretation report should be is "Abnormal haemoglobin"
