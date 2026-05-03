Feature: Phase 1 — Suggested conditions are ranked by the rule action, the cornerstones, and historical conditions, and capped at a usable size.

  # This file specifies the *intended* behaviour after Phase 1 of the
  # targeted-suggested-conditions design (see
  # documentation/design/targeted_suggested_conditions_phase_1.md).
  #
  # Three sets of scenarios:
  #   Set A — Action targeting (Einstein only, no cornerstones, no rules).
  #           Ranking is driven by the comment text of the rule action,
  #           verified across Add / Remove / Replace.
  #   Set B — Cornerstone discrimination (Einstein + Planck as a CC).
  #           Suggestions that distinguish Einstein from Planck rank above
  #           those that hold for both cases.
  #   Set C — Historical conditions. Backdoor rules establish conditions
  #           previously used for a comment; those conditions surface at
  #           the top when the same comment is added again.
  #
  # Plus one limit scenario at the start to verify the cap on the total number of suggestions.

  ##############################################################################
  # Hard cap on number of suggestions
  ##############################################################################

  Scenario: The number of suggestions presented to the user is capped at 20
    # Cap chosen so the list is comfortably scannable end-to-end. Ranking
    # places the best suggestions near the top; the cap is a safety net, not
    # the primary UX. May be tightened once Phase 3 LLM/embedding signals
    # consistently push the best suggestions into the top 10.
    Given a case with name Einstein is stored on the server
    And I start the client application
    When I request that the comment "Routine review." be added
    Then the number of suggested conditions should be at most 20

  ##############################################################################
  # Set A — Action targeting (Einstein only, no rules, no cornerstones)
  #
  # Einstein has Haemoglobin 194 (high), MCV 100.2 (high), Sodium 141 (normal),
  # Sex M, etc. With no rules and no cornerstones, only the comment-text
  # varies, so ranking changes ONLY when the comment changes.
  ##############################################################################

  @single
  Scenario: When adding a comment, suggestions whose attribute or direction match the comment text rank above unrelated suggestions
    Given a case with name Einstein is stored on the server
    And I start the client application
    When I request that the comment "Elevated haemoglobin may be significant." be added
    Then the suggested condition "HAEMOGLOBIN" should appear before "AST"
    And the suggested condition "HAEMOGLOBIN" should appear before "Anion gap"

  Scenario: A different comment changes the ranking accordingly
    Given a case with name Einstein is stored on the server
    And I start the client application
    When I request that the comment "Elevated MCV may be significant." be added
    Then the suggested condition "MCV" should appear before "HAEMOGLOBIN"
    And the suggested condition "MCV" should appear before "Sodium"

  Scenario: When removing a comment, suggestions matching the comment text rank above unrelated suggestions
    Given a case with name Einstein is stored on the server
    And a backdoor rule is built for case Einstein to add the comment "MCV elevated." with conditions:
      | Sex is "M" |
    And I start the client application
    When I request that the following comment be removed:
      | MCV elevated |
    Then the suggested condition "MCV" should appear before "Sodium"
    And the suggested condition "MCV" should appear before "HAEMOGLOBIN"

  Scenario: When replacing a comment, suggestions matching the REPLACEMENT comment rank above those matching the original
    Given a case with name Einstein is stored on the server
    And a backdoor rule is built for case Einstein to add the comment "Elevated haemoglobin may be significant." with conditions:
      | Sex is "M" |
    And I start the client application
    When I request that the comment be replaced by "macrocytosis MCV."
    Then the suggested condition "MCV" should appear before "HAEMOGLOBIN"
    And the suggested condition "MCV" should appear before "Sodium"

  ##############################################################################
  # Set B — Cornerstone discrimination (Einstein session, Planck cornerstone)
  #
  # Planck has Haemoglobin 139 (normal), MCV 91.3 (normal), Sex M.
  # Einstein has Haemoglobin 194 (high), MCV 100.2 (high), Sex M.
  # The session comment is deliberately chosen to have NO predicate-vocabulary
  # tokens (i.e. nothing matching the attributes), so comment-overlap is zero for every candidate and ranking is
  # driven entirely by cornerstone discrimination.
  ##############################################################################

  Scenario: When a cornerstone is shown, suggestions that distinguish the case from the cornerstone rank above those that hold for both
    # Two assertions, both showing "discriminating beats non-discriminating
    # even when alphabetically disadvantaged":
    #   HAEMOGLOBIN: Einstein 194 (clearly high), Planck 139 (normal).
    #   MCV:         Einstein 100.2 (marginally high), Planck 91.3 (normal).
    #   Albumin:     Einstein 38, Planck 40 — both normal, does NOT discriminate.
    # Alphabetically Albumin < HAEMOGLOBIN and Albumin < MCV, so without
    # discrimination Albumin would win on tiebreak.
    # Both discriminatory attributes (Haemoglobin and MCV) should rank higher than Albumin
    Given a case with name Einstein is stored on the server
    And a case with name Planck is stored on the server
    And a backdoor rule is built for case Planck to add the comment "Routine review." with conditions:
      | Sex is "M" |
    And I start the client application
    When I request that the comment "Investigate further." be added
    And the case Planck is shown as the cornerstone case
    Then the suggested condition "HAEMOGLOBIN" should appear before "Albumin"
    And the suggested condition "MCV" should appear before "Albumin"

  Scenario: A condition that holds for both the session case and the cornerstone is not preferred
    # Sex is "M" holds for both Einstein and Planck (discrimination = 0).
    # HAEMOGLOBIN ≥ 180 holds only for Einstein (discrimination = 1).
    Given a case with name Einstein is stored on the server
    And a case with name Planck is stored on the server
    And a backdoor rule is built for case Planck to add the comment "Routine review." with conditions:
      | Sex is "M" |
    And I start the client application
    When I request that the comment "Investigate further." be added
    And the case Planck is shown as the cornerstone case
    Then the suggested condition "HAEMOGLOBIN" should appear before "Sex is \"M\""

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
    Given a case with name Einstein is stored on the server
    And a case with name Planck is stored on the server
    And a backdoor rule is built for case Planck to add the comment "Elevated haemoglobin may be significant." with conditions:
      | eGFR ≥ 70 |
    And I start the client application
    And I see the case Einstein as the current case
    When I request that the comment "Elevated haemoglobin may be significant." be added
    And the case Planck is shown as the cornerstone case
    Then the suggested condition "eGFR ≥ 70" should appear before "HAEMOGLOBIN"
    And the suggested condition "eGFR ≥ 70" should appear before "Sodium"

  Scenario: Conditions used by multiple historical rules for the same comment rank above conditions used only once
    # Two historical rules for "Elevated haemoglobin may be significant" both use "eGFR ≥ 70";
    # one rule for the same comment uses "Bilirubin Total ≤ 20". The
    # historical scorer counts matching rules, so eGFR (count 2) ranks
    # above Bilirubin Total (count 1).
    Given a case with name Einstein is stored on the server
    And a case with name Planck is stored on the server
    And case Curie is provided having data:
      | eGFR            | 80 |
      | Bilirubin Total | 12 |
    And case Bohr is provided having data:
      | eGFR            | 78 |
      | Bilirubin Total | 10 |
    And a backdoor rule is built for case Planck to add the comment "Elevated haemoglobin may be significant." with conditions:
      | eGFR ≥ 70 |
    And a backdoor rule is built for case Curie to add the comment "Elevated haemoglobin may be significant." with conditions:
      | eGFR ≥ 70 |
    And a backdoor rule is built for case Bohr to add the comment "Elevated haemoglobin may be significant." with conditions:
      | Bilirubin Total ≤ 20 |
    And I start the client application
    When I request that the comment "Elevated haemoglobin may be significant." be added
    And the cornerstone case indicator shows 1 of 3
    Then the suggested condition "eGFR ≥ 70" should appear before "Bilirubin Total ≤ 20"

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
    Then the suggested condition "eGFR ≥ 70" should NOT be the first suggestion
