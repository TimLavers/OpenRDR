Feature: Open and close Knowledge Bases using the chat interface
  @chat
  Scenario: The user should be able to use the chat to get a list of the available Knowledge Bases.
    Given A Knowledge Base called B has been created
    And A Knowledge Base called C has been created
    And A Knowledge Base called A has been created
    And I start the client application
    And the chat is showing
    And I enter the following text into the chat panel:
      | What KBs are available? |
    Then the chatbot response consists of the following lines:
      | A        |
      | B        |
      | C        |
      | Thyroids |
    And stop the client application

  @chat
    @single
  Scenario: The user should be able to use the chat to open a Knowledge Base.
    Given A Knowledge Base called A has been created
    And a new case with the name CaseA1 is stored in the Knowledge Base A
    And A Knowledge Base called B has been created
    And a new case with the name CaseB1 is stored in the Knowledge Base B
    And I start the client application
    And the chat is showing
    And I enter the following text into the chat panel:
      | Please open A |
    Then the chatbot response contains the following phrase:
      | opened |
    Then the displayed KB name is now A
    Then I should see the case CaseA1 as the current case
    And I enter the following text into the chat panel:
      | Please open b |
    Then the chatbot response contains the following phrase:
      | opened |
#    Then the displayed KB name is now B
    Then I should see the case CaseB1 as the current case
    And stop the client application

