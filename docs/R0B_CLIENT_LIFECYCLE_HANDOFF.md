# R0b - Client animation and real-world lifecycle

**Status: ready to execute. Serial order: R0a -> R1a -> R0b -> R1b.**
Consumers: Claude Code/Opus or Codex/Sol.
This is a supplied handoff; it need not exist at a particular repository path.

Baseline inspected: `master` at **`f9d41aeb611964a2353fcb0f2b4eba326066478c`**,
the merge of R1a PR #4, 2026-09-12 UTC. R0a PR #3 is also merged.
Use the current accepted mainline/contribution branch and preserve newer work;
this SHA identifies the inspection, not a reset target.

## 1. Outcome and scope

A client entering an ongoing encounter should see its **current animation
phase**, not replay the beginning while the server is already landing a hit.
Correct that presentation defect without moving the server's accepted contact
windows or measured attack paths. Then establish real two-client and
save/stop/restart evidence for the existing entities.

| Presentation in scope | Existing consumers |
|---|---|
| Timed attacks | Great Izuchi scratch/tail swipe/tail slam; Rathian right/left bites |
| Opening roars | Great Izuchi, Rathian and Rathalos |
| Already-authored death presentation | Great Izuchi, Rathian, Rathalos and Aptonoth, while their current bodies remain present |

Preserve death priority for every species, including ordinary vanilla death
where no authored clip is used. Locomotion, Aptonoth's cosmetic grazing and
existing endemic effects remain unchanged; this is not a project to synchronize
every idle animation or redesign toad/flashbug telegraphs.

**Not included:** carving/corpse retention, rewards, new attacks, flight,
animation retargeting, new sounds/effects, habitat redesign, or a GeckoLib
upgrade/fork. Keep all existing creatures, assets, spawning and tools.
Do not fix the bounded delayed roar rearm during a committed attack merely
because it is nearby code; R0a explicitly documented that separate limitation.

The maintainer interview and delegated PM defaults already supply direction.
Make sensible equivalent implementation choices and record substantive local
overrides; do not conduct another workshop. R1b carving is the next handoff,
not work to start automatically inside this packet.

## 2. Current evidence and code map

The merged source contains **85 GameTests**, consistent with the newest
`TEST_PLAN.md`. PR #4's earlier 86 figure is superseded. Its recorded results
are not a fresh run by this handoff author.

R1a genuinely generated the habitat on all three prescribed seeds and observed
a mineshaft in it. **Live natural MH populations, a village inside the habitat,
and human visual acceptance remain open.** Its prior live observation lost the
client partway through and also observed no vanilla hostile population.
Do not turn those discarded observations into a spawn-bug diagnosis or a pass.
Carry them explicitly into the final release checklist.

Preserve Minecraft 1.21.1, NeoForge 21.1.248, GeckoLib **4.9.2**, TerraBlender
4.1.0.8, Java 21 target, ModDevGradle 2.0.146 and Gradle wrapper 9.2.1.
Both actual clients and the dedicated server need the same current mod build
and both required libraries.

Java paths below are under `src\main\java\com\carro1001\mhnw`.

| Read/change surface | Relevant current behavior |
|---|---|
| `entity\GreatIzuchi.java`, `Rathian.java` | Synced attack ID/start game time/sequence already exist. `mainAnim` resets on sequence change, then plays the clip from its beginning |
| `entity\RoarGoal.java`, `Roarable.java`, the three roar users | Server countdown is synced, but there is no stable presentation start timestamp for a roar instance |
| `entity\Aptonoth.java`, `Rathalos.java` and other death consumers | Authored death branch exists; it has no common late-tracking age contract |
| `client\MHNWClient.java`, `BoneProbe.java`, `AttackVolumeOverlay.java` | Existing renderer wiring and measurement/diagnostic surfaces; do not replace the renderer architecture |
| New common-safe `animation\ServerTimedAnimationController.java` and a minimal clock descriptor if useful | Suggested home for the small adapter; reuse an equivalent helper if newer code has one |
| `entity\AttackProfile.java`, both combat goals | Accepted server timing/damage/path contracts; read, do not retune to conceal a presentation bug |
| `entity\MonsterPart.java` and owning entities' ID/lifecycle methods | Parent owns health; parts are rebuilt and registered through native NeoForge behavior |
| `MHNWGameTests.java`, `MHNWCommands.java`, `MHNWConfig.java` | Existing regressions and debug toggle; use them rather than adding another framework |
| `docs\TEST_PLAN.md`, `docs\DEFERRED.md`, `docs\ANIMATION_MANIFEST.json` | Newest evidence, explicit remaining gates, authored clip inventory |

