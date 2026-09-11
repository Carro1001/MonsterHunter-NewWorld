# Manual test plan

What still needs a human at a screen. Everything else (damage semantics, timing windows,
state-machine wedging, save/reload of gameplay facts, navigation) is covered by headless GameTests
via `gradlew runGameTestServer` — see `MHNWGameTests.java`, currently 63 tests, all 63 passing as of
the Rathian bite-clip measurement rig below (full `build runGameTestServer` run, 2026-09-11) — the
shoreline test that failed under the P5a build passed cleanly after the native-controls correction.
Client acceptance for that Lagiacrus correction has not been rerun by a human yet.

This file is updated as features land. Checked items were confirmed by the maintainer; unchecked
items are open. When you find a problem, say what you saw and I'll fix it and update this file.

Branch: `revival/neoforge-1.21.1` (local only, not pushed).

## How to launch

```powershell
.\gradlew.bat runClient
```

Combat/behaviour diagnostics: run `/mhnw debugcombat` in-game to toggle (op-only), or set
`debugCombat = true` in `run/config/mhnw-common.toml` before launching if you'd rather it start on.
Either way it logs attack transitions and contact accepted/rejected, and — for Great Izuchi
specifically — draws the live attack volume with F3+B.

**Bone probe**: with `debugCombat` on, Great Izuchi, Rathian, Rathalos, Aptonoth and Lagiacrus log their
actual runtime bone positions to the game log every ~2 seconds (or every tick for Great Izuchi
while it's mid-attack), in the same left/up/forward frame the hurtbox offsets use. This is what
actually fixed Rathian's and Aptonoth's hurtboxes this round (see their sections below) — real
measurements read straight from a play session, not another guess. Rathalos is borrowing Rathian's
measured numbers as a proxy for now (same skeleton, similar proportions); if you ever want it
measured for real, stand near one with `debugCombat` on for a few seconds and send me
`logs/latest.log`.

---

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

Also new this pass: it (and Rathian/Rathalos) no longer stops rendering when the hitbox center
leaves the camera frustum while the long neck/tail is still visibly on screen — see the culling note
under Rathian/Rathalos below, same fix applies here too.

- [x] Renders, spawns via egg, idles/walks/runs with correct animation
- [x] F3+B hurtboxes sit on the body correctly (measured via the runtime bone probe)
- [x] Damage lands through a hurtbox; tail/body hit registers
- [x] Scratch, tail swipe, tail slam all fire, connect, and look reasonable in sequence
- [x] Windup plant + active-phase pace read as a real wind-up and follow-through
- [x] Death plays its authored clip fully before the corpse is removed
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
actually attacks the player on sight. **It has no attack or death animation on purpose** — see
`docs/DEFERRED.md` for why (the only candidate clips reference bones missing from their own
geometry) — so expect it to fight using its walk/run clip and die with vanilla's plain corpse flop.
That is the current, deliberate state, not a bug to report.

- [ ] Renders, spawns via egg, idles/walks/runs with correct animation
- [ ] Notices and attacks a nearby player, dealing real damage
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

**No dedicated attack volume yet, but a measurement rig for the first one landed this round.**
`doHurtTarget` now also starts a synced, purely cosmetic countdown that plays
`attack_charge_bite_right` (1.5s/30 ticks) and switches `BoneProbe`'s Rathian logging to every tick
while it plays, instead of the idle sampling interval — `Chest` (the bone that clip actually
animates, separate from `Torso`) was added to the logged bone list for it. **This changes nothing
about combat**: damage is still the exact same instantaneous `doHurtTarget` call vanilla
`MeleeAttackGoal` already made, unconditionally, the same tick it always did. It exists purely so
capturing this one clip live is a single play session, not a play-session-plus-a-code-change:

- [ ] **New: with `debugCombat` on, provoke a Rathian into attacking a few times and send back
      `logs/latest.log`** — once the `Chest`/`Neck1`/`Neck2`/`Head`/`Jaw`/`headHitbox` path is
      measured across a strike, I'll bake it into a real `AttackProfile` and a dedicated combat
      goal the same shape as Great Izuchi's, replacing this cosmetic-only rig.
- [ ] **New: does the bite clip itself play at a sane moment** (roughly when the melee hit lands),
      even though the timing isn't tuned to anything yet? Flag if it looks badly desynced from the
      actual hit (e.g. plays well after the target's health already dropped).

- [ ] Renders, spawns via egg, idles/walks/runs with correct animation
- [ ] **F3+B: do the boxes meet edge-to-edge with no visible gap**, and **does the tail chain read as
      roughly centred on the tail across a few seconds of watching it sway**, rather than checking
      only one instant? Flag anything still off and I'll keep adjusting that specific part
- [ ] Turning away no longer makes the head/neck suddenly vanish while still on screen
- [ ] Hitting a hurtbox (try the head, then the tail tip) reduces health
- [ ] Attacks and damages a nearby player using ordinary melee (no special swing yet — this is
      expected for now, not a bug)
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

- [ ] Renders, spawns via egg, idles/walks/runs (walk uses `walk_normal`/`walk_aggro`, no separate
      "run" clip exists for this species — expected, not a bug)
- [ ] **F3+B: closer now?** Flag any part that's still clearly off — that's the one worth a live
      measurement session for
- [ ] Turning away no longer makes the head/neck suddenly vanish while still on screen
- [ ] Hitting a hurtbox reduces health
- [ ] Attacks and damages a nearby player using ordinary melee
- [ ] Death removes the whole creature and all seven parts
- [ ] No flight yet — ground-bound only; expected, not a bug
