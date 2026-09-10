# Monster Hunter: New World - revival handoff

## 1. Purpose, authority, and scope

This is an implementation handoff, not a request to implement everything in one
agent turn. It is intended for an orchestrating coding model, including Opus 5 or
Sol, working with bounded subagents. Follow the dependency order and exit gates;
do not substitute plausible-looking code for demonstrated in-game behavior.

Repository: `Carro1001/MonsterHunter-NewWorld`.
Local audit baseline: `8eec41d7845c80d5944f9575806fa58a570de2a4`, on `master`,
last local commit dated 2025-04-02. Research session date: 2026-09-09.
Local findings below are static code/resource observations, not claims that the
old mod was successfully built or played during this audit.

**User-confirmed decisions**

- This was not a completed/released mod with an existing audience. Target fresh
  worlds; old-save compatibility is not required.
- Preserve the models and their associated creative work. Existing implementation
  code may be replaced where that is the cleaner solution.
- The goal is creatures in the world with appropriate hitboxes, health, and
  dependable behavior. Monster Hunter flavor is desirable; exact balance and
  combat fidelity can follow.
- Apply KISS ("keep it simple") and YAGNI ("you aren't gonna need it") to both
  implementation and project management.
- Prefer an established mod/modpack compatibility cluster over the newest game
  release. Neither Forge nor GeckoLib is a hard requirement; choose them only
  where they improve the new project's development or compatibility story.

**Recommendation: preserve the content, replace the unreliable runtime seams,
and rebuild incrementally.** This is neither a blind version-number port nor a
reason to discard the repository. Keep its history, art, animation work, useful
species-specific ideas, and credits. Start a clean runtime from a supported MDK,
prove one creature end to end, then bring over the rest in small batches.

**Selected default: Minecraft 1.21.1 + NeoForge 21.1.248 + Java 21 +
GeckoLib 4.9.2.** Use ModDevGradle 2.0.146 and its Gradle 9.2.1 wrapper.
This targets an established, actively maintained modpack/content intersection,
not the newest Minecraft release. The loader pin deliberately matches the
released Enigmatica 10 compatibility reference rather than needlessly requiring
the newer 21.1.250 development candidate. Published requirements match; P0/P1
still have to establish a working mod.

For a quick decision read, see **section 7.7** on modding-version clusters and
**section 6** on code salvage. Implement through **section 5**, using the
invariants in **section 4**. The copyable kickoff is in **section 8**.

The first playable milestone is not the final requested outcome. Do not call the
revival complete while the other existing creatures remain statues, lack health,
or only appear through developer commands.

## 2. Preservation inventory and asset readiness

Paths in this section are relative to the repository root.

| Material | Audited contents | Treatment |
|---|---|---|
| `src\main\resources\assets\mhnw\geo` | 16 geometry JSON files: 12 living-creature geometries, two severed tails, and two different armor geometries | Preserve original exports and exact bone names before any conversion |
| `src\main\resources\assets\mhnw\animations` | 14 animation JSON files containing 100 named entries; not all entries contain animation | Preserve; classify usable, mismatched, empty, and unused separately |
| `src\main\resources\assets\mhnw\textures` | 79 PNG textures; the 80th resource PNG is `src\main\resources\logo.png` | Preserve texture variants, UV dimensions, transparency, and credit |
| `src\main\resources\data\mhnw\multihitboxlib\hitbox_profiles` | Four profiles: Great Izuchi, Lagiacrus, Rathalos, Rathian | Preserve as authoring/reference data, not automatically as the new runtime format |
| `src\main\java\com\carro1001\mhnw\client\models\entities\BugModel.java` | Java-authored bitterbug/godbug mesh and procedural movement | This is also a model asset; a resources-only copy would lose it |
| `src\generated\resources` | 112 generated JSON files, including translations, item models, recipes, and damage tags | Preserve history; regenerate only content actually being restored |
| Authoring/sound sources | No tracked `.bbmodel`, `.blend`, `.psd`, `.aseprite`, `.ogg`, or `.wav` files found | Ask contributors for editable sources and provenance; do not promise nonexistent sound/animation assets |

The geometry exports use `format_version: 1.12.0`; the animation exports use
`format_version: 1.8.0`. These are asset-format versions, not Minecraft target
versions. Do not change them just because the mod moves to another game release.
Animation content includes MoLang expressions and different loop modes, not just
numeric keyframes.

### 2.1 Living-creature roster

There are 12 exported living-creature geometries plus the Java bug model.
Endemic-life variants reuse a model; tails and armor are not additional monsters.

| Creature/family | Animation readiness in this snapshot | Minimum eventual behavior direction |
|---|---|---|
| Aptonoth | Seven clips: idle, walk, run, eat, attack, death, dead | Passive herbivore, roam, flee or defend when provoked |
| Great Izuchi | Eleven entries including pose, locomotion, sleep, roar, rally, three attacks, death; six profile parts | First combat vertical slice; ground pursuit and telegraphed attacks |
| Izuchi | Four clips: idle, sleep, walk, run; no attack/death clip | Small ground monster; simple independent behavior before pack coordination |
| Rathian | Thirty entries, including ground/flight/attack clips and demonstration clips; seven profile parts | Ground-oriented wyvern, charge/fireball/tail attack; bounded flight later in its packet |
| Rathalos | Thirty entries, including flight and attack clips; six profile parts; some clips use an older skeleton | Flying wyvern with a dependable takeoff/attack/landing cycle |
| Lagiacrus | Seven clips: roar, land locomotion, swimming; seven profile parts; no named combat clip | Amphibious movement first, then a telegraphed attack |
| Blango | `animation.blango.new` has no bone tracks | Small snow monster; needs authored or explicitly approved temporary motion |
| Blangonga | `animation.blangonga.new` has no bone tracks | Large snow monster; simple melee first, pack leadership later |
| Deviljho | `animation.deviljoe.new` has no bone tracks; preserve the spelling of the existing key | Aggressive brute with a small readable melee repertoire |
| Zinogre | `animation.zinogre.new` has no bone tracks | Ground predator; charge/discharge flavor after basic combat works |
| Flashbug | Three entries: `animation.flashfly.fly`, `animation.flashbug.fly`, `animation.flashbug.idle` | Flying endemic life with a bounded flash effect |
| Toad | Walk, idle, fuse; poison/sleep/paralysis/blast texture variants | Non-pursuing endemic life; proximity/provocation-triggered status cloud |
| Bitterbug/godbug | Java mesh, two textures, procedural movement | Small ambient/collectible creatures, not hostile monsters |

These behavior directions are proposed game design, not assertions that the old
implementation already implements them or that every Monster Hunter mechanic
must be reproduced.

### 2.2 Asset problems and import requirements

1. Four creature animation files are empty placeholders despite containing a
   named animation. A JSON parser reporting success is not proof of animation
   readiness. Coding agents must not announce those creatures as fully animated.
2. Rathalos clips `attack_air_fireball`, `attack_claw_scratch`, `attack_bite`,
   and `attack_airsweep` reference bone names absent from its current geometry.
   Examples include `rightwinglimb1` and `leftFoot2`; see the corresponding keys
   in `rathalos.animation.json` around lines 7747, 11865, 12833, and 14797.
   Retarget against the actual skeleton with visual review; do not globally
   rename bones or suppress warnings to make a validator pass.
3. Existing clips use MoLang expressions, easing, and `hold_on_last_frame`.
   Exercise them with the selected GeckoLib parser and renderer before activating
   them in gameplay. JSON validity does not establish playback compatibility.
4. All four current profiles' `synched-bones` names exist in their corresponding
   geometry. Those marker bones are useful preserved authoring work, but their
   world positions depend on parent transforms, animation, orientation, and scale.
