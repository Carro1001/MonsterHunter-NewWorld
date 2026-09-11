# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this
repository.

## Read this first if you're picking this up cold

This is a from-scratch revival of an older Forge 1.20.1 mod, in progress on the
`revival/neoforge-1.21.1` branch. **`master` is the old, larger, Forge/MultiHitBoxLib/SmartBrainLib
codebase and is not what you are working on** — everything below describes the revival branch only.
The full plan, rationale, and phase-by-phase runbook live in `docs/REVIVAL_HANDOFF.md`; read that
before starting new feature work, not just this file.

For "where exactly did we leave off": `docs/TEST_PLAN.md` has a dated, round-by-round history under
each species' section (what changed, why, and what's still an open checklist item) and states the
current GameTest count at the top. `docs/DEFERRED.md` lists everything consciously postponed, with
enough context to pick each item up without needing this conversation's history. Both are updated
every round of work, not just at milestones — treat a stale-looking entry as a sign something was
missed, not as the current source of truth.

**Standing constraints that apply to any future work on this branch, unless told otherwise:**
- `revival/neoforge-1.21.1` stays **local-only, never pushed**, until the maintainer explicitly says
  otherwise.
- Commits end with `Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>` (PRs additionally with
  `🤖 Generated with [Claude Code]`).
- KISS/YAGNI: this rewrite is deliberately much smaller than the old `master` codebase (see below) —
  don't reintroduce abstraction layers (a Brain-AI framework, a third-party multipart library, a
  Model/Renderer split per entity) that the port already decided not to carry over, unless a real
  need shows up, not a hypothetical one.
- Prefer a GameTest over a manual test whenever the behaviour is server-side and verifiable
  headlessly (damage, timing, state machines, save/reload, navigation). Anything visual (does a
  hurtbox sit on the model, does an animation look right) needs a human at a screen — see
  `docs/TEST_PLAN.md`'s own framing of that split.
- After any code change: rebuild and run the full GameTest suite (see below), update
  `docs/TEST_PLAN.md`/`docs/DEFERRED.md` as needed, then commit.

## What this is

A Minecraft NeoForge mod (`mhnw`, "Monster Hunter: New World") adding Monster Hunter-style
creatures — currently Great Izuchi, Izuchi (small), Rathian, Rathalos, Aptonoth, Toad, Flashbug, Bug —
with GeckoLib-animated models and, for the large monsters, part-based hurtboxes. More species
(Zinogre, Deviljho, Lagiacrus, Blango, Blangonga) are planned per the handoff's runbook but not yet
ported.

## Build & run

Standard NeoForge ModDevGradle project (`net.neoforged.moddev` plugin, not classic ForgeGradle). Use
the wrapper:

- `./gradlew build`: compile and build the mod jar.
- `./gradlew runClient`: launch a dev client with the mod loaded.
- `./gradlew runServer`: launch a dev dedicated server.
- `./gradlew runData`: run data generators; output goes to `src/generated/resources/`.
- `./gradlew runGameTestServer`: run every registered GameTest headlessly and exit non-zero on
  failure. This is the test suite — there is no separate unit test framework.

**JDK note:** the project compiles to Java 21 (`java.toolchain.languageVersion`, since Mojang ships
Java 21 to end users on 1.21.1). Gradle itself (wrapper pinned to `9.2.1`) also needs a JDK it can
run on; if the machine's default `java`/`JAVA_HOME` doesn't work for Gradle's own startup, point
`JAVA_HOME` at one that does for `gradlew` invocations. The working invocation used throughout this
branch's own session history on this machine:
`JAVA_HOME="C:\Program Files\Java\jdk-22" ./gradlew.bat --no-daemon build runGameTestServer`
(bash-style quoting; adjust the path if the local JDK 21/22 install lives elsewhere). A build/test
round on this machine takes roughly 30 seconds.

Combat/behaviour diagnostics: `/mhnw debugcombat` toggles logging in-game (op-only) — see
`MHNWCommands`/`MHNWConfig`. With it on, `BoneProbe` (client-only) logs every named GeckoLib bone's
measured world position, converted into the same left/up/forward local frame every species'
`localToWorld` uses, and `AttackVolumeOverlay` draws Great Izuchi's live attack volume with F3+B.
This is the only sane way to get real hurtbox/attack numbers — see "Hurtboxes are static offsets"
below for why guessing offline doesn't work.

