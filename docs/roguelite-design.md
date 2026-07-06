# Roguelite Racing Design

## Design Direction

The roguelite is about developing a driving style during a run. The player drives
the car, earns experience from meaningful race actions, and commits to one of
three skill trees: Velocity, Outlaw, or Flow.

This replaces the earlier team-owner and management-economy proposal. Cars keep
the same base physics so the build comes from skills and abilities rather than
from incompatible car-stat combinations.

## Run Loop

1. Start with the standard car and no specialization.
2. Choose between branching races, weather conditions, and unusual events.
3. Earn base and style experience during each race.
4. Level up and choose skills between races or at safe points.
5. Commit to one skill tree early in the run.
6. Find and upgrade active abilities that interact with that tree.
7. Complete the final race with the build created during the run.

Experience, levels, skills, and ability upgrades reset after a run. Permanent
progression should primarily unlock new skill choices, abilities, events, and
cosmetics. Permanent raw-stat bonuses would make later runs automatically easier
and weaken build decisions.

## Progression Rules

- The first two levels offer neutral choices and let the player test the run.
- At approximately level 3, the player commits to one tree.
- A committed player cannot buy nodes from the other two trees during that run.
- Each tree should contain approximately 12 to 15 nodes over several tiers.
- Branches, prerequisites, and mutually exclusive choice nodes create multiple
  builds within the same specialization.
- A run should provide enough points to complete one branch and part of another,
  but not enough to purchase the entire tree.
- The player has two active ability slots. Abilities are separate from skill
  trees, but the selected tree changes how they work.

## Experience

Every build receives base experience for race outcomes:

- Forward route progress.
- Completing laps and races.
- Gaining and holding positions.
- Finishing on the podium and winning.
- Completing event-specific objectives.

The selected tree grants additional style experience. Style experience should
make the chosen behavior level faster without making the race objective
irrelevant.

## Velocity Tree

Velocity is for drivers who win through lap time, clean overtakes, and preserved
momentum.

### Style Experience

- Completing a lap faster than the run's current reference time.
- Maintaining high speed while making positive route progress.
- Drafting and then completing a clean overtake.
- Exiting a corner with high speed.
- Linking corners without collisions or leaving the road.

### Branches

- **Top Speed:** higher maximum speed, stronger drafting, and longer boosts.
- **Apex:** better corner-exit acceleration and less speed loss while turning.
- **Momentum:** bonuses that grow while avoiding collisions and off-road driving.

### Example Skills

- **Launch Control:** stronger acceleration at race starts and after resets.
- **Draft Hunter:** following an opponent charges the next acceleration burst.
- **Overtake Surge:** a confirmed clean overtake grants temporary power.
- **Late Braker:** braking near a corner increases exit acceleration if the car
  remains on the road.
- **Clean Streak:** consecutive clean corners increase experience and top speed.

### Abilities

- **Overdrive:** temporarily increases power and top speed but makes braking more
  difficult.
- **Slipstream Burst:** stores energy while drafting and releases it when changing
  lane or overtaking.
- **Perfect Launch:** provides a short, powerful acceleration window.

### Capstone

**Redline:** sustained high-speed progress gradually increases acceleration and
experience gain. A collision, strong slowdown, or off-road excursion resets the
effect.

## Outlaw Tree

Outlaw is for drivers who win through contact, blocking, intimidation, and race
disruption.

### Style Experience

- Making an opponent lose a position through contact.
- Blocking a genuine overtake attempt.
- Ramming without losing significant momentum.
- Forcing an opponent away from its preferred racing line.
- Retaliating against a car that recently hit the player.

### Branches

- **Rammer:** stronger impacts, reduced recoil, and frontal-contact bonuses.
- **Defender:** better blocking, more stability, and resistance to being pushed.
- **Saboteur:** more frequent and effective disruptive abilities.

### Example Skills

- **Reinforced Nose:** frontal impacts transfer more momentum to the target.
- **Hold the Door:** staying ahead of a nearby opponent builds defensive strength.
- **Cheap Shot:** hitting a target from the side briefly reduces its grip.
- **Vendetta:** the last opponent to hit the player becomes marked for bonus
  experience and ability charge.
- **Scrap Collector:** meaningful impacts recover part of a defensive ability.

### Abilities

