Feature: Replace comment

  Scenario: The user should be able to use the chat to replace a comment with a valid condition
    Given case Bondi is provided having data:
      | wave height | 2   |
      | UV          | 7.0 |
    And the interpretation of the case Bondi consists of the following comments:
      | Go to the beach. |
      | Bring flippers.  |
    And I start the client application
    And I see the case Bondi as the current case
    When Replace the comment "Bring flippers." by "Don't forget sunscreen." with the reasons:
      | wave height is more than 0.5 |
    Then the report should be "Go to the beach. Don't forget sunscreen."

