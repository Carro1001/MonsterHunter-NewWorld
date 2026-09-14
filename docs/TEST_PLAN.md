# Manual test plan

What still needs a human at a screen. Everything else (damage semantics, timing windows,
state-machine wedging, save/reload of gameplay facts, navigation) is covered by headless GameTests
via `gradlew runGameTestServer` — see `MHNWGameTests.java`, **currently 175 tests, all passing**
(`.\gradlew.bat --no-daemon build runGameTestServer`, 2026-09-12, presentation/feel round and the
corpse-presentation fix, both rebased onto the R3/Izuchi-tail-swipe master; 157 after the corpse fix
alone, 156 before either, 148 after the R2 field-preparation packet, 123 before that packet, 121
before the R1 PR #6 review round, 97 before
R1, 85 before R0b). The earlier 69-, 75-, 85-, 86- and 97-test figures are superseded — note the R0a round's
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
Either way it logs attack transitions and contact accepted/rejected, and draws Great Izuchi's and
small Izuchi's live attack volumes with F3+B.

**Bone probe**: with `debugCombat` on, Great Izuchi, small Izuchi, Rathian, Rathalos, Aptonoth and Lagiacrus log their
actual runtime bone positions to the game log every ~2 seconds (or every tick for Great Izuchi
while it's mid-attack), in the same left/up/forward frame the hurtbox offsets use. This is what
actually fixed Rathian's and Aptonoth's hurtboxes this round (see their sections below) — real
measurements read straight from a play session, not another guess. Rathalos is borrowing Rathian's
measured numbers as a proxy for now (same skeleton, similar proportions); if you ever want it
measured for real, stand near one with `debugCombat` on for a few seconds and send me
`logs/latest.log`.


---

## Alpha pass 2 — Great Izuchi's real health, and two jawblades to choose between (2026-09-13)

175 tests, all passing.

- **`GreatIzuchi.MAX_HEALTH` is 120**, the number `DEFERRED.md` always named as the intent. 40 was a
  development convenience; a charged Giant Jawblade ended it in three hits, which leaves no room for
  the armour-then-rematch arc. Still a playtest number — the alpha decides whether it stays.
- **The Giant Jawblade is GeckoLib-animated, and the 48-model approach is gone.** Both shipped side
  by side for one round so they could be held one after the other; the clip won on 2026-09-13. The
  48 `giant_jawblade_charge_*.json` models, `tools/gen_jawblade_charge_models.js`, the `mhnw:charge`
  item property and `CHARGE_POSE_STEPS` were deleted, and `GiantJawbladeGeoItem` folded into
  `GiantJawbladeItem`. The item id, recipe and stats are unchanged, so nothing on disk moved.
- **The hunter's arms are posed too**, in third person, for every player -- a custom
  `HumanoidModel.ArmPose`, no animation library. See `docs/WEAPON_POSING.md`.

### Playtest results — three rounds, all resolved

| | |
|---|---|
| **J1** | Third-person charge — **accepted** after three corrections: pivot moved to the hand, swing cut to 3/5 (54 degrees, not 90), grip moved into the top third of the handle. |
| **J2** | First person — **accepted**. The clip poses the weapon; the arms are not posed there (`applyForgeHandTransform` would, and is priced in `DEFERRED.md`). |
| **J3** | Rotation signs — **correct as authored**, no negation needed. |
| **J5** | Release — **accepted**; the snap back to rest reads as the swing. |
| **J4** | Hotbar icon, dropped item, item frame — **was broken, now fixed**. The icon wound itself up in real time along with the held weapon. `isPerspectiveAware()` does not prevent this: it gives each context its own animation state, but they all run the same predicate, and the predicate matches by stack identity — which the hotbar and the hand share, being the same object. The clip is now gated to the four hand contexts by `GiantJawbladeItem.animatesIn`, guarded by `r3JawbladeAnimatesOnlyInAHand`. The `display` block was never the problem; all nine contexts survived the move unchanged. |
| **J6** | A **second player** charging nearby — **still unchecked**. Their wind-up is found by stack identity and plays from its own start, so a charge already in progress when you look at it replays from the beginning. |

### Second playtest of the GeckoLib jawblade — three fixes, all in assets

- **Crash on right-click in third person.** `enumExtensions` was at the root of
  `neoforge.mods.toml`; FML reads it per-mod (`IModInfo.getConfig()`), so it was silently ignored
  and `EnumProxy.getValue()` threw inside the render path. Moved under `[[mods]]`, and the accessor
  now degrades to the ordinary pose and logs once rather than crashing.
- **The swing was far too wide** — the blade came out flat across the screen and left the
  first-person view at full charge. Both the clip's rotations and `MHNWArmPoses`' four constants
  were cut to **three fifths**: the blade now tops out at 54 degrees rather than 90, the lead arm at
  about 64 rather than 106. **Scale those two together or the grip and the blade disagree.**
- **The hunter gripped the weapon by its pommel.** The display translation of `[0, 7, 1.75]` put
  the hand at geo y `-7`, two units off the end of a handle running `-9..2`. Mid-handle is `-3.5`,
  so the translation became `3.5` — and then `-0.5` in the third round, putting the hand at geo
  `0.5`, in the top third of the handle just under the guard, where a lead hand actually goes.
  Mid-handle still read as low.

### Two facts this round established the hard way

- **`runGameTestServer` exits zero when the mod fails to load.** The first cut of the GeckoLib item
  named `Minecraft` in its own method body, which NeoForge's `RuntimeDistCleaner` refuses on a
  dedicated server; mod loading threw, no test ran at all, and Gradle still printed BUILD
  SUCCESSFUL. **A green build is not proof the suite ran** — check for the
  `GAME TESTS COMPLETE` line, and for `invalid dist` in the log. The fix is in
  `GiantJawbladeItem.registerControllers`: the predicate is an anonymous class, which is a
  separate class file and so loads only when the render path runs it. A lambda is not.
- **GeckoLib 4.9.2 supports no `anim_time_update` MoLang field** (checked in the sources jar, as
  with the no-public-seek finding). So the charge clip is kept in step with the charge by matching
  its length to `OVERCHARGE_TICKS` and its keyframes to `TIER_TICKS`, and
  `r3GeckoJawbladeMatchesTheWeaponAndItsChargeClock` recomputes both from the constants rather than
  hardcoding seconds.

---

## Alpha pass 1 — habitat exclusivity and the herbivore's bone (2026-09-13)

For the friends-and-family alpha. Two gameplay changes and one real test fix; still 174 tests, all
passing (`.\gradlew.bat --no-daemon build runGameTestServer`, three consecutive clean runs).

- **The Verdant Hunting Grounds no longer spawns vanilla mobs.** Its biome JSON carried a
  plains/forest roster of its own — sheep/pig/chicken/cow/wolf, eight monsters at weight 95-100
  each, bats at 10 — while Great Izuchi's modifier adds one entry at **weight 2**. The mod's own
  creatures were being outbid in their own habitat, so the hunt the biome exists for barely
  happened. `creature`, `monster` and `ambient` are now empty in the JSON; the biome modifiers are
  the only thing that fills them, which also makes those modifiers the single owner of the roster
  the way `CLAUDE.md` already says they should be. `underground_water_creature` (glow squid) is
  left alone: it competes with nothing of ours. Vanilla dungeon spawners from
  `minecraft:monster_room` are untouched — that is a structure, not the surface population.
  `huntingGroundsHasOneEntryPerSpecies` now asserts this per category rather than per species, so a
  re-added cow fails it too.
- **Aptonoth's third carve yields 2 bone** (was a second helping of raw meat). The habitat's own
  bone source, so the armor's vanilla half doesn't send a player back to a skeleton somewhere else.
  Hide and claw are unchanged and still Izuchi-only: the first Izuchi kill in iron/leather stays
  the gate on bone armor, which is what then makes Great Izuchi tractable.
- **`izuchiAttacksAndDamagesTarget` was intermittently failing on a clean tree**, not from this
  round's changes. The cause was environmental and took far too long to find: `run/config/mhnw-common.toml`
  had `debugCombat = true` left over from a play session, and that flag used to gate small Izuchi's
  unfinished, damage-less tail slam. With logging on the mob picked the slam half the time and spent
  88 ticks landing nothing. Three timeout raises (400→800→1600) and four fixture theories -- aim
  pinning, position pinning, tick phase, target re-assertion -- all measured the same ~20% and all
  missed it. What found it was making the failure message report *swipes started* versus *ticks
  inside the active window*: `swipes started: 0, phase: ATTACK` pointed straight at attack
  selection. The gate is now `MHNWConfig.TAIL_SLAM_PREVIEW`, the budget is back to the original 400,
  and twelve consecutive runs pass **with the stale `debugCombat = true` still in place**.
  Lesson worth keeping: make the failure message discriminate between distinct defects before
  touching the timeout.

### Still open for the alpha

- Natural-spawn density has not been re-observed in a real world since the vanilla roster came out.
  H08 below was already open; this makes its numbers stale in the player's favour, not accurate.

---

## R1 — the first hunting loop (2026-09-12)

Branch `r1/first-hunting-loop`, off accepted `master` at `516884c` (PR #5). The rest of R1 after
R1a's habitat and R0b's presentation: carvable corpses, the material/food economy, bone armor, and
small-Izuchi harassment.

### Commands and results

```powershell
.\gradlew.bat --no-daemon build runGameTestServer   # BUILD SUCCESSFUL, 123/123 GameTests passed
.\gradlew.bat --no-daemon runServer                 # Done (4.668s), no errors
git --no-pager diff --check                         # clean
```

Baseline before any edit, on this machine, this session: **97/97, BUILD SUCCESSFUL**. After the
packet: **121/121**. After the PR #6 review round: **123/123**, and the suite was run **eight
consecutive times with no failure** to answer that round's reproducibility finding. The 26 new tests
are the `r1*` block at the end of `MHNWGameTests.java`.

The dedicated-server boot was run against a throwaway game directory (`run/serversmoke`, deleted
afterwards) rather than `run/`, so the maintainer's own `run/world` was never opened. That required
a temporary `gameDirectory` line in `build.gradle`'s server run block and a dev-server
`eula.txt=true`; both were reverted/removed, and `git status` on `build.gradle` is clean. Noted as a
local override.

Jar inspection of `build/libs/mhnw-0.2.0.jar`: all eight new item models present, each resolving to
a texture that is actually packaged; seven recipes (four shaped armor, three cooking) and three
empty entity loot tables present; all eight item names and all five carve messages present in
`en_us.json`. Checked programmatically, not by eye.

### What landed

- **Carving.** `entity/CarveState.java`, one contract held by composition in `GreatIzuchi`,
  `Izuchi` and `Aptonoth`. Sneak + main-hand use on a corpse, three personal carves per eligible
  player, deterministic reward table, all-or-nothing inventory insertion, ten-tick per-player
  debounce, localized action-bar feedback.
- **Corpse window.** 12,000 entity-ticking ticks, counted in vanilla's own `deathTime` rather than
  a second saved field. `setPersistenceRequired()` at death keeps the distance-despawn rule from
  deleting a body early. Parts stay parent-owned for the whole window and unregister once with it.
- **Economy.** `mhnw:monster_hide`, `monster_claw`, `raw_meat`, `cooked_meat` and the four legacy
  armor ids, in existing vanilla creative tabs. Furnace/smoker/campfire cooking, four shaped armor
  recipes, three empty loot tables.
- **Bone armor.** `ArmorMaterials.IRON` verbatim for stats; `mhnw:bone_armor_set_bonus` +0.1
  knockback resistance as a transient modifier recomputed on every armor-slot equipment change.
  Worn model is the complete eight-bone `geo/entity/bone_armor.geo.json` with all eight bone getters
  mapped in `MHNWClient.BoneArmorRenderer`.
- **Small-Izuchi harassment.** `entity/IzuchiHarassGoal.java` replaced `MeleeAttackGoal`: circle at
  5–8 blocks, a randomized 40–80 tick opportunity window, a dart of at most 30 ticks landing at most
  one ordinary hit, then a 20–40 tick retreat. At most one darter within 12 blocks.

### Correction to the packet's own arithmetic

The handoff gives the four armor patterns and then summarises the full set as "7 hide, 4 claws and
13 bones". The patterns as written actually cost **5 hide, 4 claws, 15 bones**. The patterns are the
concrete spec and were implemented verbatim; the summary line is an arithmetic slip. Product intent
is unaffected — two fully carved Great Izuchi still cover the hide and claws, and ordinary skeleton
bones finish the set. README quotes the real cost.

### Mutation runs — these tests can actually fail

Each mutation was applied, the suite run, then reverted:

| Mutation | Caught by |
|---|---|
| `CarveState.fits` always returns true | `r1fullinventoryconsumesnothing` — "a carve into a full inventory was still counted" |
| debounce check removed | `r1debouncerejectsarepeatedclick` — "carved twice inside the 10-tick debounce" |
| pack scan never sees a neighbour darting | `r1izuchipackkeepsonedarteratatime` — "2 Izuchi darted at once" |
| dart deadline × 100 | `r1izuchiunreachabledartendsonitsdeadline` — "a dart ran 449 ticks, past its 30-tick deadline" |

The fourth mutation is why `r1IzuchiUnreachableDartEndsOnItsDeadline` exists as a separate test: in
`r1IzuchiCirclesDartsAndRetreats` the dart always reaches melee range and ends on its hit, so the
deadline is never the thing that stops it, and that test passed with the deadline mutated. Sealing
the target in stone is what makes the deadline the only way out.

### PR #6 adversarial review round (same day)

Three findings, all three accepted and fixed. Suite went 121 → 123.

**1. The unreachable-dart test was nondeterministic.** Its final assertion sampled `isDarting()` at
the 500-tick observation boundary — an arbitrary instant in a repeating randomized loop, so a
perfectly legal dart that happened to start just before the window closed failed the test. It did,
once, in review: one clean run failed with "the Izuchi was still holding the dart slot at the end of
the run" and an immediate rerun passed. Rewritten to count darts that were observed to *end*, and
to assert only on those: `completedDarts > 0` and `longestCompletedDart <= 30 + tolerance`. The
end-of-run sample is gone. The reproducibility claim was re-earned by running the full suite **eight
consecutive times, 123/123 every time**. The original failure was not itself reproduced on demand —
the diagnosis is from the assertion's shape, not from a caught repeat.

**2. Peaceful difficulty did not cancel the harassment.** Real, and the packet asks for it
explicitly. Peaceful discards hostile mobs through `Mob.checkDespawn` only when their
`shouldDespawnInPeaceful()` agrees, and `Izuchi` deliberately returns false — so a world switched to
peaceful mid-fight kept both the Izuchi and its target, and the goal, which only checked target and
liveness, kept circling and darting. `IzuchiHarassGoal.canUse()` now routes through
`canHarass(mob, target, difficulty)`, which refuses peaceful. Note this was equally true of the
vanilla `MeleeAttackGoal` it replaced; the packet's contract for the new goal is what puts it in
scope.

The test passes the difficulty in rather than setting it on the level. Difficulty is global and the
GameTest world is shared with every test running beside it — flipping it to peaceful even for a few
ticks discards other tests' vanilla hostile mobs (`r1BoneArmorFullSetAddsOneKnockbackModifier` alone
would lose its zombie), which is exactly the cross-test interference the arena fixture note below
already warns about. So `r1IzuchiHarassmentStopsOnPeaceful` asserts the rule against the real
precondition and that `canUse()` consults the live difficulty. **A genuine in-world peaceful switch
is a named human check** (below).

**3. Death left the harassment phase and aggressive flag stale for the whole corpse window.** Also
real, and it is the case an in-goal guard structurally cannot cover: vanilla stops ticking every
goal the instant `isDeadOrDying()` is true, so `stop()` never runs for a mob killed mid-dart.
`Izuchi.die` now clears the phase, the aggressive flag and the navigation. The neighbours' pack scan
keeps its `isAlive()` filter — that is not redundant, it covers the case `die()` cannot: a body
removed by `discard()` without `die()` ever running.

`r1IzuchiDeathClearsTheHarassmentState` waits for a dart to be genuinely in flight before delivering
the kill, so a passing run cannot be one where there was nothing to clear.

The review found no dependency or abstraction bloat, and nothing to change about the carving
composition, the `deathTime` reuse, the iron material reuse, the deterministic reward table or the
neighbour scan.

### PR #7 adversarial review round (same day)

Two P1 correctness findings, both reproduced against the code before fixing and both now guarded by
a test that fails without the fix.

**1. The five-block flash radius was a ten-block cube.** `FlashEffect.flash` used the inflated
broad-phase `AABB` as its final eligibility test. An inflated box reaches ~8.7 blocks at its corners,
so a target on the diagonal at `(+4, +4)` — 6.38 blocks from the flash — was flashed anyway. Fixed
with one squared-distance guard against `RADIUS * RADIUS`, keeping the box as the cheap query it
should always have been. This was a **pre-existing P3 bug inherited by R2**, not something the packet
introduced: the wild flashbug's original `release()` inflated its own bounding box the same way. The
fix lands in the shared helper, so both callers get it.

The existing out-of-range cow could not have caught this: at `|dx| = 6.5` it is outside the cube and
never reaches the guard. The new test's cow is deliberately inside the cube (`|dx| = |dz| = 4.5`) and
outside the radius, and asserts both of those facts about itself first, so it cannot quietly stop
testing what it was written for.

**2. A latecomer could steal an already-burning fuse.** `Toad.hurt` tested `provokerId == null` to
mean "nothing recorded yet" — but `null` is also the correct, final record for a fuse lit by a mob or
the environment. Since `ToadFuseGoal.start()` clears `provoked`, a player who hit the toad during
that same 40-tick fuse filled in the empty slot and collected carve credit for a blast somebody else
set off. Attribution is now recorded only by a hit taken while `!isFusing()`, and records "nobody"
explicitly rather than leaving the slot open.

Mutation run: with the two guards reverted and the new tests kept, exactly those two tests fail and
nothing else does.

### PR #7 follow-up review: the pre-start scheduler gap

A third P1, and the first fix for finding 2 was genuinely incomplete rather than merely narrow.

Guarding attribution on `!isFusing()` alone is not enough. That flag is set by
`ToadFuseGoal.start()`, which the goal selector runs on its **own every-other-tick cadence**, not
during `hurt`. So between the hit that sets `provoked = true` and the goal actually starting, there
is a window of one or two real ticks in which a second hit still sees `!isFusing()` and overwrites
`provokerId` -- the same theft the first fix was meant to stop, one scheduler tick earlier.

Both flags are now required, and they cover two different windows:

| Guard | Window it covers |
|---|---|
| `!provoked` | between the first hit and `start()`, where `isFusing()` is still false |
| `!isFusing()` | during the burning fuse, where `start()` has already cleared `provoked` again |

`r2SecondHitBeforeTheFuseStartsDoesNotStealAttribution` lands both hits back to back in the same
tick, with nothing ticking the toad in between, and asserts up front that the fuse has not started
yet -- so it cannot stop covering the gap it was written for. Mutation run: reverted to the
`!isFusing()`-only guard, exactly that one test fails and nothing else does.

### Live crash during the review round, and the coverage gap it exposed

A dev client threw a flash bomb and the integrated server crashed with
`NoClassDefFoundError: com/carro1001/mhnw/entity/FlashEffect`.

**Cause: the build-directory race again, self-inflicted.** `build/classes` was rewritten at
16:41:43; the crash was at 16:41:18 — a `clean build` had deleted the directory and had not yet
written it back while that client was running. `FlashEffect` is referenced only from
`FlashBombProjectile.onHit`, so it is loaded lazily, on first impact: precisely the class that would
still be missing. It has no client-only reference of any kind and ships in the jar. Nothing in the
code is at fault, and no test can defend against a classpath deleted underneath a running process.

This is the same rule as the earlier flake, and it now has two incidents behind it: **do not build
while a client or server is running against this project's `build/`.**

**The real gap it exposed.** `r2ThrownFlashBombReleasesOnceOnImpact` builds the projectile through
its `EntityType` constructor and drops it, so it never touched `FlashBombItem.use` or the
`(Level, LivingEntity)` shooter constructor — the item-to-projectile handoff a player actually
performs had no coverage at all. `r2FlashBombThrownFromTheHandFliesAndFlashes` now drives the whole
thing: use the item from a survival hand, consume exactly one, start the cooldown, confirm exactly
one projectile entered the world, let it fly and hit the floor unaided, and check it flashed a
facing target and discarded itself. It passes, which is also the positive evidence that the crashed
path is sound.

### Gates: what is closed and what is not

| Gate | Status |
|---|---|
| R1-01 registry/economy | **Closed headlessly** — `r1ItemIdsResolve`, `r1RawMeatCooksInEveryStation`, `r1BoneArmorRecipesCraft`, `r1CarvableSpeciesDropNoDeathItems`, plus the jar inspection above |
| R1-02 attribution | **Closed** — `r1AttributionCreditsOnlyRealPlayerDamage`, `…AProjectilesOwner`, `…AnOwnedAttackersPlayer`, `…IgnoresAnOwnerlessAttacker` |
| R1-03 personal quota | **Closed** — `r1TwoPlayersEachGetTheirOwnThreeCarves`, `r1CarveGatesAreEnforcedServerSide`, `r1DebounceRejectsARepeatedClick` |
| R1-04 atomic inventory | **Closed** — `r1FullInventoryConsumesNothing` |
| R1-05 persistence/expiry | **Closed in memory** — `r1ParticipationSurvivesALiveRoundTrip`, `r1CorpseRoundTripKeepsCountsAndRemainingTicks`. A real disk restart is still a human gate (below) |
| R1-06 corpse lifecycle | **Closed** — `r1CorpseWindowIsTwelveThousandTicks`, `r1CorpsesStayInertThenExpireExactlyOnce`, `r1CorpseGrantsExperienceOnlyOnce` |
| R1-07 armor | **Closed for stats and the modifier** — `r1BoneArmorMatchesIronStats`, `r1BoneArmorFullSetAddsOneKnockbackModifier`. Appearance is a human gate |
| R1-08 Izuchi harassment | **Closed** — six `r1Izuchi*` tests, including the peaceful precondition and death-clears-state cases added in the review round; the live peaceful transition is a human check |
| R1-09 regression | **Closed** — all 97 prior tests still pass; no measured attack, presentation clock, habitat mapping, spawn guard, endemic effect or roster change |

### What still needs a human — R1

- [ ] **Fresh-world end-to-end.** Without commands: find the habitat, meet the roster naturally,
      hunt a Great Izuchi with its escorts, carve, cook, craft and wear the full set, hunt again.
- [ ] **Bone armor appearance.** All four pieces visible in the correct slots on a normal player,
      following it through walking and crouching, first and third person. Specifically: **the boots
      must be visible.** That is the whole reason the complete `geo/entity/` export is used instead
      of the `geo/item/armor/` migration copy, which has no boot bones — the code path is proven
      headlessly, the pixels are not. Also check for gross mirroring, z-fighting or a missing
      texture.
- [ ] **Izuchi pack feel.** Does it read as circling and picking, with real pauses, rather than four
      rushers? Does target loss and rough terrain feel fair? The timings (40–80 window, 30-tick
      dart, 20–40 retreat) are initial tuning values; a small evidence-backed adjustment is allowed
      and should be recorded here.
- [ ] **Two distinct clients on one dedicated server.** Each gets only their own three carves;
      both agree on the body, its parts and its removal; simultaneous or both-hand interaction
      cannot duplicate a grant. This extends the R0b two-client gate rather than replacing it.
- [ ] **Real disk restart.** `save-all flush` → graceful stop → restart → rejoin, and check that
      living participation, carve counters and remaining corpse time all survive, with no duplicate
      escorts, XP, rewards or resumed combat. `r1CorpseRoundTripKeepsCountsAndRemainingTicks` is an
      in-memory NBT round trip and is **not** a substitute for this.
- [ ] **Carving feel.** Is shift + right-click on a large body discoverable? Do the action-bar
      messages read right and not spam?
- [ ] **Corpse presentation (2026-09-12 fix, needs eyes).** Three things at once on a fresh kill of
      each carvable species: no red tint on the body for the whole window (GeckoLib tints anything
      with `deathTime > 0`, which for us is 12,000 ticks); the body stays down after its death clip
      rather than popping upright (Aptonoth did — its clip rolls the body -90 about Z and vanilla's
      own flop added +90 over the same ~20 ticks, so they cancelled; `getDeathMaxRotation` is now
      zero for it, as it already was for Great Izuchi, Rathian and Rathalos); and shift +
      right-click lands on the model itself, not on a box floating where the living creature stood.
      `r1CorpsePartsDropToTheGround` proves the boxes come down to ground level, not that they
      visually cover the fallen model — parts keep their standing horizontal spread, so a species
      whose clip throws the body sideways may still read badly. That is the case to watch for.
- [ ] **A real in-world peaceful switch.** With an Izuchi actively harassing, run
      `/difficulty peaceful` and check it stops circling and darting and deals no further damage,
      then `/difficulty normal` and check it resumes. `r1IzuchiHarassmentStopsOnPeaceful` proves the
      precondition, not the live transition — the transition cannot be tested headlessly without
      changing global difficulty underneath every concurrently running test.

## The mod's own creative tab (2026-09-12)

Every MHNW item was scattered across five vanilla tabs (ingredients, food, combat, tools, spawn
eggs). `ModCreativeTabs.MAIN` replaces that with one tab of its own, icon the Great Izuchi spawn egg;
`onBuildCreativeTabs` now populates it in one pass instead of five key-matched branches. **159/159**
after `clean build` / `runGameTestServer` / `build runGameTestServer`.

`modCreativeTabResolvesWithItsIcon` checks the tab resolves in the registry with the intended icon.
It deliberately does not reconstruct NeoForge's `BuildCreativeModeTabContentsEvent` by hand to prove
the full accepted-item list and ordering headlessly -- that event needs a live
`ItemDisplayParameters`/`InsertableLinkedOpenCustomHashSet` pair only the real tab-population path
constructs, and every item it would check is already proven to resolve by the existing registry
tests. What it cannot catch is a wrong section or a missing `event.accept` call; that is a
30-second look at the tab in a running client, which is what actually happened here.

- [ ] **Open the tab.** Every item and spawn egg present, in the intended shelf order, nothing
      duplicated from a leftover vanilla-tab branch.

## R3 — The Giant Jawblade (2026-09-12)

Branch `r3/giant-jawblade`, from `origin/master` `0d972d0` (the PR #7 merge). Fresh baseline on that
commit before any edit: **148/148**. After the packet: **156/156**, eight new tests.

```powershell
.\gradlew.bat --no-daemon clean build        # baseline
.\gradlew.bat --no-daemon runGameTestServer  # 148/148, the accepted R2 tree
.\gradlew.bat --no-daemon build runGameTestServer   # 156/156 after the packet
```

### What the tests actually drive

| Gate | Test | What it would catch |
|---|---|---|
| R3-01 | `r3JawbladeRegistryAndResources` | id, packaged model/atlas/lang, the exact 3x3 pattern, and a rearranged grid that must **not** craft |
| R3-02 | `r3JawbladeHasItsStatedNumbers` | the numbers read off a ticked player's attributes (9.0 / 0.8), 250 durability, bone repairs and iron does not, and unequipping leaves nothing behind |
| R3-03 | `r3OrdinaryAttackRunsTheVanillaPath` | a left-click through a `MonsterPart`: parent health falls, one point of wear, one carve participant |
| R3-04 | `r3ChargeStrikesOnlyAfterThirtyRealTicks`, `r3CancelledChargeChangesNothing` | nothing at 28 ticks, a strike by 32, and a released hold that costs no damage, cooldown or durability |
| R3-05 | `r3ChargeRangeAndOcclusion` | in range hits; past 4.5 blocks, off the view vector, and behind a wall all miss — each case asserting its own measured distance |
| R3-06 | `r3ChargeStrikesExactlyOneTarget` | a second target in line is untouched, and a charge against a multipart body costs no more health than one measured ordinary hit |
| R3-07/08 | `r3ChargeMissRecoversAndCarriesNoState` | a miss still starts exactly one cooldown and costs no durability, the cooldown genuinely refuses the next charge and then expires, and an item round trip keeps durability while carrying no charge state |

### Mutation runs — these tests can actually fail

| Mutation | Result |
|---|---|
| `REACH` 4.5 -> 50 | `r3ChargeRangeAndOcclusion` failed |
| `addCooldown` deleted from `finishUsingItem` | `r3ChargeMissRecoversAndCarriesNoState` and `r3ChargeStrikesOnlyAfterThirtyRealTicks` failed |

The reach mutation initially failed at the fixture's own distance guard rather than at a behavioural
assertion, because the guard was written against the constant it was meant to police. The test now
states 4.5 literally as well, so widening the constant fails as the behaviour change it is.

### The fixture lesson that cost a round

**`ServerPlayer.tick()` is not the entity tick.** It is the connection tick — menus, camera, game
mode — and `doTick()` is the one that calls `super.tick()`, which is what counts down a held use,
expires an item cooldown and applies equipment attribute modifiers. A mock player is in the level but
not in the server's ticking player list, so `thenIdle` leaves it frozen entirely. Three tests failed
in a way that read as broken gameplay ("observed total attack damage 1.0", "the completed charge did
not strike", "the recovery cooldown never expired") when the fixture was simply standing still. See
`tickOnce` in `MHNWGameTests`.

A second one, cheaper: `ProjectileUtil.getHitResultOnViewVector` asks for the view vector at partial
tick **zero**, which interpolates from `yRotO`/`xRotO`. A fixture that sets only the current rotation
aims one tick into the past. `aimAt` sets both.

### PR #8 adversarial review round (same day)

Two findings, both real, both reproduced locally before being fixed.

**[P1] The charged strike swept.** `Player.attack` with a fully cooled `SwordItem` triggers vanilla's
sweep, so every living thing within a block of the struck target took 1.0 -- against a contract that
says single target. Fixed by refusing `SWORD_SWEEP` in `canPerformAction`; see `CLAUDE.md` on why
that, and not a flag around the attack call.

**The fixture lesson here is the more valuable half.** `r3ChargeStrikesExactlyOneTarget` already had
a second cow, and it passed against a genuinely sweeping weapon for *two* independent reasons: the
second cow sat at z=12, outside sweep's own 3-block range, and the test drove the charge through the
item seam without ticking the player, so the attack was never fully cooled and vanilla would not have
swept regardless. The test now places a bystander beside the target and calls `coolDown`, which
ticks to full strength and asserts it got there. Reintroducing the sweep fails it:
`a bystander beside the target lost 1.0 health; the strike swept`.

**[P2] A landed strike played two attack cues.** `Player.attack` already emits the hit's own cue, and
`finishUsingItem` added `PLAYER_ATTACK_STRONG` unconditionally on top. The cue now plays only on a
miss (`PLAYER_ATTACK_NODAMAGE`), and the target filter gained `Entity::isAttackable` to match the
handoff's own wording. **Neither half is GameTest-covered:** a played sound is not observable
headlessly, and the `isAttackable` narrowing has no convenient pickable-but-unattackable fixture.
Both are on the human checklist below.

### Build, jar and server evidence

- `build/libs/mhnw-0.2.0.jar` contains `com/carro1001/mhnw/item/GiantJawbladeItem.class`,
  `assets/mhnw/models/item/giant_jawblade.json`, `assets/mhnw/textures/item/giant_jawblade_model.png`
  and `data/mhnw/recipe/giant_jawblade.json`.
- `runServer` reached `Done (0.382s)!` with the mod loaded and no client-class loading error; the
  process was then killed by the smoke test's own timeout (exit 143). The weapon class imports
  nothing from `net.minecraft.client`.
- `git diff --check` clean.

### R3 art landed (2026-09-12), in two deliveries

The first delivery was `giant_jawblade.bbmodel` and its exported Java Item Model JSON: real cuboid
geometry, no `display` block at all. Verified before wiring it in: the embedded texture decodes
pixel-identical to the atlas already in the repo, so this was the model that atlas was actually
unwrapped from. Wired with `parent: minecraft:item/handheld` and hand/head/fixed transforms borrowed
from the old, never-shipped `BoneBlade.bbmodel` as a starting pose, since nothing authored existed.

Reported result: **the weapon didn't render at all** — in hand, in the inventory and on the ground,
where it still cast a shadow. An earlier revision of this document blamed the `gui` display scale.
**That was wrong**, and it is recorded here because it is the more useful half of the round.

The real cause was `parent: minecraft:item/handheld`, added by the implementing agent to inherit hand
transforms. `item/handheld` parents `item/generated` parents `builtin/generated`, and
`ModelBakery.bakeUncached` routes any model whose **root** parent is that marker through
`ItemModelGenerator` — which discards the model's own `elements` entirely and builds quads from
`layer0`..`layer4` (`ItemModelGenerator.LAYERS`). A cuboid model names its texture `"0"`, so the
generator found no layers, emitted **zero quads**, and produced a working item that renders nothing
while still casting an entity shadow. Nothing is logged on this path, which is why two rounds of
inspection found no error.

A 3D item model therefore declares **no parent** and carries its own `display` block.
`r3JawbladeModelDoesNotInheritTheFlatItemChain` now enforces exactly that, as text, because model
baking is client-only and a dedicated server cannot bake the model to count its quads.

The same day, a second delivery added a **fully authored `display` block** — `thirdperson_righthand`/
`_lefthand`, `firstperson_righthand`/`_lefthand`, `ground`, `gui`, `head`, `fixed`, `on_shelf` — from
the artist. Geometry and texture were diffed byte-for-byte against the first delivery and are
unchanged; only the pose is new. `giant_jawblade.json` now carries that geometry and that display
block verbatim, and **no parent at all**. No separate 2D icon exists, so GUI also renders the real
3D model, at the artist's own pose.

### Small Izuchi's animation set, redelivered (2026-09-13)

The artist supplied a new `izuchi.animation.json` adding `death`, `roar`, `rally` and
`attack_tailslam` to the five clips already shipped. It is committed **verbatim except for one
character**, and that exception is worth knowing about.

**One clip would not load at all.** GeckoLib reported
`Unable to parse animation: animation.izuchi.attack_tailswipe -> Failed to parse expression
'-97.4073+16.8822+'` — a MoLang expression truncated mid-term, trailing `+` and nothing after it. A
malformed expression fails the **whole clip**, so the tail swipe silently ceased to exist while the
other eight loaded normally; the only symptom in play was "the swipe isn't playing", with no error
unless you read the client log. The fix was deleting that one `+`, which restores the value
byte-for-byte to what the previously shipped file had at that exact keyframe — a known-good value,
not authored animation.

**It may want a different fix upstream.** Neighbouring keyframes on the same bone and axis read
`…+16.8822+Math.sin((query.anim_time - 1.5) * 180) * 1`, so the export more likely truncated a
`Math.sin` tail than added a stray operator. If so the correct value is the longer one and the tail
tip moves slightly differently. That is an art decision, and it needs fixing at source or the next
export reintroduces it.

**Nothing else is affected.** All 14 animation files were scanned — 13,320 expression strings — for
trailing operators, doubled operators, unbalanced parentheses and empty expressions. Exactly one
problem, the one above.

**The phantom bones were left alone, deliberately.** Every new clip animates 8 bones Izuchi's
geometry does not have (`left_shoulder`, `right_shoulder`, `left_ankle`, `right_ankle`, `mane`,
`tailblade`, `left_hand`, `right_hand` — Great Izuchi's skeleton). `GeoModel.crashIfBoneMissing()`
returns `false`, so GeckoLib skips those tracks silently; they are inert, and keeping them means the
next redelivery diffs cleanly against what the artist actually holds.

**Only `death` is wired.** Small Izuchi previously fell through to the idle branch while dead, so a
corpse stood there breathing for the entire ten-minute carve window. It now has the same synched
death anchor the other four species use, played with `thenPlayAndHold` — which matters, because
`thenPlay` passes `LoopType.DEFAULT` and DEFAULT **defers to the JSON's own `loop` field**, and this
clip's says `true`. `thenPlayAndHold` sets `HOLD_ON_LAST_FRAME` explicitly and wins.
`izuchiDeathAnchorAgesWithRealTicks` guards the anchor. `roar`, `rally` and `attack_tailslam` are
present and unwired.

### PR #10 adversarial review round (2026-09-13)

Three findings against head `9595c01`, all accepted. The reviewer independently reproduced
`clean build runGameTestServer` at 169/169 with a clean `git diff --check` before raising them.

**P1, and a real defect: tier damage was not stable unless the swing timer happened to be full.**
`Player.attack` scales damage by vanilla's attack-strength ramp, and at this weapon's 0.8 attack
speed that timer is 25 ticks -- but tier one was 20. A charge begun right after a left-click
therefore released undercooled. Measured, not estimated: **6.64 landed against an advertised 9.0**
(the reviewer's own estimate of ~7.4 was conservative; vanilla's curve is quadratic,
`0.2 + f*f*0.8`, not linear). `r3ChargeTiersLandTheirStatedDamage` cooled the attack before every
case, which masked it completely.

The fix keeps `Player.attack` semantics rather than bypassing them: `attackStrengthTicker` counts up
*during* a hold, so making the shortest charge equal the swing timer means holding one always
refills it. `TIER_TICKS[0]` 20 -> 25, and every tier now lands its stated damage from any starting
state. `r3ChargeFromAnUncooledWeaponStillLandsItsTier` was written **before** the fix and observed
failing at 6.64, then passing at 9.0.

**P1, documentation that would have caused a future agent to undo the work.**
`docs/R3_BONE_GREATSWORD_HANDOFF.md` still specified the superseded single fixed 30-tick
auto-firing `SPEAR` strike with no tiers, and `CLAUDE.md` still carried the matching stale bullets
alongside the new contract -- contradicting itself inside one section. The handoff subsection now
opens with a SUPERSEDED block and a before/after table pointing at the live contract, rather than
being quietly edited into agreement; the exclusion list says which exclusion was lifted and which
still bind.

**P2, death-clip statements contradicting the code** in `CLAUDE.md`, `docs/DEFERRED.md`, this file's
Izuchi checklist and `Izuchi.java`'s own class javadoc, all still saying no authored death clip
exists. All four updated, and the deferred entry's old "if a death clip is added later, check
whether it needs `getDeathMaxRotation` zeroed" is answered in place: it did, and it is.

### Izuchi pack anger and no-infighting (2026-09-13)

Piglin-style anger on small Izuchi, built on vanilla's `NeutralMob`. They stay hostile on sight;
hitting one makes every Izuchi within 16 blocks hold a grudge against that player for 20-39 seconds,
surviving loss of sight, chunk unload and reload, and expiring on its own.

`izuchiPackSharesAngerWithWhoeverHitOne` asserts the **grudge**, not the live target, and that is the
point: this species is hostile on sight, so every neighbour already targets the nearest player and
"did it target them" would pass with no pack behaviour at all. Mutation-verified by removing the
propagation (`packmate=false bystander=false`). `izuchiAngerSurvivesReloadAndExpires` covers the
memory across a save/load round trip and the timer running out.

**A real bug found on the way in:** both attack volumes damaged every `LivingEntity` they touched, so
a Great Izuchi's swipe hit its own escorts and those escorts retaliated through `HurtByTargetGoal`.
`izuchiPackDoesNotFightItself` covers all three directions, that nobody records a packmate as an
attacker, that they read as allied, and that **a player's hit still lands** -- without that last one
the test would pass against a guard that simply refused everything. Mutation-verified by removing
the guard (`leader->escort=true escort->escort=true`).

### R3 charge rework (2026-09-13): three tiers, held and released

**This overrides the R3 packet's own locked contract**, at the maintainer's direction after playing
it. `R3_BONE_GREATSWORD_HANDOFF.md` section 4 specifies one fixed 30-tick hold that fires by itself,
with "no damage multiplier" and an explicit exclusion of "charge tiers". All three are now gone:

| | Packet contract | Now |
|---|---|---|
| Firing | auto-fires when the 30-tick hold completes | **release to swing** |
| Tiers | none, explicitly excluded | **three**, at 25 / 45 / 75 ticks |
| Damage | fixed 9.0, "reach not damage" | **9.0 / 12.5 / 16.0** by tier |
| Overhold | n/a | **100 ticks auto-swings at tier one's damage** — the charge is wasted |
| Pose | `UseAnim.SPEAR` | `UseAnim.NONE` — SPEAR is the trident raise and read wrong |
| Movement | vanilla's 20% input scaling only | that **× 0.35 per tick**, a heavy crawl |

What did **not** change: one `Player.attack` per swing, one target, 4.5 blocks, block-clipped trace,
no sweep, 30-tick recovery on hit or miss. The tier bonus is a transient `ATTACK_DAMAGE` modifier
applied around that one call and removed in a `finally`, so enchantments, durability, attack events
and carve attribution still scale on vanilla's own pipeline rather than on arithmetic of ours.

**Superseded 2026-09-13 — the charge lean is now one GeckoLib clip.** The weapon is a `GeoItem`
drawn by `MHNWClient.JawbladeRenderer`; the 48 generated pose models, their generator and the
`mhnw:charge` item property were deleted after the two approaches were compared in play. What
remains true and worth keeping from that round: **an item property function is the only render hook
that receives the holder**, which is why the model-swap approach existed at all; a BEWLR and a
baked-model wrapper both get the stack alone. GeckoLib's item animation state has no holder either,
so the surviving clip recovers one by stack identity. See `docs/WEAPON_POSING.md`.

### What still needs a human — R3

- [ ] **Confirm it renders and reads right**, now that there is an authored pose rather than a
      borrowed placeholder or a missing one: first/third person both hands, GUI, ground, item frame.
- [ ] **Feel of the charge.** Do 25/45/75-tick tiers read as a deliberate wind-up? Is the lean
      smooth at 48 steps, and does the crawl feel like commitment rather than like a bug?
- [ ] **Tier cues.** Rising riptide sound per tier, particles off the weapon side that are visible
      in first person, and `ENCHANTED_HIT` instead of `CRIT` once waiting stops paying.
- [ ] **One cue per strike** (PR #8 P2, not headlessly testable). A landed charge should sound once,
      not twice; a miss should sound once. Also worth an ear: that left-click still sounds normal
      now that this weapon no longer sweeps.
- [ ] **F3+B against a real monster.** That the centred strike picks the visible part, does not
      reach through a wall and does not double-hit the parent — proven headlessly, not visually.
- [ ] **Two clients and a real restart.** Agreement on pose, target, damage, durability and
      cooldown; a bystander beside the target is not hit; an item survives a real restart with no
      resumed charge.

## R2 — Field preparation (2026-09-12)

Baseline: `master` at `78bd066`, the PR #6 merge. Branch `r2/field-preparation`.

Three preparation loops, one PR. Nothing in R0/R0a/R0b/R1/R1a changed behaviour: the 123 tests that
existed before this packet all still pass, unmodified.

```text
Aptonoth carve -> raw meat -> 80-tick BBQ spit  -> cooked meat
wild Flashbug  -> glass bottle -> one flash bomb -> one bounded thrown flash
wild toad      -> water bucket -> same variant on release -> hit once -> existing effect
```

### What landed

- **`mhnw:bbq_spit`** — shapeless from one `mhnw:raw_meat` plus one stick, stacks to one, held-use
  pose `BLOCK`, fixed 80-tick server-authoritative use, yields exactly one existing
  `mhnw:cooked_meat` and a 20-tick cooldown. No block, no GUI, no timing window, no fuel, no rare
  tier. The three R1 cooking recipes (furnace, smoker, campfire) are untouched.
- **`mhnw:bottled_flashbug`** — right-click a live Flashbug with a vanilla glass bottle. Stacks to
  16, crafting remainder is one glass bottle.
- **`mhnw:flash_bomb`** — shapeless from one bottled flashbug plus one paper; the bottle comes back
  through ordinary crafting-remainder semantics. Snowball-shaped throw, 10-tick cooldown, releases
  once on first impact, no damage of any kind, no terrain effect, not recoverable.
- **`FlashEffect`** — the one shared flash: 5-block radius, line of sight, 0.65 horizontal facing
  dot product, players never affected, 40 ticks of Blindness 0 and Movement Slowdown II. The wild
  Flashbug now calls it too, gaining the bounded slowdown while keeping its own hit-only trigger,
  11-tick telegraph, radius, facing/LOS rule and one-release discard.
- **Four toad buckets** at the exact preserved legacy ids and icons — `poisontoad_bucket`,
  `sleeptoad_bucket`, `paratoad_bucket`, `nitrotoad_bucket` — with vanilla water-bucket capture and
  empty-bucket release. Variant, custom name, health and `FromBucket` survive a round trip. A bare
  `/give` stack still releases the variant its item id names.
- **Blastoad attribution** — a player who provokes a toad is retained as the transient source for
  that fuse; a BLAST release names them as the explosion's causing entity, so the damage reaches
  `CarveState` through the identical rule as a direct hit.

### Automated results

148/148 passing. 25 new tests covering gates R2-01..R2-11, added next to the existing endemic and
R1 blocks (21 in the first cut, 2 more from the PR #7 review round below). Commands actually run, in this order:

```powershell
.\gradlew.bat --no-daemon clean build runGameTestServer   # fresh 123-test baseline, before any edit
.\gradlew.bat --no-daemon runGameTestServer               # iterating
.\gradlew.bat --no-daemon clean build
.\gradlew.bat --no-daemon runGameTestServer
.\gradlew.bat --no-daemon build runGameTestServer
git diff --check
```

### Repeats, and the one flake

The repetition targets the scheduling-sensitive new tests specifically: the 40-tick toad fuse under
a real goal tick, the thrown bomb flying and impacting on its own, and the wild flashbug's 11-tick
telegraph.

A first batch of seven `--rerun-tasks` repeats gave **6 passed, 1 failed**. The failure fired in the
same minute a `runServer` had been started **concurrently** with that loop — and `--rerun-tasks`
rewrites `build/classes` underneath an already-running server. That server run failed too, with
`Failed to load class com.carro1001.mhnw.MHNW` and
`NullPointerException: Cannot invoke "java.lang.Class.getName()" because "cls" is null`, which is a
half-written build directory, not a mod defect.

Both were then re-run **strictly serially, with nothing else touching the build directory**:

| Batch | Result |
|---|---|
| 7 repeats, concurrent with a `runServer` start | 6 passed, 1 failed (build-directory race, above) |
| 8 repeats, serial | **8/8 — all 144 passing every time** |
| final `clean build`, then `runGameTestServer`, then `build runGameTestServer` | all passing |
| after the PR #7 review fixes: `clean build runGameTestServer` | **146/146** |
| after the PR #7 review fixes: 6 repeats, serial | **6/6 — all 146 passing every time** |
| after the PR #7 follow-up fix: `clean build runGameTestServer` | **148/148** |
| after the PR #7 follow-up fix: 5 repeats, serial | **5/5 — all 148 passing every time** |
| after the corpse-presentation fix (death tint + grounded corpse parts): `build runGameTestServer` | **149/149** |
| after the presentation/feel round (flash-bomb fuse, bug box, spit model, JEI/Jade) | **150/150** |

The lesson worth keeping: **do not run `runServer` and `runGameTestServer --rerun-tasks` at the same
time on this project.** They share one `build/` and one `run/`, and the loser sees a half-written
class directory. That is a harness rule, not a code bug — but it looks exactly like a flaky test
until you line up the timestamps.

### JEI and Jade are client-run-only, and that is load-bearing

`build.gradle` puts both on `clientAdditionalRuntimeClasspath`, not `runtimeOnly`. A plain
`runtimeOnly` also loads them into `runGameTestServer`, where Jade's server ping throws
`Payload jade:server_ping_v1 may not be sent to the client!` at every mock player a test creates --
observed live as 11 failing R1/R2 tests that have nothing to do with Jade. Neither mod is declared
in `neoforge.mods.toml` and neither ships.

### Three fixture lessons worth not rediscovering

1. **`GameTestHelper.makeMockServerPlayerInLevel()` arrives creative.** Its anonymous subclass
   overrides `isCreative()` to true and its abilities start with `instabuild`. Vanilla's
   `ItemUtils.createFilledResult` deliberately keeps the input stack under infinite materials, so a
   survival bucket/bottle transaction tested with that fixture silently asserts the creative path.
   `setGameMode(SURVIVAL)` before using it; `r2CreativeBottleCaptureNeitherLosesNorDuplicates` is
   the one test that deliberately does not.
2. **`Bucketable.bucketMobPickup` casts to `ServerPlayer`** to award `FILLED_BUCKET`. A detached
   `makeMockPlayer` cannot catch a toad at all — it throws. Capture tests need a level-resident
   player; release tests do not.
3. **A mock player's held item is an inventory slot.** Counting the hand separately from
   `getInventory()` double-counts and makes an exactly-once assertion pass for the wrong reason, or
   fail for it. Two of these tests were wrong this way before the first run caught them.

### One production fix the tests forced

`BarbecueSpitItem.finishUsingItem` returned a fresh cooked meat even when handed an already-empty
stack. Not reachable in play — vanilla stops a use the moment its stack runs out — but it meant
"cannot double-complete" was only true by luck. Guarded.

### Gates: what is closed and what is not

| Gate | Status |
|---|---|
| R2-01 registries/data | closed headlessly — ids, entity type, both recipes and the exact variant mapping |
| R2-02 BBQ transaction | closed headlessly |
| R2-03 Flashbug capture | closed headlessly, survival and creative |
| R2-04 flash recipe/container | closed headlessly, through vanilla's own remaining-items path |
| R2-05 flash impact | closed headlessly — full eligibility matrix, a diagonal just-outside-radius case, plus a genuinely thrown bomb |
| R2-06 wild regression | closed headlessly |
| R2-07 toad capture mapping | closed headlessly, all four variants |
| R2-08 release/round trip | closed headlessly, including a bare `/give` stack and a save/load |
| R2-09 deployed effects | closed headlessly |
| R2-10 attribution | closed headlessly — provoked, unprovoked, non-damaging variants and mixed-source ordering |
| R2-11 regression | closed — all 123 pre-existing tests pass unchanged |

**Not closed, and not claimed:** every item in "What still needs a human — R2" below. No client was
available during this packet, so none of the appearance, feel, two-client or real-disk-restart
observations happened. A GameTest is not a substitute for any of them.

### What still needs a human — R2

- [ ] BBQ spit has a readable inventory/held model, the four-second hold reads as cooking rather
      than eating, and the cancelled hold visibly does nothing
- [ ] Bottled Flashbug (temporary vanilla experience-bottle sprite) and flash bomb (temporary
      vanilla firework-star sprite) are distinct and readable from each other and from existing items
- [ ] The thrown bomb renders throughout its flight and flashes once at impact
- [ ] Capture a naturally spawned or egg-spawned Flashbug with a real bottle, craft the bomb, throw
      it at hostile mobs; looking away and taking cover feel like understandable counterplay
- [ ] Capture and release all four toad variants: the filled icon and name match the creature, the
      empty bucket returns once, the released texture is unchanged, and it stays idle until hit
- [ ] In survival, deploy a Blastoad near a carvable monster, retreat during the warning, and
      confirm its damage counts toward that player's later carve eligibility
- [ ] Two clients: projectile, flash/effects, toad variant/fuse/removal and every inventory
      transaction agree for both observers, with no duplicate entities or items
- [ ] Save, restart and rejoin with filled buckets and released toads; verify variants, names and
      counts on the real disk

The still-open R0/R0b/R1/R1a human gates below are **not** closed by this packet either — natural
population, village interaction, the full loop, armor appearance, combat feel, two-client and
restart observations all remain exactly as open as they were.

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
synthetic one — and compares five controllers over all 26 animated bones. The baselines are **stock
GeckoLib controllers, not more adapters**: a stock controller advanced from age 0 is literally the
pre-R0b code path, so matching it is the claim actually being made. Observed `2026-09-12`,
`./gradlew runClient`:

```
[anim-selfcheck] clip=animation.great_izuchi.attack_scratch length=65.0 bones=26 joinAge=35 transition=5 expectedAnimTime=1.5
[anim-selfcheck] stock on-time   left_leg tick=0.0 start=-0.005904972458272415 end=-0.49561713343155017 animTime=1.5
[anim-selfcheck] adapter on-time left_leg tick=0.0 start=-0.005904972458272415 end=-0.49561713343155017 animTime=1.5
[anim-selfcheck] adapter late    left_leg tick=0.0 start=-0.005904972458272415 end=-0.49561713343155017 animTime=1.5
[anim-selfcheck] stock frame 0   left_leg tick=0.0 start=0.020517575069641837 end=0.15378863984192914
[anim-selfcheck] stock cold      left_leg no sample
[anim-selfcheck] PASS stock GeckoLib on-time baseline produced a pose
[anim-selfcheck] PASS that baseline is at the absolute expected sampler time (1.5s, i.e. clip tick 30)
[anim-selfcheck] PASS the pose at age 35 is distinguishable from the clip's first frame, so the comparisons below can tell a seek from a replay
[anim-selfcheck] PASS C09: the adapter leaves an ON-TIME observer exactly where stock GeckoLib put it
[anim-selfcheck] PASS late observer's FIRST frame produced a pose at all
[anim-selfcheck] PASS late observer's first frame matches the stock on-time pose exactly
[anim-selfcheck] PASS late observer reached the absolute expected sampler time, not merely the same time as a baseline that could have moved with it (1.5s)
[anim-selfcheck] PASS late observer did NOT replay the clip's first frame
[anim-selfcheck] PASS control: an unmodified GeckoLib 4.9.2 controller does NOT reach that pose cold, so the comparisons above are meaningful
```

The late observer's first frame is the **stock on-time pose** exactly, on every bone, at the
**absolute** expected sampler time of 1.5 s — which is clip tick 30, which is `35 - 5`. The adapter
leaves an on-time observer exactly where stock GeckoLib put it, which is the presentation half of
C09 measured rather than argued. The stock cold control produced **no sample at all**, which is the
cold-controller trap described above, observed rather than assumed.

**Why the baselines are stock.** The first version of this probe used the adapter as its own on-time
reference. A Codex review of PR #5 pointed out that a mutation shifting every observer *together*
would move the comparison and its baseline by the same amount and still pass. That was correct, and
it is now covered two ways: stock baselines, and an assertion on the absolute expected sampler time
rather than only on agreement between observers. The mutation table below includes the exact case
that review described.

### Mutation runs — these tests can actually fail

Every new check was confirmed to fail when the thing it guards is removed. Each was reverted
immediately after.

| Mutation | Result |
|---|---|
| Adapter returns after pass one (no seek) | self-checks FAIL; the late observer produces *no sample*, `animTime` 0.0 vs 1.5 |
| `controllerTickFor` returns raw action age **and** the adapter re-anchors every frame — the correlated shift the PR review described, which the earlier probe would have passed | self-check FAILs three ways: the adapter moves an **on-time** observer off the stock baseline, the late observer no longer matches it, and the absolute sampler time is 1.75 s instead of 1.5 s. `animationClockMapsActionAgeToClipTime` also fails |
| `controllerTickFor` returns raw action age | `animationClockMapsActionAgeToClipTime` fails: *"the clip should start exactly when the blend ends, not at 5.0"* |
| `RoarGoal.start` does not set the anchor | all three roar-anchor tests plus the distinct-instance test fail |
| Death anchor stamps `gameTime`, not `gameTime - deathTime` | reload test fails: *"the reloaded body restarted its death clip: age went from 10 back to 1"* |

### Gates: what is closed and what is not

| Gate | Status |
|---|---|
| C01 build / server isolation | **Passed.** 97/97 GameTests; a real dedicated server (`runServer`) reached `Done (0.251s)` with no class-loading failure; `R0b-02` constructs the adapter on a dedicated server so a stray client import fails the suite |
| C02 clock snapshots | **Passed**, headlessly. Every instance reconstructible from synced data, repeats distinguishable, no `tickCount` clock, reload cancels transient action while health and death progress survive |
| C03 actual sampler | **Passed** on a real client, above, against stock-GeckoLib baselines and an absolute expected sampler time |
| C04 encounter coverage | **Partial.** The mechanism is proven for scratch and is shared verbatim by all three Great Izuchi attacks, both Rathian bites and all three roars. Per-clip human observation is still open |
| C05 death lifecycle | **Passed** headlessly (anchors, precedence over a frozen attack, no extra death processing, no extended lifetime). Visual acceptance open |
| C06 render lifecycle | **Partial.** Resource reload and cull/retrack are handled by re-anchoring on deviation, and per-entity clocks make cross-contamination structurally impossible; **not** yet observed live |
| C07 two actual clients | **NOT MET — code complete, evidence pending.** See below |
| C08 real restart | **NOT MET — code complete, evidence pending.** See below |
| C09 no gameplay/asset regression | **Passed.** No attack window, damage value, measured path, hurtbox, model, animation file or biome file was touched; the full suite including every pre-existing R0a/R1a regression passes. The presentation half is now measured too: the self-check asserts the adapter leaves an on-time observer exactly where stock GeckoLib put it |

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

### PR #5 review round (same day)

A Codex review of `a5046f6` requested changes. All three findings were accepted and fixed; the first
was a real hole in the evidence rather than in the shipped behaviour.

1. **The sampler probe compared the adapter to itself** (P1). Its "on-time" and "frame zero"
   references were also `ServerTimedAnimationController`s, so a mutation shifting every observer
   together would have moved the comparison and its baseline in step and still passed. Fixed by
   making both baselines stock GeckoLib controllers — the literal pre-R0b code path — adding an
   assertion that an on-time observer under the adapter still lands exactly where stock GeckoLib put
   it, and asserting the **absolute** expected sampler time rather than only agreement. The exact
   mutation the review described now fails three checks; it is in the table above.
2. **The headless clock test exercised a parallel formula** (P2). `clipTimeFor` was only ever called
   by the test, while `seekTo` duplicated the same subtraction, so changing the production copy could
   have left every GameTest green. `clipTimeFor` is deleted; there is now one method,
   `controllerTickFor`, which runtime seeking calls and the GameTest asserts. Confirmed wired: a
   mutation to that method alone moved the **runtime** sampler from 1.5 s to 1.75 s.
3. **`AGENTS.md` was stale** (P2). It was a near-verbatim copy of `CLAUDE.md` and had already drifted
   before R0b — it still described an attack timeline for "Great Izuchi only" although Rathian's
   measured bite timeline shipped in P4, and had no R1a section at all. Rather than sync a third
   divergence by hand, it is now a short pointer to `CLAUDE.md`. One manual, no copies.
4. **That pointer then told Codex to sign as Claude** (P2, caught on the follow-up pass). The
   duplicate manual carried exactly one genuinely tool-specific line, the commit/PR attribution
   trailer, and collapsing it dropped the Codex variant. `CLAUDE.md` now lists both trailers
   verbatim. A first attempt also generalised them to the running model; that was scope creep on a
   maintainer policy and was reverted on the next pass.

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
actually attacks the player on sight. It now plants for the recovered `brain`-branch tail swipe:
the server owns its 48-tick action clock and only permits damage in ticks 36–47. The client draws
the exact same live-fitted attack volumes as green developer boxes when debug combat is enabled.
The maintainer supplied a video confirming the clip plays, and the 2026-09-13 `BoneProbe` capture
fitted the tail path. The static F3+B envelope is head plus two reduced tail parts after its
surplus tip part was removed; both envelopes still need a visual acceptance pass.

**As of the artist's 2026-09-13 redelivery this species has a death clip, a roar and a rally**, all
wired, so death is no longer vanilla's corpse flop -- the clip lays the body down itself and
`getDeathMaxRotation` is zeroed accordingly. `attack_tailslam` also arrived and runs as a real timed
action, but it is deliberately unarmed and gated behind `debugCombat` until its damage envelope gets
its own live capture; see `docs/DEFERRED.md`.

- [ ] Renders, spawns via egg, idles/walks/runs with correct animation
- [ ] Notices and attacks a nearby player, dealing real damage
- [ ] **New: the death clip plays and the body stays down** for the whole carve window, with no
      red tint and no vanilla flop fighting the clip
- [ ] **New: the opening roar** plays once per engagement and freezes the circling, and the
      **rally** plays on whichever one you hit, not on its packmates
- [ ] **New: hit one and the whole pack comes for you**, and they never hit or anger each other or
      the Great Izuchi they escort
- [ ] **Capture needed: the tail slam.** Turn on `debugCombat`, let one play, and record `tail2`
      and `tail_claw` through its active window so its damage envelope can be fitted
- [ ] **New: tail swipe plants before contact, and its green boxes follow the tail through the
      active phase** — headless timing/contact and one-hit-per-swing are GameTest-covered; needs a
      live `debugCombat`/F3+B pass to accept the measured path visually
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