Read these focused surfaces, not the entire historical revival research or
every source model. No Drive asset export is required.

## 3. Clock contract: server facts, client presentation

### Attacks

Reuse the existing attack ID, sequence and start **game time**. A client entity's
local `tickCount` starts when it is created on that client, so it cannot define
the server action's age. Do not use day time or wall-clock time either.

A presentation instance is identified by the entity plus its action kind and
sequence/start identity. A repeated identical attack is a new instance.
Getting a fresh controller or reloading render resources is **not** a new
server action.

### Roars

Add the smallest stable synced presentation anchor needed at `RoarGoal.start`,
consumed by the three existing `Roarable` entities. A start game-time value can
identify the instance; a new generic combat state machine is unnecessary.
Keep the existing countdown, goal priority, rearm behavior and stop rules.
Do not rewrite the server clock to make a client timing equation convenient.

Define and test the age convention around the first start/decrement tick.
R0a observes 70 ticks remaining for the 71-tick Great Izuchi roar and 99 for the
100-tick wyvern roars. That scheduling detail must be accounted for, not hidden
by changing their durations.

### Death

Death presentation wins over any stale attack/roar fields. Normal death stops
goal ticking; do not expect an in-goal guard to clear those fields afterward.

Give the existing authored death users a server-owned presentation-age basis.
Prefer a synced start anchor initialized at actual death and reconstructed on
load from vanilla's saved death progress, rather than a second independently
ticking or separately persisted death state machine. Keep existing health,
removal, XP and part-ownership behavior.

**Do not extend body lifetime.** Great Izuchi currently has a 38-tick authored
death hold; the other listed classes retain their current vanilla lifecycle.
Their authored clip lengths and body lifetimes are not all equal. Synchronize
what is visible while the body exists; do not keep a body alive to finish a clip.
Reconstructing presentation on load must not call `die` or grant XP/loot again.
R1b will deliberately decide its own ten-minute corpse implementation later.

### Shared boundaries

- Initial tracking data must contain enough information to select the current
  instance and age. Do not rely only on a one-shot start event or tell clients
  to replay missed events.
- Finite actions stay transient across live-entity reload: health persists,
  but an interrupted attack/roar is cancelled as in R0a.
- Use partial ticks for interpolation once the authoritative snapshot is
  available. Handle a future/temporarily incomplete clock defensively; never
  silently treat every valid late snapshot as age zero.
- Keep one small per-entity presentation state, not static timing shared by a
  renderer, a global entity cache, or a new controller allocation every frame.
- Client animation and callbacks never apply damage, position gameplay parts,
  choose targets or alter server timing.

## 4. Verified GeckoLib 4.9.2 constraints

The exact published source archive was inspected; see section 8.
There is **no public `seek` or `setAnimationTick` setter** on its
`AnimationController`. Do not invent one or copy GeckoLib 5 examples.

| Verified API/behavior | Consequence for the implementation |
|---|---|
| Public `process(GeoModel<T>, AnimationState<T>, Map<String, GeoBone>, Map<String, BoneSnapshot>, double, boolean)` | A small controller adapter can participate in actual sampling without replacing the renderer |
| Protected `adjustTick(double)`, `tickOffset`, `shouldResetTick`, transition/queue state | There are local extension points; they are version-specific, not a universal seek API |
| `setAnimation` builds a queue only after `lastModel` exists, enters `TRANSITIONING` and requests a tick reset | Advancing a cold controller before normal initialization can leave no usable clip |
| `adjustTick` returns zero when resetting; `process` resets again when transition completes | Supplying a nonzero incoming time alone does not preserve the desired animation age |
| Transition queue initialization depends on zero adjusted time or the first-tick path | A naive override that always returns action age can skip queue loading on later actions |
| `processCurrentAnimation` samples keyframes and sets MoLang `query.anim_time` from the adjusted clip time | Seek the real sampler; printing the correct requested age is not sufficient |
| `AnimationState.animationTick` / model animation time are not simply the current clip position | Assigning them, `DataTickets.TICK`, or `AnimatableManager.startedAt` alone is not a demonstrated fix |
| `forceAnimationReset()` requests a reload; it does not perform a seek | Keep episode resets separate from correcting playback time |
| Speed is multiplied into elapsed time in `adjustTick` | A speed boost is not a seek, and speed zero can collapse sampled time to zero rather than freeze the current pose |
| Hold-on-last-frame mode changes state to `PAUSED`; expressions are still evaluated by the sampling path | Bound the sampled time for a held pose so MoLang does not continue evolving past its intended end |

