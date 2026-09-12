# Manual test plan

What still needs a human at a screen. Everything else (damage semantics, timing windows,
state-machine wedging, save/reload of gameplay facts, navigation) is covered by headless GameTests
via `gradlew runGameTestServer` — see `MHNWGameTests.java`, **currently 97 tests, all passing**
(full `.\gradlew.bat --no-daemon build runGameTestServer`, 2026-09-12, R0b client-lifecycle packet;
85 before it). The earlier 69-, 75-, 85- and 86-test figures are superseded — note the R0a round's
real observed count was 74, not the 75 this line used to claim. The shoreline test that failed under the P5a build passed
cleanly after the native-controls correction. Client acceptance for that Lagiacrus correction has
not been rerun by a human yet.

`docs/ANIMATION_MANIFEST.json` is a generated inventory of every species' animation clips (name,
length, loop mode) — regenerate with `node tools/gen_animation_manifest.js`, check it before asking
"does this species have a clip for X."

This file is updated as features land. Checked items were confirmed by the maintainer; unchecked
items are open. When you find a problem, say what you saw and I'll fix it and update this file.

Branch: **superseded** — the revival was squash-merged into `master` (`ab59e19`), and both `master`
and `revival/neoforge-1.21.1` exist on `origin`. "Local only, not pushed" no longer describes
reality. Standing rule going forward: no further push or publish without explicit authorization.

## How to launch

```powershell
.\gradlew.bat runClient
```

Combat/behaviour diagnostics: run `/mhnw debugcombat` in-game to toggle (op-only), or set
`debugCombat = true` in `run/config/mhnw-common.toml` before launching if you'd rather it start on.
Either way it logs attack transitions and contact accepted/rejected, and — for Great Izuchi
specifically — draws the live attack volume with F3+B.

