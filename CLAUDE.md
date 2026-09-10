# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A Minecraft Forge mod ("Monster Hunter: New World", mod id `mhnw`) that adds Monster Hunter-style
large monsters (Rathalos, Rathian, Zinogre, Deviljho, Lagiacrus, Izuchi/Great Izuchi, etc.), with
GeckoLib-animated models, part-breaking hitboxes, and MH-style AI (aggression states, rage/exhaust
buildup, rally mechanics).

Build tooling is Gradle via ForgeGradle. There is no CMake in this repo despite the name similarity.

## Build & run

Standard ForgeGradle project. Use the wrapper (`./gradlew` on bash, `gradlew.bat` on plain
PowerShell/cmd):

- `./gradlew build`: compile and build the mod jar.
- `./gradlew runClient`: launch a dev client with the mod loaded.
- `./gradlew runServer`: launch a dev dedicated server.
- `./gradlew runData`: run Forge data generators; output goes to `src/generated/resources/`
  (declared as an extra resources source dir in `build.gradle`, so generated data ships with the mod).
- `./gradlew runGameTestServer`: run the gametest server run config.

There are no lint or test tasks configured beyond the standard Gradle/ForgeGradle ones; this repo
has no unit test suite.

**JDK note:** the project *compiles* to Java 17 (`java.toolchain.languageVersion`), but Gradle itself
(8.8, via the wrapper) must also *run* on a JDK it supports, and it cannot run on JDK 21+ builds like
25. If the machine's default `java`/`JAVA_HOME` is newer (check with `java -version`), point
`JAVA_HOME` at a JDK 17 install for `gradlew` invocations, e.g. on this machine:
`JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-17.0.11.9-hotspot" ./gradlew build`. Symptom of
getting this wrong: `Unsupported class file major version 69` (that's JDK 25) during Gradle's own
startup, before any project compilation happens.

Key versions (`gradle.properties`): Minecraft 1.20.1, Forge 47.3.22, Java 17, Parchment mappings
(`2023.09.03-1.20.1`). Mod group/package is `com.carro1001.mhnw`.

### Dependencies (all embedded/deobfuscated via `fg.deobf`)

- **GeckoLib**: animated entity models/renderers.
- **MultiHitBoxLib (MHLib)**: per-part hitboxes for monsters (breakable parts like tails, heads);
  hitbox layouts are JSON profiles under `src/main/resources/data/mhnw/multihitboxlib/hitbox_profiles/`.
- **SmartBrainLib (SBL)** + `mixin-booster`: brain/AI framework used alongside vanilla `Goal`s.
- **JEI**: compile/runtime only, for recipe viewing integration.
- **Jade** (via CurseMaven): waila/tooltip integration.

Custom repos are declared in `build.gradle` for each of these (Cloudsmith for GeckoLib/SBL,
CurseMaven for Jade, a custom Ivy pattern for GitHub Releases-hosted deps). If a dependency version
bump 404s, check whether the artifact moved between these repo patterns.

## Architecture

### Entity class hierarchy

Monsters build up through a layered abstract hierarchy in `entities/`:

- `NewWorldEntity`: base for all mod mobs: GeckoLib `GeoEntity` wiring, home position tracking,
  shared attribute defaults (`prepareAttributes()`), extended render distance.
- `NewWorldGrowingEntity`: adds growth/aging (`IGrows`).
- `NewWorldMonsterEntity`: the "large monster" base: implements `IMultipartEntity` (MultiHitBoxLib)
  for breakable parts, `IAttributes`, and `Enemy`. Carries synced state for aggression state, death
  state (multi-stage carving after death), limping, rally state, sleeping, tail-cut, rage
  buildup/exhaustion buildup, and current attack animation id. Concrete monsters (`RathalosEntity`,
  `RathianEntity`, `ZinogreEntity`, `DeviljhoEntity`, `LagiacrusEntity`, `IzuchiEntity`,
  `GreatIzuchiEntity`, ...) extend this and mostly configure attributes, goals, and animations.
- Smaller fauna (`AptonothEntity`, `BugEntity`, `ToadEntity`, `BlangoEntity`, `BlangongaEntity`,
  `FlashBugEntity`) extend `NewWorldEntity`/`NewWorldGrowingEntity` directly without the monster
  machinery.
- `MonsterBreakablePartEntity` (in `entities/helpers/`) is the MHLib sub-part entity representing a
  single breakable hitbox (e.g. a tail or head segment); `IMonsterBreakablePart.PART` enumerates part
  types. `TailEntity` is a standalone entity used both as a droppable tail and as a growing sub-model.

Custom AI lives in `entities/ai/`: a mix of vanilla `Goal` subclasses (`HitboxMeeleeAttackGoal`,
`MonsterAggressionStateGoal`, `RallyGoal`, `SleepGoal`, `ExhaustedStallGoal`, per-monster attack/stroll
goals), a `entities/ai/brain/` package for SmartBrainLib Brain behaviours, and pathing helpers in
`entities/ai/util/` (`MMPathFinder`, `MMPathNavigatorGround`, `SmartBodyHelper`). Check an individual
entity's `registerGoals()`/`getFightTasks()` before assuming which framework it uses; see next.

### AI: Goal system vs. Brain system

Minecraft/Forge has two parallel mob-AI frameworks; this mod has monsters on both, one at a time per
monster (Brain and Goals *can* coexist on the same entity, see below, but no monster mixes Brain-driven
and Goal-driven combat for the same behaviour):

- **Goal system** (vanilla, `net.minecraft.world.entity.ai.goal.Goal` + `GoalSelector`): what every
  monster except Great Izuchi uses. `DragonEntity.registerGoals()` is the fullest example
  (`NearestAttackableTargetGoal`, `MonsterAggressionStateGoal`, `DragonMeleeAttackGoal`,
  `DragonShootFireballGoal`, stroll/fly goals), and `RathalosEntity`/`RathianEntity` inherit it
  wholesale. `IzuchiEntity` still layers `HitboxMeeleeAttackGoal` on top of the
  `NewWorldMonsterEntity` base goals.
- **Brain system** (`net.minecraft.world.entity.ai.Brain`, driven via SmartBrainLib's
  `SmartBrainOwner`/`BrainActivityGroup`/`ExtendedSensor`/`ExtendedBehaviour` wrappers):
  **`GreatIzuchiEntity` runs on this now** (rewritten 2026-08-22, from scratch; see below, not from
  the stale `origin/brain` branch). `NewWorldMonsterEntity.serverAiStep()` (vanilla, `final`) always
  ticks `goalSelector` regardless of Brain use, so a Brain-based monster still keeps small Goals for
  behaviour the Brain doesn't own (`GreatIzuchiEntity.registerGoals()` keeps `FloatGoal`,
  `MonsterAggressionStateGoal`, `ExhaustedStallGoal`, `RallyGoal`, `SleepGoal`, deliberately *not*
  `super.registerGoals()`, since that also adds `NearestAttackableTargetGoal` and
  `WaterAvoidingRandomStrollGoal`, which would fight the Brain for targeting/movement each tick).
  `customServerAiStep()` calls `tickBrain(this)` and mirrors the Brain's attack-target memory onto
  vanilla `Mob#setTarget()`, since `MonsterAggressionStateGoal` (kept as a Goal) still reads that
  field directly rather than the Brain memory.
  `mixin-booster` (`org.sinytra.mixinbooster`) is pulled in specifically to make SmartBrainLib's mixins
  play with Forge on 1.20.1.

