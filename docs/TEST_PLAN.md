# Manual test plan

What still needs a human at a screen. Everything else (damage semantics, timing windows,
state-machine wedging, save/reload of gameplay facts) has a GameTest and runs headless via
`gradlew runGameTestServer` — see `MHNWGameTests.java`, currently 24 tests, all passing.

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

- [x] Renders, spawns via egg, idles/walks/runs with correct animation
- [x] F3+B hurtboxes sit on the body correctly (measured via the runtime bone probe)
- [x] Damage lands through a hurtbox; tail/body hit registers
- [x] Scratch, tail swipe, tail slam all fire, connect, and look reasonable in sequence
- [x] Windup plant + active-phase pace read as a real wind-up and follow-through
- [x] Death plays its authored clip fully before the corpse is removed
- [ ] **A11 natural spawning** — deferred by your call; wired but never observed (see `docs/DEFERRED.md`)
- [ ] **A10 navigation scenarios** — corner, body-wide passage, narrow passage, step, unreachable
      target — deferred by your call (see `docs/DEFERRED.md`)
- [ ] **A08/A09 two-client agreement** — needs a dedicated server + two clients, which I cannot run;
      deferred to polishing (see `docs/DEFERRED.md`)

## Aptonoth (P3 passive herbivore)

Just added a fix this pass (client never played its RUN animation while fleeing — always looked
like it was walking even at full panic speed). Please recheck the fled-animation item specifically.

- [ ] Renders, spawns via egg, idles/walks with correct animation
- [ ] **Re-check:** when hurt, visibly runs (not just walks) away — this was broken and just fixed
- [ ] Eats grass occasionally (the `eat` animation should play when it's actually eating, not just
      standing still) — grass must be nearby (short/tall grass on a grass block) for this to trigger
      at all; it's a low-probability vanilla check, so may take a while to observe
- [ ] Retaliates (rarely) if cornered while already fleeing — low priority to confirm, documented as
      a soft, not-guaranteed behaviour
- [ ] Does not attack players or other mobs unprovoked

## Toad (P3 endemic hazard)

- [ ] Renders with the correct one of four textures (poison/sleep/paralysis/blast) — spawn several to
      see variety, since it's chosen randomly at spawn
- [ ] Sits mostly still, occasional slow hop
- [ ] Approaching it or hitting it triggers the `fuse` telegraph animation, then a released cloud
- [ ] Poison variant: nearby player takes poison damage over time
- [ ] Paralysis variant: nearby player is heavily slowed
- [ ] Sleep variant: nearby player gets nausea + mild slowness (**not** any kind of control lock —
      flag immediately if it ever blocks or overrides your input, that would be a real bug)
- [ ] Blast variant: a real small explosion (knockback/damage), **no terrain destroyed**
- [ ] After releasing, it goes quiet for a while, then can trigger again later (this exact recovery
      was a real bug caught by GameTest, not visually — worth a casual confirm that it does eventually
      go off a second time on a long enough encounter, but not urgent since the automated test covers it)

## Flashbug (P3 flying endemic life)

- [ ] Flies around, hovers, does not land often
- [ ] Approaching it or hitting it triggers a brief flash telegraph, then a blinding flash
- [ ] Blindness only affects things that could actually see the flash (nothing through walls)
- [ ] Same recovery-after-cooldown note as the toad above

## Bug / bitterbug/godbug (P3 ambient life)

- [ ] Renders with the hand-modeled mesh (not GeckoLib) — legs and antennae move procedurally while
      walking
- [ ] Two variants: common bitterbug, rare golden godbug (~2% of spawns) — may need several spawn-egg
      uses to see a godbug
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

This one needs the most scrutiny of anything so far. Its seven hurtboxes were solved offline from
the geometry rather than measured live with the bone probe (unlike everything else in this file),
because I have no way to spawn it, walk around it, or trigger anything myself without you at the
keyboard. See `docs/DEFERRED.md` for exactly what is and is not trustworthy about the numbers.

**No dedicated attack animation.** Like Izuchi, it fights using ordinary melee with no custom swing;
unlike Izuchi, real attack clips exist (charge, bite, tailwhip, fireball) but need the same
bone-probe measurement pass Great Izuchi's attacks got before they can be wired safely — attempting
that blind risks shipping a volume that misses or hits through the wrong geometry, worse than not
having it.

- [ ] Renders, spawns via egg, idles/walks/runs with correct animation
- [ ] **F3+B: do the seven green hurtboxes sit on the body?** This is the important one — if
      anything looks badly placed (especially vertically — see the deferred-doc note about the feet
      solving lower than Great Izuchi's did), that confirms the offline estimate needs correcting,
      and I'll need a bone-probe session against it the same way Great Izuchi's claw got fixed
- [ ] Hitting a hurtbox (try the head, then the tail tip) reduces health
- [ ] Attacks and damages a nearby player using ordinary melee (no special swing — this is expected
      for now, not a bug)
- [ ] Death removes the whole creature and all seven parts
- [ ] No flight yet — it should behave as a purely ground-bound creature; this is expected, not a bug

## Rathalos (P4 ground wyvern, ground-only)

Same situation as Rathian (offline-solved hurtboxes, no dedicated attack, ordinary melee combat),
but its attack clips are further blocked: they reference bones that don't exist anywhere in this
species' model at all, confirmed by checking, not guessed — see `docs/DEFERRED.md`. That needs
actual art/model-editor work before it's even worth a bone-probe pass, unlike Rathian's, which
just needs measuring.

- [ ] Renders, spawns via egg, idles/walks/runs (walk uses `walk_normal`/`walk_aggro`, no separate
      "run" clip exists for this species — expected, not a bug)
- [ ] **F3+B: do the six green hurtboxes sit on the body?** Same importance as Rathian's row
- [ ] Hitting a hurtbox reduces health
- [ ] Attacks and damages a nearby player using ordinary melee
- [ ] Death removes the whole creature and all six parts
- [ ] No flight yet — ground-bound only; expected, not a bug
