# R0a - Preserve and lock the working baseline

**Status: ready to execute. Scope: R0a only.**
Roadmap: [v4 working direction](../ROADMAP.md).
Inspected baseline: `revival/neoforge-1.21.1` at
`3ca5d46d11a60323170cbd2e7777879af3c421c1`, 2026-09-11.
Consumers: Claude Code with Opus or Codex with Sol.

## 1. Outcome and scope

Deliver a small, reviewable baseline-hardening change: protect the working roar,
death and reload behavior with precise regressions; fix demonstrated in-scope
defects; correct misleading handoff/status/license metadata.
**Most of the expected diff is tests and documentation, not new production AI.**

The revival already works. Preserve its entire implemented roster, current
endemic effects, measured geometry/attack paths, assets, debug tools and spawn
eggs. Do not rebuild monsters, disable toads/flashbugs, replace native parts,
refit models, change balance or begin the custom biome/carving implementation.

The product interview is complete enough to work from. Roadmap v4 supplies
sensible PM defaults; the maintainer/dev can override details locally with a
short explanation. Do not launch another approval interview or treat the old
response's DRAFT header as a stop condition.

**Why this packet first:** recent roar bugs passed the existing broad tests.
The code is now fixed, but its timing needs a regression that would actually
catch the old bug. Upcoming ten-minute corpses also make it important to know
what vanilla death really stops, rather than changing code to satisfy a false
assumption about goal cleanup.

**Not this packet:** R0b owns late-tracking animation correction and real
two-client/save-restart acceptance. R0b remains required before first release,
but lack of a human test session must not block R0a or subsequent R1a work.

## 2. Preflight: one bounded read, not a repository re-audit

Work on the current revival checkout or a contribution branch based on it.
The planning branch containing this document may still contain the obsolete
Forge runtime. If these Java paths/platform pins are absent, use the existing
revival checkout through the contributor's normal workflow; **do not port the
old checkout to make this packet fit**.

Record HEAD and dirty state. Preserve local changes and newer commits. If HEAD
is newer than the pinned baseline, inspect that delta first and reuse completed
work. Do not reset/cherry-pick backwards to the pinned commit or overwrite
someone's live work. A clean fresh commit containing the same tests means the
task may already be partly done.

| Read | Focus |
|---|---|
| `CLAUDE.md`, `AGENTS.md` | Local operating rules; distinguish stale factual statements from instructions |
| `docs\TEST_PLAN.md`, relevant `docs\DEFERRED.md` sections | Latest observed results and current acceptance gaps, not every historical experiment |
| `src\main\java\com\carro1001\mhnw\MHNWGameTests.java` | The specific existing tests named below and nearby helpers |
| `src\main\java\com\carro1001\mhnw\entity\RoarGoal.java`, `Roarable.java` | Real-tick countdown, rearm clock, goal scheduling, death |
| `src\main\java\com\carro1001\mhnw\entity\GreatIzuchi.java`, `Rathian.java` | Synced state, fresh-instance defaults, normal death hold and presentation precedence |
| `src\main\java\com\carro1001\mhnw\entity\Rathalos.java` | Third real roar user; read only what the shared regression needs |
| `src\main\java\com\carro1001\mhnw\entity\MonsterPart.java` | Damage forwards to parent; part lookup/removal is NeoForge-owned |
| `build.gradle`, `gradle.properties` | Existing wrapper/run tasks, exact versions and metadata input |

Do not read the entire 70 KB historical revival plan, re-audit Drive exports,
inspect every unimplemented species or investigate GeckoLib seeking in R0a.
The archive is background, not the work queue. Current relevant code and this
packet supply the scope.

## 3. Facts to preserve

