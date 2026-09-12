# Monster Hunter: New World - roadmap decision response

Status: DRAFT FOR MAINTAINER CONFIRMATION
Proposal reviewed: `docs/ROADMAP.md`, proposal ID `mh-nw-roadmap-2026-09-11-v1`, prepared 2026-09-11
Interview date: 2026-09-11
Decision-maker(s) and authority: **alexVR (guiguivelez@gmail.com) - sole decision owner** for scope,
ordering, release, and art acceptance (Q01, Q23). No second approver named.
Interviewing agent/tool: Claude Code (Opus 5), decision-workshop session, no runtime code modified
Current implementation branch and commit: `revival/neoforge-1.21.1` @ **`e9b9eb7`**
("RoarGoal: fix countdown running at half speed, freezing after the roar"), one commit ahead of the
`b2a0da4` the proposal inspected. Verified pushed: `origin/revival/neoforge-1.21.1` is at the same
`e9b9eb7`.
Asset snapshot(s) considered: the proposal's own inspection of `MH_NW -20260912T005548Z-1-001`
(135 files). Not re-inspected in this session; the Drive folder was not available to this agent.

---

## 1. Confirmation and unresolved authority

- **Who reviewed this response?** Not yet reviewed. This document records an interview conducted
  2026-09-11; the maintainer has not yet confirmed the summary below.
- **Which decisions are confirmed by the authorized maintainer?** All decisions in section 4 were
  answered directly by the authorized decision owner during the interview. They are recorded as
  *answered*; they become *confirmed* when the maintainer confirms this document's summary of them.
  The consequential-decision list in section 8 marks which ones specifically need that confirmation.
- **Which still require another person's approval?** None for scope. One external dependency remains:
  art is supplied by a third party with **no stated commitment or date** (Q22).
- **Do not mark this response confirmed until the maintainer confirms the summary.**

### Corrections to the proposal established during this session

| Proposal statement | Correction | Source |
|---|---|---|
| Inspected revision is `b2a0da4` | HEAD is `e9b9eb7`; the delta is a RoarGoal countdown fix. Not materially stale. | `git log` |
| (§2) branch "does exist remotely" | **Correct.** `origin/revival/neoforge-1.21.1` = `e9b9eb7`. | `git ls-remote` |
| `CLAUDE.md` / `TEST_PLAN.md`: branch is "local only, not pushed" | **Stale - these are the wrong documents,** not the roadmap. Both need updating. | `git ls-remote` |
| `CLAUDE.md` roster omits Lagiacrus | Stale. `Lagiacrus.java` + `LagiacrusPursuitGoal.java` exist, matching the roadmap. | source tree |
| (§4) "The existing license/provenance disagreement is still a distribution gate" | **Rejected by the maintainer.** Art is made by a credited local artist; the repository's existing GPL-3.0 licence governs. This line should be removed from the roadmap. | Q25 |

---

## 2. Agreed product direction

- **Intended player experience and audience:** an open-world hunting mod (Q02). Monsters live in the
  survival world; players find them, hunt, **carve**, craft, prepare, and choose a harder hunt.
  Audience is small-group survival players using ordinary gear; difficulty is **"fair but real"** -
  a prepared player wins with attention, carelessness costs a lot of health, telegraphed attacks,
  no one-shots (Q07).
- **Compatibility targets / named must-have mods or packs:** NeoForge 1.21.1 only, current pins
  preserved. **No named packs or mods** must coexist. Standalone plus one representative pack
  profile is sufficient checking; no universal-compatibility claim (Q06).
- **Explicit non-goals (first release):** quest-led progression, hunter ranks, instanced missions,
  a new dimension, currency economy, ore generation, a custom crafting station, corpse-independent
  drop tables, Fabric/multi-loader, a newer Minecraft version.
- **Changes from the proposal, with reasons:**
  1. **Carving is in the first release, not deferred** (Q09a/Q10). The proposal's default was
     ordinary kill loot first and carving later; the maintainer wants the MH carve ritual as the
     *only* reward path from the start.
  2. **A custom biome is the sole habitat** (Q26, new). The proposal assumed vanilla biome tags.
  3. **A full MHW-faithful player combat overhaul is a stated future goal** (Q14) - no stage in the
     proposal covers it. Requested as a new late stage; see section 9.
  4. **Brief control-loss ailments are permitted** (Q18/Q18a), bounded. The proposal's flat
     "no permanent input lock" rule needs restating as a bounded rule.

---

## 3. First release contract

