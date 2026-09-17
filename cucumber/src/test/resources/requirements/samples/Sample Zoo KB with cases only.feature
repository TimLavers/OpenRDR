Feature: A sample KB can be created that has the Zoo Animals cases.

  Scenario: Zoo KB cases
    Given A Knowledge Base called ZooCases has been created from the "Zoo Animals - cases only" sample
    And I start the client application
    Then the count of the number of cases is 101
    When I select case aardvark
    Then I see these case values:
      | hair     | true  |  |
      | feathers | false |  |
      | eggs     | false |  |
      | milk     | true  |  |
      | airborne | false |  |
      | aquatic  | false |  |
      | predator | true  |  |
      | toothed  | true  |  |
      | backbone | true  |  |
      | breathes | true  |  |
      | venomous | false |  |
      | fins     | false |  |
      | legs     | 4     |  |
      | tail     | false |  |
      | domestic | false |  |
      | catsize  | true  |  |

    When I select case catfish
    Then I see these case values:
      | hair     | false |  |
      | feathers | false |  |
      | eggs     | true  |  |
      | milk     | false |  |
      | airborne | false |  |
      | aquatic  | true  |  |
      | predator | true  |  |
      | toothed  | true  |  |
      | backbone | true  |  |
      | breathes | false |  |
      | venomous | false |  |
      | fins     | true  |  |
      | legs     | 0     |  |
      | tail     | true  |  |
      | domestic | false |  |
      | catsize  | false |  |

