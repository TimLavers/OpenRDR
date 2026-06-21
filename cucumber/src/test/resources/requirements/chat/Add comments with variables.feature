Feature: Add comments with variables

  Scenario: The user should be able to use the chat to add a comment with a single variable
    Given case Bondi is provided having data:
      | Wave | excellent |
      | Sun  | hot       |
    And I start the client application
    And I see the case Bondi as the current case
    And the report is empty
    When  I build a rule to add the comment "The wave quality is {wave}"
    Then the report should be "The wave quality is excellent"

  Scenario: The user should be able to use the chat to add a comment with multiple variables
    Given case Bondi is provided having data:
      | Wave | excellent |
      | Sun  | hot       |
    And I start the client application
    And I see the case Bondi as the current case
    When  I build a rule to add the comment "The wave quality is {wave} and the air temperature is {sun}"
    Then the report should be "The wave quality is excellent and the air temperature is hot"

  @single
  Scenario: Variables in a comment should be re-evaluated when the selected case changes
    Given case Bondi is provided having data:
      | Wave | excellent |
      | Sun  | hot       |
    And case Malabar is provided having data:
      | Wave | non-existent |
      | Sun  | scorching    |
    And I start the client application
    And I see the case Bondi as the current case
    And  I build a rule to add the comment "The wave quality is {wave} and the air temperature is {sun}"
    When I select the case Malabar
    Then the report should be "The wave quality is non-existent and the air temperature is scorching"
    And pause

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
