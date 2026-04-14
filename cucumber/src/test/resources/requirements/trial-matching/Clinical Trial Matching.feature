Feature: A sample KB can be created for Clinical Trial Matching.

  @single
  Scenario: Clinical Trial Matching KB
    Given I start the client application
    And I create a Knowledge Base with the name OncoTreeMatch
    Then I send a case to "OncoTreeMatch" for each row in the trial-conditions file
    And pause
    Then the count of the number of cases is 1508
    And the backdoor selects the Knowledge Base "OncoTreeMatch"

    And a backdoor rule is built for case 1 to add the comment "NSCLC" with conditions:
      | canonical contains "cervical cancer" |

    And a backdoor rule is built for case 2 to add the comment "BRAIN" with conditions:
      | canonical contains "cns" |

    And a backdoor rule is built for case 4 to add the comment "NSCLC" with conditions:
      | canonical contains "nsclc" |

    And a backdoor rule is built for case 5 to add the comment "ALL" with conditions:
      | canonical contains "all" |

    And a backdoor rule is built for case 7 to add the comment "AML" with conditions:
      | canonical contains "aml" |

    And a backdoor rule is built for case 8 to replace the comment "AML" with "AMLMRC" with conditions:
      | canonical contains "myelodysplastic" |

    And pause for 30 seconds