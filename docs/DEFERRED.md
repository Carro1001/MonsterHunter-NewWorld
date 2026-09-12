# Deferred work

Things consciously postponed, with enough context to pick them up cold. Nothing here is
"done" or "not needed" — it is "not now", by decision.

Acceptance IDs refer to the matrix in `REVIVAL_HANDOFF.md` section 4.6.

## Found during R2 (2026-09-12), deferred out of that packet

### The two Flashbug items wear temporary vanilla sprites
**Status:** deliberate, and named in the R2 packet itself. `mhnw:bottled_flashbug` uses
`minecraft:item/experience_bottle` and `mhnw:flash_bomb` uses `minecraft:item/firework_star`. There
is no accepted flash-bomb or bottled-bug icon in the preserved art, and `fulgurbug.png` is
deliberately **not** repurposed — it is reserved for its own later material identity. Replacing
these is an art task, not a code one; the model JSONs are one line each to repoint. Whether the two
read as distinct from each other and from existing items is an open human gate in
`docs/TEST_PLAN.md`.

### No antidote, ailment engine or input-lock semantics
**Status:** scheduled elsewhere, not skipped. Roadmap Q17 puts the MH antidote with Rathian in R4,
and Q18's player-control-loss rule is explicitly not a reason to redesign today's non-locking
vanilla slowdown/confusion stand-ins. R2 deliberately added neither. The four toad effects and their
durations are unchanged from P3.

### The flash helper is a helper, not an effect system
**Status:** deliberate. `FlashEffect` is one static method with two real callers. It has no
registry, no per-effect subclass and no configuration. If a third caller ever appears that needs a
different radius or duration, those become parameters before they become a framework.

### Blastoad attribution ignores every hit after the one that lights the fuse
**Status:** deliberate, and tightened by the PR #7 review. Only a hit taken while `!isFusing()` sets
`Toad.provokerId`, and a fuse lit by a mob or the environment records "nobody" — permanently, for
that fuse. A player who joins in afterwards gets no credit, even though their hit is real. That is
the intended reading of "the provoking player": the one who set it off. If a future packet ever
wants shared credit for a blast several people contributed to, that is a product decision about
`CarveState` participation, not a bug in this rule.

### Blastoad attribution is last-fuse only, and does not survive a logout
**Status:** deliberate, and the narrower choice on purpose. `Toad.provokerId` is a transient uuid
cleared by `ToadFuseGoal.stop()`. A provoker who logs out or dies during the 40-tick warning simply
drops out and the blast becomes an ordinary unattributed explosion — rather than being persisted
into a saved owner relationship, which is what the packet forbids. Only the first hit of a fuse
records a provoker; a second player hitting the same lit toad does not steal credit.

### Bucket release does not force-load or protect the chunk
**Status:** same shape as the R1 corpse window, and for the same reason. A released toad gets
`FromBucket` and so survives vanilla's distance-despawn rule, but nothing here keeps its chunk
loaded. Deploying one and walking a long way off leaves it where a normal creature would be left.

### No dung bombs, Bitterbug/Fulgurbug collection, or other endemic capture
**Status:** out of R2 scope by the packet's own exclusion list. `monster_feces.png` and
`fulgurbug.png` remain unused preserved art. Capture was built for exactly two species because
exactly two were asked for; it is two narrow `mobInteract` overrides, not a capture framework, so a
third species is a deliberate decision rather than a free extension.

## Found during R1 (2026-09-12), deferred out of that packet

### The corpse window is not configurable
**Status:** deliberate. 12,000 entity-ticking ticks is a constant (`CarveState.CORPSE_TICKS`), not a
server config entry, because nobody has asked to tune it and the packet's own contract states one
number. `MHNWConfig` is where it would go if a server operator ever wants it; the tests read the
constant, so a config would need one of them to read the config instead.

### Carving has no animation, sound or particle
**Status:** deliberate, R1 scope. A carve is instant and reports itself on the action bar. A carve
progress bar, a swing clip or a sound cue is presentation work with no authored asset behind it, and
R1's product contract asks only for localized bounded messages.

### The handoff's full-set cost summary was wrong; the patterns were implemented
**Status:** resolved by picking the concrete spec, recorded here so it is not "fixed" back.
`R1_FIRST_HUNTING_LOOP_HANDOFF.md` section 3 gives four shaped patterns and then says the full set
costs "7 hide, 4 claws and 13 bones". Those patterns actually cost **5 hide, 4 claws, 15 bones**.
The patterns were implemented verbatim. If the intent really was 7/4/13, that is a product change to
the patterns, not a recipe bug.

