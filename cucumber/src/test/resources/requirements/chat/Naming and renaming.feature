# This file specifies the behaviour described in step 14 of
# documentation/design/repeat_inferencing.md.
@delay_after_cuke
Feature: Naming comments, and renaming comments and derived attributes
  Every comment has a name, so that the user can refer to it later. The name is
  assigned by the system (C1, C2, ...) and the user is told it when the comment
  is accepted. Comments and derived attributes can be renamed at any time: a
  name is only a label, since everything refers to an attribute by its identity.
  Attributes that come with the case data cannot be renamed.

  Scenario: The user is told the name of a comment when it is accepted, and that it can be changed
    Given case Bondi is provided having data:
      | wave height | 2 |
    And I start the client application
    And I see the case Bondi as the current case
    When I request that the comment "Let's surf." be added
    Then the chatbot tells me the name of the comment and that it can be renamed

  Scenario: A comment is named C1 by the system and shown with that name
    Given case Bondi is provided having data:
      | wave height | 2 |
    # The rule is built without going through the chat, so the comment is
    # auto-named with the first free "C" name.
    And a rule exists to add the comment "Let's surf." to case Bondi with no conditions
    And I start the client application
    When I see the case Bondi as the current case
    Then the comment "Let's surf." should be shown with the name "C1"

  Scenario: Two comments added in one session are named C1 then C2
    Given case Bondi is provided having data:
      | wave height | 2 |
    And a rule exists to add the comment "Let's surf." to case Bondi with no conditions
    And a rule exists to add the comment "Surf's up." to case Bondi with no conditions
    And I start the client application
    When I see the case Bondi as the current case
    Then the comment "Let's surf." should be shown with the name "C1"
    And the comment "Surf's up." should be shown with the name "C2"

  Scenario: The user can rename a comment through the chat
    Given case Bondi is provided having data:
      | wave height | 2 |
    And I start the client application
    And I see the case Bondi as the current case
    And I build a rule to add the comment "Let's surf."
    And the report should be "Let's surf."
    When I request that the comment just added be renamed to "Surfing advice"
    Then the chatbot confirms a rename to "Surfing advice"
    # Only the name has changed: the comment itself is unaffected.
    And the report should be "Let's surf."

  Scenario: A comment is shown with the name the user gave it in place of the one it was given
    Given case Bondi is provided having data:
      | wave height | 2 |
    And a rule exists to add the comment "Let's surf." to case Bondi with no conditions
    And I start the client application
    And I see the case Bondi as the current case
    And the comment "Let's surf." should be shown with the name "C1"
    When I request that the attribute "C1" be renamed to "Surfing advice"
    Then the chatbot confirms that "C1" has been renamed to "Surfing advice"
    # The comment is unchanged; only the name beside it is different.
    And the comment "Let's surf." should be shown with the name "Surfing advice"

  Scenario: A comment can be renamed while a rule is being built
    Given case Bondi is provided having data:
      | wave height | 2 |
    And I start the client application
    And I see the case Bondi as the current case
    And I request that the comment "Let's surf." be added
    And the chatbot tells me the name of the comment and that it can be renamed
    # Renaming is not rule building, so it needs no rule session and disturbs none.
    When I request that the comment just added be renamed to "Surfing advice"
    Then the chatbot confirms a rename to "Surfing advice"
    # The session is untouched, so the rule can be completed as usual. The condition
    # is added with the step for a session already under way, because the rename is
    # now the most recent message and no new suggestions follow it.
    And I add the condition "wave height is more than 0.5"
    And the chatbot has asked if I want to provide any more reasons and I decline
    And the chatbot has completed the action
    And the report should be "Let's surf."

  Scenario: The user can rename a derived attribute, and the new name is shown for the case
    Given case Fermi is provided having data:
      | weight | 65   |
      | height | 1.72 |
    And I start the client application
    And a backdoor rule is built for case Fermi to assign the formula "weight / (height * height)" to the derived attribute "BMI" with no conditions
    And I select the case Fermi
    And the UI should show the value for derived attribute "BMI" as "21.97"
    When I request that the attribute "BMI" be renamed to "Body mass index"
    Then the chatbot confirms that "BMI" has been renamed to "Body mass index"
    # The value is unaffected: only the name has changed.
    And the UI should show the value for derived attribute "Body mass index" as "21.97"

  Scenario: An attribute that comes with the case data cannot be renamed
    Given case Bondi is provided having data:
      | wave height | 2 |
    And I start the client application
    And I see the case Bondi as the current case
    When I request that the attribute "wave height" be renamed to "Swell"
    Then the chatbot explains that the attribute "wave height" cannot be renamed
    And the case should show the attributes in order:
      | wave height |