5. `geo\entity\bone_armor.geo.json` and
   `geo\item\armor\bone_armor.geo.json` are different models, not interchangeable
   duplicates. The item armor animation map is empty. Armor restoration is
   outside the creature baseline, but neither export should be destroyed.
6. `DragonRenderer.java` hides `tailclub` and changes to `_tailcut` textures.
   Preserve the severed-tail models/textures and the idea; do not assume every
   species uses that bone name.
7. `LagiacrusModel.java` mutates interpolation fields while rendering. Preserve
   its visual intent, not render-driven gameplay state. Procedural tail/neck
   smoothing is polish, not a prerequisite for the first playable creature.

### 2.3 Preserve before replacing

Before removing old runtime files, record the baseline commit and a manifest of
asset paths, SHA-256 hashes, species, referenced clips, texture variants, and
known author/permission status. Include `BugModel.java` and the hitbox profiles.
The manifest can be a small machine-readable file plus a short human-readable
exception list; do not build an asset-management service.

Keep canonical originals in version control. If an export needs conversion or
retargeting, make that a reviewable change with before/after renders, and retain
the original through the recorded baseline. Do not keep two active mod source
trees or compile old and new registrations together.

**Distribution gate:** the root `LICENSE` is GPLv3 text, `gradle.properties`
identifies GPL, and `META-INF\mods.toml` instead says `All Rights Reserved`.
Resolve this inconsistency with the maintainers and establish model/texture
provenance and contributor permissions before distribution. A code license does
not itself establish rights to every art asset or Monster Hunter branding.
Preserve existing artist/dependency credits; do not invent ownership or
unilaterally relicense assets.

## 3. KISS/YAGNI rules for every implementation packet

These are acceptance rules, not optional style advice.

| Rule | Concrete consequence |
|---|---|
| One game version, one loader | No Forge/Fabric/NeoForge abstraction layer or multi-loader build |
| One working creature before a framework | Implement the Great Izuchi slice concretely; extract only behavior actually shared by the second creature |
| One owner for an action | One server-side attack controller; navigation goals and animation callbacks cannot independently start/deal the same attack |
| Ordinary Minecraft first | Use normal entity health, damage, navigation, tracking, persistence, and loader facilities before inventing replacements |
| Animation is presentation | No renderer/client packet may decide damage, part health, aggression, or world movement |
| One source of timing truth | Attack phases and client presentation derive from the server action state; no unrelated magic timers in several classes |
| Small dependency budget | Every dependency must solve a demonstrated problem on the pinned target; no speculative AI, compatibility, or optimization libraries |
| Small data budget | Named constants and a few typed values first; no behavior DSL, custom editor, generic species scripting engine, or universal combat schema |
| Small state budget | Persist durable gameplay facts, synchronize visible state, derive the rest; do not synchronize every timer/bone every tick |
| Small work packets | Give an agent one bounded deliverable and its tests; do not ask it to port all monsters while redesigning the AI |
| No fake completion | A placeholder animation, giant catch-all hitbox, disabled damage path, swallowed exception, or failing test exclusion is not a solution |
| Learn before optimizing | Add a performance fix only after measuring a concrete cost; no new pathfinder, IK solver, or concurrency system by default |

The initial implementation does **not** need old-world migration, breeding,
growth/random sizes, part severing, a quest system, a weapon/armor progression
system, custom elemental damage math, a boss HUD, multiplayer difficulty scaling,
perfect Monster Hunter combos, a behavior-tree engine, a server skeletal-animation
interpreter, or a custom hitbox editor.

Deferral is milestone-specific. Ground, flight, and water movement are eventually
required where they are essential to a creature's agreed baseline. Natural
spawning, multiplayer correctness, model-aligned damage regions, and health are
part of the requested outcome, not optional polish.

**End-of-packet simplicity pass:** remove unused hooks/configuration, collapse
one-implementation interfaces, identify duplicate state ownership, and ask of
each new abstraction/dependency: "Which behavior in this packet fails without
it?" If there is no concrete answer, leave it out. Do not remove tests, explicit
error reporting, or server validation in the name of simplicity.

## 4. Runtime contract: the implementation must preserve these invariants

This section specifies behavior, not a requirement to create an interface/class
for every heading. Start with a concrete entity, one combat goal, a small part
class if needed, client renderer/model, registration, and tests. Extract helpers
only when a second real caller needs them.

### 4.1 Health and ownership

Use one ordinary Minecraft living entity as the authoritative creature:
attributes, `getHealth`, healing, armor/resistance, damage attribution, death, and
loot belong to that parent. Hitbox parts are not separate living monsters.
Start with fixed adult scale and normal Minecraft damage; do not port the custom
elemental damage system or cooldown-bypassing damage tags by default.

Damage received through a named part must pass through the parent's normal
damage pipeline exactly once. A projectile hitting a head must not also damage
the root body a second time. The same principle applies to explosions or other
area effects that enumerate multiple parts. Preserve distinct legitimate attacks
from different attackers; a blanket "one hit per monster per tick" workaround
would incorrectly discard multiplayer damage.

When part breaking is added later, parent health and part durability are
different quantities. Define whether durability consumes pre- or post-mitigation
damage and test that rule; do not confuse it with a second application of parent
damage. Zero/rejected damage must not silently sever a part.

### 4.2 Three different meanings of "hitbox"

| Geometry | Responsibility | Initial implementation |
|---|---|---|
| Movement/collision bounds | Occupying space, block collision, step clearance, navigation | One conservative body-sized root envelope with a measured ground footprint |
| Hurtboxes | Where a player/projectile can damage the creature | A small set of server-owned body/head/tail regions for large creatures; a single fitted region is fine for tiny endemic life |
| Attack volumes | Where an active claw, bite, tail, projectile, or cloud damages others | Explicit server-side volumes enabled only during an attack's active interval |

A head or tail hurtbox does not magically make the pathfinder understand that
appendage. Conversely, a single huge body box spanning a long tail is not
acceptable combat geometry. Do not promise pixel-perfect mesh collision:
well-fitted, documented AABB approximations are sufficient if attacks look fair.

Start with native loader multipart support if its feasibility gate passes.
The gate must exercise actual client melee selection, projectile collision,
server entity lookup, parent damage, and cleanup; merely constructing parts is
not evidence that all interaction paths work.

For initial pose matching, use a few explicit body-local offsets and, where an
attack visibly moves a limb, a short authored sequence of offsets tied to that
attack's age. A small linear interpolation helper is enough. Do not implement a
server clone of GeckoLib, evaluate arbitrary MoLang on the server, or obtain
authoritative transforms from a rendered client.

Document the local axis convention, origin, units, and yaw convention next to
the geometry constants. Model coordinates and Minecraft world coordinates are
not interchangeable. Verify the transform at four cardinal orientations using
the real model, including parent-bone offsets; blindly copying a marker's pivot
is wrong. Apply scale once if variable sizes are introduced later.

Pose approximations must pass visual/interaction acceptance, not merely compile.
If the first attack cannot be made fair with a small authored volume path, stop
and present that concrete limitation. Do not quietly replace it with an oversized
radius check or enlarge the project into a general animation engine.

### 4.3 One server-side attack timeline

Use vanilla goals for ambient behavior and one custom combat goal for selecting,
approaching, orienting, and executing attacks. That goal owns the relevant move
and look controls while attacking. No parallel melee goal, Brain task, or
animation keyframe listener may independently deal the same attack.

The minimal logical phases are `IDLE`, `WINDUP`, `ACTIVE`, `RECOVERY`, with death
overriding all of them. A cooldown may remain after recovery. Prefer deriving the
phase from attack ID and elapsed ticks rather than storing several counters that
can contradict one another.