### `MonsterPart` is collidable while it is a corpse
**Status:** unchanged from before R1, but now visible for longer. A held body's hurtboxes still
return true from `canBeCollidedWith`, so a Great Izuchi corpse is something you bump into for the
whole carving window rather than for 38 ticks. Left alone on purpose: a solid body is arguably
correct for a thing you walk up to and carve, and the swept-envelope roughness already noted in
`MonsterPart` is the same issue. Revisit if a live session says a corpse is in the way.

## Found during R1a (2026-09-12), deferred out of that packet

### The habitat is rare: ~1.2% of the surface at region weight 2
**Status:** measured, deliberately left alone.

Sampled on seed `0`: 2 of 169 positions on a 256-block grid across a 3,072-block box around spawn were
the habitat, and a 225-chunk probe around the located site found 17 habitat chunks. The biome is
comfortably findable — 524, 101 and 475 blocks from spawn on the three specified seeds, all well inside
the 4,096-block target — but a player is not going to stumble into one often, and it makes any natural
population slow to sample.

Left as is on purpose: the agreed acceptance criterion is the distance target, which passes on all
three seeds, and region weight is a product-visible density decision rather than something to inflate
because it makes testing easier. The knob is `HuntingGroundsRegion.WEIGHT`. Revisit it with playtest
feedback about finding the place, not with a test-convenience argument.

## Found during R0a (2026-09-11), deferred out of that packet

### `RoarGoal`'s disengage clock stalls while a committed attack holds the goal's flags
**Status:** real, measured, harmless enough to leave. `RoarGoal` keeps its re-arm bookkeeping inside
`canUse()` — deliberately, so it uses real elapsed `getGameTime()` rather than a per-call counter.
But vanilla's `GoalSelector` never calls `canUse()` on a goal whose flags (here `MOVE`/`LOOK`) are
held by a running goal that reports `isInterruptable() == false`, and `GreatIzuchiCombatGoal` reports
exactly that while an action is committed. So if the target is dropped mid-swing, the disengage clock
does not start until that action finishes: measured at 161 real ticks to re-arm instead of the
intended 100.

Consequence is bounded and small — at most one action's length of extra delay before a monster is
willing to play its opening roar again, and only when aggro is lost mid-attack. Fixing it properly
means moving the clock somewhere that ticks unconditionally (entity `tick()`, or a goal that owns no
flags), which is a structural change to a shipped, working mechanic and explicitly out of scope for a
baseline-hardening packet. `greatIzuchiReArmsRoarOnlyAfterTheRealDisengageInterval` therefore holds
`attackCooldown` high so no action commits, and measures the clean disengage the interval is actually
specified for; the caveat is commented in the test itself. Pick this up if a live session shows the
delayed re-roar reading as wrong, not before.

### R0b / pre-release acceptance gates still open
Named here so they are not lost between packets. Updated after the R0b packet, and again after R1
(2026-09-12). **R1 widened two of these rather than closing them:** the two-client gate now also has
to show that each client gets only their own three carves and that a simultaneous or both-hand
interaction cannot duplicate a grant, and the disk-restart gate now also has to show that living
participation, per-player carve counters and remaining corpse time all survive. Both procedures are
written out in `TEST_PLAN.md` under "What still needs a human — R1".
- ~~Correctly aged presentation for a client that starts tracking mid-action (GeckoLib seeking).~~
  **Done in R0b** — `animation/ServerTimedAnimationController`, proven through GeckoLib's real
  sampler on a client (`client/AnimationSeekSelfCheck`, phase gap 0.0 ticks). See
  `docs/TEST_PLAN.md`'s R0b section.
- Two actual clients agreeing on damage, health, parts and death. **Still open** — R0b could not
  reach two distinct player identities from its environment. The exact procedure is written down in
  `TEST_PLAN.md` under "What still needs a human — R0b".
- A real save / stop / restart / rejoin cycle, including part reconstruction from disk. **Still
  open.** R0a's T04 and R0b's own reload test cover in-memory `saveWithoutId`/`load` round trips
  only, which is not the same claim and is not offered as one. R0b additionally established that a
  headless attempt is not available: Gradle does not forward stdin to `runServer`, so a dedicated
  server started that way cannot be driven by console commands.
- Fresh human confirmation of roar and death playback, now including that a mid-action join shows
  the middle of the clip rather than its start.

## Found during R0b (2026-09-12), deferred out of that packet

### Rathian's and Rathalos's death clips are still cut short by the vanilla body lifetime
Both authored death clips are 50 ticks, but neither class overrides `tickDeath`, so vanilla removes
the body at `deathTime` 20 and only the first ~15 ticks of the clip are ever shown (clip time is
`age - 5`). Aptonoth's 20-tick clip is cut the same way. Only Great Izuchi holds longer, at 38 ticks
for its 38-tick clip.

