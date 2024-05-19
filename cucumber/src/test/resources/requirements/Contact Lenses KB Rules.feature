Feature: The cases described in the Contact Lenses sample KB get the correct interpretations

  @single
  Scenario: Contact Lenses cases
    Given the Contact Lenses sample KB has been loaded
    And I start the client application
    Then the count of the number of cases is 24
    And pause for 1 second

    When I select case Case1
    Then the interpretation field should be empty

    When I select case Case2
    Then the interpretation should be "soft"

    When I select case Case3
    Then the interpretation field should be empty

    When I select case Case4
    Then the interpretation should be "hard"

#    When I select case Case5
#
#    When I select case Case6
#
#    When I select case Case7
#
#    When I select case Case8
#
#    When I select case Case9
#
#    When I select case Case10
#
#    When I select case Case11
#
#    When I select case Case12
#
#    When I select case Case13
#
#    When I select case Case14
#
#    When I select case Case15
#
#    When I select case Case16
#
#    When I select case Case17
#
#    When I select case Case18
#
#    When I select case Case19
#
#    When I select case Case20
#
#    When I select case Case21
#
#    When I select case Case22
#
#    When I select case Case23
#
#    When I select case Case24
#
    And stop the client application
