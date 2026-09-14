# Weapon posing

How a held weapon is made to move in this mod, what each option can and cannot reach, and the
numbers you have to derive rather than eyeball. Written while getting the Giant Jawblade's charge to
read, so that the second weapon does not re-learn it.

Everything marked **verified** was read out of the actual sources jar or observed in a running game
during R3/alpha. Everything marked **untried** is a real API whose shape has been checked but which
nothing in this repo uses yet — treat it as a starting point, not as a recipe known to work.

---

## 1. Decide what actually has to move

Three different things can move, they are reached by three different mechanisms, and confusing them
wastes a round:

| What moves | Mechanism | Reaches other players? |
|---|---|---|
| **The weapon's own geometry** | GeckoLib clip, or swapped models | Yes |
| **The player's arms, third person** | `IClientItemExtensions.getArmPose` + a custom `HumanoidModel.ArmPose` | Yes |
| **The arm + weapon as one unit, first person** | `IClientItemExtensions.applyForgeHandTransform` | N/A — local player only |
| **The player's whole body** | Nothing in vanilla or NeoForge. Needs a library | — |

**A GeckoLib clip on the item never moves the arms.** GeckoLib touches `HumanoidModel` only for
armor (`GeoArmorRenderer`, `HumanoidArmorLayerMixin`); an item clip poses the item's own bones and
nothing else. If the requirement is "the hunter shoulders the weapon", that is rows 2 and 3, not
row 1.

## 2. The one number you must derive: where the hand is

**This cost a playtest round. Do not guess it, and do not use the artist's modelling origin.**

A bone that rotates about the wrong point swings the grip out of the player's hand. The hand is not
where the model was drawn around — it is fixed by the model's own `display` block:

Vanilla's `ItemRenderer.render` applies the display transform, then `translate(-0.5, -0.5, -0.5)`,
then draws the model at 1/16 scale. So a Java-model point `P` lands exactly on the hand when

```
P = 8 - translation            (Java model coordinates)
pivot = P - 8 = -translation   (geo / Blockbench coordinates)
```

For the Giant Jawblade's `thirdperson_righthand` translation of `[0, 7, 1.75]`, the hand is at geo
`[0, -7, -1.75]`, and that is the bone pivot. The artist's own rotation origin for the same model is
`[0, -3, 0]` — four units up the handle, where the blade was *modelled* around. Pivoting there
visibly threw the weapon out of the grip mid-charge.

`r3GeckoJawbladeMatchesTheWeaponAndItsChargeClock` recomputes this from the model file rather than
hardcoding it, so a new `display` block and a stale pivot cannot disagree silently. **Copy that
assertion for the next weapon.**

**The translation is also how you choose where along the grip the hunter holds.** The hand lands on
whatever model point the formula names, so the display block decides that too -- it is not only a
placement. The Giant Jawblade shipped at `[0, 7, 1.75]`, putting the hand at geo y `-7` on a handle
running `-9..2`: two units off the pommel, which read on screen as holding the sword by its very
end. Mid-handle is `-3.5`, so the translation became `3.5`. When a weapon looks mis-gripped, this is
the number, and the pivot has to be re-derived with it.

Two consequences:

- **The pivot is per display context, but a geo file has only one.** First person uses a different
  translation (and a rotation, and a scale), so one pivot cannot be correct in both. Ours is set for
  third person. If a weapon needs both to be exact, the lever is `isPerspectiveAware()` plus a
  separate clip per context — not a second pivot.
- **Never add a `position` track to a charge clip.** Translating the bone is exactly the thing that
  moves the weapon back off the hand the pivot exists to keep it on. More wind-up means more
  rotation. The test asserts the absence of a position track for this reason.

## 3. GeckoLib on an item: the wiring, and three traps

**Verified**, this is what `GiantJawbladeItem` does.

Files, resolved by `DefaultedItemGeoModel(ResourceLocation(MOD_ID, "<name>"))`:

```
assets/mhnw/geo/item/<name>.geo.json
assets/mhnw/animations/item/<name>.animation.json
assets/mhnw/textures/item/<name>.png          # .withAltTexture(...) if the art keeps another name
assets/mhnw/models/item/<name>.json           # "parent": "builtin/entity" + the display block
```

