# Roguelite Championship Design

## Run Loop

1. Start with a random Tier 1 driver and no modifications.
2. Race five laps on every circuit in one championship.
3. Earn experience from racecraft while driving and from finishing position.
4. On level-up, pause the race and choose one of three offers. Skip discards
   that level's reward and immediately resumes XP progression.
5. The competitor with the most championship points after the final circuit
   wins. Every other competitor loses.
6. From the result screen, Continue starts another shuffled championship while
   retaining every competitor's level, experience, driver, and cards.

The default championship contains ten cars. Championship ties are resolved by
the latest race finish, then by stable vehicle ID. There is no championship
winner card or level bonus because the run has already ended.

The player and every rival have independent progression. Rivals earn the same
position-based experience. They maximize tier gain across the complete loadout
and prefer the weakest slot when gains tie. Driver benchmark rating breaks ties
between otherwise equal upgrades. This prevents repeated marginal driver swaps
from leaving tuning, technique, powerup, or revenge slots empty or under-tiered.

## Experience

- Every upward change in the live race standings earns 2 XP per position gained.
- Setting a new race-fastest lap earns 6 XP.
- Executing a revenge effect earns 4 XP.
- Hitting a rival and pushing it from the road earns 6 XP.
- Sustaining an on-road drift earns 1 XP per second.
- Racecraft awards are capped at 30 XP per car and lap. The HUD shows this
  budget beside the general level XP bar. The cap is less than half of even
  the first level requirement and requires an unusually active lap to fill.
- Custom runs can set the per-lap racecraft cap from 5 to 200 XP.
- Racecraft XP stops immediately after a competitor completes its final lap.
- First place earns 100 XP and last place earns 30 XP at race end.
- Intermediate positions are interpolated evenly between those values.
- Level 1 requires 80 XP. Each later level costs 2 XP more than the previous
  level.
- Every level gained grants one reward choice. Only one reward can be pending.
- While a reward is pending, XP progression is paused and another level cannot
  be earned.
- Skipping discards the current reward. The next level generates fresh offers.
- Legacy saves containing queued or postponed rewards are normalized to one
  immediately available choice.

## Loadout

Each competitor has five fixed slots:

- Driver.
- Tuning.
- Technique.
- Powerup.
- Revenge.

Cards have no upgrade levels. Selecting a card previews it in its matching
slot. Accepting commits the replacement, cancelling restores the displayed
loadout, and Skip discards the current reward without changing the loadout.
Duplicate modifications cannot be equipped. Every competitor starts with the
benchmarked driver with the highest average lap time in the Driver slot.

The active loadout remains available from the in-race HUD. Driver entries show
average lap time, maximum recorded speed, average off-road percentage, and
average drift percentage rather than a maintained name or description. Driver
tiers are ordered by average lap time: slower drivers appear in early tiers and
the lowest average lap times appear in later tiers.

## Tiers

There are three card tiers. Each competitor unlocks tiers independently from
its level: Tier 1 before level 12, Tier 2 from level 12, and Tier 3 from level
19. This provides ten Tier 1 choices and seven Tier 2 choices before advancing;
a representative default championship reaches about five Tier 3 choices before
the final result. A Tier 2 or Tier 3 New Game choice raises the minimum offered
tier without changing the XP curve or level gates. Offers contain only drivers
and modifications from the competitor's currently unlocked tier.

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
reinforced heavy-contact, and aero-efficiency choices alongside balanced
power, top-speed, and grip packages. They deliberately do not change braking.

Technique cards reward driving events rather than running on a timer. The
catalog includes corner-exit launches, drafting, clean momentum, road recovery,
drift and slipstream releases, overtaking, fast apexes, perfect laps, and
combined racecraft. A failed event provides no benefit.

Powerups are visible, automatic abilities with contextual triggers and
cooldowns. Nitro, rockets, overdrive, and hyperdrive wait for open straights;
grip devices wait for corners; draft devices wait for a rival ahead; shields
wait for close traffic; and the Ram Reactor arms only when a rival is close in
front. Triggering starts the cooldown, so a powerup cannot fire continuously.

There are no explicit card-pair IDs or hidden combination bonuses. Equipped
effects act on the same car and can combine naturally, making useful loadouts
emerge from their behavior instead of from maintained pairing rules.

Every modification has dedicated artwork in
`assets/roguelite/cards/card_art_atlas.png`. Drivers continue to use their car
image. The atlas is loaded lazily by presentation code.

## Isolation Rules

- RL training never creates progression, presents cards, advances rivals, or
  applies card modifiers.
- Card artwork and ability animation state are created and updated only while
  presentation is enabled.
- Sandbox mode applies the five-slot loadout configured from its card catalog.
- Normal races configure each car from its persistent run loadout when a
  circuit starts.
- Runtime effect state, such as a temporary boost timer, resets between
  circuits.
- Competition mode, driver choice, modifications, XP, level, progression
  cycle, roster, pending rewards, and whether a reward resumes the active race
  are saved for the current run.

## Code Structure

The implementation is under
`core/src/com/github/jbescos/gameplay/roguelite`.

- `RogueliteRun` owns level-based tiers, offers, and player/rival progression.
- `RogueliteExperienceAwards` defines racecraft XP amounts and thresholds.
- `RogueliteTournament` owns championship ordering and winner rules.
- `RogueliteCompetitorProgress` owns XP, level, and pending rewards.
- `RogueliteLoadout` enforces one card per typed slot.
- `DriverProfileCatalog` ranks benchmarked drivers and assigns driver tiers.
- `RogueliteCardCatalog` contains immutable modification metadata and tiers.
- `RogueliteCarUpgrades` combines equipped effects and dispatches driving
  events.
- `RogueliteUpgradeEffect` is the stable runtime effect contract.
- `TieredTuningEffect`, `RaceTechniqueEffect`, `CooldownPowerupEffect`, and
  `CooldownRevengeEffect` implement the four modification families.
- `RogueliteEffectFactory` dispatches catalog IDs to those family effects.
- `AbilityActivationVisual` contains presentation-only ability animation state.
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
