# R1 - Finish the first hunting loop

**Status: ready to execute. Scope: the remaining R1 work after R1a and R0b.**
Baseline: `master` at `516884ca091cce948724fc1cdb6128ab659d7216`, the merge of PR #5 on
2026-09-12. PR #5's source head was `bdb5571827513777216d7170eb68e08bcb1c632e`; the trees are
identical. Consumers: Claude Code or Codex.

The maintainer explicitly authorizes this packet's implementation branch to be pushed and one pull
request to be opened against `master`. That authorization covers this packet only. Do not merge the
PR, publish a release, force-push, or alter unrelated branches.

## 1. Outcome and scope

Complete the first survival loop that `docs/ROADMAP.md` defines:

```text
fresh world -> find Verdant Hunting Grounds -> meet the existing wildlife ->
hunt Great Izuchi and its escorts -> carve personal rewards -> cook meat ->
craft and wear bone armor -> hunt again with the modest full-set benefit
```

R1a already owns the habitat and automatic-spawn wiring. R0b already owns correctly aged finite
animation presentation. This packet owns everything still required for R1:

1. R1b's persistent, personal corpse-carving contract for Great Izuchi, small Izuchi and Aptonoth.
2. The small material/food economy needed by those carves.
3. Craftable and wearable bone armor with iron-equivalent protection and the agreed full-set trait.
4. Ground-based small-Izuchi circling and occasional dart-in harassment instead of constant melee.
5. End-to-end integration, regression evidence, current documentation and a reviewable PR.

This is intentionally one larger coherent PR because the maintainer asked for the rest of R1
together. Keep each concern in a small commit or otherwise reviewable slice; do not turn that into a
generic loot, skill, AI or corpse framework.

**Not included:** R2 prepared-meat/BBQ mechanics, flash bombs, bucketed toads, any custom weapon,
part breaking, new ores or stations, nests/retreat/rally, Rathian/Rathalos/Lagiacrus progression,
animation retargeting, new small-Izuchi attack/death art, broader player combat, biome redesign,
version/dependency changes, or a public release.

Preserve every registered species, current endemic effects, measured attack paths/hurtboxes,
TerraBlender placement, R0b presentation clocks and developer tools. Rathian, Rathalos and
Lagiacrus remain summonable development content but are not made carvable or survival encounters by
this packet.

## 2. Preflight and current facts

Fetch `origin`, inspect HEAD and dirty state, and branch from the accepted PR #5 result. The current
checkout may still be `r0b/client-lifecycle`; its content is accepted, but the new contribution
branch should start at `origin/master` so ancestry is unambiguous. Suggested branch:
`r1/first-hunting-loop`. Preserve newer work and any supplied handoff file; do not reset, overwrite,
or silently stash user changes.

Read fully before editing:

- `CLAUDE.md` and the `AGENTS.md` pointer.
- This packet and the R1/R1b/decision sections of `docs/ROADMAP.md`.
- The current R0b and R1a sections of `docs/TEST_PLAN.md`, plus relevant `docs/DEFERRED.md` entries.
- `GreatIzuchi`, `Izuchi`, `Aptonoth`, `MonsterPart`, their goals, `ModEntities`, `MHNW`,
  `MHNWClient`, and the nearby `MHNWGameTests` helpers.
- The exact GeckoLib 4.9.2 armor APIs from the resolved sources jar before writing a renderer.
- The bone-armor geometry, animation and texture files named below.

Current evidence to preserve:

- Minecraft 1.21.1, NeoForge 21.1.248, GeckoLib 4.9.2, TerraBlender 4.1.0.8, Java 21,
  ModDevGradle 2.0.146 and Gradle 9.2.1 are deliberate pins.
- PR #5 reports 97/97 GameTests, a clean build, a real dedicated-server boot and a passing
  client-side GeckoLib sampler check. Run a fresh baseline once; a historical report is not a run.
- The Verdant Hunting Grounds generates and was located on all three prescribed seeds. A mineshaft
  was observed inside it. Live natural MH populations, a village inside the habitat and human biome
  acceptance are still observation gaps, not known code defects.