| Current fact | Consequence |
|---|---|
| Minecraft 1.21.1; NeoForge 21.1.248; GeckoLib 4.9.2; Java 21 target; ModDevGradle 2.0.146; Gradle wrapper 9.2.1 | No version/dependency changes |
| `RoarGoal.requiresUpdateEveryTick()` already returns true | Do not reimplement the fix; prove it stays effective through normal AI ticking |
| Rearming uses elapsed `level().getGameTime()`, with a 100-tick disengagement interval | A test that merely eventually sees rearming can miss a doubled interval |
| Great Izuchi, Rathian and Rathalos all implement `Roarable` | All three should exercise the real roar-duration path; a tiny test helper is reasonable |
| Dead mobs normally stop ticking their goals | An `isAlive()` guard inside `Goal.tick()` is not guaranteed to run after death |
| A dead monster may retain a nonzero synced attack ID or roar countdown | Assert inert behavior and eventual removal, not fictitious automatic clearing of unused fields |
| `mainAnim` checks death before roar/attack | Do not reorder it to appease a state-field assertion |
| Great Izuchi/Rathian use synced action ID, start game time and sequence | Keep their server-owned damage timeline and measured attack data unchanged |
| Parts are not independently saved living mobs | Check parent ownership and NeoForge lookup membership, not only `part.isRemoved()` |
| Great Izuchi already has a forest biome spawn entry and natural-spawn config | Preserve them in R0a; R1a will replace the habitat entry, not leave an interim spawn vacuum |
| Toads release current effects on provocation and disappear; flashbugs flash eligible non-player targets and disappear | Retain existing behavior and tests without gating or redesign |
| Lagiacrus's bank-exit test proves bounded failure cleanup, not successful climbing | Do not "fix" this unrelated movement limitation in this packet |

Keep measured hurtboxes static/server-owned and attack volumes separate.
No renderer positions, model pivots or camera visibility may become gameplay
authority. Do not copy a Drive snapshot over working exports.

## 4. Implementation sequence

### Step A - Establish the automated baseline

Use the existing wrapper and installed working JDK configuration:

```powershell
.\gradlew.bat --no-daemon runGameTestServer
```

Java compilation targets 21; use a locally installed Gradle-compatible JDK.
Do not hardcode another contributor's JDK directory, install global tools or
change the wrapper simply because the shell's `JAVA_HOME` is wrong.
If the exact current code already passed in this session, reuse that result.

The inspected branch reports 69 tests. Treat that as a reference, not an
assertion that today's count must remain 69. Verify that real tests ran and that
the task exits successfully; an empty/no-tests run is not success.
Record baseline failures before editing. Repair an in-scope defect; do not
quietly weaken assertions or expand into an unrelated species rewrite.

### Step B - Add or strengthen the missing regression evidence

Use `MHNWGameTests`, the existing `arena` template, normal entity spawning and
the repository's `GameTestHelper` sequence conventions. Reuse existing helpers.
Keep test subjects' AI enabled when testing goal scheduling; making the victim
inert is fine. **Calling `RoarGoal.tick()` directly cannot catch vanilla's
half-rate scheduling bug.**

The following is an evidence matrix, not a demand for one method per row or a
particular total test count. Keep helpers small and test-only.

| Evidence | Required assertion | Existing starting point |
|---|---|---|
| **T01 - Real roar duration, all three species** | Observe the first positive countdown under normal AI. Record actual game time and remaining ticks once. Observe progression and require completion near that real-tick deadline, with at most a small explained scheduling tolerance (start with two ticks). No attack while a live subject is roaring. A half-rate countdown must fail | `greatIzuchiRoarsOnFirstEngagement`; `Roarable.roarDurationTicks()` and `getRoarTicks()` |
| **T02 - Real rearm interval, Great Izuchi** | A completed engagement does not rearm early; rearming occurs within 100 ticks plus up to four ticks of normal polling tolerance after sustained target loss. A brief loss/reacquisition does not rearm and restarts the sustained-loss window | `greatIzuchiReArmsRoarAfterARealDisengage` currently proves only eventual rearming |
| **T03 - Death during roar and attack** | For Great Izuchi and Rathian, reach a live roar/attack via ordinary AI, kill the subject, then observe the former victim through the remaining death hold. No further outgoing melee damage. Parent is eventually removed and parts leave NeoForge lookup. Do not require attack ID/countdown to self-clear | Existing death tests plus the newly documented `RoarGoal`/combat-goal death behavior |
| **T04 - Meaningful reload, Great Izuchi and Rathian** | Start with deliberately reduced, non-default health and an active action/roar; serialize through supported entity save/load behavior into a fresh instance. Health survives, transient action/roar does not resume, initial cooldown remains positive, and owned part count/names remain correct | `reloadCancelsTransientCombatState`; Rathian's constructor/default synced state |
| **T05 - Real part unregistration** | After existing parent-removal scenarios, each retained part reference is absent from `helper.getLevel().getPartEntities()` | `assertLagiacrusPartsUnregistered` already demonstrates this API; strengthen equivalent Great Izuchi/Aptonoth/Rathian/Rathalos assertions where they only inspect flags |

