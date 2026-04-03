Feature: Navigate cornerstones via chat

  @single
  Scenario: The user should be able to navigate to the next cornerstone case via chat
    Given cases are set up as follows:
      | Case name | attribute name | value | comment    | condition    |
      | Case1     | x              | 1     | Comment 1. | x is in case |
      | Case2     | x              | 2     | Comment 2. | x is in case |
      | Case3     | x              | 3     | Comment 3. | x is in case |
    And I start the client application
    And I see the case Case1 as the current case
    And I request that the comment "Comment 4." be added
    And pause
    And the case Case2 is shown as the cornerstone case
    And the chatbot has asked if I want to provide any reasons and I decline
    When I ask the chatbot to show the next cornerstone case
    Then the case Case3 is shown as the cornerstone case
    And the chatbot has mentioned the cornerstone case "Case3"

  Scenario: The user should be able to navigate to the previous cornerstone case via chat
    Given cases are set up as follows:
      | Case name | attribute name | value | comment    | condition    |
      | Case1     | x              | 1     | Comment 1. | x is in case |
      | Case2     | x              | 2     | Comment 2. | x is in case |
      | Case3     | x              | 3     | Comment 3. | x is in case |
    And I start the client application
    And I see the case Case1 as the current case
    And I request that the comment "Comment 4." be added
    And the case Case2 is shown as the cornerstone case
    And the chatbot has asked if I want to provide any reasons and I decline
    And I ask the chatbot to show the next cornerstone case
    And the case Case3 is shown as the cornerstone case
    When I ask the chatbot to show the previous cornerstone case
    Then the case Case2 is shown as the cornerstone case
    And the chatbot has mentioned the cornerstone case "Case2"

  Scenario: The user should be able to navigate forward through all cornerstone cases and allow them
    Given cases are set up as follows:
      | Case name | attribute name | value | comment    | condition    |
      | Case1     | x              | 1     | Comment 1. | x is in case |
      | Case2     | x              | 2     | Comment 2. | x is in case |
      | Case3     | x              | 3     | Comment 3. | x is in case |
    And I start the client application
    And I see the case Case1 as the current case
    And I request that the comment "Comment 4." be added
    And the case Case2 is shown as the cornerstone case
    And the chatbot has asked if I want to provide any reasons and I decline
    And I ask the chatbot to show the next cornerstone case
    And the case Case3 is shown as the cornerstone case
    And the chatbot has asked if want to allow the report change to cornerstone case "Case3" and I confirm
    When the chatbot has asked if want to allow the report change to cornerstone case "Case2" and I confirm
    Then there are no cornerstone cases showing
    And the chatbot has completed the action
    And the report should be "Comment 1. Comment 2. Comment 3. Comment 4."

  Scenario: The chatbot should indicate which cornerstone case is being shown
    Given cases are set up as follows:
      | Case name | attribute name | value | comment    | condition    |
      | Case1     | x              | 1     | Comment 1. | x is in case |
      | Case2     | x              | 2     | Comment 2. | x is in case |
      | Case3     | x              | 3     | Comment 3. | x is in case |
    And I start the client application
    And I see the case Case1 as the current case
    And I request that the comment "Comment 4." be added
    And the case Case2 is shown as the cornerstone case
    And the chatbot has asked if I want to provide any reasons and I decline
    And the chatbot has mentioned the cornerstone case "Case2"
    When I ask the chatbot to show the next cornerstone case
    Then the chatbot has mentioned the cornerstone case "Case3"
