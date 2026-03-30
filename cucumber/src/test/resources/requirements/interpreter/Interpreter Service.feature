Feature: KB available as API endpoint

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

  Scenario: The comments given for a case are returned by the interpretation service
    Given I start the client application
    And I create a Knowledge Base with the name Glucose
    And case Case1 for KB Glucose is provided having data:
        | Age     | 34  |
        | Sex     | M   |
        | Glucose | 3.8 |
    And I select case Case1
    When I build a rule to add the comment "Normal glucose results." with condition
      | Glucose ≤ 5.5 |
    Then case Case2 for KB Glucose gets the interpretation "Normal glucose results." when it is provided having data:
      | Age     | 51  |
      | Sex     | F   |
      | Glucose | 4.8 |



