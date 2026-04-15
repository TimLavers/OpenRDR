Feature: A sample KB can be created for Clinical Trial Matching.

  @single
  Scenario: Clinical Trial Matching KB
    Given I start the client application
    And I create a Knowledge Base with the name OncoTreeMatch
    And I send a case to "OncoTreeMatch" for each row in the trial-conditions file
    And the backdoor selects the Knowledge Base "OncoTreeMatch"
    And the count of the number of cases is 1508

    When backdoor rules are built as follows:
      | Case | Add    | Remove | Conditions                             |
      | 1    | CERVIX |        | canonical contains "cervical cancer"   |
      | 2    | BRAIN  |        | canonical contains "cns"               |
      | 4    | NSCLC  |        | canonical contains "nsclc"             |
      | 5    | ALL    |        | canonical contains "all"               |
      | 7    | AML    |        | canonical contains "aml"               |
      | 8    | AMLMRC | AML    | canonical contains "myelodysplastic"   |
      | 10   | AMLMRC | AML    | canonical contains "mds"               |
      | 12   | MEL    |        | canonical contains "melanoma"          |
      | 13   | MNM    |        | canonical contains "acute leukemia"    |
      | 14   | ALAL   | MNM    | canonical contains "ambiguous lineage" |
      | 16   | ALL    |        | canonical contains "lymphoblastic"     |
    Then the interpretation of each case should be as follows:
      | 1  | CERVIX |
      | 2  | BRAIN  |
      | 3  |        |
      | 4  | NSCLC  |
      | 5  | ALL    |
      | 6  | ALL    |
      | 7  | AML    |
      | 8  | AMLMRC |
      | 9  | AML    |
      | 10 | AMLMRC |
      | 11 |        |
      | 12 | MEL    |
      | 13 | MNM    |
      | 14 | ALAL   |
      | 15 | MNM    |
      | 16 | ALL    |
    And pause

