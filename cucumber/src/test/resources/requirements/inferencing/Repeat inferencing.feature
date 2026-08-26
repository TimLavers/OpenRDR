# This file specifies the *intended* behaviour after Phase 1 of the
# repeat-inferencing design (see documentation/design/repeat_inferencing.md).
# Phase 1 step 9: step definitions and backdoor plumbing are now in place.
Feature: Repeat inferencing via derived attributes.
  A rule can assign a value to a derived attribute, and other rules can use
  that value in their conditions. Inference repeats until the interpretation
  is stable. Conditions that would create a dependency cycle are neither
  suggested nor accepted.

  ##############################################################################
  # Chained rules: a rule conditioned on the output of another rule
  ##############################################################################
  Scenario: A comment rule can be conditioned on a derived value assigned by another rule
    Given case Fermi is provided having data:
      | Glucose | 12.0 |
    And I start the client application
    And a backdoor rule is built for case Fermi to assign the value "diabetic" to the derived attribute "Diabetes status" with conditions:
      | Glucose ≥ 11.0 |
    And a backdoor rule is built for case Fermi to add the comment "Diabetic diet advice given." with conditions:
      | Diabetes status is "diabetic" |
    And I select the case Fermi
    Then the interpretation report should be "Diabetic diet advice given."
    And the derived value "Diabetes status" should be "diabetic"

  ##############################################################################
  # Numeric conditions on assigned values
  ##############################################################################

  Scenario: A numeric condition can be used on an assigned derived value
    Given case Curie is provided having data:
      | Glucose | 12.0 |
    And I start the client application
    And a backdoor rule is built for case Curie to assign the value "7" to the derived attribute "Risk score" with conditions:
      | Glucose ≥ 11.0 |
    And a backdoor rule is built for case Curie to add the comment "High risk patient." with conditions:
      | Risk score > 5 |
    And I select the case Curie
    Then the interpretation report should be "High risk patient."

  ##############################################################################
  # Absence conditions on derived attributes
  ##############################################################################

  Scenario: A rule can be conditioned on the absence of a derived attribute
    Given case Bohr is provided having data:
      | Glucose | 5.0 |
    And I start the client application
    And a backdoor rule is built for case Bohr to assign the value "diabetic" to the derived attribute "Diabetes status" with conditions:
      | Glucose ≥ 11.0 |
    And a backdoor rule is built for case Bohr to add the comment "No evidence of diabetes." with conditions:
      | Diabetes status is not in case |
    And I select the case Bohr
    Then the interpretation report should be "No evidence of diabetes."

  ##############################################################################
  # Formula-based derived attributes
  ##############################################################################

  Scenario: An unconditional formula rule assigns a computed value, usable in conditions
    Given case Pauli is provided having data:
      | weight | 93.0 |
      | height | 1.8  |
    And I start the client application
    And a backdoor rule is built for case Pauli to assign the formula "weight / (height * height)" to the derived attribute "BMI" with no conditions
    And a backdoor rule is built for case Pauli to add the comment "Elevated BMI." with conditions:
      | BMI > 28 |
    And I select the case Pauli
    Then the interpretation report should be "Elevated BMI."

  Scenario: A formula referencing an attribute with no value makes no assignment
    # Bohr is here to make height an attribute of the knowledge base. Without it
    # the formula would name nothing at all, which is a different case: what is
    # under test is an attribute the case has no value for.
    Given case Bohr is provided having data:
      | weight | 80.0 |
      | height | 1.9  |
    And case Dirac is provided having data:
      | weight | 93.0 |
    And I start the client application
    And a backdoor rule is built for case Dirac to assign the formula "weight / (height * height)" to the derived attribute "BMI" with no conditions
    When I see the case Dirac as the current case
    Then the derived value "BMI" should not be present

  ##############################################################################
  # Cycle prevention
  ##############################################################################
  Scenario: A condition that would create a dependency cycle is not suggested
    # "Alpha" is assigned when "Beta" is absent. A rule assigning "Beta"
    # conditioned on "Alpha" would create the cycle Alpha → Beta → Alpha.
    Given case Heisenberg is provided having data:
      | A | 1.0 |
    And a backdoor rule is built for case Heisenberg to assign the value "yes" to the derived attribute "Alpha" with no conditions
    And a backdoor rule is built for case Heisenberg to assign the value "yes" to the derived attribute "Beta" with conditions:
      | Alpha is in case |
    And I start the client application
    When I request that the derived value "alpha" be removed
    Then none of the suggestions should contain any of the following terms:
      | Alpha |
      | Beta  |

  Scenario: A manually entered condition that would create a dependency cycle is refused with an explanation
    Given case Heisenberg is provided having data:
      | A | 1.0 |
    And a backdoor rule is built for case Heisenberg to assign the value "yes" to the derived attribute "Alpha" with no conditions
    And a backdoor rule is built for case Heisenberg to assign the value "yes" to the derived attribute "Beta" with conditions:
      | Alpha is in case |
    And a backdoor rule is built for case Heisenberg to assign the value "yes" to the derived attribute "Gamma" with conditions:
      | Beta is in case |
    And a backdoor rule is built for case Heisenberg to assign the value "yes" to the derived attribute "Delta" with conditions:
      | Gamma is in case |
    And I start the client application
    And I request that the derived value "alpha" be removed
    When I provide the following reason:
      | Gamma is in case |
    Then the chat should explain that the condition would create a cycle involving the following terms:
      | Alpha |
      | Beta  |
      | Gamma |
