Feature: Filtering attributes in the Case View
  # The case-view filter widget lets the user reduce the displayed attribute
  # rows to those that match a substring of the attribute name, value text,
  # reference range or units. The filter is owned by the case control so it
  # applies uniformly to the current case and any cornerstone case shown
  # alongside it. Edge cases (case-insensitivity, value/range/units matching,
  # blank/no-match queries, drag-and-drop suppression while filtered, escape
  # to clear, persistence across case selection and cornerstone changes) are
  # exercised by unit tests; this scenario is just the end-to-end happy path.

  @single
  Scenario: Typing a filter restricts the case view to matching attributes
    Given a case with name Einstein is stored on the server
    And I start the client application
    And I see the case Einstein as the current case
    When I enter the filter text "MCV"
    Then I should see these attributes:
      | MCV |