- R0b's two-client and real disk restart gates remain open. This packet adds persistence that those
  same real-world checks must now cover.
- There is no production item/economy system yet. `ModEntities.ITEMS` currently contains spawn eggs
  only. Prefer one small `ModItems` registry for new ordinary items without moving the working spawn
  eggs just to make names prettier.
- Great Izuchi and Aptonoth are multipart; parts forward damage to their parent. Small Izuchi is not
  multipart. Eligibility, corpse lifetime and rewards belong to the parent only.
- Normal death stops goals. R0b presentation already gives death priority over stale attack/roar
  fields and clamps held terminal poses. Do not clear or redesign those clocks for carving.

### Bone-armor asset fact that must not be guessed around

Neither geometry below is the held/inventory item model. Held items use ordinary 2D item-model JSON
and the four icon textures (`bone_head.png`, `bone_chestplate.png`, `bone_legging.png`,
`bone_boots.png`). Both geo files are candidates for the armor rendered on an equipped player.

Repository history establishes their roles:

- `assets/mhnw/geo/entity/bone_armor.geo.json` is the original complete worn model. The 2022
  `BoneArmorRenderer` explicitly mapped all eight `Head`, `Body`, `RightArm`, `LeftArm`,
  `RightLeg`, `LeftLeg`, `RightBoot`, `LeftBoot` bones.
- `assets/mhnw/geo/item/armor/bone_armor.geo.json` is a later 1.20.1 migration copy for
  GeckoLib's default item/armor path. It renamed the head/body/arm/leg bones to the newer
  `armor*` convention but **dropped `armorRightBoot` and `armorLeftBoot`**. It is still equipped
  armor geometry, not a held-item model; a stock `GeoArmorRenderer` would render its feet slot
  empty.

Use the complete geometry as the default candidate with a tiny explicit model/renderer mapping to
its real bone names and to `textures/item/armor/bone_armor.png`. Do not copy geometry between paths,
pretend the missing boot bones exist, or allocate a renderer every frame. If live viewing shows the
complete candidate is not the canonical accepted export, stop only the visual-acceptance claim and
record the precise issue; do not replace accepted monster assets or invent armor art.

## 3. Locked R1 product contract

These are working decisions, not questions to reopen.

### Carving and corpse lifetime

- Carvable species: Great Izuchi, small Izuchi and Aptonoth only.
- Eligibility: positive health damage credited to a player, that player's projectile/tool, or an
  owned/tamed attacker with a resolvable player owner. Proximity, targeting, healing and ownerless
  environmental damage grant nothing.
- Interaction: main-hand **shift + right-click**, server-authoritative, one carve per interaction,
  ten real entity-ticking ticks of per-player debounce.
- Allowance: three personal carves per eligible player, per corpse. Two players each receive three;
  one player's actions never consume the other's allowance.
- Corpse: the dead parent remains inert and visible for **12,000 entity-ticking ticks** from death,
  pauses while unloaded or the server is stopped, then removes once. Do not force-load chunks and
  do not use wall-clock time.
- Persistence: participants, per-player used/remaining carves and remaining corpse ticks survive
  normal chunk save/load, stop/start and reconnect. A reconnect never renews eligibility or quota.
- Rewards: carving is the only item-reward path. Keep existing normal XP once at death, but no
  ordinary death-item drops for these three species.
- Inventory: if the entire next reward stack cannot fit, grant nothing and consume neither the
  carve nor its deterministic result. Give a localized make-room message.
- Feedback: localized, bounded messages cover the sneak-use hint, successful carve with remaining
  count, ineligibility, full inventory and exhausted allowance. Avoid repeated chat spam.
- Parts may forward interaction to the same parent if necessary for the long model to be usable,
  but they never own or duplicate participant state, counters, expiry, XP or rewards.

Track eligibility only after an accepted hit actually reduces health. Resolve direct players first,
then the causing player's projectile/owned attacker using the version-correct APIs. Do not infer an
owner from a name, nearest player or last target.

### Deterministic first-release rewards