**Scope: R0 + R1 only, built around Great Izuchi (Q03).**

**Included creatures** (all four exposed via natural spawning, Q04):
- **Great Izuchi** - the hunt target. Combat accepted as-is.
- **Aptonoth** - passive meat/hide source.
- **Small Izuchi** - escort pack. Ships with **vanilla melee damage and no dedicated attack or death
  clip** (it has none in its own asset), with **no placeholder animation**. Accepted known visual gap.
- **Toad / Flashbug / Bug** - endemic life, present as world flavour; their gameplay uses are R2.

**New requirement (Q27):** Small Izuchi must **hover/circle at range and dart in occasionally to pick
at the player**, not apply constant melee pressure. This is a behavioural change from today's plain
`MeleeAttackGoal` and is in R1 scope.

**Habitat (Q26):** a **custom biome is the only place these creatures spawn.** No vanilla-biome-tag
fallback. The biome reuses vanilla structures. Consequence accepted: a player adding this mod to an
established world finds nothing until they reach newly generated chunks.

**Included gear, consumables and progression mechanics:**
- **Bone armor** - craftable, wearable, vanilla-comparable armor value plus **one modest
  hunting-oriented set trait** (Q16). Mixed vanilla/MH pieces stay viable.
- **Cooked meat** via ordinary vanilla cooking.
- **Vanilla crafting table and vanilla smithing table only.** A custom mod-home station is deferred
  until there is more armor to justify it (Q12).
- No custom weapon in the first release (greatsword is R3, Q15).

**Reward model (Q09, Q09a, Q10a, Q10b, Q11) - the largest addition to R1:**
- **Personal rewards per participant.** Eligibility = **any damage dealt**.
- **Carving only. Nothing drops on death.**
- Carve interaction: **shift + right-click**, **3 carves per eligible player**, per corpse.
- The corpse **remains in the world in a "dead" state for 10 minutes** (matching the vanilla item
  despawn timer), then despawns. Uncarved loot is lost.
- Carve results: **reliable basics, rare trophies.** The materials first-tier gear needs come
  reliably; rare items are later goals and never gate basic equipment.

**Player-visible end-to-end loop:**
fresh world -> travel to the custom biome -> find a Great Izuchi (and its Izuchi escort) ->
hunt it with ordinary gear -> shift+right-click the corpse three times -> obtain hide/claw/bone/meat
-> craft and wear bone armor, cook meat -> hunt again better equipped.

**Required evidence / who can provide it (Q08):**

| Gate | Provider |
|---|---|
| Dedicated server + two clients: no duplicate carve grants, no desync on a monster already mid-attack when a client starts tracking | maintainer |
| Save / restart / rejoin: parts survive, **per-player carve counters and the 10-minute timer persist**, no reward duplication on reconnect | maintainer (GameTest-coverable in part) |
| Human play-through of the full loop with no commands | maintainer |

A green `runGameTestServer` was **not** selected as a release gate. Recorded instead as the existing
per-change workflow rule already mandated by `CLAUDE.md`. *Flagged for confirmation* - it sits oddly
beside Q05's acceptance of the 69 tests as a trustworthy baseline.

**Explicit exclusions from the first release:** Rathian, Rathalos, Lagiacrus, any custom weapon, any
ailment or status effect, flash bombs, bucketed toads, part breaking, ores, a custom workstation,
Guild features, a field guide.

**Art or external blockers for R1:** none identified. R1 uses existing accepted runtime assets plus
the already-present `bone_armor.bbmodel`/`bone_armor.png` (attachment/render acceptance still
required). The first release is **not** gated on the uncommitted art supply; R3 is.

---

## 4. Question-by-question decisions

