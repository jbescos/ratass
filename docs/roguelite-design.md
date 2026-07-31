# Roguelite Championship Design

## Run Loop

1. Start with the lowest-rated benchmarked driver and no modifications.
2. Race every circuit in the current championship.
3. Earn experience after each race according to finishing position.
4. On level-up, choose one of three offers or defer all pending choices until
   the next level.
5. After the final circuit of championships 1 and 2, eliminate the three
   competitors with the fewest championship points.
6. Reset championship points, keep surviving loadouts and experience, and
   begin the next championship with three fewer competitors.
7. Championship 3 starts with four finalists. The competitor with the most
   championship points after its final circuit wins the run.

With the default field, the championship sizes are 10, 7, and 4. The run
ends immediately if the player is in the bottom three after championships 1 or 2.
Championship ties are resolved by the latest race finish, then by stable
vehicle ID.

The player and every rival have independent progression. Rivals earn the same
position-based experience. They maximize tier gain across the complete loadout
and prefer the weakest slot when gains tie. Driver benchmark rating breaks ties
between otherwise equal upgrades. This prevents repeated marginal driver swaps
from leaving tuning, technique, or gadget slots empty or under-tiered.

## Experience

- First place earns 100 XP and last place earns 30 XP.
- Intermediate positions are interpolated evenly between those values.
- Level 1 requires 180 XP. Each later level costs 60 XP more than the previous
  level.
- Every level gained grants one reward choice. Several rewards can be pending
  at once and are resolved one at a time.
- Waiting for the next level preserves every pending reward and discards only
  the current offers. When another level is earned, fresh options appear and
  the newly earned reward joins the same queue. Choices can therefore be
  carried into a later championship tier, but doing so requires earning a full
  additional level.

## Loadout

Each competitor has four fixed slots:

- Driver.
- Tuning.
- Technique.
- Gadget.

Cards have no upgrade levels. Selecting a card previews it in its matching
slot. Accepting commits the replacement, cancelling restores the displayed
loadout, and Wait for Next Level locks pending rewards until another level is
earned without changing the loadout.
Duplicate modifications cannot be equipped. Every competitor starts with the
benchmarked driver with the highest average lap time in the Driver slot.

The active loadout remains available from the in-race HUD. Driver entries show
average lap time, maximum recorded speed, average off-road percentage, and
average drift percentage rather than a maintained name or description. Driver
tiers are ordered by average lap time: slower drivers appear in early tiers and
the lowest average lap times appear in later tiers.

## Tiers

There are three card tiers. A new run selects its starting tier. Each later
championship advances the offered tier by one, capped at tier 3:

| Starting tier | Championship offers |
| ---: | :--- |
| 1 | 1, 2, 3 |
| 2 | 2, 3, 3 |
| 3 | 3, 3, 3 |

Offers contain only drivers and modifications from the current championship's
tier. This prevents lower-tier options from consuming the limited choices in a
later championship.

Drivers are sorted from worst to best using generated benchmark metadata. Ten
drivers are distributed across the three tiers. Every run starts with the
lowest-rated driver, while the selected starting tier determines which driver
and modification upgrades can first be offered.

## Driver Benchmarks

Each policy can have
`assets/ai/policies/profileXX/driver_metadata.json`. The generated metadata
contains:

- Policy hash and benchmark version.
- Overall, pace, control, and consistency ratings.
- Finish rate.
- Average fastest lap, average lap, off-road actions per completed lap, and
  on-road drift percentage.

`tools/rl/generate_driver_metadata.sh` creates missing metadata. Pass
`--force-driver-metadata` after the profile argument to refresh existing
metadata. Starting real training invalidates the trained profile's old metadata
because it no longer describes that policy. Successful training regenerates it unless
`RL_GENERATE_DRIVER_METADATA=0` is set.

If metadata is absent or unreadable at game startup, the policy remains usable
through deterministic ratings based on profile order until a benchmark is
generated.

## Card Families

Tuning cards are permanent, predictable setups. They include lightweight mass,
reinforced heavy-contact, and low-drag aerodynamic choices alongside balanced
power, top-speed, and grip packages. They deliberately do not change braking.

Technique cards reward driving events rather than running on a timer. The
catalog includes corner-exit launches, drafting, clean momentum, road recovery,
drift and slipstream releases, overtaking, fast apexes, perfect laps, and
combined racecraft. A failed event provides no benefit.

Gadgets are visible, automatic abilities with contextual triggers and
cooldowns. Nitro, rockets, overdrive, and hyperdrive wait for open straights;
grip devices wait for corners; draft devices wait for a rival ahead; shields
wait for close traffic; and the Ram Reactor arms only when a rival is close in
front. Triggering starts the cooldown, so a gadget cannot fire continuously.

There are no explicit card-pair IDs or hidden combination bonuses. Equipped
effects act on the same car and can combine naturally, making useful loadouts
emerge from their behavior instead of from maintained pairing rules.

Every modification has dedicated artwork in
`assets/roguelite/cards/card_art_atlas.png`. Drivers continue to use their car
image. The atlas is loaded lazily by presentation code.

## Isolation Rules

- RL training never creates progression, presents cards, advances rivals, or
  applies card modifiers.
- Card artwork and gadget animation state are created and updated only while
  presentation is enabled.
- Sandbox mode applies the four-card loadout configured from its card catalog.
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
- `RogueliteLoadout` enforces one card per typed slot.
- `DriverProfileCatalog` ranks benchmarked drivers and assigns driver tiers.
- `RogueliteCardCatalog` contains immutable modification metadata and tiers.
- `RogueliteCarUpgrades` combines equipped effects and dispatches driving
  events.
- `RogueliteUpgradeEffect` is the stable runtime effect contract.
- `TieredTuningEffect`, `RaceTechniqueEffect`, and `CooldownGadgetEffect`
  implement the three card families.
- `RogueliteEffectFactory` dispatches catalog IDs to those family effects.
- `GadgetActivationVisual` contains presentation-only gadget animation state.
- `RatassGame` coordinates races, progression screens, save transitions, and
  rendering.

## Adding A Modification

1. Add its stable ID to `RogueliteCardId`.
2. Add its title, effect text, tier, slot category, visual style, and unused
   artwork index to
   `RogueliteCardCatalog`.
3. Add the card image to the matching atlas cell documented beside the asset.
4. Extend the matching family effect and factory dispatch.
5. Add offer, slot, trigger, cooldown, and modifier assertions to the roguelite
   tests.

The catalog tests fail when a visible modification has no runtime effect or
when two cards share an artwork index.
