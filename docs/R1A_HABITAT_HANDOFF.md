# R1a - Verdant Hunting Grounds

**Status: ready to execute after the accepted R0a baseline.**
Scope: one naturally generating habitat and its wildlife, not the whole R1 loop.
Consumers: Claude Code/Opus or Codex/Sol. This handoff can be supplied as a file;
it does not have to be committed or placed at a particular repository path.

Reviewed input: R0a PR #3 at `3fdb7607101eef8db1d5cc3ff9922a1024a3a752`.
Research date: 2026-09-12 UTC. PR #3 was still open when inspected.
Prefer the resulting mainline after it lands; an accepted contribution branch
containing the same work also suffices. A squash merge need not preserve that
SHA as an ancestor. Use actual code, not the historical roadmap claim that
`master` is still the old Forge runtime.

## 1. Deliverable and non-goals

In an ordinary fresh Overworld, players can find **Verdant Hunting Grounds**:
a temperate, lightly wooded meadow/low-hill biome with vanilla oak/birch trees,
grass, caves, ores, plains villages and ordinary mineshafts. Great Izuchi,
its small-Izuchi escort, Aptonoth, toads, flashbugs and bugs spawn there through
normal Minecraft spawning. Their existing behavior remains intact.

**Keep the agreed defaults; do not interview the maintainer again.** Equivalent
API choices, reasonable spawn/placement tuning and better locally evidenced
details may be adopted with a short note. Escalate only a material product
change, unavailable input, destructive action or publication decision.

Not included: carving, loot progression, armor, custom blocks/ores/structures,
nests, rally/retreat, small-Izuchi harassment AI, new attacks/ailments, R0b client
seeking, or a new dimension/world preset. Retain all currently registered
species and eggs, including unfinished encounters not exposed in this biome.

R0b remains a pre-release requirement, not a prerequisite for this feature.
Do not spend this packet fixing the documented delayed roar rearm during a
committed attack; R0a consciously retained that bounded behavior.

## 2. Starting facts and chosen implementation

Current platform: Minecraft 1.21.1, NeoForge 21.1.248, GeckoLib 4.9.2, Java 21
target, ModDevGradle 2.0.146 and Gradle wrapper 9.2.1. Preserve those pins.

At the reviewed PR head:

- `MHNW.onRegisterSpawnPlacements` registers only Great Izuchi.
- `great_izuchi_spawns.json` adds it to `#minecraft:is_forest`, weight 2, group 1.
- `MHNWConfig.NATURAL_SPAWNING` exists, but Great Izuchi's current predicate
  consults it only for `MobSpawnType.NATURAL`.
- Great Izuchi creates 1-4 escorts for `NATURAL` and `SPAWNER`, not for eggs/
  summons. It places them at the leader's Y without terrain/obstruction checks.
- Aptonoth, Toad and Flashbug are `CREATURE`; Bug is `AMBIENT`; Great/small
  Izuchi are `MONSTER`. Keep these categories initially.
- Toad/Bug variants are chosen in `finalizeSpawn` and already persist.
- `runData` and generated-resource wiring exist, but a biome registry/datagen
  implementation does not. One biome does not require a new datagen framework.
- R0a reports 74 passing GameTests and fixes license metadata. Those results
  were reported on the exact PR head, not rerun by this handoff author.

### Add one justified dependency

Use **TerraBlender NeoForge 1.21.1 / 4.1.0.8** for compatible Overworld biome
placement. Registering a biome JSON and adding spawns does not make the biome
generate. This dependency avoids maintaining our own biome-source mixin or
overwriting the world's global noise settings.

Verified published artifact:

```text
Repository: https://maven.minecraftforge.net
Coordinate: com.github.glitchfiend:TerraBlender-neoforge:1.21.1-4.1.0.8
Mod ID: terrablender
Runtime mod version: 4.1.0.8
```

The release is indexed as beta; this is the existing published 1.21.1 build,
not a nightly or a recommendation to move Minecraft forward. Its actual jar
was inspected: Java class version 65/Java 21, implementation version 4.1.0.8,
NeoForge dependency range `[20.4,)`. Forge Maven serves the artifact; Maven
Central did not. Packaging compatibility still needs the normal build/server
smoke in this project.

Add a content-filtered repository for `com.github.glitchfiend` and:

```groovy
implementation "com.github.glitchfiend:TerraBlender-neoforge:${minecraft_version}-${terrablender_version}"
```