| ID | Status | Maintainer answer | Rationale / tradeoff | Affected stages | Unresolved point / owner / revisit condition |
|---|---|---|---|---|---|
| Q01 | confirmed | Maintainer alone decides all scope, ordering and release matters. | No second approver; fastest decision path, no external gates. | All | None. Art acceptance also sits with the maintainer (see Q23). |
| Q02 | confirmed | Open-world hunt -> harvest -> craft -> prepare loop. | Matches the proposal's recommendation; avoids the "model showcase" outcome. | Product direction | Guild bounties not committed as a goal (see Q21). |
| Q03 | confirmed | Smallest first release = **Great Izuchi loop only** (R0 + R1). | Complete loop over complete roster; avoids gating on art delivery. | R0/R1 | None. |
| Q04 | modified | All four creature groups exposed in survival - **plus a custom biome for them to appear in, reusing vanilla structures.** | Stronger identity and controlled ecology; the biome addition is new scope in R1. | R0/R1 | Biome contents/structure set undefined; maintainer; before R1 worldgen work begins. |
| Q04a | modified | Small Izuchi ships as **pack pressure with vanilla melee damage**, no attack/death clip, **no placeholder art**. | Honest over faked; keeps the pack identity without blocking on a skeleton retarget. | R1 | Animation retarget deferred; maintainer; revisit when Izuchi art is supplied. Noted as an accepted exception to the Q19 authenticity bar (escort, not an encounter). |
| Q05 | modified | Accepted: **Great Izuchi combat**, **Rathian bite + roar**, **the 69 GameTests**. Not affirmed: hurtbox measurement methodology. Added a new behavioural requirement (see Q27). | Avoids re-litigating proven work; the methodology question was simply not answered. | R0/R1/R4 | **Hurtbox range-midpoint methodology status = unknown.** Owner: maintainer. Revisit before the next species' hurtbox fitting (Rathalos/Lagiacrus). Not to be treated as rejected. |
| Q06 | confirmed | NeoForge 1.21.1, current pins, no named packs. | Preserves the working platform; no compatibility promises. | Platform | None. |
| Q07 | confirmed | Small-group survival, "fair but real". | Telegraphed, punishing of carelessness, never one-shot. | Balance, R1/R4 | Exact numbers are later balance work. |
| Q08 | modified | Gates = **two-client dedicated server**, **save/restart/rejoin**, **human loop play-through**. Green GameTest suite not selected as a gate. | Behaviour a human must see; automated tests already run per change. | All release gates | *Needs confirmation:* is the GameTest suite genuinely not a release gate? Owner: maintainer. |
| Q09 | modified | **Personal rewards per participant.** | Better co-op feel than shared drops; pulls eligibility tracking into R1. | R1/R5/R7 | None on the decision; implementation risk recorded in section 8. |
| Q09a | modified | Eligibility = **any damage dealt**. Rewards delivered via a **carve** mechanic. | Simplest testable rule; leeching irrelevant among friends. | R1/R5 | None. |
| Q10 | **rejected (proposal default)** | **Carving is essential to the first hunting loop.** Shift + right-click, 3 carves, MH-style. | The proposal recommended ordinary loot first; maintainer wants the ritual from day one. | R1/R5 | Carving brings corpse persistence/ownership/despawn work into R1 - see section 8. |
| Q10a | confirmed | **3 carves per eligible player**; corpse persists "dead" in world for **10 minutes** (vanilla item despawn timer), then despawns. | Matches personal rewards and MH co-op. | R1 | Per-player counters + timer must survive save/reload and reconnect. A counter reset = infinite loot. |
| Q10b | confirmed | **Carving only - no death drops at all.** | One reward path, one owner, no double-grant possible. | R1 | Discoverability risk: a player who doesn't know to carve gets nothing. No hint/toast was selected. Owner: maintainer; revisit at the human play-through gate. |
| Q11 | confirmed | Reliable basics, rare trophies later. Never gate basic gear on rares. | Enforces the roadmap's "no circular progression" rule. | R1/R3/R4/R6 | None. |
| Q12 | modified | **Vanilla crafting table + vanilla smithing table** for now. Custom mod-home station deferred. | Zero UI work in R1. | R1/R2/R3 | Revisit condition stated by maintainer: **"once we add more armor."** Owner: maintainer. |
| Q13 | confirmed | One visible break -> **bonus carve / guaranteed part material**. One part, one species first. | Fits the carve model directly; not a universal part simulator. | R5 | Severing and behaviour-changing breaks remain later extensions. |
| Q14 | modified | **First release: vanilla-compatible combat.** Future release: **a fuller MHW-faithful combat overhaul is an intended goal.** | Protects R1 scope and modpack compatibility now; records a major future direction. | R1 now; new late stage later | The proposal has **no stage** for this. Requested as new stage R8 (section 9). It will conflict with the roadmap's "preserve vanilla agency" and "modpack coexistence" rules, which must then be revised. Owner: maintainer; revisit after R3. |
| Q15 | confirmed | **Bone greatsword (BoneBlade)** is the first custom weapon. | Most iconic MH weapon; natural home for a charged attack. | R3 | **Most art-blocked option:** no textures, no bound faces, internal name mismatch (`BoneBlade` vs `BoneGreatsword`). See Q22a. |
| Q16 | confirmed | Armor value plus **one modest set trait**; mixed equipment stays viable. | Avoids a skill-tree/decoration engine. | R1/R3 | The specific trait is unchosen. Owner: maintainer; revisit at R1 item design. |
| Q17 | modified | Essential R2: **prepared meat/BBQ**, **flashbug -> flash bomb**, **bucketed toads**. Antidote/poison counter **not** essential. | Deepens the existing hunt without depending on an ailment that has no source yet. | R2 | Antidote moves to R4, arriving with Rathian's poison. |
| Q18 | modified | **Brief control loss is allowed** on players. | More faithful than the proposal's flat prohibition. | R2/R4/R6 | None on the decision; the rule rewrite is in section 9. |
| Q18a | confirmed | Hard cap: **<= 2 seconds (~40 ticks), escapable/shortenable by player input.** | Reads as a stagger, not a freeze; the player always has something to do. | R2/R4/R6 | Applies to every future ailment; no exceptions granted. |
| Q19 | confirmed | **Small distinctive moveset (2-4 readable, measured moves) plus fair behaviour.** Not every MH move. | Matches what Great Izuchi already is. | R4/R6, art gates | Q04a's Small Izuchi is recorded as an accepted exception (escort, not an encounter). Ground-only-flyer exceptions were **not** granted - Rathalos must fly to ship. |
| Q20 | modified | **R2 field preparation comes immediately after the first release**, before Rathian. | Deepens the one working hunt rather than adding another; makes the next monster better when it arrives. | R2 before R4 | The proposal's default was Rathian first. Rathian becomes the third increment. |
| Q21 | modified | Scheduled: **Great Izuchi rallies its escort**, **monsters retreat/rest when hurt** - the latter implemented via **"nests"/rest locations in the custom biome** acting as spawn anchors and retreat destinations. Not scheduled: field guide, Guild bounties. | Retreat gets a concrete destination instead of open-ended fleeing; ties R7 to R1's biome work. | R7 (with an R1 design link) | Nests are a **reserved concept, not R1 build work.** Owner: maintainer; revisit when the biome is designed - reserving the idea is free, building it now is not. The rally clip still needs an agreed trigger. |
| Q22 | **unknown (external)** | Art is supplied by someone else, **with no commitment and no dates.** | Honest statement of the real supply situation. | R3, all art-gated packs | **Unresolved and owned externally.** No deliverable is committed. |
| Q22a | **deferred / assumption** | *"Let's assume we'll get the art soon."* | Maintainer declined to pick a fallback after the conflict was presented. | R3 | **This is an assumption, not a commitment** (see section 9). No fallback weapon, no self-authored texture work, and no reordering were chosen. R3 remains undated. Owner: maintainer; revisit when R3 is due to start and the art has not arrived. |
| Q23 | confirmed | **Maintainer approves in-game appearance and picks the canonical asset version.** | Consistent with Q01; the artist supplies, the maintainer accepts. | All art / contact geometry | Replacing an accepted runtime model can invalidate measured hurtboxes - revalidation stays a maintainer decision per asset. |
| Q24 | confirmed | **One workstream at a time: maintainer + one agent.** | No shared-file conflicts; simplest integration. | Workstream split, delivery | The roadmap's parallelism section (§5) becomes largely informational - see section 9. No dates; hobby cadence. |
| Q25 | **corrected** | The proposal's premise is wrong: **art is made by a credited local artist, and the repository's existing licence (GPL-3.0) is the correct one.** No provenance disagreement exists. | Removes an assumed distribution gate. | Public release | The maintainer did not separately address Capcom's Monster Hunter names/species as IP; that is distinct from art provenance and is **not** recorded as resolved. Owner: maintainer; revisit before any public distribution. |
| Q26 (new) | confirmed | **Custom biome only** - no vanilla-biome-tag fallback. The mod expects a fresh world; this is an intentional stance, not a limitation. | Strongest identity, cleanest balance, no vanilla ecology pollution. | R0/R1, R6 | Requires R1's exit criterion to be rewritten (section 9). Existing-world players are knowingly unserved. |
| Q27 (new) | confirmed | Small Izuchi must **hover/circle at range and dart in to attack occasionally**, "like they are picking at you" - not constant melee pressure. | Reads as a harassing pack rather than a swarm of zombies. | R1 | New goal work, not covered by the proposal. Pairs naturally with the Q21 rally behaviour. |

