Feature: Creation of cornerstone cases when rules are built
  @single
  Scenario: Only one copy of a case is stored as a cornerstone
    Given case Case1 is provided having data:
      | x | 1 |
      | y | 1 |
    And case Case2 is provided having data:
      | x | 2 |
      | y | 2 |
    And I start the client application
    And I should see the case Case1 as the current case
    And I build a rule to add the comment "Go to the beach." with condition
      | x > 0 |
    Then the interpretation should be "Go to the beach."
    And pause for 2 seconds
    And the cornerstone case count should be 1
    And I build another rule to append the comment "Shoot some tubes." with condition
      | y > 0 |
    Then the interpretation should be "Go to the beach. Shoot some tubes."

    And pause for 60 seconds



