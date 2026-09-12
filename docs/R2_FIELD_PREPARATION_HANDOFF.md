# R2 - Field preparation

**Status: ready to execute. Scope: the complete R2 increment after R1.**
Baseline: `master` at `78bd06618849116f786bdc64cd8932b124531301`, the merge of PR #6 on
2026-09-12. PR #6's source head was `a6da65a8208217e3c6fe34864404a898f1cd83a4`; the trees are
identical. Consumers: Claude Code or Codex.

The maintainer explicitly authorizes this packet's implementation branch to be pushed and one pull
request to be opened against `master`. That authorization covers this packet only. Do not merge the
PR, publish a release, force-push, or alter unrelated branches.

## 1. Outcome and scope

Turn three already-present resources into field-preparation choices before the next large-monster
packet:

```text
Aptonoth carve -> raw meat -> fixed-duration portable BBQ -> cooked meat
wild Flashbug -> bottle -> one crafted flash bomb -> one bounded thrown flash
wild toad -> matching water bucket -> same variant on release -> hit once -> existing effect
```

This packet owns:

1. One small, fixed-duration portable BBQ interaction using R1 raw/cooked meat.
2. A survival-accessible Flashbug capture, one-to-one flash-bomb recipe and throwable flash effect.
3. Vanilla-style capture/release for all four existing toad variants using the preserved bucket
   item IDs and icons.
4. Correct player attribution when a provoked deployed Blastoad damages a carvable monster.
5. Focused GameTests, packaged data/client resources, current documentation and one reviewable PR.

The three features belong in one R2 PR, but keep them as small reviewable commits or equivalent
slices. They share registries, creative tabs and integration evidence; they do not justify a generic
consumable, ailment, ability, cooldown or deployment framework.

**Not included:** antidotes or a new poison counter, player input locking, a custom ailment engine,
a BBQ block/GUI/timing minigame, rare-meat tiers, fuel simulation, slinger/ammunition, stamina,
custom weapons, part breaking, new ores/stations, dung bombs, Bug/Fulgurbug collection, nests/rally,
Rathian/Rathalos progression, effect-icon replacement, biome changes, dependency changes or a
public release.

Preserve the R1 hunting/carving/armor loop, R1a habitat and spawn gate, R0b server-timed
presentation, measured attacks and hurtboxes, every registered species, and all existing developer
tools. The first-release human gates remain honest open gates until a person performs them; R2 can
be implemented without claiming those observations happened.

## 2. Preflight and current facts

Fetch `origin`, inspect HEAD and dirty state, and create or reuse `r2/field-preparation` from the
accepted `origin/master`. The checkout may still be the merged `r1/first-hunting-loop` source
branch. Its tree is accepted, but do not stack another contribution onto a merged branch. Preserve
newer work and this supplied handoff; do not reset, overwrite or silently stash user changes.

Read fully before editing:

- `CLAUDE.md` and the `AGENTS.md` pointer.
- This packet and the R2/Q17/Q18/Q28/Q31 sections of `docs/ROADMAP.md`.
- The current R1/endemic sections of `docs/TEST_PLAN.md` and relevant `docs/DEFERRED.md` entries.
- `Toad`, `ToadFuseGoal`, `Flashbug`, `FlashbugFlashGoal`, `EndemicAreaEffectGoal`, `CarveState`,
  `ModEntities`, `ModItems`, `MHNW`, `MHNWClient` and their nearby `MHNWGameTests` coverage.
- The pinned 1.21.1 sources for `Bucketable`, `MobBucketItem`, `BucketItem`,
  `ThrowableItemProjectile`, `Snowball`, item remainders and interaction results. Do not port the
  1.20.1 signatures by memory.
- The preserved 1.20.1 `ToadBucket`/`ToadEntity` only as behavioral evidence, not copy-ready code.

Current evidence to preserve:

- Minecraft 1.21.1, NeoForge 21.1.248, GeckoLib 4.9.2, TerraBlender 4.1.0.8, Java 21,
  ModDevGradle 2.0.146 and Gradle 9.2.1 are deliberate pins.
- PR #6 reports a clean build, dedicated-server smoke, and **123/123 GameTests eight consecutive
  times** after its review fixes. Run a fresh baseline; a historical report is not a run.
- Toad has one entity type and four saved/synced variants: `POISON`, `SLEEP`, `PARALYSIS`, `BLAST`.
  A hit starts a 40-tick fuse. The three status variants apply their existing five-second effects;
  BLAST makes a small terrain-safe explosion. Every variant then discards the toad exactly once.
