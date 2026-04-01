Feature: While building a rule, hovering over the rule action in the interpretation panel should show the conditions so far

  Scenario: Should show conditions when hovering over an added comment during rule building
    Given case Bondi is provided having data:
      | Sun  | hot       |
      | Wave | excellent |
    And I start the client application
    And I request that the comment "Go to the beach." be added
    When I provide the following reasons:
      | Sun is in case |
    And the chatbot has asked if I want to provide any more reasons
    Then the condition showing for the comment "Go to the beach." is:
      | Sun is in case |

  Scenario: Should show conditions when hovering over a removed comment during rule building
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
    Then the condition showing for the comment "Go to the beach." is:
      | Sun is in case |

  Scenario: Should show conditions when hovering over a replaced comment during rule building
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
    Then the condition showing for the comment "Go to Manly." is:
      | Sun is in case |
