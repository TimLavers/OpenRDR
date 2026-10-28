@ai_diabetes_demo @delay_after_cuke
Feature: Demonstrate AI reading clinical notes and driving a downstream rule
  Synthetic cases only. Both chat authoring and classification use the configured
  real models. This is a visible demonstration with asserted outcomes, not a
  deterministic fixture or a clinical accuracy assessment.
  The six short examples are taught in the definition; the longer notes are not.

  Background:
    Given a default KB is opened

  Scenario: Define diabetes from notes in chat and review ten different cases
    Given case Demo01 is provided having data:
      | age            | 58       |
      | gender         | M        |
      | glucose        | 9.5      |
      | clinical_notes | Diabetic |
    And case Demo02 is provided having data:
      | age            | 64       |
      | gender         | F        |
      | glucose        | 8.2      |
      | clinical_notes | T2 Diab  |
    And case Demo03 is provided having data:
      | age            | 22       |
      | gender         | M        |
      | glucose        | 10.4     |
      | clinical_notes | T1 diab  |
    And case Demo04 is provided having data:
      | age            | 45       |
      | gender         | U        |
      | glucose        | 5.6      |
      | clinical_notes | FH diab  |
    And case Demo05 is provided having data:
      | age            | 70       |
      | gender         | F        |
      | glucose        | 6.2      |
      | clinical_notes | ?diabetes |
    And case Demo06 is provided having data:
      | age            | 31       |
      | gender         | F        |
      | glucose        | 5.9      |
      | clinical_notes | gest diabetes |
    And case Demo07 is provided having data:
      | age            | 72       |
      | gender         | M        |
      | glucose        | 5.2      |
      | clinical_notes | No history of diabetes. Routine hypertension review. |
    And case Demo08 is provided having data:
      | age            | 55       |
      | gender         | M        |
      | glucose        | 8.9      |
      | clinical_notes | Mother has diabetes; patient also has established type 2 diabetes. |
    And case Demo09 is provided having data:
      | age            | 66       |
      | gender         | F        |
      | glucose        | 7.8      |
      | clinical_notes | Follow-up: longstanding T2DM, diagnosed in 2012. Attending annual review. No new symptoms. |
    And case Demo10 is provided having data:
      | age            | 40       |
      | gender         | U        |
      | glucose        | 5.8      |
      | Visit          | Annual review; notes not supplied |
    And I start the client application
    And I select the case Demo01
    And the chatbot has asked if I would like to add a comment

    # Author the meaning, not an algorithm or a literal classification.
    When I enter the following text into the chat panel:
      """
      Create an AI-evaluated Boolean derived attribute called hasDiabetes from
      clinical_notes. Assign true only when the notes clearly affirm that the
      patient personally has diabetes, including type 1 or type 2. Otherwise
      assign false. Family history alone, suspected or questioned diabetes,
      and gestational diabetes alone do not qualify. A family-history mention
      does not negate an independent confirmed personal diagnosis. Do not
      infer diabetes from medication or laboratory results alone. Only evaluate
      the latest episode, and assign no value when notes are missing or blank.

      Preserve these labelled examples exactly:
      "FH diab" -> false
      "?diabetes" -> false
      "gest diabetes" -> false
      "Diabetic" -> true
      "T2 Diab" -> true
      "T1 diab" -> true

      Please show me the definition and preview for this case for acceptance.
      """
    Then the chatbot response contains the following terms:
      | hasDiabetes             |
      | clinical_notes          |
      | Preview                 |
      | true                    |
      | Accept this definition? |
    When I confirm
    And pause
    And the chatbot has asked if I want to provide any reasons and I decline
    Then the chatbot has completed the action
    And the UI should show the value for derived attribute "hasDiabetes" as "true"

    # A normal RDR condition consumes the AI Boolean.
    When I request that the comment "The clinical notes indicate a personal history of diabetes." be added
    And I provide only the following reason:
      | hasDiabetes is "true" |
    Then the interpretation report should be "The clinical notes indicate a personal history of diabetes."

    # Walk the case list visibly. Four seconds per case gives the audience
    # time to compare the note, Boolean, and presence or absence of the comment.
    When I select the case Demo01
    Then the UI should show the value for derived attribute "hasDiabetes" as "true"
    And the interpretation report should be "The clinical notes indicate a personal history of diabetes."
    And pause for 4 seconds

    When I select the case Demo02
    Then the UI should show the value for derived attribute "hasDiabetes" as "true"
    And the interpretation report should be "The clinical notes indicate a personal history of diabetes."
    And pause for 4 seconds

    When I select the case Demo03
    Then the UI should show the value for derived attribute "hasDiabetes" as "true"
    And the interpretation report should be "The clinical notes indicate a personal history of diabetes."
    And pause for 4 seconds

    When I select the case Demo04
    Then the UI should show the value for derived attribute "hasDiabetes" as "false"
    And the interpretation report should be ""
    And pause for 4 seconds

    When I select the case Demo05
    Then the UI should show the value for derived attribute "hasDiabetes" as "false"
    And the interpretation report should be ""
    And pause for 4 seconds

    When I select the case Demo06
    Then the UI should show the value for derived attribute "hasDiabetes" as "false"
    And the interpretation report should be ""
    And pause for 4 seconds

    When I select the case Demo07
    Then the UI should show the value for derived attribute "hasDiabetes" as "false"
    And the interpretation report should be ""
    And pause for 4 seconds

    When I select the case Demo08
    Then the UI should show the value for derived attribute "hasDiabetes" as "true"
    And the interpretation report should be "The clinical notes indicate a personal history of diabetes."
    And pause for 4 seconds

    When I select the case Demo09
    Then the UI should show the value for derived attribute "hasDiabetes" as "true"
    And the interpretation report should be "The clinical notes indicate a personal history of diabetes."
    And pause for 4 seconds

    When I select the case Demo10
    Then the UI should show the value for derived attribute "hasDiabetes" as "Not evaluated: no clinical_notes value"
    And the interpretation report should be ""
    And pause for 4 seconds

    # Revisit a classified case and explain its stored result.
    When I select the case Demo08
    And I enter the following text into the chat panel:
      """
      Explain why hasDiabetes has this value, using its saved evidence and definition.
      """
    Then the chatbot response contains the following terms:
      | hasDiabetes |
      | true        |
      | clinical_notes |
    And pause for 6 seconds