R1 contains reliable basics and no rare trophy roll. Use the carve index to select one stack; this
makes the full-inventory retry exact without a separate saved RNG/pending-roll subsystem.

| Species | Carve 1 | Carve 2 | Carve 3 |
|---|---|---|---|
| Great Izuchi | 4 `mhnw:monster_hide` | 2 `mhnw:monster_claw` | 4 `minecraft:bone` |
| Small Izuchi | 1 `mhnw:monster_hide` | 1 `mhnw:monster_claw` | 1 `minecraft:bone` |
| Aptonoth | 2 `mhnw:raw_meat` | 2 `mhnw:monster_hide` | 2 `mhnw:raw_meat` |

The next result is therefore derived from the saved carve count. A failed inventory attempt and a
save/reload naturally produce the same result; do not add a random-roll ledger for deterministic
data.

### Materials, food and armor

Register these item IDs unless a newer accepted branch already establishes exact equivalents:

- `mhnw:monster_hide`, rendered with the existing `textures/item/izuchi_hide.png`.
- `mhnw:monster_claw`, rendered with `textures/item/sharp_claw.png`.
- `mhnw:raw_meat`, rendered with `textures/item/raw_monster_meat.png`.
- `mhnw:cooked_meat`, rendered with `textures/item/well_done_monster_meat.png`.
- Preserve the established legacy armor IDs `mhnw:bone_head`, `bone_chestplate`, `bone_legging`,
  `bone_boots`, using the existing four inventory icons and shared worn-armor assets. Fresh-world
  scope means old Forge saves are not promised to migrate, but changing established names buys
  nothing here.

Raw/cooked meat should use vanilla raw/cooked beef as the initial food baseline. Add ordinary
furnace, smoker and campfire cooking recipes using the version-correct 1.21.1 data format. No BBQ
state, quality minigame, special eating animation or new effect belongs here.

Bone armor uses iron-equivalent slot protection, durability and enchantability, zero extra
toughness and zero built-in knockback resistance. Its stable full-set modifier ID is
`mhnw:bone_armor_set_bonus`: while all four exact pieces are equipped, add **+0.1 knockback
resistance** with add-value semantics; remove it immediately when the set becomes incomplete. It
must not stack, persist as a stale modifier, or require a general armor-skill system. Recompute it
through the smallest server-owned hook that is correct on equip, unequip, death and rejoin.

Use these shaped recipe defaults (`H` hide, `C` claw, `B` vanilla bone):

```text
helmet       chestplate    leggings      boots
BHB          B B           HCH           C C
B B          BCB           B B           B B
             HBH           B B
```

The full set costs 7 hide, 4 claws and 13 bones. Two fully carved Great Izuchi provide the custom
materials plus eight bones; ordinary skeleton bones complete the first set. Vanilla crafting is
enough. The smithing table is permitted by the roadmap, not mandatory; do not add a pointless
smithing recipe merely to mention both stations.

Expose the items in appropriate existing vanilla creative tabs and add translations. Do not add a
custom tab, ore, workstation, guidebook, recipe library or optional integration.

### Small-Izuchi harassment

Replace its always-closing `MeleeAttackGoal` with one small ground-navigation goal, suggested name
`IzuchiHarassGoal`. It may retain ordinary `Mob.doHurtTarget` damage because no valid attack clip
exists. The behavior must read as hanging back, circling and occasionally picking at the target:

- **Circle:** prefer reachable points roughly 5-8 blocks from the current target at ordinary speed;
  change side/point periodically rather than orbiting with impossible perfect geometry.
- **Dart:** after a randomized 40-80-tick opportunity window, close at about 1.25x speed for at most
  30 ticks, attempt at most one ordinary melee hit, then leave whether it hit or missed.
- **Retreat:** move back toward the 6-8-block band for roughly 20-40 ticks before circling again.
- At most one nearby Izuchi within about 12 blocks may be in its dart phase at once. A stuck or dead
  darting member must release that slot within the bounded 30-tick attempt.
- Target loss/death, goal stop, peaceful transition, entity death and reload cancel transient
  navigation/phase state. Do not persist a half-finished dart.
