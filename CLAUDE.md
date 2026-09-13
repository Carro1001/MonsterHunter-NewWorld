# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this
repository.

## Read this first if you're picking this up cold

This is a from-scratch revival of an older Forge 1.20.1 mod. **Superseded as of 2026-09-11:** the
revival is no longer a side branch — `revival/neoforge-1.21.1` was squash-merged into `master`
(`ab59e19`), so `master` *is* the NeoForge 1.21.1 rewrite and everything below describes it. The
old Forge/MultiHitBoxLib/SmartBrainLib codebase survives only in history and on `origin/brain`.

New feature work is scoped by `docs/ROADMAP.md` (revision `mh-nw-roadmap-2026-09-11-v4`) and the
numbered packets it hands out, e.g. `docs/R0_BASELINE_HANDOFF.md` and
`docs/R1_FIRST_HUNTING_LOOP_HANDOFF.md`. Work the current packet; do not
re-read or restart the historical P0-P8 runbook. `docs/REVIVAL_HANDOFF.md` (70 KB) is background for
*why* the architecture looks like this, not the work queue.

For "where exactly did we leave off": `docs/TEST_PLAN.md` has a dated, round-by-round history under
each species' section (what changed, why, and what's still an open checklist item) and states the
current GameTest count at the top. `docs/DEFERRED.md` lists everything consciously postponed, with
enough context to pick each item up without needing this conversation's history. Both are updated
every round of work, not just at milestones — treat a stale-looking entry as a sign something was
missed, not as the current source of truth.

**Standing constraints that apply to any future work on this branch, unless told otherwise:**
- **Superseded:** the old "`revival/neoforge-1.21.1` stays local-only, never pushed" rule no longer
  describes reality — both that branch and `master` exist on `origin`. The standing rule is now:
  **no further push or publish without explicit authorization**, per push, not implied by the
  branches already being remote.
- **Attribution is per agent — sign as whichever one you actually are.** Commits end with a
  `Co-Authored-By:` trailer, PRs additionally with a `🤖 Generated with` line:
  - Claude Code: `Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>`, `🤖 Generated with
    [Claude Code](https://claude.com/claude-code)`.
  - Codex: `Co-Authored-By: Codex Sonnet 5 <noreply@anthropic.com>`, `🤖 Generated with [Codex]`.

  Attribution rules handed to your session directly win over this bullet — that is why R0b's own
  commits say `Claude Opus 5`.
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
- Repository guidance lives **only** in this file. `AGENTS.md` is a pointer to it, on purpose: the
  two were near-verbatim copies and drifted twice before a PR review caught it. Add guidance here;
  don't restore a second copy.

## What this is

A Minecraft NeoForge mod (`mhnw`, "Monster Hunter: New World") adding Monster Hunter-style
creatures — currently Great Izuchi, Izuchi (small), Rathian, Rathalos, Aptonoth, Lagiacrus, Toad,
Flashbug, Bug — with GeckoLib-animated models and, for the large monsters, part-based hurtboxes.

As of R1 it also has a survival loop: Great Izuchi, small Izuchi and Aptonoth leave carvable corpses,
carving is the only way to get their materials, and those materials cook and craft into bone armor.
As of R2 there are three field-preparation loops on top of that: a portable BBQ spit, glass-bottle
Flashbug capture into a throwable flash bomb, and water-bucket capture/release of all four toad
variants. As of R3 there is one weapon, the Giant Jawblade, crafted from those same carve
materials.

Species notes that are easy to get wrong from an older doc:
- **Lagiacrus is ported**, not planned: a limited *movement* baseline (seven native parts, amphibious
  navigation, bounded land pursuit with no outgoing damage, underwater breathing). Its bank-exit
  GameTest proves bounded failure cleanup, not successful climbing — that limitation is deliberate
  and documented in `docs/DEFERRED.md`, not a bug to fix in passing.
- **Rathian** has a real measured/mirrored bite timeline and the opening roar; it is not
  vanilla-melee-only.
