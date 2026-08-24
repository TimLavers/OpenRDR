Feature: The user should be able to determine the order of comments in a report

  Scenario: When building a rule to append a comment to the report, the comment should appear at the end of the report
    Given case Manly is provided having data:
      | Wave | good |
      | Sun  | warm |
    And case Bondi is provided having data:
      | Wave | excellent |
    And I start the client application
    And I select the case Manly
    And  I build a rule to add the comment "Go to the beach."
    When I build another rule to add the comment "And bring your flippers."
    Then the interpretation should contain the text "Go to the beach. And bring your flippers."
    And I select the case Bondi
    And the interpretation should contain the text "Go to the beach. And bring your flippers."
