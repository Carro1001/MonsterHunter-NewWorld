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

Two fixes this pass, both from your screenshot/gameplay feedback:

- **Hurtbox shape**: was a single, near-square AABB that read as too tall/boxy and offset toward
  the tail. Now three `MonsterPart` hurtboxes (body/head/tail), the same multipart approach the
  large monsters use, offline-solved from the idle pose — **not yet measured live**, same caveat as
  Rathian/Rathalos below, so please check the fit.
- **Flee duration**: vanilla `PanicGoal` only ever dashes to one point ~5 blocks away then stops;
  `AptonothPanicGoal` now keeps it fleeing for a sustained ~5 seconds after a hit instead of one
  short hop. (You mentioned this might have been a creative-mode artifact — this fix addresses the
  underlying mechanism regardless, so worth rechecking in survival too if you get a chance.)

- [ ] Renders, spawns via egg, idles/walks with correct animation
- [ ] **New: F3+B — do the three green hurtboxes (body/head/tail) actually sit on the body now, or
      still look off?** These are offline-solved, not measured, same as Rathian/Rathalos
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

Reworked this pass per your feedback: players are now completely excluded from being blinded, a
non-player victim must actually be facing it (not just able to see it), it takes no fall damage, and
it should stay low (within ~4 blocks of the ground) instead of drifting up out of reach.

- [ ] Flies around, hovers, does not land often
- [ ] **New: stays low** — roughly 3-4 blocks above the ground under it, not drifting far higher
- [ ] **New: never takes fall damage**, however it moves
- [ ] Approaching it or hitting it triggers a brief flash telegraph, then a blinding flash
- [ ] **Re-check: you (the player) are never blinded by it**, no matter how you're looking at it
- [ ] **Re-check: a nearby mob only gets blinded if it's actually facing the bug** — one facing away
      should be unaffected even if it's close and has line of sight
- [ ] Blindness (for a facing non-player) only affects things that could actually see the flash
      (nothing through walls)
- [ ] After releasing, it goes quiet for a while, then can flash again later (this exact recovery
      was a real bug caught by GameTest, not visually — worth a casual confirm that it does eventually
      go off a second time, but not urgent since the automated test covers it)

## Bug / bitterbug/godbug (P3 ambient life)

Health lowered to a flat 1 hit point this pass per your feedback — any hit, including a bare-handed
punch, should kill it in one now, since it's meant to be collected rather than fought.

- [ ] Renders with the hand-modeled mesh (not GeckoLib) — legs and antennae move procedurally while
      walking
- [ ] **Re-check: dies to a single hit**, including an unarmed punch
- [ ] Two variants: common bitterbug, rare golden godbug (~2% of spawns) — may need several spawn-egg
      uses to see a godbug (or summon one directly to confirm the model/texture without waiting on
      the roll: `/summon mhnw:bug ~ ~ ~ {Variant:1b}`)
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

**Your screenshots confirmed the seven hurtboxes are genuinely misplaced** — floating above/in front
of the wings, nowhere near the actual body — not just "needs a look," a confirmed bug. My offline
FK-solved offsets were wrong for this species; I can't fix that blind a third time (guessed once for
the original placement, guessed again for Great Izuchi's own claw fix technique applied here, both
proved wrong against your screenshots). This needs a live bone-probe measurement session: F3+B
overlay on, walk around it, and I read the actual runtime bone positions the same way Great Izuchi's
claw got fixed originally, rather than solving the geometry offline again. **Flagging this as blocked
on that session, not something I can just re-guess and ship.**

Also fixed this pass: the entity no longer stops rendering (head/neck suddenly disappearing) when
you turn away and its root hitbox leaves the camera frustum while the model still visibly extends
into frame — vanilla only inflates the culling box by a flat 0.5 blocks, nowhere near enough for a
creature whose neck/tail reach several blocks past the root box. Confirm this is actually fixed
visually; I can't render a frame to check it myself.

**No dedicated attack animation.** Like Izuchi, it fights using ordinary melee with no custom swing;
unlike Izuchi, real attack clips exist (charge, bite, tailwhip, fireball) but need the same
bone-probe measurement pass Great Izuchi's attacks got before they can be wired safely — attempting
that blind risks shipping a volume that misses or hits through the wrong geometry, worse than not
having it.

- [ ] Renders, spawns via egg, idles/walks/runs with correct animation
- [ ] **F3+B: confirmed broken last round — still floating above/in front of the wings?** Expected
      yes until a bone-probe measurement session happens; this checkbox is really "still broken the
      same way," not a fresh look
- [ ] **New: turning away no longer makes the head/neck suddenly vanish** while still on screen
- [ ] Hitting a hurtbox (try the head, then the tail tip) reduces health
- [ ] Attacks and damages a nearby player using ordinary melee (no special swing — this is expected
      for now, not a bug)
- [ ] Death removes the whole creature and all seven parts
- [ ] No flight yet — it should behave as a purely ground-bound creature; this is expected, not a bug

## Rathalos (P4 ground wyvern, ground-only)

Same situation as Rathian — **your screenshots confirmed its six hurtboxes are also badly
misplaced**, same root cause (offline FK solve proved wrong), same fix needed (a live bone-probe
measurement session, not another blind offline solve). Its attack clips are further blocked: they
reference bones that don't exist anywhere in this species' model at all, confirmed by checking, not
guessed — see `docs/DEFERRED.md`. That needs actual art/model-editor work before it's even worth a
bone-probe pass, unlike the hurtbox placement, which just needs measuring.

Also fixed this pass: same culling fix as Rathian/Great Izuchi (head/neck no longer vanishes when
the root hitbox leaves frame while the model is still visible).

- [ ] Renders, spawns via egg, idles/walks/runs (walk uses `walk_normal`/`walk_aggro`, no separate
      "run" clip exists for this species — expected, not a bug)
- [ ] **F3+B: confirmed broken last round — still floating out of place?** Same "still broken the
      same way" framing as Rathian's row, pending a bone-probe session
- [ ] **New: turning away no longer makes the head/neck suddenly vanish** while still on screen
- [ ] Hitting a hurtbox reduces health
- [ ] Attacks and damages a nearby player using ordinary melee
- [ ] Death removes the whole creature and all six parts
- [ ] No flight yet — ground-bound only; expected, not a bug