Use `terrablender_version=4.1.0.8`. Declare `terrablender` a **required**
dependency on **both sides**, with runtime range `[4.1.0.8,4.2)` initially.
If using template properties, wire them into `generateModMetadata`'s
replacement map. The runtime version does **not** have the `1.21.1-` prefix
that the Maven version has. Do not shade/jar-in-jar the library or change loaders.

**Do not copy TerraBlender's example build wholesale.** Even its 1.21.1 branch
contains example files using older Java 17/build plugins, a no-argument mod
constructor and `new ResourceLocation(...)`. Keep MHNW's current injected
`IEventBus`/`ModContainer` constructor and `ResourceLocation.fromNamespaceAndPath`.

## 3. Small owned surfaces

Use existing naming/layout where a newer checkout already provides an equivalent.
Names below are concrete defaults, not a reason to duplicate an existing helper.

| Surface | Work |
|---|---|
| `build.gradle`, `gradle.properties`, metadata template | The one dependency/repository and required-mod metadata |
| `registry\ModBiomes.java` | Resource key and one wildlife-selector tag key; not a `DeferredRegister<Biome>` |
| `worldgen\HuntingGroundsRegion.java` | One TerraBlender Overworld region |
| `MHNW.java` | Enqueue region registration during common setup; register the six relevant spawn placements |
| `entity\HuntingSpawnRules.java` | Small shared automatic-spawn guard and the appropriate physical predicates |
| `entity\GreatIzuchi.java` | Reuse the guard and bound terrain-safe escort placement; no combat changes |
| `data\mhnw\worldgen\biome\verdant_hunting_grounds.json` | One dynamic-registry biome |
| `data\mhnw\worldgen\placed_feature\trees_hunting_grounds.json` | Sparse placement using the existing vanilla configured tree feature |
| Existing Great Izuchi biome modifier plus one wildlife modifier | Retarget the leader and add the four independent wildlife entries once |
| Biome tags and `assets\mhnw\lang\en_us.json` | Selector, vanilla classification/structure eligibility, displayed name |
| Existing tests and status docs | Registry/spawn/escort evidence and reproducible real-world observations |

Java paths above are under `src\main\java\com\carro1001\mhnw`.
Data/assets are under `src\main\resources`. Author these few JSON files directly;
if a newer branch already has datagen for these registries, use that instead.
Never maintain handwritten and generated copies of the same resource.

## 4. Implementation sequence

### A. Add the biome and prove it participates in the real Overworld

Biome key: **`mhnw:verdant_hunting_grounds`**.
Region ID: **`mhnw:overworld_hunting_grounds`**, `RegionType.OVERWORLD`,
initial region weight **2**. Region weight and mob spawn weights are different.

Use `FMLCommonSetupEvent.enqueueWork` to call `Regions.register` once.
The verified 4.1 API override is:

```java
public void addBiomes(Registry<Biome> registry,
        Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> mapper)
```

Within that region, use `addModifiedVanillaOverworldBiomes` and
`builder.replaceBiome` for **`Biomes.PLAINS` and `Biomes.FOREST`** to the new key.
Keep other parameter mappings. This is a weighted region-local replacement,
not permission to replace every forest/plains biome globally or alter the
Overworld preset. Let the vanilla climate/noise system supply ordinary terrain.
No custom surface-rule registration is needed for normal grass/dirt surfaces.

Base the biome's data on the **1.21.1** vanilla forest export linked in section 8.
Keep normal precipitation, temperature 0.7/downfall 0.8, vanilla ambience,
carvers, ores, and normal vanilla spawn entries. "No new ores" means no MH ore
system, not stripping vanilla mining/caves out of this biome.

Make it more open by copying the version-matched vanilla
`trees_birch_and_oak` placed feature into our namespace, retaining its configured
feature and placement-filter order, but changing its count provider to the
constant **3**. Replace the tree entry at the same position in the forest's
vegetation-step list with our placed-feature ID. Use the translated biome name
`biome.mhnw.verdant_hunting_grounds = Verdant Hunting Grounds`.

Preserve the order of shared vanilla placed features. Reversing shared feature
order between biomes can cause a feature-cycle crash. Do not also inject the
same trees through a feature modifier or overwrite vanilla feature JSON.

**Early milestone:** the mod boots with the dependency, the dynamic biome
registry resolves the key, and a normal new Overworld can locate the biome.
Do this before wiring every creature, so a placement problem stays small.
There is no silent "fall back to forest" path if registry/placement wiring fails.

### B. Add classification and structures without replacing vanilla data

Add the biome to these vanilla tags with additive files (`replace: false`):

- `minecraft:is_overworld`
- `minecraft:is_forest`
- `minecraft:has_structure/village_plains`
- `minecraft:has_structure/mineshaft`