R0b deliberately did not change this: the packet's scope was synchronizing what is visible while a
body exists, and it was explicitly told not to extend any body's lifetime. Recorded here because the
right owner is whichever packet decides corpse retention — R1b is already going to decide its own
ten-minute corpse — and it would be wasteful to change the hold twice.

### The self-check probe cannot run as a GameTest, and that is structural
`client/AnimationSeekSelfCheck` was written as a GameTest first. It cannot run there: `GeoModel`
references `Minecraft`, and NeoForge's `RuntimeDistCleaner` refuses to load it on a dedicated server.
So any future test that needs GeckoLib's real sampler has to be a client-side probe, not a GameTest.
Worth knowing before somebody spends the same hour rediscovering it.

### `AGENTS.md` is now a pointer, not a copy
It used to duplicate `CLAUDE.md` almost verbatim and had drifted twice before anyone noticed (missing
Rathian's P4 attack timeline and the whole R1a habitat section). It is now a few paragraphs pointing
at `CLAUDE.md`. If a future tool insists on its own instruction file, give it a pointer too — do not
restore a second copy of the manual.

### The adapter's re-anchor tolerance is one tick, and has not been tuned online
`TOLERANCE_TICKS` is 1.0: below the two-tick acceptance target, above partial-tick float noise. It
has only been exercised locally, where a correctly tracking controller never re-anchors at all. On a
real connection with jitter it may re-anchor more often, which is correct but costs a second
`process` pass in the frames where it happens. If that ever shows up in a profile, raising it toward
two ticks is the knob — not widening it until an obvious replay passes.

## Deferred to the pre-release / survival phase

### Great Izuchi — `rally` clip not wired
**Status:** the species ships a second roar-family clip, `animation.great_izuchi.rally` (3s, distinct
from the 3.5417s `roar` `RoarGoal` now plays on first engagement — see `docs/TEST_PLAN.md`), that
nothing triggers yet.

MHW's own rally roars are usually a monster calling in help or re-engaging with a boosted state, and
this species is specifically a pack leader (spawns 1-4 escort Izuchi) — a natural fit, but no trigger
condition for it has actually been specified. To close it: get an explicit design call on when this
should fire (rallying its own escort on some cue? a second, angrier roar past a rage/health
threshold? something else?), then wire it the same way `roar` was — a synced ticks-remaining field,
an animation branch, and either a variant of `RoarGoal` or a small addition to it if the trigger turns
out to be roar-shaped. Don't invent a trigger condition and ship it silently; this is exactly the kind
of behaviour worth confirming before building, not after.

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
**Status:** built and headlessly verified as of R1a; **a live population has still never been seen.**

R1a replaced the guesswork here. Spawn entries now live on `#mhnw:spawns_hunting_wildlife` rather
than the vanilla forest tag, cover five species with agreed weights and counts, and are gated by
`HuntingSpawnRules` — which reads the `naturalSpawning` server config for both `NATURAL` and
`CHUNK_GENERATION` (only `NATURAL` was gated before, so worldgen-seeded passives bypassed the off
switch). All of that is covered by GameTests: the resolved entries, their categories and counts, the
absence of entries for small Izuchi and the unfinished wyverns, and the accept/reject behaviour of the
guard for both automatic sources and every manual origin.

What is still open is the observation, and it is an observation gap rather than a suspected defect.
See `docs/TEST_PLAN.md`'s R1a section for the actual numbers, but in short: 576 freshly generated
chunks produced 251 vanilla animals and no MHNW mobs, which sounds alarming until you narrow it to the
17 chunks that were actually habitat and realise the whole sample amounts to one or two
chunk-generation spawn events. The habitat is roughly 1.2% of the surface, so enlarging that sample is
slow. A live player-driven window was attempted with a real dev client connected over
`--quickPlayMultiplayer`, but it counted zero vanilla monsters as well, so it was measuring nothing
and its numbers were discarded.

To close it: one long session with a player who stays connected inside a habitat patch — a human at a
client is the cheap way — confirming that wildlife appears without an egg, that a Great Izuchi turns
up at night with its escort, that `naturalSpawning = false` stops new MH spawns while vanilla wildlife
carries on, and that the rarity reads as rare rather than empty or flooded. If the population genuinely
turns out to be starved once that is observable, the narrow knobs are the per-species weights in
`data/mhnw/neoforge/biome_modifier/hunting_wildlife_spawns.json`, and `Bug` moving from `AMBIENT` to
`CREATURE` so it stops competing with bats. Do not reach for global mob caps or a custom spawn
scheduler.

### A plains village inside the habitat has not been observed
**Status:** eligible by tag, not yet seen.

`data/minecraft/tags/worldgen/biome/has_structure/village_plains.json` adds the habitat additively, and
the mineshaft half of the same pair *was* observed for real — a `minecraft:mineshaft` inside the
habitat on seed `8675309`. No plains village has been found inside the habitat yet: on all three
sampled seeds the nearest village to the habitat site was in a neighbouring biome. That is what a
~1.2% habitat share and vanilla village spacing predict, and tag membership is not in doubt, so this is
a sampling gap rather than a bug.

To close it: on any seed, `/locate structure minecraft:village_plains` from inside a habitat patch, or
walk a few villages and check the biome underfoot, and confirm one generates in the habitat and looks
normal. A structure-tag assertion on its own is explicitly not proof, which is why this is listed.

### Izuchi (small) — attack and death animation
**Status:** genuinely hostile and damaging (ordinary `Mob.doHurtTarget` through R1's
`IzuchiHarassGoal`, no custom timeline), but with no dedicated attack or death clip to present. R1
changed *when* it closes in, not what it plays while doing so: the circle/dart/retreat loop is
presented with the existing walk/run clips, and the gap below is untouched. This is a P4 decision
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

**`attack_charge_bite_left` is now wired too** (`RathianCombatGoal.BITE_LEFT`), mirrored from
`BITE_RIGHT`'s own measured path rather than a separate capture -- see `BITE_LEFT`'s own doc for why
that's a reasonable inference (the right bite's real data already showed no consistent left/right
bias) and what would replace it if a live capture of the left clip ever shows it isn't a clean mirror.
Attack selection (`chooseAttack`) now has two real candidates to alternate between.

