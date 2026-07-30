# Roguelite Championship Design

## Run Loop

1. Start with the lowest-rated benchmarked driver and no modifications.
2. Race every circuit in the current championship.
3. Earn experience after each race according to finishing position.
4. On level-up, choose one of three offers or skip it.
5. After the final circuit of championships 1-4, eliminate the two
   competitors with the fewest championship points.
6. Reset championship points, keep surviving loadouts and experience, and
   begin the next championship with two fewer competitors.
7. Championship 5 starts with two finalists. The competitor with the most
   championship points after its final circuit wins the run.

With the default field, the championship sizes are 10, 8, 6, 4, and 2. The run
ends immediately if the player is in the bottom two after championships 1-4.
Championship ties are resolved by the latest race finish, then by stable
vehicle ID.

The player and every rival have independent progression. Rivals earn the same
position-based experience and choose randomly from their own level-up offers.

## Experience

- First place earns 100 XP and last place earns 30 XP.
- Intermediate positions are interpolated evenly between those values.
- Level 1 requires 180 XP. Each later level costs 60 XP more than the previous
  level.
- Every level gained grants one reward choice. Several rewards can be pending
  at once.
- Skipping consumes that reward without changing the loadout.

## Loadout

Each competitor has four fixed slots:

- One driver slot.
- Three modification slots.

Cards have no upgrade levels. Selecting a new driver replaces the current
driver. Selecting a modification with all three modification slots occupied
requires replacing one equipped modification. Duplicate modifications cannot
be equipped.

The active loadout remains available from the in-race HUD. Driver entries show
measured pace, control, consistency, and finish rate rather than a maintained
name or description.

## Tiers

There are five card tiers. Each championship unlocks its matching tier:

| Championship | Highest available tier |
| ---: | ---: |
| 1 | 1 |
| 2 | 2 |
| 3 | 3 |
| 4 | 4 |
| 5 | 5 |

Offers only contain drivers and modifications from unlocked tiers. This makes
later championships materially stronger without scaling vehicle physics
implicitly.

Drivers are sorted from worst to best using generated benchmark metadata. Ten
drivers are divided into two drivers per tier, so the strongest drivers cannot
appear in the first championships. Every run starts with the lowest-rated
driver.

## Driver Benchmarks

Each policy can have
`assets/ai/policies/profileXX/driver_metadata.json`. The generated metadata
contains:

- Policy hash and benchmark version.
- Overall, pace, control, and consistency ratings.
- Finish rate.
- Average fastest lap, average lap, and off-road actions per completed lap.

`tools/rl/generate_driver_metadata.sh` creates missing metadata. Pass
`--force-driver-metadata` after the profile argument to refresh existing
metadata. Starting real training invalidates the trained profile's old metadata
because it no longer describes that policy. Successful training regenerates it unless
`RL_GENERATE_DRIVER_METADATA=0` is set.

If metadata is absent or unreadable at game startup, the policy remains usable
through deterministic ratings based on profile order until a benchmark is
generated.

## Modifications And Synergies

| Tier | Card | Main effect | Synergy |
| ---: | --- | --- | --- |
| 1 | Turbocharger | More acceleration | Aerodynamic Kit adds maximum speed |
| 1 | Countersteer Servo | More control while sliding | Drift Capacitor charges faster |
| 1 | Recovery Differential | Grip and power after returning to the road | Clean Momentum adds recovery speed |
| 2 | Aerodynamic Kit | Less aerodynamic drag | Extends temporary effects |
| 2 | Reinforced Bumper | Less frontal recoil and more push | Kinetic Recycler receives more impact energy |
| 2 | Storm Tires | Retains more grip in rain and snow | Storm Dynamo charges faster |
| 3 | Drift Capacitor | Sustained on-road slip charges a corner-exit boost | Countersteer Servo adds slide control |
| 3 | Draft Receiver | Longer and stronger slipstream | Overtake Injector strengthens the pass burst |
| 4 | Kinetic Recycler | A collision creates temporary acceleration | Reinforced Bumper increases recovery |
| 4 | Clean Momentum | Clean on-road driving builds maximum speed | Recovery Differential adds recovery speed |
| 5 | Overtake Injector | Passing triggers a temporary power burst | Draft Receiver strengthens the burst |
| 5 | Storm Dynamo | Bad weather gradually charges acceleration | Storm Tires increases the charged bonus |

A synergy is active only while both required modifications occupy slots.
Replacing either card removes the synergy immediately.

## Isolation Rules

- RL training never creates progression, presents cards, advances rivals, or
  applies card modifiers.
- Sandbox mode uses base vehicle physics without roguelite modifiers.
- Normal races configure each car from its persistent run loadout when a
  circuit starts.
- Runtime effect state, such as a temporary boost timer, resets between
  circuits.
- Driver choice, modifications, XP, level, championship, survivors, and
  pending rewards are saved for the current run.

## Code Structure

The implementation is under
`core/src/com/github/jbescos/gameplay/roguelite`.

- `RogueliteRun` owns championship tier, offers, and player/rival progression.
- `RogueliteTournament` owns championship ordering, elimination, and final
  winner rules.
- `RogueliteCompetitorProgress` owns XP, level, and pending rewards.
- `RogueliteLoadout` enforces one driver and three modification slots.
- `DriverProfileCatalog` ranks benchmarked drivers and assigns driver tiers.
- `RogueliteCardCatalog` contains immutable modification metadata and tiers.
- `RogueliteCarUpgrades` combines equipped effects and dispatches driving
  events.
- `RogueliteUpgradeEffect` is the stable contract implemented by one class per
  modification.
- `RogueliteEffectFactory` is the registration point between a card ID and its
  runtime effect.
- `RatassGame` coordinates races, progression screens, save transitions, and
  rendering.

## Adding A Modification

1. Add its stable ID to `RogueliteCardId`.
2. Add its title, effect text, tier, and optional synergy to
   `RogueliteCardCatalog`.
3. Implement one focused `RogueliteUpgradeEffect` class.
4. Register that class in `RogueliteEffectFactory`.
5. Add offer, slot, synergy, and modifier assertions to the roguelite tests.

The catalog-registration test fails when a visible modification has no runtime
effect.