**Bone probe**: with `debugCombat` on, Great Izuchi, Rathian, Rathalos, Aptonoth and Lagiacrus log their
actual runtime bone positions to the game log every ~2 seconds (or every tick for Great Izuchi
while it's mid-attack), in the same left/up/forward frame the hurtbox offsets use. This is what
actually fixed Rathian's and Aptonoth's hurtboxes this round (see their sections below) — real
measurements read straight from a play session, not another guess. Rathalos is borrowing Rathian's
measured numbers as a proxy for now (same skeleton, similar proportions); if you ever want it
measured for real, stand near one with `debugCombat` on for a few seconds and send me
`logs/latest.log`.


---

## R0b — client animation lifecycle (2026-09-12)

**What the packet was for.** A client that started rendering a monster part way through an attack,
a roar or a death played that clip from its first frame while the server was already landing the
hit. Presentation could therefore lead contact by up to a clip length — 65 ticks for Great Izuchi's
scratch — for exactly the observers who joined mid-fight. Server damage, the measured attack paths,
the timings, the geometry, the assets and the R1a habitat are untouched.

### What landed

- **`animation/ServerTimedAnimationController.java`** — a small version-pinned adapter on GeckoLib's
  own `AnimationController`. Common-loadable: no `net.minecraft.client` import, no `Minecraft`, no
  reference to `MHNWClient`.
- **Presentation anchors.** `Roarable` gained `getRoarStartTime()`/`setRoarStartTime()`, set once in
  `RoarGoal.start()` and synced by Great Izuchi, Rathian and Rathalos. Great Izuchi, Rathian,
  Rathalos and Aptonoth each gained a synced death anchor. Attacks reuse the action
  id/start-time/sequence that already existed.
- **One priority decision.** Each `mainAnim` now picks the clip, the presentation instance and the
  clock together (death, then roar, then attack, then locomotion), so a death pose cannot end up
  driven by a stale attack clock. Idle/locomotion/grazing still use GeckoLib's own clock.
- **12 new GameTests** (85 → 97) and a client-side sampler probe, `client/AnimationSeekSelfCheck`.

### The calibrated clock-to-clip convention

A controller with transition length `L` blends for its first `L` ticks and only then starts the clip
at time zero, so an **on-time observer has always shown `clipTime = age - L`**. The adapter
reproduces exactly that function for every observer rather than inventing a new one:

```
controllerAge == action age;   clipTime == clamp(age - L, 0, clipLength)
```

`L` is 5 for Great Izuchi/Rathian/Rathalos and 6 for Aptonoth, named as `TRANSITION_TICKS` on each.
An on-time controller already satisfies this within a tick and is never touched — which is why the
measured paths and accepted contact frames are unchanged by construction rather than by promise.
Feeding raw action age in as clip time instead would have shifted every already-measured attack by
five ticks; a GameTest now fails if anybody tries it.

Two consequences worth knowing, neither of them new behaviour:

- The action still ends at the profile's `actionEnd`, so the last `L` ticks of each attack clip have
  never been shown and still are not.
- Local latency is now removed rather than preserved. Before, an observer watching from the start
  still lagged by however long the synced action id took to arrive (about a tick locally, more
  online); the adapter pins everybody to the same authoritative curve, so an on-time observer moves
  by at most that latency, toward the server rather than away from it.

### How the seek is actually done (GeckoLib 4.9.2 has no public seek)

Verified against the published sources jar, SHA-256
`009055c5d7b848a8bed826ed8887936d85c5f711d0c19f0fa2547950db99ee41` — the one the handoff names.
There is no `seek`/`setAnimationTick`; `forceAnimationReset()` requests a *reload*, not a seek; a
speed modifier multiplies elapsed time rather than moving it. The one real lever is `adjustTick`:
`clipTime = speed * max(seekTime - tickOffset, 0)`, and `tickOffset` is `protected`. So the adapter
moves the clip's origin and nothing else. No reflection, no private field, no copied processor, no
fork.

A cold controller cannot simply be started at a nonzero time: `setAnimation` builds its queue only
once `lastModel` exists, and `process` polls that queue only while the adjusted tick is zero.
Advancing it first leaves it TRANSITIONING with no current animation — *a correct number and no
clip at all*. That is not a hypothetical; the mutation run below reproduced it. So each frame runs
GeckoLib's own initialization first and only then, if the result is at the wrong age, re-anchors and
lets GeckoLib sample again in the same render call. The second pass clears and rewrites the bone
queues, so the first pass's zero-time output never reaches the model. In steady state the second
pass does not happen at all.

The desired age is clamped just below `L + clipLength`, which does two things the library would not:
an expired one-shot holds its final pose instead of collapsing to the base skeleton, and a
`hold_on_last_frame` clip stops advancing `query.anim_time`, so MoLang motion in a held death pose
does not keep evolving.

### Death anchors, and why nothing is persisted

Vanilla's `deathTime` already counts the corpse hold but is never synced. The anchor is stamped
once, server-side, as `gameTime - deathTime`, which is correct both for a fresh death (`deathTime`
0) and for a body restored from disk (vanilla has already read its saved `DeathTime` back). So
there is no second death state machine, no second saved field, and no second call into `die()` —
no repeat XP or loot. **No body's lifetime was extended**: Great Izuchi still holds 38 ticks for its
authored clip and the other three keep vanilla's removal at `deathTime` 20, so Rathian's and
Rathalos's 50-tick death clips still only get their first ~15 ticks shown. Synchronizing what is
visible while a body exists was the scope; keeping a body alive to finish a clip was not.

### C03, the sampler — passed, on a real client

The clock can be tested headlessly. The sampler it drives cannot: `GeoModel` references
`Minecraft`, so NeoForge's `RuntimeDistCleaner` refuses to load it on a dedicated server
(*"Attempted to load class net/minecraft/client/multiplayer/ClientLevel for invalid dist
DEDICATED_SERVER"*). That was found by writing the harness as a GameTest first and watching it fail
— and it is the same architectural fact the rest of this mod is built on. So it lives in
`client/AnimationSeekSelfCheck`, runs once on the client while `debugCombat` is on, and logs one
line per check.

It uses Great Izuchi's **own baked `attack_scratch` clip** from the live animation cache — not a
synthetic one — and compares three observers over all 26 animated bones: one rendering since age 0,
one whose *first ever* frame is at age 35, and one driven only to clip time zero. Observed
`2026-09-12`, `./gradlew runClient`:

```
[anim-selfcheck] clip=animation.great_izuchi.attack_scratch length=65.0 bones=26 joinAge=35 transition=5
[anim-selfcheck] on-time   left_leg tick=0.0 start=-0.005904972458272415 end=-0.49561713343155017 animTime=1.5
[anim-selfcheck] late      left_leg tick=0.0 start=-0.005904972458272415 end=-0.49561713343155017 animTime=1.5
[anim-selfcheck] frame 0   left_leg tick=0.0 start=0.020517575069641837 end=0.15378863984192914 animTime=0.0
[anim-selfcheck] stock     left_leg no sample
[anim-selfcheck] PASS on-time observer produced a pose
[anim-selfcheck] PASS the pose at age 35 is distinguishable from the clip's first frame, so the comparisons below can tell a seek from a replay
[anim-selfcheck] PASS late observer's FIRST frame produced a pose at all
[anim-selfcheck] PASS late observer's first frame matches the on-time pose exactly
[anim-selfcheck] PASS late observer did NOT replay the clip's first frame
[anim-selfcheck] PASS query.anim_time agrees between the two observers (1.5 vs 1.5)
[anim-selfcheck] PASS control: an unmodified GeckoLib 4.9.2 controller does NOT reach that pose cold, so the comparisons above are meaningful
```

Phase gap **0.0 ticks**, against a target of two — the late observer's first frame is the on-time
pose exactly, on every bone, and `query.anim_time` is 1.5 s, which is clip tick 30, which is
`35 - 5`. The stock control produced **no sample at all**, which is the cold-controller trap
described above, observed rather than assumed.

### Mutation runs — these tests can actually fail

Every new check was confirmed to fail when the thing it guards is removed. Each was reverted
immediately after.

| Mutation | Result |
|---|---|
| Adapter returns after pass one (no seek) | 4 of 7 self-checks FAIL; the late observer produces *no sample*, `animTime` 0.0 vs 1.5 |
| `clipTimeFor` returns raw action age | `animationClockMapsActionAgeToClipTime` fails: *"the clip should start exactly when the blend ends, not at 5.0"* |
| `RoarGoal.start` does not set the anchor | all three roar-anchor tests plus the distinct-instance test fail |
| Death anchor stamps `gameTime`, not `gameTime - deathTime` | reload test fails: *"the reloaded body restarted its death clip: age went from 10 back to 1"* |

### Gates: what is closed and what is not

| Gate | Status |
|---|---|
| C01 build / server isolation | **Passed.** 97/97 GameTests; a real dedicated server (`runServer`) reached `Done (0.251s)` with no class-loading failure; `R0b-02` constructs the adapter on a dedicated server so a stray client import fails the suite |
| C02 clock snapshots | **Passed**, headlessly. Every instance reconstructible from synced data, repeats distinguishable, no `tickCount` clock, reload cancels transient action while health and death progress survive |
| C03 actual sampler | **Passed** on a real client, above |
| C04 encounter coverage | **Partial.** The mechanism is proven for scratch and is shared verbatim by all three Great Izuchi attacks, both Rathian bites and all three roars. Per-clip human observation is still open |
| C05 death lifecycle | **Passed** headlessly (anchors, precedence over a frozen attack, no extra death processing, no extended lifetime). Visual acceptance open |
| C06 render lifecycle | **Partial.** Resource reload and cull/retrack are handled by re-anchoring on deviation, and per-entity clocks make cross-contamination structurally impossible; **not** yet observed live |
| C07 two actual clients | **NOT MET — code complete, evidence pending.** See below |
| C08 real restart | **NOT MET — code complete, evidence pending.** See below |
| C09 no gameplay/asset regression | **Passed.** No attack window, damage value, measured path, hurtbox, model, animation file or biome file was touched; the full suite including every pre-existing R0a/R1a regression passes |

### What still needs a human — R0b

Nothing below was observed. These are the exact missing observations, not a summary.

- [ ] **C07, two actual clients.** Needs two *distinct* player identities on one dedicated server;
      two windows sharing a login kick each other and do not count. Not doable from this
      environment. To run it: `./gradlew runServer`, then two clients logged in as different
      players. With observer A holding the encounter loaded and observer B outside tracking range,
      move B in during a known action and check: (1) windup/active/recovery entry for Great Izuchi
      and Rathian; (2) entry during each of the three roars, then a later distinct roar instance;
      (3) **frustum culling** (look away while still tracking) and **true untrack/retrack** (leave
      and re-enter tracking range) separately — they are different conditions; (4) kill a subject
      mid-action and confirm both clients see the current death phase, with no ghost parts and no
      replay after removal; (5) a resource reload (F3+T) and two same-species monsters at different
      action ages; (6) one controlled parent/part hit agreeing on server and both clients. Record
      both clients' connection state at the start **and end** of the window — R1a's session lost its
      client partway through.
- [ ] **C08, a real save/stop/start/rejoin.** The in-memory `saveWithoutId`/`load` tests do **not**
      close this and are not offered as if they did. A headless attempt was made and does not work:
      Gradle does not forward stdin to `runServer`, so a dedicated server started that way cannot be
      driven by console commands. To run it: start a server, summon and injure a few subjects,
      record their UUIDs/health/variants/part names, `save-all flush`, **stop the server
      gracefully** (`stop`, not a kill), restart the same world and rejoin. Expect the same entities
      and data, freshly registered parts, and cancelled transient combat; new runtime numeric entity
      ids are expected, duplicate parents, extra escort generation and a resumed old attack are not.
- [ ] **Per-species visual acceptance.** Join mid-action and confirm the pose looks like the middle
      of the clip rather than its start, for all three Great Izuchi attacks, both Rathian bites, all
      three roars, and the four authored deaths. Also confirm the deaths still look right given that
      Rathian's and Rathalos's 50-tick clips are still cut short by vanilla's 20-tick body lifetime,
      which R0b deliberately did not change.

### R1a carryover — still not observed

R0b did not touch these and did not observe them; they are carried forward verbatim, not closed:
live natural MH populations in the habitat, a plains village inside it, and human visual acceptance
of the biome. See the R1a section below and `docs/DEFERRED.md`.

---

## R1a — Verdant Hunting Grounds (2026-09-12)

Packet: `docs/R1A_HABITAT_HANDOFF.md`. Starting revision: `dffb374` on `r0a/baseline-hardening`,
whose content is identical to `origin/master` after PR #3 was merged as `28805f5` — so this is the
accepted post-R0a mainline, not the handoff's inspection SHA `3fdb760`. Working tree was clean apart
from the untracked handoff itself. Local `master` is stale behind `origin/master`; ignore it.

**Automated result:** 74/74 passing before, **85/85 passing after**. Eleven tests added net: twelve
added, and `greatIzuchiNaturalSpawnBringsAnEscort` deleted as a strict subset of the new
`escortsLandOnGroundInOpenHabitat` (both paint the habitat, drive `finalizeSpawn(NATURAL)` and assert
1-4 escorts; the new one additionally checks ground and clearance). Nothing replaces it.

### Adversarial review corrections (same day)

Five findings, all legitimate, all fixed. Two mattered:

- **The slope regression was not reaching the raised surface.** The fixture raised the step by three
  blocks, putting its standable Y at `leaderY + 3` — outside `ESCORT_MAX_RISE` — so every raised-side
  candidate was rejected and the test was really only observing the untouched flat half, which the old
  fixed-Y code would also have passed. It now raises the entire escort ring by exactly one block,
  leaves only the leader's own 3x3 low, and asserts every escort sits at exactly `leaderY + 1`.
  Confirmed to be a real regression by reintroducing the fixed-Y loop: the test fails.
- **`isFree` tested fluid at the feet block only.** `noCollision` deliberately ignores fluids, so a
  1.1-block-tall Izuchi escort could be accepted standing dry with its head underwater — which
  contradicted the method's own stated body-volume contract. It now uses `containsAnyLiquid` over the
  same spawn AABB, with a dry-feet/submerged-head case added to the rejection test. Also confirmed by
  reintroducing the bug.

The other three: the README (the only user-facing entry point) never named the new required
dependency; the `naturalSpawning` config comment still said "monsters" after R1a extended it to
passive wildlife; and the subsumed test above.

### Installation requirement — new, and it affects players

The mod now has a **second required dependency: TerraBlender for NeoForge 1.21.1, 4.1.0.8**
(`com.github.glitchfiend:TerraBlender-neoforge:1.21.1-4.1.0.8`, from Forge Maven — Maven Central does
not serve it). It is declared required on **both sides**, range `[4.1.0.8,4.2)`. A client without it
will not connect to a server with it, and neither will start without it. It is not shaded or
jar-in-jarred, deliberately: TerraBlender coordinates biome placement between every mod that uses it,
and bundling a private copy is how that coordination breaks.

**Existing worlds do not become the new biome.** Biome placement is decided when a chunk is first
generated, so the Verdant Hunting Grounds appears only in newly generated terrain. A player adding
this update to an existing save has to travel to unexplored chunks.

### What landed

- `mhnw:verdant_hunting_grounds` — a temperate, lightly wooded meadow, based on the version-pinned
  1.21.1 vanilla forest export with one change: the `trees_birch_and_oak` entry in the vegetation step
  is replaced *in place* (order preserved, to avoid the feature-cycle hazard) with
  `mhnw:trees_hunting_grounds`, the same configured feature and the same placement-filter order but a
  constant count of 3 instead of vanilla's weighted 10/11. Vanilla precipitation, temperature 0.7,
  downfall 0.8, ambience, carvers, ores, caves and vanilla spawn entries are all kept.
- `mhnw:overworld_hunting_grounds` — one TerraBlender `RegionType.OVERWORLD` region, weight 2,
  registered once from `FMLCommonSetupEvent.enqueueWork`. Inside its own weighted share it replaces
  the `Biomes.PLAINS` and `Biomes.FOREST` climate slots; every other mapping passes through. Nothing
  global is replaced — no preset, no noise settings, no surface rules.
- Additive (`replace: false`) vanilla tag entries: `is_overworld`, `is_forest`,
  `has_structure/village_plains`, `has_structure/mineshaft`.
- `#mhnw:spawns_hunting_wildlife` — the single habitat selector, containing only our biome by default.
  Both the spawn-entry biome modifiers and the runtime spawn guard read this one tag, so they cannot
  disagree, and a pack author can extend the habitat without touching code.
- Spawn entries, owned entirely by biome modifiers (none in the biome JSON): Great Izuchi retargeted
  from `#minecraft:is_forest` to the selector (MONSTER, weight 2, 1-1), plus one wildlife modifier
  adding Aptonoth (CREATURE, 8, 2-3), Toad (CREATURE, 2, 1-2), Flashbug (CREATURE, 2, 1-2) and Bug
  (AMBIENT, 2, 1-2). No independent small-Izuchi entry; Rathian, Rathalos and Lagiacrus stay
  registered and egg-only.
- `HuntingSpawnRules` — one shared guard. Automatic spawning (`NATURAL` **and**
  `CHUNK_GENERATION`) now requires both the `naturalSpawning` server config and selector-tag
  membership, read at spawn time. Before R1a only `NATURAL` consulted the config, so worldgen-seeded
  passive spawning walked straight past the off switch. Eggs, `/summon`, spawners, breeding and
  already-saved entities are untouched by either.
- Six registered `ON_GROUND` placements on `MOTION_BLOCKING_NO_LEAVES`, including small Izuchi for
  placement validation. Aptonoth uses vanilla's real `Animal` rules; Toad/Flashbug/Bug are
  `PathfinderMob`s and get the same shape of check written out (surface by heightmap, solid spawnable
  ground, clear body volume, no fluid).
- Terrain-safe escorts. The old loop put every escort at the leader's own Y with no checks at all.
  Each escort now tries at most eight candidates in the existing 2-5 block ring, and each candidate
  walks its own column from 2 above the leader to 8 below, taking the first spot with solid ground and
  a clear body volume. A wild leader's escorts must also be inside the selector; a spawner-placed one
  keeps its unrestricted behaviour. Zero escorts on genuinely blocked terrain is the correct outcome.

**Local implementation note:** the escort Y was first resolved from the world's spawn heightmap, as
the handoff suggested. That is wrong in a way worth recording: the heightmap answers "where is the
sky", so a leader standing in a cave, under an overhang or inside a structure would have had its pack
placed on the roof above it. The bounded local column scan replaces it. The GameTest arena exposed
this immediately — see the fixture note below.

### Fixture note: the GameTest arena is a closed box

Worth knowing before touching these tests. The GameTest framework encloses every test structure in a
barrier cage **with a lid**, so the arena's `MOTION_BLOCKING_NO_LEAVES` heightmap sits above the roof
and the whole interior is, correctly, "not the surface". That means:

- The composite surface-wildlife accept path cannot be exercised in an arena. Its parts are covered
  instead: the surface rule's own logic against the arena's real heightmap
  (`huntingSurfaceRuleFollowsTheSpawnHeightmap`), and its ground/clearance components on a real grass
  block (`huntingGuardAcceptsValidHabitatPositions`, `huntingSurfaceWildlifeRejectsLiquidAndBlockedSpace`).
  Aptonoth, whose predicate is habitat plus vanilla animal rules and does not mind being indoors, is
  driven end to end through its real registered predicate.
- An earlier version of these two tests built a fixture *above* the cage lid. Do not do that again: it
  writes blocks outside the test's own bounds into a world shared with the tests running beside it,
  and it produced exactly the intermittent failures you would expect. The suite was run three times
  after the rewrite to confirm it is stable.

### Real-world generation evidence (H07) — passed

Three named scratch worlds, normal Overworld settings, `generate-structures=true`, each generated
fresh and driven over RCON. None of the maintainer's saves were touched; the scratch worlds were
deleted afterwards.

| Seed | Habitat located at | Distance from spawn | Target | Terrain sampled at the site |
|---|---|---|---|---|
| `0` | (384, 65, -320) | **524 blocks** | ≤ 4,096 | grass tops, oak/birch present, surface y 62-72 |
| `20260911` | (-144, 71, 0) | **101 blocks** | ≤ 4,096 | grass blocks, oak and birch leaves |
| `8675309` | (-304, 68, 432) | **475 blocks** | ≤ 4,096 | grass blocks, short grass, birch leaves |

All three well inside the 4,096-block target, so the region weight of 2 was left at the handoff's
default rather than tuned. `/locate biome` found it on every seed, and `execute if biome` confirmed
the located position after the chunks were actually generated. The terrain is ordinary vanilla
overworld — grass, dirt, sparse oak and birch, normal elevation variation.

**Measured habitat share:** 2 of 169 positions sampled on a 256-block grid across a 3,072-block box
around spawn on seed 0, i.e. **roughly 1.2% of the surface**. A patch probe around the seed-0 site
found 17 habitat chunks in a 225-chunk box. The biome is findable but genuinely rare. This is recorded
rather than acted on: the stated acceptance criterion is the 4,096-block distance, which all three
seeds clear comfortably, and raising the region weight is a product-visible density change that is not
this packet's call to make on a hunch. If playtesting says the habitat is too hard to find, the knob is
`HuntingGroundsRegion.WEIGHT`.

### Structures (H08, partial)

Tag eligibility is in place and one half was observed for real: on seed `8675309` a
`minecraft:mineshaft` at (-416, ~, 288) sits **inside** the habitat (`execute if biome` passed). A
plains village inside the habitat has **not** been observed — the nearest village to each habitat site
was in a neighbouring biome, which is expected given a ~1.2% habitat share and village spacing, and is
not evidence against eligibility. Still open; see `docs/DEFERRED.md`.

### Natural wildlife population (H08) — NOT observed, open

Reported honestly: **no MHNW mob has been seen to spawn automatically in a real world.** What was
actually done and measured:

- **Chunk-generation spawning, no player:** 576 freshly generated chunks around the seed-0 habitat
  produced 251 vanilla animals (54 cow, 49 pig, 90 chicken, 41 sheep, 17 wolf) and **0 MHNW mobs**.
  Narrowed to habitat chunks only — 17 of them, isolated with per-chunk volume selectors — the result
  was 4 sheep and 0 MHNW mobs. At vanilla's 0.1 creature-spawn probability per chunk that is about one
  or two spawn events total, and Aptonoth's share of the biome's creature weight is 8/57, so a zero
  here is statistically unremarkable. **It is not evidence of a defect, and it is not evidence the
  spawning works either.** The sample is simply too small, and the habitat is too sparse to enlarge it
  cheaply.
- **Natural spawning with a real player:** a dev client was connected to the scratch server with
  `--quickPlayMultiplayer` and parked inside the habitat (biome membership confirmed at the player's
  own position). Two 2,000-game-tick windows were run, one at noon and one at midnight. Both counted
  0 MHNW mobs — but they also counted **0 vanilla zombies, skeletons, creepers or bats**, which is the
  tell: the harness was not measuring live spawning at all. The client session ended partway through,
  so the night window in particular had no eligible player for some of its duration. The numbers from
  those two windows are therefore discarded, not reported as a result.

This is an **unavailable-observation gap, not a known code defect**. The guard, the resolved spawn
entries, the categories, the counts and the placement registrations are all verified headlessly by
GameTests H01/H03/H04, and the disable path is verified there for both automatic sources. What has not
been done is a long enough live session, with a player who stays connected, inside a habitat patch, to
see a population appear. That needs a human at a client or a much longer scripted session; it is
recorded in `docs/DEFERRED.md` rather than claimed.

### Client acceptance — not run

No human has looked at this biome on a screen. Nothing in R1a changes a model, texture, animation,
hurtbox or attack, so there is no new visual regression surface, but the biome's own look — tree
density, whether a meadow reads as a meadow, whether the name displays — has not been seen.

- [ ] The biome reads as a temperate, lightly wooded meadow rather than a thinned forest.
- [ ] Tree density at a constant count of 3 looks right on the ground, not just in the JSON.
- [ ] The biome name shows as "Verdant Hunting Grounds" (F3 screen, or a `/locate biome` jump).
- [ ] A plains village and a mineshaft generate somewhere in the habitat and look normal.
- [ ] Wildlife appears without a spawn egg, at a rarity that does not read as flooding.
- [ ] `naturalSpawning = false` visibly stops new MH spawns without touching vanilla wildlife.

---

## R0a — baseline hardening round (2026-09-11)

Packet: `docs/R0_BASELINE_HANDOFF.md` (R0a only), against `master` at `ee5f0a5`, whose code is
byte-identical to the packet's inspected `3ca5d46`. Working tree was clean at start.

**Automated result:** 69/69 passing before, **74/74 passing after**. Six tests added, four
strengthened, one deleted as strictly subsumed (`reloadCancelsTransientCombatState`, whose
partial-NBT round trip is a subset of T04's full one). No production code changed — the added
coverage found no defect that this packet owns.

What the six new tests establish, all through ordinary AI ticking rather than by calling goal
methods directly:

- **T01, `{greatIzuchi,rathian,rathalos}RoarRunsForItsRealClipLength`** — the roar countdown runs at
  one tick per *real* tick for the species' whole authored clip (70/99/99 ticks observed), with no
  attack and no damage to a live in-range target for the duration. The deadline is anchored to the
  first countdown value actually observed, not to a global test tick or a generous timeout.
  *Negative check performed:* flipping `RoarGoal.requiresUpdateEveryTick()` back to `false` fails
  exactly these three, at ~6 real ticks in, and the override was restored immediately.
- **T02, `greatIzuchiReArmsRoarOnlyAfterTheRealDisengageInterval`** — a held target never re-arms; a
  brief loss and re-acquisition does not re-arm and restarts the clock; a sustained loss re-arms
  within 100 + 4 ticks measured from the game time the target was actually cleared. The old test
  proved only that re-arming happened *eventually*.
- **T03, `greatIzuchiKilledMidRoarLeavesAnInertCorpse` / `rathianKilledMidAttackLeavesAnInertCorpse`**
  — killed mid-roar and mid-attack respectively, the corpse deals no further melee damage for the
  whole death hold, is removed, and its parts leave NeoForge's part lookup. Deliberately does *not*
  assert that the synced attack id or roar countdown clears: goals stop ticking at death, so those
  fields freeze, which is harmless because `mainAnim` checks death first.
- **T04, `{greatIzuchi,rathian}FullReloadKeepsHealthAndPartsButNotTheAction`** — a full
  `saveWithoutId`/`load` round trip onto a fresh instance that actually enters the level (original
  discarded first, so no duplicate UUID or part identity), starting from deliberately reduced health
  (17.0 of 40, 41.0 of 90) and a live transient state. Health survives; attack id, roar countdown
  and the action do not; the initial cooldown stays positive; part count, names and lookup
  registration all hold. The two fixtures save in deliberately *different* transient states — Great
  Izuchi mid-attack, Rathian mid-roar — because an attack fixture has `getRoarTicks() == 0` at save
  time, which would make its roar assertion unable to fail. Replaces the pre-existing
  `reloadCancelsTransientCombatState`, which round-tripped `addAdditionalSaveData` alone; every
  assertion it made is a subset of these, so it was deleted rather than left as a weaker duplicate.
- **T05** — `assertPartsUnregistered` now checks `level().getPartEntities()` (the lookup melee
  picking, projectiles and area damage actually scan) for Great Izuchi, Aptonoth, Rathian and
  Rathalos removal, not just each part's own flags. Lagiacrus's existing helper now delegates to it.

**One real behaviour found, deliberately not patched here:** `RoarGoal`'s disengage clock lives in
`canUse()`, and vanilla's `GoalSelector` does not call `canUse()` on a goal whose flags are held by a
running non-interruptable goal. A committed attack makes `GreatIzuchiCombatGoal` non-interruptable,
so dropping the target mid-swing delays re-arming by that action's remaining length — measured at
161 real ticks instead of 100. It costs at most one action's delay before a monster is willing to
roar again; recorded in `docs/DEFERRED.md` rather than restructured inside a baseline-hardening
packet. T02 therefore measures a clean disengage (the fixture holds `attackCooldown` high so no
action commits), which is the case the interval is specified for.

**Packaged license metadata:** `mod_license` moved from `All Rights Reserved` to `GPL-3.0`, matching
the repository root LICENSE and the maintainer's stated choice. Confirmed in the built artifact:
`build/libs/mhnw-0.2.0.jar!/META-INF/neoforge.mods.toml` now reads `license = "GPL-3.0"`, with
`authors` and every other field unchanged. This says nothing about third-party branding or sound
rights, which remain unresolved.

**Still pending, R0b / pre-release — not closed by this round:**
- Correctly aged presentation for a client that starts tracking mid-action (GeckoLib seeking).
- Two actual clients agreeing on damage, health, parts and death.
- A real save / stop / restart / rejoin cycle, including part reconstruction from disk.
- Fresh human confirmation of roar and death playback.

These four are the reason R0a completion is not first-release verification. They do not block R1a.

**Adversarial review pass (2026-09-12).** Four findings on PR #3, all accepted and fixed: the two
reload fixtures both saved mid-attack, so their roar assertion could not fail (Rathian now saves
mid-roar); every Markdown cross-link between `docs/ROADMAP.md` and `docs/R0_BASELINE_HANDOFF.md`
pointed at a nonexistent `docs/plans/` directory (paths corrected, content untouched); this file
still named the two replaced roar tests and claimed Rathian/Rathalos roar coverage existed only on
Great Izuchi; and the superseded partial-NBT reload test was left in place beside its own superset.
Suite after the pass: 74/74. A follow-up review then caught one more time-travel artifact: `docs/ROADMAP.md` §2 still told the next agent that `master` holds the legacy Forge runtime, which would have it reject the correct checkout — now labelled superseded, with the corrected form of the author's original point kept. Prose only, so the green suite was not rerun for it.

## Lagiacrus (P5a movement baseline, 2026-09-11)

Implemented spawn egg/summon registration, parent health/damage, seven native `MonsterPart`
hurtboxes, the preserved GeckoLib model/texture and six land/swim locomotion clips. No outgoing
attacks or natural spawning. There is no death clip; vanilla death presentation remains enabled.

Movement uses one `AmphibiousPathNavigation` with `SmoothSwimmingMoveControl` and
`SmoothSwimmingLookControl` throughout land and water, with collision-aware native axolotl-style water travel and underwater
breathing. The species goal acquires visible survival/adventure players (or follows an explicitly
assigned valid living target), retries paths every 20 ticks, abandons after three failed/partial
paths or 200 ticks total, then prevents pursuit for 100 ticks. Target loss, goal stop, death,
removal and NBT load clear pursuit. No separate target goal can bypass the retry cooldown.

Root dimensions, health/speed and seven static offsets are provisional design estimates, not live
measurements or converted raw pivots. The root covers the body; parts use the preserved locator
names in head-to-tail order. Both land and swim use the same feet-origin left/up/forward frame,
in blocks, rotated only by body yaw. Live fitting remains pending, including the different poses.

Twelve focused tests were added: registration/egg/part shape and lookup; root-first
and part-first damage deduplication; distinct attackers and attribution; death cleanup; discard
cleanup; land pursuit without damage; submerged movement/breathing without damage; shoreline
entry into water and bounded bank-exit pursuit using the same navigation/control instances; sealed-target abandonment
and retry suppression; an absolute deadline for a stalled but reachable path; target-loss cleanup;
and NBT health/parts/pursuit cleanup with a reload cooldown. These exercise server contracts, not
client picking, visual alignment or full P5 acceptance.

**Shoreline correction (2026-09-11):** decoded `arena.nbt`: its floor is at y=0. The original
water and bank began at y=2, leaving an undercut at y=1; removing the bank instead flooded the
intended dry side. The corrected fixture supplies a continuous floor at y=1, source water at y=2,
and a solid one-block bank with its walking surface at y=3. Native swimming look control avoids
ordinary `LookControl`'s per-tick pitch reset; while submerged, pursuit requests yaw only so
path-node movement owns pitch instead of competing with eye-target tracking.

Even with that pairing and valid geometry, the two-block-wide root stalled at the bank (x=7,
feet approximately y=2.17) until the existing pursuit deadline cleared the target. The native
swimming controller has no jump handling, and collision stepping requires ground contact (or a
downward vertical collision); a floating body cannot rely on that to climb the bank. P5a therefore
does **not** claim reliable water-to-land bank traversal. The renamed
`lagiacrusEntersWaterAndBoundsBankExit` requires actual land-to-water target approach, then starts
bank-exit pursuit and requires cleanup within the unchanged 200-tick bound. It also checks control
identity and both targets' health. No other test was removed or disabled; the count remains 63.
`compileJava` passed. A temporary native GameTest wrapper ran only this final shoreline method
against the same arena: **1/1 passed**. The wrapper was removed afterward; no broad validation or
client run was performed. The earlier targeted bidirectional attempts still failed at the bank,
which is why the limitation is explicit rather than marked fixed.

- [ ] Spawn via creative egg and `/summon mhnw:lagiacrus`; confirm texture and model render
- [ ] Observe idle/walk/run and swim_idle/swim/swim_fast, including shallow-water transitions
- [ ] With F3+B and `/mhnw debugcombat`, capture the seven locators through full land/swim loops
      at four cardinal headings; fit static envelopes from live ranges, not a narrow sample
- [ ] Confirm melee/projectile picking on jaw and tail, ordinary damage and vanilla death/removal
- [ ] Observe open and obstructed shorelines, shallow water, and movement against solid terrain
- [ ] Dedicated server/two-client agreement, save/quit/reload, natural spawning and P5 combat remain
      pending; this packet does not close A01-A14 or the release species baseline

## Great Izuchi (P2 combat slice)

New this pass, per your feedback ("Great izuchi spawns with 1-4 izuchis around it"): a naturally- or
spawner-spawned Great Izuchi now brings 1-4 escort Izuchi along with it. A spawn-egg/`/summon`'d one
does not (deliberately — that's a placed, not a wild, spawn).

**MHW-style opening roar, this round**, and shared with Rathian/Rathalos (see `RoarGoal`/`Roarable`):
a large monster now roars once when it first acquires a target, not again for the rest of that fight,
and only re-arms after a real stretch with no target (5s, provisional), not a one-tick target flicker
(a dodge, a brief line-of-sight loss). The goal sits above whatever owns combat movement/look, so the
roar genuinely freezes the fight rather than playing underneath an attack goal that keeps swinging.
GameTest-covered — **superseded as of R0a**: the original `greatIzuchiRoarsOnFirstEngagement` and
`greatIzuchiReArmsRoarAfterARealDisengage` were replaced by the stricter
`greatIzuchiRoarRunsForItsRealClipLength` (real-tick countdown across the whole clip) and
`greatIzuchiReArmsRoarOnlyAfterTheRealDisengageInterval` (the 100-tick interval measured, plus the
brief-loss restart), and Rathian and Rathalos now have their own roar-duration tests rather than
relying on the shared goal. See the R0a round above.
`rally` (a second, distinct clip this species also has, likely for calling its escort) is **not**
wired — no trigger condition for it has been specified yet, see `docs/DEFERRED.md`.

Also new this pass: it (and Rathian/Rathalos) no longer stops rendering when the hitbox center
leaves the camera frustum while the long neck/tail is still visibly on screen — see the culling note
under Rathian/Rathalos below, same fix applies here too.

- [x] Renders, spawns via egg, idles/walks/runs with correct animation
- [x] F3+B hurtboxes sit on the body correctly (measured via the runtime bone probe)
- [x] Damage lands through a hurtbox; tail/body hit registers
- [x] Scratch, tail swipe, tail slam all fire, connect, and look reasonable in sequence
- [x] Windup plant + active-phase pace read as a real wind-up and follow-through
- [x] Death plays its authored clip fully before the corpse is removed
- [ ] **New: roars once when it first notices you, doesn't roar again mid-fight, and roars again
      if you break off long enough and re-engage** — GameTest-covered for the state machine itself;
      worth a live look for whether it *reads* right (does it actually freeze/plant during the roar,
      does the timing feel right). **Fixed a real bug this round**: the countdown that ends the roar
      only actually ran every other real tick (same throttling `Mob.serverAiStep` applies to
      re-checking idle goals), so it took roughly twice as long as the clip's own real duration —
      the clip finished and held its last frame (its own authored loop mode) well before the
      countdown let go, which read as "froze after the roar." Confirmed live on Rathalos before the
      fix; needs a fresh look on all three now that the countdown runs every tick like the combat
      goals already do.
- [ ] **New: turning away from a close-up Great Izuchi no longer makes its head/neck vanish
      mid-turn** — this was a real bug (see Rathian/Rathalos note below), fixed for all three
- [ ] **New: a naturally-spawned Great Izuchi has 1-4 regular Izuchi nearby when you first find it**
- [ ] **A11 natural spawning** — deferred by your call; wired but never observed (see `docs/DEFERRED.md`)
- [x] **A10 navigation scenarios** — closed, not deferred: added six headless GameTests (open ground,
      an outside corner, a passage exactly as wide as the body, a passage narrower than the body, a
      single-block step, and a fully sealed unreachable target), all passing on the first run — no
      code change was needed, vanilla ground navigation already handles this body correctly. See
      `navigatesOpenGroundToReachTarget` through `anUnreachableTargetDoesNotProduceAnUnboundedRepathLoop`
      in `MHNWGameTests.java`.
- [ ] **A08/A09 two-client agreement** — needs a dedicated server + two clients, which I cannot run;
      deferred to polishing (see `docs/DEFERRED.md`)

## Aptonoth (P3 passive herbivore)

Ninth round this pass, from a screenshot showing the eighth round's new `tail_4` sitting too high
and too big:

- **`tail_4` recomputed from a tighter trend.** The previous extrapolation had drifted from the
  tail_1-to-tail_3 two-step delta rather than the tighter tail_2-to-tail_3 one-step delta, compounding
  the overshoot (`up` 3.18, `forward` -5.15, sized 2.0x1.0). Recomputed from the tighter delta and
  shrunk toward `tail_3`'s own size (`up` 2.85, `forward` -4.20, sized 1.3x0.8).
- **Whole tail chain (`tail_1`-`tail_4`) nudged down and given more height**, per "a little lower and
  a bit taller in the Y axis" — same principle as Rathian's tail: idle sway means no single centre
  value reads right against every frame, so widen the box to cover the swing instead of chasing a
  perfect centre.

Eighth round (previous), from a top-down screenshot showing the tail visibly continuing past the
last box:

- **Root box raised again**: `BODY_HEIGHT` 2.0 → 2.4. Still an eyeballed nudge, not a direct
  measurement (would need a leg/foot bone in a capture to pin the ground reference exactly) — flag
  again if this overshoots into "too tall."
- **Chest moved forward again and enlarged**: `forward` 1.00 → 1.60 (short of the measured neck base
  at 1.74), size 1.1 → 1.4.
- **New: a fourth tail segment at the actual tip.** `tail_3` sits exactly at the measured tail2
  *bone*, but that bone's own mesh cube extends roughly 2 more blocks beyond its pivot before the
  tail visually ends — confirmed by the screenshot showing real tail mesh past the last box. There's
  no bone out there to measure (tail1/tail2 are the only two tail bones in the model), so `tail_4` is
  extrapolated by continuing the one real observed trend (the tail_1-to-tail_3 delta in both `up` and
  `forward`) one more step, rather than a fresh guess, and sized generously since it's now an
  estimate rather than a measurement.
- Flee duration unchanged (still a sustained flee after a hit, not one short hop).

- [ ] Renders, spawns via egg, idles/walks with correct animation
- [ ] **F3+B: does the root box now read as tall enough**, without overshooting into looking too
      tall again?
- [ ] **F3+B: is the chest box clearly visible, separate from the white box, and close to the face?**
- [ ] **F3+B: does the tail's fourth box now reach the actual tail tip**, instead of the tail
      continuing past the last box?
- [ ] **F3+B: is `tail_4` no longer oversized/floating too high**, and does the whole tail chain read
      as centred on the tail rather than sitting slightly above it?
- [ ] **Re-check:** when hurt, flees for a real sustained duration, not just one short hop
- [ ] Eats grass occasionally (the `eat` animation should play when it's actually eating, not just
      standing still) — grass must be nearby (short/tall grass on a grass block) for this to trigger
      at all; it's a low-probability vanilla check, so may take a while to observe
- [ ] Retaliates (rarely) if cornered while already fleeing — low priority to confirm, documented as
      a soft, not-guaranteed behaviour
- [ ] Does not attack players or other mobs unprovoked

## Toad (P3 endemic hazard)

Reworked this pass per your feedback: it no longer triggers from mere proximity (only from being
hit/interacted with), can't be killed by being hit (only its own release removes it), and releasing
now really does make it "blow up" — a smoke-poof burst and it's gone, not a fuse that goes quiet and
can fire again later.

- [ ] Renders with the correct one of four textures (poison/sleep/paralysis/blast) — spawn several to
      see variety, since it's chosen randomly at spawn
- [ ] Sits mostly still, occasional slow hop
- [ ] **Re-check: walking up to it and standing next to it does nothing** — only hitting it should
      trigger the fuse now
- [ ] Hitting it triggers the `fuse` telegraph animation, then a released cloud, then a smoke-poof
      and the toad itself disappears
- [ ] **Re-check: hitting it repeatedly (before the fuse finishes) never drops it below full health**
      or kills it directly — only the release should ever remove it
- [ ] Poison variant: nearby player takes poison damage over time
- [ ] Paralysis variant: nearby player is heavily slowed
- [ ] Sleep variant: nearby player gets nausea + mild slowness (**not** any kind of control lock —
      flag immediately if it ever blocks or overrides your input, that would be a real bug)
- [ ] Blast variant: a real small explosion (knockback/damage), **no terrain destroyed**, and only
      this variant should deal actual explosion damage — the other three should only apply their
      status effect, no direct damage

## Flashbug (P3 flying endemic life)

Second round this pass:

- **Fixed the "constantly flashes" bug** — it was still triggering from mere proximity (the default
  `EndemicAreaEffectGoal` behaviour), which combined with a 5-second cooldown read as flashing
  nonstop the whole time you were near it. Now, like the toad, it only triggers from being hit or
  interacted with, and releasing now discards it (smoke-poof + gone), the same "blowing up" behaviour
  the toad got last round — no more recovering-after-cooldown for either of them.
- **Fixed the dark/muddy colour** — the legacy renderer forced a minimum glow (block light 12) on
  this species specifically, since its texture is a mostly-dark body with small glowing-yellow
  segments; this port had dropped that, so ordinary ambient lighting made it look like plain dark
  yellow instead of glowing. Restored.
- Did **not** add the optional small ambient idle-flash you floated ("can emit a smaller localized
  flash as it idles") — skipping that unless it turns out the creature reads as too static without
  it now that the constant flashing is gone; easy to add later if you want it.

Everything from last round (player exclusion, facing requirement, no fall damage, stays low) is
unchanged.

- [ ] Flies around, hovers, does not land often
- [ ] **Re-check: no longer flashes constantly just from you standing nearby** — should stay dark
      until you actually hit or interact with it
- [ ] **New: colour reads as a proper glowing yellow, not dark/muddy**, including in daylight and at
      night
- [ ] **New: after releasing, the flashbug itself disappears** (smoke-poof), it does not go quiet and
      come back
- [ ] Hitting it triggers a brief flash telegraph, then a blinding flash
- [ ] You (the player) are never blinded by it, no matter how you're looking at it
- [ ] A nearby mob only gets blinded if it's actually facing the bug — one facing away should be
      unaffected even if it's close and has line of sight
- [ ] Never takes fall damage, however it moves; stays low (roughly 3-4 blocks off the ground)

## Bug / bitterbug/godbug (P3 ambient life)

Second round this pass: godbug chance raised from 2% to an even 50/50 (was taking too long to see one
during testing). Health (1-hit-kill) unchanged from last round.

- [ ] Renders with the hand-modeled mesh (not GeckoLib) — legs and antennae move procedurally while
      walking
- [ ] Dies to a single hit, including an unarmed punch
- [ ] **Re-check: godbug now shows up roughly half the time**, not rarely — a handful of spawn-egg
      uses should show both variants (or summon one directly: `/summon mhnw:bug ~ ~ ~ {Variant:1b}`)
- [ ] Wanders passively, never attacks, never flees

## Izuchi (P4 small monster)

Genuinely hostile, unlike everything in P3 — this is the first thing since Great Izuchi that
actually attacks the player on sight. **It has no attack or death animation on purpose** — see
`docs/DEFERRED.md` for why (the only candidate clips reference bones missing from their own
geometry) — so expect it to fight using its walk/run clip and die with vanilla's plain corpse flop.
That is the current, deliberate state, not a bug to report.

- [ ] Renders, spawns via egg, idles/walks/runs with correct animation
- [ ] Notices and attacks a nearby player, dealing real damage
- [x] **New: targets pillagers on sight, same as Great Izuchi/Rathian/Rathalos** — GameTest-covered
      (`izuchiTargetsAPillagerOnSight`)
- [ ] Naps occasionally when nothing is around (uses the `sleep` clip) — this is a rare, roughly
      1-in-20-minutes-of-idle-time random event per goal-selection check, so may take a while to
      observe; not urgent to confirm
- [ ] Wakes immediately and attacks if hit or approached while sleeping
- [ ] Does not get stuck permanently asleep or permanently passive

## Rathian (P4 ground wyvern, ground-only)

**Hurtboxes switched from averaging to range-midpoint**, in direct answer to "can we avoid nudging
around." Two rounds of plain averaging (a handful of samples, then twelve) still weren't stable: each
capture's mean leaned toward whichever slice of the idle sway happened to get sampled more, so
successive "fixes" kept overshooting each other (`stinger`'s `up` alone went 0.40 → 1.00 → this
round's 0.30). A ~25-sample capture let a different method actually hold: for a bone that sways
through a real range every loop, the *midpoint of the observed min/max* is robust to sampling bias
in a way a mean isn't, as long as both extremes were seen at least once — which this many samples
reliably does. This shouldn't need another re-centring the way the mean-based passes did.

**Two parts added to close real, visible gaps** (previous round): the neck-head gap was 0.87 blocks
and the tail_end-stinger gap was 1.1 blocks, computed directly from the measured centres and part
widths. Added `throat` (between neck and head) and `tail_tip` (between tail_end and stinger),
interpolated at each gap's midpoint. Rathian now has 9 parts, not 7.

**Round three (previous): still-visible gaps and a still-low tail chain.** A screenshot showed both
problems clearly. Widths for the torso/neck/throat and the whole tail chain were set to the *exact*
distance to each neighbour, so consecutive boxes meet exactly ("start where the next ends") — this
meant shrinking `throat`/`tail_tip`/`stinger`, which the previous round's interpolated midpoints had
sized into overlapping their neighbours instead of closing the gap. A flat +0.35 raise was also
applied to the tail chain's `up`, on the theory that the screenshot's rendered pose sat higher than
the statistical centre.

**Round four (this pass): the +0.35 raise was the wrong fix, reverted.** A further screenshot still
showed the tail reading low, and closer analysis of the full observed range (`stinger` alone swings
across roughly a full block, `-0.48` to `1.08`, during ordinary idle sway) showed why: no single
centre value reads correctly against *every* frame of a sway that wide, whichever moment a screenshot
happens to catch — shifting the centre up just moves which frames look wrong. Reverted to the plain
range-midpoint centre (the actual mathematical "centred on the bone" value, which is what was asked
for), and instead widened `height` on the tail chain (`tail_base` through `stinger`) so the box spans
the real swing instead of one instant of it. This is expected to read as "mostly right, every frame"
rather than "perfectly centred in some frames, visibly off in others."

Also fixed a few rounds ago: the entity no longer stops rendering (head/neck suddenly disappearing)
when the root hitbox leaves the camera frustum while the model still visibly extends into frame.

**"Boxes aren't updating at all" report, round four:** checked the on-disk file directly — it already
has the reverted, range-midpoint values (`torso up=2.23`, not the old +0.35-boosted `2.58`), so the
source was correct at the time. Turned out moot either way: the *next* capture (round five, below)
came from the model's own dedicated locator bones and moved most of these numbers again regardless.

**Round five: switched from inferring off mesh bones to reading the model's own locator bones
directly.** A fresh capture showed the bone probe logging `torsoHitbox`, `neckHitbox`, `headHitbox`,
`baseTailHitbox`, `midTailHitbox`, `tailEndHitbox`, `stingerHitbox` — dedicated placement bones the
model already ships, not mesh bones being inferred from — with near-zero variance across samples
(unlike the wider-variance mesh-bone captures earlier rounds used). `head` barely moved, confirming
the earlier numbers were on the right track for that part; but per this round's screenshot the chain
was "still in the same place, too low" — and indeed `torso`/`tail_base`/`tail_mid` had drifted low
against these locators and were raised, while `tail_end`/`stinger` had drifted both low and too far
out and were pulled in and down further. `throat`/`tail_tip` recomputed as the midpoint of their own
updated neighbours, and every width recomputed the same "exact distance to each neighbour" way as
round three — barely changed, confirming that part of the methodology already held up.

**Round six: round five's locator-bone capture was itself a narrow, non-representative slice.** A
follow-up screenshot showed the tail chain reading worse — moved further down than before. A second
capture, taken during the part of the idle loop that actually sways, showed the `*Hitbox` locator
bones swing just as much as any mesh bone (`stingerHitbox` alone spans roughly -0.46 to 1.24, a
1.7-block range) — round five's capture had simply caught a narrow, low slice of that range and
mistaken it for a stable value, the same mistake range-midpoint was adopted to avoid in the first
place, just one level down the bone hierarchy. Fixed by combining both captures' observed min/max per
bone and range-midpointing the combined span, same methodology as always, applied one level deeper.
**Deferred, per your call, to the polishing pass** rather than continuing to chase this live — we're
spending real time on a system that may keep needing a wider capture each round; worth a proper
multi-minute capture across a full idle cycle (or several) when polishing, rather than another
short live-tweak loop now.

**Real attack timeline landed two rounds ago: `RathianCombatGoal`, replacing plain vanilla
`MeleeAttackGoal`.** The reported problem was cadence, not damage: vanilla melee closes to contact
range and deals damage the instant it touches, which read as a body-slam that only afterward played
a bite clip, rather than a bite that actually connects. `RathianCombatGoal` is the same
windup/active/recovery shape as `GreatIzuchiCombatGoal`.

**This round: the strike volume and phase timing were replaced with a real measured path**, baked
from a live capture, plus two behaviour fixes reported from that same session. The round-one
estimate (a static point close to the body, active ticks 11-20) turned out wrong on both counts:
bucketing every `Jaw`-bone sample from the capture by its position in the clip showed the jaw
actually stays reared up and far out (up 4+, forward 5.3-6.9) for most of the clip, and only
descends toward something reachable in its *last* third (age 19-28, up dropping from 4.48 to 0.91,
forward settling to 4.3-5.5) — that descent is baked in now as a real 4-point path, and the
active window moved to match it (see `RathianCombatGoal.BITE_RIGHT`'s own doc for the numbers). This also
explains the "box next to its feet, not its face" report: the round-one point undershot the real
reach by roughly 4-5 blocks.

**Two behaviour fixes from the same report:**
- The goal now stops approaching once within the bite's own maximum reach (padded from where the
  real path can land) instead of closing all the way to touching range and then swinging backward
  for the bite — it tries to fight from the distance the attack actually needs, not point-blank.
- Attack selection was restructured to the same shape as `GreatIzuchiCombatGoal`'s (filter
  candidates by range, discourage repeating the last choice, break ties randomly) even though
  there's still only one candidate today — distance can influence which attack gets picked once a
  second one exists, without ever guaranteeing the same choice at the same distance every time, per
  your explicit ask ("shouldn't guarantee so it's not spamming").

- [x] **New: does the bite now land at a believable distance and height**, rather than the box
      reading as being near the feet? **Confirmed live** — reads right now, no longer near the feet.
- [x] **New: does it now hold its ground at roughly biting range** instead of closing all the way
      to contact first? **Confirmed live** — every attack fired from 5.4-6.0 blocks out (the edge of
      the new range band), 8 of 9 landed; the one miss at max range is expected variance for a path
      from a single capture, not a bug.
- [ ] **New: bites from both sides now** — `attack_charge_bite_left` was added alongside
      `attack_charge_bite_right`, mirrored from the same measured path (not a separate capture: the
      right bite's real data already showed no consistent left/right bias, and a left/right clip pair
      is ordinarily authored as a mirror of one another). Selection alternates rather than always
      picking the same angle — flag it if the left one looks off, since it hasn't been checked live
      on its own.
- [ ] **New: roars once on first engagement, same mechanic as Great Izuchi** — GameTest-covered on
      this species directly as of R0a (`rathianRoarRunsForItsRealClipLength`: its own 99-tick clip
      runs down at one tick per real tick, with no attack or damage to an in-range target for the
      duration), not only via Great Izuchi's shared goal. Still worth a live look — Rathian's own
      roar clip hasn't been watched play yet, and no GameTest can judge that.
- [ ] Renders, spawns via egg, idles/walks/runs with correct animation
- [ ] **F3+B: do the boxes meet edge-to-edge with no visible gap**, and **does the tail chain read as
      roughly centred on the tail across a few seconds of watching it sway**, rather than checking
      only one instant? Flag anything still off and I'll keep adjusting that specific part
- [ ] Turning away no longer makes the head/neck suddenly vanish while still on screen
- [ ] Hitting a hurtbox (try the head, then the tail tip) reduces health
- [x] **New: targets pillagers on sight, the same as Great Izuchi** — GameTest-covered
      (`rathianTargetsAPillagerOnSight`), also worth a quick look live if a raid/patrol is nearby
- [ ] Death removes the whole creature and all nine parts
- [ ] No flight yet — it should behave as a purely ground-bound creature; this is expected, not a bug

## Rathalos (P4 ground wyvern, ground-only)

**Using Rathian's real measurements as a proxy**, per your call ("rathalos and rathian are almost
identical, you can use the numbers for one on the other"): no `[rathalos]` bone-probe lines have
shown up in a log yet, but the two species share the same base skeleton and closely similar
proportions, so Rathian's offsets are plugged in directly rather than waiting on a Rathalos-specific
session — refreshed again this round to match Rathian's round-five numbers (the model's own
`*Hitbox` locator bones, not inferred mesh-bone positions). Should be far closer than the old offline-solved/hand-corrected
guess, though not guaranteed pixel-perfect the way an actual Rathalos measurement would be — if any
one part still looks off, that's the part worth a real session for. Its attack clips are blocked
regardless: they reference bones that don't exist anywhere in this species' model at all, confirmed
by checking, not guessed — see `docs/DEFERRED.md`. That needs actual art/model-editor work before
it's even worth wiring.

Also fixed a few rounds ago: same culling fix as Rathian/Great Izuchi.

**MHW-style opening roar wired this round**, same shared `RoarGoal`/`Roarable` mechanic as Great
Izuchi/Rathian — works independently of the broken attack clips above, since it's just a presentation
clip with no attack-volume mechanics of its own.

- [ ] Renders, spawns via egg, idles/walks/runs (walk uses `walk_normal`/`walk_aggro`, no separate
      "run" clip exists for this species — expected, not a bug)
- [ ] **F3+B: closer now?** Flag any part that's still clearly off — that's the one worth a live
      measurement session for
- [ ] Turning away no longer makes the head/neck suddenly vanish while still on screen
- [ ] Hitting a hurtbox reduces health
- [ ] Attacks and damages a nearby player using ordinary melee
- [x] **New: targets pillagers on sight, same as Great Izuchi/Rathian/Izuchi** — GameTest-covered
      (`rathalosTargetsAPillagerOnSight`)
- [ ] **New: roars once on first engagement** — GameTest-covered on this species directly as of R0a
      (`rathalosRoarRunsForItsRealClipLength`; since Rathalos uses vanilla melee and has no synced
      attack id, the victim's health is what proves it doesn't attack mid-roar), not only via Great
      Izuchi's shared goal. Worth a live look since this species' own roar clip hasn't been watched
- [ ] Death removes the whole creature and all seven parts
- [ ] No flight yet — ground-bound only; expected, not a bug