| State | Owner and lifetime |
|---|---|
| Parent health | Vanilla living-entity state; saved/tracked through the platform |
| Attack ID, action sequence, start tick | Server sets; current state available to newly tracking clients |
| Elapsed attack time and phase | Derived from the chosen timeline; never client wall-clock authority |
| Cooldown and targets already hit | Server-only, bounded to the action; do not network a growing collection |
| Target and movement decisions | Server AI; clear when no longer valid |
| Texture/type variant | Save and synchronize if the creature has variants |
| Part durability/broken flags | Add only with part breaking; parent owns durable saved state |
| Bone interpolation/particles | Client presentation, not saved gameplay state |

Required transition rules:

1. Only start when alive, target valid, cooldown elapsed, and the attack's entry
   conditions hold. Start exactly once and allocate a new action sequence.
2. Windup visibly telegraphs. Choose and document the point at which facing locks
   or becomes turn-rate-limited; the monster must not snap a committed attack
   behind itself to follow a dodging player.
3. Only the active interval evaluates attack volumes. Exclude the attacker, its
   own parts, and otherwise ineligible victims; normalize multipart victims to
   their parent before deduplication. Intersect potential victims'
   bounding boxes, not just their centers. Account for intervening terrain so a
   melee attack cannot hit someone through a wall.
4. Track victims by action sequence and, only when intentionally supported,
   strike index. A continuously overlapping victim receives at most one hit per
   strike. Leave vanilla damage invulnerability in place unless a specific
   designed multi-hit attack has its own reviewed rule.
5. Recovery prevents immediate re-entry. Repeating the same attack must produce
   a new sequence and new animation, not a stuck cached animation.
6. Death/removal cancels immediately. Target loss cancels safely before a strike;
   once an active strike is committed, it may finish at its already chosen pose,
   but cannot acquire a new victim by instantly re-aiming.
7. Goal stop, failed navigation, teleport/dimension transfer, and chunk reload
   have explicit cleanup. For the first implementation, loading a creature
   cancels transient combat and imposes a short cooldown rather than replaying
   an interrupted attack or persisting raw entity references.

Initially give Great Izuchi **one** working attack. The existing scratch clip is
3.25 seconds (65 ticks at normal playback); tail swipe is 2.375 seconds (round
up to 48 ticks), tail slam 4.375 seconds (88 ticks). These are clip lengths, not
proven contact windows. Scratching has several visible motions; one damage
application per action is an acceptable provisional design, not an accidental
every-tick multi-hit.

Before accepting that attack, the implementation agent must provide a concrete
timeline with integer windup/active/recovery boundaries and an overlay showing
which motion makes contact. Calibrate by viewing the asset; do not guess contact
timing from the filename. Keep visual playback at normal speed initially.

### 4.4 Client presentation and networking

Render the existing geometry/textures through the selected GeckoLib release.
Locomotion can use movement state; committed actions follow the server action
identity and age. Animation callbacks may play cosmetic effects, never apply
health changes or choose AI transitions.

Use normal entity tracking for compact state where possible. A join-in-progress
client must receive the current attack/variant state, not require an earlier
one-shot packet that it missed. Account for attack age on arrival; do not replay
the windup of an attack that has already ended. Prove the chosen GeckoLib API can
implement this with its actual version's state/controller model.

Common/server code must not import client renderer/model classes. Register
renderers and overlays only on the physical client. A dedicated server with no
rendering clients must make identical damage decisions.

Part identity belongs to the parent and has a stable bounded ordering. Verify
the loader's ID allocation, client reconstruction, tracking, lookup, and removal
rules against its actual source. Do not persist transient numeric entity IDs,
spawn independently saved duplicate parts, or leak parts when a parent unloads.

If native interaction needs a small custom packet, the feasibility report must
explain why before adding it. The server must validate ownership, current target,
reach, geometry, dimension, and allowed interaction. Never accept arbitrary
client-supplied bone transforms, damage amounts, or part-health changes.

### 4.5 Movement and in-world integration

Use vanilla ground navigation first. Give the body a plausible step height and
footprint; re-path at a bounded interval and abandon unreachable targets rather
than recalculating paths forever. Do not copy the old custom pathfinder unless a
small reproducible scenario proves ordinary navigation cannot meet the need.

For later wyverns, explicit ground/takeoff/flight/landing states must have one
navigation owner and bounded altitude/landing search. For Lagiacrus, land/water
mode changes must swap the relevant navigation/control together and handle a
shoreline and shallow water. No generic universal locomotion framework is needed.

Spawn eggs and summonability come first for development; they are not final
world integration. Before a species is accepted, add appropriate biome tags,
spawn placement, rarity/group size, and normal population limits. Use the
loader's supported spawn/data mechanisms, not a loop that spawns extra mobs every
world tick. Start rare, make spawning disableable, and use a documented temporary
test setting to demonstrate it without waiting for random chance.

Avoid terrain destruction by default. Preserve vanilla environmental damage and
reasonable despawn/persistence behavior. Make an explicit decision for named or
persistent creatures rather than marking every naturally spawned monster
persistent and exhausting world mob capacity.

### 4.6 Observable acceptance, not screenshots alone

Use the selected loader's existing test facilities and a small test arena.
Add only the minimum test setup needed to make these assertions repeatable.
Tests should use fixed initial positions and deterministic choices.

| ID | Acceptance requirement |
|---|---|
| A01 | Clean build and both client and dedicated-server startup on the pinned dependency tuple; no accidental client-only class loading |
| A02 | Spawn, save, unload/reload, reconnect, and remove each accepted species without missing assets, duplicate parts, or a permanently stalled entity |
| A03 | Parent health decreases once for a successful melee or projectile hit on a part; misses do nothing; independent players' legitimate hits are not conflated |
| A04 | Points inside a named hurtbox can hit it; probes at least 0.5 block outside every hurtbox cannot; exercise front, rear, flanks, and high/low attacks at four orientations |
| A05 | No attack damage in windup, recovery, idle, or after death; one hit per intended strike despite prolonged overlap |
| A06 | Two players can each be hit once by an area strike; multiple parts on one victim do not turn that into duplicate damage |
| A07 | Repeating the same attack, losing/reacquiring a target, blocking sight, and failing a path cannot wedge the state machine or teleport an attack onto a new target |
| A08 | Action presentation and actual contact align in a client overlay; a newly tracking second client sees the current state rather than restarting old actions |
| A09 | Single-player and dedicated server with two clients agree on health and death; moving the camera away does not alter monster damage or part positions |
| A10 | Creature navigates open terrain, a corner, a body-wide passage, a blocked passage, a step, and an unreachable target without an unbounded retry loop |
| A11 | Natural spawn placement is valid and controlled; disabling spawning works; death and unload clean up the entire multipart creature |
| A12 | Flight/water species additionally pass ceiling, landing, target-loss, shoreline, shallow-water, and mode-transition scenarios applicable to that species |
| A13 | Death occurs once, awards loot once, stops attacking, and finishes presentation/removal; later part severing also occurs at most once |
| A14 | A fixed arena with ten active large monsters and two players runs for ten minutes without a stuck state, steadily accumulating parts, or steadily growing action/victim collections |

For A08, record active-frame overlays at windup end, first contact, middle/end of
contact, and recovery. A provisional fitting target is no unexplained contact
more than 0.25 block beyond the intended striking surface at adult scale; if
AABB approximation makes that unsuitable, obtain explicit visual acceptance and
record the per-attack tolerance. Do not label a coarse whole-creature radius as
"proper hitboxes."

A14 is a correctness/soak gate, not a hardware-independent FPS promise. Record
the machine, scenario, tick timings, and remaining symptoms. Profile only if it
reveals a problem.