- **Ram Charge:** temporarily reinforces the car and increases collision impulse.
- **Oil Slick:** creates a temporary low-grip area behind the player.
- **Jammer:** briefly reduces nearby opponents' acceleration or steering control.
- **Revenge:** empowers the next hit against the marked opponent.

### Capstone

**No Rules:** successful disruptive actions restore ability charge and steal some
of the victim's momentum.

## Flow Tree

Flow is for drivers who use controlled drifting, technical car control, and
difficult conditions to maintain speed through corners.

### Style Experience

- Controlled on-road drifting with positive route progress.
- Linking several corners into one drift chain.
- Drifting close to a road edge without crossing it.
- Recovering from a slide without leaving the road.
- Performing well in rain, snow, or other low-grip events.

### Branches

- **Drift:** better speed retention and ability generation from slip angle.
- **Control:** stronger counter-steering, rotation control, and recovery.
- **Adaptation:** reduced weather penalties and bonuses in unusual events.

### Example Skills

- **Tire Heat:** controlled slip builds a temporary grip and acceleration resource.
- **Exit Boost:** straightening after a valid drift converts stored energy into
  acceleration.
- **Countersteer:** reduces unwanted rotation while the car is sliding.
- **Edge Dancer:** controlled drifting near a road edge grants extra experience.
- **All Weather:** rain and snow reduce grip less and generate more style charge.

### Abilities

- **Handbrake Turn:** initiates a controlled rotation without immediately losing
  all speed.
- **Drift Boost:** stores energy while sliding and releases it when the car
  straightens.
- **Focus:** briefly improves steering precision and rotation control.
- **Recovery:** rapidly restores grip after a dangerous slide.

### Capstone

**Flow State:** linked controlled corners progressively increase handling,
acceleration, and experience until the chain is broken.

## Ability Synergies

Abilities are shared rewards, but every tree should modify them differently.
This creates synergies without allowing players to purchase nodes from multiple
trees.

Examples:

| Ability | Velocity | Outlaw | Flow |
| --- | --- | --- | --- |
| Nitro | Longer and faster | Creates an impact shockwave | Recharges through drifting |
| Shield | Prevents momentum loss | Reflects collision recoil | Protects the drift chain |
| Recovery | Restores speed quickly | Grants temporary armor | Restores grip and rotation control |
| Jammer | Helps complete an overtake | Stronger disruption | Creates a low-grip opening for a drift |

Skills within a tree should also form explicit combinations:

- Draft Hunter charges Slipstream Burst; Overtake Surge rewards its successful use.
- Tire Heat stores drift energy; Exit Boost converts it into straight-line speed.
- Hold the Door arms Ram Charge; Scrap Collector helps recharge it after contact.

## Experience Validation

Experience must reward useful outcomes rather than actions that can be farmed.

- An overtake counts only after the player remains ahead for a short confirmation
  period.
- Drift experience requires sufficient speed, positive progress, a meaningful
  slip angle, and contact with the road.
- Each corner has a limited drift-experience budget.
- Collision experience requires the opponent to lose speed, progress, or position.
- Self-destructive contact should grant little or no experience.
- Repeated hits against the same opponent have diminishing returns.
- Blocking experience requires forward progress and a nearby opponent that was
  genuinely attempting to pass.
- High-speed experience stops when driving in circles or repeating the same route
  section.

Style experience rates should be normalized around expected experience per minute
so no tree levels substantially faster merely because its trigger occurs more
frequently.

## Integration Constraints

- Skills and abilities should initially affect only the player during normal
  gameplay.
- Base vehicle physics remain unchanged across visual car choices.
- RL training and lap-time evaluation remain free from roguelite modifiers.
- Existing AI policies should not need retraining to introduce player progression.
- Weather and event modifiers can interact with skills, but skill effects should
  be applied as temporary gameplay modifiers rather than permanent physics changes.
- New systems should expose explicit events such as confirmed overtake, valid
  drift, successful block, and meaningful impact. XP should consume those events
  instead of independently approximating race behavior in several places.

## First Implementable Version

1. Add run-scoped experience and levels.
2. Add one short branch with three passive nodes per tree.
3. Commit the player to a tree when the first specialization node is selected.
4. Implement confirmed overtake, valid drift, and meaningful-impact events.
5. Add one active ability per tree: Overdrive, Ram Charge, and Drift Boost.
6. Present one skill choice after each race.
7. Reset the build when the run ends.

This is enough to validate whether the three driving styles create meaningfully
different runs before building full trees, meta-progression, or a large ability
pool.