Files belong under `data\minecraft\tags\worldgen\biome`, with the last two in
the `has_structure` subdirectory. These are eligibility additions; do not copy
or replace the entire vanilla structure sets, structure definitions or tags.
Respect the player's generate-structures setting.

Create **`mhnw:spawns_hunting_wildlife`**, initially containing only our biome,
under `data\mhnw\tags\worldgen\biome`. Use that same selector in spawn data and
the automatic-spawn guard. Pack authors can deliberately extend the tag later;
the default must not include vanilla forests.

### C. Wire one source of spawn entries and the existing config

Keep MHNW spawn entries out of the biome JSON; use biome modifiers as the single
owner of them. Retarget the existing `great_izuchi_spawns.json` from the vanilla
forest tag to `#mhnw:spawns_hunting_wildlife`. Add the other four independent
entries in one wildlife modifier using NeoForge's supported spawner list.

| Species | Existing category | Initial weight / group | Special rule |
|---|---|---|---|
| Great Izuchi | MONSTER | 2 / 1-1 | Retain ordinary monster darkness/difficulty rules; creates the escort |
| Small Izuchi | MONSTER | **No independent entry** | Existing 1-4 escort path only |
| Aptonoth | CREATURE | 8 / 2-3 | Appropriate vanilla animal ground/light conditions |
| Toad | CREATURE | 2 / 1-2 | Ground/surface placement, existing variant initialization |
| Flashbug | CREATURE | 2 / 1-2 | Safe surface spawn, then its existing flight control takes over |
| Bug | AMBIENT | 2 / 1-2 | Surface bug, not bat cave/Halloween spawning rules |

Leave Rathian, Rathalos and Lagiacrus registered but without natural entries in
this packet. Keep vanilla mobs and other mods' spawn entries intact.

Register `ON_GROUND` placements with the existing
`MOTION_BLOCKING_NO_LEAVES` heightmap for the six listed entity types, including
small Izuchi for placement validation. Use properly typed predicates: Aptonoth
can use animal rules; a `PathfinderMob` toad/bug is not an `Animal` to cast.

For **automatic sources (`NATURAL` and `CHUNK_GENERATION`)**, the shared guard
must require both the current `naturalSpawning` server config and selector-tag
membership. Read config at spawn time, not static initialization or datagen.
Worldgen-spawned passive creatures must not bypass the off switch.

For new surface wildlife, require appropriate solid spawnable ground, empty
fluid/headroom and a surface position rather than a cave. Use vanilla physical
checks and the heightmap; do not globally alter collision or mob categories.
Flashbugs need not be created floating in unsupported air just because they fly.

Do not apply the habitat/config restriction to eggs, commands, spawners, loaded
entities or existing animals. `naturalSpawning=false` stops new automatic MH
spawns, not biome generation, vanilla wildlife, manual development access or
entities already present. It must not affect saved world biome keys.

Weights are starting values, not promised density. If a naturally tested
population is starved by caps or terrain, adjust our weight/rule narrowly and
record why. Do not raise global mob caps, erase vanilla mobs, add a spawn
scheduler, or call `setPersistenceRequired` on all wildlife.
Bug's `AMBIENT` category competes with bats; if that demonstrably prevents a
usable surface population, moving just Bug to `CREATURE` is an allowed,
documented local override, not a reason to invent a separate spawn engine.

### D. Make naturally created escorts safe on actual terrain

The current fixed-Y escort loop was acceptable in a flat spawn-egg demo, not
proof that a pack can appear safely on hills. This placement correction is
in scope; redesigning pack combat is not.

- Keep the current requested count 1-4 and the existing `NATURAL`/`SPAWNER`
  origin distinction. Egg/summoned leaders still do not create escorts.
- For each requested escort, try at most **eight** nearby candidates in the
  current 2-5 block radius. Resolve local surface Y and test actual Izuchi body
  clearance, solid ground, fluid and other-entity obstruction before adding it.
- Do not load/generate distant chunks to complete the count. Skip an impossible
  member after the bounded attempts; never retry forever or spawn inside stone.
- For a naturally spawned leader, each natural escort location must also be in
  the habitat selector. A leader may stand at the biome edge. Manual spawner
  use keeps its existing non-biome-restricted behavior.
- Keep `finalizeSpawn` and its `MOB_SUMMONED` child reason; this is not a new
  independent population or an excuse to respawn escorts every tick/reload.

In an open valid area expect 1-4 escorts. On obstructed terrain a smaller count,
including zero if nowhere is valid, is preferable to suffocation or unbounded
work. An unexpected entity-factory failure should be reported, not disguised
as normal terrain rejection. Do not change hurtbox geometry to make checks pass.