Automated server tests cannot establish that a texture renders correctly, an
animation looks right, or real client targeting works. Manual client and
two-client evidence is required for those rows. If the coding environment cannot
launch the clients, report those rows as pending and give the maintainer exact
steps; never mark them passed by inference.

## 5. Implementation runbook and agent work packets

The platform decision and old-code evidence later in this document constrain this
runbook. Read the entire handoff before P0. Complete one integration gate before
opening the next set of shared-runtime changes.

### P0 - Freeze the target and preserve the originals

**Owner:** orchestrator. **Depends on:** maintainer approval of the proposed target.

Deliver a small recorded version matrix with exact Minecraft, Java, loader,
Gradle/plugin, GeckoLib, mapping, and test setup versions, plus source URLs.
Use the official target-version MDK; do not update this Forge build by replacing
`forge` strings with `neoforge`. Check the selected APIs against matching-version
documentation/source. Never mix 1.21.1 and 26.x examples.

Create the preservation manifest from section 2 and record the existing branch
tips before removal. Review any unique `brain` branch work before deciding what
to reuse. Keep the `mhnw` namespace unless there is a concrete reason not to;
this is convenience, not an old-world compatibility commitment.

Replace the build/runtime foundation in reviewable commits while preserving art
and provenance. Only the new runtime should compile/register. Do not delete
unrelated user changes or leave stale generated recipes referring to removed
registrations in the built jar. Leave deferred content in its preserved baseline
or a clearly non-loaded archive, not in active data directories.

Pin build-plugin and dependency versions rather than using `1.+` or open plugin
ranges. Make metadata match the actual artifact: mod ID/version, exact supported
game/loader range, required dependencies, and license wording approved by the
maintainers. Use the MDK's resource expansion instead of contradictory hardcoded
versions.

Set up one small CI workflow for the build; add the registered server GameTests
once P1/P2 supply them. Do not create a GUI test farm or a new testing framework.

**Exit:** A01 on an otherwise minimal mod, an inspectable built jar, an asset
manifest, a working client/server/test launch procedure, and no accidental loss
of geometry or authorship. No monster AI work until this is green.

### P1 - Prove the risky seam: rendering plus native multipart interaction

**Owner:** one runtime agent. **Depends on:** P0.

Use Great Izuchi as a temporary stationary prototype with normal health, a
renderer, and just enough native parts to exercise body/head/tail interactions.
This is a spike to be incorporated or removed, not a second permanent framework.

Prove real player melee and arrows against the tail outside the root body's
bounds, from a dedicated-server client. Also exercise a body hit, a miss, an
area-damage source, two independent attackers, despawn/removal, and reconnect.
Inspect native multipart parent/child ID handling rather than assuming every
vanilla dragon behavior generalizes to arbitrary mobs.

Add a developer-only overlay for root bounds, named hurtboxes, attack volumes,
and action phase/age. Use F3+B where sufficient and add only missing information.
The overlay reads or depicts server-defined geometry; it is not authoritative.

**Exit:** A02-A04 and the relevant parts of A09/A11, with actual client evidence.
Document the chosen native interaction path and any limitations. If it fails,
report the smallest missing capability and the supported alternatives; do not
silently restore client-trusted bone synchronization or write a new multipart
network stack as an unreviewed fallback.

### P2 - Finish one Great Izuchi combat slice

**Owner:** one combat agent. **Depends on:** P1.

Wire normal attributes, target selection, ground navigation, idle/walk/run/death
animation, and one scratch attack using section 4. Keep concrete implementation
in the species entity and a combat goal until repetition demonstrates an
extraction point. Use named provisional health/speed/damage/cooldown constants;
balance tuning is not a reason to leave the behavior unfinished.

Publish the attack's contact timeline and body-local volume path next to the
implementation. The scratch/death clips already exist; do not regenerate the
model. Implement all stop/death/reload/target-loss paths with the same cleanup.
Add sparse development diagnostics for attack ID/sequence, transition reason,
and rejected/accepted contact; no per-frame production log flood.

Prove the first slice with a spawn egg, then wire one appropriate biome spawn
entry and a spawn-disable setting. Finish the tests in the same packet as the
behavior, not in a future cleanup milestone.

**Exit:** A01-A11 and A13 for Great Izuchi. This is the first playable result.
Tail swipe/slam, rallying followers, part breaking, growth, and custom elements
are not part of this gate.

### P3 - Demonstrate reuse with Aptonoth and endemic life

**Owner:** species agent(s) after shared contracts freeze. **Depends on:** P2.

Bring over Aptonoth, toads, flashbug, and bitterbug/godbug in small independent
species packets. Aptonoth proves that the new design supports passive/fleeing
behavior without pretending everything is a hostile boss. Endemic life proves
variant persistence and bounded status effects without heavyweight multipart
handling for tiny creatures.

Extract the smallest shared base/helper only where these implementations
actually repeat P2 code. Preserve the Java bug mesh and both textures. Add
correct display names and spawn items as species are integrated, not an entire
unrelated crafting/ore system.

Use ordinary effects as clearly labeled provisional equivalents where exact
Monster Hunter ailments would require a new system. For example, a temporary
slowness-based paralysis is not a complete custom paralysis implementation.
Sleep/control-lock semantics require an explicit later design rather than
silently disabling all of a player's controls.

**Exit:** each integrated species passes applicable A01-A11/A13; variants survive
save/reload; effects cannot trigger indefinitely or damage through walls
unintentionally. Keep the shared combat core stable while species agents work.

### P4 - Add remaining ground combat and the wyverns

**Owner:** species agents, one navigation/runtime owner. **Depends on:** P2 and
the reusable boundaries established in P3.

Add Izuchi with simple independent movement/targeting. First try a focused
retarget of the additional `brain` branch attack/death assets described in
section 6.1; if unsuitable, obtain an authored clip or approval for temporary
presentation. Do not build a pack manager merely to make the species function.

Next add Rathian ground combat, then a bounded flight mode, then Rathalos.
Reuse only the flight code actually common to those two. Begin with one sound
attack per species, add another only after its predecessor is synchronized and
fair. Use existing working clips before repairing the mismatched Rathalos clips;
do not play clips that target nonexistent bones.

Projectiles must be created server-side, have the correct owner, respect ordinary
terrain collision, and deal damage once. No terrain destruction by default.
Ground and flight attack variants must use the right geometry and animation;
switching a Boolean must not leave the wrong navigation controller active.

**Exit:** applicable A01-A14 for these species, including takeoff/landing,
ceiling/obstacle handling, and loss of a target in flight. They are not "done"
as permanent hovering statues or ground-only Rathalos unless the maintainer
explicitly agrees to a reduced release scope.

### P5 - Add Lagiacrus without reviving the old IK dependency chain

**Owner:** one movement/species agent. **Depends on:** P2; avoid concurrent edits
to navigation shared with P4.

Prove land and water movement, then shoreline transition and amphibious target
pursuit. Use the authored swim/land clips. Keep procedural neck/tail smoothing
cosmetic and off the critical path. Add one attack with an authored or explicitly
approved temporary presentation; the existing asset set has no named attack clip.

**Exit:** applicable A01-A14, especially shallow water, leaving water, blocked
shoreline, invalid paths, and mode changes without duplicated goals/controllers.

### P6 - Complete the four art-blocked species

**Owner:** species agents plus a maintainer/artist decision. **Depends on:** P2
for runtime; asset acquisition can begin as soon as P0 finishes.

Blango, Blangonga, Deviljho, and Zinogre have geometry/textures but no actual
animation tracks in their placeholder files. Request editable originals or
new exports from the contributors. If none exist, obtain explicit approval for
small temporary procedural or newly authored animations. Do not invent the
existence of polished legacy animations.