---

## 5. Proposed stage disposition

| Stage | Keep / modify / split / defer / drop | Player outcome | Required inputs | Required output / acceptance | Release priority | Suggested owner |
|---|---|---|---|---|---|---|
| **R0** | **modify** - add custom-biome spawning; drop "choose which creatures are exposed" (all four are) | Existing encounters behave consistently in survival and multiplayer | Current `e9b9eb7` baseline | Two-client dedicated server correct; save/restart/rejoin correct; parts cleanup; mid-action tracking not misrepresented as synchronized; controlled spawning in the new biome with a disable config | **1 (with R1)** | maintainer + agent |
| **R1** | **modify - significantly larger than proposed** | Find a Great Izuchi in a custom biome, hunt it, carve it three times, craft and wear bone armor, cook meat, hunt again | R0; bone armor asset acceptance | The loop in section 3, end to end, no commands, on a **fresh world**; personal carve rewards never duplicate across save/reload or reconnect | **1** | maintainer + agent |
| **R1a** (split, new) | **split out of R1** | A custom biome exists, generates, reuses vanilla structures and hosts the roster | none (worldgen only) | Biome generates; the four creature groups spawn there at controlled rates; spawning disableable by config | 1 - blocks R1 | maintainer + agent |
| **R1b** (split, new) | **split out of R1** | Corpses persist and are carvable | R0 death/parts semantics | 10-minute dead corpse; 3 carves per eligible player; shift+right-click; persists across save/reload; no duplicate grants; nothing drops on death | 1 - blocks R1 | maintainer + agent |
| **R2** | **modify** - promoted ahead of R4; antidote removed | Prepared meat, flash bombs and bucketed toads change how a hunt is approached | R1 item conventions; existing endemic-life implementations | Each of the three has a source, a visible effect, a duration and a reason to carry it; capture/release consumes or restores the container exactly once and preserves the variant | **2** | maintainer + agent |
| **R3** | **keep, undated** | A bone greatsword is a reason to hunt again | R1 materials; **greatsword art that does not yet exist** | Textured, bound, rendered greatsword with one committed attack; a survival acquisition route | 4 - **art-blocked, no date** | maintainer + agent + external artist |
| **R4** | **modify** - Rathian only; Rathalos split out | Rathian is a distinct, readable poison encounter with useful rewards | R1; poison ailment + antidote (moved here from R2) | A second measured attack; poison with warning and counter, within the <=2s control-loss cap; fitted hurtboxes; carvable rewards | **3** | maintainer + agent |
| **R4b** (split) | **split out of R4, defer** | Rathalos is a real flying wyvern | R4; valid attack presentation (its melee clips reference 14-17 bones absent from its own geometry) | Bounded takeoff/attack/landing cycle; readable fireballs; sane behaviour on terrain/target loss. **Ground-only shipping was not permitted (Q19)** | 5 - art-blocked | maintainer + agent + external artist |
| **R5** | **keep** | Targeted attacks break a part and grant a bonus carve | R1 reward ownership; one accepted multipart encounter | One visible break, once, granting a bonus carve or guaranteed part material; survives save/rejoin; vanilla and other mods' weapons qualify | 6 | maintainer + agent |
| **R6** | **keep** as independent packs | Thunder / aquatic / frost encounters expand preparation and loot | R1 plus each encounter's own effect/movement needs | Per pack: findable, recognizable threat, a response, a useful carve, and its counterplay | 7 | maintainer + agent |
| **R7** | **modify** - narrowed to two behaviours | Great Izuchi rallies its escort; wounded monsters retreat to nests/rest sites | Accepted hunts; stable rewards; biome nest concept | Bounded rally with an agreed trigger; retreat to a nest rather than open-ended flight | 8 | maintainer + agent |
| **R7b** (split, defer) | **split out of R7, defer** | Field guide / Guild bounties | R7 | - | not scheduled | - |
| **R8** (new) | **new stage, conceptual** | MHW-faithful player combat: stamina, dodge, sheathe, weapon movesets | R3; a decision to revise the "preserve vanilla agency" and "modpack coexistence" rules | Undefined - conceptual only | last | maintainer |

