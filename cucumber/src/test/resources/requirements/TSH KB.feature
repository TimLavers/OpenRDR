Feature: The cases described in the TSH paper can be represented in OpenRDR

  @single
  @tsh
  Scenario: TSH cases
    Given the TSH sample KB has been loaded
    And I start the client application
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


    And stop the client application