Current transition lengths are **5 ticks** for Great Izuchi/Rathian/Rathalos
and **6** for Aptonoth. The initial blend and the subsequent clip are distinct
timelines. Setting every transition to zero, or feeding raw server age directly
as clip time, can shift an already-measured attack even if late entry improves.

The manifest rounds durations to game ticks. For an exact terminal sample,
use the loaded animation's actual length, not an assumed integer rounding.
All scoped current attack/roar/death assets were inspected and have no sound,
particle or custom-instruction marker tracks. Do not build a generic marker
replay engine. If newer code adds callbacks, seeking must not fire a backlog
of historical effects, and none may become gameplay authority.

### Preferred bounded approach

Use a small version-pinned controller adapter plus the minimum shared clock/
selection helper. Keep it common-loadable if common entity registration names
it: no `net.minecraft.client` imports, static `Minecraft` access or reference
to `MHNWClient` from common initialization. Actual renderer probes remain
client-only. A dedicated-server startup is required to prove the boundary.

Let GeckoLib perform normal clip/queue initialization before advancing a late
instance. For example, an internal initialization pass can prepare the queued
clip without displaying its zero-time output, followed by the real sample in
the same render call. Do not blindly call `super.process` twice every frame:
initialization, new-instance reset, transition completion and resource reload
need explicit handling. A supported equivalent approach is fine.

Select the finite presentation and its clock from the **same priority decision**
(death, then roar, then attack). Avoid separate selectors that can choose a
death clip with an attack clock. Delegate ordinary idle/locomotion to the
existing behavior.

This is researched guidance, **not a prevalidated adapter implementation**.
First prove one Great Izuchi attack end to end, then extend the proven helper.
Do not solve a difficult initialization edge by copying the whole GeckoLib
processor, reflecting into private fields, forking the library or evaluating
the skeleton/MoLang independently.

## 5. Implementation sequence

### A. Establish a reference before changing playback

Record HEAD, dirty state and the relevant current automated result. Reuse a
fresh result for the exact code; the historical 85 count is not a new run.

Take a bounded client-side reference for an already-tracked Great Izuchi attack
and Rathian bite: action identity, server-derived age, controller/clip state
and one or more useful probed limb positions. Retain the existing timings,
model scale and measured attack paths. A client-capable comparison harness
using the actual GeckoLib sampler is also valid; a pure clock mock is not.

Use the existing `debugCombat`/`BoneProbe` surfaces. Enable debug settings
separately on the dedicated server and each client: the server's command does
not automatically change another process's COMMON config.

Diagnostics should identify entity UUID/instance, action kind/sequence/start,
authoritative age, actual sampled clip time/state and the observed bone pose.
Log on transitions/initial or resumed rendering with bounded capture windows,
not permanent per-frame/per-bone spam. Do not report only the age you intended
to pass to the sampler.

### B. Prove the Great Izuchi vertical slice

Implement the clock-to-playback adapter for its current attacks first.
Exercise cold late entry, ordinary on-time entry, a repeated identical attack,
and returning to idle. The **first rendered result after a complete late
snapshot is available** should be in the current phase, not a fresh intro.

Calibrate the relationship between authoritative action age, existing transition
time and clip time against the reference. Do not guess that subtracting five
ticks is sufficient for every initialization path. Preserve normal observer
contact timing as well as fixing the late observer.

Initial local/dedicated test target: **at most two game ticks of phase difference**
between equivalent on-time and late observers after both hold the same current
snapshot, with no full windup replay. This is not a zero-latency Internet
guarantee. Record any clock/latency limitation instead of widening tolerance
until an obvious replay passes.

Prove actual bone sampling and MoLang phase, not merely an arithmetic helper
or a logged request. If queue/reset behavior still prevents a correct sample,
fix this slice before changing all species. Do not retune attack windows,
damage or hitbox paths to hide a presentation regression.

### C. Extend only the proven behavior

Apply it to all three Great Izuchi attacks, both Rathian bites, the three
opening roars and the four existing authored death users.

Cover transition boundaries, end/hold behavior, repeated instances, rendering
after a long cull, resource reload and two same-species entities with different
action ages. An expired one-shot must not loop back to frame zero; a held pose
must not keep moving because its MoLang time is unbounded.

