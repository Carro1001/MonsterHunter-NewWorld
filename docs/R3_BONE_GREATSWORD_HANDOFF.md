# R3 - The first bone greatsword

**Status: implementation contract ready; art intake is incomplete.**
Baseline: `master` at `0d972d024609745f3bb7d7f4698ad66a8f880bee`, the merge of PR #7 on
2026-09-12. PR #7's source head was `bae614043bf63b898be20b1d74dde8278df685b4`.
The merge also contains direct-master commit `169234b1bf871e262519f41532f57aeeae7aa0c4`, which added
`textures/item/giant_jawblade_model.png`. Consumers: Claude Code or Codex.

The maintainer authorizes this packet's implementation branch to be pushed and one pull request to
be opened against `master` **only after the art gate in section 2 is satisfied**. If it is not, do
not create placeholder art, commit speculative weapon code or open an empty PR. Report the exact
missing inputs and stop. Once satisfied, the authorization covers this packet only: do not merge
the PR, publish a release, force-push or alter unrelated branches.

## 1. Outcome and scope

Add one survival-obtainable bone greatsword with one deliberate secondary attack:

```text
Great Izuchi carves -> one shaped weapon recipe -> slow, hard vanilla sword hits
                                           -> hold right-click 30 ticks
                                           -> one longer-reach committed strike
```

This packet owns:

1. The delivered bone-greatsword presentation in first person, third person and inventory.
2. One `SwordItem`-shaped weapon using ordinary vanilla damage/enchantment/durability hooks.
3. One fixed 30-tick, single-target, block-clipped charged strike with bounded recovery.
4. A recipe using only existing R1 carve materials plus a stick.
5. Focused GameTests, resource/server/client checks, current documentation and one reviewable PR.

This is one weapon, not a weapon system. There is no common moveset API to design before a second
weapon exists.

**Not included:** a second greatsword move or charge tier, combos, sweeping/AOE charge damage,
sheathing, stamina, dodge/roll, player animation libraries, keybinds, packets, screen shake, a
skill/decoration tree, sharpness gauges, weapon upgrades, ores, a forge/workstation, other weapons,
part breaking, monster changes, R4 poison/Rathian work, R8 combat changes or public release.

Preserve R0-R2 combat, multipart, carving, corpse, armor, habitat, spawn, preparation and endemic
contracts. Ordinary vanilla and other mods' weapons remain valid; nothing globally replaces player
attack or movement behavior.

## 2. Art gate and current evidence

### What actually exists on `master`

Direct commit `169234b` added exactly one 64-by-64 PNG:

```text
src/main/resources/assets/mhnw/textures/item/giant_jawblade_model.png
```

Visual and pixel inspection show that it is a sparse UV atlas for a held 3D model, not a readable
inventory sprite. The commit message says **“Greatsword texture (on-hand)”** and its body says
**“inventory 2d item texture still needed.”** Treat those as deliberate asset metadata.

The active repository and every fetched remote branch contain no `BoneBlade.bbmodel`, no exported
greatsword geometry/model JSON and no older weapon implementation. `docs/ROADMAP.md` records a
point-in-time Drive snapshot in which `items/equipment/bone/BoneBlade.bbmodel` existed, but that
download is not a canonical runtime path, is no longer present in the current workspace and was
described there as having no bound textures/faces. A prose audit is not geometry.

### Required before implementation starts

Both of these must be available in the checkout or explicitly supplied to the implementing agent:

1. **Canonical held geometry** whose faces/UVs are already bound to
   `giant_jawblade_model.png`, either as a runtime-ready static item model or as the accepted
   Blockbench source plus a deterministic export route. The implementing agent may perform a
   mechanical export; it may not invent the missing shape, UV map or face binding.
2. **Accepted 2D inventory icon**, expected as
   `assets/mhnw/textures/item/giant_jawblade.png` or an explicitly named equivalent. Do not crop the
   UV atlas, generate a replacement or use a vanilla sword icon unless the maintainer separately
   authorizes that fallback.