- **Small Izuchi's** missing dedicated attack/death clips are an **accepted first-release
  presentation limitation** (roadmap v4), not a prerequisite for R1. See "Attack timeline" below.
- **Toad and Flashbug** endemic behaviour ships as-is and is retained; R2 extended it rather than
  redesigning it — both are now capturable, and the Flashbug's flash moved into a shared helper,
  but neither species' trigger, telegraph, radius or one-release discard changed.
- Still unported: Zinogre, Deviljho, Blango, Blangonga.

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
run on, and provisions its own JDK 21 toolchain for the actual compilation. As of R0a (2026-09-11)
the plain wrapper invocation works on this machine with no override at all —
`./gradlew.bat --no-daemon build runGameTestServer` under the ambient `JAVA_HOME` (Temurin 25) —
so the old `JAVA_HOME="C:\Program Files\Java\jdk-22"` prefix is no longer required; use it only if
Gradle's own startup actually fails, and don't hardcode another contributor's JDK path. A build/test
round on this machine takes roughly 30 seconds.

Combat/behaviour diagnostics: `/mhnw debugcombat` toggles logging in-game (op-only) — see
`MHNWCommands`/`MHNWConfig`. With it on, `BoneProbe` (client-only) logs every named GeckoLib bone's
measured world position, converted into the same left/up/forward local frame every species'
`localToWorld` uses, and `AttackVolumeOverlay` draws Great Izuchi's live attack volume with F3+B.
This is the only sane way to get real hurtbox/attack numbers — see "Hurtboxes are static offsets"
below for why guessing offline doesn't work.

Key versions (`gradle.properties`): Minecraft 1.21.1, NeoForge 21.1.248, Java 21, Parchment mappings
`2024.11.17`, GeckoLib 4.9.2, TerraBlender 4.1.0.8, Gradle wrapper 9.2.1. Mod group/package is `com.carro1001.mhnw`. These
are deliberate, sourced pins (handoff section 7.3) — don't float them to "latest" without checking
that section first. As of R1a there are **two external dependencies, GeckoLib and TerraBlender** (the latter
required on both sides, range `[4.1.0.8,4.2)`, from Forge Maven; it is what makes
`mhnw:verdant_hunting_grounds` actually generate, and it is deliberately not jar-in-jarred);
the old codebase's MultiHitBoxLib,
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
  `MonsterPart`/`AttackProfile`/`CarveState`/`FlashEffect` helpers — flat, not nested under
  per-species subpackages. `FlashBombProjectile` lives here too: it is an entity, not an item.
- `com.carro1001.mhnw.animation`: `ServerTimedAnimationController` (R0b). One class,
  common-loadable by design.