Key versions (`gradle.properties`): Minecraft 1.21.1, NeoForge 21.1.248, Java 21, Parchment mappings
`2024.11.17`, GeckoLib 4.9.2, Gradle wrapper 9.2.1. Mod group/package is `com.carro1001.mhnw`. These
are deliberate, sourced pins (handoff section 7.3) — don't float them to "latest" without checking
that section first. **GeckoLib is the only external dependency**; the old codebase's MultiHitBoxLib,
SmartBrainLib, mixin-booster, JEI, and Jade integrations were not carried over (see Architecture).

## Architecture

This is a much smaller codebase than the old `master` branch (roughly 5,000 lines across everything
under `src/main/java/com/carro1001/mhnw/`, GameTests included) — there is no `NewWorldEntity`
hierarchy, no Brain-AI framework, no third-party multipart or hitbox-profile-JSON system. Entities
extend a vanilla base (`Monster`, `Animal`) directly and implement GeckoLib's `GeoEntity` directly;
each large monster hand-rolls its own small amount of shared logic (`localToWorld`, `positionParts`,
the fairness-corrected `hurt` guard) rather than inheriting it, and the repeated doc comments across
`Rathian`/`Rathalos`/`Aptonoth`/`GreatIzuchi` note this as a real, acknowledged duplication —
described in each class as "a genuine extraction candidate once enough of this is common to justify
the risk of restructuring already-shipped code," not an oversight.

### Package layout

- `com.carro1001.mhnw`: `MHNW` (the `@Mod` entry point — registers entities, attributes, spawn
  placements, creative tab contents, GameTests, and the `/mhnw` command), `MHNWCommands`,
  `MHNWConfig` (client/server config, including `debugCombat`), `MHNWGameTests` (the entire GameTest
  suite, one file).
- `com.carro1001.mhnw.entity`: every entity class, its species-specific `Goal`s, and the shared
  `MonsterPart`/`AttackProfile` helpers — flat, not nested under per-species subpackages.
- `com.carro1001.mhnw.client`: `MHNWClient` (renderer registration; each renderer is a small nested
  static class in this one file, not a separate `*Renderer.java` per entity — e.g.
  `MHNWClient.RathianRenderer extends GeoEntityRenderer<Rathian>`), `BoneProbe` (the measurement
  tool described above), `AttackVolumeOverlay` (F3+B attack-volume drawing).
- `com.carro1001.mhnw.registry`: `ModEntities`, the one `DeferredRegister` holder for entity types
  and their spawn eggs.

### Multipart hurtboxes: native NeoForge, not a library

Large monsters (`GreatIzuchi`, `Rathian`, `Rathalos`, and the passive `Aptonoth`) use `MonsterPart`
(`entity/MonsterPart.java`), which extends NeoForge's own `net.neoforged.neoforge.entity.PartEntity`
directly — **no MultiHitBoxLib or other third-party multipart dependency**, unlike the old `master`
branch. A part has no health of its own; `MonsterPart.hurt` forwards to the parent, which owns
health/mitigation/death, and the parent de-duplicates so one area effect touching several parts
still costs exactly one hit. Each owning entity keeps its own `MonsterPart[]`, its own
`localToWorld(left, up, forward)` (a yaw-only rotation of a local offset around the entity's
position), and calls `positionParts()` exactly once per tick, from `tick()` after `super.tick()`
returns (a real, fixed bug: calling it a second time from `aiStep()` as well corrupted an
interpolation "old" value, not just wasted work — see any of these classes' own `tick()` doc
comment if touching this again).

**Hurtboxes are static offsets, not live bone tracking — this is a load-bearing architectural fact,
not a limitation to "fix":** the dedicated server never runs GeckoLib's animation system at all,
only the client renders/animates bones, so a `MonsterPart`'s position must be a fixed number the
server can rely on unconditionally every tick; it can never literally track a currently-animating
bone. In practice this means every species' hurtbox constants are **measured** (via `BoneProbe`,
live, with `debugCombat` on) rather than computed from the model's animation data, and the
measurement methodology itself went through several rounds documented in `docs/TEST_PLAN.md`:
plain averaging over a few samples proved unstable (whichever slice of an idle sway got sampled more
biased the mean); range-midpoint (midpoint of observed min/max over ~25 samples) was adopted instead
as the robust-to-sampling-bias fix; and even the model's own dedicated `*Hitbox`-suffixed locator
bones (present in Great Izuchi's, Rathian's, and Rathalos's `.geo.json` files, e.g. `torsoHitbox`,
`headHitbox`) still sway with the idle animation just like any mesh bone, so a *narrow* capture of
even those needs the same range-midpoint treatment, not a single trusted sample. Read the current
Rathian/Rathalos constructor comments before changing any hurtbox number — they carry the exact
lesson-by-lesson history of what was tried and why it was wrong, and repeating an already-disproved
approach (a single mean, a flat directional nudge) wastes a full test round.