The delivered filename resolves the earlier working-name ambiguity for this packet:

- Registry ID: `mhnw:giant_jawblade`.
- Display name: **Giant Jawblade**.
- `BoneBlade` / internal `BoneGreatsword` in the roadmap remain historical source/working names,
  not additional items.

If the accepted geometry package names a materially different weapon, stop for one naming decision
before registering an ID. Item IDs become save data; do not casually rename one after release.

### Rendering default after the gate is satisfied

The weapon is static: the audited Blockbench source had no authored animation. Prefer ordinary
baked item models. NeoForge 21.1.248 already supplies `neoforge:separate_transforms`, which can use
the 3D model only for first/third-person hand contexts and the flat icon for GUI/ground/fixed
contexts. That is the first choice because it needs no renderer class or animation runtime.

Use a client-only GeckoLib/custom renderer only if the accepted export format genuinely cannot be
represented by the native static model path. Record why, keep it out of common-loadable classes and
prove a dedicated server starts. Do not add a weapon animation controller for a static asset.

The implementing agent owns mechanical model export, JSON display transforms and minor scale/grip
adjustments needed to show the supplied art. Changing geometry, repainting the texture or inventing
an icon remains an art change and needs maintainer approval.

## 3. Preflight and branch facts

Fetch `origin`, inspect HEAD and dirty state, and create or reuse `r3/giant-jawblade` from the latest
accepted `origin/master`. Preserve newer work and supplied/untracked art or handoffs; do not reset,
overwrite or silently stash user changes.

At handoff authoring time the current local `r2/field-preparation` branch also has two clean,
unpublished commits beyond its merged remote head:

```text
5db5bd8 Corpse presentation: no death tint, hurtboxes on the fallen body
adec304 Corpse presentation: keep Aptonoth down, ground every species' parts
```

Together they report 149/149 tests but are not in `origin/master`; master reports 148/148 after R2.
They are user work. Leave the branch and both commits reachable. Do not cherry-pick this unrelated
corpse fix into R3 merely to make it travel. If it merges separately before R3 starts, branch from
the new master and accept the resulting 149-test baseline; otherwise R3 starts from the 148-test
master.

Read fully before editing:

- `CLAUDE.md` and the `AGENTS.md` pointer.
- This packet and R3/Q11/Q12/Q14/Q15/Q22/Q23 in `docs/ROADMAP.md`.
- The current R1/R2 sections and top count in `docs/TEST_PLAN.md`, plus relevant
  `docs/DEFERRED.md` entries.
- `ModItems`, `MHNW`, `CarveState`, `MonsterPart`, existing item classes and nearby item/attribution
  GameTests.
- The pinned 1.21.1 sources for `SwordItem`, item attributes/use completion, `Player.attack`,
  `ProjectileUtil` and NeoForge `SeparateTransformsModel` before choosing signatures/model JSON.
- Every supplied greatsword source/export/texture file, including its real format, UVs and display
  transforms. Do not assume a `.bbmodel` and a runtime model are interchangeable.

Current implementation baseline to preserve:

- Minecraft 1.21.1, NeoForge 21.1.248, GeckoLib 4.9.2, TerraBlender 4.1.0.8, Java 21,
  ModDevGradle 2.0.146 and Gradle 9.2.1 are deliberate pins.
- R2's accepted PR head passes 148/148 GameTests. Run the actual selected baseline fresh; historical
  counts are not a run.
- Native `Level.getEntities` includes NeoForge `PartEntity` instances, and `MonsterPart.hurt`
  forwards to one parent owner. A view-vector target query can therefore select one visible part
  without adding multipart-specific weapon code.
- `Player.attack` already owns attack events, enchantment scaling, cooldown strength, knockback,
  durability callbacks, sounds/stats and a player-caused damage source. Reuse it rather than
  calculating a parallel damage pipeline.
- `CarveState` credits positive accepted player damage. A charged attack that ends in exactly one
  `Player.attack` needs no special reward integration.
- No custom player animation library or packet protocol exists, and this packet does not add one.