- `com.carro1001.mhnw.client`: `MHNWClient` (renderer registration; each renderer is a small nested
  static class in this one file, not a separate `*Renderer.java` per entity — e.g.
  `MHNWClient.RathianRenderer extends GeoEntityRenderer<Rathian>`), `BoneProbe` (the measurement
  tool described above), `AttackVolumeOverlay` (F3+B attack-volume drawing),
  `AnimationSeekSelfCheck` (the R0b sampler probe -- it lives here, not in `MHNWGameTests`, because
  `GeoModel` references `Minecraft` and NeoForge's `RuntimeDistCleaner` refuses to load it on a
  dedicated server; any future test needing GeckoLib's real sampler has to be a client probe too).
- `com.carro1001.mhnw.registry`: `ModEntities` (entity types + their spawn eggs) and `ModItems`
  (R1's carve materials, meats and the four bone-armor pieces). Two `DeferredRegister<Item>` holders
  into the same registry, deliberately: the eggs are shipped registrations whose only fault is
  living in a class named after entities, and moving them buys a prettier name for a rename risk.
- `com.carro1001.mhnw.item`: `BoneArmorItem` — all four slots, one class, iron stats, and the
  `mhnw:bone_armor_set_bonus` full-set trait — plus R2's `BarbecueSpitItem`, `FlashBombItem` and
  `ToadBucketItem`.

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

### Presentation is aged, not restarted (R0b)

Every finite, server-timed clip -- the timed attacks, the three opening roars, the four authored
deaths -- plays at its *real* age, so a client that starts rendering mid-action sees the current
phase instead of replaying the windup while the server lands the hit.
`animation/ServerTimedAnimationController` does it, and the **convention is deliberately the one
that already existed**: a controller with transition length `L` blends for `L` ticks and only then
starts the clip at zero, so an on-time observer has always shown `clipTime = age - L`. The adapter
reproduces that function for everybody rather than inventing a new one, which is why no measured
attack path, active window or accepted contact frame moved. `L` is `TRANSITION_TICKS` on each
species (5, or 6 for Aptonoth). **Do not "simplify" this to feeding raw action age in as clip
time** -- that shifts every measured attack by five ticks, and a GameTest fails if you try.

GeckoLib 4.9.2 has no public seek (verified against the sources jar the R0b handoff names, SHA-256
`009055c5...db99ee41`); `forceAnimationReset()` reloads rather than seeks, and a speed modifier
multiplies elapsed time rather than moving it. The only lever is `tickOffset`, which is `protected`.
A cold controller also cannot just be advanced: `process` polls its queue only while the adjusted
tick is zero, so advancing first leaves it with a correct number and no clip at all -- observed
live, not assumed. Hence: run GeckoLib's own initialization pass first, then re-anchor and let it
sample again in the same render call. Don't reflect into it, copy the processor or fork the library.

Each `mainAnim` picks the clip, the presentation instance and the clock from **one** priority
decision (death, roar, attack, locomotion) -- splitting them is how a death pose ends up driven by a
stale attack clock. Roars and deaths carry synced start-time anchors; the death one is stamped as
`gameTime - deathTime`, which reconstructs itself on load from vanilla's own saved `DeathTime`
without persisting anything of ours and without calling `die()` twice. No body's lifetime was
extended; see `docs/DEFERRED.md` on Rathian's and Rathalos's death clips still being cut short by
vanilla's 20-tick removal.

### Attack timeline: Great Izuchi and Rathian, so far

`GreatIzuchiCombatGoal`/`RathianCombatGoal` (vanilla `Goal`s, not a Brain-system behaviour — there is
no SmartBrainLib dependency on this branch) each own their species' whole combat loop: target
approach, orientation, attack selection, and the attack's phase timeline (`WINDUP` → `ACTIVE` →
`RECOVERY`, derived from a single "action age" counter rather than several counters that could
disagree). Everything that differs between individual attacks (range band, damage, active window,
the measured limb path) lives in `AttackProfile`, a small data-only record each goal reads from —
see either goal's own doc comment for the phase table and the exact contract. Rathian's is a
deliberate duplicate of Great Izuchi's rather than a shared base class — see `RathianCombatGoal`'s
own doc for why generalizing now would be premature (two implementations isn't the same as knowing
what they need to share).

Every attack-volume path in either goal is a *measured* keyframe track from a live `BoneProbe`
capture, not an offline guess — offline solving from keyframed, MoLang-heavy attack clips has been
tried and confirmed untrustworthy twice (Great Izuchi's claw, then again investigating Rathian's).
Rathian's `attack_charge_bite_left` is the one exception worth knowing about: it's mirrored from the
measured `attack_charge_bite_right` path rather than its own capture, on the reasoning that the
right bite's real data already showed no consistent left/right bias — replace it with its own
measurement if a capture of the left clip ever shows that assumption was wrong.

Rathalos and Izuchi still fight with ordinary vanilla `MeleeAttackGoal`/`Mob.doHurtTarget` and have
**no custom attack presentation** — a deliberate, documented P4 gap (`docs/DEFERRED.md`), not an
oversight:
- Rathalos's real attack clips exist in its `.geo.json`/animation files but aren't wired to any
  attack volume; its four melee clips reference 14-17 bone names each that don't exist anywhere in
  its own geometry (confirmed, not assumed — a model-editor retarget or a new clip is needed before
  those can play correctly at all).
- Izuchi (small) has no attack or death clip in its own preserved asset at all (idle/sleep/walk/run
  only). A candidate attack/death set exists on the archived `origin/brain` branch
  (`legacy/candidate-art-brain-branch/izuchi.*`), but every attack/death/roar/rally clip in it
  references bones (`left_shoulder`, `right_shoulder`, `left_ankle`, `right_ankle`, `mane`,
  `tailblade`, `left_hand`, `right_hand`) that belong to Great Izuchi's richer skeleton, not
  Izuchi's own — confirmed directly by diffing each clip's referenced bones against Izuchi's own
  `.geo.json`. This needs either a real retarget, a newly authored clip, or explicit approval to
  reuse an existing clip as a labelled placeholder — a decision left to the maintainer, not made
  unilaterally.

### The opening roar (`RoarGoal`/`Roarable`)

A small MHW-style mechanic shared by Great Izuchi, Rathian and Rathalos: a monster roars once on
first engaging a target, not again mid-fight, and re-arms only after a real disengage (no target for
a few real seconds, not a one-tick flicker). `Roarable` is a small marker interface (same generic-goal
pattern as `MonsterPart`/`BoneProbe`, not a shared base class); `RoarGoal<T extends Mob & Roarable>`
owns the engage/re-arm state machine and sits above each species' own combat/melee goal in priority
so the roar genuinely freezes the fight. Works independently of an attack timeline existing at all —
Rathalos has it despite its broken attack clips, since a roar is just a presentation clip.

**A goal that tracks real elapsed time must override `requiresUpdateEveryTick()` and use game time,
not a per-call tick counter.** `Mob.serverAiStep()` only polls a *non-running* goal's `canUse()`
every other real tick, and only ticks a *running* goal's own `tick()` every real tick if that goal's
`requiresUpdateEveryTick()` returns true (the combat goals already override this for their own
windup/active/recovery timing). `RoarGoal` initially missed this for its own countdown, which
silently made the roar take twice as long as the clip's real duration to let go — the clip finished
and held its last frame (its own authored `hold_on_last_frame` loop mode) well before the goal's
countdown reached zero, which read live as "froze after the roar."

**A dead entity stops running its goals entirely, permanently, for the whole corpse-hold window a
species with an authored death clip needs — not just until some in-goal check catches it.**
`LivingEntity.travel()` gates the call to `serverAiStep()` (which ticks every goal) behind
`!isImmobile()`, and `isImmobile()` is `isDeadOrDying()`; once that's true, no goal — including one
that explicitly checks `isAlive()` inside its own `tick()`, as both combat goals and `RoarGoal` do —
gets ticked again to act on it. A synced field like `getAttackId()`/`getRoarTicks()` therefore simply
freezes at whatever value it held the instant death began, rather than clearing. This is harmless in
practice only because `mainAnim()` checks `isDeadOrDying()` before reading any of that state, so
presentation is correct regardless — but don't expect an in-goal `isAlive()` guard to actually fire
under normal death; a GameTest that kills a monster mid-action and expects the goal's own synced
state to self-clear will fail, not because combat is broken, but because goal ticking stopped first.

### Carving, corpses and the R1 economy

`entity/CarveState.java` is the whole contract, held by composition in exactly three species
(`GreatIzuchi`, `Izuchi`, `Aptonoth`) which forward five things to it — damage, interaction, death,
save, load — and own none of it themselves. Composition, not a base class, because `Monster` and
`Animal` are different superclasses; three fixed consumers do not get a capability, an attachment, a
corpse entity or a generic loot service.

Three facts worth not rediscovering:

- **The corpse timer is vanilla's own `deathTime`, not a field of ours.** The window is 12,000 ticks
  of *this entity ticking*, which is exactly what `deathTime` counts: it advances in `tickDeath`,
  pauses for free while a chunk is unloaded or the server is down, and vanilla already saves it as
  `DeathTime` (a short — 12,000 fits). Holding the body is just declining to call
  `super.tickDeath()` until it gets there. A second saved counter would be the same number written
  twice. This also means R0b's death anchor (`gameTime - deathTime`) keeps working unchanged.
- **`Mob.interact` is `final` and returns `PASS` for anything not alive**, so a corpse can only be
  reached through `interactAt` — which the client tries first anyway. `MonsterPart.interactAt`
  forwards to the parent purely so a five-block-long body is carvable from somewhere other than the
  narrow root envelope under its chest.
- **A dead mob is still subject to `Mob.checkDespawn`'s distance rule**, which would delete a body
  long before the window expires. Each carvable species calls `setPersistenceRequired()` in `die`;
  the window deliberately does not force-load chunks.

Eligibility is recorded only when `super.hurt` both accepted the hit *and* health actually fell, so
absorbed, invulnerable and duplicated part damage grant nothing. Rewards are a deterministic table
indexed by that player's carve count, which is what makes a full-inventory retry exact without any
saved pending-roll state. The three species have empty loot tables under
`data/mhnw/loot_table/entities/`: carving is the only item-reward path, and normal XP still drops
once, at the real death, through vanilla.

Bone armor uses `ArmorMaterials.IRON` directly rather than a private copy of iron's numbers. The
set bonus is a **transient** attribute modifier recomputed from scratch by
`BoneArmorItem.refreshSetBonus` on every `LivingEquipmentChangeEvent` for an armor slot — transient
so it can never be written into saved attribute data and outlive the set, recomputed wholesale so
equip/unequip/death/rejoin need no separate hook and it cannot stack. The worn model is
`geo/entity/bone_armor.geo.json` (the complete eight-bone export) with the eight bone getters
overridden in `MHNWClient.BoneArmorRenderer`; the later `geo/item/armor/` copy uses GeckoLib's
default `armor*` names but has **no boot bones at all**, so a stock `GeoArmorRenderer` pointed at it
renders bare feet.

`entity/IzuchiHarassGoal.java` replaced small Izuchi's vanilla `MeleeAttackGoal`: bounded
circle → dart → retreat, ordinary `doHurtTarget` damage, no new clip and no attack timeline. At most
one Izuchi within 12 blocks darts at a time, enforced by reading `Izuchi.isDarting()` off the living
neighbours. Every field it owns is transient; a reload starts from nothing. It overrides
`requiresUpdateEveryTick()` for the same reason `RoarGoal` has to.

Two cancellation paths that are not obvious and were both missed in the first cut:

- **Peaceful difficulty is part of the goal's own precondition** (`canHarass`). Peaceful only
  despawns hostile mobs whose `shouldDespawnInPeaceful()` agrees, and `Izuchi` deliberately returns
  false — so without this a world switched to peaceful keeps the Izuchi, keeps its target, and keeps
  attacking. Vanilla's `MeleeAttackGoal` had the same hole; the R1 contract is what closes it.
- **Death clears the phase in `Izuchi.die`, not in the goal.** A dead mob never ticks a goal again
  (see the lifecycle note above), so `stop()` cannot run for a mob killed mid-dart and its state
  would freeze for the whole corpse window. The neighbours' `isAlive()` filter is still not
  redundant: it covers a body removed by `discard()`, where `die()` never runs at all.

### R2 field preparation

Three small loops, each built on a vanilla mechanism rather than a framework of ours. The packet's
own exclusion list is load-bearing: **no** generic consumable base, effect registry, ailment engine,
cooldown service or deployment/owner model was added, and none should be added to extend these.

**The BBQ spit is vanilla's held-use machinery, not a timer of ours.** `BarbecueSpitItem` declares
80 ticks from `getUseDuration`; vanilla counts it down and calls `finishUsingItem` exactly once, on
the server, only on a completed hold. That is why "cancelling consumes nothing" has no cancellation
code behind it — a release, a swap or a death simply never reaches that call. `stacksTo(1)` is what
makes the result transactional with no inventory arithmetic: the held stack is always exactly one
spit, so returning the cooked meat replaces its own slot. `ItemUtils.createFilledResult` supplies
the creative infinite-materials convention for free.

**`FlashEffect` is one static method with exactly two callers**, extracted only once the thrown
bomb made the wild Flashbug's flash genuinely repeat. Radius 5, line of sight, 0.65 horizontal
facing dot, players never affected, 40 ticks of Blindness 0 plus Movement Slowdown II. Its
line-of-sight test is point-based (`level.clip`) rather than `Entity.hasLineOfSight`, because a
thrown bomb's flash point is a spot in the air or just off a block face — there is no source entity
left by the time it runs. Do not turn this into an effect registry.

`FlashBombProjectile` deliberately **does not** override `onHitEntity`. `ThrowableItemProjectile`'s
own impact path deals no damage (vanilla's `Snowball` adds its own), so leaving the method alone is
what makes "deals no damage" true structurally, rather than a subtraction someone could delete.

**`ToadBucketItem` extends `BucketItem`, not `MobBucketItem`, for two concrete reasons.** Vanilla's
`MobBucketItem` keeps its spawn seam private and lets `finalizeSpawn` roll the creature's random
state *before* the bucket data is applied, so a bare `/give` stack would release a random variant —
exactly the case the four fixed item ids exist to get right. And its constructor takes an
`EntityType`, which would force resolving the deferred toad holder while items are still
registering. So only the tiny spawn seam is reproduced (`checkExtraContent`), and everything else —
placing the water, returning the empty bucket, the creative rule — stays `BucketItem`'s. The variant
is a property of the **item**, not the stack: `ModItems.toadBucket(variant)` is the single place the
enum and the legacy ids (`nitrotoad_bucket` for `BLAST`) are joined. `Toad.finalizeSpawn` also skips
its random roll for `MobSpawnType.BUCKET`, and `FromBucket` drives the same
`requiresCustomPersistence`/`removeWhenFarAway` pair vanilla's fish use.

**Blastoad attribution reuses `CarveState`'s rule, it does not add a second one.**
`CarveState.resolvePlayer` went from `private` to package-visible for this. `Toad.provokerId` is a
transient uuid recorded on the first hit of a fuse, cleared by `ToadFuseGoal.stop()`, and passed to
`level.explode` as the source entity — vanilla then builds a `PLAYER_EXPLOSION` damage source whose
causing entity is that player, so the damage reaches `CarveState` through the identical path as a
sword swing. A bucket release, a mob-triggered blast or a non-damaging variant credits nobody, and
nothing here guesses the nearest player.

**Two fixture facts that cost a test round each.**
`GameTestHelper.makeMockServerPlayerInLevel()` arrives **creative** (its anonymous subclass hard-codes
`isCreative()`), under which `ItemUtils.createFilledResult` deliberately keeps the input stack — so a
survival bucket/bottle transaction tested with it silently asserts the creative path instead.
And `Bucketable.bucketMobPickup` casts to `ServerPlayer` to award `FILLED_BUCKET`, so a detached
`makeMockPlayer` cannot catch a toad at all. Both are recorded in `docs/TEST_PLAN.md`'s R2 section.

### R3: one weapon, and it is a vanilla sword

`item/GiantJawbladeItem.java` is the whole packet. `mhnw:giant_jawblade` is a plain `SwordItem` on
`Tiers.IRON` whose only departures from an iron sword are its numbers -- 9.0 total attack damage and
0.8 attack speed, both expressed as vanilla's own attribute modifiers rather than constants read
back out -- and bone as its repair material.

Its one addition is a charged strike, and every part of it is borrowed:

- **The charge is vanilla's held use.** 30 ticks from `getUseDuration`, `UseAnim.SPEAR`, and
  `finishUsingItem` called once on the server only on a completed hold -- the same shape as R2's BBQ
  spit, for the same reason: "releasing early does nothing" needs no cancellation code, because a
  release never reaches that method. **Nothing is stored anywhere**: no field, no component, no
  attachment, no packet, so a reload cannot resume or cash in a charge.
- **The target query is `ProjectileUtil.getHitResultOnViewVector`**, the same block-clipped trace
  vanilla's projectiles use, out to 4.5 blocks. A wall stops the strike because the trace stops, not
  because of a check of ours, and `Level.getEntities` already includes NeoForge `PartEntity`
  instances, so a `MonsterPart` is selectable with no multipart-specific code.
- **The damage is `Player.attack`,** called at most once. That keeps attack events, enchantments,
  knockback, durability, sounds, stats and the player-caused damage source -- and therefore
  `CarveState` attribution -- on exactly the path a left-click uses. NeoForge's own patch to that
  method resolves a `PartEntity` to its parent for durability and post-attack effects.

There is no damage multiplier, cone, sweep, charge tier or combo, and no weapon/moveset abstraction:
one weapon does not tell you what two weapons would share. The 30-tick recovery cooldown applies on
a hit and on a miss, which is the whole cost of the longer reach.

**The weapon answers "no" to `SWORD_SWEEP`, and that is load-bearing.** Vanilla decides to sweep by
asking the held item, and every `SwordItem` says yes -- so a fully cooled strike dealt 1.0 to every
living thing within a block of the target, which is precisely the attack the charge produces (30
held ticks against a 25-tick delay is always fully cooled). Refusing the ability in
`canPerformAction` is the whole fix: no flag around the attack call, no per-player state, nothing
that can leak to another weapon. It costs this weapon its left-click sweep too, deliberately -- the
alternative is the transient state the packet forbids. A GameTest keeps a bystander standing
*beside* the target, because the in-line pair never enters sweep range and would never have caught
it.

**The presentation is a placeholder, by explicit maintainer decision.** Only the on-hand UV atlas
(`textures/item/giant_jawblade_model.png`) was ever delivered; there is no geometry bound to it and
no inventory icon, so `models/item/giant_jawblade.json` currently points at vanilla's iron sword
sprite. Swapping in the real art is a one-file model change -- no code, and no item id change. See
`docs/DEFERRED.md`.

### Registration and client wiring

`ModEntities` is the one `DeferredRegister` holder (entity types + spawn eggs together); attribute
suppliers are wired centrally in `MHNW.onAttributeCreation`, not per entity class. `MHNWClient`
(gated to the client dist by NeoForge's own event timing, not a `DistExecutor` split) registers one
renderer per entity as a small nested static class — except R2's `flash_bomb`, which points straight
at vanilla's `ThrownItemRenderer` and needs no class, model or texture of ours — most `extends GeoEntityRenderer<T>` directly
(GeckoLib 4.x needs no separate `EntityModel` class the way the old renderer pattern did); `Bug` is
the one exception, a plain `MobRenderer`/`BugModel` pair, since it isn't GeoLib-animated.

### Worldgen: one biome, placed by TerraBlender

`mhnw:verdant_hunting_grounds` (R1a) is a datapack biome — `registry/ModBiomes.java` holds only its
`ResourceKey` and the `#mhnw:spawns_hunting_wildlife` selector `TagKey`, deliberately not a
`DeferredRegister<Biome>`, since the data lives in JSON. `worldgen/HuntingGroundsRegion.java` is the
one TerraBlender region (weight 2) that makes it generate at all: registering a biome JSON and adding
spawn entries does nothing on its own, because the vanilla Overworld biome source never picks a key it
was not told about. Within its own weighted share the region swaps the `PLAINS` and `FOREST` climate
slots; nothing global is replaced.

`entity/HuntingSpawnRules.java` owns the automatic-spawn gate for every R1a species — habitat tag plus
the `naturalSpawning` server config, for **both** `NATURAL` and `CHUNK_GENERATION`. Its `isFree` tests
fluid with `containsAnyLiquid` over the whole spawn AABB, not the feet block: `noCollision` ignores
fluids, and the escort placement shares this helper for a 1.1-block-tall Izuchi, so a feet-only check
accepted one standing dry with its head underwater. Manual origins
(eggs, `/summon`, spawners, breeding) are never gated. Spawn entries live only in biome modifiers,
never in the biome JSON, so there is one owner of them.

### Data

Static datapack-style data (loot tables, spawn placement biome modifiers) lives under
`src/main/resources/data/mhnw/`. Generated data goes to `src/generated/resources/` via `runData` —
don't hand-edit files there. GeckoLib assets (`.geo.json`, `.animation.json`, textures) live under
`src/main/resources/assets/mhnw/{geo,animations,textures}/`.

`docs/ANIMATION_MANIFEST.json` is a generated inventory of every species' animation clips (name,
length in seconds/ticks, loop mode), covering every `.animation.json` under `assets/mhnw/animations/
entity/` — including species not yet ported (their assets already exist, entity code doesn't).
Regenerate it with `node tools/gen_animation_manifest.js` after adding or changing a species'
animation file; don't hand-edit the JSON itself, same rule as `src/generated/`. Check it before
asking "does this species have a clip for X" or re-deriving a clip's length by hand.

## Testing

`MHNWGameTests.java` (one file, `@GameTestHolder(MHNW.MOD_ID)`) is the entire automated suite, run
via `./gradlew runGameTestServer`. It deliberately covers only what a human at a screen cannot
reliably check and what would regress silently — damage semantics (one hit through a part costs
the parent exactly one hit, distinct attackers aren't conflated, damage only lands inside an
attack's active window), state-machine edges (reload cancels transient combat, death removes every
part exactly once), the R1 carving contract (attribution, per-player quota, atomic inventory,
persistence and the corpse window), the item/recipe registry, armor stats and the full-set modifier,
bounded Izuchi harassment, the R3 weapon contract (its observed attributes, the exact recipe
pattern, the 30-tick charge through a really ticked player, range/occlusion/off-axis target
selection, single-target-only damage against a multipart body, and miss recovery), the R2
preparation contract (both recipes and their exact inputs and
remainder, the 80-tick BBQ transaction and its exactly-once conversion, glass-bottle Flashbug
capture in survival and creative, the full flash eligibility matrix plus a genuinely thrown bomb,
all four toad variants' capture/release/round-trip including a bare `/give` stack, and Blastoad
carve attribution against a real carvable parent), ground pathing across open terrain, an outside corner, a body-width passage, a
too-narrow passage, a single-block step, and a fully sealed unreachable target, and — as of the R0a
baseline packet — real-tick timing: that the opening roar counts down one tick per *real* tick for
its whole clip on all three roaring species, that the disengage re-arm honours its real 100-tick
interval, that a monster killed mid-roar/mid-attack deals no further damage and leaves NeoForge's
part lookup, and that a full `saveWithoutId`/`load` round trip keeps health and parts while dropping
the transient action (saved mid-attack for Great Izuchi, mid-roar for Rathian, so neither
assertion is vacuous). Timing tests anchor their deadline to an observed countdown rather than a
broad timeout, so a half-rate countdown fails them (verified by reintroducing the bug). It deliberately does **not** cover whether a texture renders, an animation looks
right, or a hurtbox visually sits on the body — those need `docs/TEST_PLAN.md`'s human checklist.
Current test count and pass status are stated at the top of `docs/TEST_PLAN.md`; keep that number in
sync when adding tests.

**The GameTest arena is enclosed in a barrier cage with a lid.** Its spawn heightmap therefore sits
above the roof and the whole interior reads as "not the surface" — which is correct, and which is why
`HuntingSpawnRules`' surface rule cannot be accepted positively inside an arena. Do not "fix" that by
building a fixture above the lid: that writes outside the test's own bounds into a world shared with
the tests running concurrently beside it, and it produced real intermittent failures once already. See
the R1a fixture note in `docs/TEST_PLAN.md`.