The item implements `GeoItem` and hands back a renderer through `createGeoRenderer`
(GeckoLib's own equivalent of `IClientItemExtensions.getCustomRenderer`).

### Trap 1: `builtin/entity`, never `builtin/generated`

The BEWLR model must parent `builtin/entity` and carries no `elements` of its own. A 3D item model
that is *not* GeckoLib-rendered must declare **no parent at all** — every vanilla item parent roots
at `builtin/generated`, and `ModelBakery` then throws the geometry away and builds quads from
`layer0`, which a cuboid model does not have. That renders a working item with a shadow and no
visible geometry, logging nothing. Both rules are guarded by GameTests, because model baking is
client-only.

### Trap 2: naming a client class in the item's own method bodies crashes the dedicated server

`RuntimeDistCleaner` refuses to load `net.minecraft.client.*` on a server, and the verifier resolves
it when the class is *linked* — which happens the moment `ModItems` constructs the item. A lambda
body compiles to a synthetic method **on the same class** and so is not lazy enough.

Put the client access in an **anonymous class**. That is a separate class file, loaded only when the
enclosing method actually runs, and `registerControllers` only runs from the render path.

> **`runGameTestServer` exits zero when the mod fails to load like this.** No test runs and Gradle
> prints BUILD SUCCESSFUL. A green build is not proof the suite ran — check for the
> `GAME TESTS COMPLETE` line, and for `invalid dist` in the log.

### Trap 3: stopping a controller is not rewinding it

`AnimationController.setAnimation` reloads a clip only when the reload flag is set, or when a
*different* animation is requested. Returning `PlayState.STOP` leaves `currentRawAnimation` in
place, so the next activation resumes a `hold_on_last_frame` clip **on its held last frame** — for a
charge, the fully-wound pose, forever. Call `forceAnimationReset()` on the not-playing branch.

Also set `isPerspectiveAware()` to `true` on any item whose hotbar icon renders alongside the held
copy; without it both share one animation manager and fight over it.

## 4. Keeping a clip in step with a gameplay clock

**GeckoLib 4.9.2 has no public seek and no `anim_time_update` MoLang support.** Both verified in the
sources jar; `forceAnimationReset()` reloads rather than seeks, and a speed modifier scales elapsed
time rather than moving it. `GeoModel.applyMolangQueries` exists but there is no time field to feed.

So there are two honest ways to sync a clip to a gameplay timer:

1. **Match the durations** and start them together. The jawblade's clip is 5.0s because
   `OVERCHARGE_TICKS` is 100, and its keyframes sit at 1.25/2.25/3.75s because `TIER_TICKS` is
   {25,45,75}. The GameTest recomputes both from the constants, so changing a tier without
   re-authoring the clip fails a test instead of silently desyncing the pose from the damage.
   **Cost:** a viewer who starts rendering someone *else* mid-action sees the clip from its
   beginning.
2. **Re-anchor on every render**, which is what `animation/ServerTimedAnimationController` does for
   entities (R0b). It runs GeckoLib's own initialization pass, then re-anchors and lets it sample
   again in the same render call. That is the machinery to reach for if mid-action joins matter;
   read its class doc before touching it, including why clip time is `age - TRANSITION_TICKS` and
   not raw age.

## 5. Moving the player's arms — what is actually available

### Third person: a custom `ArmPose` — **in use, see `client/MHNWArmPoses`**

`IClientItemExtensions.getArmPose(LivingEntity, InteractionHand, ItemStack)` returns a
`HumanoidModel.ArmPose`, and `ArmPose` is an `IExtensibleEnum` with a constructor taking an
`IArmPoseTransformer`:

```java
void applyTransform(HumanoidModel<?> model, LivingEntity entity, HumanoidArm arm);
```

Inside it you set `model.rightArm` / `model.leftArm` rotations directly, and the interface's own
javadoc names `getUseItemRemainingTicks()` as the intended driver — which is exactly the clock every
charge in this mod already derives its tier from. `isTwoHanded()` is what puts the off hand on the
grip too.

**No third-party library. No mixin.** Adding a constant needs NeoForge's enum extension
(`net.neoforged.fml.common.asm.enumextension`): an `enumextensions.json` naming the enum, the
constant, the constructor descriptor and a `EnumProxy` field to take the parameters from.

**Two things that cost real time here:**

- **`enumExtensions` goes inside `[[mods]]`, not at the root of `neoforge.mods.toml.`** FML reads it
  via `IModInfo.getConfig().getConfigElement("enumExtensions")` — the per-mod config. At the root it
  is silently ignored: no warning, no error, and the first symptom is `EnumProxy.getValue()` throwing
  inside the render path and crashing the game on right-click.
- **Never let `getValue()` reach the renderer unguarded.** It throws when the constant was not
  registered. Catch it, fall back to `null` (which is a legal `getArmPose` answer meaning "ordinary
  pose"), and log once. A cosmetic stance must not be able to take rendering down.

A dedicated server does load cleanly with this in place — verified, because `ArmPose` is a client
class and the extension only processes when that class loads, which never happens on a server.

### First person: `applyForgeHandTransform` — **untried here, call site verified**

```java
boolean applyForgeHandTransform(PoseStack, LocalPlayer, HumanoidArm, ItemStack,
                                float partialTick, float equipProcess, float swingProcess)
```

It transforms the hand and the item together, which is the only way to make the grip stay put while
the weapon swings in first person.

**Returning `true` replaces vanilla's entire arm-transform block**, not just the use-animation part
— including `applyItemArmTransform`, which is the ordinary equip placement and bob
(`translate(±0.56, -0.52 + equippedProgress * -0.6, -0.72)`). Reproduce that before adding your own
rotation, or the weapon renders in the wrong place entirely.

If a weapon uses this, its GeckoLib clip should return `PlayState.STOP` for the first-person
perspectives, or the item gets rotated twice. `isPerspectiveAware()` and
`DataTickets.ITEM_RENDER_PERSPECTIVE` are how the predicate tells which context it is in.

### What is **not** available

- **Posing the player's body, legs or stance.** Needs KosmX's playerAnimator or equivalent. Priced
  in `DEFERRED.md`; still a third external dependency plus an authored asset per stance.
- **`UseAnim.SPEAR` as a greatsword wind-up.** Tried, rejected on sight: it raises the weapon
  vertically overhead like a trident throw. Recorded in `GiantJawbladeItem`'s class doc. Do not
  re-propose it. `BOW`/`CROSSBOW` pose a drawing hand and `BLOCK` a shield arm; none are closer.
- **A render hook that receives the holder and the stack together, other than the two above.** An
  item property function receives the holder (that is how the superseded 48-model approach worked,
  and it remains the only such hook); a BEWLR
  and a baked-model wrapper both get the stack alone. GeckoLib's item animation state carries only
  `ITEMSTACK`, `TICK` and `ITEM_RENDER_PERSPECTIVE` — **no holder** — so an item clip that depends on
  who is holding it must recover the holder by stack identity against `level.players()`, the way
  `GiantJawbladeItem` does.

## 6. Converting a Blockbench `.bbmodel` to `geo.json` by hand

Prefer Blockbench's own GeckoLib export. If you are converting anyway (we did, to keep the artist's
file as the source of truth):

- **X is mirrored.** Bedrock geometry's cube `origin` is `[-to.x, from.y, from.z]`, not `from`. A
  symmetric or zero-width blade makes this a no-op; a wider one will not.
- **`uv_offset` is omitted when it is `[0,0]`.** Derive it from the east face instead: in the box-UV
  layout that face sits at `(u, v + depth)`, so `u = east.uv[0]`, `v = east.uv[1] - depth`.
- **Take the pivot from section 2, not from the file.** Neither the group origin (Blockbench's
  untouched default) nor the cube origins are the hand.

## 7. Checklist for the next weapon

1. Author the model and its `display` block. That block, not the geometry, decides where the hand is.
2. Check where that puts the hand along the grip before anything else -- `-translation` against the
   handle cube's own extent. Then compute the bone pivot as `-thirdperson_righthand.translation` and
   assert it in a GameTest.
3. Decide what must move — weapon only, or arms too — using the table in section 1.
4. If a clip is driven by a gameplay timer, match durations and assert the timings against the
   constants, or re-anchor per render. Do not invent a third way.
5. Rotation only in charge clips. No position tracks.
6. Client code in anonymous classes; check the log for `GAME TESTS COMPLETE` and `invalid dist`.
7. The visual result cannot be tested headlessly. Add a lettered row to `docs/TEST_PLAN.md` and get
   a human to look, in **both** persons.
