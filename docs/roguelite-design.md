# Roguelite Card Design

## Run Loop

1. Start a run with no cards.
2. Complete a race.
3. Choose one of three card offers or skip the reward.
4. Carry the selected cards into the next circuit.
5. Repeat until the run ends.

Cards are run-scoped. Starting a new game clears the player's cards and every
rival's cards.

## Card Levels

- The first copy of a card grants level 0.
- Later copies upgrade it to level 1 and then level 2.
- A maximum-level card is removed from the offer pool.
- Offers never contain the same card twice.
- Once the player owns an upgradable card, an upgrade is guaranteed within three
  reward screens.
- Skipping never changes the player's inventory.

The collection button remains available during a normal game and shows each
owned card once with its current level. The selection counter includes both new
cards and level upgrades. The collection is paginated, with page capacity
chosen from the current viewport width, so catalog growth never compresses the
cards into unreadable rows.

## Rival Progression

Every active rival receives one random eligible card after each race. This also
happens when the player skips a reward, so repeatedly skipping has a real cost.
Rival inventories are independent: two rivals can develop different card sets
and levels during the same run.

## Cards And Synergies

Each synergy becomes active as soon as both cards in the pair are owned. The
cards do not need to have matching levels.

| Card | Main effect | Synergy |
| --- | --- | --- |
| Turbocharger | More acceleration and a level-2 redline speed bonus | Aerodynamic Kit increases the redline bonus |
| Aerodynamic Kit | Less aerodynamic drag | Level 2 makes temporary effects fade slower |
| Drift Capacitor | Sustained on-road slip charges a corner-exit boost | Countersteer Servo charges it faster |
| Countersteer Servo | More steering authority and grip while sliding | Drift Capacitor adds more slide control |
| Draft Receiver | Longer and stronger slipstream | Overtake Injector adds more draft strength |
| Overtake Injector | Passing a rival triggers a temporary power burst | Draft Receiver can double a level-2 burst |
| Reinforced Bumper | Less frontal recoil and more push | Kinetic Recycler receives more impact energy |
| Kinetic Recycler | A collision creates a temporary acceleration recovery | Reinforced Bumper increases the recovery |
| Storm Tires | Retains more grip in rain and snow | Storm Dynamo charges faster |
| Storm Dynamo | Weather gradually charges acceleration | Storm Tires increases the charged bonus |
| Clean Momentum | Clean on-road driving builds maximum speed | Recovery Differential adds recovery speed |
| Recovery Differential | A safe return to the road grants grip and power | Clean Momentum adds speed during recovery |

## Isolation Rules

- RL training never presents cards, advances rival inventories, or applies card
  modifiers.
- Sandbox mode also uses base vehicle physics without card modifiers.
- Normal races configure each car from its run inventory when a circuit starts.
- Runtime effect state, such as a temporary boost timer, resets between circuits.
- Card ownership and levels persist until a new game starts.

## Code Structure

The implementation is under
`core/src/com/github/jbescos/gameplay/roguelite`.

- `RogueliteCardCatalog` contains presentation metadata and level descriptions.
- `RogueliteCardInventory` owns level progression and maximum-level checks.
- `RogueliteRun` owns offers and independent player/rival inventories.
- `RogueliteCarUpgrades` combines active modifiers and dispatches driving events.
- `RogueliteUpgradeEffect` is the stable contract implemented by one class per
  card.
- `RogueliteEffectFactory` is the only registration point between a card ID and
  its runtime effect.
- `RatassGame` only coordinates race transitions, supplies driving state, and
  renders card data.

## Adding A Card

1. Add its stable ID to `RogueliteCardId`.
2. Add title, description, level text, and synergy to
   `RogueliteCardCatalog`.
3. Implement one focused `RogueliteUpgradeEffect` class.
4. Register that class in `RogueliteEffectFactory`.
5. Add progression and modifier assertions to the roguelite tests.

The catalog-registration test fails when a catalog card has no runtime effect,
which prevents a visible card from silently doing nothing.