- No teleporting, flying/hovering, rally buff, reinforcement spawning or leader-owned pack service.
  Do not change the already-bounded escort creation code.
- The existing locomotion clips remain the presentation. No placeholder attack/death animation and
  no new synchronized attack timeline are required.

These timings are initial tuning values. A small evidence-backed adjustment is allowed and must be
recorded; deleting the pauses or letting all escorts rush together is a product change, not tuning.

## 4. Small owned implementation surfaces

Use equivalent existing owners if current code has moved. Expected additions/edits are bounded to:

| Surface | Responsibility |
|---|---|
| `registry/ModItems.java` (preferred) and `MHNW.java` | New items/material if needed, registration, creative tabs and the one set-bonus hook |
| `item/BoneArmorItem.java` plus minimal client model/renderer | Four armor items sharing one static implementation and the complete existing geometry |
| One small carving state/helper in `entity` | Participant attribution, counters, deterministic next reward, inventory preflight and NBT |
| `GreatIzuchi.java`, `Izuchi.java`, `Aptonoth.java` | Delegate damage/death/tick/interaction/save behavior to that helper; no other species |
| `MonsterPart.java` | Only a narrow parent-interaction forwarder if live usability requires it |
| `entity/IzuchiHarassGoal.java`, `Izuchi.java` | The bounded circle/dart/retreat loop replacing constant melee |
| Version-correct item models, recipes, loot tables/tags and `en_us.json` | Survival data and localized feedback |
| `MHNWGameTests.java` | Focused regressions using current helpers and the `arena` template |
| `README.md`, `CLAUDE.md`, `docs/TEST_PLAN.md`, `docs/DEFERRED.md`, relevant roadmap status | Current player requirement, architecture, evidence and remaining gates |

A small composition helper used by exactly the three carvable species is appropriate because
`Monster` and `Animal` prevent one shared entity base. Do not introduce capabilities, attachments,
a universal corpse entity, a generic loot service, a database, mixins, reflection or another
dependency for three fixed consumers.

## 5. Implementation sequence

### A. Establish the accepted baseline

```powershell
.\gradlew.bat --no-daemon build runGameTestServer
```

Confirm real tests ran. Record the exact baseline count and failures before editing. Do not rerun an
unchanged successful suite after prose-only changes.

### B. Add the minimal economy first

Register items, models, translations, recipes and explicit no-death-item loot behavior for the
three carvable species. Use the correct singular/plural resource directories for Minecraft 1.21.1
after checking the version's vanilla data; do not copy older Forge 1.20.1 layouts by memory.

Prove the registry and recipes resolve on a real server. Missing resource warnings, a recipe that
cannot be discovered/crafted, or an item model pointing at a nonexistent texture are defects, not
manual acceptance gaps.

### C. Implement one authoritative carving state

Implement and prove Great Izuchi as the vertical slice, then attach the same state contract to small
Izuchi and Aptonoth. The parent must remain the one death/XP/reward owner.

Persist participant UUIDs while the entity is still alive as well as after death: a chunk unload or
server restart between an early hit and the kill must not erase participation. Persist the exact
carve counts and remaining corpse ticks. Debounce timestamps are transient; they do not affect the
hard three-carve limit and need no disk contract.

Retain the dead parent by changing only these species' death-removal path. Let `deathTime` continue
to support vanilla/R0b presentation, keep the body inert, and perform normal final removal once at
expiry. Great Izuchi's and Aptonoth's parts stay attached and registered while their parent corpse
exists, then leave NeoForge's part lookup once with it. Do not call `die`, death loot, XP or removal
twice on load or expiry.

Do not make the test suite wait 12,000 ticks. Assert the configured default, then exercise a
near-expiry saved state through a small package-private test seam or a supported NBT round trip.

### D. Render and equip bone armor

Use GeckoLib 4.9.2's actual `GeoItem`/`GeoRenderProvider`/`GeoArmorRenderer` contract from the pinned
source, with client-only model/renderer code kept off a dedicated server. Map all eight bones in the
complete geometry and cache the renderer. Static armor needs no speculative animation framework.

