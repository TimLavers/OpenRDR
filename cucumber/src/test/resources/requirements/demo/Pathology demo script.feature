Feature: Demo script — derived attributes, repeat inferencing and the AI report
  This scenario is the step-by-step script for demo scenario 3 in
  packaging/README-demo.txt, run against the same data as the seeded "Taylor"
  case in the Demo KB. A passing run means the demo is solid.

  It shows, in order:
  - the Derived attributes panel empty state (feature discoverability),
  - a formula-based derived attribute (BMI),
  - a rule-based derived attribute (Diabetes status), with the suggested
  conditions led by the out-of-range HbA1c,
  - comments conditioned on the derived values (repeat inferencing), one
  of them quoting the BMI value via a comment variable,
  - the AI report turning the clipped, fragmentary comments into readable
  prose and consolidating their separate follow-ups into one recommendation.

  The comments are deliberately terse. A rule's comment is written once and
  reused verbatim on every case it fires for, so experts write them clipped
  and self-contained; several landing on the one case read as disconnected
  fragments. Re-wording them for this patient, and merging three dangling
  actions into a single plan, is work no individual rule can do, because no
  rule knows what the others concluded.

  @single
  Scenario: BMI and Diabetes status drive comments that the AI integrates into a report
    Given case Taylor is provided with the following values, reference ranges and units:
      | Attribute | Value | Low | High | Units |
      | HbA1c     | 7.8   | 4.0 | 6.0  | %     |
      | Height    | 1.78  |     |      | m     |
      | Weight    | 98    |     |      | kg    |
      | Age       | 54    |     |      |       |
      | Sex       | F     |     |      |       |
    And I start the client application
    And I see the case Taylor as the current case

    # Before any rules are built, the Derived attributes panel shows its
    # empty state, so the audience sees where derived values will appear.
    And the derived attributes panel should show that there are none for the case

    # Act 1 — a formula-based derived attribute.
    And the chatbot has asked if I would like to add a comment
    When I request that the derived attribute "BMI" be added with formula "weight / height ^ 2"
    And I provide only the following reason:
      | Height is in case |
    Then the UI should show the value for derived attribute "BMI" as "30.93"
    And the formula showing for the derived value is "Weight/Height^2"

    # Act 2 — a rule-based derived attribute. The suggested conditions are
    # led by the out-of-range HbA1c (suggestion prioritisation).
    When I request that the derived attribute "Diabetes status" be added with value "diabetic"
    Then the first suggested condition is "HbA1c is high"
    When I provide only the following reason:
      | HbA1c is high |
    Then the UI should show the value for derived attribute "Diabetes status" as "diabetic"

    # Act 3 — repeat inferencing: comments conditioned on the derived
    # values. The first comment quotes the BMI via a comment variable.
    # Each comment is clipped and ends in a dangling action.
    When I request that the comment "Obesity. BMI {BMI}. Weight reduction." be added
    And I provide only the following reason:
      | BMI > 30 |
    Then the interpretation report should be "Obesity. BMI 30.93. Weight reduction."

    When I request that the comment "Diabetic. Dietary review." be added
    And I provide only the following reason:
      | Diabetes status is "diabetic" |

    # A third comment, on the raw out-of-range HbA1c, so that consolidating
    # the follow-ups is visibly non-trivial.
    When I request that the comment "HbA1c above target. Repeat in 3 months." be added
    And I provide only the following reason:
      | HbA1c is high |

    # Act 4 — the AI report. It must do more than echo the comments: it
    # writes them up as prose and consolidates the three separate actions
    # into a single Recommendation section that appears in no one rule.
    When I click to show the report panel
    Then the report should contain the phrases:
      | 30.93          |
      | diabet         |
      | Recommendation |
      | weight         |
      | dietary        |
      | 3 months       |
    And pause