**Dependency changes from the proposal:**
- R1 now depends on **new worldgen (R1a)** and a **persistent carvable corpse (R1b)**. Both are new
  critical path, neither existed in the proposal's R1.
- **R2 now precedes R4** (Q20).
- The **antidote/poison counter moves from R2 to R4**, where its source actually exists (Q17).
- **Rathalos is split from Rathian** so Rathian can ship alone, and Rathalos's flight is not
  optional (Q19 granted no ground-only exception).
- **R8 is new** and has no proposal equivalent (Q14).

---

## 6. Art and content commitments

| Asset / family | Present readiness | Desired use | Confirmed next deliverable | Owner / approver | Commitment versus aspiration | What can ship without it |
|---|---|---|---|---|---|---|
| Great Izuchi model + animations | **accepted in game** | R1 hunt target | none needed | maintainer approves | n/a - already accepted | - |
| Izuchi (small) attack/death clips | **planned/not supplied** - own asset has idle/sleep/walk/run only; `origin/brain` candidates rig to Great Izuchi's skeleton | Attack and death presentation | **None. Retarget deferred.** | local artist supplies / maintainer approves | **aspiration** | R1 ships it attack-clip-less with vanilla melee damage and no placeholder (Q04a) |
| Bone armor (`bone_armor.bbmodel` + PNG) | **exported candidate** - textured `modded_entity`, 19 elements, 128x128 | R1 craftable armor | Attachment/render acceptance | maintainer approves | needed for R1 | Nothing - this is R1's only equipment |
| Bone greatsword (`BoneBlade.bbmodel`) | **present/source-only** - no textures, no bound faces, internal name is `BoneGreatsword` | R3 first custom weapon | **None committed.** Maintainer's position: *"assume we'll get the art soon."* | local artist supplies / maintainer approves | **aspiration, explicitly unapproved** | R1, R2 and R4 all ship without it |
| Bone scythe / hunter's bow / heavy bowgun | present/source-only (scythe has a bound texture; bow has a 16x16-vs-64x64 UV mismatch) | Later R3 weapons | none | local artist / maintainer | aspiration | everything |
| Hide / claw / meat / bone material sprites | exported candidate | R1 carve rewards | none needed | maintainer | usable now | - |
| Custom biome assets | **planned/not supplied** - no biome exists yet | R1a habitat | Biome definition (worldgen, not art) | maintainer | **needed for R1** | Nothing - R1 depends on it |
| Nest / rest-site feature | planned/not supplied | R7 retreat destination, R1 spawn anchor | none - reserved concept only | maintainer | aspiration | everything |
| Rathalos melee clips | **broken** - four melee clips reference 14-17 bones absent from its own geometry | R4b | retarget or new clips | local artist / maintainer | aspiration | R4 (Rathian) ships without it |
| Effect icons (iceblight, lethal_poison, sleep, thunderblight) | exported candidate - four 18x18 icons | R4/R6 ailment presentation | none needed | maintainer | usable when ailments exist | R1, R2 |
| Ore blocks / crystals / processed items | preserved art, byte-identical to the old repo | later R3 forging | none | maintainer | aspiration | everything - no ores in R1 (Q12) |
| Bucketed-animal icons (4 variants) | exported candidate | R2 portable endemic tools | none needed | maintainer | usable now | R1 |

