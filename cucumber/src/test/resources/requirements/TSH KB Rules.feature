Feature: The cases described in the TSH paper can be represented in OpenRDR

  @tsh
  Scenario: TSH cases
    Given the TSH sample KB has been loaded
    And I start the client application
    Then the count of the number of cases is 34

    When I select case 1.4.1
    Then the interpretation should be "Normal T4 and TSH are consistent with a euthyroid state."

#    When I select case 1.4.2
#    Then I see these case values:

#    When I select case 1.4.3
#
#    When I select case 1.4.4
#
#    When I select case 1.4.5
#
#    When I select case 1.4.6
#
#    When I select case 1.4.7
#
#    When I select case 1.4.8
#
#    When I select case 1.4.9
#
#    When I select case 1.4.10
#
#    When I select case 1.4.11
#
#    When I select case 1.4.12
#
#    When I select case 1.4.13
#
#    When I select case 1.4.14
#
#    When I select case 1.4.15
#
#    When I select case 1.4.16
#
#    When I select case 1.4.17
#
#    When I select case 1.4.18
#
#    When I select case 1.4.19
#
#    When I select case 1.4.20
#
#    When I select case 1.4.21
#
#    When I select case 1.4.22
#
#    When I select case 1.4.23
#
#    When I select case 1.4.24
#
#    When I select case 1.4.25
#
#    When I select case 1.4.26
#
#    When I select case 1.4.27
#
#    When I select case 1.4.28
#
#    When I select case 1.4.29
#
#    When I select case 1.4.30
#
#    When I select case 1.4.31
#
#    When I select case 1.4.32
#
#    When I select case 1.4.33
#
# We haven't done case 35 as it is a chemistry panel.
#
#    When I select case 1.4.35

    And stop the client application
