@single
@voice-is-fake
Feature: Voice recognition

  The chat panel supports voice input. Clicking the microphone button
  starts a recording session; clicking it again stops the session and
  the transcribed utterance is inserted into the chat text field. These
  scenarios use a fake voice recognition backend that bypasses the real
  microphone and Gemini transcription, so they exercise the UI wiring
  only — not the audio pipeline or the LLM.

  Scenario: A dictated utterance is inserted into the chat text field
    Given case Bondi is provided having data:
      | Wave | excellent |
      | Sun  | hot       |
    And I start the client application
    And I see the case Bondi as the current case
    And the chatbot has asked if I would like to add a comment
    And I activate the microphone
    And the microphone should be active
    When I dictate "the wave is excellent"
    And I deactivate the microphone
    And the microphone should be inactive
    Then the chat text field should contain "the wave is excellent"

  Scenario: Multiple dictated utterances are concatenated in the chat text field
    Given case Bondi is provided having data:
      | Wave | excellent |
      | Sun  | hot       |
    And I start the client application
    And I see the case Bondi as the current case
    And the chatbot has asked if I would like to add a comment
    When I activate the microphone
    And I dictate "the waves are excellent"
    And I dictate "and the sun is hot"
    And I deactivate the microphone
    Then the chat text field should contain "the waves are excellent and the sun is hot"
