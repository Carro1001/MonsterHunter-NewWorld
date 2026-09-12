# Deferred work

Things consciously postponed, with enough context to pick them up cold. Nothing here is
"done" or "not needed" — it is "not now", by decision.

Acceptance IDs refer to the matrix in `REVIVAL_HANDOFF.md` section 4.6.

## Deferred to the pre-release / survival phase

### Lagiacrus — remaining P5 after the P5a movement baseline (2026-09-11)
**Status:** P5a implemented; maintainer reported build success and 62/63 tests passing, with the
bidirectional shoreline test failing. The narrowed shoreline test and compilation passed after
the correction; no full-suite rerun. See `TEST_PLAN.md` for the native bank-exit limitation.
Spawn egg/summon, ordinary parent health/damage/death, seven native parts, preserved rendering and
land/swim clips, underwater breathing, one native amphibious navigation/control pair and bounded
server pursuit are wired. Pursuit has no outgoing damage, retries at most once per 20 ticks,
abandons after three failed/partial paths or 200 ticks, and waits 100 ticks before retrying.
Target loss, goal stop, death, removal and reload cancel transient pursuit.

The root footprint and seven static hurtboxes are unmeasured provisional design estimates. The
seven locator names are `jawHitbox`, `neckMidHitbox`, `neckBaseHitbox`, `tailBaseHitbox`,
`tailMidHitbox`, `tailLastHitbox`, `tailEndHitbox`. `BoneProbe` is wired for a live range-midpoint
fitting pass across land/swim poses and cardinal headings. No gameplay positions come from bones
or raw pivots. Model alignment, client picking, locomotion playback and shoreline behavior need
live acceptance. Reliable water-to-land bank traversal remains unsupported: with a properly
contained one-deep pool and native swimming look/move controls, the two-block-wide floating root
still stalls at a one-block bank until bounded pursuit expires. Native swimming control has no
jump handling. The shoreline test now requires land-to-water approach and bounded bank-exit
cleanup, not successful bidirectional traversal; no custom movement workaround was added.
Further A10/A12 terrain cases, blocked shorelines and multiplayer/soak gates remain pending.

No named attack or death clip exists. A P5 attack needs newly authored art or explicit approval
for a labelled temporary presentation before a server timeline/volume is implemented. Roar is
not an attack substitute. Vanilla death presentation remains; cosmetic neck/tail smoothing and
balance tuning are later work. Natural spawning is still deferred under A11; no Lagiacrus biome
modifier or spawn placement was added. This packet is not full P5 or release acceptance.

### A11 — natural spawning
**Status:** wired but never observed.

A biome spawn entry exists at `data/mhnw/neoforge/biome_modifier/great_izuchi_spawns.json`
(`#minecraft:is_forest`, weight 2, group of 1), and `GreatIzuchi.checkSpawnRules` consults the
`naturalSpawning` server config. Neither has ever been seen to work: every monster so far has been
placed with a spawn egg.

To close it: confirm a monster actually appears without a spawn egg, confirm that flipping
`naturalSpawning` to false stops it, and check rarity and group size feel right rather than
flooding a forest. Spawn weight and biome choice are guesses and should be revisited.

### Izuchi (small) — attack and death animation
**Status:** genuinely hostile and damaging (ordinary vanilla `MeleeAttackGoal`/`Mob.doHurtTarget`,
no custom timeline), but with no dedicated attack or death clip to present. This is a P4 decision
point per the handoff, not an oversight: the preserved master-branch asset has only idle/sleep/
walk/run. A candidate attack/death set exists on the archived `brain` branch
(`legacy/candidate-art-brain-branch/izuchi.animation.json`), but the handoff's own audit (section
6.1) found those clips reference bone names (`left_shoulder`, `left_ankle`, `mane`, `tailblade`)
absent even from their own paired geometry — they need an actual retargeting pass in a model
editor, not code, before they would play correctly at all.

To close it: either retarget those brain-branch clips against the current geometry (visual review
required, not something to do blind), or obtain a newly authored attack/death clip, or get explicit
approval to reuse an existing clip (e.g. a lunge using `run`) as a labelled temporary presentation.
Whichever is chosen, wire it the same way `GreatIzuchi`/`Toad`/`Flashbug` already do: add the
`RawAnimation`, branch on it in `mainAnim`, and if a real death clip is added, check whether it
needs `getDeathMaxRotation` zeroed the way Great Izuchi's does (only add that override if the new
clip actually fights vanilla's flop the way Great Izuchi's did).

