Feature: A sample KB can be created for Clinical Trial Matching.

  @single
  Scenario: Clinical Trial Matching KB
    Given I start the client application
    And I create a Knowledge Base with the name OncoTreeMatch
    And I send a case to "OncoTreeMatch" for each row in the trial-conditions file
    And the backdoor selects the Knowledge Base "OncoTreeMatch"
    And the count of the number of cases is 1508
    When backdoor rules are built as follows:
      | Case | Add    | Remove | Conditions                                     |
      | 1    | CERVIX |        | canonical contains "cervical cancer"           |
      | 2    | BRAIN  |        | canonical contains "cns"                       |
      | 4    | NSCLC  |        | canonical contains "nsclc"                     |
      | 5    | ALL    |        | canonical contains "all"                       |
      | 7    | AML    |        | canonical contains "aml"                       |
      | 8    | AMLMRC | AML    | canonical contains "myelodysplastic"           |
      | 10   | AMLMRC | AML    | canonical contains "mds"                       |
      | 12   | MEL    |        | canonical contains "melanoma"                  |
      | 13   | MNM    |        | canonical contains "acute leukemia"            |
      | 14   | ALAL   | MNM    | canonical contains "ambiguous lineage"         |
      | 16   | ALL    |        | canonical contains "lymphoblastic"             |
      | 21   | AML    |        | canonical contains "myeloid leukaemia"         |
      | 22   | AML    |        | canonical contains "myeloid leukemia"          |
      | 27   | AML    |        | canonical contains "promyelocytic"             |
      | 28   | MNM    |        | canonical contains "undifferentiated leukemia" |
    And pause for 300 seconds
    Then the interpretation of each case should be as follows:
      | 1  | CERVIX        |
      | 2  | BRAIN         |
      | 3  | UNK           |
      | 4  | NSCLC         |
      | 5  | BLL,TLL       |
      | 6  | BLL,TLL       |
      | 7  | AML           |
      | 8  | AMLMRC        |
      | 9  | AML           |
      | 10 | AML,MDS       |
      | 11 | UNK           |
      | 12 | ACRM          |
      | 13 | ALAL          |
      | 14 | ALAL          |
      | 15 | ALAL,AML      |
      | 16 | BLL,TLL       |
      | 17 | BLL,TLL       |
      | 18 | BLL,TLL       |
      | 19 | BLL,TLL       |
      | 20 | BLL,TLL       |
      | 21 | AML           |
      | 22 | AML           |
      | 23 | AML           |
      | 24 | AML           |
      | 25 | AML           |
      | 26 | AML           |
      | 27 | APLPMLRARA    |
      | 28 | AUL           |
      | 29 | UNK           |
      | 30 | ACYC          |
      | 31 | UNK           |
      | 32 | UNK           |
      | 33 | UNK           |
      | 34 | UNK           |
      | 35 | ADRENAL_GLAND |
      | 36 | BLL           |
      | 37 | WT            |
      | 38 | UNK           |
      | 39 | TLL           |
      | 40 | TLL           |
      | 41 | UNK           |
      | 42 | UNK           |
      | 43 | BILIARY_TRACT |
      | 44 | BREAST        |
      | 45 | UNK           |
      | 46 | GBC           |
      | 47 | CHOL,HCCIHCH  |
      | 48 | CCRCC         |
      | 49 | COADREAD,CMC  |
      | 50 | CSCC          |