Implement a minimal readable behavior per species using the established ground
runtime. Pack leadership, Zinogre charging/discharging, and Deviljho's larger
repertoire are later flavor increments after each creature can move, fight, take
damage, die, and spawn correctly.

**Exit:** each species passes the applicable acceptance matrix and has approved
non-placeholder presentation. If art remains unavailable, report exactly which
species are blocked; the all-models baseline is still incomplete.

### P7 - Finish the requested all-creature baseline

**Owner:** orchestrator/integration reviewer. **Depends on:** P3-P6.

Produce one roster checklist matching section 2.1. Every row must have a render,
appropriate bounds, normal health/damage/death behavior, meaningful ambient or
combat behavior, natural world integration, and dedicated-server evidence.
Every variant must have correct appearance and persistence.

Run the acceptance matrix, including the ten-monster soak. Re-run data generation
and inspect the jar for missing or stale resources and accidental client-only
dependencies. Document installation, exact supported versions, dependency
requirements, known behavior simplifications, and reproducible manual scenarios.
Resolve license/provenance questions before distribution.

**Exit:** the requested baseline is actually complete, or the maintainer has
explicitly accepted a documented reduced roster. Do not relabel the P2 slice as
the entire revival.

### P8 - Optional Monster Hunter fidelity, only after P7

Add improvements as separately approved small tasks: additional attacks,
Great Izuchi/Blangonga coordination, enrage/exhaustion, bounded retreat/sleep,
part durability/severing with the saved flags and tail assets, elemental/status
flavor, sound, cosmetic IK, growth/size variation, and balance.

If severing is selected, explicitly cover part HP persistence, exactly-once break
events, disabled severed hurtboxes, parent damage, late-joining clients, tail
rendering, and single loot ownership. If variable scale is selected, test render,
root dimensions, parts, attack reach, and navigation together.

Quest systems, weapons/armor, ore progression, multi-loader support, and exact
Monster Hunter simulation are separate projects unless the maintainer expands
the goal. The existence of old files is not authorization to rebuild all of them.

### 5.1 Delegation protocol

The orchestrator owns build files, dependency versions, registrations, shared
entity/combat state, networking contracts, and integration. Do not let several
agents independently "improve" these surfaces.

Use at most two implementation agents concurrently at first, and only when their
file ownership is disjoint. Good parallel work: repairing a species' animation
assets while another agent implements a frozen species adapter. Bad parallel
work: two agents changing the base entity, attack state, and registration together.
Testing shared runtime changes follows that implementation; it is not an excuse
for another agent to redesign the contract in parallel.

Give each agent this packet, not just "read the repo and port X":

```text
Task: <one P-number deliverable or one species subset>
Baseline commit: <current integration commit>
Pinned platform: <exact tuple from P0>
Read first: docs\REVIVAL_HANDOFF.md sections <relevant numbers>
Owned files: <explicit paths; list forbidden shared files too>
Existing interfaces/state: <actual current names and method signatures>
Required behavior: <bounded inputs, transitions, outputs>
Must preserve: <applicable section 4 invariants>
Out of scope: <explicit exclusions>
Acceptance: <A-number rows and exact runnable commands/manual steps>
Stop conditions: <dependency mismatch, missing API/art, shared-contract change>
Return: changed paths, behavioral result, evidence, limitations, decisions needed
Do not: add dependencies, change the target, merge another branch wholesale,
       modify other agents' files, or mark manual-only gates passed without a run.
```

The orchestrator reviews every result, integrates it, and runs the combined
scenario before dispatching work that depends on it. A subagent's confident
summary is not acceptance evidence. If an agent needs to change a shared
contract, pause affected work, update the contract once, and redistribute it.

Keep progress in the host's task/session tracker or one small checklist. Do not
create a stack of overlapping plans, architecture rewrites, speculative tickets,
or an agent-coordination framework. Use more capable reasoning for a demonstrated
unresolved design failure, not to repeatedly re-audit already settled packets.

## 6. Audit evidence: what to salvage, and why not just switch AI libraries

The current foundation is Forge 1.20.1 / Forge 47.3.22 / Java 17 / Gradle 8.8,
Parchment `2023.09.03-1.20.1`, GeckoLib 4.7, MultiHitboxLib 1.8.1, and a declared
SmartBrainLib 1.15 dependency. There are 114 Java source files, no tracked test
sources found, and a configured but not populated GameTest run configuration.

This is not ancient pre-Brain code frozen at that point in Minecraft's history.
It already reached 1.20.1 and contains both a vanilla Brain experiment and a
separate SmartBrainLib branch. The question is runtime reliability and effort,
not whether Forge's organizational history invalidated the models.

Unless otherwise noted, paths below are under
`src\main\java\com\carro1001\mhnw`.

| Evidence | What it establishes | Revival consequence |
|---|---|---|
| `entities\IzuchiEntity.java` defines/ticks a vanilla Brain; it inherits the base monster goals | `master` already mixes Brain-based behavior with inherited Goal infrastructure | Pick one owner of movement/combat for a species; another Brain rewrite alone will not settle ownership |
| `entities\GreatIzuchiEntity.java:58-73` installs three same-priority melee goals, with a TODO that it keeps choosing the same attack | Attack selection is distributed across competing goals, not a deliberate attack selector | Use one combat goal and an explicit choice; start with only one attack |
| `entities\ai\HitboxMeeleeAttackGoal.java:90-105` calls `hurt(...)` and then `monster.doHurtTarget(entity)` for the same overlapping victim | Two damage entry points are invoked for one detected contact; exact observed damage still depends on vanilla cooldown/mitigation | Replace with one intentional parent/target damage path and regression assertions |
| The same goal sets victims non-invulnerable, inflates both contact boxes, and counts attack-check passes rather than maintaining per-victim strike records | Contact fairness, eligibility, and multi-hit ownership are not explicitly modeled | Do not preserve these shortcuts; use bounded attack geometry and per-strike victim deduplication |
| `entities\NewWorldMonsterEntity.java:152-163` retrieves an existing part-type list or `List.of(partEntity)`, but never appends a later same-type part | Subsequent parts of the same logical type are omitted from that lookup list | Preserve named-part intent, not this bookkeeping; test multiple parts of one type |
| `entities\NewWorldMonsterEntity.java:96-113,140-150,212-236` implements carving, sets health back to 1 in `die`, and gates cleanup through death state | Corpse/carving lifecycle replaces normal death and adds another state machine | Start with vanilla health/death/loot and bounded visual death; carving is not required by the user's goal |
| `entities\helpers\MonsterBreakablePartEntity.java:68-120` forwards damage, stores local HP, serializes it, and conditionally creates a severed tail | There is useful part-durability work; it is not accurate to say the old code has no part persistence at all | Revisit later with parent-owned durability, single break events, and lifecycle tests |
| The four profile JSON files set `sync-with-model` and `trust-client` to true | The existing configuration explicitly opts into client-trusting synchronization | Do not carry that authority model into the reboot; test server-owned parts without rendering |
| `entities\DragonEntity.java` has ground/flying navigation/control modes; `entities\LagiacrusEntity.java` has amphibious travel and manual chain math | There is useful species intent, but movement and cosmetic work are intertwined with old APIs | Reuse behavior concepts and selected calculations only after ordinary navigation proves insufficient |
| `registration\ModEntities.java` registers entities and eggs; no entity spawn-placement/biome-spawn wiring was found in the audited source/resources | Registration is not the same as natural world integration | Natural spawning is a required explicit packet deliverable |
| `build.gradle`, `gradle.properties`, and `META-INF\mods.toml` disagree about ranges/versions/license and include auxiliary integrations | The current build is not a clean specification of a new supported platform | Use a fresh target MDK and minimal dependencies, not a mechanical import/package rename |

