Feature: The user can make rules that change the interpretive report

  @single
  Scenario: The user should be able to build a rule to add a comment with no conditions using the chat
    Given case Bondi is provided having data:
      | Wave | excellent |
      | Sun  | hot       |
    And I start the client application
    And I see the case Bondi as the current case
    And the report is empty
    When I enter the into the chat panel the text:
      | The report should say "Let's surf" |
    And pause
    And the chatbot responds with text containing the phrases:
      | Just to confirm              |
      | add the comment "Let's surf" |
    Then the report should be "Let's surf"
    And stop the client application