### Attack timeline: Great Izuchi only, so far

`GreatIzuchiCombatGoal` (a vanilla `Goal`, not a Brain-system behaviour — there is no SmartBrainLib
dependency on this branch) is the sole owner of Great Izuchi's combat: target approach, orientation,
attack selection, and the attack's phase timeline (`WINDUP` → `ACTIVE` → `RECOVERY`, derived from a
single "action age" counter rather than several counters that could disagree). Everything that
differs between individual attacks (range band, damage, active window, which part(s) act as the
attack volume) lives in `AttackProfile`, a small data-only class this goal reads from — see either
file's own doc comment for the phase table and the exact contract.

Every other current monster (`Izuchi`, `Rathian`, `Rathalos`) fights with ordinary vanilla
`MeleeAttackGoal`/`Mob.doHurtTarget` and has **no custom attack presentation yet** — this is a
deliberate, documented P4 gap (`docs/DEFERRED.md`), not an oversight:
- Rathian's and Rathalos's real attack clips exist in their `.geo.json`/animation files but aren't
  wired to any attack volume; Rathalos's four melee clips reference 14-17 bone names each that don't
  exist anywhere in its own geometry (confirmed, not assumed — a model-editor retarget or a new
  clip is needed before those can play correctly at all).
- Izuchi (small) has no attack or death clip in its own preserved asset at all (idle/sleep/walk/run
  only). A candidate attack/death set exists on the archived `origin/brain` branch
  (`legacy/candidate-art-brain-branch/izuchi.*`), but every attack/death/roar/rally clip in it
  references bones (`left_shoulder`, `right_shoulder`, `left_ankle`, `right_ankle`, `mane`,
  `tailblade`, `left_hand`, `right_hand`) that belong to Great Izuchi's richer skeleton, not
  Izuchi's own — confirmed directly by diffing each clip's referenced bones against Izuchi's own
  `.geo.json`. This needs either a real retarget, a newly authored clip, or explicit approval to
  reuse an existing clip as a labelled placeholder — a decision left to the maintainer, not made
  unilaterally.

### Registration and client wiring

`ModEntities` is the one `DeferredRegister` holder (entity types + spawn eggs together); attribute
suppliers are wired centrally in `MHNW.onAttributeCreation`, not per entity class. `MHNWClient`
(gated to the client dist by NeoForge's own event timing, not a `DistExecutor` split) registers one
renderer per entity as a small nested static class — most `extends GeoEntityRenderer<T>` directly
(GeckoLib 4.x needs no separate `EntityModel` class the way the old renderer pattern did); `Bug` is
the one exception, a plain `MobRenderer`/`BugModel` pair, since it isn't GeoLib-animated.

### Data

Static datapack-style data (loot tables, spawn placement biome modifiers) lives under
`src/main/resources/data/mhnw/`. Generated data goes to `src/generated/resources/` via `runData` —
don't hand-edit files there. GeckoLib assets (`.geo.json`, `.animation.json`, textures) live under
`src/main/resources/assets/mhnw/{geo,animations,textures}/`.

## Testing

`MHNWGameTests.java` (one file, `@GameTestHolder(MHNW.MOD_ID)`) is the entire automated suite, run
via `./gradlew runGameTestServer`. It deliberately covers only what a human at a screen cannot
reliably check and what would regress silently — damage semantics (one hit through a part costs
the parent exactly one hit, distinct attackers aren't conflated, damage only lands inside an
attack's active window), state-machine edges (reload cancels transient combat, death removes every
part exactly once), and — as of the A10 navigation work — ground pathing across open terrain, an
outside corner, a body-width passage, a too-narrow passage, a single-block step, and a fully sealed
unreachable target. It deliberately does **not** cover whether a texture renders, an animation looks
right, or a hurtbox visually sits on the body — those need `docs/TEST_PLAN.md`'s human checklist.
Current test count and pass status are stated at the top of `docs/TEST_PLAN.md`; keep that number in
sync when adding tests.
