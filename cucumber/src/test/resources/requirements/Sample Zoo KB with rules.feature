Feature: A sample KB can be created that has the Zoo Animals cases and rules.
#  @single
  Scenario: Single classification Zoo KB
    Given I start the client application
    And I create a Knowledge Base with the name Zoo based on the "Zoo Animals" sample
    Then the count of the number of cases is 101

    When I select case aardvark
    Then the interpretation should be "mammal"

    When I select case antelope
    Then the interpretation should be "mammal"
And pause
    When I select case bass
    Then the interpretation should be "fish"

    When I select case bear
    Then the interpretation should be "mammal"

    When I select case boar
    Then the interpretation should be "mammal"

    When I select case buffalo
    Then the interpretation should be "mammal"

    When I select case calf
    Then the interpretation should be "mammal"

    When I select case carp
    Then the interpretation should be "fish"

    When I select case catfish
    Then the interpretation should be "fish"

    When I select case cavy
    Then the interpretation should be "mammal"

    When I select case cheetah
    Then the interpretation should be "mammal"

    When I select case chicken
    Then the interpretation should be "bird"

    When I select case chub
    Then the interpretation should be "fish"

    When I select case clam
    Then the interpretation should be "mollusc"

    When I select case crab
    Then the interpretation should be "mollusc"

    When I select case crayfish
    Then the interpretation should be "mollusc"

    When I select case crow
    Then the interpretation should be "bird"

    When I select case deer
    Then the interpretation should be "mammal"

    When I select case dogfish
    Then the interpretation should be "fish"

    When I select case dolphin
    Then the interpretation should be "mammal"

    When I select case dove
    Then the interpretation should be "bird"

    When I select case duck
    Then the interpretation should be "bird"

    When I select case elephant
    Then the interpretation should be "mammal"

    When I select case flamingo
    Then the interpretation should be "bird"

    When I select case flea
    Then the interpretation should be "insect"

    When I select case frog
    Then the interpretation should be "amphibian"

    When I select case frog2
    Then the interpretation should be "amphibian"

    When I select case fruitbat
    Then the interpretation should be "mammal"

    When I select case giraffe
    Then the interpretation should be "mammal"

    When I select case girl
    Then the interpretation should be "mammal"

    When I select case gnat
    Then the interpretation should be "insect"

    When I select case goat
    Then the interpretation should be "mammal"

    When I select case gorilla
    Then the interpretation should be "mammal"

    When I select case gull
    Then the interpretation should be "bird"

    When I select case haddock
    Then the interpretation should be "fish"

    When I select case hamster
    Then the interpretation should be "mammal"

    When I select case hare
    Then the interpretation should be "mammal"

    When I select case hawk
    Then the interpretation should be "bird"

    When I select case herring
    Then the interpretation should be "fish"

    When I select case honeybee
    Then the interpretation should be "insect"

    When I select case housefly
    Then the interpretation should be "insect"

    When I select case kiwi
    Then the interpretation should be "bird"

    When I select case ladybird
    Then the interpretation should be "insect"

    When I select case lark
    Then the interpretation should be "bird"

    When I select case leopard
    Then the interpretation should be "mammal"

    When I select case lion
    Then the interpretation should be "mammal"

    When I select case lobster
    Then the interpretation should be "mollusc"

    When I select case lynx
    Then the interpretation should be "mammal"

    When I select case mink
    Then the interpretation should be "mammal"

    When I select case mole
    Then the interpretation should be "mammal"

    When I select case mongoose
    Then the interpretation should be "mammal"

    When I select case moth
    Then the interpretation should be "insect"

    When I select case newt
    Then the interpretation should be "amphibian"

    When I select case octopus
    Then the interpretation should be "mollusc"

    When I select case opossum
    Then the interpretation should be "mammal"

    When I select case oryx
    Then the interpretation should be "mammal"

    When I select case ostrich
    Then the interpretation should be "bird"

    When I select case parakeet
    Then the interpretation should be "bird"

    When I select case penguin
    Then the interpretation should be "bird"

    When I select case pheasant
    Then the interpretation should be "bird"

    When I select case pike
    Then the interpretation should be "fish"

    When I select case piranha
    Then the interpretation should be "fish"

    When I select case pitviper
    Then the interpretation should be "reptile"

    When I select case platypus
    Then the interpretation should be "mammal"

    When I select case polecat
    Then the interpretation should be "mammal"

    When I select case pony
    Then the interpretation should be "mammal"

    When I select case porpoise
    Then the interpretation should be "mammal"

    When I select case puma
    Then the interpretation should be "mammal"

    When I select case pussycat
    Then the interpretation should be "mammal"

    When I select case raccoon
    Then the interpretation should be "mammal"

    When I select case reindeer
    Then the interpretation should be "mammal"

    When I select case rhea
    Then the interpretation should be "bird"

    When I select case scorpion
    Then the interpretation should be "mollusc"

    When I select case seahorse
    Then the interpretation should be "fish"

    When I select case seal
    Then the interpretation should be "mammal"

    When I select case sealion
    Then the interpretation should be "mammal"

    When I select case seasnake
    Then the interpretation should be "reptile"

    When I select case seawasp
    Then the interpretation should be "mollusc"

    When I select case skimmer
    Then the interpretation should be "bird"

    When I select case skua
    Then the interpretation should be "bird"

    When I select case slowworm
    Then the interpretation should be "reptile"

    When I select case slug
    Then the interpretation should be "mollusc"

    When I select case sole
    Then the interpretation should be "fish"

    When I select case sparrow
    Then the interpretation should be "bird"

    When I select case squirrel
    Then the interpretation should be "mammal"

    When I select case starfish
    Then the interpretation should be "mollusc"

    When I select case stingray
    Then the interpretation should be "fish"

    When I select case swan
    Then the interpretation should be "bird"

    When I select case termite
    Then the interpretation should be "insect"

    When I select case toad
    Then the interpretation should be "amphibian"

    When I select case tortoise
    Then the interpretation should be "reptile"

    When I select case tuatara
    Then the interpretation should be "reptile"

    When I select case tuna
    Then the interpretation should be "fish"

    When I select case vampire
    Then the interpretation should be "mammal"

    When I select case vole
    Then the interpretation should be "mammal"

    When I select case vulture
    Then the interpretation should be "bird"

    When I select case wallaby
    Then the interpretation should be "mammal"

    When I select case wasp
    Then the interpretation should be "insect"

    When I select case wolf
    Then the interpretation should be "mammal"

    When I select case worm
    Then the interpretation should be "mollusc"

    When I select case wren
    Then the interpretation should be "bird"

    And stop the client application