Prove stats and the full-set modifier headlessly. Then run a bounded client check if available:
every slot visible on a normal player, correct limb attachment while walking/crouching, no invisible
boots, gross mirroring, z-fighting or missing texture. Human appearance approval remains explicitly
pending if no client can be observed.

### E. Replace small-Izuchi pressure

Implement the three bounded phases through normal ground navigation and ordinary melee damage.
Exercise it through vanilla goal scheduling rather than only calling the goal methods directly.
Keep tests deterministic by asserting bounded properties and exposing the smallest test seam needed;
do not make correctness depend on a lucky random dart.

### F. Integrate, document and prepare the PR

Run the full source-to-sink path: carve materials, cook meat, craft/equip armor, set bonus on/off,
corpse persistence and expiry, and an Izuchi pack that visibly pauses between attacks. Update current
docs with exact results and leave historical evidence intact.

Carry open R0b/R1a observations honestly. If real clients are available, this is the efficient time
to close them; if not, code completion and a PR are still valid, with precise pending procedures.

## 6. Required automated evidence

Use the existing `MHNWGameTests` class and arena conventions. Prefer normal AI and supported entity
save/load paths. Every asynchronous test must prove its setup was reached before succeeding.

| Gate | Required assertion |
|---|---|
| **R1-01 Registry/economy** | All eight new item IDs resolve; models/recipes/loot data are packaged; raw meat cooks through furnace, smoker and campfire; the three species have no ordinary death-item reward path |
| **R1-02 Attribution** | Accepted health loss from a direct player, player projectile and resolvable owned attacker records only the correct UUID; proximity, ownerless/environmental damage and rejected/duplicate part damage do not |
| **R1-03 Personal quota** | Two distinct players can each claim exactly three rewards; a fourth attempt grants nothing; main-hand+sneak, range, living/dead state and ten-tick debounce are enforced server-side |
| **R1-04 Atomic inventory** | A full or insufficient inventory changes neither inventory nor carve count; retry gives the identical deterministic stack; successful insertion consumes exactly one claim |
| **R1-05 Persistence/expiry** | Participation can survive a live-entity round trip before death; dead save/load preserves UUIDs, exact per-player counts and remaining ticks; unloaded/offline time does not advance the timer; near-expiry removal happens once without renewal |
| **R1-06 Corpse lifecycle** | All three species become inert immediately, deal no further damage and remain for the configured window; Great Izuchi/Aptonoth parts stay parent-owned during the corpse and unregister with final removal; no duplicate XP/death processing/reward owner exists |
| **R1-07 Armor** | Slot defense equals iron, toughness/built-in resistance stay zero, all four exact pieces add one +0.1 modifier, incomplete/mixed sets remove it, re-equip/reload cannot stack it |
| **R1-08 Izuchi harassment** | A target produces circle -> bounded dart -> retreat behavior under normal scheduling; at most one nearby member darts; at most one hit occurs per dart; failed navigation and target loss clear within bounds; no transient dart resumes after reload |
| **R1-09 Regression** | All previous R0a/R0b/R1a tests pass; no measured attack, presentation clock, habitat mapping, natural-spawn guard, endemic effect or roster regression |

Use mock/server players where the 1.21.1 GameTest API supports them. If a specific client-only render
fact cannot be tested headlessly, test the common registration/state contract and leave only the
actual appearance as a named human gate. Do not make a GameTest load `GeoModel`; R0b already proved
that dedicated-server dist cleaning forbids it.

Final code validation:

```powershell
.\gradlew.bat --no-daemon build runGameTestServer
git --no-pager diff --check
```

Also inspect the built jar for the new models, recipes, loot data and translations, and perform one
bounded `runServer` startup to catch client-only armor class loading. Run `runData` only if this
implementation actually adds a data provider. Stop test processes you own by their specific process
IDs; never delete maintainer worlds.

## 7. Human and real-process acceptance

These observations decide release readiness, not whether a sound code PR may be opened:

- [ ] In a fresh world, without commands, find the habitat, naturally encounter the R1 roster, hunt
      Great Izuchi with its escorts, carve, cook, craft/wear the full armor set and repeat.