## 5. Evidence required

Use the existing GameTest setup and a fresh final build/full-suite run. Adapt
fixtures for the intentional new habitat restriction rather than weakening
their assertions. Minecraft 1.21.1 **does** expose
`GameTestHelper.setBiome(ResourceKey<Biome>)`; use it for habitat fixtures.
That does not prove normal terrain generation.

| Gate | Required evidence |
|---|---|
| **H01 Registry and packaging** | Biome, sparse placed feature and selector resolve; built jar contains the intended resources and required TerraBlender metadata; normal client and dedicated-server startup have no missing-key/class/feature-cycle errors |
| **H02 Placement mapping** | Our Overworld region includes the new key for the intended plains/forest climate slots and retains other mappings; no global preset/noise-source replacement |
| **H03 Resolved spawn data** | After modifiers, our biome has exactly one intended MH entry per independent species with correct categories/counts; no independent small Izuchi or unfinished wyverns; standalone vanilla forest has no leftover MH entry |
| **H04 Guard and config** | Correct biome + valid conditions can pass; wrong biome and disabled config reject automatic spawns for all relevant types; include `CHUNK_GENERATION`, not just `NATURAL`. Manual origins remain usable |
| **H05 Physical/escort cases** | Open, sloped, obstructed, liquid and biome-edge fixtures; correct Y/clearance and bounded attempts; 1-4 in an open valid area, no forced suffocation in a blocked one; no escort creation on reload/summon |
| **H06 Existing behavior** | R0a regressions and existing toad/flashbug/variant tests still pass. No changes to measured attacks, effects, roar/death presentation or registered roster |
| **H07 Actual Overworld generation** | Three named scratch worlds, seeds **0**, **20260911**, **8675309**, normal Overworld settings: locate the actual biome within **4,096 horizontal blocks of spawn** on each, record coordinates/distances and inspect its generated terrain |
| **H08 Structures and natural wildlife** | A plains village and a mineshaft demonstrably generate in the custom biome with structures enabled; observe genuinely automatic wildlife and escort spawning, then verify disabled-config behavior in a separate controlled run |

For config tests, avoid changing a shared server config across multiple async
test ticks while other tests run. A synchronous group of predicate assertions
can use `try/finally` restoration in one callback; a longer disabled-population
observation belongs in an isolated server/world run. Do not leave the setting
changed after a failing test.

For natural-spawn proof, `helper.spawn`, an egg, or manually calling
`finalizeSpawn(NATURAL)` is **not** the natural spawn algorithm. Use ordinary
fresh chunks and entity-ticking gameplay with `doMobSpawning` enabled. Great
Izuchi retains darkness/difficulty conditions, so inspect an appropriate
nighttime window; passive wildlife and AMBIENT bugs have different spawn/cap
cadences. Record what was actually seen, not what the table theoretically allows.
An active natural-spawning observation also needs a non-spectator player at a
valid distance; a headless server with no eligible player cannot prove this
part merely by waiting. Start with bounded daytime/nighttime observations of
2,000 game ticks each, recording species counts and positions. If a type never
appears, inspect player distance, light, ground, chunks and caps before tuning.
Do not turn one observed species into a claim that all six were observed.

Use named scratch worlds, not the maintainer's existing survival save. Developer
commands such as `/locate biome mhnw:verdant_hunting_grounds`, vanilla structure
location and entity inspection are useful evidence tools; do not place a biome
or structure with commands and call that natural generation. A structure-tag
membership assertion alone is not proof a village can generate there.

The three-seed distance is a concrete initial target, not a universal guarantee.
If it fails, first tune this region's weight/mapping and rerun the same seeds.
Do not swap seeds until they pass or silently turn a 4,096 target into 20,000.
If a better local target is justified, record the old/new target and why.
No requirement makes every species or both structures appear in every sampled
chunk, or all three seeds contain a nearby village.

A code-capable agent should perform headless generation/registry/spawn evidence
where possible. A missing human visual session is reported separately; it need
not stall subsequent carving development, but must not be called a visual pass.
If an eligible client/player cannot be provided, explicitly retain live natural-
population observation as pending rather than looping on an empty server.
An unfindable biome, crash, duplicate spawn entries or ineffective config is a
code defect to fix, not "human acceptance pending."

## 6. Delivery discipline

At the start, inspect HEAD and the accepted R0a delta once. Reuse newer work;
do not reset to this handoff's SHA or rerun the old rewrite. The local
contributor may store handoffs adjacent under `docs`; do not recreate a
`docs\plans` hierarchy just because an older supplied document used it.