- Flashbug has one hit-triggered 11-tick telegraph. It flashes non-player living entities in a
  five-block radius only when they have line of sight and face the source, then discards itself.
  Its current Blindness lasts 40 ticks. Mere proximity does not trigger either endemic species.
- Existing natural-spawn behavior and those one-release rules are intentional. Capture is an
  alternative interaction, not permission to make endemic life auto-fire, recover or generate
  repeatable clouds.
- `CarveState` credits actual accepted health loss from a player, player projectile or resolvable
  owned attacker. A deployed Blastoad must enter that same path as its provoking player, not as a
  nearby-player guess.
- R1 already owns `mhnw:raw_meat` and `mhnw:cooked_meat`; furnace, smoker and campfire recipes must
  remain. The preserved `rare_monster_meat.png` visibly reads as meat on a skewer and is available
  for the BBQ input item without adding an intermediate food tier.
- Four production-ready bucket icons and their legacy IDs already exist under
  `assets/mhnw/textures/item`: `nitrotoad_bucket`, `poisontoad_bucket`, `paratoad_bucket` and
  `sleeptoad_bucket`. There is no accepted flash-bomb or bottled-Flashbug icon.
- `ModItems` and `ModEntities` intentionally have separate item deferred registers. Add the new
  registrations at the smallest dependency-safe surface; do not move existing spawn eggs or IDs
  merely to make the class layout prettier.

## 3. Locked R2 product contract

### Portable BBQ

Add one non-food item:

- ID: `mhnw:bbq_spit`.
- Display name: **Raw BBQ Spit**.
- Model: ordinary generated item using the preserved
  `mhnw:item/rare_monster_meat` sprite. Do not rename or duplicate the PNG.
- Recipe: one `mhnw:raw_meat` plus one vanilla stick, shapeless, yields one spit.
- Maximum stack size: one. This keeps completion transactional without an inventory service.

Using the spit is one fixed action, not a timing game:

- Right-click and hold for **80 ticks** (four seconds).
- Use a vanilla held-use pose that keeps the item visible; `BLOCK` is the default if the pinned API
  offers no closer built-in pose.
- Emit restrained vanilla fire/smoke and campfire sound cues during or at completion. They are
  presentation only; the server's completed use duration is authority.
- Completing on the server replaces the one spit with exactly one existing
  `mhnw:cooked_meat`, then applies a **20-tick item cooldown**.
- Releasing early, changing item, dying or otherwise cancelling consumes nothing and grants
  nothing. Completion cannot run twice.
- A one-item held stack means the result can replace its input slot; do not add a queue, ground
  overflow policy or full-inventory subsystem.
- Creative mode may retain the input under vanilla infinite-material conventions, but must not run
  duplicate completion paths.

This is intentionally portable field cooking with a clear time cost. It does not replace the three
R1 cooking recipes, produce `rare_meat`, inspect a timing window, require fuel, place a block or
open a screen.

### Flashbug capture and flash bomb

Add two items and one small projectile entity:

| ID | Contract |
|---|---|
| `mhnw:bottled_flashbug` | Maximum stack 16; crafting remainder is one glass bottle |
| `mhnw:flash_bomb` | Maximum stack 16; throwable consumable |
| `mhnw:flash_bomb` entity type | Small `MISC` thrown-item projectile rendered with its item stack |

The item and entity registries are distinct, so the repeated path is intentional. Use a vanilla
experience-bottle model/texture for the bottled bug and a vanilla firework-star model/texture for
the bomb as explicit temporary visuals. Do not repurpose `fulgurbug.png`; it is preserved for its
own later material identity. No new bitmap art is required in this code packet, and the temporary
icons remain a named client observation.

Capture contract:

- Right-click a live Flashbug with a vanilla glass bottle.
- The server consumes exactly one bottle, removes exactly that Flashbug without firing its flash,
  and returns exactly one `bottled_flashbug`, using vanilla filled-container inventory/creative
  behavior rather than hand-written slot arithmetic.
- Capture takes precedence only for the glass-bottle interaction. Hitting the entity still follows
  the existing telegraph/effect/discard path; empty-hand proximity still does nothing.
- A full inventory may use vanilla's visible filled-container fallback, but may not silently lose
  the capture or duplicate the bottle/result.

