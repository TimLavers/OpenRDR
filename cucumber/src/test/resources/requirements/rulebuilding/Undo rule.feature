Feature: The user can undo rules via the chatbot

  Undoing the last rule is done conversationally: the user asks the chatbot to
  undo, the chatbot previews the rule that would be removed and asks for
  confirmation, and only an explicit affirmation actually performs the undo.

  @single
  Scenario: When a rule is undone, the interpretation of a case changes back to what is was prior to the rule being built
    Given case Bondi is provided having data:
      | Wave | excellent |
      | Sun  | hot       |
      | Tide | low       |
    And I start the client application
    And I see the case Bondi as the current case
    And pause
    And I build a rule to add the comment "Go to Bondi." with conditions
      | Sun is hot |
    And the interpretation should be "Go to Bondi."
    When I ask the chatbot to undo the last rule
    And I confirm the undo
    Then the interpretation should be empty

  Scenario: Initially, there are no rules to undo
    Given case Bondi is provided having data:
      | Wave | excellent |
      | Sun  | hot       |
    And I start the client application
    And I see the case Bondi as the current case
    When I ask the chatbot to undo the last rule
    Then the chatbot says there are no rules to undo

  Scenario: Undo rules from sample KB
    Given I start the client application
    And I create a Knowledge Base with the name ContactLenseUndo based on the "Contact Lense Prescription" sample
    And the count of the number of cases is 24
    And I select case Case24
    And the interpretation should be empty
    When I ask the chatbot to undo the last rule
    And I confirm the undo
    Then the interpretation should be "hard"