**Revalidation rule (Q23):** the maintainer alone picks the canonical version when a Drive export
collides with a working runtime asset. **Replacing an accepted model or animation invalidates that
species' measured hurtboxes and attack paths** - Great Izuchi's and Rathian's measured work is the
material at risk. Do not bulk-copy a snapshot over working assets. No dates are recorded anywhere
in this table because none were given.

---

## 7. Parallel work and integration

- **Workstreams that can start independently:** in practice, **none run concurrently.** The
  maintainer stated capacity of **one workstream at a time, maintainer + one agent** (Q24). The
  roadmap's §5 parallelism analysis is therefore **informational for a future contributor, not an
  operating model.** It should not drive any restructuring of `MHNW`, `ModEntities`, `MHNWClient`
  or `MHNWGameTests` - there is no collaboration bottleneck to solve.
- **Shared decisions/interfaces that must settle first** (before R1 implementation planning):
  - Carve material IDs and the bone-armor recipe inputs.
  - The **carve-eligibility and carve-counter save format** - per-player counters plus the 10-minute
    timer, on the corpse, idempotent across reconnect. This is the single highest-risk shared
    contract in R1.
  - The biome ID and which vanilla structure sets it joins.
  - Bone armor's one set trait (unchosen).
- **Integrator and ownership of shared files:** the maintainer, exclusively. All shared registration
  and core changes serialize through one agent session at a time.
- **Contributor availability actually stated:** the maintainer works with one agent at a time; the
  art supplier has stated **no availability and no commitment**. **No dates exist in this document.**
