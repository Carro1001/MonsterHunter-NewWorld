package com.carro1001.mhnw.item;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

/**
 * The comparison build: the Giant Jawblade's charge posed by a GeckoLib clip instead of by 48
 * generated models. Same weapon in every other respect -- it extends {@link GiantJawbladeItem} and
 * overrides no combat, no timing and no attribute, so anything that feels different between the two
 * in-game is presentation and nothing else. That is the entire point of it existing.
 *
 * <p><b>This is a throwaway.</b> Two swords that fight identically is not a shipping state; one of
 * the two is meant to be deleted once the maintainer has looked at both. See {@code docs/DEFERRED.md}.
 *
 * <h2>How the clip stays in step with the charge, without a seek</h2>
 * {@code animations/item/giant_jawblade.animation.json} holds one 5-second {@code charge} clip whose
 * keyframes sit on the real tier boundaries -- 1.25s/2.25s/3.75s for 25/45/75 ticks -- and whose
 * length is exactly {@link GiantJawbladeItem#OVERCHARGE_TICKS}. The controller starts it the tick
 * the hold starts, so clip time and charge time are the same clock and no seek is needed. GeckoLib
 * 4.9.2 has no public seek and no {@code anim_time_update} MoLang support (both checked in the
 * sources jar, not assumed), so matching the durations is the only way to do this without the
 * re-anchoring machinery {@code animation/ServerTimedAnimationController} exists for on entities.
 *
 * <p>The known cost of that: a viewer who starts rendering a <em>different</em> player mid-charge
 * sees the wind-up from its beginning rather than its middle. Acceptable for a comparison, and the
 * reason this is not simply the shipping approach.
 *
 * <h2>Finding the holder, which GeckoLib does not hand us</h2>
 * {@link software.bernie.geckolib.renderer.GeoItemRenderer} puts only the stack, the tick and the
 * render perspective into the animation state -- there is no holder in it, checked in its source.
 * So the holder is recovered the same way {@link GiantJawbladeItem#chargeProgress} already
 * identifies it: by stack identity. {@code getUseItem()} returns the very object the renderer was
 * handed, and vanilla already syncs "is using an item" for every player, so this works for other
 * people's charges too and needs no packet of ours.
 *
 * <p>{@link #isPerspectiveAware()} is true because the hotbar icon renders continuously alongside
 * the held copy; without it both would share one animation manager and fight over it.
 *
 * <h2>The clip rotates and never translates, on purpose</h2>
 * The bone pivot is the grip ({@code [0,-3,0]} -- the artist's own rotation origin, shared by every
 * cube and by every element of the hand-authored 2D model), so rotation alone keeps the handle in
 * the player's hand. The first cut pivoted on the {@code .bbmodel}'s group origin, {@code [0,8,0]},
 * which is Blockbench's untouched default sitting halfway up the blade; it swung the handle clear
 * of the hand. Any position track in the clip reintroduces that, so there is none: if the wind-up
 * needs more travel, give it more rotation.
 */
public class GiantJawbladeGeoItem extends GiantJawbladeItem implements GeoItem {

    private static final RawAnimation CHARGE = RawAnimation.begin().thenPlayAndHold("charge");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public GiantJawbladeGeoItem(Properties properties) {
        super(properties);
    }

    /** Separate animation state per display context, so the hotbar icon and the held copy don't share one. */
    @Override
    public boolean isPerspectiveAware() {
        return true;
    }

    /**
     * <b>The predicate is an anonymous class on purpose, and this is load-bearing.</b> It has to
     * reach {@code Minecraft} to find who is holding the stack, and a dedicated server refuses to
     * load any {@code net.minecraft.client} class -- NeoForge's {@code RuntimeDistCleaner} throws
     * on it. Naming one anywhere in <em>this</em> class's own method bodies is enough: the verifier
     * resolves it when the class is linked, which happens on the server the moment
     * {@link com.carro1001.mhnw.registry.ModItems} constructs the item. Observed, not theorised --
     * a lambda here crashed mod loading on {@code LocalPlayer}.
     *
     * <p>An anonymous class is a separate class file, so it is loaded only when this method
     * actually runs, and this method only runs from GeckoLib's render path. Do not "tidy" it back
     * into a lambda or a private helper.
     *
     * <p>Worse, {@code runGameTestServer} exited zero through that crash -- no test ran and the
     * build reported success. A dist violation will not be caught by the suite; read the log.
     */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "charge", 0,
                new AnimationController.AnimationStateHandler<GiantJawbladeGeoItem>() {
                    @Override
                    public PlayState handle(AnimationState<GiantJawbladeGeoItem> state) {
                        ItemStack stack = state.getData(DataTickets.ITEMSTACK);
                        if (stack == null || !isCharging(stack)) {
                            // Stopping is not rewinding. The controller keeps currentRawAnimation
                            // across a STOP, and setAnimation only reloads a clip when the reload
                            // flag is set or a *different* animation is requested -- so without
                            // this, the second charge resumed the held last frame and the blade
                            // simply stayed vertical from then on. Observed, and it is why the
                            // first charge looked right and no later one did.
                            state.getController().forceAnimationReset();
                            return PlayState.STOP;
                        }
                        return state.setAndContinue(CHARGE);
                    }

                    /**
                     * Whether this exact stack is mid-charge. The lookup is over
                     * {@code level.players()} because only a player can use this weapon, and it
                     * compares by identity rather than by item so two players charging two
                     * jawblades do not pose each other's.
                     */
                    private boolean isCharging(ItemStack stack) {
                        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                        if (mc.level == null) {
                            return false;
                        }
                        for (Player player : mc.level.players()) {
                            if (player.isUsingItem() && player.getUseItem() == stack) {
                                return true;
                            }
                        }
                        // The first-person hand renderer can hand out a copy rather than the held
                        // object, so fall back to the one holder that view can belong to.
                        return mc.player != null && mc.player.isUsingItem()
                                && ItemStack.isSameItemSameComponents(mc.player.getUseItem(), stack);
                    }
                }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    /**
     * GeckoLib's own equivalent of {@code IClientItemExtensions.getCustomRenderer}. The renderer
     * class is resolved reflectively-late by the consumer, so naming a client-only class here does
     * not load it on a dedicated server -- the same reason {@code MHNWClient} can be referenced
     * from nowhere in common code.
     */
    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer renderer;

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (this.renderer == null) {
                    this.renderer = new com.carro1001.mhnw.client.MHNWClient.JawbladeRenderer();
                }
                return this.renderer;
            }
        });
    }
}
