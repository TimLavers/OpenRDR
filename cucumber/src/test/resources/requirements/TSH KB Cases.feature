Feature: The cases described in the TSH paper can be represented in OpenRDR

  Scenario: TSH KB cases
    Given the TSH sample KB has been loaded
    And I start the client application
    And the cases are loaded
    Then the count of the number of cases is 34

    When I select case 1.4.1
    Then I see these case values:
      | Sex               | F                 |            |
      | Age               | 28                |            |
      | TSH               | 0.67 mU/L         | 0.50 - 4.0 |
      | Free T4           | 16 pmol/L         |   10 - 20  |
      | Patient Location  | General Practice. |            |
      | Tests             | TFTs              |            |
      | Clinical Notes    | Lethargy.         |            |

    When I select case 1.4.2
    Then I see these case values:
      | Sex               | F                 |            |
      | Age               | 28                |            |
      | TSH               | 0.67 mU/L         | 0.50 - 4.0 |
      | Patient Location  | General Practice. |            |
      | Tests             | TFTs              |            |
      | Clinical Notes    | Lethargy.         |            |

    When I select case 1.4.3
    Then I see these case values:
      | Sex               | F                 |            |
      | Age               | 36                |            |
      | TSH               | 0.74 mU/L         | 0.50 - 4.0 |
      | Free T4           | 8 pmol/L          |   10 - 20  |
      | Patient Location  | General Practice. |            |
      | Tests             | TFTs              |            |
      | Clinical Notes    | Weight loss.      |            |

    When I select case 1.4.4
    Then I see these case values:
      | Sex               | F                 |            |
      | Age               | 57                |            |
      | TSH               | 7.3 mU/L          | 0.50 - 4.0 |
      | Patient Location  | General Practice. |            |
      | Tests             | TFTs              |            |
      | Clinical Notes    | Weight gain.      |            |

    When I select case 1.4.5
    Then I see these case values:
      | Sex               | F                 |            |
      | Age               | 57                |            |
      | TSH               | 7.3 mU/L          | 0.50 - 4.0 |
      | Free T4           | 13 pmol/L         |   10 - 20  |
      | Patient Location  | General Practice. |            |
      | Tests             | TFTs              |            |
      | Clinical Notes    | Weight gain.      |            |

    When I select case 1.4.6
    Then I see these case values:
      | Sex               | M                 |            |
      | Age               | 76                |            |
      | TSH               | 4.5 mU/L          | 0.50 - 4.0 |
      | Free T4           | 15 pmol/L         |   10 - 20  |
      | Patient Location  | General Practice. |            |
      | Tests             | TFTs              |            |
      | Clinical Notes    | Routine check.    |            |

    When I select case 1.4.7
    Then I see these case values:
      | Sex               | F                 |            |
      | Age               | 62                |            |
      | TSH               | 14.0 mU/L         | 0.50 - 4.0 |
      | Free T4           | 13 pmol/L         |   10 - 20  |
      | Patient Location  | General Practice. |            |
      | Tests             | TFTs              |            |
      | Clinical Notes    | Constipation.     |            |

    When I select case 1.4.8
    Then I see these case values:
      | Sex               | F                                 |            |
      | Age               | 27                                |            |
      | TSH               | 0.05 mU/L                         | 0.50 - 4.0 |
      | Free T4           | 13 pmol/L                         |   10 - 20  |
      | Patient Location  | Obstetric clinic.                 |            |
      | Tests             | TFTs                              |            |
      | Clinical Notes    | Period of amenorrhea 12/40 weeks. |            |

    When I select case 1.4.9
    Then I see these case values:
      | Sex               | F                 |            |
      | Age               | 32                |            |
      | TSH               | 4.6 mU/L          | 0.50 - 4.0 |
      | Free T4           | 13 pmol/L         |   10 - 20  |
      | TPO Antibodies    | 33 kU/L           |   < 6      |
      | Patient Location  | General Practice. |            |
      | Tests             | TFTs              |            |
      | Clinical Notes    | Trying for a baby.|            |

    When I select case 1.4.10
    Then I see these case values:
      | Sex               | M                  |            |
      | Age               | 55                 |            |
      | TSH               | 0.02 mU/L          | 0.50 - 4.0 |
      | Free T4           | 18 pmol/L          |   10 - 20  |
      | Patient Location  | General Practice.  |            |
      | Tests             | TFTs               |            |
      | Clinical Notes    | Feeling very tired.|            |

    When I select case 1.4.11
    Then I see these case values:
      | Sex               | M                 |            |
      | Age               | 55                |            |
      | TSH               | 0.02 mU/L         | 0.50 - 4.0 |
      | Free T4           | 18 pmol/L         |   10 - 20  |
      | Free T3           | 6.1 pmol/L        | 3.0 - 5.5  |
      | Patient Location  | General Practice. |            |
      | Tests             | TFTs              |            |
      | Clinical Notes    | Hyperthyroid?     |            |

    When I select case 1.4.12
    Then I see these case values:
      | Sex               | M                  |            |
      | Age               | 74                 |            |
      | TSH               | 59 mU/L            | 0.50 - 4.0 |
      | Free T4           | <5 pmol/L          |   10 - 20  |
      | Patient Location  | General Practice.  |            |
      | Tests             | TFTs               |            |
      | Clinical Notes    | Hypothyroid?       |            |

    When I select case 1.4.13
    Then I see these case values:
      | Sex               | M                  | M                                               |            |
      | Age               | 74                 | 74                                              |            |
      | TSH               | 59 mU/L            | 40 mU/L                                         | 0.50 - 4.0 |
      | Free T4           | <5 pmol/L          | 8 pmol/L                                        |   10 - 20  |
      | Patient Location  | General Practice.  | General Practice.                               |            |
      | Tests             | TFTs               | TFTs                                            |            |
      | Clinical Notes    | Hypothyroid?       | Hypothyroid, started T4 replacement 1 week ago. |            |

    When I select case 1.4.14
    Then I see these case values:
      | Sex               | F                  |            |
      | Age               | 43                 |            |
      | TSH               | 0.72 mU/L          | 0.50 - 4.0 |
      | Free T4           | 16 pmol/L          |   10 - 20  |
      | Patient Location  | General Practice.  |            |
      | Tests             | TFTs               |            |
      | Clinical Notes    | On T4 replacement. |            |

    When I select case 1.4.15
    Then I see these case values:
      | Sex               | F                  |            |
      | Age               | 54                 |            |
      | TSH               | 5.6 mU/L           | 0.50 - 4.0 |
      | Free T4           | 12 pmol/L          |   10 - 20  |
      | Patient Location  | General Practice.  |            |
      | Tests             | TFTs               |            |
      | Clinical Notes    | On T4 replacement. |            |

    When I select case 1.4.16
    Then I see these case values:
      | Sex               | F                  |            |
      | Age               | 61                 |            |
      | TSH               | 0.02 mU/L          | 0.50 - 4.0 |
      | Free T4           | 19 pmol/L          |   10 - 20  |
      | Patient Location  | General Practice.  |            |
      | Tests             | TFTs               |            |
      | Clinical Notes    | On T4 replacement. |            |

    When I select case 1.4.17
    Then I see these case values:
      | Sex               | F                                                              |            |
      | Age               | 51                                                             |            |
      | TSH               | 0.12 mU/L                                                      | 0.50 - 4.0 |
      | Free T4           | 19 pmol/L                                                      |   10 - 20  |
      | Patient Location  | General Practice.                                              |            |
      | Tests             | TFTs                                                           |            |
      | Clinical Notes    | Previous total thyroidectomy for thyroid cancer. On thyroxine. |            |

    When I select case 1.4.18
    Then I see these case values:
      | Sex               | F                  | F                                               |            |
      | Age               | 56                 | 56                                              |            |
      | TSH               | 4.3 mU/L           | 3.6 mU/L                                        | 0.50 - 4.0 |
      | Free T4           | 13 pmol/L          | 12 pmol/L                                       |   10 - 20  |
      | Tests             | TFTs               | TFTs                                            |            |
      | Clinical Notes    |                    | Subclinical hypothyroidism, follow-up.          |            |

    When I select case 1.4.19
    Then I see these case values:
      | Sex               | F                  |            |
      | Age               | 37                 |            |
      | TSH               | 0.03 mU/L          | 0.50 - 4.0 |
      | Free T4           | 20 pmol/L          |   10 - 20  |
      | Patient Location  | General Practice.  |            |
      | Tests             | TFTs               |            |
      | Clinical Notes    | Amenorrhea.        |            |

    When I select case 1.4.20
    Then I see these case values:
      | Sex               | F                    |            |
      | Age               | 53                   |            |
      | TSH               | <0.01 mU/L           | 0.50 - 4.0 |
      | Free T4           | 16 pmol/L            |   10 - 20  |
      | Free T3           | 5.5 pmol/L           |  3.0 - 5.5 |
      | Patient Location  | General Practice.    |            |
      | Tests             | TFTs                 |            |
      | Clinical Notes    | Annual check.        |            |

    When I select case 1.4.21
    Then I see these case values:
      | Sex            | F             | F                        |            |
      | Age            | 53            | 53                       |            |
      | TSH            | <0.01 mU/L    | <0.01 mU/L               | 0.50 - 4.0 |
      | Free T4        | 16 pmol/L     | 17 pmol/L                |   10 - 20  |
      | Free T3        | 5.5 pmol/L    | 6.1 pmol/L               |  3.0 - 5.5 |
      | Tests          | TFTs          | TFTs                     |            |
      | Clinical Notes | Annual check. | Previous suppressed TSH. |            |

    When I select case 1.4.22
    Then I see these case values:
      | Sex               | F                                                           |            |
      | Age               | 84                                                          |            |
      | TSH               | <0.01 mU/L                                                  | 0.50 - 4.0 |
      | Free T4           | 45 pmol/L                                                   |   10 - 20  |
      | Free T3           | 18 pmol/L                                                   |  3.0 - 5.5 |
      | Patient Location  | Emergency Department.                                       |            |
      | Tests             | TFTs                                                        |            |
      | Clinical Notes    | Severe hypertension, sweating and palpitation. |            |

    When I select case 1.4.23
    Then I see these case values:
      | Sex               | F                                                         |            |
      | Age               | 46                                                        |            |
      | TSH               | <0.01 mU/L                                                | 0.50 - 4.0 |
      | Free T4           | 7 pmol/L                                                  |   10 - 20  |
      | Patient Location  | General Practice.                                         |            |
      | Tests             | TFTs                                                      |            |
      | Clinical Notes    | Started carbimazole therapy recently for Graves’ disease. |            |

    When I select case 1.4.24
    Then I see these case values:
      | Sex               | F                                           |            |
      | Age               | 59                                          |            |
      | TSH               | 120 mU/L                                    | 0.50 - 4.0 |
      | Patient Location  | Nuclear Medicine.                           |            |
      | Tests             | TFTs                                        |            |
      | Clinical Notes    | Thyroid cancer. Pre I-131 Thyrogen therapy. |            |

    When I select case 1.4.25
    Then I see these case values:
      | Sex                | F                                               |     |
      | Age                | 64                                              |     |
      | Thyroglobulin      | 31 μg/L                                         |     |
      | Anti-Thyroglobulin | <1 kU/L                                         | < 4 |
      | Patient Location   | Oncology Clinic.                                |     |
      | Tests              | Tg/TgAb                                         |     |
      | Clinical Notes     | Thyroid cancer. Post thyroidectomy, monitoring. |     |

    When I select case 1.4.26
    Then I see these case values:
      | Sex                | F                                               |     |
      | Age                | 64                                              |     |
      | Thyroglobulin      | <0.1 μg/L                                       |     |
      | Anti-Thyroglobulin | 14 kU/L                                         | < 4 |
      | Patient Location   | Oncology Clinic.                                |     |
      | Tests              | Tg/TgAb                                         |     |
      | Clinical Notes     | Thyroid cancer. Post thyroidectomy, monitoring. |     |

    When I select case 1.4.27
    Then I see these case values:
      | Sex               | M                                  |            |
      | Age               | 50                                 |            |
      | TSH               | 4.2 mU/L                           | 0.50 - 4.0 |
      | Free T4           | 11 pmol/L                          |   10 - 20  |
      | Free T3           | 5.6 pmol/L                         |  3.0 - 5.5 |
      | TPO Antibodies    | 876 kU/L                           |   < 6      |
      | Patient Location  | General Practice. |            |
      | Tests             | TFTs                               |            |
      | Clinical Notes    | Family history of thyroid disease. |            |

    When I select case 1.4.28
    Then I see these case values:
      | Sex               | M                 |            |
      | Age               | 63                |            |
      | TSH               | <0.01 mU/L        | 0.50 - 4.0 |
      | Free T4           | 23 pmol/L         |   10 - 20  |
      | Free T3           | 5.0 pmol/L        |  3.0 - 5.5 |
      | Patient Location  | General Practice. |            |
      | Tests             | TFTs              |            |
      | Clinical Notes    | On amiodarone.    |            |

    When I select case 1.4.29
    Then I see these case values:
      | Sex               | M                 |            |
      | Age               | 53                |            |
      | TSH               | 1.3 mU/L          | 0.50 - 4.0 |
      | Free T4           | 26 pmol/L         |   10 - 20  |
      | Patient Location  | General Practice. |            |
      | Tests             | TFTs              |            |
      | Clinical Notes    | Diabetes.         |            |

    When I select case 1.4.30
    Then I see these case values:
      | Sex               | M                 |            |
      | Age               | 53                |            |
      | TSH               | 1.3 mU/L          | 0.50 - 4.0 |
      | Free T4           | 26 pmol/L         |   10 - 20  |
      | Free T3           | 6.1 pmol/L        |  3.0 - 5.5 |
      | Patient Location  | General Practice. |            |
      | Tests             | TFTs              |            |
      | Clinical Notes    | Diabetes.         |            |

    When I select case 1.4.31
    Then I see these case values:
      | Sex               | M                   |            |
      | Age               | 39                  |            |
      | TSH               | <0.01 mU/L          | 0.50 - 4.0 |
      | Free T4           | 43 pmol/L           |   10 - 20  |
      | Free T3           | 22 pmol/L           |  3.0 - 5.5 |
      | Sodium            | 143 mmol/L          |  134 - 146 |
      | Potassium         | 2.4 mmol/L          |  3.4 - 5.0 |
      | Bicarbonate       | 18 mmol/L           |   22 - 32  |
      | Urea              | 6.0 mmol/L          |  3.0 - 8.0 |
      | Creatinine        | 62 μmol/L           |  60 - 110  |
      | eGFR              | >90 mL/min/1.73 m∧2 |            |
      | Patient Location  | Emergency Dept.     |            |
      | Tests             | TFTs                |            |
      | Clinical Notes    | General weakness.   |            |

    When I select case 1.4.32
    Then I see these case values:
      | Sex               | M                    |            |
      | Age               | 60                   |            |
      | TSH               | 4.5 mU/L             | 0.50 - 4.0 |
      | Free T4           | 8 pmol/L             |   10 - 20  |
      | Patient Location  | General Practice.    |            |
      | Tests             | TFTs                 |            |
      | Clinical Notes    | Previous raised TSH. |            |

    When I select case 1.4.33
    Then I see these case values:
      | Sex               | M                         |            |
      | Age               | 67                        |            |
      | TSH               | 0.02 mU/L                 | 0.50 - 4.0 |
      | Free T4           | 8 pmol/L                  |   10 - 20  |
      | Patient Location  | General Practice.         |            |
      | Tests             | TFTs                      |            |
      | Clinical Notes    | Pituitary failure. On T4. |            |

# We haven't done case 35 as it is a chemistry panel.

#  TSH is ">100" in the document, but 1.4.24 has "120".
    When I select case 1.4.35
    Then I see these case values:
      | Sex               | F                     |            |
      | Age               | 66                    |            |
      | TSH               | 100 mU/L              | 0.50 - 4.0 |
      | Free T4           | 8 pmol/L              |   10 - 20  |
      | Patient Location  | Emergency Department. |            |
      | Tests             | TFTs                  |            |
      | Clinical Notes    | Semi-coma.            |            |

    And stop the client application
