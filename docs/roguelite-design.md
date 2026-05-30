# Roguelite Racing Design Notes

## Core Fantasy

The player is the owner of a racing brand/team, not the driver. Cars drive
themselves through trained AI personalities. The player's job is to manage money,
drivers, car development, repairs, and race strategy across a season/run.

## Core Loop

1. Choose a race, driver, car setup, and sponsor objective.
2. Watch the AI race.
3. Earn money, reputation, research data, parts, and sponsor bonuses.
4. Pay ongoing costs: driver salary, repairs, entry fees, and maintenance.
5. Choose upgrades or hires before the next race.
6. Continue through a short championship season.

The run should create pressure because earning money is not enough; the team also
has to spend money just to stay competitive.

## Economy

Money sources:

- Race placement rewards.
- Sponsor bonuses.
- Rival/team challenge bonuses.
- Selling old parts.
- Championship prizes.

Money sinks:

- Driver salary per race.
- Car repairs after damage.
- Maintenance costs for engine, tires, brakes, and chassis.
- Research projects.
- New parts.
- Entry fees for higher-tier races.
- Facility upkeep once team infrastructure exists.

Important design goal: expensive choices should feel powerful but create risk.
For example, an expert driver may win more often, but the salary can bankrupt a
weak team if results are bad.

## Drivers

Drivers are AI personalities plus cost and reliability.

Possible traits:

- Rookie: cheap, inconsistent, lower salary.
- Expert: strong results, high salary.
- Aggressive: overtakes and collides more.
- Clean: fewer collisions, safer, maybe slower.
- Qualifier: very fast alone, weaker in traffic.
- Veteran: consistent, expensive, low mistake rate.
- Reckless: high ceiling, high repair bills.

Driver costs:

- Signing fee.
- Salary per race.
- Optional morale/contract renewal later.

## Car Development

Car stats should become roguelite build variables:

- Engine power / acceleration.
- Top speed.
- Braking strength.
- Grip.
- Mass.
- Collision strength.
- Aerodynamic drag.
- High-speed stability.
- Off-road slowdown/recovery.

Upgrades can have tradeoffs:

- More horsepower but harder control.
- Stronger chassis but heavier car.
- Better grip but lower top speed.
- Better brakes but higher maintenance.

## Research

Research should offer semi-random upgrades, so every run feels different.

Example choices:

- Engine prototype: +8% acceleration, +2% top speed, +3% maintenance cost.
- Brake compound: +12% brake strength, -4% tire durability.
- Lightweight chassis: -6% mass, -8% collision strength.
- Aero kit: +10% high-speed stability, -3% acceleration.

Research resources:

- Money.
- Race data.
- Reputation gates for better tiers.

## Parts And Loot

Races can award parts:

- Common: small stat boosts.
- Rare: larger stat boosts with downside.
- Legendary: strong effect with major tradeoff.
- Damaged/cheap parts: useful early, risky later.

Parts should be replaceable and sellable.

## Damage And Maintenance

Damage creates economic pressure:

- Engine damage reduces acceleration/top speed.
- Brake damage reduces braking.
- Tire damage reduces grip.
- Chassis damage affects collision recovery/stability.

After each race, the player chooses:

- Repair fully.
- Patch cheaply.
- Skip repair and risk performance loss.
- Replace a part.

## Sponsors

Sponsors create short-term goals and change strategy:

- Finish top 5.
- Beat a named rival.
- Complete race with low collision count.
- Reach a speed target.
- Make aggressive overtakes.
- Finish with low repair cost.

Sponsors can pay more for riskier objectives.

## Facilities

Longer-term unlocks:

- Garage: reduces repair cost.
- Simulator: improves driver consistency.
- Wind tunnel: unlocks aero upgrades.
- Research lab: better upgrade choices.
- Scouting office: better driver offers.
- Data center: better post-race analysis.

Facilities should have upkeep costs so expanding too fast is dangerous.

## Rival Teams

Rivals give the season personality:

- Fast but fragile team.
- Aggressive contact-heavy team.
- Clean high-consistency team.
- Rich team with expensive drivers.
- Underdog team that improves over time.

Rival behavior can map to different trained policy profiles.

## First Implementable Version

Start small:

- One car owned by the player team.
- Hire one driver from a small list.
- Run a 5-race season.
- Earn money based on placement.
- Pay driver salary and repair/maintenance costs after each race.
- Choose one of three random upgrades between races.
- Car stats affect physics.
- Driver choice selects an AI policy/personality.

This gives a real roguelite loop without needing a full management sim first.

## RL Implications

Future policies should observe normalized car stats:

- top speed
- acceleration
- brake strength
- grip
- mass
- aero/drag
- turn authority

Training should randomize those stats across the expected upgrade range, so the
same policy can adapt to weak, normal, and upgraded cars.