### Rathian — flight, and the real charge/bite/tailwhip/fireball timeline
**Status:** ground-only, genuinely hostile via ordinary vanilla melee (no custom attack volume).
**Hurtbox placement is now resolved**, not deferred: the offline FK solve was confirmed wrong by
screenshots (round one), a hand-eyeballed correction was confirmed still meaningfully wrong by
`BoneProbe` measurement (round two — the neck/head `up` values were still off by close to a full
block even after "nudging in the right direction"), and round three replaced the constructor's
offsets with real measured values read from the maintainer's own `logs/latest.log` (`[rathian]`
bone-probe lines, `debugCombat` on). `BoneProbe` was generalized from Great-Izuchi-only to any
`GeoEntity` to make this possible (`client/BoneProbe.java`); see its class doc and `Rathian`'s own
constructor comment for the exact numbers and how they were derived.

The lesson this leaves behind: a hand-corrected guess in the right *direction* was still off by
close to a full block on `up` — "closer" is not the same as "correct," and only a real measurement
closed the actual gap.

Flight is out of scope for this pass by the handoff's own text for this species ("bounded flight
later in its packet"); nothing flight-related (takeoff/landing states, a flying navigation mode)
exists yet.

The real attack clips (`attack_charge_bite_left/right`, `attack_tailwhip`, the fireball clips,
`attack_backhop`, `attack_backflip_ground/flying`) are not wired to any custom attack volume. While
investigating this, a real bug was found and fixed in the offline FK tooling itself: a keyframed
bone's per-keyframe MoLang formula was being evaluated at the currently-sampled query time instead
of that keyframe's own declared time, which is wrong for any multi-keyframe channel (Bedrock bakes
each keyframe to a fixed number using its own time, then linearly interpolates between those fixed
numbers). Fixing it did not close the gap against Great Izuchi's runtime-measured claw path, though,
so something else about actively-animated, heavily keyframed chains still makes offline solving
untrustworthy for motion, not only this bug. Rathian's attack clips use exactly that pattern (four
real keyframes per bone on the Chest/Neck/Head chain, each carrying its own compound MoLang formula)
where the idle pose that was trusted for the hurtbox offsets uses only plain constants and single
sine waves — the same category of channel that solved Great Izuchi's tail to within 0.05 block of
the runtime-measured truth.

**The first real attack is wired: `RathianCombatGoal`, replacing plain vanilla `MeleeAttackGoal`.**
The reported problem was cadence: vanilla melee closes to contact range and deals damage the instant
it touches, which read as a body-slam that only afterward played a bite clip. `RathianCombatGoal` is
the same windup/active/recovery shape as `GreatIzuchiCombatGoal`: it stops the approach, winds up
while `attack_charge_bite_right` plays (`Rathian.BITE`/`ATTACK_BITE`, synced attack id/age/sequence,
same pattern as Great Izuchi's), and only evaluates a hit volume during the active window.

**The strike volume and phase timing are now a real measured path, not a hand-estimate** — closed,
this round. A live capture (`debugCombat` on, several bites against a Pillager) let the round-one
estimate be checked against the actual `Jaw`-bone motion, and it was wrong on both counts it was
guessed: the jaw doesn't dip close to the body early on, it stays reared up and far out (up 4+,
forward 5.3-6.9) for most of the clip, and only descends toward something reachable in the clip's
last third (age 19-28, up dropping from 4.48 to 0.91, forward settling to 4.3-5.5). `BITE`'s path is
now those four real bucketed keyframes, and the windup/active/recovery split moved to bracket that
descent (18/19-28/29) instead of the middle third. `minRange`/`maxRange` were set from where that
path can actually reach (2.0-6.0), replacing the old 0-3.0 band that let the attack fire from
touching distance, well short of where the real path lands.

Two behaviour fixes landed in the same pass, both from the same live-play report: the goal now stops
approaching once within the bite's own maximum reach instead of closing to touching range and then
reaching backward for the strike, and attack selection was restructured to the same
filter-by-range/discourage-repeat/random-tiebreak shape as `GreatIzuchiCombatGoal`'s (see
`RathianCombatGoal.chooseAttack`) even though there's still one candidate today, per explicit
direction that distance should influence but never guarantee which attack a fight uses once a second
one exists.

Still open: `attack_tailwhip` and the other clips (`attack_charge_bite_left`, the fireball clips,
`attack_backhop`, `attack_backflip_ground/flying`) need the same live-capture treatment as `BITE`
just got, most likely as additional entries in `RathianCombatGoal`'s `ALL` array rather than a new
goal per clip -- the selection machinery is already shaped for that.

`RathianCombatGoal` was written as its own class rather than generalizing `GreatIzuchiCombatGoal`,
deliberately: this is the second real attack-timeline implementation now, exactly the point this
section used to say was worth reconsidering that decision at, but doing so now would mean
restructuring Great Izuchi's already-shipped, player-tested combat at the same time as standing up
Rathian's first cut, with no way to interactively verify the result beyond GameTests. Revisit once
Rathian's own timeline has also seen live play and a second Rathian attack (tailwhip, most likely)
exists to compare against.

