Feature: The cases described in the Contact Lenses sample KB get the correct interpretations

  @single
  Scenario: Contact Lenses cases
    Given the Contact Lenses sample KB has been loaded
    And I start the client application
    Then the count of the number of cases is 24
    And pause for 3 seconds

    When I select case Case1
    Then the interpretation field should be empty

    When I select case Case2
    Then the interpretation should be "soft"

    When I select case Case3
    Then the interpretation field should be empty

    When I select case Case4
    Then the interpretation should be "hard"

    When I select case Case5
    Then the interpretation field should be empty

    When I select case Case6
    Then the interpretation should be "soft"

    When I select case Case7
    Then the interpretation field should be empty

    When I select case Case8
    Then the interpretation should be "hard"

    When I select case Case9
    Then the interpretation field should be empty

    When I select case Case10
    Then the interpretation should be "soft"

    When I select case Case11
    Then the interpretation field should be empty

    When I select case Case12
    Then the interpretation should be "hard"

    When I select case Case13
    Then the interpretation field should be empty

    When I select case Case14
    Then the interpretation should be "soft"

    When I select case Case15
    Then the interpretation field should be empty

    When I select case Case16
    And pause for 2 seconds
    Then the interpretation field should be empty

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
