Feature: Comment tooltips contain the conditions so far while building a rule

  Background:
    Given a default KB is opened

  Scenario: The tooltip for an added comment contains the conditions during rule building
    Given case Bondi is provided having data:
      | Sun  | hot       |
      | Wave | excellent |
    And I start the client application
    And I request that the comment "Go to the beach." be added
    When I provide the following reasons:
      | Sun is in case |
    And the chatbot has asked if I want to provide any more reasons
    Then the backdoor tooltip for the comment "Go to the beach." has conditions:
      | Sun is in case |

  Scenario: The tooltip for a removed comment contains the conditions during rule building
    Given case Bondi is provided having data:
      | Sun  | too hot |
      | Wave | poor    |
    And the interpretation of the case Bondi is "Go to the beach."
    And I start the client application
    And I select case Bondi
    And I start to build a rule to remove the comment "Go to the beach."
    When I provide the following reasons:
      | Sun is in case |
    And the chatbot has asked if I want to provide any more reasons
    Then the backdoor tooltip for the comment "Go to the beach." has conditions:
      | Sun is in case |

  Scenario: The tooltip for a replaced comment contains the conditions during rule building
    Given case Bondi is provided having data:
      | Sun  | hot       |
      | Wave | excellent |
    And the interpretation of the case Bondi is "Go to Bondi."
    And I start the client application
    And I select case Bondi
    And I start to build a rule to replace the comment "Go to Bondi." by "Go to Manly."
    When I provide the following reasons:
      | Sun is in case |
    And the chatbot has asked if I want to provide any more reasons
    Then the backdoor tooltip for the comment "Go to Manly." has conditions:
      | Sun is in case |