The seven hurtbox *offsets* are now measured (see above), but the part *sizes* (width/height) are
still borrowed from the archived hitbox profile nominally named for this species, which itself
points at Rathalos's model/texture files (a copy-paste bug in that old file) — treat sizes as a
same-size-class approximation, not Rathian-specific, until someone reports a box that's clearly the
wrong size rather than the wrong place.

### Rathalos — flight, and the attack timeline (worse off than Rathian's)
**Status:** ground-only, genuinely hostile via ordinary vanilla melee, six hurtboxes now using
Rathian's own real `BoneProbe` measurements as a proxy, on the maintainer's own call ("rathalos and
rathian are almost identical, you can use the numbers for one on the other") rather than waiting for
a Rathalos-specific session — no `[rathalos]` lines have appeared in a log yet, only `[rathian]`,
`[aptonoth]` and `[great_izuchi]`. The two species share the same base skeleton layout
(`BoneProbe.WYVERN_BONES` lists identical bone names for both) and closely similar idle-pose
proportions, so a real measurement for one is a materially better estimate for the other than another
offline solve or hand-eyeballed guess. `BoneProbe` is already wired into `RathalosRenderer` too, so
if a live session ever shows a specific part meaningfully off from this borrowed baseline, that
part's own measured value should replace the borrowed one.

Flight deferred for the same reason as Rathian's, though it undersells this species more: Rathalos
is the more archetypally airborne of the two.

The attack situation is worse than Rathian's, confirmed directly rather than assumed: all four
melee attack clips (`attack_claw_scratch`, `attack_bite`, `attack_airsweep`, `attack_air_fireball`)
reference 14 to 17 bone names each (wing membrane and talon bones, mostly) that do not exist
anywhere in this species' own geometry at all. This is exactly the example the handoff's own audit
named (section 6.1: `rightwinglimb1`, `leftFoot2`). It is not a measurement problem the runtime bone
probe can solve the way Great Izuchi's claw path was fixed — there is no bone to measure, only one
that was never added to the model or was renamed and never reconciled with the animation file. It
needs an actual retargeting pass in a model editor, or a newly authored clip, before any attack
presentation is possible for this species.

To close it: open the geometry and the attack clips together in a model editor, find out whether
the referenced bones were simply renamed (in which case a rename-back or a data patch on the
animation file might fully fix it) or never existed at all (in which case retargeting each clip's
keyframes onto the real skeleton is real authoring work). Only after that is done would a
bone-probe measurement pass make sense, the same order Great Izuchi's attacks were done in.

## Deferred to the polishing phase

### A02 — actual save/quit/reload cycle
**Status:** the part of A02 that is this mod's own responsibility is GameTest-covered
(`reloadCancelsTransientCombatState`); a genuine disk save-quit-reload-reconnect cycle is not, and
cannot be exercised from inside a GameTest structure.

What is checked: `addAdditionalSaveData`/`readAdditionalSaveData` round-trip a mid-fight monster
through NBT and the resulting fresh entity starts idle with a cooldown, matching runtime contract
section 4.3 rule 7 (reload cancels transient combat rather than resuming it), and it ends up with
the same part count it started with, guarding against the previous implementation's part-list bug.

What is not checked: an actual world save, process restart, and rejoin; whether generated resources
or other saved data survive that cycle; multiple entities across a real chunk unload/reload. That
needs a real client/server session, same as A08/A09 below.

### A08 / A09 — two-client and dedicated-server agreement
**Status:** the server-side half is covered; the multi-client half is not, and cannot be from
this environment.

Already true: contact is decided entirely server-side, action state rides synched entity data
rather than a one-shot packet (so a client that starts tracking mid-fight receives the live
action), and the developer overlay draws the same volume the server hits with.

Not yet demonstrated: that two clients connected to a dedicated server agree on health and death,
that a second client joining mid-attack sees the current action rather than restarting an old one,
and that moving a camera away changes nothing about damage or part positions.

Known limitation to verify when this is picked up: a client that starts tracking part way through
an action begins the clip at its first frame rather than seeking to the action's age, so its
presentation can lead contact by up to the 65-tick clip length. Contact is unaffected.

## Balance values that are testing placeholders, not decisions

- `GreatIzuchi.MAX_HEALTH` is 40, deliberately low so a slice dies quickly during development.
  Intended closer to 120.
- `GreatIzuchiCombatGoal.SCRATCH_DAMAGE` is 2.5 per strike, up to 3 strikes.
- Illagers are targeted alongside players, added to make attacks observable from outside a fight.
  Keep or drop deliberately.
