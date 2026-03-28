Feature: The Zoo KB can be built using the backdoor rule-building endpoint.

  Scenario: Build the Zoo KB from cases
    Given I start the client application
    And I create a Knowledge Base with the name Zoo based on the "Zoo Animals - cases only" sample
    Then the count of the number of cases is 101

    And the backdoor selects the Knowledge Base "Zoo"

    And a backdoor rule is built for case aardvark to add the comment "mammal" with conditions:
      | milk is "true" |

    And a backdoor rule is built for case bass to add the comment "fish" with conditions:
      | aquatic is "true" |

    And a backdoor rule is built for case chicken to add the comment "bird" with conditions:
      | feathers is "true" |

    And a backdoor rule is built for case clam to add the comment "mollusc" with conditions:
      | backbone is "false" |
      | breathes is "false" |
      | legs is "0"         |

    And a backdoor rule is built for case crab to replace the comment "fish" with "mollusc" with conditions:
      | backbone is "false" |
      | fins is "false"     |

    And a backdoor rule is built for case dolphin to remove the comment "fish" with conditions:
      | milk is "true" |

    And a backdoor rule is built for case duck to remove the comment "fish" with conditions:
      | feathers is "true" |

    And a backdoor rule is built for case flea to add the comment "insect" with conditions:
      | eggs is "true"      |
      | breathes is "true"  |
      | backbone is "false" |

    And a backdoor rule is built for case frog to replace the comment "fish" with "amphibian" with conditions:
      | milk is "false"     |
      | breathes is "true"  |
      | feathers is "false" |

    And a backdoor rule is built for case pitviper to add the comment "reptile" with conditions:
      | backbone is "true" |
      | breathes is "true" |
      | legs is "0"        |
      | eggs is "true"     |

    And a backdoor rule is built for case scorpion to add the comment "mollusc" with conditions:
      | backbone is "false" |
      | breathes is "true"  |
      | legs is "8"         |

    And a backdoor rule is built for case seasnake to replace the comment "fish" with "reptile" with conditions:
      | backbone is "true"  |
      | feathers is "false" |
      | fins is "false"     |
      | legs is "0"         |

    And a backdoor rule is built for case slug to replace the comment "insect" with "mollusc" with conditions:
      | legs is "0" |

    And a backdoor rule is built for case tortoise to add the comment "reptile" with conditions:
      | backbone is "true" |
      | eggs is "true"     |
      | tail is "true"     |
      | legs is "4"        |

    And a backdoor rule is built for case newt to remove the comment "reptile" with conditions:
      | aquatic is "true" |
      | legs is "4"       |

    Then the cases should have interpretations as follows:
      | aardvark | mammal    |
      | antelope | mammal    |
      | bass     | fish      |
      | bear     | mammal    |
      | boar     | mammal    |
      | buffalo  | mammal    |
      | calf     | mammal    |
      | carp     | fish      |
      | catfish  | fish      |
      | cavy     | mammal    |
      | cheetah  | mammal    |
      | chicken  | bird      |
      | chub     | fish      |
      | clam     | mollusc   |
      | crab     | mollusc   |
      | crayfish | mollusc   |
      | crow     | bird      |
      | deer     | mammal    |
      | dogfish  | fish      |
      | dolphin  | mammal    |
      | dove     | bird      |
      | duck     | bird      |
      | elephant | mammal    |
      | flamingo | bird      |
      | flea     | insect    |
      | frog     | amphibian |
      | frog2    | amphibian |
      | fruitbat | mammal    |
      | giraffe  | mammal    |
      | girl     | mammal    |
      | gnat     | insect    |
      | goat     | mammal    |
      | gorilla  | mammal    |
      | gull     | bird      |
      | haddock  | fish      |
      | hamster  | mammal    |
      | hare     | mammal    |
      | hawk     | bird      |
      | herring  | fish      |
      | honeybee | insect    |
      | housefly | insect    |
      | kiwi     | bird      |
      | ladybird | insect    |
      | lark     | bird      |
      | leopard  | mammal    |
      | lion     | mammal    |
      | lobster  | mollusc   |
      | lynx     | mammal    |
      | mink     | mammal    |
      | mole     | mammal    |
      | mongoose | mammal    |
      | moth     | insect    |
      | newt     | amphibian |
      | octopus  | mollusc   |
      | oryx     | mammal    |
      | parakeet | bird      |
      | penguin  | bird      |
      | pheasant | bird      |
      | pike     | fish      |
      | piranha  | fish      |
      | pitviper | reptile   |
      | platypus | mammal    |
      | polecat  | mammal    |
      | pony     | mammal    |
      | porpoise | mammal    |
      | puma     | mammal    |
      | pussycat | mammal    |
      | raccoon  | mammal    |
      | reindeer | mammal    |
      | rhea     | bird      |
      | scorpion | mollusc   |
      | seahorse | fish      |
      | seal     | mammal    |
      | sealion  | mammal    |
      | seasnake | reptile   |
      | seawasp  | mollusc   |
      | skimmer  | bird      |
      | skua     | bird      |
      | slowworm | reptile   |
      | slug     | mollusc   |
      | sole     | fish      |
      | sparrow  | bird      |
      | squirrel | mammal    |
      | starfish | mollusc   |
      | stingray | fish      |
      | swan     | bird      |
      | termite  | insect    |
      | toad     | amphibian |
      | tortoise | reptile   |
      | tuatara  | reptile   |
      | tuna     | fish      |
      | vampire  | mammal    |
      | vole     | mammal    |
      | vulture  | bird      |
      | wallaby  | mammal    |
      | wasp     | insect    |
      | wolf     | mammal    |
      | worm     | mollusc   |
      | wren     | bird      |