These are code observations and design risks, not a claim that each symptom was
reproduced in-game. In particular, do not describe the double damage entry points
as a measured exact damage multiplier without reproducing that result.

### 6.1 The `brain` branch contains additional work worth rescuing

Audited branch head:
`27e05f281bb56bd19d6e98d4a973360794db1eb6`.
The GitHub comparison reports two commits ahead and three behind `master`;
their common ancestor is `84a5e81e52f0c177255195966b55ab0e98733304`.
The substantive commit is explicitly titled "half baked izuchi brain transplant"
and "switching from goal to smart brain".

Do not merge this branch wholesale. It predates the current master dependency
update, and its build declarations do not declare SmartBrainLib despite the
new SBL imports. Its `AnimatableHitboxMeleeAttack.java` contains useful ideas:
an explicit hit window, delayed action, and attack cooldown memory. However, it
also allocates `hitIDToEnemyUUID` without using it in the active damage loop, and
its `stop` method clears the target but not all the animation/timer state.
Changing schedulers does not itself solve hit deduplication or interruption.

**Important additional art:** the branch has a modified Izuchi geometry, a
four-part Izuchi hitbox profile, and nine animation keys instead of master's four:
idle, walk, run, roar, rally, tail swipe, tail slam, death, sleep. Preserve these
as candidate sources in P0.

They are not automatically ready to use: the branch's roar, rally, tail attacks,
and death reference bones absent even from the branch's paired geometry, including
names such as `left_shoulder`, `left_ankle`, `mane`, and `tailblade`. The paired
geometry has 28 bones and a 128-by-128 texture declaration. A focused retargeting
pass may rescue these clips and avoid authoring from scratch; simply copying
the nine keys into master would not fix them.

Compare branch assets as a set with the intended textures and validate bone
references/playback before adopting them. Do not use a three-dot branch diff's
"modified" label as proof that its version is newer or better than master's
current file.

### 6.2 Salvage decisions

| Keep substantially | Preserve as reference / selectively port | Replace or defer |
|---|---|---|
| Geometry, textures, sound/art source if contributors supply it, working animation clips, bug mesh, credits, repository history | Hitbox marker names and dimensions, species names/attributes as tuning hints, model/renderer resource paths, flight/water intent, breakable-part ideas, branch Izuchi art | Build/toolchain scaffolding, combat timing/ownership, death/carving complexity, client-trusted hitbox synchronization, mixed AI orchestration |
| Existing creature roster and texture variants | Small proven math helpers when a current behavior really needs them | Ore/crafting/armor/progression, blanket growth, IK, SBL dependency, and optional JEI/Jade/instrumentation until they earn a current requirement |

The clean-runtime recommendation is about reducing coupled risk, not asserting
that all old Java is worthless. Even on the old target, these combat/lifecycle
seams would need attention. Conversely, retaining a behavior algorithm or art
asset does not require retaining its original inheritance tree and dependencies.

### 6.3 Final simplicity decisions

The plan deliberately keeps Gecko-style asset reuse, but does not require
retaining GeckoLib merely out of habit: its justification is preserving existing
geometry, animations, MoLang, and authoring workflow without writing an animation
system. Dropping it should require evidence of a simpler working replacement,
not a desire to achieve an arbitrary dependency count of zero.

An old-build reproduction is useful only if it answers a concrete regression or
visual-reference question. It is not a prerequisite for preserving art or
starting P0, and should not become an unbounded attempt to resurrect unavailable
old dependency infrastructure.

Do not multiply the acceptance matrix into dozens of redundant full builds.
Share deterministic fixtures and parameterize species where appropriate. Run
the smallest relevant tests for a species packet, then the shared scenarios at
an integration gate. Full startup/soak and modpack compatibility runs belong at
the appropriate combined milestone, not after every texture or constant edit.

Compatibility does not mean guaranteeing every mod combination. After the
standalone slice passes, use one small representative same-loader/same-version
mod set, then a chosen real pack at P7. Record exact versions and any conflicts.
Do not add a multi-loader layer, compatibility mixins, or mod-specific patches
until a reproduced conflict requires them.

## 7. Platform research and version-specific implementation guidance

Availability below was checked against primary manifests, official MDKs, upstream
source, and published artifact metadata during the 2026-09-09 research session.
Matching published requirements are not proof that this mod has run on the tuple.
Refresh the chosen pins at P0 only for a concrete reason; do not replace them with
floating "latest" dependencies or start chasing snapshots.

### 7.1 The Forge split does not make the repository worthless

NeoForge began in July 2023; modern NeoForge is a distinct loader/API target, not
a relabeled Forge jar. The shared ancestry makes the source concepts familiar,
but newer event, registry, networking, and toolchain APIs still need a real port.
Forge itself continues to publish releases. A blanket "Forge is dead" premise
would be incorrect.

For this project, choose based on the intended modpack ecosystem and working
entity/rendering facilities, not the interpersonal history of the split. Fabric
is viable too, but there is no demonstrated reason to rebuild this project around
its integration APIs or ship both loaders before one release works.