Craft one `flash_bomb` shapeless from one `bottled_flashbug` plus one paper. The emptied glass
bottle returns exactly once through normal crafting-remainder semantics. One caught bug therefore
funds one bomb; do not add powder, casing, ammunition or bulk-yield systems.

Throw contract:

- Right-click launches one projectile with snowball-like velocity/inaccuracy, consumes one bomb in
  survival and starts a **10-tick cooldown**. Play one ordinary throw sound.
- First block or entity impact releases exactly once, broadcasts one visible vanilla flash/poof
  burst and discards the projectile. It deals no direct or terrain damage and cannot be recovered.
- Radius is **5 blocks**. Affected targets must be alive, non-player living entities, have an
  unobstructed view of the flash point, and face it using the current **0.65 horizontal dot-product
  threshold**. Offset a block-hit origin just outside the impacted face if needed so the block does
  not occlude its own flash.
- Apply Blindness 0 and Movement Slowdown 2 for **40 ticks**. The slowdown gives the carried item a
  real repositioning use; do not clear targets, disable AI, cancel committed attack timelines or
  manipulate controls. Players remain entirely unaffected.
- Refactor the wild Flashbug and projectile through one small shared flash helper now that there
  are two real callers. The wild Flashbug gains the same bounded slowdown while retaining its
  existing radius, facing, line-of-sight, hit trigger, 11-tick telegraph and one-release discard.
  Do not generalize the helper into an effect registry.

Looking away, putting solid cover between target and flash, or leaving the radius is accessible
counterplay. The projectile's particles are never effect authority.

### Bucketed toads

Expose the four preserved bucket items with this exact mapping:

| Toad variant | Filled item ID | Existing icon |
|---|---|---|
| `POISON` | `mhnw:poisontoad_bucket` | `poisontoad_bucket.png` |
| `SLEEP` | `mhnw:sleeptoad_bucket` | `sleeptoad_bucket.png` |
| `PARALYSIS` | `mhnw:paratoad_bucket` | `paratoad_bucket.png` |
| `BLAST` | `mhnw:nitrotoad_bucket` | `nitrotoad_bucket.png` |

Use vanilla bucket semantics:

- A live toad plus a vanilla **water bucket** produces exactly one matching filled bucket and
  removes exactly that entity. Other items keep their existing behavior.
- Store the normal bucketable entity data plus the variant. Custom name, health and the variant
  survive capture/release. A filled item supplied without custom bucket data must still release the
  variant named by its item ID.
- Releasing places exactly one live `mhnw:toad`, sets its matching variant and returns exactly one
  empty bucket in survival. It sets/persists `FromBucket` and uses vanilla custom-persistence rules
  so a deployed toad does not immediately distance-despawn.
- `finalizeSpawn` must not replace a bucket-spawned variant with a random one. Save/reload retains
  `FromBucket` and the variant.
- Release does **not** ignite the toad. It must be deliberately hit afterward, preserving the
  current 40-tick warning and one-release discard. Recapturing it before provocation is allowed and
  returns the same variant.
- Repeating capture/release cannot multiply toads, water, empty buckets or filled buckets. Use the
  current 1.21.1 `Bucketable`/bucket-item data-component path; do not revive raw 1.20.1 stack NBT or
  copy its client-only entity code.

The four existing effects and durations remain unchanged. Their counterplay remains the readable
fuse, leaving the three-block radius, cover where applicable, and vanilla milk for removable
statuses. No antidote or paralysis/sleep control-lock semantics are introduced here.

### Blastoad attribution

When a player, the player's projectile, or a resolvable player-owned attacker provokes a toad,
retain that player as the transient provoking source for the current fuse. If the variant is BLAST,
create the existing terrain-safe explosion with that player as its causing entity while the source
is still resolvable. This makes positive explosion damage reach `CarveState` through the same
accepted player-damage contract as a direct hit.

Do not award participation merely for releasing a bucket, standing nearby, being hit by the same
blast, or provoking a non-damaging status toad. Do not guess the nearest player. Clear the transient
source when the fuse releases/stops or the entity is captured; it is not a permanent owner/tame
relationship. If API shape makes a tiny package-local UUID resolver useful, reuse the existing
`CarveState` attribution rule instead of creating a second incompatible rule.

## 4. Small owned implementation surfaces

Prefer the smallest version-correct form of these roles; exact class names may vary:

