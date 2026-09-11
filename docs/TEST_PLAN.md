# Manual test plan

What still needs a human at a screen. Everything else (damage semantics, timing windows,
state-machine wedging, save/reload of gameplay facts) has a GameTest and runs headless via
`gradlew runGameTestServer` — see `MHNWGameTests.java`, currently 45 tests, all passing.

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

**Bone probe**: with `debugCombat` on, Great Izuchi, Rathian, Rathalos and Aptonoth all log their
actual runtime bone positions to the game log every ~2 seconds (or every tick for Great Izuchi
while it's mid-attack), in the same left/up/forward frame the hurtbox offsets use. This is what
actually fixed Rathian's and Aptonoth's hurtboxes this round (see their sections below) — real
measurements read straight from a play session, not another guess. Rathalos is borrowing Rathian's
measured numbers as a proxy for now (same skeleton, similar proportions); if you ever want it
measured for real, stand near one with `debugCombat` on for a few seconds and send me
`logs/latest.log`.

---

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
- [ ] **A10 navigation scenarios** — corner, body-wide passage, narrow passage, step, unreachable
      target — deferred by your call (see `docs/DEFERRED.md`)
- [ ] **A08/A09 two-client agreement** — needs a dedicated server + two clients, which I cannot run;
      deferred to polishing (see `docs/DEFERRED.md`)

## Aptonoth (P3 passive herbivore)

Fourth round this pass:

- **Hurtboxes measured, not guessed** (previous round). Ran a BoneProbe session against your own
  log and plugged the actual head/tail1/tail2 bone positions in directly. Confirmed by a second log
  capture this round: the measured numbers came back essentially identical to what was already in
  the code, so the *positions* were already right — the "proportions off" feedback this round was
  about something else (below).
- **New: added a third tail segment to close a real coverage gap.** `aptonoth.geo.json`'s own
  tail1/tail2 meshes are each about 1.9-2.0 blocks long, but the two hurtboxes at the measured bone
  positions only had small (0.6-0.8 block) boxes at each end, leaving roughly 1.9 blocks of visible
  tail in the middle with no hurtbox at all — almost certainly what read as disconnected, floating
  green boxes rather than a tail that's actually covered. Added `tail_2` at the interpolated
  midpoint between the two measured points, the same overlapping-segments idea the large monsters'
  tails already use for the identical reason. Not itself a fresh measurement, but grounded in the
  real authored mesh length, not a guess about proportions.
- Dropped the separate green "body" box (previous round, unchanged): Aptonoth never fights back, so
  the plain root hitbox already covers the torso without a redundant duplicate.
- Flee duration unchanged (still a sustained flee after a hit, not one short hop).

- [ ] Renders, spawns via egg, idles/walks with correct animation
- [ ] **F3+B: does the tail now read as continuously covered** rather than two small floating boxes
      with a gap between them? Head box should still sit right on the head as before
- [ ] **New: no green box floating where the old "body" box used to be** — only head and a
      three-segment tail, body damage should come from the plain white hitbox like any other animal
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

**Hurtboxes are now real BoneProbe measurements**, not a guess. Your `logs/latest.log` had
`[rathian]` bone-probe lines from a session with `debugCombat` on; the measured left/up/forward for
each `*Hitbox`-named bone is plugged in directly. This is a strictly better fix than the two earlier
attempts (offline FK solve, then a hand-eyeballed "nudge it down" correction) — and it turned out the
hand correction, while pointed the right direction, still hadn't gone nearly far enough: the neck and
head `up` values were still off by close to a full block even after that pass.

Also fixed two rounds ago: the entity no longer stops rendering (head/neck suddenly disappearing)
when the root hitbox leaves the camera frustum while the model still visibly extends into frame.

**No dedicated attack animation.** Like Izuchi, it fights using ordinary melee with no custom swing;
real attack clips exist (charge, bite, tailwhip, fireball) but wiring them needs the hurtbox
placement settled first, or a swing could land or miss for the wrong reason. Now that placement is
measured rather than guessed, this is worth revisiting.

- [ ] Renders, spawns via egg, idles/walks/runs with correct animation
- [ ] **F3+B: do the seven boxes actually sit on the body now?** This was measured directly from a
      real play session, so it should be a much closer fit than either earlier round — flag anything
      that still looks off and I'll re-measure that specific part
- [ ] Turning away no longer makes the head/neck suddenly vanish while still on screen
- [ ] Hitting a hurtbox (try the head, then the tail tip) reduces health
- [ ] Attacks and damages a nearby player using ordinary melee (no special swing — this is expected
      for now, not a bug)
- [ ] Death removes the whole creature and all seven parts
- [ ] No flight yet — it should behave as a purely ground-bound creature; this is expected, not a bug

## Rathalos (P4 ground wyvern, ground-only)

**Now using Rathian's real measurements as a proxy**, per your call ("rathalos and rathian are
almost identical, you can use the numbers for one on the other"): no `[rathalos]` bone-probe lines
have shown up in a log yet, but the two species share the same base skeleton and closely similar
proportions, so Rathian's measured offsets are plugged in directly rather than waiting on a
Rathalos-specific session. Should be far closer than the old offline-solved/hand-corrected guess,
though not guaranteed pixel-perfect the way an actual Rathalos measurement would be — if any one
part still looks off, that's the part worth a real session for. Its attack clips are blocked
regardless: they reference bones that don't exist anywhere in this species' model at all, confirmed
by checking, not guessed — see `docs/DEFERRED.md`. That needs actual art/model-editor work before
it's even worth wiring.

Also fixed a few rounds ago: same culling fix as Rathian/Great Izuchi.

- [ ] Renders, spawns via egg, idles/walks/runs (walk uses `walk_normal`/`walk_aggro`, no separate
      "run" clip exists for this species — expected, not a bug)
- [ ] **F3+B: closer now, using Rathian's numbers?** Flag any part that's still clearly off — that's
      the one worth a live measurement session for
- [ ] Turning away no longer makes the head/neck suddenly vanish while still on screen
- [ ] Hitting a hurtbox reduces health
- [ ] Attacks and damages a nearby player using ordinary melee
- [ ] Death removes the whole creature and all six parts
- [ ] No flight yet — ground-bound only; expected, not a bug
