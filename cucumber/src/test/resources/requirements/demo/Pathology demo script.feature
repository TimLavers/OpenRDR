Feature: Demo script — derived attributes, repeat inferencing and the AI report
  This scenario is the step-by-step script for demo scenario 3 in
  packaging/README-demo.txt, run against the same data as the seeded "Sam"
  case in the Demo KB. A passing run means the demo is solid.

  It shows, in order:
  - the Derived attributes panel empty state (feature discoverability),
  - a formula-based derived attribute (BMI),
  - a rule-based derived attribute (Diabetes status), with the suggested
  conditions led by the out-of-range HbA1c,
  - comments conditioned on the derived values (repeat inferencing), one
  of them quoting the BMI value via a comment variable,
  - the AI report integrating the stilted comments into a readable report.

  Scenario: BMI and Diabetes status drive comments that the AI integrates into a report
    Given case Sam is provided with the following values, reference ranges and units:
      | Attribute | Value | Low | High | Units |
      | HbA1c     | 7.8   | 4.0 | 6.0  | %     |
      | Height    | 1.78  |     |      | m     |
      | Weight    | 98    |     |      | kg    |
      | Age       | 54    |     |      |       |
      | Sex       | M     |     |      |       |
    And I start the client application
    And I see the case Sam as the current case

    # Before any rules are built, the Derived attributes panel shows its
    # empty state, so the audience sees where derived values will appear.
    And the derived attributes panel should show that there are none for the case

    # Act 1 — a formula-based derived attribute.
    And the chatbot has asked if I would like to add a comment
    When I request that the derived attribute "BMI" be added with formula "weight / (height * height)"
    And I provide only the following reason:
      | Height is in case |
    Then the UI should show the value for derived attribute "BMI" as "30.93"
    And the formula showing for the derived value is "Weight/(Height*Height)"

    # Act 2 — a rule-based derived attribute. The suggested conditions are
    # led by the out-of-range HbA1c (suggestion prioritisation).
    When I request that the derived attribute "Diabetes status" be added with value "diabetic"
    Then the first suggested condition is "HbA1c is high"
    When I provide only the following reason:
      | HbA1c is high |
    Then the UI should show the value for derived attribute "Diabetes status" as "diabetic"

    # Act 3 — repeat inferencing: comments conditioned on the derived
    # values. The first comment quotes the BMI via a comment variable.
    When I request that the comment "BMI of {BMI} indicates obesity. Weight reduction is advised." be added
    And I provide only the following reason:
      | BMI > 30 |
    Then the interpretation report should be "BMI of 30.93 indicates obesity. Weight reduction is advised."

    When I request that the comment "The patient is diabetic. Dietary review is recommended." be added
    And I provide only the following reason:
      | Diabetes status is "diabetic" |

    # Act 4 — the AI report integrates the two stilted comments.
    When I click to show the report panel
    Then the report should contain the phrases:
      | BMI    |
      | 30.93  |
      | diabet |
