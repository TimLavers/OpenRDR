@ignore
Feature: The Zoo KB can be built with the user interface.

  Scenario: Build the Zoo KB from cases
    Given I start the client application
    And I create a Knowledge Base with the name Zoo based on the "Zoo Animals - cases only" sample
    Then the count of the number of cases is 101

    And I select case aardvark
    And I build a rule to add the comment "mammal" with conditions
      | milk is "true" |

    And I select case bass
    And I build a rule to add the comment "fish" with conditions
      | aquatic is "true" |

    And I select case chicken
    And I build a rule to add the comment "bird" with conditions
      | feathers is "true" |

    And I select case clam
    And I build a rule to add the comment "mollusc" with conditions
      | backbone is "false" |
      | breathes is "false" |
      | legs is "0"         |

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
      | newt     | mammal    |
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
      | squirrel | mollusc   |
      | slug     | mammal    |
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

    And stop the client application
