@delay_after_cuke
Feature: Rule action with reasons
  The user can give the report change and its reasons in one instruction, for example
  'Add the comment "Let's surf." reason "wave height is more than 0.5"'. The rule session starts, the reasons are
  added as conditions, and the chat asks whether there are more reasons, exactly as if the reasons had been typed one
  by one. See documentation/design/chat_architecture.md.

  Background:
    Given a default KB is opened

  Scenario: Add a comment and its reason in one instruction
    Given case Bondi is provided having data:
      | wave height | 2 |
    And I start the client application
    And I see the case Bondi as the current case
    When I request that the comment "Let's surf." be added for the reasons:
      | wave height is more than 0.5 |
    And the chatbot has asked if I want to provide any more reasons and I decline
    Then the report should be "Let's surf."
    And the condition showing for the comment "Let's surf." is:
      | wave height > 0.5 |

  Scenario: Add a comment with several reasons in one instruction
    Given case Bondi is provided having data:
      | wave height | 2   |
      | UV          | 7.0 |
    And I start the client application
    And I see the case Bondi as the current case
    When I request that the comment "Let's surf." be added for the reasons:
      | wave height is more than 0.5 |
      | UV is less than 10           |
    And the chatbot has asked if I want to provide any more reasons and I decline
    Then the report should be "Let's surf."
    And the conditions showing for the comment "Let's surf." are:
      | wave height > 0.5 |
      | UV < 10.0         |

  Scenario: Replace a comment and give the reason in one instruction
    Given case Bondi is provided having data:
      | wave height | 2   |
      | UV          | 7.0 |
    And the interpretation of the case Bondi consists of the following comments:
      | Go to the beach. |
      | Bring flippers.  |
    And I start the client application
    And I see the case Bondi as the current case
    When I request that the comment "Bring flippers." be replaced by "Don't forget sunscreen." for the reasons:
      | UV is more than 5 |
    And the chatbot has asked if I want to provide any more reasons and I decline
    Then the report should be "Go to the beach. Don't forget sunscreen."

  Scenario: Assign a derived value and give the reasons in one instruction
    Given case Fermi is provided with the following values, reference ranges and units:
      | Attribute | Value | Low | High | Units |
      | Height    | 1.72  |     | 2.5  | m     |
      | Weight    | 65    |     | 100  | kg    |
    And I start the client application
    And the chatbot has asked if I would like to add a comment
    When I request that the derived attribute "bmi" be added with formula "weight/height^2" for the reasons:
      | Weight is in case |
      | Height is in case |
    And the chatbot has asked if I want to provide any more reasons and I decline
    Then the UI should show the value for derived attribute "bmi" as "21.97"
    And the UI should show the following conditions for the derived value "bmi":
      | Weight is in case |
      | Height is in case |

  Scenario: A reason that cannot be understood does not stop the others
    Given case Bondi is provided having data:
      | wave height | 2 |
    And I start the client application
    And I see the case Bondi as the current case
    When I request that the comment "Let's surf." be added for the reasons:
      | wave height is more than 0.5 |
      | the sun is hot               |
    Then the chatbot indicates that a reason could not be understood
    And the chatbot has asked if I want to provide any more reasons and I decline
    And the report should be "Let's surf."
    And the condition showing for the comment "Let's surf." is:
      | wave height > 0.5 |
