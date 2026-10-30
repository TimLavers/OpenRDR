@file-dialogs-are-fake @ai_evaluation_export
Feature: Preserve observed AI classifications through export and import
  This separate live-model scenario follows the AI authoring and demonstration work.
  Exact saved provenance, including timestamp and model version, survives the archive.
  Server unit tests additionally prohibit any classifier call when reusing imported records.

  Scenario: Imported AI values drive the same comments with the same saved provenance
    Given a default KB is opened
    And case Export01 is provided having data:
      | clinical_notes | T2 Diab |
    And case Export02 is provided having data:
      | clinical_notes | FH diab |
    And I start the client application
    And I select the case Export01
    And the chatbot has asked if I would like to add a comment
    When I enter the following text into the chat panel:
      """
      Create an AI-evaluated Boolean derived attribute called hasDiabetes from
      clinical_notes. True means clear affirmative personal type 1 or type 2
      diabetes. Family history alone, suspected diabetes and gestational diabetes
      alone are false. Do not infer diabetes from medication or tests alone.
      Evaluate only the latest episode; missing or blank notes assign no value.
      Examples: "FH diab" -> false; "?diabetes" -> false; "gest diabetes" -> false;
      "Diabetic" -> true; "T2 Diab" -> true; "T1 diab" -> true.
      Show the definition and preview for acceptance.
      """
    Then the chatbot response contains the following terms:
      | hasDiabetes             |
      | Preview                 |
      | true                    |
      | Accept this definition? |
    When I confirm
    And the chatbot has asked if I want to provide any reasons and I decline
    Then the chatbot has completed the action
    When I request that the comment "Personal diabetes history documented." be added
    And I provide only the following reason:
      | hasDiabetes is "true" |
    Then the interpretation report should be "Personal diabetes history documented."
    When I remember the saved AI provenance for "hasDiabetes"
    And I select the case Export02
    Then the UI should show the value for derived attribute "hasDiabetes" as "false"
    And the interpretation report should be ""
    When I remember the saved AI provenance for "hasDiabetes"
    And the file chooser will select an export destination
    And I enter the following text into the chat panel:
      | Export this KB |
    Then the chat history contains "Exported \"Thyroids\" to"
    And the exported archive contains a knowledge base

    Given the file chooser will select the configured KB archive Whatever
    When I enter the following text into the chat panel:
      | Import a KB |
    Then the chat history contains "Imported \"Whatever\" and opened it."
    When I enter the following text into the chat panel:
      | Delete the knowledge base Thyroids |
    And I enter the following text into the chat panel:
      | yes |
    Then the chatbot response contains the following terms:
      | Deleted | Thyroids |
    Given the file chooser will select the previously exported KB archive
    When I enter the following text into the chat panel:
      | Import a KB |
    Then the chat history contains "Imported \"Thyroids\" and opened it."
    When I select the case Export01
    Then the UI should show the value for derived attribute "hasDiabetes" as "true"
    And the interpretation report should be "Personal diabetes history documented."
    And the saved AI provenance for "hasDiabetes" is unchanged
    When I select the case Export02
    Then the UI should show the value for derived attribute "hasDiabetes" as "false"
    And the interpretation report should be ""
    And the saved AI provenance for "hasDiabetes" is unchanged