For T01/T02, retain observations across ticks. A `succeedWhen` predicate that
becomes true once early in the action does not establish the later deadline.
Anchor the deadline to the **observed start/countdown**, not an assumed global
test tick. Avoid timing tests whose broad timeout would also pass the old bug.
Keep the target alive/in range so the test measures the intended transition.
Rathalos uses vanilla melee, not a synced attack-ID API: observe the victim's
health during its roar rather than inventing `Rathalos.getAttackId()`.

For T03, capture victim health **after the lethal action** and exclude unrelated
damage from the fixture. The subject may hold its death pose for its existing
species-specific duration; do not remove it prematurely. A test can assert
server inertness without claiming it inspected client animation.

For T04, prefer the supported full entity save/load path after confirming its
1.21.1 signature. The current Great Izuchi test only round-trips additional
save data and checks idle/cooldown/part count; it does not prove every vanilla
field or a disk restart. Use a fresh instance, not a load onto the active
original. If the clone enters the level, avoid duplicate UUIDs/part identities
and dispose of the original through normal lifecycle. No persistence of attack
age or roar engagement is required by this packet.

For T05, native parts can be unregistered without their own `isRemoved` flag
being set. Use the actual lookup assertion already present for Lagiacrus.
Do not create separately registered/spawned part entities as a workaround.

New tests must prove their setup was reached before succeeding. Empty part
arrays, a subject that never attacked, a victim that already died, or a test
subject with AI disabled cannot establish these contracts.

### Step C - Repair only what the evidence demonstrates

If all assertions pass, **leave production behavior alone**. Added regression
coverage is a valid delivery; there is no requirement to manufacture a code fix.

If one fails, distinguish a bad fixture/incorrect expectation from a real
defect. Fix the smallest owner of the demonstrated behavior and keep a failing-
before/passing-after regression. Preserve existing damage, timings, geometry,
death presentation and all unrelated features.

Do not force dead synced state to zero merely because an early test guessed it
would clear, add a new state machine, generalize both combat goals, or bolt on a
corpse system here. R1b will deliberately change corpse retention; its later
tests will replace the applicable normal-removal expectation while preserving
inertness and once-only ownership.

### Step D - Make the next handoff truthful

Update the relevant current summaries in `CLAUDE.md`, `AGENTS.md`,
`docs\TEST_PLAN.md` and `docs\DEFERRED.md`. Preserve useful dated history; label
superseded claims instead of rewriting history as though it never happened.

- The revival branch is pushed; "local-only, never pushed" is stale status.
  Replace it with the actual state and **no further push without authorization**.
- Lagiacrus exists as a limited movement baseline; it is not simply unported.
- Rathian has a real measured/mirrored bite timeline and opening roar, not
  vanilla-melee-only combat.
- Small Izuchi's lack of dedicated attack/death clips is an accepted first-
  release presentation limitation; retargeting is not a prerequisite for R1.
- Existing toad/flashbug mechanics are retained. R2 extends them.
- Record exact evidence from this delivery; do not relabel every open visual
  or real-restart checkbox as passed.
- Point new feature work to the working roadmap and scoped packets, not a
  mandatory reread/restart of historical P0-P8.
- Prefer targeted checks while iterating, then one full build/GameTest pass
  for the coherent delivery. Prose-only edits need no game rebuild.