Still open: `attack_tailwhip` and the other clips (`attack_charge`, the fireball clips,
`attack_backhop`, `attack_backflip_ground/flying`) need the same live-capture treatment `BITE_RIGHT`
got, as additional entries in `RathianCombatGoal`'s `ALL` array -- the selection machinery is already
shaped for that, and now proven with two entries, not just one.

`RathianCombatGoal` was written as its own class rather than generalizing `GreatIzuchiCombatGoal`,
deliberately: this is the second real attack-timeline implementation now, exactly the point this
section used to say was worth reconsidering that decision at, but doing so now would mean
restructuring Great Izuchi's already-shipped, player-tested combat at the same time as standing up
Rathian's first cut, with no way to interactively verify the result beyond GameTests. Revisit once
Rathian's own timeline has also seen live play and a second real Rathian attack (tailwhip, most
likely, not just the mirrored left bite) exists to compare against.

**The MHW-style opening roar is wired for this species too** (shared `RoarGoal`/`Roarable`, see the
Great Izuchi section above) -- not deferred, closed this round.

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

**The MHW-style opening roar works independently of all this** (shared `RoarGoal`/`Roarable`, see
the Great Izuchi section above) and is wired -- it's just a presentation clip, no attack-volume
mechanics of its own, so the broken melee clips above don't block it.

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

R1 added more that rides on this same gate: carving participation is earned while a creature is
alive and has to survive a chunk unload before the kill, and the 12,000-tick corpse window is
counted in vanilla's `deathTime`, which is supposed to pause across an offline period rather than
catch up. `r1ParticipationSurvivesALiveRoundTrip` and
`r1CorpseRoundTripKeepsCountsAndRemainingTicks` are in-memory NBT round trips and are explicitly not
offered as the disk claim.

### A08 / A09 — two-client and dedicated-server agreement
**Status:** the server-side half is covered; the multi-client half is not, and cannot be from
this environment.

Already true: contact is decided entirely server-side, action state rides synched entity data
rather than a one-shot packet (so a client that starts tracking mid-fight receives the live
action), and the developer overlay draws the same volume the server hits with.

Not yet demonstrated: that two clients connected to a dedicated server agree on health and death,
that a second client joining mid-attack sees the current action rather than restarting an old one,
and that moving a camera away changes nothing about damage or part positions.

~~Known limitation: a client that starts tracking part way through an action begins the clip at its
first frame rather than seeking to the action's age.~~ **Fixed in R0b** (2026-09-12) — see
`animation/ServerTimedAnimationController` and `TEST_PLAN.md`'s R0b section. The multi-client
*observation* remains open regardless; the fix being in place is not evidence that two clients
agree, and R0b does not claim it is.

## Balance values that are testing placeholders, not decisions

- `GreatIzuchi.MAX_HEALTH` is 40, deliberately low so a slice dies quickly during development.
  Intended closer to 120.
- `GreatIzuchiCombatGoal.SCRATCH_DAMAGE` is 2.5 per strike, up to 3 strikes.
- Illagers are targeted alongside players, added to make attacks observable from outside a fight.
  Keep or drop deliberately.
