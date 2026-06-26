@delay_after_cuke
Feature: Help for comments with variables

  The UI is almost entirely chat based, so the user is told - gently and only once per session - that a
  comment can include a case attribute value by delimiting the attribute name with braces, e.g. {TSH}.
  The first time the user adds a comment in a session, the chatbot includes a short tip; it must not
  repeat that tip for subsequent comments in the same session.

  Scenario: The chatbot mentions the variable facility the first time a comment is added
    Given case Bondi is provided having data:
      | Wave | excellent |
    And I start the client application
    And I see the case Bondi as the current case
    And the report is empty
    When I request that the comment "The surf is good" be added
    Then the chatbot mentions that a case value can be inserted into a comment using braces

  Scenario: The chatbot mentions the variable facility at most once per session
    Given case Bondi is provided having data:
      | Wave | excellent |
    And I start the client application
    And I see the case Bondi as the current case
    And the report is empty
    And I build a rule to add the comment "The surf is good"
    When I build a rule to add another comment for the same case "The water is cold"
    Then the chatbot has mentioned the comment variable facility exactly once