Proceed in small slices: dependency/biome placement -> vegetation/structures ->
spawn/config -> safe escorts -> evidence/docs. Validate the smallest relevant
slice while iterating, then run:

```powershell
.\gradlew.bat --no-daemon build runGameTestServer
git --no-pager diff --check
```

Use the current working JDK/toolchain setup. No global tool installation or
build-plugin migration. Run `runData` only if this implementation actually uses
datagen; do not create providers just to justify that existing task.

Update the relevant existing status docs and installation requirements: players
need the matching NeoForge TerraBlender dependency on both sides, and old terrain
does not become the new biome. Record the accepted R0a baseline, new resources,
actual seed/spawn evidence, local tuning overrides and pending visual gates.
Do not claim the complete hunt/carve/craft loop: R1b and equipment are still next.

Use one main agent. Delegate only concrete independent work that saves context,
not one agent per JSON file. Do not wait in an autonomous loop for a human.
Stop after R1a; no automatic commits, pushes or publication without the user's
current authorization. Never remove user worlds during test cleanup.

## 7. Copyable kickoff

```text
Implement the supplied R1A_HABITAT_HANDOFF.md for MonsterHunter-NewWorld.
Use the accepted post-R0a code: preferably mainline after PR #3 lands, or an
accepted branch containing its equivalent changes. Inspect HEAD/local edits
and preserve newer work; the referenced SHA is not a reset target.

The document may be an attachment or local file, not a checked-in path. Use it
as the task contract. Follow current repository instructions and source, but
do not resurrect historical claims that master still contains the old Forge
runtime. No new product interview is needed; record sensible local overrides.

Deliver only R1a: Verdant Hunting Grounds in the normal Overworld, the pinned
TerraBlender integration, sparse vanilla vegetation/selected structures,
habitat-only automatic wildlife, the existing spawn-disable config for natural
and chunk-generation sources, and bounded terrain-safe escorts.

Preserve all existing monsters, effects, assets, measured combat and developer
access. No carving, equipment, new AI, nests, client seeking or broad refactor.
Do not copy TerraBlender's stale example build/API boilerplate.

Prove actual generation and natural spawning as specified, not just JSON
registration, eggs or a flat GameTest fixture. Use scratch worlds, retain the
same seed/distance targets, and report actual evidence plus any pending visual
gate. Use one main agent; delegate only when useful. Stop after R1a.
Leave changes reviewable; do not commit, push or publish unless asked.
```

## 8. Verified references

- [Reviewed R0a PR #3](https://github.com/Carro1001/MonsterHunter-NewWorld/pull/3),
  head `3fdb760`; reported 74/74 after its own review corrections.
- [Published TerraBlender POM](https://maven.minecraftforge.net/com/github/glitchfiend/TerraBlender-neoforge/1.21.1-4.1.0.8/TerraBlender-neoforge-1.21.1-4.1.0.8.pom).
  Inspected jar SHA-256:
  `6f9b488dbfebe134b848d1a07f462fed711bb1c96b6bd50645eaa8b9569da0b1`.
- [TerraBlender Region API](https://github.com/Glitchfiend/TerraBlender/blob/c90344362b5e813c43f38fed19ea82412c1fffc3/Common/src/main/java/terrablender/api/Region.java)
  and [region-local replacement example](https://github.com/Glitchfiend/TerraBlender/blob/c90344362b5e813c43f38fed19ea82412c1fffc3/Example/NeoForge/src/main/java/terrablender/example/TestRegion2.java).
  API references only; the example build/constructor code is stale.
- [NeoForge 1.21.1 biome modifiers](https://docs.neoforged.net/docs/1.21.1/worldgen/biomemodifier/)
  covers spawn data, required physical restrictions and feature-order hazards.
- [Minecraft 1.21.1 forest export](https://github.com/misode/mcmeta/blob/aacd2a457333b258d044a642f38270c8cddbc628/data/minecraft/worldgen/biome/forest.json)
  and [tree placement export](https://github.com/misode/mcmeta/blob/aacd2a457333b258d044a642f38270c8cddbc628/data/minecraft/worldgen/placed_feature/trees_birch_and_oak.json):
  version-pinned generated vanilla data, not the latest-version schema.
- [GameTestHelper 1.21.1 API](https://mappings.dev/1.21.1/net/minecraft/gametest/framework/GameTestHelper.html)
  lists `setBiome(ResourceKey<Biome>)`; it does not make a flat test world a
  substitute for normal Overworld placement evidence.

This is researched implementation guidance, not a claim that TerraBlender has
already been integrated, booted or visually accepted in this mod.