- `BarbecueSpitItem`: one fixed held-use action and one transactional result.
- `FlashBombItem` and `FlashBombProjectile`: launch, impact, effect and discard.
- `FlashEffect`: package-local radius/LOS/facing/effect helper shared by two actual callers.
- `ToadBucketItem` only if the vanilla `MobBucketItem` cannot express the fixed item-ID variant
  safely. Copy only the tiny 1.21.1 spawn seam required; do not fork general bucket behavior.
- `Toad`: `Bucketable` state/data plus transient provoker attribution.
- Existing registries, creative-tab event and `MHNWClient` renderer registration.

Do not add capabilities, attachments, networking packets, menus, keybinds, services, abstract
consumable bases or a universal owner/effect model. Vanilla item use, projectiles, data components,
bucket transactions and effects already own those jobs.

Suggested production-resource changes are limited to item models, language entries and two
shapeless recipes. The four bucket PNGs and BBQ sprite already exist. Register the preparation
items in sensible vanilla tabs: BBQ/flash consumables in food/tools/combat as appropriate, and
filled toad buckets alongside functional tools. Do not reorganize unrelated tab entries.

## 5. Implementation sequence

### A. Establish the accepted baseline

1. Fetch and verify `origin/master` is at or descends from the PR #6 merge.
2. Preserve local files and create `r2/field-preparation` from `origin/master`.
3. Run the clean baseline build and full GameTests once; record the real result.
4. Inspect the pinned vanilla sources and the legacy bucket evidence before choosing signatures.

### B. Add BBQ as an independent vertical slice

Register/model/localize the spit, add its one recipe, implement the 80-tick completion, and prove
early cancellation plus exactly-one conversion before touching endemic entities.

### C. Add Flashbug capture and throwable use

Implement filled-container capture, recipe/remainder, projectile/renderer, then extract the small
shared flash helper. Preserve the wild trigger and run all existing Flashbug tests immediately.

### D. Add toad buckets and attribution

Implement `Bucketable`, one exact variant mapping, then all four items. Prove command-created bare
buckets, round-trip persistence and no auto-fuse before changing BLAST attribution. Make the
attribution change last and cover it directly against a carvable parent.

### E. Integrate and prepare the PR

Add focused tests near the existing endemic/R1 blocks, run regressions, perform bounded client and
dedicated-server checks, update documentation, inspect the built jar, review the final diff, then
commit/push/open one PR.

## 6. Required automated evidence

Keep every existing test passing and add focused coverage for at least these behaviors:

| Gate | Required proof |
|---|---|
| **R2-01 Registries/data** | Three new preparation item IDs, four bucket IDs and the projectile entity resolve; models/lang/recipes are packaged; the two recipes match the exact inputs/results |
| **R2-02 BBQ transaction** | Use cannot complete before 80 ticks; cancellation changes nothing; one completed use produces one cooked meat, consumes one survival spit, starts one cooldown and cannot double-complete |
| **R2-03 Flashbug capture** | One glass bottle plus one live Flashbug yields one bottled item and no flash; stack/full-inventory and creative behavior neither lose nor duplicate contents; ordinary hurt still uses the old release path |
| **R2-04 Flash recipe/container** | One bottled bug plus paper yields one bomb and one empty bottle exactly once; no bulk or recursive remainder duplication |
| **R2-05 Flash impact** | First impact applies the two 40-tick effects only to eligible facing non-player targets in radius/LOS, misses player/look-away/covered/out-of-range targets, changes no health/blocks and discards once |
| **R2-06 Wild regression** | Wild Flashbug retains hit-only 11-tick telegraph and one discard, now sharing the same bounded flash effects; proximity still does nothing |
| **R2-07 Toad capture mapping** | Each of four variants plus one water bucket produces its exact filled ID, preserves name/health/variant and removes one toad with no fuse/effect |
| **R2-08 Toad release/round trip** | Each filled ID, including a bare command-created stack, returns one empty bucket and one correct non-fusing persistent toad; save/load and recapture preserve variant/FromBucket without multiplication |
| **R2-09 Deployed effects** | A released toad still needs a hit, retains the existing 40-tick fuse/effect/duration/discard for all variants, and cannot release twice |
| **R2-10 Attribution** | A player-provoked Blastoad that causes positive damage grants only that player carve participation; release/proximity/non-damaging variants and an unrelated nearby player do not |
| **R2-11 Regression** | All pre-existing 123 tests pass; no R0/R1 combat, multipart, corpse, economy, armor, habitat, spawn, presentation, roster or other endemic regression |

