@chat
Feature: Move attributes using the chat bot
  Scenario: The user should be able to use the chat to reorder the attributes in the case
    Given case Bondi is provided having data:
      | glucose | 4.5 |
      | sex     | F   |
      | hdl     | 1.7 |
      | age     | 22  |
      | ldl     | 3.8 |
    And I start the client application
    And I see the case Bondi as the current case
    And the chat is showing
    And the chatbot has asked if I would like to add a comment
    And I enter the following text into the chat panel:
      | No. Please move age above glucose |
    And pause for 5 seconds
    Then the case should show the attributes in order:
      | age     |
      | glucose |
      | sex     |
      | hdl     |
      | ldl     |
    And stop the client application

