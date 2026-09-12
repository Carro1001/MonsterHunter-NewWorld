# Monster Hunter: New World - product roadmap

**Status: decision-workshop draft, not an approved roadmap or implementation
authorization.**
Proposal ID: `mh-nw-roadmap-2026-09-11-v1`.
Prepared 2026-09-11 from the revival branch and the supplied Google Drive export.
The requester is gathering a proposal for the project decision-makers, not
approving features on their behalf. Product direction, stage contents, ordering
and recommended defaults require the brother/maintainer's decisions.

**Handoff sequence:** proposal -> maintainer interview -> decision-response
draft -> finalized roadmap -> separately scoped implementation plans.
Do not skip the middle steps.

## 0. Instructions to the receiving Claude Code or Codex agent

Your job in this session is **decision discovery**, not implementation. Interview
the maintainer using the proposal and the question register in section 7, then
produce the response artifact specified in section 8 for them to send back to
the roadmap author. The proposal's detail is intended to make decisions easier;
it does not mean those decisions have already been made.

1. Read the whole proposal and check the current revival revision/status without
   editing runtime code. Separate repository evidence, human reports, proposed
   defaults and confirmed decisions. If this proposal is stale, record the
   correction and source in the response rather than silently assuming a rewrite.
2. Confirm who can make scope decisions and who must approve art or releases.
   Do not claim team agreement if only one contributor has expressed a preference.
3. Ask **one focused question at a time**. Use the tool's question interface when
   available. Explain the real tradeoff, give the proposal's recommended default,
   and offer a small number of alternatives. Wait for the answer before treating
   any choice as accepted.
4. Start with Q01-Q08. Then follow the selected scope through the relevant later
   questions. Probe ambiguous answers: "all monsters eventually" does not define
   the first release, and "proper sleep" does not define acceptable player control.
   Ask for player-visible examples and what is explicitly excluded.
5. Challenge incompatible combinations constructively. For example, demanding
   every monster set in the first release conflicts with unknown art delivery;
   promising independent agents while all can edit combat core conflicts with
   clear ownership. Present the conflict and let the maintainer choose a tradeoff.
6. Do not re-ask an answered question verbatim. Summarize the answer, rationale,
   affected stages and follow-up. Conditional questions may be deferred or marked
   out of scope, but must not disappear from the response.
7. Keep all unconfirmed suggestions labeled. Never convert silence, a folder
   name, a model's recommendation or a requester's preference into approval.
   Ask the maintainer to confirm your summary of consequential decisions.
8. Produce `docs\ROADMAP_DECISIONS_RESPONSE.md` using section 8. If that file already
   contains someone else's response, preserve it and use a named revision rather
   than overwriting it. If repository writing is unavailable, return the same
   Markdown as a downloadable/copyable response.
9. Stop after the decision response. **Do not implement features, generate the
   stage implementation plans, change dependencies, modify art, push, publish,
   or rewrite the accepted runtime under the guise of preparing the roadmap.**

Use subagents only if a concrete factual uncertainty needs investigation.
Keep the human interview with the main agent; do not launch parallel agents to
invent product decisions or interrogate different contributors without agreement.

Starter prompt for the maintainer:

```text
Read docs\ROADMAP.md and conduct the maintainer decision workshop described in
section 0. This is not an implementation task. Interview me one question at a
time, challenge unclear scope/tradeoffs, and record my confirmed decisions.
Then produce docs\ROADMAP_DECISIONS_RESPONSE.md using section 8 so I can send
it back to the roadmap author. Do not finalize the roadmap or begin coding.
```

## 1. Product direction

**An open-world hunting mod: discover wildlife, prepare, hunt, harvest, craft,
and choose a harder encounter.** It should fit into an existing Minecraft
survival world or modpack, not require abandoning that world for instanced
missions, a new dimension, or a replacement combat/progression system.

The requester favors open-world hunting and crafting, with optional Guild
bounties later. This is the **recommended direction for maintainer confirmation**,
not an assertion that the team has approved it. Proposed constraints are:

- Keep Minecraft 1.21.1 / NeoForge and the working revival architecture. Do not
  restart the rewrite or reopen the loader decision without a concrete blocker.
