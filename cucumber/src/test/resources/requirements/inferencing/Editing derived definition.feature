# This file specifies the behaviour described in
# documentation/design/editing_derived_attribute_definitions.md.
Feature: Editing the definition of a derived attribute
  A derived attribute has a stored definition (its formula or value). The
  user can edit that definition in place via the chat, so that the correction
  applies everywhere the attribute is given by its definition — to existing
  and new cases alike. No rule is built and there is no cornerstone review.
  In contrast, a case- or condition-specific correction builds an override
  rule and leaves the definition unchanged. An edit that would make an
  attribute depend on itself is refused.

  Scenario: Editing a definition corrects the value for existing and new cases without building a rule
    Given case Fermi is provided having data:
      | weight | 65   |
      | height | 1.72 |
    And I start the client application
    # The BMI formula is wrong: it should divide by the square of the height.
    And a backdoor rule is built for case Fermi to assign the formula "weight / height" to the derived attribute "BMI" with no conditions
    And I select the case Fermi
    And the UI should show the value for derived attribute "BMI" as "37.79"
    When I request that the definition of the derived attribute "BMI" be changed to "weight / (height * height)"
    Then the chatbot response contains the following terms:
      | definition |
      | BMI        |
    # The existing case gets the corrected value, with no rule change.
    And the UI should show the value for derived attribute "BMI" as "21.97"
    # A new case gets the corrected value too.
    Given case Curie is provided having data:
      | weight | 90  |
      | height | 2.0 |
    And I select the case Curie
    And the UI should show the value for derived attribute "BMI" as "22.5"

  Scenario: A case-specific correction builds an override rule and leaves the definition unchanged
    Given case Pauli is provided having data:
      | weight | 65   |
      | height | 1.72 |
    And case Curie is provided having data:
      | weight | 90  |
      | height | 2.0 |
    And I start the client application
    And a backdoor rule is built for case Pauli to assign the formula "weight / (height * height)" to the derived attribute "BMI" with no conditions
    And I select the case Pauli
    And the UI should show the value for derived attribute "BMI" as "21.97"
    When I request that the derived value "BMI" be replaced with "25" for reason "weight is 65"
    Then the UI should show the value for derived attribute "BMI" as "25"
    # The definition is unchanged, so other cases still get the defined value.
    And I select the case Curie
    And the UI should show the value for derived attribute "BMI" as "22.5"

  Scenario: An edit that would create a dependency cycle is refused with an explanation
    Given case Heisenberg is provided having data:
      | A | 1.0 |
    And a backdoor rule is built for case Heisenberg to assign the value "yes" to the derived attribute "Alpha" with no conditions
    And a backdoor rule is built for case Heisenberg to assign the value "yes" to the derived attribute "Beta" with conditions:
      | Alpha is in case |
    And I start the client application
    When I request that the definition of the derived attribute "Alpha" be changed to "Beta * 2"
    Then the chat should explain that the condition would create a cycle involving the following terms:
      | Alpha |
      | Beta  |