There's a stale `origin/brain` remote branch (forked before several `master` commits, including the
scaling fix) with a half-baked, never-finished SmartBrainLib migration of the small `IzuchiEntity`
(not Great Izuchi). It's superseded by the from-scratch `GreatIzuchiEntity` rewrite and not worth
merging; useful only as historical reference for what didn't work (it called
`.useMemory(NEAREST_ATTACKABLE)` on `TargetOrRetaliate` without ever registering the sensor that
populates that memory, among other things; the rewrite doesn't call `.useMemory()` at all and lets
`TargetOrRetaliate` fall back to its `NEAREST_VISIBLE_LIVING_ENTITIES` + `attackablePredicate` path,
which only needs `NearbyLivingEntitySensor`).

**Porting another monster to Brain:** follow `GreatIzuchiEntity` as the template (sensors, core/idle/
fight task groups, the `registerGoals()`-without-`super` pattern, the `customServerAiStep()` target
mirror). Reuse `HitboxAnimationAttack<E extends NewWorldMonsterEntity>`
(`entities/ai/brain/HitboxAnimationAttack.java`) for hitbox-based melee attacks instead of writing a
new one per monster; it's generic over any `NewWorldMonsterEntity` and replaces
`HitboxMeeleeAttackGoal` for Brain-based monsters (see next section). SmartBrainLib ships its own
`AnimatableMeleeAttack`, but that's a single instantaneous `doHurtTarget` hit after a delay: it does
not do the part-hitbox overlap checking this mod's combat model needs, so it doesn't replace
`HitboxAnimationAttack`.

### Hitbox-based hurt system

Monsters do **not** use Minecraft's normal single-bounding-box hurt/attack flow. Instead:

- Each `NewWorldMonsterEntity` implements MultiHitBoxLib's `IMultipartEntity`, and each monster has a
  hitbox profile JSON at `data/mhnw/multihitboxlib/hitbox_profiles/<name>.json` declaring `parts[]`
  (own AABB, whether it's `collidable`/`can-receive-damage`, a `damage-modifier`) and a
  `synched-bones` list; MHLib syncs each named part's world position every tick to the pivot of the
  matching GeckoLib bone in the monster's `.geo.json` model (`"sync-with-model": true`). Parts using
  the custom `mhnw:breakable` box type (registered in `ModHitboxTypes`/`BreakablePartHitboxType`) get
  wrapped as `MonsterBreakablePartEntity`, a real sub-entity with its own `hp`, `PART` type
  (`TAIL`/`HEAD`/`WING`/`CLAW`/`OTHER`), and `will_cut` behavior (e.g. severed tails spawn a
  standalone `TailEntity`, see `MonsterBreakablePartEntity.hurt`).
- Actual attack damage is dealt by walking these part hitboxes, not `Mob.doHurtTarget`:
  `HitboxMeeleeAttackGoal` (the `Goal`-system version) triggers a GeckoLib animation via
  `triggerAnim`, waits `tickForAttackChecksToBegin` ticks, then each tick during the active window
  inflates the *attacking* monster's named part hitbox (e.g. the claw) and checks overlap against
  nearby `LivingEntity`s, hurting them directly with a custom `DamageSource` (`ModDamageTypes.RAW`,
  bypasses armor/resistances, aka "raw damage"). `AnimatableHitboxMeleeAttack` on the `brain` branch
  is the same idea reimplemented as an SBL `DelayedBehaviour`. Player damage to monsters instead goes
  through each part's own `MonsterBreakablePartEntity.hurt()` (players hit whichever part hitbox their
  weapon actually intersects), which is how part-breaking/tail-cutting works.
- **Great Izuchi's attacks are on the Brain system now, see below** for how the hitbox-check design
  and the growth-scale sync question actually got resolved.

### Great Izuchi's Brain migration (done, 2026-08-23)

Great Izuchi's combat runs on SmartBrainLib now, rewritten from scratch (not from `origin/brain`, see
above). It's confirmed working live: claw, tailslam, and tailswipe all land real hits and can kill a
target. Getting there took several wrong turns worth knowing about before touching this code again:

- **`HitboxAnimationAttack`** (`entities/ai/brain/HitboxAnimationAttack.java`) replaced the old
  hand-picked hit-check tick window entirely. It checks the bone-synced part hitbox for real overlap
  every tick for the whole swing, hitting each target at most once. No animation-specific frame
  numbers to tune by hand; the hitbox is wherever the animation actually put it.
- A real, confirmed bug (not the growth-scale theory below) was a countdown seeded wrong in
  `start()`, which meant the hit-check code never ran at all regardless of hitbox position. If a fix
  has zero effect, check whether the code is even executing before tuning it further.
- Vanilla `Behavior`'s default duration is hardcoded to 60 ticks and SmartBrainLib's
  `.runFor(...)` doesn't override it in this version; `HitboxAnimationAttack` overrides `timedOut()`
  directly with its own tick counter instead.
- The growth-scale hitbox correction described in an earlier version of this doc (re-applying
  `getMonsterScale()` to the synced part position in `MonsterBreakablePartEntity`) was tried and
  reverted: it pulled every part hitbox toward the entity's center on any monster below max scale
  roll (which is most of them, since the roll is always <=1.0), confirmed by watching it live.
  `MonsterBreakablePartEntity` now trusts MHLib's raw sync as-is.
- `clawHitbox` and `tailEndHitbox` in `great_izuchi.geo.json` had real rigging bugs (wrong bone
  parent, wrong pivot) that looked like code bugs until the bone data was actually dumped and
  checked. Any monster's hitbox that "won't reach" or "sits in the wrong spot" no matter what the
  Java-side tuning does is worth checking at the model level first.

Full write-up of every bug and fix lives in the `great-izuchi-brain-migration` project memory.

### Dependency versions (bumped 2026-08-22, MC stays 1.20.1)

`gradle.properties` versions were audited and updated to the latest available for MC 1.20.1 (verified
with a clean `./gradlew build`, see JDK note above):

