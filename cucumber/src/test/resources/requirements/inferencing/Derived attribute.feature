Feature: Derived attribute
  A case has derived attribute values assigned by the KB, a collapsible
"Derived values" panel displays each derived attribute name and its value.
Hovering over any part of a derived attribute row shows a tooltip with the
formula (when it is not just the displayed value) and the conditions that
assigned the value. The panel is hidden when there are no derived values.

  Scenario: The derived values panel shows attribute name, value and condition
    Given case Fermi is provided having data:
      | Glucose | 12.0 |
    And I start the client application
    And a backdoor rule is built for case Fermi to assign the value "diabetic" to the derived attribute "Diabetes status" with conditions:
      | Glucose ≥ 11.0 |
    When I select the case Fermi
    Then the UI should show the derived value "Diabetes status" as "diabetic"
    And the UI should show the following conditions for the derived value "Diabetes status":
      | Glucose ≥ 11.0 |

  Scenario: The derived values panel shows attribute name,formula and value
    Given case Fermi is provided with the following values, reference ranges and units:
      | Attribute | Value | Low | High | Units |
      | Height    | 1.72  |     | 2.5  | m     |
      | Weight    | 65    |     | 100  | kg    |
    And I start the client application
    And a backdoor rule is built for case Fermi to assign the formula "weight/height**2" to the derived attribute "BMI" with conditions:
      | Height is in case |
    When I select the case Fermi
    Then the UI should show the derived value "BMI" as "21.97"
    And the formula showing for the derived value is "Weight/Height**2"

  Scenario: The derived values panel is hidden when there are no derived values
    Given case Fermi is provided having data:
      | Glucose | 5.0 |
    And I start the client application
    And I select the case Fermi
    Then the derived values panel should be hidden

  @single
  Scenario: The user should be prevented from creating a derived attribute with the same name as an existing attribute
    Given case Fermi is provided with the following values, reference ranges and units:
      | Attribute | Value | Low | High | Units |
      | Height    | 1.72  |     | 2.5  | m     |
      | Weight    | 65    |     | 100  | kg    |
    And a backdoor rule is built for case Fermi to assign the formula "weight/height**2" to the derived attribute "BMI" with conditions:
      | Height is in case |
    And I start the client application
    And I select the case Fermi
    When I request that the derived value for "bmi" be removed
    Then the UI should show the derived value "BMI" as "21.97"
    And the formula showing for the derived value is "Weight/Height**2"
    And pause