## 4. Locked gameplay contract

### Item properties

Implement `mhnw:giant_jawblade` as the smallest suitable `SwordItem` subclass, with tooltip-visible
targets rather than guessed constructor arguments:

| Property | Contract |
|---|---|
| Total attack damage | **9.0** at full attack strength |
| Attack speed | **0.8** |
| Durability | **250** |
| Enchantability/tool behavior | Vanilla iron-tier sword behavior |
| Repair item | Vanilla bone |
| Creative tab | Combat |

Use the pinned `SwordItem.createAttributes`/tier API so normal left-click attacks remain completely
vanilla. If the exact constructor expresses the total through tier bonus plus an item modifier,
test the observed player attributes/tooltip result instead of copying the table's number into the
wrong parameter.

The weapon has no passive buff, set bonus, status, armor interaction or monster-specific damage.
Its role is simply a slower, harder early weapon with one deliberate longer-reach option.

### Survival acquisition

Add one shaped crafting-table recipe:

```text
BBC
BHC
BSH
```

- `B`: vanilla bone, four total.
- `C`: `mhnw:monster_claw`, two total.
- `H`: `mhnw:monster_hide`, two total.
- `S`: vanilla stick, one total.
- Result: one `mhnw:giant_jawblade`.

One complete Great Izuchi carve sequence supplies at least those monster materials and bones, so
the recipe is never rare-gated or circular. It competes with the bone-armor use of the same
materials, giving the player a real equipment choice without another economy tier.

Do not add ore generation, intermediate blade parts, smithing templates or a custom workstation.

### Fixed charged strike

The secondary attack is one fixed held-use completion, following R2's proven server-authoritative
shape rather than adding a state machine:

1. Main-hand right-click with no item cooldown starts using the weapon.
2. Use duration is **30 real ticks** with vanilla `SPEAR` use presentation. Vanilla held-item use
   supplies its ordinary movement slowdown; do not immobilize or override input.
3. Releasing early, swapping the item, dying or otherwise cancelling produces no strike, cooldown,
   damage or durability loss.
4. Completing 30 ticks asks the server for the first attackable/pickable entity on the player's
   block-clipped view vector, to a maximum of **4.5 blocks**. Use the native projectile/view query or
   a direct equivalent; solid blocks cap the trace. Do not guess a nearest entity off-axis.
5. If a target exists, call `Player.attack(target)` exactly once. Do not manufacture a custom
   damage source or call both a part and its parent. The 30-tick windup exceeds this weapon's normal
   full-strength delay, so the vanilla attack should be fully cooled; prove that rather than
   force-writing the private attack ticker.
6. Whether the strike hits or misses, swing the main hand, play one appropriate vanilla attack cue
   and apply a **30-tick cooldown** to the item. This miss recovery is what makes completion a
   commitment rather than a free target scanner.
7. A successful hit uses ordinary sword durability exactly once. A miss consumes no durability.

The charged strike does not multiply damage beyond the weapon's normal fully cooled 9.0. Its reason
to exist is deliberate 4.5-block reach with a visible 30-tick commitment. Normal left-click remains
the faster close-range option. Do not add a cone, sweep, cleave, charge tiers, release-timing bonus,
knock-up or target lock.

Presentation cues are not hit authority. The server trace, completion and `Player.attack` call own
the result. Client prediction may start the pose immediately, but may not damage locally.

### Cancellation and persistence

Store no per-player or per-stack charge fields. Vanilla's active-use state is transient and already
ends on release, item swap, death and disconnect. A saved/reloaded item is just an ordinary weapon;
it cannot resume or cash in a pre-reload charge. There is no capability, attachment, data component
or packet for this.

## 5. Small owned implementation surfaces

Prefer:

- One `GiantJawbladeItem extends SwordItem` for use completion and the target query.
- One item registration, one recipe, one language entry and the minimum accepted model resources.
- Native `neoforge:separate_transforms` model JSON if the accepted static export supports it.
- Existing `MHNW.onBuildCreativeTabs` for the combat-tab entry.
- Focused additions to the existing `MHNWGameTests` class.