- **Forge** `47.3.22` → `47.4.23` (latest for 1.20.1).
- **GeckoLib** `4.7` → `4.8.4` (latest).
- **JEI** `15.2.0.27` → `15.20.0.116` (latest).
- **Jade** (CurseMaven file id) `4711195` → `6855440` (11.13.2, latest).
- **mixin-booster** `0.1.1` → `0.1.3` (latest).
- **SmartBrainLib** `1.15`: already latest for `SmartBrainLib-forge-1.20.1` on Cloudsmith (SBL's
  1.20.1 line stops there; newer SBL releases target 1.20.4/1.21+). Unchanged.
- **MultiHitBoxLib** `1.8.1`: already the latest MC1.20.1 release. Unchanged, but **its repo
  location moved**, see below.
- The `brain` branch's `gradle.properties` still points at older versions across the board, another
  reason not to merge it wholesale; re-check/re-bump versions after rebasing it onto `master`.

**MultiHitBoxLib's GitHub-releases host disappeared.** `build.gradle` originally fetched it via a
custom Ivy pattern hitting `github.com/dertoaster98/multihitboxlib/releases/...`, but that repo is
gone (the project, per its Modrinth listing "MultiHitboxLib (DISCONTINUED)", has been forked to
`github.com/ZigyTheBird/MultiHitBoxLib`, which has no releases published). A clean build with no
pre-warmed Gradle cache would fail to resolve it at all, independent of any version bump. Fixed by
adding a Modrinth maven repo (`https://api.modrinth.com/maven`) and switching the dependency
coordinate to `maven.modrinth:multihitboxlib:${multihitbox_version}` (Modrinth's maven uses its own
project slug/id as the artifact id and its own plain version string, e.g. `1.8.1`, not the
`<mc_version>-<lib_version>` string used in the old Ivy pattern). If this dependency needs bumping
again, get the version number from Modrinth (`api.modrinth.com/v2/project/multihitboxlib/version`),
not from GitHub.

No decompiled/mapped vanilla Minecraft source is cached locally yet (`~/.gradle/caches/forge_gradle`
has no `joined`/`client`-sources jar). Opening the project in an IDE with the Forge/ForgeGradle and
"Minecraft Development" plugins (or running the IDE-sync Gradle task) will trigger the decompile +
Parchment-mapping step and make vanilla MC classes (e.g. `Brain`, `Mob`, `LivingEntity`) readable
with real names, which is useful for Brain-system work since SmartBrainLib wraps vanilla `Brain`
fairly thinly.

### Registration

All Forge `DeferredRegister`s live under `registration/` (`ModEntities`, `ModItems`, `ModBlocks`,
`ModParticle`, `ModDamageTypes`, `ModHitboxTypes`, `ModTabs`, `ModRenderTypes`, plus feature/placed
feature registries). `ModEntities.registerEntity(...)` is the common helper for defining an
`EntityType` + spawn egg together; entity attribute suppliers are wired centrally in
`RegistrationHelper.onAttributeCreate`, not on each entity class's own registration line; when
adding a new monster, both `ModEntities` and `RegistrationHelper` need updating.

Mod-wide constants (mod id, entity/item name strings) are in `utils/MHNWReferences`. Registration
and setup are split across `MHNW` (main `@Mod` class, wires event bus + registries),
`setup/CommonSetup`, `setup/ClientSetup` (`DistExecutor`-gated), and `setup/ModConfig`
(client/server TOML configs). `ModelLayerLocation`-shaped helpers live in `ClientSetup` specifically,
not the shared `RegistrationHelper`; that type doesn't exist on a dedicated server.

### Client rendering

Per-entity GeckoLib model + renderer pairs live in `client/models/entities/` and
`client/renderers/entities/`, following the vanilla `EntityModel`/`EntityRenderer` naming pattern
(`RathalosModel`/`RathalosRenderer`, etc.), with `NewWorldGrowingEntityModel`/
`NewWorldGrowingEntityRenderer` and `NewWorldMonsterEntityModel` as shared bases (renderer has a
`scaleModelForRender` hook for growth scaling). GeckoLib animation/geometry assets are under
`src/main/resources/assets/mhnw/{animations,geo}/`, textures under `.../textures/`. Custom particles
(ice, poison, sleep, thunder) each have a paired `*Particle`/`*ParticleType` class under
`client/particles/`.

### Data

Static datapack-style data (loot tables, damage types, recipes, hitbox profiles) lives under
`src/main/resources/data/mhnw/`. Anything reproducible by the data generators instead lives in
`datagen/` (currently loot table providers) and is emitted to `src/generated/resources/` by
`runData`. Don't hand-edit generated files under `src/generated/`; change the generator and rerun.

MultiHitBoxLib hitbox profiles (`data/mhnw/multihitboxlib/hitbox_profiles/*.json`) define the
breakable-part layout per monster and must stay in sync with the `PART` handling in each entity's
Java code.
