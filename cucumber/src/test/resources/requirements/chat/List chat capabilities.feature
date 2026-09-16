Feature: List chat capabilities

  Background:
    Given a default KB is opened

  @single
  Scenario: The user should be able to see what chat capabilities are available
    Given cases are set up as follows:
      | Case name | attribute name | value | comment    | condition    |
      | Case1     | x              | 1     | Comment 1. | x is in case |
    And I start the client application
    And I see the case Case1 as the current case
    And the chatbot has asked if I want to add, remove or replace a comment
    When I ask what capabilities are available
    Then the capabilities shown include:
      | add           |
      | remove        |
      | replace       |
      | review        |
      | suggested     |
      | undo          |
      | reorder       |
      | cancel        |
      | import        |
      | export        |
      | ZIP           |
      | demonstration |
      | copy          |
      | description   |
      | definition    |
      | favourites    |
      | reasons       |
    And the capabilities are shown in a formatted card
    And pause