Do not create `Weapon`, `Greatsword`, `ChargeableWeapon`, combo, moveset, renderer registry or packet
abstractions with one consumer. A second real weapon can expose what is actually common later.

## 6. Implementation sequence

### A. Enforce the art/start gate

1. Fetch and inspect `origin/master`, local commits and untracked inputs.
2. Locate and inspect the canonical geometry/export, UV bindings, 3D texture and inventory icon.
3. If geometry or icon is absent, report the section 2 blocker and stop without a branch push/PR.
4. If present, record their paths/hash/dimensions and whether any mechanical export is required.

### B. Establish the accepted code baseline

1. Create/reuse `r3/giant-jawblade` from the latest accepted `origin/master` without disturbing the
   local corpse-fix branch.
2. Run a clean build/full GameTests once and record whether the baseline is 148 or a newer accepted
   count.
3. Inspect the pinned APIs rather than porting old Forge/GeckoLib examples.

### C. Land the ordinary weapon first

Register the sword, exact observed attributes/repair behavior, recipe, localization and creative-tab
entry. Prove it behaves as a normal vanilla sword before adding its secondary use.

### D. Add the one committed attack

Implement the fixed 30-tick use, one block-clipped 4.5-block target, one `Player.attack`, and one
recovery cooldown. Test root entities, native parts, walls, misses, range and cancellation before
touching presentation polish.

### E. Integrate the accepted presentation

Use the supplied geometry/UV atlas/icon and tune only display transforms/scale/grip. Check all four
hand contexts and inventory/ground/fixed contexts. Do not hide missing faces or icon problems with a
code fallback.

### F. Validate, document and prepare the PR

Run the full suite, jar/resource inspection, dedicated server and bounded client checks. Update the
current docs, review the diff, then commit/push/open one PR.

## 7. Required automated evidence

Keep every accepted baseline test passing and add focused coverage for at least:

| Gate | Required proof |
|---|---|
| **R3-01 Registry/data** | `mhnw:giant_jawblade` resolves, appears in the intended tab, has packaged lang/model/texture resources, and the shaped recipe matches only the exact four-bone/two-claw/two-hide/stick pattern |
| **R3-02 Vanilla properties** | Equipping it produces observed total attack damage 9.0 and speed 0.8, durability 250, bone repair acceptance and no duplicated/stale attribute modifier after swap/reload |
| **R3-03 Ordinary attack** | A fully cooled normal player attack deals through the vanilla path, damages one root or selected `MonsterPart` parent once, consumes durability once and records carve participation only on positive accepted damage |
| **R3-04 Charge timing** | No strike occurs before 30 real ticks; one uninterrupted completion strikes at most once; release/swap/death before completion produces no damage/cooldown/durability loss |
| **R3-05 Range/occlusion** | A centered target within 4.5 blocks can be hit; beyond 4.5, off-axis and behind a solid wall cannot; the fixture proves its own distances and obstruction |
| **R3-06 Single target/parts** | The first selected entity or part receives one `Player.attack`; a second nearby target and sibling parts do not create AOE or duplicate parent damage |
| **R3-07 Recovery/miss** | Completed hit and completed miss each start exactly one 30-tick item cooldown; miss changes no health/durability; cooldown prevents immediate reuse and then expires normally |
| **R3-08 Reload/authority** | Item/NBT round-trip retains ordinary durability/enchantments but no charge; damage is server-only and no client renderer/model class is common-loaded |
| **R3-09 Regression** | Every accepted R0-R2 and any separately merged corpse-fix test still passes unchanged |

Drive public behavior. Do not set private use counters or call the target's `hurt` directly to make a
charge test green. Reuse the R2 held-use testing lesson where appropriate, but include at least one
real ticked use path from item in hand through completion and target selection.

Validation before push:

```powershell
.\gradlew.bat --no-daemon clean build
.\gradlew.bat --no-daemon runGameTestServer
.\gradlew.bat --no-daemon build runGameTestServer
git diff --check
```