- **Any staged work or artifacts already completed elsewhere:** the `revival/neoforge-1.21.1` branch
  at `e9b9eb7` (pushed), 69 reported-passing GameTests, accepted Great Izuchi combat, accepted
  Rathian bite and roar, provisional Lagiacrus pursuit. The archived `origin/brain` branch holds
  candidate Izuchi clips that do not fit Izuchi's skeleton.

---

## 8. Contradictions, deferred decisions and risks

| Issue | Why it matters | Affected stage | Owner | Required decision/evidence before work |
|---|---|---|---|---|
| **Carving + personal rewards is a large, unbudgeted R1 addition** | The proposal deliberately put carving *after* the first loop. It brings a persistent corpse entity, a 10-minute timer, per-player counters, save/reload persistence and reconnect idempotency into the first release. A counter that resets on reload is infinite loot. | R1 (R1b) | maintainer | Accept the enlarged R1, or move carving to R2. Needs GameTest coverage before the human play-through. |
| **Carving-only rewards with no discoverability hint** | A player who never shift+right-clicks a corpse gets nothing from a won hunt and concludes the mod is broken. No hint/toast was selected (Q10b). | R1 | maintainer | Revisit at the human play-through gate; a one-time hint is cheap insurance. |
| **Custom-biome-only spawning breaks R1's stated exit criterion** | The proposal's R1 exit says "a fresh survival **player** can discover a hunt without commands". With biome-only spawning it must read "a fresh **world**". Existing-world players are knowingly unserved. | R0/R1 | maintainer | Accepted knowingly; roadmap text must be corrected (section 9). |
| **Art supply is uncommitted, and the most art-blocked weapon was chosen first** | The proposal required this conflict to be surfaced rather than resolved by the agent. It was surfaced (Q22a); the maintainer chose to **assume** delivery rather than pick a fallback. | R3, R4b | maintainer + external artist | **Unresolved.** R3 cannot be dated or planned. Revisit when R3 is due and the art has not arrived; the fallback options (scythe instead, self-authored texture/UV work, or reordering behind R4) remain open. |
| **Hurtbox measurement methodology not affirmed** | `CLAUDE.md` calls the range-midpoint method load-bearing. It went unselected in Q05 and was recorded as `unknown`, **not** rejected. | R4, R4b, R6 | maintainer | Confirm before the next species' hurtbox fitting. Do not treat silence as either approval or rejection. |
| **GameTest suite not listed as a release gate** | Q05 accepts the 69 tests as a trustworthy baseline; Q08 did not list a green run as a gate. Recorded as a per-change workflow rule instead. | all | maintainer | Confirm the interpretation. |
| **Small Izuchi ships below the Q19 authenticity bar** | Q19 requires 2-4 readable measured moves per encounter; Small Izuchi will damage players with no attack animation at all. Recorded as an accepted exception on the grounds that it is an escort, not an encounter. | R1 | maintainer | Confirm the exception is intended. |
| **Q27 harassment behaviour vs. "fair but real"** | Hover-and-dart pack behaviour is harder to balance than steady melee - several darting attackers can out-damage a single committed one. New goal work with no existing analogue in the codebase. | R1 | maintainer | Needs the human play-through gate specifically to judge it. |
| **A future MHW combat overhaul contradicts two standing release rules** | R8 (Q14) would break "preserve vanilla agency" and "modpack coexistence" as currently written. | R8 | maintainer | Those rules must be revised when R8 is scheduled, not quietly violated. |
| **Monster Hunter names and species are Capcom IP** | Q25 resolved *art provenance* (credited local artist, GPL-3.0). It did not address the separate matter of the MH IP itself. Not recorded as resolved. | public release | maintainer | Decide before any public distribution. |
| **`CLAUDE.md` and `TEST_PLAN.md` are stale** | Both say the branch is local-only and never pushed; it is pushed at `e9b9eb7`. `CLAUDE.md`'s roster also omits Lagiacrus. Stale docs mislead the next agent. | all | maintainer | Correct both before handing out implementation plans. |

---

## 9. Requested roadmap changes

**Exact sections/stages to change and why:**

1. **§2 table, "Release confidence" row** - record that the branch is pushed and that
   `CLAUDE.md`/`TEST_PLAN.md` are the stale documents, not §2.
2. **§4, asset acceptance policy** - **delete** "The existing license/provenance disagreement is
   still a distribution gate." The maintainer states art is by a credited local artist under the
   repository's existing GPL-3.0 licence (Q25). Optionally replace it with a note that Capcom's MH
   IP remains a separate, unresolved distribution question.
