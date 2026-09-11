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

Combat/behaviour diagnostics can be turned on by editing `run/config/mhnw-common.toml` and setting
`debugCombat = true` before launching (logs attack transitions, contact accepted/rejected, and — for
Great Izuchi specifically — draws the live attack volume with F3+B).

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