- Old Forge saves were not a migration requirement for the revival. Confirm
  whether any new public release changes that obligation.
- Art arrives asynchronously. An empty named Drive folder is a future-content
  signal, not a finished asset or a promise of a delivery date.
- Stages should support different contributors using Claude Code/Opus or
  Codex/Sol. Shared contracts and observable acceptance matter more than the tool.
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
2. **Additive progression, not mandatory replacement.** Vanilla and other mods'
   gear can participate. MH equipment offers themed choices, not an obligatory
   prerequisite for dealing damage.
3. **Not every asset needs its own subsystem.** A fertilizer texture can support
   fertilizer or a dung bomb; it does not justify a farming simulation. A bowgun
   model does not justify implementing every ammunition family at once.

## 2. Where the project actually is

The working branch **does exist remotely**:
[`revival/neoforge-1.21.1`](https://github.com/Carro1001/MonsterHunter-NewWorld/tree/revival/neoforge-1.21.1).
The inspected revision is
[`b2a0da4`](https://github.com/Carro1001/MonsterHunter-NewWorld/tree/b2a0da4b0ce73507c6e2d70f076df31c369057be),
dated 2026-09-11. The planning worktree and `master` still contain the legacy
runtime; future implementation plans must start from the accepted revival
revision, not this planning worktree's old Java files.

| Area | Evidence at the inspected revival revision | Roadmap consequence |
|---|---|---|
| Foundation | NeoForge 21.1.248, Java 21, GeckoLib 4.9.2, native parts, server-owned attack timelines, runtime bone-measurement tooling | Preserve it; stage R0 is reconciliation and hardening, not another port |
| Great Izuchi | Three measured attacks, health/death, fitted hurtboxes, opening roar; maintainer confirmed the core combat visually | This is the first survival hunt, not another prototype to rebuild |
| Rathian | Measured right bite plus a mirrored left-bite candidate, range-aware selection, fitted hurtbox offsets and opening roar | Finish an iconic encounter incrementally; old summaries calling it vanilla-melee-only are stale |
| Rathalos | Ground baseline, opening roar, hurtboxes borrowing Rathian measurements; attack-art mismatches and flight remain | Not yet a finished flying-wyvern encounter |
| Lagiacrus | Amphibious pursuit baseline, seven provisional hurtboxes, no outgoing attack; water-to-land bank exit is a documented limitation | Treat fitting, movement and combat as remaining work, not an accepted species |
| Other wildlife | Aptonoth, small Izuchi, toads, flashbug and bugs have implementations; several visual/behavior checks remain | Bring them into survival selectively instead of rewriting their existing logic |
| Survival economy | No active harvested-material/armor progression or survival recipe/loot data was found in the inspected active resource/registration scan | This is the largest missing player-facing loop |
| Release confidence | Branch notes report 69 passing GameTests; natural spawning, real save/restart, two-client agreement and mid-action tracking still have open work | These are release gates, not cosmetic polish |

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
background. This proposed roadmap changes its *product sequencing*: do not wait
for every old monster to be finished before implementing useful drops and gear.
It does not erase the old handoff's damage, authority, or multiplayer requirements.

## 3. Stage map

`R` identifiers are roadmap stages, distinct from the original handoff's `P`
packets. Dependencies describe required capabilities, not a demand that one
person do everything serially. Art preparation and isolated component work can
proceed before a release gate; dependent gameplay cannot be marked accepted
until its prerequisite works.

| Stage | Player-facing outcome | Required before integration | Not included |
|---|---|---|---|
| **R0 - Trust the current hunts** | Existing encounters behave consistently in survival and multiplayer | Current revival implementation | New foundation, all-monster rewrite |
| **R1 - The first hunting loop** | Find a Great Izuchi, hunt it, obtain useful materials, craft and wear bone equipment, prepare for another hunt | R0 for release; material/recipe agreements for parallel development | Rare-drop grind, all weapon classes, mandatory quests |
| **R2 - Field preparation** | Food, useful endemic life and consumables change how a hunt is approached | R1's item/material conventions; existing endemic-life implementations | Global stamina overhaul, hard player-control locks, generic effect engine |
| **R3 - The hunter's armory** | A small number of distinct weapon choices and armor traits use the equipment art | R1; accepted art for each item | All MH weapons, large skill trees, mandatory combat-mod dependencies |
| **R4 - Wyvern hunts** | Rathian and then Rathalos offer distinct, readable poison/fire/flight encounters with useful rewards | R1; only the relevant R2 counter/effect capability | Every aerial move, full turf-war simulation |
| **R5 - Targeted part rewards** | Choosing where to attack can break or sever a supported part and change rewards | R1 reward ownership and one accepted multipart encounter; severing needs suitable art | Mesh cutting, universal destruction framework, required part gates on all gear |
| **R6 - New hunting grounds** | Thunder, aquatic and frost encounters expand preparation and loot choices | R1 plus each encounter's actual effect/movement needs | All packs shipping together or every asset being release-ready |
| **R7 - A living hunting world** | Bounded pack behavior, retreat/re-engagement and optional Guild bounties give repeated hunts context | Accepted hunts and stable rewards; not completion of the entire roster | Instanced missions, mandatory ranks, a new dimension or economy |

**Proposed release order, awaiting Q03/Q04:** R0 + R1 produce the first survival alpha.
Then R2 and one useful R3 item can progress alongside Rathian in R4.
R5 adds a signature hunting mechanic without blocking initial gear.
Rathalos and individual R6 packs graduate as their movement and art become
ready. R7 is a later enrichment track, not a prerequisite for enjoying the mod.

### R0 - Trust the current hunts

Preserve the current Great Izuchi win and the measured attack/hurtbox work.
Close the highest-risk seams before relying on them for a persistent economy:
real world save/restart, parts cleanup, dedicated-server/two-client behavior,
and tracking an attack already in progress. The branch explicitly notes that a
newly tracking client's clip can start at frame zero; that must not be called
correct synchronization just because server damage is correct.

Demonstrate controlled natural spawning, disabled-spawn configuration and
appropriate population behavior. Choose which implemented creatures are exposed
in survival. Unfinished species can remain developer-only until accepted.

**Exit:** a named, recorded revival revision is a reliable baseline; the first
release roster has no undisclosed critical behavior limitations. Other lanes
can develop in parallel, but a multiplayer economy does not ship on unverified
duplicate-damage, death or save semantics.

### R1 - The first hunting loop

Build a deliberately short loop around **Great Izuchi, small Izuchi where its
presentation is approved, and Aptonoth**. Use ordinary loot/crafting/cooking and
the available material textures: hide, claw, meat and suitable bone ingredients.
Give a player a discoverable way to learn what the materials make.

Make the existing **bone armor** craftable and wearable, with sensible baseline
protection and at most one modest hunting-oriented trait. A theme-appropriate
use for Izuchi hide/claws in construction or repair makes the monster worth
hunting without waiting for the currently empty Izuchi-armor folder.
Keep ordinary vanilla equipment viable during the first hunt.

Use raw/cooked meat through a simple preparation path. A BBQ timing minigame
is an optional later improvement, not the price of using three meat textures.
Rare plates and gems are not mandatory ingredients in the basic loop.

Start with ordinary crafting and existing Minecraft resources where sensible.
New ore world generation, a smithy interface and specialized weapon logic are
not prerequisites for obtaining the first meaningful reward.

**Exit:** a fresh survival player can discover a hunt without commands, win with
available equipment, receive the intended materials once, craft a useful item,
equip/use it, save/rejoin and repeat. A two-player hunt has a clear reward policy.

### R2 - Field preparation

Give existing toads, flashbugs and insects practical hunting uses. Preserve the
current intentional hit/interact-triggered release behavior unless the team
explicitly revises it. Bucketed-toad textures support collection and deployment;
capture/release must consume or restore the container exactly once and preserve
the variant without creating reusable infinite clouds.

Introduce **one complete preparation interaction at a time**: source, visible
effect, counter, duration/recovery, and a reason to carry it. Examples include
poison plus an antidote, a deliberate flash opportunity, and prepared meat.
The four supplied effect icons are useful presentation assets, not a complete
design for four new game systems.

Natural follow-ons are dung bombs from monster feces, sonic tools from screamer
sacks, and frost/thunder counters when their encounters actually exist.
Use throwing items or ordinary consumption first; none requires a full slinger
or ammunition framework.

**Exit:** each included effect has a gameplay source, a readable warning/state
and an accessible response. It works on the intended players/mobs without
retrigger loops, unavoidable permanent crowd control or terrain grief.
Particles enhance readable mechanics; they do not decide hits or damage.

### R3 - The hunter's armory

Develop each weapon style independently against ordinary entities and existing
multipart damage contracts; it should not require a particular future monster
class to compile or function.

The supplied **Hunter's Bow** is a sensible low-risk first ranged option once
its texture binding is completed: reuse Minecraft's bow behavior before adding
coatings. The **Bone Greatsword** can later justify a committed charged strike.
The **Bone Scythe** has a textured source, but its intended MH weapon class needs
artist/design confirmation; do not invent a fifteenth weapon class from a
filename. The **heavy bowgun** comes later because reload/ammo/presentation add
real scope and its supplied source is untextured.

Introduce armor families as their art lands: hunter's/metal/bone as practical
foundations, then monster-derived choices. Prefer a small number of explicit
piece or set traits with tradeoffs over a decoration/talisman/skill-point engine.
Resistance or preparation benefits should make choices relevant without turning
new sets into unconditional upgrades over every other mod's armor.

Mining should enter when it supplies an actual recipe. Machalite, dragonite,
carbalite and crystal assets can support later forging steps, but do not add
three unrelated grind tiers simply to consume three ore textures. Every exposed
ore needs a use, availability rule and compatibility-friendly recipe path.

**Exit:** each shipped item has accepted visuals, a survival acquisition route,
a clear role and a multiplayer-correct action/effect. An armor set or weapon
family can ship without waiting for the entire armory.

### R4 - Wyvern hunts

Finish **Rathian first** from the existing bite implementation: an additional
distinct move, deliberate poison/fire opportunities and corresponding counters
make it more than an enlarged melee mob. Finish fitting any approximate boxes
before balancing reach around them. Flight is its own bounded increment, not
something to hide inside a larger melee task.

Then give **Rathalos** a real flying-wyvern identity: valid attack presentation,
a bounded takeoff/attack/landing cycle, readable fireballs, and reliable behavior
when terrain or target loss prevents the preferred action. Do not ship endless
hovering or a grounded reskin as if the flying encounter were complete.

Use the scale/webbing/plate/tail/sack assets for useful rewards. A whole new armor
model is not mandatory for an encounter release: a real counter, consumable or
equipment recipe can provide its first reward. No active loot item should be
sold to players as meaningful progression when it has no use yet.

**Exit:** the two hunts demand distinguishable responses, have usable rewards,
and behave fairly near terrain and other players. Each wyvern can be accepted
separately; neither blocks the entire armory or all other habitats.

### R5 - Targeted part rewards

Start with **one actual part interaction**, not a universal part simulator.
A supported break should be visible, occur once, and grant a defined bonus.
Severing a supported wyvern tail is a natural later extension because tail and
tail-cut assets already exist across the preserved collections.

Parent health remains ordinary health. Part durability and broken flags are
separate facts, not a second damage application or a second monster.
Decide how vanilla/other-mod attacks qualify; our own weapon classes must not
be the only weapons allowed to participate. Make a targeted break a useful
bonus, not a circular requirement to craft the only weapon that can cause it.

Start with ordinary kill rewards plus a part bonus. Corpse carving can follow if
the team wants the ritual, but its ownership, despawn, persistence and multiplayer
rules must be settled before it replaces simple loot.

**Exit:** deliberate targeting changes a hunt/reward, multiplayer never duplicates
the reward, and save/rejoin preserves broken state. A missing cut texture blocks
that visual feature, not every other encounter or piece of equipment.

### R6 - New hunting grounds

Treat these as **independent encounter packs**, not one enormous milestone.
Each needs a finished encounter, at least one useful reward and its preparation
counterplay. New dimensions are unnecessary; appropriate existing biome tags
and controlled spawning are sufficient.

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

### R7 - A living hunting world

Once individual hunts work, add bounded ecological behavior where it creates
interesting decisions: Great Izuchi rallying its *existing* escort, a monster
retreating or resting, and limited re-engagement after a player disengages.
Avoid endless reinforcement generation or a generalized ecosystem simulation.
The currently unwired rally clip needs an agreed trigger before implementation.

Add optional **Guild bounties or a field guide** to help players find available
hunts and identify rewards. Start with a few clear objectives over existing
world encounters. No required hunter rank, quest-only damage eligibility,
instanced arena, currency economy or new dimension.

**Exit:** players have more reasons and information to revisit working hunts,
while the original open-world loop remains playable without the optional Guild
layer.

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
| `items\equipment\bone\hunter_s_bow.bbmodel` and `hunters_bow.png` | R3 initial bow, coatings later | Source contains no bound texture; source resolution is 16-by-16 while the supplied PNG is 64-by-64, so binding/UVs need review |
| `items\equipment\bone\BoneBlade.bbmodel` | R3 committed greatsword-style attack | Internal model name is `BoneGreatsword`; source has no textures and no bound faces |
| `items\equipment\bone\Bone_heavy_bowgun.bbmodel` | Later R3 ranged weapon with bounded ammunition/reload | Geometry/presentation transforms exist, but no textures or bound faces; do not call it finished art |
| Hide/claw/meat, wyvern scales/webbing/tails/plates/gem, Zinogre parts and fulgurbug | R1/R4/R6 harvest-and-craft identity | Mostly item sprites; useful source and recipe still need gameplay implementation |
| Raw, rare and well-done meat sprites | R1/R2 food preparation | Begin with ordinary cooking; reserve intermediate/rare meat until there is an intentional use |
| Flame/freezer/screamer/sleep sacks, monster fluid, drone substance, paddock oil | R2/R3/R6 consumable or crafting ingredients | Some are vertical animation strips; add correct metadata when imported. Ambiguous names need agreed source/use, not invented filler recipes |
| `items\bucketed animals` four variants | R2 portable endemic hunting tools | Item icons exist; variant-preserving capture/release and container behavior are still gameplay work |
| `items\fertilizers\monster_feces.png` | Later R2 dung bomb or simple fertilizer | An animated strip, not an implemented fertilizer system |
| Ore blocks, crystal clusters, raw ores and processed items | R3 later forging materials | All five supplied block PNGs and same-named ore item PNGs match the old repo byte-for-byte; these are preserved art, not proof of new mining progression |
| `effects\iceblight.png`, `lethal_poison.png`, `sleep.png`, `thunderblight.png` | R2/R6 readable ailments | Four 18-by-18 status icons, not particle sheets or implemented effect semantics |
| Empty `particles` folder | Future VFX polish across working hunts | Intended content slot only; use approved existing/vanilla cues in the meantime |

All five equipment Blockbench sources lack authored animations. This is not
automatically a problem for armor or ordinary static items. It does mean a
charged weapon, animated reload or drawn bow presentation must be explicitly
provided or implemented; it cannot be assumed to exist inside the project.

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
Record provenance and permission for contributed art and sound before publishing.
The existing license/provenance disagreement is still a distribution gate.

## 5. How stages can actually be independent

Independence means an accepted input and a bounded owned change, not zero shared
code. The current revival centralizes registration in `MHNW`/`ModEntities`,
renderers in `MHNWClient`, and tests in `MHNWGameTests`. Giving several agents
those entire files at once would create predictable conflicts.

| Workstream | Owns | Consumes rather than redesigns | Can progress alongside |
|---|---|---|---|
| Baseline/integration | Shared entity/damage contracts, build, release gates, final registration wiring | Measured species behavior and existing platform | Art, recipe design, isolated item work |
| Materials/recipes | Item identity, sources/sinks, loot/crafting/cooking data | Ordinary parent death/reward ownership | Equipment rendering and encounter tuning |
| Equipment | Accepted armor/item visuals and that item's bounded action/trait | Agreed material IDs, standard damage/effect rules | Other independent equipment families and monster behavior |
| Effects/endemic tools | One named effect/counter or capture/release interaction | Existing attack/application and item-use entry points | Creature animation preparation and weapon rendering |
| Encounter | One species' goals, measured moves, spawning and reward integration | Agreed materials, parts and relevant effects | Another species without shared movement/core edits |
| Art/VFX | Source/export pairs, textures, approved animation or cue revisions | Agreed action identity/timing and visual brief | Most gameplay work, until an accepted asset is required |

Before parallel implementation, agree on the smallest necessary shared facts:
material/effect IDs, damage and reward ownership, the relevant save fields,
and which layer consumes an item. These are not a request for a plugin API,
universal event bus, ECS or data-driven behavior language.

**Concrete parallel opportunities**

- After R1's material names are fixed, bone armor rendering, loot/cooking data
  and the existing Great Izuchi encounter can be worked on separately.
- R2 poison/counter work and R3 bow presentation can proceed independently.
- Rathian's next measured attack and a Zinogre source-animation pass can proceed
  independently; two agents changing shared combat state cannot.
- A frost armor export does not depend on Lagiacrus shoreline work.
- Part-breaking mechanics can be proven on one accepted encounter while other
  encounters remain on ordinary parent damage.

An encounter that needs a new effect waits for **that effect**, not all of R2.
A species-specific armor recipe waits for its **actual material source**, not
the entire R6 roster. Keep integration tasks explicit so cross-stage glue is not
silently left to the last contributor.

Use separate contribution branches based on a recorded integration revision.
One owner serializes shared registration/core changes. If the centralized test
or renderer files become an actual collaboration bottleneck, split the relevant
feature sections once; do not preemptively reorganize the whole codebase.

Later stage plans will be tool-neutral documents with a baseline, inputs,
owned paths, exclusions, dependencies, observable acceptance and stop conditions.
`CLAUDE.md` and `AGENTS.md` should point at the same accepted facts rather than
carry divergent copies of the product/architecture specification. No plan may
depend on a previous agent remembering a chat or on a contributor having this
machine's Downloads directory.

## 6. Proposed cross-stage release rules

- **Assets earn their place through gameplay.** Every survival-exposed material
  has an obtainable source and a useful sink; decorative/cosmetic items are
  clearly presented as such. Reserve unused assets without pretending they
  complete progression.
- **No circular progression.** Basic gear does not require the monster only
  that gear can damage. Essential first rewards are not gated solely by rare
  plates/gems or an unsupported severing mechanic.
- **Co-op is first-class.** Decide shared drops versus personal rewards before
  the economy lands. Death, capture, part bonuses and containers have exactly
  one owner; no duplicate grants on reconnect or overlapping parts.
- **Preserve vanilla agency.** No forced camera, permanent input lock, surprise
  compulsory stamina system or terrain-destroying attack by default.
- **Readable threats.** New elemental/status mechanics ship with warning,
  feedback and counterplay, not just an icon or particles.
- **Graceful art staging.** Empty folders do not block unrelated releases;
  missing attack presentation blocks acceptance of that attack. Temporary
  presentation must be explicitly approved and labeled, never passed off as
  finished animation.
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

## 7. Open-question register

Every question needs a row in the decision response. Use `confirmed`, `modified`,
`rejected`, `deferred`, or `unknown`. A deferred decision needs an owner and a
revisit condition. "Not relevant because stage X was excluded" is an acceptable
recorded answer, not a reason to delete the question.

The prompts below are topic-level questions. If an answer raises several separate
choices, split the follow-up into individual questions rather than demanding
approval of a bundle. Exact damage numbers, Java APIs and recipe quantities are
later implementation/balance work unless essential to a scope decision.

### A. Establish authority, product and first release

| ID | Question for the maintainer | Proposed starting point / tradeoff | Affects |
|---|---|---|---|
| Q01 | Who has authority to approve the roadmap and resolve disagreements? | Identify the decision owner; separately note any required contributor/art/release sign-off | All stages |
| Q02 | Is the open-world hunt/harvest/craft/preparation loop the intended identity? | Recommended; alternatives are quest-led progression or modular content with no authored loop | Product direction |
| Q03 | What is the smallest first survival release the team would be proud to share? | A Great Izuchi-centered loop with useful drops, bone armor and food; alternatively require Rathian too, or finish monsters first | R0/R1 sequencing |
| Q04 | Which exact creatures, gear and mechanics are mandatory in that first release? | Name the minimum set and explicit exclusions; "all current assets" is not a bounded answer | Release roster and dependencies |
| Q05 | Which current revival results are accepted, and which still need evidence? | Inspect current branch; distinguish playable Great Izuchi from open multiplayer/spawn/late-tracking and species-specific gaps | R0; avoid duplicate work |
| Q06 | Which Minecraft/loader and favorite modpacks must coexist with the mod? | Preserve NeoForge 1.21.1 unless a named compatibility need changes the decision; do not promise every combination | Platform and compatibility gate |
| Q07 | Who is the first release for, and how demanding should a hunt feel? | Small-group survival players using ordinary gear; choose accessibility versus strict MH-style punishment without inventing exact balance numbers | Progression, telegraphs, preparation |
| Q08 | What evidence is required before calling a release ready? | Automated server behavior plus real client, two-client, save/restart and survival-loop observation; identify who can run each | All release gates |

### B. Decide how hunting produces progression

| ID | Question for the maintainer | Proposed starting point / tradeoff | Affects |
|---|---|---|---|
| Q09 | How should rewards be shared in a cooperative hunt? | Shared physical loot is simplest; personal/participation rewards are possible but require eligibility and duplication rules | R1/R5/R7 |
| Q10 | Is corpse carving essential to the first hunting loop? | Ordinary kill loot first, part bonuses next, carving later; carving adds persistence, ownership and interaction work | R1/R5 |
| Q11 | What makes hunting progression satisfying rather than repetitive grind? | Reliable basic materials and useful choices; rare trophies/plates as later goals, not mandatory first-kit bottlenecks | R1/R3/R4/R6 |
| Q12 | How much new gathering and forging belongs in the first release? | Existing crafting/cooking/resources first; add ore or a smithy only when it supports an agreed useful recipe loop | R1/R2/R3 |
| Q13 | What should targeted part damage do beyond reducing parent health? | One visible break with a reward bonus first; severing and move changes only for supported species/art | R5 and related encounters |

### C. Decide combat, gear and preparation scope

| ID | Question for the maintainer | Proposed starting point / tradeoff | Affects |
|---|---|---|---|
| Q14 | Should combat remain vanilla-compatible or become a broader player combat overhaul? | Keep normal movement/combat, add specific MH weapon actions; global stamina/dodge/combo systems are a separate scope | R2/R3 and mod compatibility |
| Q15 | Which single weapon should establish the first custom equipment increment? | Bow is mechanically simple after UV work; scythe has textured art but unclear class; greatsword/bowgun need textures and more mechanics | First R3 packet; art priority |
| Q16 | How should armor differ beyond appearance and armor value? | A few explicit piece/set traits or resistances, with mixed equipment viable; avoid a full decoration/talisman engine initially | R1/R3 |
| Q17 | Which endemic tools and consumables are essential rather than nice-to-have? | Select a small useful set; existing toads/flashbug + poison counter/food before many gadgets | R2 |
| Q18 | How faithfully should MH ailments behave, especially on players? | Decide one relevant ailment at a time; ask explicit follow-ups about sleep, stun/control limits and whether missing stamina/stun systems are in scope | R2/R6 |
| Q19 | How much monster authenticity is required for an encounter to ship? | A small distinctive move set and fair behavior, not every MH move; explicitly decide ground-only/temporary-animation exceptions | R4/R6 and art gates |

### D. Decide expansion order, art commitments and delivery

| ID | Question for the maintainer | Proposed starting point / tradeoff | Affects |
|---|---|---|---|
| Q20 | Which encounter or habitat should follow the first release? | Finish Rathian, then choose a ready thunder/aquatic/frost increment; artists' confirmed readiness can change the order | R4/R6 |
| Q21 | Which ecological behaviors or Guild features are worth scheduling? | Bounded existing-pack rally/retreat and optional bounties later; no instanced missions or mandatory rank system by default | R7 |
| Q22 | Which art deliverables can contributors actually commit to next? | Separate present, being worked on, and merely intended; an empty folder is not a schedule commitment | Asset-critical paths |
| Q23 | Who chooses canonical asset versions and accepts their in-game appearance? | Record source/export revision, visual approver and measurement implications before replacing working files | All art and contact geometry |
| Q24 | Who can own the near-term workstreams, and what parallel load is realistic? | One integrator, bounded disjoint tasks; no assumed full-time capacity or deadlines inferred from enthusiasm | Workstream splits and delivery |
| Q25 | Who will resolve permissions/licensing and authorize distribution? | Establish code/art/sound provenance and license wording before publishing; do not infer permission from Drive access | Public release |

During the interview, explicitly revisit the possible conflict between Q04's
release wishlist, Q22's uncertain art delivery and Q24's capacity. Record whether
to reduce the first release, accept a labeled temporary presentation, or wait.
The agent must not make that choice by quietly downgrading a requirement.

## 8. Decision-response draft to return

The receiving agent writes a **decision response**, not a replacement roadmap.
The response should be self-contained enough for another agent to finalize the
roadmap without the interview transcript. Quote or accurately summarize the
maintainer's decisions; do not include invented consensus.

Use this structure in `docs\ROADMAP_DECISIONS_RESPONSE.md`. Populate the stage
table for R0-R7 and the question register for Q01-Q25. Add Q26+ for genuinely new
questions without renumbering the originals.

```markdown
# Monster Hunter: New World - roadmap decision response

Status: DRAFT FOR MAINTAINER CONFIRMATION
Proposal reviewed: docs\ROADMAP.md, version/date and commit or file hash
Interview date:
Decision-maker(s) and authority:
Interviewing agent/tool:
Current implementation branch and commit:
Asset snapshot(s) considered:

## 1. Confirmation and unresolved authority
- Who reviewed this response?
- Which decisions are confirmed by the authorized maintainer?
- Which still require another person's approval?
- Do not mark the response confirmed until the maintainer confirms the summary.

## 2. Agreed product direction
- Intended player experience and audience:
- Compatibility targets / named must-have mods or packs:
- Explicit non-goals:
- Any changes from the proposal, with reasons:

## 3. First release contract
- Included creatures:
- Included gear, consumables and progression mechanics:
- Player-visible end-to-end loop:
- Required evidence / who can provide it:
- Explicit exclusions:
- Art or external blockers:
- First release versus later wishlist must be distinguishable.

## 4. Question-by-question decisions
| ID | Status | Maintainer answer | Rationale / tradeoff | Affected stages | Unresolved point / owner / revisit condition |
|---|---|---|---|---|---|
| Q01 | confirmed / modified / rejected / deferred / unknown | ... | ... | ... | ... |

Include all Q01-Q25, even if deferred or not relevant to the chosen scope.
For consequential decisions, say whether the maintainer confirmed the agent's
summary or whether it is still an interpretation awaiting confirmation.

## 5. Proposed stage disposition
| Stage | Keep / modify / split / defer / drop | Player outcome | Required inputs | Required output / acceptance | Release priority | Suggested owner |
|---|---|---|---|---|---|---|
| R0 | ... | ... | ... | ... | ... | ... |

Include R0-R7. If splitting a stage, use suffixes such as R3a/R3b.
Explain changes to dependencies; do not just supply a reordered wishlist.

## 6. Art and content commitments
| Asset / family | Present readiness | Desired use | Confirmed next deliverable | Owner / approver | Commitment versus aspiration | What can ship without it |
|---|---|---|---|---|---|---|

Do not invent dates. Record canonical source/export choices and which existing
measured attacks would require revalidation if an asset changes.

## 7. Parallel work and integration
- Workstreams that can start independently:
- Shared decisions/interfaces that must settle first:
- Integrator and ownership of shared files:
- Contributor availability actually stated:
- Any staged work or artifacts already completed elsewhere:

## 8. Contradictions, deferred decisions and risks
| Issue | Why it matters | Affected stage | Owner | Required decision/evidence before work |
|---|---|---|---|---|

## 9. Requested roadmap changes
- Exact sections/stages to change and why:
- Rejected recommendations:
- New agreed features:
- Assumptions that must remain unapproved:

## 10. Readiness for finalization
- Ready for roadmap finalization: yes / partially / no
- Missing approvals or decisions:
- Which near-term stages are ready for implementation-plan drafting:
- Which stages must remain conceptual:
- No implementation has been authorized merely by completing this response.
```

The roadmap author will use the confirmed response to revise priorities,
dependencies, acceptance boundaries and the asset-critical path. Only after
that roadmap is agreed should the next small set of logically independent
implementation plans be written. Far-future content remains conceptual until
its decisions and inputs are stable.