Tests must exercise real public/scheduled behavior rather than mutate private counters to fabricate
an end state. It is acceptable to call a standard item completion/recipe seam where simulating a
held client input is not a meaningful server test, but still prove server authority and exactly-once
transactions. Do not count `runGameTestServer`'s expected success sentinel as a failed test.

Validation before push:

```powershell
.\gradlew.bat --no-daemon clean build
.\gradlew.bat --no-daemon runGameTestServer
.\gradlew.bat --no-daemon build runGameTestServer
git diff --check
```

Also inspect the built jar for every new class/model/lang/recipe and the four existing bucket icons.
Repeat any scheduling-sensitive new test enough times to expose a race; if a full-suite flake
appears, investigate it rather than publishing a lucky run. Start a real dedicated server from the
built artifact far enough to prove common code never loads the projectile renderer or other client
classes.

## 7. Human and real-process acceptance

When a client is available, perform and record:

1. BBQ spit has a readable inventory/held model; a cancelled hold does nothing; completion cues and
   the cooked result are clear without resembling an eating action.
2. Bottled Flashbug and flash bomb temporary vanilla icons are distinct/readable, and the thrown
   bomb renders throughout flight and flashes once at impact.
3. Capture a naturally spawned or egg-spawned Flashbug with a real bottle, craft the bomb, throw it
   at hostile mobs, and confirm looking/cover behavior feels understandable.
4. Capture and release every toad variant. The filled icon/name matches the creature, the empty
   bucket returns once, the released texture is unchanged, and it stays idle until hit.
5. In survival, deploy a Blastoad near a carvable monster, retreat during the warning, and confirm
   its damage counts for that player's later carve eligibility.
6. With two clients, verify the projectile, flash/effects, toad variant/fuse/removal and inventory
   transactions agree for both observers without duplicate entities/items.
7. Save/restart/rejoin with filled buckets and released toads; verify variants, names and counts on
   the real disk.

Unavailable human checks remain explicitly pending. Do not substitute a GameTest for appearance,
feel, two-client observation or a real disk restart, and do not wait indefinitely for access.
Carry forward the still-open R0/R1 natural-population, village, full-loop, armor-appearance,
combat-feel, two-client and restart gates separately; do not claim R2 closed them unless actually
observed during this packet.

## 8. Documentation and PR delivery

Update only facts made true by the implementation:

- `CLAUDE.md`: concise R2 architecture/invariants and current test count; keep `AGENTS.md` a pointer.
- `docs/TEST_PLAN.md`: dated R2 automated results, repeats, jar/server/client evidence and remaining
  human gates.
- `docs/ROADMAP.md`: mark the R2 increment implemented without marking a public release or unrelated
  future work complete.
- `docs/DEFERRED.md`: retain antidote/R4, fuller ailments, final custom icons or unavailable real-
  process evidence with precise reasons.
- `README.md`: only the short player-visible feature summary if the existing format warrants it.

Use focused commits ending with the exact Claude trailer required by `CLAUDE.md`:

```text
Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>
```

Push `r2/field-preparation` normally, never force-push, and open one PR against `master`. Its body
must include:

- the baseline and packet scope;
- exact BBQ, capture/crafting, flash and bucket behavior;
- why the existing rare-meat and bucket assets were selected and why flash visuals are temporary
  vanilla assets;
- exact commands, final test count/repeats, jar and real-process results;
- any local override of this handoff and why;
- every unavailable client/two-client/restart/release observation as pending;
- `🤖 Generated with [Claude Code](https://claude.com/claude-code)`.

Do not merge the PR or publish a release. Final handover must give starting revision, commits,
implemented contracts, validation evidence, actual final count, human evidence, local overrides,
pending gates and the PR URL.

## 9. Copyable kickoff

```text
Implement docs/R2_FIELD_PREPARATION_HANDOFF.md as one reviewable R2 PR. Fetch origin, preserve local
work, branch r2/field-preparation from the accepted PR #6 merge on origin/master, run a fresh
baseline, then deliver the fixed 80-tick BBQ spit, bottled-Flashbug -> one flash-bomb progression,
four exact variant-preserving toad buckets, and player-correct Blastoad carve attribution. Preserve
all R0/R1 behavior and the existing hit-triggered one-release endemic contract. Use pinned 1.21.1
APIs, focused GameTests, full regression/jar/server checks and honest human gates. Push normally and
open one PR only; do not merge or release.
```