Primary sources:
[NeoForge founding announcement](https://neoforged.net/news/theproject/),
[NeoForge 1.20.2 API/toolchain changes](https://neoforged.net/news/20.2release/),
[Forge's live release promotions](https://files.minecraftforge.net/net/minecraftforge/forge/promotions_slim.json).

### 7.2 The mappings change is real, but version-dependent

Mojang announced removal of Java Edition obfuscation on 2025-10-29. The normal
unobfuscated release line begins with **26.1**, released 2026-03-24; the first
26.1 snapshot already lacks the old mapping downloads. The current stable
release reported by Mojang's manifest during research was **26.2**, released
2026-06-16, requiring Java 25. Do not target the 26.3 pre-release for this revival.

Minecraft 1.20.1 and 1.21.1 are still obfuscated releases. Their toolchains still
use mappings/remapping. Official Mojang class/member names and Parchment's
additional parameter names/Javadocs are different layers. Parchment can improve
the developer experience on those releases, but is not a mandatory reason to
stay with the old Forge/Librarian configuration.

For 26.1+, official parameter names remove much of the need for Parchment; its
extra documentation can still be useful. Unobfuscated Minecraft does not mean
open-source, unchanged APIs, no loader/toolchain, or permission to redistribute
Minecraft code. It does not retroactively deobfuscate older jars.

Primary sources:
[Mojang announcement](https://www.minecraft.net/en-us/article/removing-obfuscation-in-java-edition),
[Mojang release manifest](https://piston-meta.mojang.com/mc/game/version_manifest_v2.json),
[Fabric's 26.1 migration explanation](https://fabricmc.net/2026/03/14/261.html),
[NeoForge's 26.1 release notes, including Parchment guidance](https://neoforged.net/news/26.1release/),
[Parchment setup documentation](https://parchmentmc.org/docs/getting-started).

### 7.3 Verified technical candidates

| Candidate | Java | NeoForge | Matching GeckoLib artifact | Optional SBL artifact, not a planned dependency |
|---|---|---|---|---|
| Selected established-generation target | 21 | `21.1.248` for MC `1.21.1` | `geckolib-neoforge-1.21.1-4.9.2.jar` | `SmartBrainLib-neoforge-1.21.1-1.16.11.jar` |
| Newer-generation alternative | 25 | `26.1.2.108` for MC `26.1.2` | GeckoLib `5.5.2`, matching 26.1.2 NeoForge build | SBL `2.0.0`, matching 26.1.2 build |
| Latest-stable-game alternative | 25 | `26.2.0.84` for MC `26.2` | GeckoLib `5.5.5`, matching 26.2 NeoForge build | SBL `2.0.0`, matching 26.2 build |

All three have published artifacts. Thus newer Minecraft is not ruled out by
imaginary missing GeckoLib/SBL ports. Conversely, those two libraries being
available does not establish that users' favorite content mods have moved.
Do not choose the latest row solely because its version number is larger.

The 1.21.1 GeckoLib artifact was published 2026-07-01 and declares NeoForge
`[21.1.150,)`; the selected `21.1.248` satisfies that floor. SBL 1.16.11 was
published 2025-10-01. SBL is omitted initially. "Optional" here means an
architecture choice; code referencing its classes cannot simply declare it
optional at runtime and continue loading without the library.

The inspected official 1.21.1 MDK pins ModDevGradle `2.0.146`, Gradle wrapper
`9.2.1`, Java 21, and NeoForge `21.1.250`. For this handoff, explicitly set its
NeoForge property to the published `21.1.248` release to match the compatibility
reference in section 7.7. Its optional Parchment configuration is Minecraft `1.21.1`
with mapping data `2024.11.17`. Use official Mojang mappings by default, or keep
that supported Parchment layer through ModDevGradle if helpful; do not carry
ForgeGradle's Librarian plugin into the new build.

The verified GeckoLib Maven coordinate is
`software.bernie.geckolib:geckolib-neoforge-1.21.1:4.9.2`, from
`https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/`.
Use the MDK's normal dependency mechanism, not ForgeGradle's `fg.deobf`.
The [published POM](https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/software/bernie/geckolib/geckolib-neoforge-1.21.1/4.9.2/geckolib-neoforge-1.21.1-4.9.2.pom)
confirms those coordinates.

Pin/source evidence:

- [Official 1.21.1 MDK, inspected commit](https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle/tree/30cafee9cd8d7f46427ec88fa8579d49c146df9a)
- [Selected NeoForge 21.1.248 POM](https://maven.neoforged.net/releases/net/neoforged/neoforge/21.1.248/neoforge-21.1.248.pom),
  [GeckoLib 4.9.2 release metadata](https://api.modrinth.com/v2/version/tPkJmim6),
  [SBL 1.16.11 release metadata](https://api.modrinth.com/v2/version/O5EpeqI3)
- [GeckoLib 1.21.1 source/version catalog](https://github.com/bernie-g/geckolib/blob/0d9d3ea311c93f99276b596eee9b5f69d2e0db92/gradle/libs.versions.toml)
- [26.1.2 NeoForge POM](https://maven.neoforged.net/releases/net/neoforged/neoforge/26.1.2.108/neoforge-26.1.2.108.pom),
  [GeckoLib 5.5.2 release metadata](https://api.modrinth.com/v2/version/xfVfPcoC)
- [26.2 NeoForge POM](https://maven.neoforged.net/releases/net/neoforged/neoforge/26.2.0.84/neoforge-26.2.0.84.pom),
  [GeckoLib 5.5.5 release metadata](https://api.modrinth.com/v2/version/fTK5ltWI),
  [official 26.2 MDK](https://github.com/NeoForgeMDKs/MDK-26.2-ModDevGradle)

### 7.4 GeckoLib 4 versus 5: keep the art, change the integration

On the 1.21.1 / GeckoLib 4 target, preserve the existing resource layout:
`assets\mhnw\geo\entity`, `assets\mhnw\animations\entity`, and
`assets\mhnw\textures\entity`. Rebuild the Java adapters against that version.
Do not follow GeckoLib 5 examples in a GeckoLib 4 project.

If a later approved target uses GeckoLib 5, its migration guide moves model and
animation assets under `assets\mhnw\geckolib\models` and
`assets\mhnw\geckolib\animations`, respectively. Keep useful subfolders and update
resource references. GeckoLib 5 uses extracted render-state concepts; do not
transplant live-entity renderer mutations from the old code. This is an adapter
and playback-validation task, not an instruction to remake the artwork.

Primary sources:
[GeckoLib 5 conceptual changes](https://wiki.geckolib.com/docs/geckolib5/updating/important/conceptual-changes/),
[GeckoLib 5 asset paths](https://wiki.geckolib.com/docs/geckolib5/updating/important/asset-paths/).

### 7.5 Multipart dependency decision

**Default: use a small native NeoForge multipart implementation, subject to P1.**
On the verified 1.21.1 API, the parent exposes `isMultipartEntity()` and a cached
`getParts()` array of `PartEntity` objects. Parts are not independently saved or
automatically animated/synchronized: the host entity manages their positions and
lifecycle. Native infrastructure is not a complete animated hitbox solution.

Read these matching-version sources before writing the part class:
[multipart ownership contract](https://github.com/neoforged/NeoForge/blob/327897179428559b4dd7fd29ef3f9f83aa40452f/src/main/java/net/neoforged/neoforge/common/extensions/IEntityExtension.java#L170-L196),
[official PartEntity test/example](https://github.com/neoforged/NeoForge/blob/327897179428559b4dd7fd29ef3f9f83aa40452f/tests/src/main/java/net/neoforged/neoforge/oldtest/entity/PartEntityTest.java#L76-L180).
The sample is a reference, not permission to skip real-client ID/tracking tests.
For another target, find its corresponding source instead of assuming identical
signatures or lifecycle.

The old MultiHitboxLib 1.8.1 upstream and a maintained NeoForge upgrade path could
not be established: public requests to `DerToaster98/MultiHitBoxLib` returned 404.
That does not prove abandonment or prove the old jar is unusable. A historical
public fork is version 1.2.0, not the version this mod uses; do not attribute that
fork's exact internals to 1.8.1.

More Hitboxes has a real 1.21.1 NeoForge release, but its documented/client-side
attack-box and Gecko synchronization limitations make it a poor default for
server-authoritative monster combat. CustomHitboxLib is another real candidate,
but its recent arrival and broader pathfinding overrides need an independent
feasibility case. Do not replace an uncertain dependency with another merely
because its name sounds appropriate.

Sources:
[historical MultiHitBoxLib fork](https://github.com/ZigyTheBird/MultiHitBoxLib/tree/9b06d5cfa6577057d60e73a9aaa090b51237626e),
[More Hitboxes release](https://api.modrinth.com/v2/version/1Cu922wS),
[its client-side attack-box contract](https://github.com/DarkPred/MoreHitboxes/blob/88899b3e1c4ed881b1ea65b35104453fc6df592a/common/src/main/java/com/github/darkpred/morehitboxes/api/AttackBoxData.java#L10-L16),
[CustomHitboxLib source snapshot](https://github.com/gawrmonster/CustomHitboxLib/tree/fc6111dae542dd3adfeed3fca3231b1eab2cf500).

### 7.6 Targeted validation commands for the 1.21.1 MDK

Use the new wrapper from the selected MDK, not the old repository wrapper.
After P0 has wired the test namespace and fixtures, the normal Windows entry
points are:

```powershell
.\gradlew.bat build
.\gradlew.bat runGameTestServer
.\gradlew.bat runClient
.\gradlew.bat runServer
.\gradlew.bat runData
```

Run client/server launches in separate appropriate sessions rather than treating
them as a sequential script that can finish unattended. Run data generation only
when data changes; choose the smallest registered test selection covering each
packet. Validate the packaged jar in a normal loader installation as well as dev
runs before P7.

The version-specific
[GameTest guide](https://docs.neoforged.net/docs/1.21.1/misc/gametest/)
documents `@GameTest`, `GameTestHelper`, registration through `@GameTestHolder`
or `RegisterGameTestsEvent`, and failing-test exit behavior. Do not copy the
newer 26.x test registration API into this target. The
[dedicated-server guidance](https://docs.neoforged.net/docs/1.21.1/gettingstarted/#server-testing)
explains why an integrated server alone is insufficient. Any development-only
authentication relaxation must remain isolated to a local test environment,
never a public server recommendation.

### 7.7 Where the scene has clustered: compatibility decides the target

**Yes, clustering around particular Minecraft patches still happens, but there
is no single patch shared by the entire scene.** In the dated sample below,
NeoForge **1.21.1** has the strongest demonstrated established intersection.
Forge **1.20.1** remains a meaningful legacy cluster. NeoForge **26.1.2** has an
emerging successor ecosystem, while latest-stable **26.2** is not yet equivalent
for the sampled large content mods.

This is a targeted compatibility sample, not a market-share survey or a promise
that a pack will include this mod.

| Real pack | Dated evidence | Target and significance |
|---|---|---|
| All the Mods 10 | Changelog 8.1, 2026-08-29 | NeoForge 1.21.1; still actively maintained, not simply an abandoned older pack |
| Enigmatica 10 | Release 1.32.0, 2026-08-27 | Minecraft 1.21.1, NeoForge 21.1.248; a released co-installation reference, not just independent mod availability |
| All the Mods 11 | File 0.8.0-beta, 2026-09-07 | Minecraft 26.1.2, NeoForge 26.1.2.106; a real emerging successor, with its own notes describing WIP/placeholders and arriving mods |

The released Enigmatica 10 modlist includes Create 6.0.10, Mekanism 10.7.19.85,
AE2 19.2.17, JEI 19.43.0.390, Iris 1.8.12, Terralith 2.6.2, and **GeckoLib
4.9.2**. The selected Minecraft/NeoForge/GeckoLib tuple therefore already appears
in this released pack. That actual combined set is stronger compatibility
evidence than unrelated "latest" download pages; it still does not prove this
new monster implementation will coexist without testing.

The sampled official mod release lists showed Create and Mekanism on Forge
1.20.1 and NeoForge 1.21.1, but no matching 26.1.2/26.2 artifacts were found.
AE2 had a 26.1.2 beta, but no matching 26.2 artifact was found. In contrast,
JEI, Sodium/Iris, and Biomes O' Plenty already had newer-generation releases.
Thus rendering/performance/library availability is not a proxy for content-mod
ecosystem readiness. The pinned ATM11 modlist also omits Create and Mekanism.
These absences describe the inspected sources, not a prediction about future ports.

For a creature-oriented comparison, Mowzie's Mobs 1.8.2 has published Forge
1.20.1 and NeoForge 1.21.1 artifacts. Alex's Mobs 1.22.9 is a relevant
1.20.1-only example in the inspected official release list.

**Decision rule:** use NeoForge 1.21.1 unless the maintainer supplies a must-have
pack/mod list that changes the intersection. If a non-negotiable favorite is
Forge-1.20.1-only, a fresh runtime targeting Forge 1.20.1 is rational; that does
not require preserving the old mod's fragile implementation. If the favorite
set is Fabric-centered, evaluate that concrete set instead. Neither Fabric nor
NeoForge automatically runs all mods written for the other loader.

Do not pursue simultaneous loaders, compatibility bridges, or an immediate
26.x port in an attempt to satisfy every ecosystem. Release on one cluster,
then reevaluate an actual successor when its supported mod set matches demand.
Of the newer versions sampled, 26.1.2 deserves attention first because ATM11
actually targets it; "latest Minecraft" and "next modpack convergence point"
are not necessarily the same thing.

**Compatibility gate for P7:** make a backed-up disposable copy of Enigmatica 10
1.32.0, or a newer explicitly recorded approved reference pack, and test adding
this mod and its exact required library version. Inspect the pack's existing
GeckoLib requirement before changing it; do not blindly overwrite a shared
library, resolve version conflicts by removing other mods, or redistribute a
pack assembled from unlicensed bundled jars. If library requirements conflict,
record them and decide the smallest compatible version change before claiming
support.

Compile initially against NeoForge 21.1.248 so the reference pack is not excluded
by an unnecessary 21.1.250 minimum. Use an exact Minecraft 1.21.1-compatible range,
not `[1.20.1,)`. Advertise a lower loader/library floor only after actually
validating it. Matching loader/game versions is necessary, not sufficient, for
coexistence; the pack check must also cover world spawning, combat, rendering,
and dedicated-server behavior.

Primary evidence:

- [ATM10's dated changelog](https://github.com/AllTheMods/ATM-10/blob/ab6f65e07b88423cdae1724864ba42a573ba758a/CHANGELOG.md#L7-L17)
  and [official loader/distribution page](https://www.curseforge.com/minecraft/modpacks/all-the-mods-10)
- [Enigmatica 10 release 1.32.0](https://github.com/EnigmaticaModpacks/Enigmatica10/releases/tag/1.32.0),
  [released instance manifest](https://github.com/EnigmaticaModpacks/Enigmatica10/blob/1.32.0/minecraftinstance.json),
  and [released modlist](https://github.com/EnigmaticaModpacks/Enigmatica10/blob/1.32.0/MODLIST.md)
- [ATM11 0.8.0-beta file and release notes](https://www.curseforge.com/minecraft/modpacks/all-the-mods-11/files/8828797)
  and [pinned modlist](https://github.com/AllTheMods/ATM-11/blob/3e964d42f7172a7a32d5f0dec71e96c4b3ea73a7/config/crash_assistant/modlist.json)
- [Create 1.21.1 release](https://github.com/Creators-of-Create/Create/releases/tag/mc1.21.1-6.0.10),
  [Mekanism 1.21.1 release](https://github.com/mekanism/Mekanism/releases/tag/v1.21.1-10.7.19.85),
  and [AE2 26.1.2 beta metadata](https://api.modrinth.com/v2/version/AyF0Qu5L)
- [Mowzie's Mobs NeoForge 1.21.1 artifact](https://api.modrinth.com/v2/version/xgAXTl17)
  and [Alex's Mobs 1.20.1 artifact](https://api.modrinth.com/v2/version/XoIASRVU)

## 8. Copyable orchestrator kickoff

```text
Implement the MonsterHunter-NewWorld revival using docs\REVIVAL_HANDOFF.md.

Read the full handoff once. Treat its selected ecosystem target, user-approved
fresh-world scope, KISS/YAGNI rules, and server-authority invariants as constraints.
The alternate version rows are research context, not permission to switch targets.

Begin with P0, then P1, then the complete Great Izuchi slice in P2. Do not port
the roster or design a generic monster framework before those gates pass.
Continue through the remaining packets only when their dependencies are met.

Preserve all inventoried art, the Java bug mesh, credits, and useful brain-branch
Izuchi assets. Do not merge brain wholesale or assume its extra clips are valid.
Do not undertake old-world compatibility or rebuild unrelated crafting/progression.

Use small subagents only for disjoint, bounded work. Pass each the actual current
interfaces, owned paths, platform pins, acceptance IDs, and explicit exclusions
using section 5.1. You own integration and all shared-contract decisions.

Check actual version-matched source/API signatures before implementing an
uncertain call. If a dependency/API/art requirement is missing, surface it and
the smallest viable alternative; do not invent methods, fake an animation,
swallow the failure, or silently expand the scope.

Run targeted checks as each packet is integrated. Obtain real client/two-client
evidence for rendering, targeting, and multiplayer behavior; when that environment
is unavailable, provide exact manual steps and leave those acceptance rows pending.

Finish every packet with the section 3 simplicity pass. Report completed gates,
remaining blockers, changed files, and concrete evidence. Do not claim completion
of the all-creature goal after delivering only the Great Izuchi prototype.
```
