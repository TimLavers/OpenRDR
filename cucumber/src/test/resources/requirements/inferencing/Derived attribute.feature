Feature: Derived attribute
When a case has derived attribute values assigned by the KB, a collapsible
"Derived values" panel displays each derived attribute name and its value.
Hovering over a derived attribute name shows a tooltip with the value or formula and
the conditions that assigned the value. The panel is hidden when there are
no derived values.

  @single
  Scenario: The derived values panel shows derived attribute names and values
    Given case Fermi is provided having data:
      | Glucose | 12.0 |
    And I start the client application
    And a backdoor rule is built for case Fermi to assign the value "diabetic" to the derived attribute "Diabetes status" with conditions:
      | Glucose ≥ 11.0 |
    And I select the case Fermi
    Then the UI should show the derived value "Diabetes status" as "diabetic"
    And the UI should show the formula "\"diabetic\"" for the derived value "Diabetes status"
    And the UI should show the following conditions for the derived value "Diabetes status":
      | Glucose ≥ 11.0 |

  Scenario: The derived values panel is hidden when there are no derived values
    Given case Fermi is provided having data:
      | Glucose | 5.0 |
    And I start the client application
    And I select the case Fermi
    Then the derived values panel should be hidden
