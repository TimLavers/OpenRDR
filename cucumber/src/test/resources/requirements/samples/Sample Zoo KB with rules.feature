Feature: A sample KB can be created that has the Zoo Animals cases and rules.
   Scenario: Single classification Zoo KB
    Given I start the client application
    And I create a Knowledge Base with the name Zoo based on the "Zoo Animals" sample
    Then the count of the number of cases is 101

    When I select case aardvark on the processed case list
    Then the interpretation should be "mammal"

    When I select case antelope on the processed case list
    Then the interpretation should be "mammal"

    When I select case bass on the processed case list
    Then the interpretation should be "fish"

    When I select case bear on the processed case list
    Then the interpretation should be "mammal"

    When I select case boar on the processed case list
    Then the interpretation should be "mammal"

    When I select case buffalo on the processed case list
    Then the interpretation should be "mammal"

    When I select case calf on the processed case list
    Then the interpretation should be "mammal"

    When I select case carp on the processed case list
    Then the interpretation should be "fish"

    When I select case catfish on the processed case list
    Then the interpretation should be "fish"

    When I select case cavy on the processed case list
    Then the interpretation should be "mammal"

    When I select case cheetah on the processed case list
    Then the interpretation should be "mammal"

    When I select case chicken on the processed case list
    Then the interpretation should be "bird"

    When I select case chub on the processed case list
    Then the interpretation should be "fish"

    When I select case clam on the processed case list
    Then the interpretation should be "mollusc"

    When I select case crab on the processed case list
    Then the interpretation should be "mollusc"

    When I select case crayfish on the processed case list
    Then the interpretation should be "mollusc"

    When I select case crow on the processed case list
    Then the interpretation should be "bird"

    When I select case deer on the processed case list
    Then the interpretation should be "mammal"

    When I select case dogfish on the processed case list
    Then the interpretation should be "fish"

    When I select case dolphin on the processed case list
    Then the interpretation should be "mammal"

    When I select case dove on the processed case list
    Then the interpretation should be "bird"

    When I select case duck on the processed case list
    Then the interpretation should be "bird"

    When I select case elephant on the processed case list
    Then the interpretation should be "mammal"

    When I select case flamingo on the processed case list
    Then the interpretation should be "bird"

    When I select case flea on the processed case list
    Then the interpretation should be "insect"

    When I select case frog on the processed case list
    Then the interpretation should be "amphibian"

    When I select case frog2 on the processed case list
    Then the interpretation should be "amphibian"

    When I select case fruitbat on the processed case list
    Then the interpretation should be "mammal"

    When I select case giraffe on the processed case list
    Then the interpretation should be "mammal"

    When I select case girl on the processed case list
    Then the interpretation should be "mammal"

    When I select case gnat on the processed case list
    Then the interpretation should be "insect"

    When I select case goat on the processed case list
    Then the interpretation should be "mammal"

    When I select case gorilla on the processed case list
    Then the interpretation should be "mammal"

    When I select case gull on the processed case list
    Then the interpretation should be "bird"

    When I select case haddock on the processed case list
    Then the interpretation should be "fish"

    When I select case hamster on the processed case list
    Then the interpretation should be "mammal"

    When I select case hare on the processed case list
    Then the interpretation should be "mammal"

    When I select case hawk on the processed case list
    Then the interpretation should be "bird"

    When I select case herring on the processed case list
    Then the interpretation should be "fish"

    When I select case honeybee on the processed case list
    Then the interpretation should be "insect"

    When I select case housefly on the processed case list
    Then the interpretation should be "insect"

    When I select case kiwi on the processed case list
    Then the interpretation should be "bird"

    When I select case ladybird on the processed case list
    Then the interpretation should be "insect"

    When I select case lark on the processed case list
    Then the interpretation should be "bird"

    When I select case leopard on the processed case list
    Then the interpretation should be "mammal"

    When I select case lion on the processed case list
    Then the interpretation should be "mammal"

    When I select case lobster on the processed case list
    Then the interpretation should be "mollusc"

    When I select case lynx on the processed case list
    Then the interpretation should be "mammal"

    When I select case mink on the processed case list
    Then the interpretation should be "mammal"

    When I select case mole on the processed case list
    Then the interpretation should be "mammal"

    When I select case mongoose on the processed case list
    Then the interpretation should be "mammal"

    When I select case moth on the processed case list
    Then the interpretation should be "insect"

    When I select case newt on the processed case list
    Then the interpretation should be "amphibian"

    When I select case octopus on the processed case list
    Then the interpretation should be "mollusc"

    When I select case opossum on the processed case list
    Then the interpretation should be "mammal"

    When I select case oryx on the processed case list
    Then the interpretation should be "mammal"

    When I select case ostrich on the processed case list
    Then the interpretation should be "bird"

    When I select case parakeet on the processed case list
    Then the interpretation should be "bird"

    When I select case penguin on the processed case list
    Then the interpretation should be "bird"

    When I select case pheasant on the processed case list
    Then the interpretation should be "bird"

    When I select case pike on the processed case list
    Then the interpretation should be "fish"

    When I select case piranha on the processed case list
    Then the interpretation should be "fish"

    When I select case pitviper on the processed case list
    Then the interpretation should be "reptile"

    When I select case platypus on the processed case list
    Then the interpretation should be "mammal"

    When I select case polecat on the processed case list
    Then the interpretation should be "mammal"

    When I select case pony on the processed case list
    Then the interpretation should be "mammal"

    When I select case porpoise on the processed case list
    Then the interpretation should be "mammal"

    When I select case puma on the processed case list
    Then the interpretation should be "mammal"

    When I select case pussycat on the processed case list
    Then the interpretation should be "mammal"

    When I select case raccoon on the processed case list
    Then the interpretation should be "mammal"

    When I select case reindeer on the processed case list
    Then the interpretation should be "mammal"

    When I select case rhea on the processed case list
    Then the interpretation should be "bird"

    When I select case scorpion on the processed case list
    Then the interpretation should be "mollusc"

    When I select case seahorse on the processed case list
    Then the interpretation should be "fish"

    When I select case seal on the processed case list
    Then the interpretation should be "mammal"

    When I select case sealion on the processed case list
    Then the interpretation should be "mammal"

    When I select case seasnake on the processed case list
    Then the interpretation should be "reptile"

    When I select case seawasp on the processed case list
    Then the interpretation should be "mollusc"

    When I select case skimmer on the processed case list
    Then the interpretation should be "bird"

    When I select case skua on the processed case list
    Then the interpretation should be "bird"

    When I select case slowworm on the processed case list
    Then the interpretation should be "reptile"

    When I select case slug on the processed case list
    Then the interpretation should be "mollusc"

    When I select case sole on the processed case list
    Then the interpretation should be "fish"

    When I select case sparrow on the processed case list
    Then the interpretation should be "bird"

    When I select case squirrel on the processed case list
    Then the interpretation should be "mammal"

    When I select case starfish on the processed case list
    Then the interpretation should be "mollusc"

    When I select case stingray on the processed case list
    Then the interpretation should be "fish"

    When I select case swan on the processed case list
    Then the interpretation should be "bird"

    When I select case termite on the processed case list
    Then the interpretation should be "insect"

    When I select case toad on the processed case list
    Then the interpretation should be "amphibian"

    When I select case tortoise on the processed case list
    Then the interpretation should be "reptile"

    When I select case tuatara on the processed case list
    Then the interpretation should be "reptile"

    When I select case tuna on the processed case list
    Then the interpretation should be "fish"

    When I select case vampire on the processed case list
    Then the interpretation should be "mammal"

    When I select case vole on the processed case list
    Then the interpretation should be "mammal"

    When I select case vulture on the processed case list
    Then the interpretation should be "bird"

    When I select case wallaby on the processed case list
    Then the interpretation should be "mammal"

    When I select case wasp on the processed case list
    Then the interpretation should be "insect"

    When I select case wolf on the processed case list
    Then the interpretation should be "mammal"

    When I select case worm on the processed case list
    Then the interpretation should be "mollusc"

    When I select case wren on the processed case list
    Then the interpretation should be "bird"

