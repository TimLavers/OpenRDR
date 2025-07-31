Feature: KB available as API enpoint

  Scenario: Attributes are created as required from interpreted cases
    Given I start the client application
    And I create a Knowledge Base with the name Glucose
    And case Case1 for KB Glucose is provided having data:
      | Age     | 34  |
      | Sex     | M   |
      | Glucose | 3.8 |
    When I select case Case1
    Then I should see these attributes:
      | Age     |
      | Sex     |
      | Glucose |
    And stop the client application

  Scenario: Attribute names are case sensitive
    Given I start the client application
    And I create a Knowledge Base with the name Glucose
    And case Case1 for KB Glucose is provided having data:
      | Age | 34    |
      | age | 12520 |
    When I select case Case1
    Then I should see these attributes:
      | Age |
      | age |
    And stop the client application

  @single
  Scenario: The comments given for a case are returned by the interpretation service
    Given I start the client application
    And I create a Knowledge Base with the name Glucose
    And case Case1 for KB Glucose is provided having data:
        | Age     | 34  |
        | Sex     | M   |
        | Glucose | 3.8 |
    And I select case Case1
    And I build a rule to add the comment "Normal glucose results." with conditions
        | Glucose ≤ 5.5 | Glucose ≤  | 5.5 |
    And case Case2 for KB Glucose gets the interpretation "Normal glucose results." when it is provided having data:
      | Age     | 51  |
      | Sex     | F   |
      | Glucose | 4.8 |