Keep controller registration and renderer layout otherwise intact. Do not add
new attack presentation to Rathalos or new death assets to small Izuchi or
Lagiacrus. Do not serialize controller caches, bone snapshots or live attacks.

### D. Establish real client/server and disk evidence

Use one named scratch world, the same current build and required libraries on
all processes, and **two distinct connected player identities**. Two windows
that replace/kick the same login are not two clients. Use normal authorized
test access; do not weaken a shared/public server's authentication as a shortcut.
Seed **0**, near **(384, 65, -320)**, is an already-recorded habitat location
for the current standalone setup; verify the biome rather than redoing the
whole three-seed search or assuming those coordinates fit a changed modpack.

Keep server/client processes alive for the entire observation window using the
CLI's appropriate long-lived process support. Record their actual connection
state at the start and end; the prior R1a quick-play session disconnected
partway through. Stop only test processes you own, by their specific IDs.

Use an already-connected observer B outside tracking range and observer A
keeping the encounter loaded. Move B in during a known action. This is more
repeatable than hoping a full login finishes within a short bite:

1. Observe windup, active and recovery entry for Great Izuchi/Rathian.
2. Enter during each opening roar; observe a later distinct roar instance.
3. Separately test **frustum culling** (look away while still tracking) and
   **true untrack/retrack** (new client entity/controller). They are not the
   same condition.
4. Kill a subject during an action; both clients see current death precedence.
   A late observer sees the current death phase while the body exists, and
   neither sees ghost parts or a replay after removal.
5. Exercise resource reload and multiple same-species mobs without stale
   animation or cross-entity timing contamination.
6. Compare one controlled parent/part hit, health and removal on server and
   both clients; no damage or gameplay-box behavior may depend on the camera.

Then deliberately injure and identify a few persistent test subjects, record
UUIDs/health/variants/part names, save/flush, **gracefully stop the server
process**, restart the same world, and rejoin. Confirm the same entities and
data, fresh part registration and cancelled transient combat. New runtime
numeric entity IDs are expected; duplicate parents, extra escort generation
or a resumed old attack are not.

Use supported save/load tests for death-progress reconstruction as well; do not
create a ten-minute corpse or alter production despawn rules merely to make a
short death easier to inspect. Do not call death/loot processing a second time
when restoring presentation. This packet does not promise abrupt-crash
transactionality or the future carve-counter contract.

## 6. Acceptance and honest stop conditions

| Gate | Required evidence |
|---|---|
| **C01 Build and server isolation** | Current full suite/build passes; dedicated server starts with no client-only class-loading failure. Pins/dependencies, worldgen and registered roster remain intact |
| **C02 Clock snapshots** | Late tracking can reconstruct each scoped instance; repeated actions/roars are distinguishable; no local entity tick-count clock. Fresh live reload cancels transient attack/roar, while health and necessary death progress survive |
| **C03 Actual sampler** | One-attack proof uses real GeckoLib initialization, transition and keyframe/MoLang sampling. Correct computed age with a restarted/empty clip is a failure |
| **C04 Encounter coverage** | All three Great Izuchi attacks, both Rathian bites and all three roars pass cold/on-time/late/end/repeat cases; normal measured contact is not shifted to make seeking look correct |
| **C05 Death lifecycle** | Existing authored death users take precedence over stale combat, use current age, and keep current removal behavior. Held terminal sampling stays stable; no extra damage, XP, removal or revived state |
| **C06 Render lifecycle** | True retracking, ordinary culling, resource reload and multiple same-species entities cannot replay old instances or share the wrong clock; idle/locomotion resume normally |
| **C07 Two actual clients** | Distinct clients remain connected and agree with server health, parts, action/death state; the phase target is measured after a complete shared snapshot. A one-client screenshot or fake-player server test is not this gate |
| **C08 Real restart** | Recorded entities/health/variants/parts survive a real save/stop/start/rejoin. No duplicate escorts or resumed old combat; in-memory NBT tests alone do not close it |
| **C09 No gameplay/asset regression** | Server attack windows, damage, measured paths, movement, hurtboxes, model/animation files and endemic behavior are unchanged except any narrowly demonstrated lifecycle defect |

Use existing GameTests for server state, clock math, save/load and native part
contracts. Client sampler/pose behavior needs a client-capable probe or actual
client observation; do not fabricate a headless visual pass. Add only tests
that can fail when the intended regression returns.

