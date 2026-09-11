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

Fifth round this pass, from your latest screenshot + log:

- **The white root box was genuinely too short — confirmed, not just suspected.** The measured
  "body" bone (the torso's own pivot) sits at `up=1.87`, meaning `BODY_HEIGHT=1.0` didn't even reach
  the *centre* of the chest, let alone its top — exactly matching the screenshot showing the root
  box sitting well below the visible torso. Raised to 2.0 (from 1.0) and `BODY_WIDTH` nudged to 1.3
  (from 1.2), the latter matching the real ~1.25-wide main body cube in `aptonoth.geo.json`. This
  is a bigger jump than earlier rounds' nudges, but it's grounded in a real measurement the old value
  didn't even reach, not a fresh guess. Still not itself a direct measurement of the box's ideal
  height (that would need a leg/foot bone in the capture to pin the ground reference exactly) — flag
  it again if it now reads as too tall, the opposite direction from round one's original complaint.
- **Tail: added a fourth segment and sized every tail box up a bit**, per your call ("might need an
  extra volume for the tail, or two"). Now `tail_1`..`tail_4`, the two new ones (`tail_2`, `tail_3`)
  interpolated between the two measured endpoints, closing the gaps further; each box is roughly
  10-15% bigger than last round.
- Head position unchanged (wasn't flagged this round) and dropped "body" box unchanged (previous
  round).
- Flee duration unchanged (still a sustained flee after a hit, not one short hop).

- [ ] Renders, spawns via egg, idles/walks with correct animation
- [ ] **F3+B: does the white root box now actually reach the chest**, instead of sitting well below
      it? (Flag if it now reads as too tall instead — that's the opposite failure mode)
- [ ] **F3+B: does the tail now read as continuously, snugly covered** by four boxes instead of
      gappy/undersized? Head box should still sit right on the head as before
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

**Hurtboxes are real BoneProbe measurements**, re-averaged once already. Your first `logs/latest.log`
had `[rathian]` bone-probe lines with `debugCombat` on, and the measured left/up/forward for each
`*Hitbox`-named bone was averaged in — a strictly better fix than the two earlier attempts (offline
FK solve, then a hand-eyeballed "nudge it down" correction). Your follow-up screenshot then showed
the boxes reading *too low*, and a second, larger capture (twelve samples per bone instead of a
handful) confirmed why: the first average had landed on the low end of the idle animation's sway,
most visibly for `stinger` (was `up=0.40`, corrected to `1.00`) and `tail_end` (was `1.10`, corrected
to `1.43`). Re-averaged from the bigger sample.

Also fixed a few rounds ago: the entity no longer stops rendering (head/neck suddenly disappearing)
when the root hitbox leaves the camera frustum while the model still visibly extends into frame.

**No dedicated attack animation.** Like Izuchi, it fights using ordinary melee with no custom swing;
real attack clips exist (charge, bite, tailwhip, fireball) but wiring them needs the hurtbox
placement settled first, or a swing could land or miss for the wrong reason. Now that placement is
measured rather than guessed, this is worth revisiting.

- [ ] Renders, spawns via egg, idles/walks/runs with correct animation
- [ ] **F3+B: do the seven boxes sit on the body without reading low now**, specifically the tail
      end and stinger (the two that were re-centred this round)? Flag anything still off and I'll
      keep re-measuring that specific part
- [ ] Turning away no longer makes the head/neck suddenly vanish while still on screen
- [ ] Hitting a hurtbox (try the head, then the tail tip) reduces health
- [ ] Attacks and damages a nearby player using ordinary melee (no special swing — this is expected
      for now, not a bug)
- [ ] Death removes the whole creature and all seven parts
- [ ] No flight yet — it should behave as a purely ground-bound creature; this is expected, not a bug

## Rathalos (P4 ground wyvern, ground-only)

**Using Rathian's real (now re-centred) measurements as a proxy**, per your call ("rathalos and
rathian are almost identical, you can use the numbers for one on the other"): no `[rathalos]`
bone-probe lines have shown up in a log yet, but the two species share the same base skeleton and
closely similar proportions, so Rathian's offsets are plugged in directly rather than waiting on a
Rathalos-specific session — including this round's re-centring (the `stinger`/`tail_end` fix
applies here too, since Rathalos borrows the same numbers). Should be far closer than the old
offline-solved/hand-corrected guess, though not guaranteed pixel-perfect the way an actual Rathalos
measurement would be — if any one part still looks off, that's the part worth a real session for.
Its attack clips are blocked regardless: they reference bones that don't exist anywhere in this
species' model at all, confirmed by checking, not guessed — see `docs/DEFERRED.md`. That needs
actual art/model-editor work before it's even worth wiring.

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
