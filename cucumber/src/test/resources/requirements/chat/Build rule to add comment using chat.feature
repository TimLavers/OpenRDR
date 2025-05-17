Feature: The user can use the chat to make changes the interpretive report

  Scenario: The user should be able to use the chat to add a comment with no conditions
    Given case Bondi is provided having data:
      | Wave | excellent |
      | Sun  | hot       |
    And I start the client application
    And I see the case Bondi as the current case
    And the report is empty
    And the chatbot has asked if I want to add a comment
    And I enter the into the chat panel the text:
      | The report should say "Let's surf" |
    And the chatbot has asked for confirmation
    When I enter the into the chat panel the text:
      | "yes" |
    Then the report should be "Let's surf"
    And stop the client application