Do not run `runServer`, `runClient` or `runGameTestServer --rerun-tasks` concurrently; R2 proved they
share and can rewrite the same `build/`/`run/` state. Inspect the built jar for the item class,
recipe, language entry, model loader JSON, held model, UV atlas and icon. Start a real dedicated
server from the built artifact far enough to catch accidental client-class loading.

## 8. Human and real-process acceptance

R3 cannot be called visually complete from headless evidence. With a client available, record:

1. The 2D icon is readable in inventory/hotbar/recipe output and does not show the UV atlas.
2. The 3D blade appears in first-person right and left hand and third-person right and left hand,
   with correct texture/UVs, scale, orientation and grip; no missing faces, mirroring or clipping
   through the whole player.
3. Dropped/fixed-frame presentation intentionally uses the approved 2D model unless the accepted
   art direction says otherwise.
4. Ordinary left-click looks and behaves like a slow heavy sword without breaking vanilla attack
   timing, enchantments, shields or other equipment.
5. Holding right-click visibly reads as a 30-tick windup; early release does nothing; completion
   swings once; hit and miss recovery are understandable.
6. F3+B against Great Izuchi/Aptonoth confirms the centered view strike selects the visible root or
   part, does not pass through walls and does not double-hit the parent.
7. Two clients agree on pose, target, damage, durability and cooldown. A second player near the
   target is not hit merely for being inside a wide area.
8. Save/restart/rejoin retains the item/durability/enchantments and never resumes a charge.

The maintainer is the visual-acceptance owner. A client screenshot or subjective “looks right” from
the implementing agent is evidence to review, not self-approval of altered art.

Carry all still-open R0-R2 human gates separately. R3 does not claim to close natural population,
village, full-loop, armor/corpse appearance, endemic/preparation feel, two-client or real-restart
work unless those exact observations are actually performed.

## 9. Documentation and PR delivery

After successful implementation, update only facts made true:

- `CLAUDE.md`: one concise greatsword architecture/behavior note and current test count; keep
  `AGENTS.md` a pointer.
- `docs/TEST_PLAN.md`: dated R3 results, mutation/repeat evidence, jar/server/client checks and every
  pending human gate.
- `docs/ROADMAP.md`: replace the stale “no texture/bound faces” evidence with the exact accepted art
  facts and mark R3 implemented only after visual acceptance is accurately represented.
- `docs/DEFERRED.md`: retain further weapons, upgrade trees, R8 combat, final polish or missing human
  evidence with precise triggers.
- `README.md`: one short feature/acquisition description if its existing format warrants it.

Use focused commits ending with the exact Claude attribution required by `CLAUDE.md` unless the
session directly supplies a different attribution rule:

```text
Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>
```

Push `r3/giant-jawblade` normally, never force-push, and open one PR against `master`. Its body must
include baseline, accepted art paths/provenance, any mechanical export, exact stats/recipe/charged
strike, validation and test count, client evidence, local overrides, pending gates and:

```text
🤖 Generated with [Claude Code](https://claude.com/claude-code)
```

Do not include the unrelated local corpse-presentation commit unless it first merges through its
own accepted path. Do not merge the R3 PR or publish a release.

## 10. Copyable kickoff

```text
Implement docs/R3_BONE_GREATSWORD_HANDOFF.md only if its canonical held geometry and accepted 2D
inventory icon are now supplied. The committed 64x64 giant_jawblade_model.png is only the on-hand
UV atlas; it is not either missing file. If the gate remains incomplete, report the exact missing
inputs and stop without placeholder code, push or PR. If complete, preserve local work, branch
r3/giant-jawblade from current accepted origin/master, run the fresh baseline, then deliver one
static Giant Jawblade, its R1-material recipe and one fixed 30-tick/4.5-block single-target charged
strike through Player.attack. Keep vanilla combat and all R0-R2 contracts; build no weapon/combo/R8
framework. Validate with focused GameTests, full regressions, jar/server/client evidence, push
normally and open one PR only. Do not merge or release.
```