**R1a carryover:** while real clients are available, make one bounded natural-
population observation in the known habitat and record connection eligibility,
chunks, time/light and counts. Controlled summoned combat subjects do not
prove natural spawning. If both vanilla and MH mobs are absent, first check
the observation setup. A village inside the habitat and biome visual acceptance
also remain named carryovers until actually observed. Do not spend this packet
regenerating every seed, adjusting biome weight on a hunch, or marking these
requirements satisfied merely because PR #4 merged.

R0b is fully accepted only with the required client/restart evidence. If access
is unavailable, deliver **code complete / named evidence pending**, with the
exact missing observation and how to run it. Do not burn quota waiting, invent
an approval document, or report the whole stage passed. Carry any remaining
release gate explicitly into the next handover.

R1b is next in the serial queue and must verify its own new personal rewards,
counters and expiry. This earlier restart evidence cannot validate persistence
code that has not yet been written.

## 7. Delivery and kickoff

Expected changes: a small shared clock/controller adapter, the scoped entity/
roar presentation hooks, bounded client diagnostics, focused tests, and the
relevant existing status docs. Keep runtime implementation in the code repo;
do not patch the downloaded dependency source.

Use one main agent, a proved vertical slice and small follow-on changes. Do not
delegate each entity before the adapter works. Use the installed working
toolchain and existing final command:

```powershell
.\gradlew.bat --no-daemon build runGameTestServer
git --no-pager diff --check
```

Record actual build/revision identities across the processes, not only a mod
version string that several development builds share. Keep credentials, RCON
settings and personal test worlds out of commits. Preserve user saves and stop
only test processes you own.

Update `TEST_PLAN.md`/`DEFERRED.md`: implementation, exact observed results,
calibrated clock-to-clip convention, local overrides and named remaining
evidence. No new report bureaucracy. Stop after R0b; do not start carving,
commit, push or publish without the user's current authorization.

```text
Implement the supplied R0B_CLIENT_LIFECYCLE_HANDOFF.md for MonsterHunter-NewWorld.
Use the accepted post-R1a mainline; PR #4 merged as f9d41ae. Inspect HEAD/local
changes and preserve newer work. The inspection SHA is not a reset target.

The handoff may be attached or supplied as a local file, not a checked-in path.
Use its scope and defaults without another interview. Make sensible equivalent
implementation choices and record substantive deviations.

Deliver only R0b: correctly aged existing attack/roar/death presentation for
late observers, plus real client/server and lifecycle evidence. Keep server
damage, measured paths, timings, geometry, assets and the R1a habitat intact.
No carving, new attacks, GeckoLib upgrade/fork or general animation engine.

Read the pinned GeckoLib 4.9.2 constraints in the handoff. There is no public
seek setter. Prove one Great Izuchi attack through the real initialization/
transition/sampling path before extending the adapter. Do not shift established
contact timing or log a requested age and call that a successful seek.

Use the existing validation/debug surfaces and one main agent. Distinguish true
retracking from culling, and verify two distinct clients stay connected. Record
real save/stop/start/rejoin results; do not substitute an in-memory round trip.
Carry R1a's unobserved population/village/visual checks honestly.

If human/client access is missing, deliver code complete with precise pending
gates rather than waiting indefinitely or claiming a pass. Report the actual
revision, changes, evidence, overrides and remaining observations.
Stop after R0b. Do not commit, push or publish unless asked.
```

## 8. Reference evidence

- [Accepted mainline after R1a](https://github.com/Carro1001/MonsterHunter-NewWorld/tree/f9d41aeb611964a2353fcb0f2b4eba326066478c)
  and [PR #4](https://github.com/Carro1001/MonsterHunter-NewWorld/pull/4).
  Current source/newest dated notes say 85 tests; earlier PR-body counts differ.
- [Exact GeckoLib 4.9.2 source archive](https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/software/bernie/geckolib/geckolib-neoforge-1.21.1/4.9.2/geckolib-neoforge-1.21.1-4.9.2-sources.jar).
  SHA-256: `009055c5d7b848a8bed826ed8887936d85c5f711d0c19f0fa2547950db99ee41`.
  Read `animation\AnimationController.java`, `AnimationProcessor.java`,
  `Animation.java`, `AnimatableManager.java` and `model\GeoModel.java` for the
  verified constraints above; do not substitute a newer major version's API.
- Current MHNW `mainAnim` methods, `RoarGoal`, renderer hooks and
  `ANIMATION_MANIFEST.json` establish the consumers and transition/clip facts.
  Their server timings are the input to this task, not values to redesign.

This document specifies researched implementation and acceptance work.
It does not claim a controller adapter or two-client fix has already been
implemented or validated in this planning session.
