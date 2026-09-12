# Monster Hunter: New World - product roadmap

**Status: working roadmap, ready for implementation handoffs.**
Revision: `mh-nw-roadmap-2026-09-11-v4`.
First executable packet: [R0a - Preserve and lock the working baseline](R0_BASELINE_HANDOFF.md).

**Packet status (2026-09-12).** R0a, R1a, R0b and R1 (including R1b's carving contract) are
implemented on `master`/`r1/first-hunting-loop`; see
[R1 - Finish the first hunting loop](R1_FIRST_HUNTING_LOOP_HANDOFF.md) and the dated sections of
`TEST_PLAN.md` for what each one actually delivered and which human gates remain open. The product
decisions below are unchanged.

Use the maintainer interview as the product direction. The requester witnessed
the live interview and explicitly authorized sensible PM defaults rather than
another quota-consuming approval round. **The earlier response's DRAFT wording
is not a gate.** This revision replaces v3's confirmation workflow.

**alexVR remains the scope, release and visual-acceptance owner.** Interview
choices and PM-selected implementation defaults are distinguished in section 7.
The maintainer/dev agent may override a default when current code, play or art
provides a better answer; record the change locally and keep working.
No new interview, confirmation-response document or return trip to the roadmap
author is required.

Source response SHA-256:
`9e9275e684e0e8f1319230b489e8aa1762299669110df44e244e3995d041e352`.
The original attachment is unchanged. Its answers, subsequent user direction,
verified branch evidence and the explicit PM defaults below form this working
baseline; do not silently edit the interview to pretend defaults were quoted.

## 0. How to execute without another planning loop

**Take the existing revival forward wholesale.** Preserve working monsters,
endemic effects, measured attacks, assets and developer access, including work
not yet in the first survival release. "Not in R1" means no obligation to finish
or expose that feature in R1 progression, not permission to remove it.

1. Start the linked R0a packet on the current revival branch or a contribution
   branch based on it. Do not execute the historical rewrite queue again.
2. Use section 7's defaults immediately. Do not ask the maintainer to choose
   timer internals, every recipe count, every test detail or whether to keep
   already-working toads. Technical implementation choices belong to the dev.
3. Preserve the major interview choices: custom habitat, personal carving,
   useful bone equipment, bounded MH-style encounters and the release order.
   Local substitutions for APIs, small tuning changes and simpler equivalent
   implementations need a short recorded rationale, not another workshop.
4. Ask the maintainer only when a discovered constraint would materially change
   the product, discard accepted work, break saves without agreement, require
   unavailable rights/assets, or cause a destructive/publishing action.
   Ask one focused question and continue unrelated work where possible.
5. Separate **code complete**, **human evidence pending**, and **release ready**.
   A pending two-client session must not burn agent turns or block independent
   habitat/equipment work. A known client bug must still be fixed before release.
6. Keep one coding workstream active. Use subagents only for bounded independent
   work that saves context; do not fan out a small packet by default.

Claude Code/Opus and Codex/Sol use the same acceptance contracts. Neither needs
to read the entire historical research packet or repeat the asset audit to
begin R0a. Use the kickoff in its handoff, or:

```text
Implement docs\R0_BASELINE_HANDOFF.md on the current revival branch.
Use docs\ROADMAP.md v4 as the working direction. Keep existing features;
do not conduct another interview. Use the stated PM defaults, and record
reasonable local overrides rather than blocking on them. Complete only R0a,
report the evidence and remaining R0b release work, then stop. Do not push
or publish merely because this packet is complete.
```

## 1. Product direction

**An open-world hunting mod: discover wildlife, prepare, hunt, carve, craft,
and choose a harder encounter.** The reported first-release choice is a
Great-Izuchi-centered loop in a **custom Overworld biome**, with personal carving
rewards, bone armor and ordinary cooking. This is not a quest-led progression
system or a model showcase.

The first release targets a **fresh survival world** and small-group play with
ordinary equipment: telegraphed, punishing of carelessness, but fair. Later
installation can expose the biome in newly generated chunks; no retrofit of
already generated terrain or old Forge save migration is promised.

Operating constraints:

- Keep Minecraft 1.21.1 / NeoForge and the working revival architecture. Do not
  restart the rewrite or reopen the loader decision without a concrete blocker.
- No named must-have modpack, multi-loader support, new dimension, mandatory
  quests, ranks, currency, custom station or ore generation in the first release.
- First-release combat stays vanilla-compatible. A fuller MHW-inspired player
  combat overhaul is a **future goal, R8**, not scope for the first greatsword.
- Art arrives asynchronously. An empty named Drive folder is a future-content
  signal, not a finished asset or a promise of a delivery date.
- One implementation workstream at a time; independently usable handoffs still
  matter for clean pauses, different agents and eventual additional contributors.
- Use KISS/YAGNI: small playable additions, not infrastructure for hypothetical
  features.

### What would make this more than a model showcase?

An encounter should reward understanding the monster, not merely having enough
damage. A harvested material should have a discoverable use. Equipment should
offer a reason to prepare differently. A status effect should have a readable
source and an available response. A new habitat should change the hunt, not just
recolor the same mob.

That suggests three release principles:

1. **Complete loops before a complete roster.** One good hunt that produces useful
   gear is a stronger first release than fifteen summonable monsters.
2. **Additive progression in the scheduled releases.** Vanilla and other mods'
   gear can participate. A conceptual later overhaul does not silently change
   that contract now.
3. **Not every asset needs its own subsystem.** A fertilizer texture can support
   fertilizer or a dung bomb; it does not justify a farming simulation. A bowgun
   model does not justify implementing every ammunition family at once.

## 2. Where the project actually is

The working branch **does exist remotely**:
[`revival/neoforge-1.21.1`](https://github.com/Carro1001/MonsterHunter-NewWorld/tree/revival/neoforge-1.21.1).
The inspected revision is
[`3ca5d46`](https://github.com/Carro1001/MonsterHunter-NewWorld/tree/3ca5d46d11a60323170cbd2e7777879af3c421c1),
verified against the remote head on 2026-09-11. It includes the `e9b9eb7`
per-tick roar countdown fix and later corrections to comments and documentation,
plus a defensive death guard in `RoarGoal`. The new notes correctly explain that
normal death stops goal ticking before an in-goal cleanup guard can run.
The stored interview response is now also on that branch.

Branch notes still report 69 passing GameTests; that is historical evidence,
not a fresh run here. The first two roar tests prove starting/rearming, not a
tight real-tick completion deadline. R0a fills that specific coverage gap.

**Superseded (2026-09-12), and worth reading before you judge a checkout:** this
paragraph used to say the planning worktree *and* `master` contain the legacy
runtime. That is true of the author's own planning worktree, but **`master` is
now the NeoForge 1.21.1 mainline** — `revival/neoforge-1.21.1` was squash-merged
into it as [`ab59e19`](https://github.com/Carro1001/MonsterHunter-NewWorld/commit/ab59e19),
and `master` and the revival branch have identical code. The old Forge runtime
survives only in history and on `origin/brain`. The original point still stands
in its corrected form: future implementation plans must start from the accepted
revival code — today that means `master` or a branch based on it — not from an
old planning worktree's Java files. Do not reject a checkout as "the obsolete
Forge version" merely because it is called `master`; check for the NeoForge
pins and `src/main/java/com/carro1001/mhnw/` layout instead.

| Area | Evidence at the inspected revival revision | Roadmap consequence |
|---|---|---|
| Foundation | NeoForge 21.1.248, Java 21, GeckoLib 4.9.2, native parts, server-owned attack timelines, runtime bone-measurement tooling | Preserve it; stage R0 is reconciliation and hardening, not another port |
| Great Izuchi | Three measured attacks, health/death, fitted hurtboxes, opening roar; maintainer confirmed the core combat visually | This is the first survival hunt, not another prototype to rebuild |
| Rathian | Measured right bite plus a mirrored left-bite candidate, range-aware selection, fitted hurtbox offsets and opening roar | Finish an iconic encounter incrementally; old summaries calling it vanilla-melee-only are stale |
| Rathalos | Ground baseline, opening roar, hurtboxes borrowing Rathian measurements; attack-art mismatches and flight remain | Not yet a finished flying-wyvern encounter |
| Lagiacrus | Amphibious pursuit baseline, seven provisional hurtboxes, no outgoing attack; water-to-land bank exit is a documented limitation | Treat fitting, movement and combat as remaining work, not an accepted species |
| Other wildlife | Aptonoth, small Izuchi, toads, flashbug and bugs exist. Small Izuchi currently uses plain melee; toads and flashbugs already release gameplay effects when provoked | Preserve all of it; Q27 adds harassment and R2 adds portable preparation tools |
| Survival economy | No active harvested-material/armor progression or survival recipe/loot data was found in the inspected active resource/registration scan | This is the largest missing player-facing loop |
| Release confidence | Branch notes report 69 passing GameTests; real save/restart, two-client agreement and mid-action tracking still have open work | Fresh automated pass plus actual human release evidence; keep R0a engineering completion distinct from R0b acceptance |

The test result above is a report in the branch, not a run performed for this
roadmap. The user also observed Great Izuchi working in a stream, with remaining
Rathian and Lagiacrus issues. Both sources support extending the revival rather
than restarting it.

**Reconcile documentation before handing out tasks.** `CLAUDE.md`, `AGENTS.md`,
and older paragraphs in `DEFERRED.md` contain statements superseded by later
commits: for example, local-only branch status, the roster, and Rathian's attack
implementation. Read actual code and the newest dated evidence together.
Do not treat the presence of a test as proof of a stronger contract than it
asserts: bounded failed bank-exit pursuit is not successful shoreline traversal.

The original [revival handoff](REVIVAL_HANDOFF.md) remains useful technical
background, not a competing implementation queue. The returned response changes
its product sequencing and its death presentation: a reward-bearing corpse may
remain, but active combat must end and rewards must have one server owner.
Do not interpret the old cleanup requirements as forbidding a deliberately
retained, inert corpse, or use carving to justify restoring the old AI hierarchy.

## 3. Stage map

`R` identifiers remain stable, distinct from the old handoff's `P` packets.
R1a/R1b are slices of R1, not extra releases. R4b is Rathalos; R4 now means Rathian.
Release priority is not a compile-time dependency on every earlier numbered
stage. R0 is split into a small engineering packet and pre-release client/
real-world acceptance; the first handoff implements **R0a only**.

| Stage | Player-facing outcome | Required before integration | Not included |
|---|---|---|---|
| **R0 - Trust the current hunts** | Reliable existing combat, lifecycle and multiplayer foundation | Recorded revival revision | Re-port, custom worldgen implementation, new monster roster |
| **R0a - Lock the working baseline** | Existing behavior protected by precise regressions and accurate handoff docs | Current revival; no new art or interview | Client animation seeking, new content, wholesale refactor |
| **R0b - Close release confidence** | Correct late tracking and demonstrated real-world multiplayer/save behavior | R0a; actual clients/server for visual evidence | Blocking independent R1 work while a human session is unavailable |
| **R1a - The first habitat** | A custom biome actually generates and hosts the selected wildlife | R0a baseline; habitat/population defaults | Nests, custom structures, ores, replacement world preset |
| **R1b - Personal corpse carving** | Participants claim three personal carves from a persistent corpse | R0a lifecycle; reward defaults and material contract | Shared death loot, generic economy, universal corpse handling |
| **R1 - The first hunting loop** | Biome -> Great Izuchi hunt -> carve -> bone armor and cooked meat -> repeat | R0a, R1a, R1b, armor and Q27 harassment; R0b before release | Custom weapons, part breaking, rare-gated basics |
| **R2 - Field preparation** | Prepared meat/BBQ, flash bombs and bucketed toads | R1 item conventions; agreed endemic/effect behavior | Full slinger, global stamina, generic ailment engine |
| **R3 - The hunter's armory** | Bone greatsword first; further equipment one item/family at a time | R1 materials; accepted greatsword texture/export | All weapons, decoration engine, R8 combat overhaul |
| **R4 - Rathian** | A distinctive poison hunt, appropriate counter and useful carves | R1; poison/antidote and accepted attack presentation | Rathalos dependency or an entire aerial-moveset framework |
| **R4b - Rathalos** | A real flying-wyvern encounter | Relevant R4/shared capabilities and its own valid art/flight | Ground-only release passed off as a finished Rathalos |
| **R5 - Targeted part rewards** | One visible break changes carving rewards | R1b and one accepted multipart encounter | Universal part simulator, mandatory severing gates |
| **R6 - Habitat encounter packs** | Independent thunder/aquatic/frost additions | R1 plus each pack's own habitat, art and effect needs | Entire roster at once, vanilla-biome spawn fallback |
| **R7 - Rally and retreat** | Existing escorts rally; wounded monsters retreat to rest sites | Accepted encounters and a concrete rest-site design | General ecosystem simulation, endless reinforcements |
| **R7b - Guide/Guild options** | Unscheduled future guidance or optional bounties | A later decision to include them | First-release or progression prerequisite |
| **R8 - Player combat direction** | Conceptual fuller MHW-inspired combat | New design after R3; compatibility/agency decisions | Current authorization for stamina/dodge/sheathe systems |

**Delivery order:** R0a -> R1a -> R1b -> remaining R1 integration.
Close R0b before the first release, but do not make its human-session scheduling
a prerequisite for independent R1 development.
**Release priority:** R0 + all of R1 -> R2 -> R4 (Rathian).
After that, R3 is art-gated and undated; R4b is separately art/flight-gated.
R5, independent R6 packs and R7 are later priorities, not automatically blocked
by a missing greatsword texture. R7b is unscheduled; R8 is conceptual.
The maintainer can reorder ready later work locally; there are no delivery dates.

### R0 - Trust the current hunts

Preserve the current Great Izuchi win and the measured attack/hurtbox work.
Close the highest-risk seams before relying on them for a persistent economy:
real world save/restart, parts cleanup, dedicated-server/two-client behavior,
and tracking an attack already in progress. The branch explicitly notes that a
newly tracking client's clip can start at frame zero; that must not be called
correct synchronization just because server damage is correct.

Record accepted Great Izuchi combat and Rathian bite/roar without rebuilding
them. Reconcile stale `CLAUDE.md`, `AGENTS.md` and `TEST_PLAN.md` against code.
The new roar fix needs its documented live follow-through, not another port.
Align the packaged license metadata with the maintainer's stated GPL-3.0 choice.

Define the lifecycle/spawn contracts that R1a/R1b consume. **Worldgen belongs to
R1a**, not to both R0 and R1a; the response's stage table duplicated that ownership.
Rathian, Rathalos and Lagiacrus remain registered, summonable and available for
ongoing development. Do not remove them to enforce the first-release roster.

**Exit:** a named baseline with reliable damage/parts/death boundaries, recorded
human evidence, and explicit remaining limitations. Economy-specific counters
and expiry are verified when R1b adds them, not fictitiously closed by R0.

### R0a - Lock the working baseline

Implement [the first handoff](R0_BASELINE_HANDOFF.md): reconcile the current
baseline, protect the already-fixed roar timing and inert death/reload behavior
with focused regressions, fix only demonstrated in-scope defects, and make the
handoff docs truthful. Preserve the current forest-spawn entry until R1a replaces
it with the custom habitat; do not temporarily eliminate working spawning.

**Exit:** coherent build and automated evidence, no removed working features,
and a short, accurate handover. This can finish without a new Discord session.
It does not claim the first release or all of R0 is complete.

### R0b - Close release confidence

Fix the known late-tracking presentation problem on the existing action clock;
do not confuse synchronized attack identity with correctly aged playback.
Keep this a bounded client-presentation task, not a new animation engine.
Use actual two-client/dedicated-server evidence for contact, health, parts and
death, and a genuine save/stop/restart/rejoin cycle.

**Exit:** remaining client/lifecycle release gates have observed results.
Human evidence can be collected alongside R1 integration. Missing human access
is a recorded pending gate, not permission to claim a pass or idle an agent.

### R1a - The first habitat

A **custom biome is the only natural-spawn habitat for the first-release roster**.
Reuse suitable vanilla terrain features and selected vanilla structures; no
custom structures, new art family, ore generation or nests are required.
Use **Verdant Hunting Grounds**, `mhnw:verdant_hunting_grounds`: a temperate
wooded meadow/low-hill habitat made from vanilla terrain, oak/birch vegetation,
plains villages and ordinary mineshafts. Section 7 provides overridable
discovery and population defaults; no new biome-design interview is needed.

Registering a biome or adding spawn entries is **not proof it generates in the
normal Overworld**. Plan a bounded placement feasibility check on 1.21.1; if a
compatible approach needs a worldgen library, the dev may choose one established,
version-pinned 1.21.1-compatible placement library and document the tradeoff,
instead of invasive global worldgen replacement. Do not add it speculatively.
Biome modifiers alone
are not an Overworld-placement plan (see section 9).

Use controlled population and a spawn-disable configuration. Great Izuchi's
existing escort generation and any independent small-Izuchi spawns must not
accidentally multiply packs. Ordinary movement beyond a biome boundary is not
the same as natural spawning there; this is not a request for an invisible wall.

**Exit:** the biome is demonstrably findable in normal fresh-world generation,
the chosen structures generate, and the roster spawns at controlled rates only
in the intended habitat. No command-only or custom-world-preset demonstration
substitutes for the actual survival loop. Existing terrain is not retrofitted.

### R1b - Personal corpse carving

The choice is **carving from the start**, not a suggestion to reconsider
until ordinary drops win by default:

- Eligibility: positive damage credited to a player or their owned attacker.
  Nearby presence alone is not participation; details are fixed in section 7.
- Interaction: **shift + right-click**, immediate grant, **three carves per
  eligible player**, with a short server-side debounce.
- No ordinary death-item rewards from the affected MH creatures.
- An inert, visibly dead corpse remains for **ten minutes**, then unclaimed
  rewards are lost. Persist eligibility, per-player counters and expiry across
  ordinary save/restart/rejoin; reconnect must not renew entitlement.
- Reliable basics supply first-tier equipment; rare trophies do not gate it.

**Correction:** ordinary vanilla items despawn after **five**, not ten, minutes
of entity-ticking time. Keep the maintainer's explicit ten-minute choice; do
not silently halve it. The PM default is **12,000 entity-ticking ticks**, paused
while unloaded or the server is stopped. Persist the remaining ticks; do not
force-load a chunk or use an operating-system wall clock.

Do not choose a retained-parent versus separate-corpse implementation in a product
roadmap. The required result is one inert interaction target, no live attack or
targeting, no independently reward-bearing parts, and no duplicate grant.
Normal living-mob despawn must not prematurely remove a promised carve window,
nor should a corpse need forced chunk loading. Establish the minimum saved
facts before dependent recipes and part rewards are wired.

Q30-Q34 record the defaults for reward-bearing species, attribution, immediate
interaction, full-inventory behavior and XP. Do not apply "no drops" to
vanilla/other mods' creatures, or assume disappearing toads now need corpses.
Use loot data if helpful to define carve results; **no death loot does not mean
no loot-table files**.

**Exit:** each participant can claim only their allowance; simultaneous players,
repeated interaction, both hands, ineligible players and reconnect never grant
extra carves. Ordinary save/reload preserves the exact remaining entitlement
and the selected expiry semantics. Dead monsters cannot attack. Full inventories
follow the selected policy without silent reward loss. These promises do not
claim transactional crash recovery across independently saved player/entity
data; a stronger abrupt-crash guarantee would need its own scope decision.

### R1 - The first hunting loop

**First-release roster:** Great Izuchi, Aptonoth, small Izuchi, Toad, Flashbug
and Bug. Those are six entity types in the response's four creature groups.
Great Izuchi is the main hunt; Aptonoth supplies meat/hide. **Keep existing
toad/flashbug effects and disappearance.** "No new ailment system in R1" is the
intended boundary, not "remove existing status effects."

Q27 adds **ground-based circling and occasional dart-in harassment** for small
Izuchi, not flight or constant melee pressure. "Hover" describes hanging back.
Use normal melee damage without an invented attack/death animation. This is the
reported, explicit escort-presentation exception; it does not waive fairness or
give new major encounters an animation exception. Group pressure, pauses and
disengagement need human acceptance; it must not become an unavoidable swarm.

Make **bone armor** craftable and wearable using accepted source art,
vanilla-comparable protection and **one modest hunting-oriented full-set trait**
(default: +0.1 knockback resistance while all four bone pieces are worn).
Start with iron-equivalent protection and no extra toughness; tune locally.
Mixed equipment retains normal protection and remains viable.
Use reliable hide/claw/meat and suitable bone ingredients with explicit sources
and useful recipes. Vanilla crafting/smithing are the permitted stations; there
is no requirement to invent a smithing recipe solely to use both tables.

Cook meat through ordinary vanilla cooking. No BBQ minigame, custom weapon,
ore progression, part breaking, custom workstation, Guild or field guide in R1.
Rathian, Rathalos and Lagiacrus are not first-release encounters. Portable
endemic tools and new ailment design belong to R2 or their actual encounter.
Use the small localized corpse/carve feedback specified in Q38, not a full guide.

**Exit:** in a fresh survival world, without commands, find the biome and hunt,
win with ordinary gear, carve the intended materials, craft/wear bone armor,
cook meat, save/rejoin and repeat. The maintainer demonstrates this loop plus
dedicated-server/two-client agreement, mid-action tracking, and persistent
personal rewards. R0 + R1a + R1b + this integration constitute the first release;
each small work packet can finish without pretending the release is finished.

### R2 - Field preparation

The second release increment deepens the first hunt with **prepared meat/BBQ**,
**flashbug-to-flash-bomb progression** and **bucketed toads**. Each can be a small
packet with a source, visible result, duration/recovery and reason to carry it.
The exact BBQ interaction is future design, not permission to build a minigame
framework. Antidote/poison-counter progression is scheduled with Rathian in R4.

Preserve intentional hit-triggered, one-release endemic behavior unless an
explicit decision changes it. Capture/release preserves variants and consumes or
restores containers exactly once, without infinite clouds. Source attribution
must agree with personal carve eligibility when a deployed tool deals damage.
Reuse the existing four toad effects as the initial behavior; improve them only
within a bounded later packet. Vanilla milk and avoiding the telegraphed cloud
are the initial poison counterplay; the MH antidote arrives with Rathian in R4.
The future player-control-loss rule does not require redesigning today's
non-locking vanilla slowdown/confusion effects before any further progress.

**Exit:** each included preparation interaction has readable, bounded effects
and accessible counterplay. Any player control loss obeys section 6, including
input-based recovery and no indefinite reapplication lock. Particle polish is
not a hit/damage authority. Full slinger/ammunition and stamina systems stay out.

### R3 - The hunter's armory

The **Bone Greatsword (`BoneBlade`) is the selected first custom weapon**,
replacing the proposal's bow recommendation. Geometry exists; textures and
bound faces do not. There is no committed art delivery, fallback or date.
"Assume the art will arrive soon" is not an input an agent may mark ready.
Keep R3 undated and revisit at selection; R1, R2 and Rathian do not wait for it.

Start with one accepted weapon presentation and a bounded committed attack,
using ordinary entity and multipart damage contracts. Do not implement R8
stamina, sheathing, dodge or every greatsword combo as hidden prerequisites.
Then add armor or weapons independently as their art and useful material sources
are accepted. Bow/scythe/bowgun remain later candidates, not substitute approvals.
The `BoneBlade` filename/internal `BoneGreatsword` name is a naming difference,
not an independent art blocker; untextured geometry is the actual problem.

**Exit per item/family:** accepted visuals, survival acquisition, a clear role
and multiplayer-correct behavior. One explicit armor trait does not justify a
skill/decoration engine. Ore or a custom station enters only with actual recipes
and a new scope decision; "more armor" is the maintainer's station revisit point.

### R4 - Rathian

Rathian is the third release increment after R1 and R2. Preserve the working
bite/roar; add a distinctive measured move, readable poison and an accessible
poison counter, fitted hurtboxes and useful personal carves. A mirrored bite is
not automatically a second distinctive mechanic.

Use 2-4 readable moves as the main-encounter bar, not every Monster Hunter move.
Keep the proven live range-midpoint fitting approach unless new evidence
justifies an improvement; do not re-open its approval as a prerequisite.
Bound flight work separately if required for its agreed encounter presentation;
do not infer a blanket ground-only-flyer exception from the small-Izuchi waiver.
No new armor model is needed if the rewards have another real preparation or
equipment use. Poison duration is not itself player control loss.

**Exit:** a recognizable, fair poison encounter with usable rewards and a real
counter. Terrain, multiple players and target loss do not wedge its behavior.

### R4b - Rathalos

Separate acceptance from Rathian. Valid attack presentation and a bounded
takeoff/attack/landing cycle are required; **ground-only is not an accepted
Rathalos release**. Readable fireballs and sane terrain/target-loss recovery
matter more than an exhaustive aerial repertoire.

The snapshot's four mismatched attack clips include aerial attacks, not four
strictly melee clips as the response calls them. Missing references need
retargeting or a canonical replacement, not an automatic whole-model rewrite.
Art is uncommitted; keep this increment undated.

**Exit:** a genuinely flying, distinguishable hunt with useful carves and
accepted visual contact. Failure to finish it does not withdraw Rathian.

### R5 - Targeted part rewards

Start with **one visible break on one accepted species**. Grant an agreed bonus
carve or guaranteed part material through R1b's reward ownership, not a second
shared death-drop path. Select which bonus at R5 design time.
Severing a supported wyvern tail is a natural later extension because tail and
tail-cut assets already exist across the preserved collections.

Parent health remains ordinary health. Part durability and broken flags are
separate facts, not a second damage application or a second monster.
Decide how vanilla/other-mod attacks qualify; our own weapon classes must not
be the only weapons allowed to participate. Make a targeted break a useful
bonus, not a circular requirement to craft the only weapon that can cause it.

Do not reimplement carving here: it is an R1 requirement. Additional rewards must
preserve participant ownership and a once-only broken state. Severing and
behavior-changing breaks are later extensions, not hidden first-break scope.

**Exit:** deliberate targeting changes a hunt/reward, multiplayer never duplicates
the reward, and save/rejoin preserves broken state. A missing cut texture blocks
that visual feature, not every other encounter or piece of equipment.

### R6 - New hunting grounds

Treat these as **independent encounter packs**, not one enormous milestone.
Each needs a finished encounter, a useful carve and its preparation counterplay.
Respect custom-habitat-only spawning: decide whether a pack fits the first biome
or needs a new bounded custom habitat. Do not silently use vanilla-biome tags
as a fallback or promise every climate inside the first biome.

| Pack | Candidate assets and identity | Principal risk / prerequisite |
|---|---|---|
| Thunder | Zinogre charged/supercharged states; thunderbug visuals and fulgurbug rewards | Animation readiness, readable charging/discharge, agreed thunder-effect/counter semantics |
| Aquatic | Lagiacrus with land/swim and emissive variants; later Zamite/Zamtrios if appropriate | Measured pose envelopes, shoreline behavior and a real attack; spawning an aquatic model is not a completed hunt |
| Frost | Goss Harag's enraged/ice-blade visuals, Zamtrios, Blango/Blangonga | Source/export/animation readiness; distinct ice mechanics and counters rather than several identical slowness mobs |
| Roaming threat | Deviljho as a later dangerous visitor | A completed ordinary encounter first; bounded territory/interruption behavior rather than world-wide pursuit |

These are content groupings, not promises that all their members are ready.
Zamtrios may belong to the aquatic or frost release; it does not require both
packs to ship. Blangonga's pack behavior can be independent of Goss Harag's
ice-blade combat.

**Exit per pack:** the included species is not merely registered; a player can
find it, recognize its threat, respond, harvest a useful reward and continue.
An art-blocked pack does not stop the other packs.

### R7 - Rally and retreat

Schedule Great Izuchi rallying its **existing** escort and wounded monsters
retreating/resting at **nests or rest locations in the custom habitat**.
Agree on rally triggers, rest-site ownership and bounded failure behavior before
implementation. R1a reserves no more than this design intention: **no nest
feature, persistent anchor service or retreat system is first-release work**.
The response's art-table phrase "R1 spawn anchor" conflicts with its explicit
deferral; this roadmap follows the explicit deferral.

**Exit:** rally and retreat create recognizable decisions without infinite
reinforcements, unbounded fleeing, unreachable mandatory destinations or a
general ecosystem simulator. Neither requires all R6 species first.

### R7b - Unscheduled guide/Guild options

Field guide and optional bounties are not scheduled commitments. Reconsider if
the maintainer later wants them. They do not gate rewards, ranks or world access.
A small carve-interaction hint in R1 is not a field-guide subsystem.

### R8 - Conceptual player combat direction

Record the stated aspiration for fuller MHW-inspired player combat, including
possible stamina, dodge, sheathing and weapon movesets. No acceptance contract,
timeline or implementation packet exists. Revisit **after R3**.

An overhaul does **not inherently require** discarding agency or compatibility;
that was the response agent's interpretation, not an unavoidable consequence.
Decide its reach and opt-in/compatibility boundaries when it is designed.
Do not build an abstraction layer now, silently make it mandatory, or treat R8
as advance approval to remove controls or invalidate other mods' equipment.

## 4. Asset evidence and how it changes the roadmap

Inspected source: the `MH_NW` folder inside the supplied Drive download named
`MH_NW -20260912T005548Z-1-001`. The download folder is a point-in-time reference,
not a path other contributors should hardcode or a new canonical runtime export.
The directory was inspected read-only; no supplied art was changed or uploaded.

The snapshot has **135 files**: 87 PNGs, 20 Blockbench projects, 16 JSON files,
nine Tabula projects, one GIF, one OGG and one `desktop.ini`. Folder counts are
76 entity files, 50 item files, five block files and four effect files.
File counts do not mean distinct finished assets or implemented features.

### Equipment, materials and effects

| Evidence in this snapshot | Supported roadmap opportunity | Readiness / limitation |
|---|---|---|
| `items\equipment\bone\bone_armor.bbmodel` and `bone_armor.png` | R1 visible, craftable hunting equipment | Textured `modded_entity` source, 19 elements, 128-by-128 texture; attachment/render acceptance still required |
| `items\equipment\bone\bone_scythe.bbmodel`, PNG and older `.tbl` | R3 melee weapon candidate | Blockbench source has a bound embedded texture; item presentation and intended MH weapon class need agreement |
| `items\equipment\bone\hunter_s_bow.bbmodel` and `hunters_bow.png` | Later R3 bow candidate, not the selected first weapon | Source contains no bound texture; source resolution is 16-by-16 while the supplied PNG is 64-by-64, so binding/UVs need review |
| `items\equipment\bone\BoneBlade.bbmodel` | Selected first R3 weapon: a committed greatsword attack | Internal model name is `BoneGreatsword`; source has no textures and no bound faces; no delivery commitment |
| `items\equipment\bone\Bone_heavy_bowgun.bbmodel` | Later R3 ranged weapon with bounded ammunition/reload | Geometry/presentation transforms exist, but no textures or bound faces; do not call it finished art |
| Hide/claw/meat, wyvern scales/webbing/tails/plates/gem, Zinogre parts and fulgurbug | R1/R4/R6 harvest-and-craft identity | Mostly item sprites; useful source and recipe still need gameplay implementation |
| Raw, rare and well-done meat sprites | R1/R2 food preparation | Begin with ordinary cooking; reserve intermediate/rare meat until there is an intentional use |
| Flame/freezer/screamer/sleep sacks, monster fluid, drone substance, paddock oil | R2/R3/R6 consumable or crafting ingredients | Some are vertical animation strips; add correct metadata when imported. Ambiguous names need agreed source/use, not invented filler recipes |
| `items\bucketed animals` four variants | R2 portable endemic hunting tools | Item icons exist; variant-preserving capture/release and container behavior are still gameplay work |
| `items\fertilizers\monster_feces.png` | Later R2 dung bomb or simple fertilizer | An animated strip, not an implemented fertilizer system |
| Ore blocks, crystal clusters, raw ores and processed items | R3 later forging materials | All five supplied block PNGs and same-named ore item PNGs match the old repo byte-for-byte; these are preserved art, not proof of new mining progression |
| `effects\iceblight.png`, `lethal_poison.png`, `sleep.png`, `thunderblight.png` | R2/R4/R6 presentation for individually agreed effects | Four 18-by-18 status icons, not particle sheets or implemented effect semantics |
| Empty `particles` folder | Future VFX polish across working hunts | Intended content slot only; use approved existing/vanilla cues in the meantime |

All five equipment Blockbench sources lack authored animations. This is not
automatically a problem for armor or ordinary static items. It does mean a
charged weapon, animated reload or drawn bow presentation must be explicitly
provided or implemented; it cannot be assumed to exist inside the project.

### Creature sources and export readiness

The snapshot contains seven creature geometry/animation export pairs, seven
additional creature families represented by Blockbench sources without paired
runtime exports, and a Great Thunderbug represented by a Tabula source/texture.
Existing revival creatures not supplied here, such as flashbug and the Java bug
mesh, remain preserved repository assets; the download is not the complete mod.

| Family | Snapshot evidence | Consequence for the roadmap/interview |
|---|---|---|
| Aptonoth | Source, texture, geometry and seven exported clips | Good first-loop candidate; asset availability does not replace its remaining in-game checks |
| Great Izuchi | Source, texture, geometry and eleven exported clips | Preserve the already measured runtime version; do not replace it just because a Drive file has the same name |
| Small Izuchi | Nine snapshot clips, but five reference bones absent from the paired geometry; active runtime uses the four usable locomotion/idle/sleep clips | R1's reported exception allows no dedicated attack/death clip, not a silent broken-animation import |
| Rathian | Thirty exported clips, with unresolved exact bone names in **29**; Blockbench source contains 36 animation entries | The supplied export pair is inconsistent. Choose/re-export the canonical source instead of replacing working runtime files |
| Rathalos | Thirty exported clips, four with unresolved bone names; Blockbench source contains 38 animation entries | Reconcile the existing attack-art problem; additional source entries are not automatically additional ready attacks |
| Lagiacrus | Seven exported clips; `lagiacrus.bbmodel` has 15 animation entries, while `lagiacrusAnim.bbmodel` has one | These are materially different candidate sources. Extra swim/demo/pose entries do not establish a usable combat/death animation |
| Toads | Source, geometry, three exported clips and four texture variants | Preserve the working triggered effects; extend with portable containers in R2 |
| Blango, Blangonga, Goss Harag, Zamite, Zamtrios, Zinogre | Blockbench sources/textures but zero source animation entries in the inspected files; no paired exports here | Budget actual animation/export authoring, not merely registration code |
| Deviljho | Source has one entry named `cool pose` with keyframes; no paired export here | A posed source is not locomotion/combat readiness |
| Great Thunderbug | `.tbl` model source and texture | Import/export and behavior work; not texture-only, but not a ready Gecko entity either |
| Fireball, iceball, thunderbug blast, web shot | `.tbl` sources/textures; fireball also has a GIF | Reusable art candidates for appropriate attacks, not evidence of an implemented projectile or its owning monster |

The exported-clip check compares referenced bone names case-sensitively against
each paired geometry. Aptonoth, Great Izuchi, Lagiacrus and toad have no unresolved
names on that check. This checks references only, not MoLang playback, correct
poses, texture mapping or a fair attack.

**Important distinction:** the active revival branch's Rathian geometry/animation
pair has **zero of thirty** clips with unresolved bone names on the same check.
The 29-of-30 finding is about this downloaded snapshot, not a claim that the
working Rathian suddenly regressed. This is a concrete reason to ask Q23 about
canonical sources and not bulk-import the Drive export.

Goss Harag's base/enraged/ice-blade textures and Zinogre's base/charged/
supercharged textures are all 512-by-512. Lagiacrus's base/emissive/eye textures
are also 512-by-512. These are promising state/presentation assets, not proof
that layered rendering or state transitions already work. The single
`entities\Lagiacrus\roar.ogg` needs playback review and inclusion in the
maintainer's applicable sound/asset permissions record.

The immediate art acceptance is **bone armor for R1**. The selected future
greatsword needs textured/bound art, but nobody has committed to supply it by a
date. Small Izuchi retargeting is deferred by an explicit presentation exception;
Rathalos presentation and canonical Rathian/Lagiacrus exports remain separate
requests. Ask for the next shippable increment, not a complete boss repertoire
from every artist at once.

### Future art slots, not release dependencies

The empty equipment folders are `hunters`, `izuchi`, `metal`, `nerscylla`,
`rathalos`, `rathian`, `zamtrios` and `zinogre`. Reserve a place for these families
in R3 and their appropriate encounter packs. Their art can be integrated one
family at a time without a roadmap rewrite.

`blocks\mushroom-plant blocks` is also empty. Gathering flora is a reasonable
future R2 addition if it supplies actual remedies or recipes; it is not a
commitment to a farming system.

**A Nerscylla armor folder or web-shot projectile is not evidence of a Nerscylla
monster model.** Either wait for that encounter/material source, agree on an
explicit alternate acquisition route, or leave the family unexposed. Do not
invent a whole monster implementation solely to justify an empty folder.

### Asset acceptance policy

Use four simple labels: **present/source-only**, **exported candidate**,
**accepted in game**, and **planned/not supplied**. Presence never implies the
third label. A geometry may be present while its texture, animations, sounds or
reward-item art are independently missing.

Keep an accepted runtime model/animation pair tied to a recorded revision.
Replacing it with a same-named Drive export can invalidate the measured hurtboxes
and attack paths already proven in the revival. Do not bulk-copy the new snapshot
over the working assets.

For each near-term art request, record only the creature/item, expected export,
required states/clips, relevant texture/variant, and who can visually approve it.
Ask for the next shippable increment, not every possible animation in advance.
The maintainer reports that contributed art is by a credited local artist and
the existing **GPL-3.0** license is intended. Accept that report rather than
continuing to assert an unresolved artist-permission dispute.

There is a separate, verifiable **metadata mismatch**: at `e9b9eb7`,
`gradle.properties` still says `mod_license=All Rights Reserved`, and the
NeoForge metadata template uses that value. Align that wording with the stated
license in R0; it is repository housekeeping, not a renewed ownership interview.
The statement about contributed art does not independently settle Monster
Hunter branding/third-party rights or every sound's origin. Public distribution
remains the maintainer's responsibility; this roadmap makes no legal clearance
claim and does not require a new legal subsystem before private development.

## 5. Independent handoffs with serial delivery

Q24 chooses **one workstream at a time: maintainer plus one coding agent**.
Do not manufacture a parallel-contribution bottleneck and then refactor to fix
it. Independence means a bounded owned change with named inputs and outputs,
so a later agent can continue without inheriting an entire conversation.

| Later packet boundary | Owns | Consumes; does not redesign | Separate from |
|---|---|---|---|
| R0a baseline | Focused timing/lifecycle regressions, factual docs, current license metadata | Accepted platform and measured combat | Client seeking, new worldgen and reward mechanics |
| R0b release confidence | Late-tracking presentation and real save/multiplayer acceptance | Existing action clock, accepted attacks and R0a baseline | Redesigning server combat or holding independent R1 work |
| R1a habitat | Biome placement, selected structures, spawn/config integration | Recorded roster and current spawn/escort lifecycle | Carving and equipment rendering |
| R1b carving | Eligibility, inert corpse lifecycle, persisted counters/expiry, grants | R0 damage/death ownership and agreed material IDs | Worldgen, armor traits, future part breaking |
| R1 equipment/economy | Material uses, crafting/cooking, armor render and one selected trait | R1b grants and accepted armor export | Corpse architecture and monster AI |
| R1 escort behavior | Small-Izuchi circling/darting/fair disengagement | Existing damage and pack-spawn contracts | Rally/nests, animation retargeting |
| R1 release integration | Actual biome-to-gear loop and human evidence | Completed component packets | Inventing missing component behavior at merge time |
| R2 preparation | One complete food/tool/container interaction | Item identity, reward attribution and selected effect semantics | Global player combat systems |
| R3/R4/R6 content | One weapon, species or encounter pack | Actual materials, parts and relevant effects | All other equipment or species |

Only R0a has a detailed handoff in this revision; the other rows are packet
boundaries, not a mandate for that many PRs. Default to R1a then R1b after R0a,
but they may be developed in the opposite order if it better uses available
time. R0b's human scheduling does not gate either. R1 equipment and escort work
integrate before release.

Document only the shared facts an actual consumer needs: material/effect IDs,
damage/reward ownership, saved eligibility/counters/expiry, item consumption,
and the biome/structure selection. The save representation is an implementation
design to establish before dependent packets, not a question the maintainer must
answer in Java/NBT terms. No plugin API, generic event bus, ECS or behavior DSL.

`MHNW`, `ModEntities`, `MHNWClient` and `MHNWGameTests` remain shared integration
surfaces. The active agent wires its complete feature there under maintainer
ownership. Do not split those files preemptively or leave their edits as vague
"integration later" work. If contributors later work concurrently, separate
branches and explicit ownership come before any structural refactor.

An encounter that needs a new effect waits for **that effect**, not all of R2.
A species-specific armor recipe waits for its **actual material source**, not
the entire R6 roster. Keep integration tasks explicit so cross-stage glue is not
silently left to the last contributor.

Draft only the next ready packets, without repeating roadmap approval. Each Claude Code/
Codex handoff must name its baseline, decisions, prerequisites, owned paths,
unchanged contracts, exclusions, observable acceptance and stop conditions.
Use small tasks with concrete completion evidence, not broad "make it MH-like"
goals. A blocked art/decision/manual gate stays blocked; an agent must not
approximate it silently to satisfy a goal evaluator.
`CLAUDE.md` and `AGENTS.md` should point at the same accepted facts rather than
carry divergent copies of the product/architecture specification. No plan may
depend on a previous agent remembering a chat or on a contributor having this
machine's Downloads directory.

## 6. Cross-stage release constraints

- **Assets earn their place through gameplay.** Every survival-exposed material
  has an obtainable source and a useful sink; decorative/cosmetic items are
  clearly presented as such. Reserve unused assets without pretending they
  complete progression.
- **No circular progression.** Basic gear does not require the monster only
  that gear can damage. Essential first rewards are not gated solely by rare
  plates/gems or an unsupported severing mechanic.
- **Personal reward ownership.** Participation, three carve allowances and
  expiry are server-owned facts. Death, capture, part bonuses and containers
  have one grant/consumption path; no duplicate claims on reconnect or parts.
- **Bound player control loss.** The reported limit is at most 40 ticks
  (nominally two seconds), escapable/shortenable by player input. Refreshing or
  stacking must not turn bounded episodes into an indefinite lock. The agent
  must not disable the escape input itself. This caps **control loss**, not
  every poison, resistance or food-effect duration; exact ailment semantics
  are decided with the interaction. No forced camera, compulsory first-release
  stamina system or terrain-destroying attack is authorized.
- **Readable threats.** New elemental/status mechanics ship with warning,
  feedback and counterplay, not just an icon or particles.
- **Explicit presentation exceptions.** Small Izuchi's no-dedicated-attack/death
  clip is the reported R1 escort exception. It is not general permission to
  fabricate placeholders or accept unfinished major encounters. Rathalos must
  fly. No art delivery date may be inferred from a folder or enthusiasm.
- **Preserve measured correctness.** An asset/scale/timing change triggers the
  affected visual-contact review. Source parsing and passing GameTests cannot
  establish what the player sees.
- **Modpack coexistence.** Use normal attributes, recipes/tags, biome selection
  and configuration where possible. Keep spawning bounded; do not create global
  progression gates, permanently persistent wild populations, or mandatory
  dependencies on a particular combat/quest mod.
- **A small compatibility sample, not every combination.** Check the accepted
  standalone loop and a pinned representative NeoForge pack/profile. Do not
  claim universal compatibility or change another mod's mechanics to hide a
  conflict.
- **Evidence is layered.** Use targeted checks during iteration and a fresh
  build/full GameTest run for a coherent code delivery and before release.
  Historical passes are not a current run. Do not rebuild after prose-only
  edits or repeatedly rerun an unchanged successful suite.
  The maintainer supplies two-client, real save/restart and command-free
  survival-loop evidence. Headless tests cannot approve animation contact.

**KISS/YAGNI is about implementation scope, not vetoing the chosen game.**
Keep the biome and personal carving because the maintainer asked for them;
make their implementations bounded. Do not swap in ordinary loot as a shortcut.
Also do not build nest services, global combat frameworks, a generic corpse
platform, a database-backed reward economy, every armor skill or collaboration
infrastructure for one active workstream. Preserve accepted runtime assets,
and reuse a small helper only when a real second consumer needs it.

## 7. Decision record and overridable defaults

### A. Preserve the interview, do not restart it

Treat the interview answers as the operating direction. Original IDs are
preserved; suffix answers remain in the parent row. **PM default** means a
decision made here under the user's delegation, not a quote from the maintainer.
Use it unless the dev/maintainer records a better local choice. These are not
questions to ask again.

| ID | Interview direction | Operating interpretation / override boundary |
|---|---|---|
| Q01 | alexVR is sole scope, release and art-acceptance owner | Dev and maintainer can override PM details locally; no second workshop |
| Q02 | Open-world hunt, carve, craft and prepare | No mandatory quests or ranks |
| Q03 | R0 + R1, centered on Great Izuchi, is the first release | Includes the expanded biome/carving scope |
| Q04 | Great Izuchi, Aptonoth, small Izuchi and the Toad/Flashbug/Bug group; bone armor and food | Q04a permits no dedicated small-Izuchi attack/death clips; keep working endemic effects under Q28 |
| Q05 | Great Izuchi combat, Rathian bite/roar and the reported 69-test baseline are accepted | PM default: preserve current measured geometry and live range-midpoint methodology; improve only with evidence |
| Q06 | Current NeoForge 1.21.1 pins; no mandatory named pack | Standalone plus one representative profile, not universal compatibility |
| Q07 | Small-group survival, ordinary gear, fair but demanding, telegraphs, no one-shots | Choose reference gear/health and tune provisional developer values during R1 balance; not a guarantee for every arbitrary modded loadout |
| Q08 | Maintainer provides dedicated-server/two-client, save/restart/rejoin and command-free loop evidence | PM default: fresh build/full GameTests also required for code delivery/release; unavailable human checks stay pending without stalling independent coding |
| Q09 | Personal participant rewards; Q09a says any damage dealt | Q31 specifies attribution without adding a contribution-percentage system |
| Q10 | Carving is essential in R1; Q10a: shift+right-click, three per player, ten-minute corpse; Q10b: no death-item rewards | Q29-Q34 supply ready-to-use PM defaults |
| Q11 | Reliable basics, rare trophies later | First-kit ingredients cannot depend solely on rare rolls |
| Q12 | Vanilla crafting/smithing and cooking; no new ores/station now | Revisit a station when more armor actually exists |
| Q13 | One visible break grants a bonus carve or guaranteed material | Choose that reward in R5; severing/behavior changes stay separate |
| Q14 | Vanilla-compatible first release; fuller MHW-inspired combat is a future goal | R8 is conceptual; no present permission to change global movement/combat |
| Q15 | Bone greatsword is the first custom weapon | Texture/export art uncommitted; no fallback selected |
| Q16 | Armor values plus one modest set trait, with mixed equipment viable | Q37: iron-equivalent protection and +0.1 full-set knockback resistance |
| Q17 | R2: prepared meat/BBQ, flash bombs, bucketed toads | Existing endemic effects stay; vanilla milk/avoidance cover early poison, MH antidote arrives in R4 |
| Q18 | Brief player control loss allowed; Q18a caps it at 40 ticks with input-based shortening/escape | No indefinite refresh/stack lock; decide each effect's semantics before its packet |
| Q19 | Main encounters need 2-4 distinctive readable measured moves and fair behavior | Small Izuchi is the stated escort exception; Rathalos must fly |
| Q20 | R2 immediately after the first release; Rathian next | Later art-gated order is revisited when work is selected |
| Q21 | Rally existing Izuchi escorts; wounded monsters retreat/rest at nests | Nests are R7, not R1; guide/Guild unscheduled |
| Q22 | External art supplier has made no delivery commitment | Q22a's assumed early delivery is an assumption, not a date or fallback |
| Q23 | Maintainer selects canonical assets and approves appearance | Runtime replacements trigger affected measurement/visual review |
| Q24 | One maintainer plus one agent workstream at a time | Independent handoffs, not parallel core-file editing or speculative reorganization |
| Q25 | Credited local-artist work; GPL-3.0 is the intended existing license | Align actual packaged metadata; do not conflate artist provenance with third-party branding/sound permissions |
| Q26 | Custom-biome-only natural spawning; first-release fresh-world stance | No automatic vanilla-biome fallback, terrain retrofit or new dimension |
| Q27 | Small Izuchi hangs back/circles, then occasionally darts in | Ground harassment, no new animation prerequisite; bounded pack pressure |

### B. Defaults selected here: use, measure, override locally

| ID | PM default | Reason / limit |
|---|---|---|
| Q28 | Keep every existing toad/flashbug effect, trigger and disappearance rule | Build on working features. R2 adds capture, portable deployment and consumables; no ambient-only rollback |
| Q29 | Corpse lifetime is 12,000 entity-ticking ticks; pause unloaded/offline; save remaining ticks | Matches vanilla-style ticking semantics while keeping the requested ten minutes; no chunk tickets or wall-clock service |
| Q30 | Great Izuchi, small Izuchi and Aptonoth are carvable; each gives three personal claims. Endemic life is not carvable | One small policy, no generalized corpse conversion. Great Izuchi gives materially better hide/claw yield; Aptonoth supplies meat/hide, not every monster ingredient |
| Q31 | Eligibility requires positive damage credited to a player, their projectile/tool, or an owned/tamed attacker whose player owner is resolvable | No minimum percentage, proximity/healing credit or reconstruction of ownerless environmental damage. Preserve the actual player UUID through owned tool attacks; existing vanilla damage attribution is the first choice |
| Q32 | Each shift+right-click is one immediate carve. Main-hand interaction only, ten-tick server-side debounce, no new player animation required | Small functional ritual first. Three deliberate claims; not an unrequested held-input animation system. All client requests still pass server eligibility/range/state checks |
| Q33 | If the complete reward cannot fit, give a localized "make room" message and consume neither items nor the claim. Retain the same pending roll for retry/save | No ground overflow, mailbox, duplicate grant or reroll-on-full-inventory loophole |
| Q34 | Keep existing normal XP on death, once. Carving grants items, not additional XP | "No death loot" applies to item rewards for the selected MH species, not every Minecraft drop system |
| Q35 | `mhnw:verdant_hunting_grounds`, "Verdant Hunting Grounds": temperate wooded meadow/low hills, vanilla oak/birch and terrain blocks | No new block/art prerequisite. Initial diagnostic target: find the biome within 4,096 blocks of spawn on seeds 0, 20260911 and 8675309; tune placement locally, not a guarantee for every seed |
| Q36 | Include plains villages and ordinary mineshafts using vanilla structure eligibility/placement; respect the world's structures setting | No new structures or nests; avoid joining every structure tag indiscriminately |
| Q37 | Iron-equivalent armor protection, no toughness bonus; all four bone pieces grant +0.1 knockback resistance | One standard attribute modifier, removed when the set is incomplete; mixed equipment retains ordinary stats. No skill framework or carve multipliers |
| Q38 | Localized corpse feedback states the carve interaction, remaining claims, ineligibility, full inventory or exhaustion as applicable | Small contextual feedback, not a guide/quest system. Avoid chat spam and do not reveal another player's private rewards |

**Economy defaults:** use existing meat/hide/claw art plus vanilla bone where
appropriate; do not invent a new bone texture requirement. Core material IDs
can be `mhnw:monster_hide`, `mhnw:monster_claw`, `mhnw:raw_meat` and
`mhnw:cooked_meat`, unless the live branch already has equivalent established
IDs. R1 has reliable basics and no essential rare rolls. Pick recipe/yield
quantities during its implementation so a small number of successful hunts
produces useful gear; test the full source-to-recipe path, not just registration.

**Population defaults:** preserve Great Izuchi's single-leader spawn weight as
the starting point and its existing 1-4 escorts. Small Izuchi initially enters
natural populations through that escort path, not a second independent pack
multiplier. Start Aptonoth at weight 8/group 2-3 and each endemic type at weight
2/group 1-2 within its existing mob category; honor natural-spawn disablement.
These are R1a tuning inputs, not extra R0a changes or promises about actual
population density. Observe and adjust; weights are relative, not spawn counts.

**Future effect defaults:** preserve current non-locking effects. When real
player control loss is introduced, cap one episode at 40 ticks, allow an existing
movement/jump input to shorten it, and prevent chained indefinite locking.
Implement a small concrete effect, not a global input framework. R8 remains
future design; it does not block any current packet.

## 8. Handoffs, quota and local changes

Start with [R0a](R0_BASELINE_HANDOFF.md). It is deliberately executable
without a new art export, two-client session or another product interview.
It locks the working baseline rather than holding progress until every
pre-release concern is closed.

The next planned packet is R1a's custom habitat; then R1b's personal carving.
R0b is separate client/release work and must be completed before publishing the
first survival release, not before writing any new feature. Detailed future
packets should be grounded in the branch as it exists when assigned; do not
spend the implementation agent's quota pre-writing every R6 species now.

For each delivery, update the relevant dated section of `docs\TEST_PLAN.md`
and any changed item in `docs\DEFERRED.md`. Keep the handover short:

| Record | Necessary content |
|---|---|
| Baseline/result | Actual start and resulting revision, or uncommitted working-tree state |
| Implemented | Player/maintainer-visible change and owned files |
| Evidence | Commands/outcomes and any actual human observation; no inferred visual passes |
| Local overrides | Default ID, old/new choice and one-sentence reason, only if something changed |
| Still pending | Concrete next task or human gate, not an invitation to repeat all decisions |

Do not create a separate approval document, planning database or change-control
ceremony. For example, if a pinned dependency exposes a different method than
expected, use the supported equivalent and record it. If play shows +0.1
knockback resistance is unhelpful, tune it with the maintainer. If the only
available approach would replace global worldgen or discard accepted models,
escalate that material change instead of hiding it in an implementation detail.

Use one main agent, small coherent edits, targeted checks during iteration and
a final full build/GameTest run for code changes. Subagents are optional, not a
quota entitlement. Prefer a single precise lookup over another agent re-reading
the whole repository. Stop cleanly after the assigned packet; do not continue
through the roadmap merely to use the rest of a rolling quota window.

## 9. Evidence and corrections used in this revision

| Evidence | What it establishes; what it does not |
|---|---|
| Returned response, SHA-256 recorded above | Interviewed product direction; later user instruction says to proceed with sensible, locally overridable defaults instead of another approval round |
| [Current baseline 3ca5d46](https://github.com/Carro1001/MonsterHunter-NewWorld/tree/3ca5d46d11a60323170cbd2e7777879af3c421c1) | Latest inspected remote head, retained roar fix, corrected death/goal semantics and committed interview response |
| [Revival commit e9b9eb7](https://github.com/Carro1001/MonsterHunter-NewWorld/commit/e9b9eb7e1a1af8e34bc942218f76684bbbf8f7a2) | Pushed revision, per-tick roar fix and updated notes; reported 69 passing tests, not a new run by this roadmap author |
| `Toad`, `ToadFuseGoal`, `FlashbugFlashGoal` | Already-working effects and disappearance, preserved under the explicit instruction to build on the revival wholesale |
| `gradle.properties` and the NeoForge metadata template at that revision | Packaged license still derives from `All Rights Reserved`; separate from the maintainer's statement about artist permission |
| [Minecraft item despawning](https://minecraft.wiki/w/Item_(entity)#Despawning) | Ordinary item lifetime is 6,000 entity-ticking ticks/five minutes; the requested corpse lifetime stays ten minutes |
| [NeoForge 1.21.1 biome modifiers](https://docs.neoforged.net/docs/1.21.1/worldgen/biomemodifier/) | Adding spawns/features to biomes is distinct from proving a new biome participates in Overworld placement; shared vanilla features also need ordering care |
| Section 4's v2 snapshot audit | Downloaded Rathian exports are inconsistent while the inspected revival pair is not; source availability is not canonical runtime acceptance |

No runtime, dependency or supplied asset was changed for this revision. Future
plans must recheck the actual revival head and approved decisions; these pinned
facts are an evidence baseline, not a request to revert newer contributor work.
