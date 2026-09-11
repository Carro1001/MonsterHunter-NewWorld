# Deferred work

Things consciously postponed, with enough context to pick them up cold. Nothing here is
"done" or "not needed" — it is "not now", by decision.

Acceptance IDs refer to the matrix in `REVIVAL_HANDOFF.md` section 4.6.

## Deferred to the pre-release / survival phase

### A11 — natural spawning
**Status:** wired but never observed.

A biome spawn entry exists at `data/mhnw/neoforge/biome_modifier/great_izuchi_spawns.json`
(`#minecraft:is_forest`, weight 2, group of 1), and `GreatIzuchi.checkSpawnRules` consults the
`naturalSpawning` server config. Neither has ever been seen to work: every monster so far has been
placed with a spawn egg.

To close it: confirm a monster actually appears without a spawn egg, confirm that flipping
`naturalSpawning` to false stops it, and check rarity and group size feel right rather than
flooding a forest. Spawn weight and biome choice are guesses and should be revisited.

### A10 — navigation scenarios
**Status:** only open flat ground has been exercised.

The creature is 1.6 blocks wide with a step height of 1.0, which is unusual enough that vanilla
ground navigation deserves real coverage: open terrain, an outside corner, a body-wide passage, a
passage narrower than the body, a single-block step, and a target that cannot be reached at all
(which must not produce an unbounded re-path loop).

One concrete hint that this matters: during combat testing the log showed a burst of
`approach: no path` failures when a victim was held just outside attack range. That particular
deadlock is fixed, but it showed vanilla pathing does struggle to route this body to a target
already touching it.

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
