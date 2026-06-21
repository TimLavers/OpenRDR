Feature: Add comments with variables

  @@single
  Scenario: The user should be able to use the chat to add a comment with a single variable
    Given case Bondi is provided having data:
      | Wave | excellent |
      | Sun  | hot       |
    And I start the client application
    And I see the case Bondi as the current case
    And the report is empty
    When  I build a rule to add the comment "The wave is {wave}"
    Then the report should be "The wave is excellent"

  Scenario: The user should be able to use the chat to add a comment with multiple variables
    Given case Bondi is provided having data:
      | Wave | excellent |
      | Sun  | hot       |
    And I start the client application
    And I see the case Bondi as the current case
    And the report is empty
    When  I build a rule to add the comment "The wave is {wave} and the sun is {sun}"
    Then the report should be "The wave is excellent and the sun is hot"

  Scenario: The user should be able to use the chat to add a comment with a variable when the attribute has no value
    Given case Bondi is provided having data:
      | Wave | excellent |
    And I start the client application
    And I see the case Bondi as the current case
    And the report is empty
    When  I build a rule to add the comment "The wave is {wave} and the sun is {sun}"
    Then the report should contain "The wave is excellent"
    And the report should contain "sun" as an unresolved marker

  Scenario: Building a rule with variables should create a cornerstone copy of the processed case
    Given case Bondi is provided having data:
      | Wave | excellent |
      | Sun  | hot       |
    And I start the client application
    And I see the case Bondi as the current case
    And the report is empty
    When I build a rule to add the comment "The wave is {wave} and the sun is {sun}"
    Then the report should be "The wave is excellent and the sun is hot"
    And the processed case list should contain:
      | Bondi |
    And the cornerstone case list should contain:
      | Bondi |

  Scenario: The user should be able to use the chat to add comments with variables to two cases
    Given a list of cases with the following names is stored on the server:
      | Case1 |
      | Case2 |
    And I start the client application
    And I should see the case Case1 as the current case
    And I build a rule to add the comment "The wave is {wave} and the sun is {sun}"
    And the report should be "The wave is excellent and the sun is hot"
    And select the case Case2
    And the interpretation should be "The wave is excellent and the sun is hot"
    When I add another comment "Bring flippers.", allowing the report change to the cornerstone case
    Then the report should be "The wave is excellent and the sun is hot. Bring flippers."