- [ ] Small Izuchi reads as circling/darting pack pressure, not four constant melee rushers; pauses,
      target loss and terrain behavior feel fair.
- [ ] All four bone pieces render in the correct slots and follow a player through walking,
      crouching and ordinary first/third-person play; no invisible boots or broken texture.
- [ ] Two distinct clients on one dedicated server each receive only their own three carves, agree
      on body/parts/removal and cannot duplicate a grant with simultaneous/both-hand interaction.
- [ ] Real `save-all flush` -> graceful stop -> restart -> rejoin preserves living participation,
      corpse counters and remaining time without duplicate escorts, XP, rewards or resumed combat.
- [ ] R0b late-action/culling/retracking observations remain correct with the retained corpse.
- [ ] R1a carryovers: live natural MH population, disabled-config comparison, a plains village
      inside the habitat and human biome appearance.

If access is missing, update `TEST_PLAN.md` with the exact unchecked procedure. Do not wait in an
agent loop, label an in-memory NBT test a disk restart, use summon eggs as natural-spawn evidence, or
claim that a compiled armor model was visually approved.

## 8. Documentation and PR delivery

Update the smallest current surfaces needed:

- `README.md`: describe the first loop and carving interaction without turning it into a wiki;
  retain the TerraBlender install requirement and new-chunks note.
- `CLAUDE.md`: current item/carving/armor/Izuchi architecture and new GameTest count.
- `docs/TEST_PLAN.md`: dated R1 result, commands, mutation/regression evidence where useful, and
  exact human/real-process status.
- `docs/DEFERRED.md`: close only items actually delivered; retain R2+, rally/nests, art, R0b/R1a
  observations and any new precise limitation.
- `docs/ROADMAP.md`: only a short factual status/link update if needed; do not rewrite its product
  decision history.

After validation, inspect the complete diff for unrelated changes and secrets. Commit using the
agent-appropriate exact attribution in `CLAUDE.md`. Push `r1/first-hunting-loop` normally and open
one PR against `master`. The PR body must include:

- accepted starting revision (`516884c`) and resulting commit(s);
- player-visible implementation summary;
- exact build/GameTest/server/client outcomes and actual test count;
- local overrides to this packet, if any;
- unchecked human/R0b/R1a gates, each named rather than hidden behind “manual testing pending”;
- the repository-required `Generated with` attribution line.

Do not auto-merge the PR. Do not publish artifacts or a release. If GitHub authentication prevents
the authorized push/PR, leave the local branch and commits complete and report the exact command
failure; do not force credentials or substitute another remote.

## 9. Copyable kickoff

```text
Implement docs/R1_FIRST_HUNTING_LOOP_HANDOFF.md on a new
r1/first-hunting-loop branch based on accepted origin/master at 516884c
(PR #5 merged). Inspect HEAD and local changes first; preserve newer work and
the supplied handoff. The SHA is an inspection baseline, never a reset target.

Deliver the rest of R1 in one reviewable PR: personal three-carve corpses for
Great Izuchi, small Izuchi and Aptonoth; persisted participation/counters and
12,000 entity-ticking-tick expiry; deterministic reliable materials; ordinary
meat cooking; complete bone armor with iron-equivalent stats and the +0.1
full-set knockback-resistance trait; and bounded small-Izuchi circle/dart/retreat
behavior. Use the packet's exact defaults without another product interview.

Preserve the R1a habitat, R0b clocks, measured combat/hurtboxes, all creatures,
endemic effects and toolchain pins. No R2, weapons, part breaking, rally/nests,
new attack art, biome redesign or generic framework. Resolve the documented
bone-armor export mismatch deliberately and keep client renderer classes off a
dedicated server.

Run the full build/GameTest workflow, jar/resource checks and a dedicated-server
smoke; run bounded client/manual checks where access exists and record the rest
honestly. Update the current docs. This request explicitly authorizes pushing
this packet's branch and opening one PR against master; follow CLAUDE.md's exact
Claude commit/PR attribution. Do not merge or publish a release.
```
