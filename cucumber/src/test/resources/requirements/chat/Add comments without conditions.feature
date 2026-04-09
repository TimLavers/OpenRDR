@delay_after_cuke
Feature: Add comments without conditions
  Scenario: The user should be able to use the chat to add a comment to a blank report, with no conditions
    Given case Bondi is provided having data:
      | Wave | excellent |
      | Sun  | hot       |
    And I start the client application
    And I see the case Bondi as the current case
    And the report is empty
    When  I build a rule to add the comment "Bring flippers."
    Then the report should be "Bring flippers."

  Scenario: The user should be able to use the chat to add two comments with no conditions
    Given case Bondi is provided having data:
      | Wave | excellent |
      | Sun  | hot       |
    And I start the client application
    And I see the case Bondi as the current case
    And the report is empty
    When  I build a rule to add the comment "Bring flippers."
    Then the report should be "Bring flippers."

  Scenario: Building a rule should create a cornerstone copy of the processed case
    Given case Bondi is provided having data:
      | Wave | excellent |
      | Sun  | hot       |
    And I start the client application
    And I see the case Bondi as the current case
    And the report is empty
    When I build a rule to add the comment "Bring flippers."
    Then the report should be "Bring flippers."
    And the processed case list should contain:
      | Bondi |
    And the cornerstone case list should contain:
      | Bondi |

  Scenario: The user should be able to use the chat to add comments with no conditions to two cases
    Given a list of cases with the following names is stored on the server:
      | Case1 |
      | Case2 |
    And I start the client application
    And I should see the case Case1 as the current case
    And  I build a rule to add the comment "Let's surf."
    And the report should be "Let's surf."
    And select the case Case2
    And the interpretation should be "Let's surf."
    When I add another comment "Bring flippers.", allowing the report change to the cornerstone case
    Then the report should be "Let's surf. Bring flippers."
