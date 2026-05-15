Feature: Phase 1 — Suggested conditions are ranked by the rule action, the cornerstones, and historical conditions, and capped at a usable size.

  # This file specifies the *intended* behaviour after Phase 1 of the
  # targeted-suggested-conditions design (see
  # documentation/design/targeted_suggested_conditions_phase_1.md).
  #
  # Three sets of scenarios:
  #   Set A — Action targeting
  #   Set B — Cornerstone discrimination
  #   Set C — Historical conditions.
  #
  # Plus one limit scenario at the start to verify the cap on the total number of suggestions.

  ##############################################################################
  # Hard cap on number of suggestions
  ##############################################################################

  Scenario: The number of suggestions presented to the user is capped at 20
    # Cap chosen so the list is comfortably scannable end-to-end. Ranking
    # places the best suggestions near the top;
    Given a case with name Einstein is stored on the server
    And I start the client application
    When I request that the comment "Routine review." be added
    Then the number of suggested conditions should be at most 20

  ##############################################################################
  # Set A — Action targeting
  #
  # With no rules and no cornerstones, only the comment-text
  # varies, so ranking changes ONLY when the comment changes.
  ##############################################################################

  @single
  Scenario: When adding a comment, suggestions whose attribute or direction match the comment text rank above unrelated suggestions
    Given a case with name Einstein is stored on the server
    And I start the client application
    And pause
    When I request that the comment "Elevated haemoglobin may be significant." be added
    Then the suggested condition "HAEMOGLOBIN" should appear before "AST"
    And the suggested condition "HAEMOGLOBIN" should appear before "Anion gap"

  Scenario: A different comment changes the ranking accordingly
    Given a case with name Einstein is stored on the server
    And I start the client application
    When I request that the comment "Elevated MCV may be significant." be added
    Then the suggested condition "MCV" should appear before "AST"

  Scenario: When removing a comment, suggestions matching the comment text rank above unrelated suggestions
    Given a case with name Einstein is stored on the server
    And a backdoor rule is built for case Einstein to add the comment "MCV elevated." with conditions:
      | Sex is "M" |
    And I start the client application
    When I request that the following comment be removed:
      | MCV elevated. |
    Then the suggested condition "MCV" should appear before "AST"

  Scenario: When replacing a comment, suggestions matching the REPLACEMENT comment rank above those matching the original
    Given a case with name Einstein is stored on the server
    And a backdoor rule is built for case Einstein to add the comment "Elevated haemoglobin may be significant." with conditions:
      | Sex is "M" |
    And I start the client application
    When I request that the comment be replaced by "macrocytosis MCV."
    Then the suggested condition "MCV" should appear before all of the following suggestions:
      | AST         |
      | HAEMOGLOBIN |

  Scenario: When adding a comment to a case with many attributes, the user should be able to add a condition from a large list of suggestions.
    Given a case with name Einstein is stored on the server
    And a cornerstone case with name Planck is stored on the server
    And I start the client application
    And I request that the comment "Abnormal haemoglobin" be added
    And the case Planck is shown as the cornerstone case
    When I provide only the following reason:
      | haemoglobin is abnormal |
    Then there are no cornerstone cases showing
    And the interpretation report should be is "Abnormal haemoglobin"

  ##############################################################################
  # Set B — Cornerstone discrimination
  #
  # The session comment is deliberately chosen to have NO predicate-vocabulary
  # tokens (i.e. nothing matching the attributes), so comment-overlap is zero for every candidate and ranking is
  # driven entirely by cornerstone discrimination.
  ##############################################################################

  Scenario: A condition that discriminates between the session case and the cornerstone is preferred
    # Waves ≤ 1.5 holds only for Bondi (discrimination = 1).
    # Waves ≥ 1.5 holds for both (discrimination = 0).
    # Sun is "hot" holds for both (discrimination = 0).
    # UV index ≤ 1 and UV index ≥ 1 hold for both (discrimination = 0).
    # The Einstein and Planck cases cannot be used as there are more that 20 discriminatory suggestions
    Given case Bondi is provided having data:
      | Sun      | hot |
      | UV index | 1   |
      | Waves    | 1.5 |
    And cornerstone case Malabar is provided having data:
      | Sun      | hot |
      | UV index | 1   |
      | Waves    | 2.0 |
    And I start the client application
    When I request that the comment "Go to the beach" be added
    And the case Malabar is shown as the cornerstone case
    Then the suggested condition "Waves ≤ 1.5" should appear before all of the following suggestions:
      | Waves ≥ 1.5               |
      | Sun is "hot"              |
      | UV index ≤ 1.0            |
      | UV index ≥ 1.0            |
      | case is for a single date |

  ##############################################################################
  # Set C — Historical conditions
  #
  # Several backdoor rules establish "Elevated haemoglobin may be significant." with the same
  # condition "eGFR ≥ 70" across multiple cases. When the user later adds
  # the SAME comment to Einstein, "eGFR ≥ 70" surfaces high purely on
  # historical signal — even though it has no comment-text overlap and no
  # cornerstone-discrimination advantage (it holds for the historical
  # cornerstones too).
  ##############################################################################

  Scenario: Conditions historically used for the same comment rank above unrelated suggestions
    # One historical rule for "Elevated haemoglobin may be significant". It uses "eGFR ≥ 70".
    # As a suggestion for a subsequence rule, it has priority over attributes that have abnormal values.
    # There is no cornerstone case.
    Given a case with name Einstein is stored on the server
    And a case with name Planck is stored on the server
    And a backdoor rule is built for case Planck to add the comment "Elevated Hb may be significant." with conditions:
      | eGFR ≥ 70             |
      | HAEMOGLOBIN is normal |
    And I start the client application
    And I see the case Einstein as the current case
    When I request that the comment "Elevated Hb may be significant." be added
    Then the suggested condition "eGFR ≥ 70" should appear before all of the following suggestions:
      | AST increasing         |
      | HAEMOGLOBIN increasing |

  Scenario: Conditions used by multiple historical rules for the same comment rank above conditions used only once
    # Three historical rules for "Let's go to the beach."
    # Two rules use: Waves ≥ 1
    # One rule uses: Sun is "hot"
    # We expect the Waves suggestion to be before Sun suggestion
    Given case Bondi is provided having data:
      | Sun      | hot |
      | UV index | 1   |
      | Waves    | 1.5 |
    And case Malabar is provided having data:
      | Sun      | hot |
      | UV index | 2   |
      | Waves    | 2.0 |
    And case Coogee is provided having data:
      | Sun      | hot |
      | UV index | 3   |
      | Waves    | 2.0 |
    And case Maroubra is provided having data:
      | Sun      | hot |
      | UV index | 4   |
      | Waves    | 2.0 |
    And I start the client application
    # Ensure that each case gets the the comment via a different rule
    And a backdoor rule is built for case Malabar to add the comment "Let's go to the beach." with conditions:
      | Waves ≥ 1       |
      | UV index is "2" |
    And a backdoor rule is built for case Coogee to add the comment "Let's go to the beach." with conditions:
      | Waves ≥ 1       |
      | UV index is "3" |
    And a backdoor rule is built for case Maroubra to add the comment "Let's go to the beach." with conditions:
      | Sun is "hot"    |
      | UV index is "4" |
    And I start the client application
    And I see the case Bondi as the current case
    When I request that the comment "Let's go to the beach." be added
    Then the suggested condition "Waves ≥ 1.0" should appear before the following suggestion:
      | Sun is "hot |

  Scenario: Historical signal applies only to rules whose conclusion matches the action's target conclusion
    # The backdoor rule uses a DIFFERENT comment, so the historical scorer
    # must not surface its condition for "Elevated haemoglobin may be significant".
    Given a case with name Einstein is stored on the server
    And a case with name Planck is stored on the server
    And a backdoor rule is built for case Planck to add the comment "Some other finding." with conditions:
      | eGFR ≥ 70 |
    And I start the client application
    When I request that the comment "Elevated haemoglobin may be significant." be added
    And the case Planck is shown as the cornerstone case
    # the historical condition does not even make the top 20
    Then the suggestion "eGFR ≥ 70" should NOT appear