3. **§3, R1 exit criterion** - change "a fresh survival player can discover a hunt without commands"
   to "a fresh **world**", reflecting custom-biome-only spawning (Q26).
4. **§3, R1 scope** - add the custom biome (R1a) and the persistent carvable corpse (R1b) as
   first-release work. Add Q27's hover-and-dart Small Izuchi behaviour.
5. **§3, R1/R5** - move carving from R5 into R1; carving is the **only** reward path, replacing
   death drops entirely (Q10, Q10b).
6. **§3 release order** - R0+R1 -> **R2** -> R4 (Rathian) -> R3 (whenever art lands) -> R5 -> R6
   -> R7 (Q20).
7. **§3, R4** - split into R4 (Rathian) and R4b (Rathalos). Record that ground-only shipping was
   **not** granted as an exception (Q19).
8. **§3, R2** - remove the antidote/poison counter; it moves to R4 with its source. Keep prepared
   meat, flash bombs and bucketed toads (Q17).
9. **§3, R7** - narrow to rally + retreat-to-nests. Field guide and Guild bounties become
   unscheduled (Q21).
10. **§5 parallel work** - reframe as guidance for a hypothetical future contributor. Stated
    capacity is one workstream at a time (Q24); no file reorganization is warranted.
11. **§6 release rules, "Preserve vanilla agency"** - replace the flat "no permanent input lock"
    with the bounded rule: **control-loss effects may not exceed ~2 seconds (~40 ticks) and must be
    shortenable by player input** (Q18a).
12. **Add stage R8 (conceptual)** - MHW-faithful player combat overhaul, with an explicit note that
    scheduling it requires revising the "preserve vanilla agency" and "modpack coexistence" rules
    (Q14).
13. **Add Q26 and Q27** to the §7 register without renumbering existing questions.

**Rejected recommendations:**
- The proposal's "ordinary kill loot first, carving later" (Q10) - carving is in R1.
- The proposal's "Rathian first after release" (Q20) - R2 comes first.
- The proposal's implicit vanilla-biome-tag spawning (Q26) - custom biome only.
- The proposal's claimed license/provenance disagreement (Q25) - the premise is wrong.

**New agreed features not in the proposal:** the custom biome (Q04/Q26); the shift+right-click,
3-carves-per-player, 10-minute-corpse mechanic (Q09a/Q10a/Q10b); Small Izuchi hover-and-dart
harassment (Q27); nests/rest sites as retreat destinations (Q21); a future MHW combat overhaul (Q14).

**Assumptions that must remain unapproved:**
- **"Assume we'll get the art soon" (Q22a) is an assumption, not a commitment.** No deliverable,
  owner-side promise or date exists. R3 and R4b must not be scheduled as though art were secured,
  and no fallback was selected.
- **The hurtbox range-midpoint methodology is `unknown`, not approved** (Q05).
- Every decision in section 4 is *answered*, not yet *confirmed* - see section 1.

---

## 10. Readiness for finalization

- **Ready for roadmap finalization:** **partially.**
- **Missing approvals or decisions:**
  - Maintainer confirmation of this document's summary (section 1) - **nothing is confirmed until
    then.**
  - The four items flagged for confirmation in section 8: the enlarged R1, the GameTest-gate
    interpretation, the Small Izuchi authenticity exception, and the hurtbox methodology status.
  - A fallback decision for R3's art blocker, or an explicit acceptance that R3 stays undated.
  - Bone armor's one set trait, and the biome's ID/structure set - both needed before R1
    implementation planning, neither needed before roadmap finalization.
- **Which near-term stages are ready for implementation-plan drafting** (after this response is
  confirmed): **R0**, **R1a** (custom biome), **R1b** (carvable corpse), then **R1** proper. These
  have inputs, acceptance criteria and no external blockers.
- **Which stages must remain conceptual:** **R3** and **R4b** (art-blocked, no commitment),
  **R6** (each pack's art and effect semantics undecided), **R7b** (unscheduled), **R8** (no scope,
  and it contradicts standing release rules).
- **No implementation has been authorized merely by completing this response.** No runtime code,
  dependency, asset or branch state was modified during this session.

---

*Interview conducted and this response drafted by Claude Code (Opus 5), 2026-09-11. All answers are
the maintainer's own; interpretations are labeled as such. Section 8 lists every point where this
agent's reading still needs the maintainer's confirmation.*
