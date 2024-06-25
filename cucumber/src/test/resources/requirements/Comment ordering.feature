Feature: The user should be able to determine the order of comments in a report

  Scenario: When building a rule to append a comment to the report, the comment should appear at the end of the report
    Given a list of cases with the following names is stored on the server:
      | Case1 |
      | Case2 |
    Given I start the client application
    And I build a rule to add the comment "Go to the beach." for case Case1
    When I build another rule to append the comment "And bring your flippers."
    Then the interpretation field should contain the text "Go to the beach. And bring your flippers."
    And the changes badge indicates that there is no change
    And I select the case Case2
    And the interpretation field should contain the text "Go to the beach. And bring your flippers."
    And stop the client application