Align `gradle.properties`'s `mod_license` with the maintainer's stated
`GPL-3.0` choice. The current value is `All Rights Reserved` and is expanded
into `src\main\templates\META-INF\neoforge.mods.toml`.
Do not hand-edit generated metadata, replace the root license, remove credits
or claim that this resolves third-party branding rights.

### Step E - Validate and hand over

```powershell
.\gradlew.bat --no-daemon build runGameTestServer
git --no-pager diff --check
```

Inspect the built jar's `META-INF\neoforge.mods.toml` to confirm the intended
license value actually reached the artifact. Keep existing mod/loader versions
and dependencies. If the environment prevents a command, report the exact
failure and remedy attempted; do not substitute a historical pass.

Do not rerun an unchanged green suite after prose-only edits. No new test
framework, CI project, source exporter, live server or dependency is necessary
for this packet.

## 5. Ownership and exclusions

**Expected edited paths:** `MHNWGameTests.java`, `gradle.properties`, and the
four current handoff/status documents named above. A small test-only helper in
the existing test class is fine.

**Conditional production edits:** the relevant existing entity/goal/part owner
only if a new accurate test demonstrates a real defect. Explain that link.
Do not rearrange registration/render/test files for hypothetical concurrency.

**Preserve unchanged unless the maintainer explicitly redirects:** assets and
generated animation manifest; attack profiles and measured paths; loader/
library versions; current natural-spawn data; all existing species, eggs and
endemic behavior; armor/crafting/worldgen feature scope.

Leave R0b's animation seeking and dedicated-server/client work for its own
packet. Do not spend this packet reverse-engineering GeckoLib or claiming that
synced attack IDs prove correct late-client playback.

## 6. Completion, pending gates and quota discipline

R0a is code-complete when T01-T05 have the required evidence, in-scope defects
are fixed, the full suite/build pass, and the handoff/docs reflect reality.
Counts may differ as tests are strengthened or parameterized; report the
actual tests, not just "added N tests."

Record these separately as **R0b/pre-release pending**, unless real newer
evidence already closes them:

- Correctly aged presentation for a client that starts tracking mid-action.
- Two actual clients agreeing on damage, health, parts and death.
- A real save/stop/restart/rejoin cycle, including part reconstruction.
- Fresh human confirmation of roar/death playback if it has not occurred.

These pending gates do not prevent moving next to R1a. They do prevent
calling the whole first release verified. A live session can be scheduled by
the maintainer later; do not wait in an agent loop for one.

Use one main agent. Subagents are optional for a genuinely independent bounded
task, not one per file or test. Do not pre-write the rest of the roadmap,
re-interview the maintainer, repeatedly poll unchanged results or keep a goal
running after R0a is complete.

Leave a short handover in the existing dated test/status documentation:
start/result revision, implemented changes, actual command results, local
overrides if any, and the named remaining gates. Follow the contributor's
normal local commit workflow; do not assume permission to push/publish from
the fact the branch already exists remotely.

## 7. Copyable kickoff

```text
Implement docs\plans\R0_BASELINE_HANDOFF.md, R0a only, on the current
revival/neoforge-1.21.1 code or a contribution branch based on it.

Read this packet and only the relevant current code/status sections it names.
The maintainer interview is the direction; ROADMAP.md v4's PM defaults are
usable now and locally overridable. Do not conduct another approval interview.
Keep all working creatures, endemic effects, models, measured combat and tools.

First reconcile HEAD with the inspected 3ca5d46 baseline; preserve newer work.
Add/strengthen the specified real-tick roar, inert-death, reload and actual
part-lookup regressions. Fix only demonstrated in-scope defects. Dead goals
normally stop ticking, so do not require unused dead action fields to clear.
Update the stale current docs and packaged license metadata.

Use one main agent; delegate only when it actually saves quota. Use the existing
Gradle/GameTest workflow. Report actual evidence and distinguish R0a completion
from pending R0b client/restart acceptance. Lack of a human session does not
block R0a or later R1a work. No worldgen, carving, art replacement, GeckoLib
seeking, broad refactor, dependency changes or other roadmap stages here.
Stop after this packet. Do not push or publish without separate authorization.
```
