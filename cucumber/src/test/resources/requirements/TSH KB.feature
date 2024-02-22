Feature: The cases described in the TSH paper can be represented in OpenRDR

#  @single @tsh
  Scenario: TSH cases
    Given the TSH sample KB has been loaded
    And I start the client application
    And the count of the number of cases is 34

    When I select case 1.4.1
#    Then
#  assertEquals(dataShown.size, 7)
#  checkAgeSexTestsLocation(28, "F")
#  checkTSH("0.67")
#  checkFreeT4("16")
#  checkNotes( "Lethargy.")


    And pause for 30 seconds
    And stop the client application
