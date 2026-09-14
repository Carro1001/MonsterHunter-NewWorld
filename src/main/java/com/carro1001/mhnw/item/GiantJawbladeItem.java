package com.carro1001.mhnw.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.function.Predicate;

/**
 * R3's one weapon: an ordinary iron-tier sword that is slower and harder than vanilla's, plus a
 * three-tier charged strike you hold and release.
 *
 * <h2>It is a vanilla sword first</h2>
 * Left-click is entirely {@link SwordItem}: the tier supplies durability, mining behaviour and
 * enchantability, and {@link SwordItem#createAttributes} supplies the damage and speed modifiers.
 * The two numbers are expressed the way vanilla expresses them, so the tooltip and the player's
 * real attributes -- not a constant of ours -- are what a test asserts:
 * <pre>
 *   damage  1.0 (player base) + 2.0 (iron tier bonus) + {@link #DAMAGE_MODIFIER} = 9.0
 *   speed   4.0 (player base) + {@link #SPEED_MODIFIER}                          = 0.8
 * </pre>
 * Only the repair material differs from iron: bone, because this is a bone weapon carved off a
 * monster rather than smelted.
 *
 * <h2>The charge: hold, watch the tier, release</h2>
 * Holding main-hand use builds through three tiers at {@link #TIER_TICKS}, each announced by its
 * own cue. <em>Releasing</em> is what swings, and the tier reached is the damage that lands
 * ({@link #TIER_DAMAGE}). Release before tier one and nothing happens at all -- no strike, no
 * cooldown, no wear. Hold past {@link #FIZZLE_TICKS} and the charge dies where it stands: the blade
 * drops, a dull cue plays, and releasing afterwards does nothing. The weapon never swings by itself:
 * overcharging costs you the charge rather than spending it badly.
 *
 * <p>Every strike, at every tier, traces {@link #REACH} blocks down the player's own view vector
 * and hits at most one thing. Hit or miss, the item goes on a {@link #RECOVERY_TICKS} cooldown.
 *
 * <p>Three things this deliberately does not have. There is <em>no state of ours</em>: vanilla's
 * active-use countdown is the charge, so release, swap, death and disconnect already end it, the
 * tier is derived from that counter rather than stored, and nothing is saved, synched or attached.
 * There is <em>no damage pipeline of ours</em>: the tier bonus is a transient
 * {@link Attributes#ATTACK_DAMAGE} modifier applied around one {@link Player#attack} call, so
 * enchantments, attack events, knockback, durability, sounds, stats, the player-caused damage
 * source and therefore {@code CarveState} attribution all scale and fire on their ordinary path --
 * including through a {@code MonsterPart}, which NeoForge's own patch to that method resolves to
 * its parent. And there is <em>no target search of ours</em>:
 * {@link ProjectileUtil#getHitResultOnViewVector} is the same block-clipped trace vanilla's own
 * projectiles use, so a wall stops the strike because the trace stops, not because of a check.
 *
 * <p>{@code Level.getEntities} includes NeoForge {@code PartEntity} instances, so a big monster's
 * hurtbox is selectable here with no multipart-specific code at all.
 *
 * <h2>Why the pose is {@link UseAnim#NONE}</h2>
 * {@code SPEAR} raises the weapon vertically overhead -- vanilla's trident wind-up -- which reads
 * nothing like winding up a greatsword, and was reported as exactly that. There is no built-in pose
 * for a two-handed charge and this packet adds no player-animation library, so the weapon stays in
 * its normal grip and the charge is communicated by its tier cues and by how hard it slows you.
 */
public class GiantJawbladeItem extends SwordItem implements software.bernie.geckolib.animatable.GeoItem {

    /**
     * Charge ticks needed to reach tier one, two and three. A greatsword charge is meant to be a
     * decision you commit to, not a tap.
     *
     * <p>Tier one is <b>25 and not 20 for a reason that is not feel</b>: {@link Player#attack}
     * scales damage by vanilla's attack-strength ramp, and at this weapon's 0.8 attack speed the
     * swing timer is 20/0.8 = 25 ticks. A charge shorter than that, begun right after a left-click,
     * would land scaled-down damage instead of the number this class advertises -- quadratically,
     * so a 20-tick tier one measured 6.64 rather than 9.0 (PR #10 review, P1). Because
     * {@code attackStrengthTicker} counts up during the hold, making the shortest charge equal the
     * swing timer means holding one always refills it, and every tier lands its stated damage from
     * any starting state. Keep {@code TIER_TICKS[0] >= 25} if the attack speed ever changes;
     * {@code r3ChargeFromAnUncooledWeaponStillLandsItsTier} fails if it does not.
     */
    public static final int[] TIER_TICKS = {30, 70, 125};

    /** Total attack damage each tier lands, in the same units the tooltip shows. Tier one is simply
     * the weapon's own 9.0, so a short charge buys reach and a long one buys damage as well. */
    public static final float[] TIER_DAMAGE = {9.0F, 12.5F, 16.0F};

    /**
     * Hold this long and the charge fizzles out: the weapon does not swing, the blade drops back to
     * rest, and releasing afterwards does nothing at all.
     *
     * <p>It replaced an auto-swing at tier one's damage, which put a hit on the screen that the
     * player never asked for. Overcharging now costs you the charge instead of spending it badly,
     * which is the Monster Hunter reading of the same mistake.
     *
     * <p>Like every other part of this weapon it is <b>derived, never stored</b>:
     * {@link #tierFor} simply refuses to name a tier past this point, so "the charge is dead" needs
     * no field, no component and no packet, and cannot survive a reload.
     */
    public static final int FIZZLE_TICKS = TIER_TICKS[TIER_TICKS.length - 1] + 60;

    /**
     * What {@link #getUseDuration} reports: long enough that vanilla never ends the hold on its own.
     *
     * <p>The bow's own value, and for the bow's own reason -- the item wants to be held until the
     * player decides otherwise. {@link #finishUsingItem} is unreachable in practice as a result,
     * which is exactly the "it shouldn't auto release" contract; the charge count is still derived
     * from this number minus vanilla's countdown, so nothing else about the timing changes.
     */
    public static final int USE_DURATION_TICKS = 72000;

    /**
     * Recovery after a completed strike, hit or miss. The commitment.
     *
     * <p>50 ticks as of the alpha, up from 30. What it actually gates is worth being precise about:
     * it is an {@code ItemCooldowns} entry, and vanilla item cooldowns block <em>use</em>, not
     * attacks -- so this stops you starting another charge for two and a half seconds, and does
     * nothing to left-click rate. Throttling left-click would mean lowering {@link #SPEED_MODIFIER}
     * instead, which drags {@link #TIER_TICKS}[0] up with it (see the attack-strength note below).
     */
    public static final int RECOVERY_TICKS = 50;

    /** Maximum reach of the charged strike, in blocks. */
    public static final double REACH = 4.5D;

    /** Added to the tier's own +2.0, on top of the player's 1.0 base, for a total of 9.0. */
    public static final float DAMAGE_MODIFIER = 6.0F;

    /** Added to the player's 4.0 base, for 0.8 attacks per second. */
    public static final float SPEED_MODIFIER = -3.2F;

    /**
     * Horizontal movement kept per tick while charging, on top of the 20% input scaling vanilla
     * already applies to any held use ({@code LocalPlayer.aiStep}). The result is a heavy crawl:
     * committing to the swing costs you the ability to reposition.
     *
     * <p>Deliberately a per-tick multiply on the existing motion rather than a movement-speed
     * attribute modifier. A modifier has to be added and then removed again on every path that can
     * end a charge -- release, completion, swap, death, dropping the weapon mid-hold -- and one
     * missed path leaves a player permanently slowed with no way to clear it. This applies only on
     * the ticks a charge is genuinely running, so it has nothing to clean up and nothing to leak.
     */
    public static final double CHARGE_MOVEMENT_SCALE = 0.35D;

    public static final Tier TIER = Tiers.IRON;

    /** The transient tier bonus. One id, added and removed around a single attack. */
    private static final ResourceLocation CHARGE_BONUS_ID =
            ResourceLocation.fromNamespaceAndPath(com.carro1001.mhnw.MHNW.MOD_ID, "charged_strike");

    /** Vanilla's own picking rule, narrowed to things an attack can actually land on. */
    private static final Predicate<Entity> TARGETS =
            EntitySelector.NO_SPECTATORS.and(Entity::isPickable).and(Entity::isAttackable);

    /** One escalating cue per tier; riptide's three levels are already a rising set. */
    private static final SoundEvent[] TIER_CUES = {
            SoundEvents.TRIDENT_RIPTIDE_1.value(), SoundEvents.TRIDENT_RIPTIDE_2.value(),
            SoundEvents.TRIDENT_RIPTIDE_3.value()};

    public GiantJawbladeItem(Properties properties) {
        super(TIER, properties.attributes(createAttributes(TIER, DAMAGE_MODIFIER, SPEED_MODIFIER)));
    }

    private static final software.bernie.geckolib.animation.RawAnimation CHARGE =
            software.bernie.geckolib.animation.RawAnimation.begin().thenPlayAndHold("charge");

    /**
     * One release arc per tier, and the ordinary left-click's arc too -- one weapon, one way of
     * moving. Each starts at its own tier's wound angle and sweeps further the harder the charge
     * was, so a tier three release covers roughly twice the ground of a tier one in the same time.
     *
     * <p>Its clock is vanilla's own swing, which costs nothing and reaches everyone: {@code swing()}
     * is already called at the end of every {@link #strike}, and {@code swinging} is synced to every
     * client that can see the holder. Which arc to play comes from
     * {@link com.carro1001.mhnw.registry.ModDataComponents#SWING_TIER}, written at the moment of
     * release and synced with the stack.
     *
     * <p><b>All three are the same length, deliberately.</b> The clip moves the weapon and not the
     * arm -- {@code HumanoidModel.setupAttackAnimation} runs after the arm pose and overwrites it,
     * with no hook between -- so the blade is stuck with vanilla's swing duration for company. A
     * heavier charge therefore reads as a <em>wider, faster</em> sweep rather than a slower one; a
     * genuinely slower follow-through needs the arm, which needs a mixin or a player-animation
     * library. See {@code docs/WEAPON_POSING.md}.
     */
    private static final software.bernie.geckolib.animation.RawAnimation[] SWINGS = {
            software.bernie.geckolib.animation.RawAnimation.begin().thenPlay("swing_1"),
            software.bernie.geckolib.animation.RawAnimation.begin().thenPlay("swing_2"),
            software.bernie.geckolib.animation.RawAnimation.begin().thenPlay("swing_3")};

    /**
     * Blend ticks between clips -- GeckoLib's lerp, and the only one available.
     *
     * <p>It smooths charge-into-swing and swing-into-rest. It cannot smooth a controller
     * {@code STOP}, which is instant; a gap where neither clip is playing snaps the bone to the
     * model's rest pose no matter how long this is. See {@link #beginSwing}.
     */
    private static final int TRANSITION_TICKS = 2;

    private final software.bernie.geckolib.animatable.instance.AnimatableInstanceCache cache =
            software.bernie.geckolib.util.GeckoLibUtil.createInstanceCache(this);

    /** Separate animation state per display context, so the hotbar icon and the held copy don't share one. */
    @Override
    public boolean isPerspectiveAware() {
        return true;
    }

    /**
     * Whether the charge clip should play in a given render context: only in a hand.
     *
     * <p><b>{@link #isPerspectiveAware()} alone does not do this.</b> It gives each display context
     * its own {@code AnimatableManager}, but every one of them still runs the same predicate -- and
     * the predicate finds the charge by stack identity, which the hotbar icon and the held copy
     * share, because they are the same {@code ItemStack} object. So the inventory icon wound itself
     * up in real time along with the weapon. The icon is meant to be a picture of the item, posed
     * once by the model's own {@code gui} display transform.
     *
     * <p>Deliberately a whitelist of the four hand contexts rather than a blacklist of GUI: an item
     * frame, a dropped stack, an armour stand's hand and a head slot should all be still too, and a
     * blacklist would have let each new context animate until someone noticed.
     *
     * <p>Common code and a common enum on purpose, so the rule is reachable from a GameTest -- what
     * renders cannot be checked headlessly, but which contexts are allowed to move can.
     */
    public static boolean animatesIn(net.minecraft.world.item.ItemDisplayContext context) {
        return context == net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                || context == net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || context == net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
                || context == net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
    }

    @Override
    public software.bernie.geckolib.animatable.instance.AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
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
    public void registerControllers(
            software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new software.bernie.geckolib.animation.AnimationController<>(
                this, "charge", TRANSITION_TICKS,
                new software.bernie.geckolib.animation.AnimationController
                        .AnimationStateHandler<GiantJawbladeItem>() {
                    @Override
                    public software.bernie.geckolib.animation.PlayState handle(
                            software.bernie.geckolib.animation.AnimationState<GiantJawbladeItem> state) {
                        net.minecraft.world.item.ItemDisplayContext context = state.getData(
                                software.bernie.geckolib.constant.DataTickets.ITEM_RENDER_PERSPECTIVE);
                        ItemStack stack = state.getData(
                                software.bernie.geckolib.constant.DataTickets.ITEMSTACK);
                        if (context == null || !animatesIn(context) || stack == null) {
                            return stop(state);
                        }
                        if (isCharging(stack)) {
                            return state.setAndContinue(CHARGE);
                        }
                        if (isSwinging(stack)) {
                            return state.setAndContinue(SWINGS[swingTier(stack)]);
                        }
                        return stop(state);
                    }

                    /**
                     * Stopping is not rewinding. The controller keeps {@code currentRawAnimation}
                     * across a STOP, and {@code setAnimation} only reloads a clip when the reload
                     * flag is set or a <em>different</em> animation is requested -- so without the
                     * reset, the second charge resumed the held last frame and the blade simply
                     * stayed wound up from then on. Observed, and it is why the first charge looked
                     * right and no later one did.
                     *
                     * <p>Charge and swing are two different {@code RawAnimation}s, so switching
                     * between them restarts on its own; only the idle gap needs this.
                     */
                    private software.bernie.geckolib.animation.PlayState stop(
                            software.bernie.geckolib.animation.AnimationState<GiantJawbladeItem> state) {
                        state.getController().forceAnimationReset();
                        return software.bernie.geckolib.animation.PlayState.STOP;
                    }

                    /**
                     * Whether this exact stack is mid-swing. Matched on the held item rather than
                     * the used one, because a swing is not a use -- by the time this runs the
                     * charge has already been released.
                     */
                    private boolean isSwinging(ItemStack stack) {
                        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                        if (mc.level == null) {
                            return false;
                        }
                        for (Player player : mc.level.players()) {
                            if (player.swinging && player.getMainHandItem() == stack) {
                                return true;
                            }
                        }
                        return mc.player != null && mc.player.swinging
                                && ItemStack.isSameItemSameComponents(mc.player.getMainHandItem(), stack);
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
                            if (GiantJawbladeItem.isCharging(stack, player)) {
                                return true;
                            }
                        }
                        // The first-person hand renderer can hand out a copy rather than the held
                        // object, so fall back to the one holder that view can belong to.
                        return mc.player != null && GiantJawbladeItem.isCharging(
                                mc.player.getUseItem(), mc.player)
                                && ItemStack.isSameItemSameComponents(mc.player.getUseItem(), stack);
                    }
                }));
    }

    /**
     * GeckoLib's own equivalent of {@code IClientItemExtensions.getCustomRenderer}. Naming the
     * renderer inside an anonymous class keeps it off a dedicated server for the reason above.
     */
    @Override
    public void createGeoRenderer(
            java.util.function.Consumer<software.bernie.geckolib.animatable.client.GeoRenderProvider> consumer) {
        consumer.accept(new software.bernie.geckolib.animatable.client.GeoRenderProvider() {
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

    /**
     * No sweep, ever -- left-click included.
     *
     * <p>Vanilla decides to sweep by asking the held item whether it can {@code SWORD_SWEEP}, and
     * every {@link SwordItem} says yes, so a fully cooled strike was dealing 1.0 to every living
     * thing within a block of the target. A charged strike is contractually one target, and a full
     * charge is always fully cooled, so the sweep fired on exactly the attack that must not have
     * it. Answering "no" here is the whole fix: no flag around the attack call, no per-player
     * state, and nothing that can leak to another weapon.
     *
     * <p>It costs this weapon vanilla's left-click sweep too. That is the intended trade rather
     * than a side effect -- a two-handed bone greatsword swinging at 0.8 a second is a
     * single-target weapon in both modes.
     */
    @Override
    public boolean canPerformAction(ItemStack stack, net.neoforged.neoforge.common.ItemAbility ability) {
        return ability != net.neoforged.neoforge.common.ItemAbilities.SWORD_SWEEP
                && super.canPerformAction(stack, ability);
    }

    /**
     * The third-person charge stance: the hunter's arms raise the weapon as the charge builds.
     *
     * <p>This is what the vanilla {@link UseAnim} options could not give (see the class note above
     * on why {@code SPEAR} was rejected), and it needs no animation library: {@code ArmPose} is an
     * extensible enum, and NeoForge asks the held item which one to use. Both jawblades inherit it,
     * because how the <em>arms</em> move is independent of whether the weapon itself is drawn by
     * swapped models or by a GeckoLib clip -- keeping the comparison between those two honest.
     *
     * <p>Returning {@code null} outside a charge is what leaves an idle hunter in the ordinary item
     * pose; the custom pose is only ever active while vanilla's own use countdown is running.
     *
     * <p>The anonymous class is deliberate and load-bearing, not a style choice --
     * {@link com.carro1001.mhnw.client.MHNWArmPoses} reaches {@code HumanoidModel}, and naming it
     * in this class's own method bodies would link a client class on a dedicated server and crash
     * mod loading. See that class, and {@code docs/WEAPON_POSING.md}.
     */
    @Override
    public void initializeClient(java.util.function.Consumer<
            net.neoforged.neoforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
            @Override
            public net.minecraft.client.model.HumanoidModel.ArmPose getArmPose(
                    LivingEntity entity, net.minecraft.world.InteractionHand hand, ItemStack stack) {
                return chargeProgress(stack, entity) > 0.0F
                        ? com.carro1001.mhnw.client.MHNWArmPoses.greatswordCharge()
                        : null;
            }
        });
    }

    /**
     * An ordinary left-click resets the arc to the weakest one, so a swing nobody charged does not
     * inherit the last charged release's sweep.
     *
     * <p>This covers a left-click that <em>connects</em>. A left-click that swings at thin air after
     * a charged strike keeps the wider arc until the next attack, because vanilla tells the server
     * nothing about a missed swing -- {@code LeftClickEmpty} is a client-only event. Closing that
     * needs a packet of our own for a one-frame cosmetic difference, which is not a trade worth
     * making; see {@code docs/DEFERRED.md}.
     */
    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, net.minecraft.world.entity.Entity target) {
        stack.set(com.carro1001.mhnw.registry.ModDataComponents.SWING_TIER.get(), 0);
        return false;
    }

    /** Bone, not iron: the tier is iron only for its numbers. */
    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        return repairCandidate.is(Items.BONE);
    }

    /** See the class doc: vanilla has no two-handed wind-up pose, and this packet adds none. */
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    /**
     * The hold runs to the overcharge point, not to a tier. Vanilla counts this down and calls
     * {@link #finishUsingItem} only if the player never let go, which is exactly the overcharge
     * case; every deliberate swing arrives through {@link #releaseUsing} instead.
     */
    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return USE_DURATION_TICKS;
    }

    /**
     * Main hand only, and not while recovering. Vanilla's {@code ServerPlayerGameMode} already
     * refuses to start a use on a cooled-down item, but stating it here is what makes the recovery
     * a property of the weapon rather than of one call site, and what lets a test drive it.
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND || player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    /**
     * Charging is heavy: the crawl, and one cue on the tick a tier is reached.
     *
     * <p>The cue fires on the exact tick a threshold is crossed, which needs no "last tier" field --
     * the charge count is derived from vanilla's own countdown, so each boundary is a value it
     * passes through exactly once.
     */
    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        int charged = USE_DURATION_TICKS - remainingUseDuration;
        if (charged >= FIZZLE_TICKS) {
            if (charged == FIZZLE_TICKS && level instanceof ServerLevel serverLevel) {
                level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                        SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.8F, 1.4F);
                serverLevel.sendParticles(ParticleTypes.SMOKE,
                        entity.getX(), entity.getEyeY() - 0.2D, entity.getZ(),
                        12, 0.3D, 0.2D, 0.3D, 0.01D);
            }
            return;
        }
        entity.setDeltaMovement(entity.getDeltaMovement()
                .multiply(CHARGE_MOVEMENT_SCALE, 1.0D, CHARGE_MOVEMENT_SCALE));
        if (level.isClientSide) {
            return;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        int tier = tierFor(charged);

        // A steady trickle off the blade while it builds, thicker at each tier, so the hold reads
        // as gathering rather than as a stuck input. Max tier switches particle entirely: the point
        // where waiting longer stops paying is the one the player most needs to see.
        if (tier >= 0 && charged % 2 == 0) {
            // In first person the weapon is drawn at a fixed place on the screen rather than at a
            // point in the world, so particles emitted "at the blade" are never where the blade
            // looks like it is -- they land on the camera and are invisible. These sit out in front
            // of the holder and down to the weapon-hand side instead, which is inside the first
            // person field of view and still reads as coming off the weapon in third person.
            net.minecraft.world.phys.Vec3 look = entity.getViewVector(1.0F);
            // (-look.z, 0, look.x) already points to the holder's RIGHT, which is the weapon hand;
            // negating it put the whole effect off the left of the screen.
            net.minecraft.world.phys.Vec3 weaponSide =
                    new net.minecraft.world.phys.Vec3(-look.z, 0.0D, look.x);
            net.minecraft.world.phys.Vec3 blade = entity.getEyePosition()
                    .add(look.scale(1.0D))
                    .add(weaponSide.scale(0.55D))
                    .add(0.0D, -0.35D, 0.0D);
            boolean maxed = tier == TIER_TICKS.length - 1;
            serverLevel.sendParticles(maxed ? ParticleTypes.ENCHANTED_HIT : ParticleTypes.CRIT,
                    blade.x, blade.y, blade.z, 4 + tier * 3, 0.22D, 0.22D, 0.22D, 0.01D);
        }

        for (int reached = 0; reached < TIER_TICKS.length; reached++) {
            if (charged != TIER_TICKS[reached]) {
                continue;
            }
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                    TIER_CUES[reached], SoundSource.PLAYERS, 0.7F, 1.0F);
            if (reached == TIER_TICKS.length - 1) {
                serverLevel.sendParticles(ParticleTypes.CRIT,
                        entity.getX(), entity.getEyeY() - 0.2D, entity.getZ(),
                        16, 0.35D, 0.25D, 0.35D, 0.04D);
            }
        }
    }

    /**
     * Letting go. The tier reached is the strike; below tier one there is no strike at all, and no
     * cooldown or wear either, so an aborted charge costs nothing.
     */
    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeCharged) {
        if (!(entity instanceof Player player)) {
            return;
        }
        int charged = USE_DURATION_TICKS - timeCharged;
        int tier = tierFor(charged);

        if (tier < 0) {
            // A charge that never reached tier one still swings, at the weakest arc -- letting go
            // early should look like a wasted swing rather than like nothing happened. It still
            // costs nothing: no damage, no cooldown, no wear. A charge that already *fizzled* is
            // excluded, because it announced its own death with a cue and a dropped blade; swinging
            // again afterwards would undo that.
            if (charged < FIZZLE_TICKS) {
                beginSwing(stack, player, 0);
            }
            return;
        }

        beginSwing(stack, player, tier);
        if (level.isClientSide) {
            return;
        }
        strike(level, player, TIER_DAMAGE[tier] - TIER_DAMAGE[0]);
    }

    /**
     * Start the swing on <b>both</b> sides, and record which arc it is.
     *
     * <p>The client half is prediction, in vanilla's own style -- {@code Minecraft.startAttack}
     * swings locally rather than waiting for the server to say so -- and it is load-bearing rather
     * than an optimisation. Without it there is a gap: the charge ends the instant the button comes
     * up, but {@code swinging} only becomes true on the client when the server's animate packet
     * lands a tick or two later. For those ticks the blade is neither charging nor swinging, the
     * controller stops, and the bone snaps to the model's rest pose -- which for this weapon is
     * vertical. That read in play as the blade teleporting upright and swinging from there.
     *
     * <p><b>No amount of blending fixes that</b>, which is worth knowing before someone reaches for
     * a longer transition: a controller {@code STOP} is instant, and GeckoLib's transition only
     * lerps <em>between clips</em>. The fix is to never stop in the first place.
     *
     * <p>The tier is computed the same way on both sides from vanilla's own countdown, so the
     * client picks the right arc immediately instead of reading a component the server has not
     * synced back yet. The two-argument {@code swing} is deliberate: {@code LocalPlayer} overrides
     * only the one-argument form, and that override sends a swing packet the server has no use for
     * here -- it is already swinging this player itself.
     */
    private static void beginSwing(ItemStack stack, Player player, int tier) {
        stack.set(com.carro1001.mhnw.registry.ModDataComponents.SWING_TIER.get(), tier);
        // Both sides: the client for its own view, the server so everyone else sees it too. The
        // flag is vanilla's "tell the swinging player as well", which only the server needs to do.
        player.swing(InteractionHand.MAIN_HAND, !player.level().isClientSide);
    }

    /**
     * Unreachable in normal play, and deliberately inert.
     *
     * <p>{@link #USE_DURATION_TICKS} is an hour of holding, so vanilla never gets here; the weapon
     * swings only when the player lets go. It used to swing itself at tier one's damage after the
     * overcharge window, which is the behaviour {@link #FIZZLE_TICKS} replaced. Left overridden
     * rather than deleted so that the "never swings on its own" rule is stated where someone
     * lowering the use duration would read it.
     */
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        return stack;
    }

    /** Whether this exact stack has a live charge; fizzling ends it even while vanilla's use is held. */
    public static boolean isCharging(ItemStack stack, LivingEntity holder) {
        return holder != null && holder.isUsingItem() && holder.getUseItem() == stack
                && USE_DURATION_TICKS - holder.getUseItemRemainingTicks() < FIZZLE_TICKS;
    }

    /** How far along a live charge this stack is, rising smoothly to 1.0 at the top tier. */
    public static float chargeProgress(ItemStack stack, LivingEntity holder) {
        if (!isCharging(stack, holder)) {
            return 0.0F;
        }
        int charged = USE_DURATION_TICKS - holder.getUseItemRemainingTicks();
        int fullyWound = TIER_TICKS[TIER_TICKS.length - 1];
        return Math.min(1.0F, charged / (float) fullyWound);
    }

    /**
     * Which arc the last swing should use: the recorded tier, clamped into range.
     *
     * <p>Defaults to the weakest arc, which is also what an ordinary left-click and a released
     * half-charge get -- a swing nobody paid for should look like the cheapest one.
     */
    public static int swingTier(ItemStack stack) {
        Integer recorded = stack.get(
                com.carro1001.mhnw.registry.ModDataComponents.SWING_TIER.get());
        return recorded == null ? 0 : Math.clamp(recorded.intValue(), 0, SWINGS.length - 1);
    }

    /**
     * Which tier a given number of charged ticks has reached, or -1 for "no strike": either not yet
     * tier one, or held so long the charge has fizzled.
     */
    public static int tierFor(int chargedTicks) {
        if (chargedTicks >= FIZZLE_TICKS) {
            return -1;
        }
        int reached = -1;
        for (int tier = 0; tier < TIER_TICKS.length; tier++) {
            if (chargedTicks >= TIER_TICKS[tier]) {
                reached = tier;
            }
        }
        return reached;
    }

    /**
     * One swing: trace, at most one {@link Player#attack}, then the recovery cooldown whether or
     * not anything was there.
     *
     * <p>The tier bonus rides a transient attribute modifier rather than a damage number of ours,
     * so everything {@code Player.attack} does downstream -- enchantment scaling, the attack event,
     * durability, the hit's own sound -- sees one coherent, larger hit instead of a base hit plus a
     * correction applied afterwards. The modifier is removed in a {@code finally}: it exists for
     * one method call and cannot outlive it even if the attack throws.
     */
    private void strike(Level level, Player player, double tierBonus) {
        AttributeInstance damage = player.getAttribute(Attributes.ATTACK_DAMAGE);
        boolean boosted = damage != null && tierBonus != 0.0D;
        if (boosted) {
            damage.addTransientModifier(new AttributeModifier(
                    CHARGE_BONUS_ID, tierBonus, AttributeModifier.Operation.ADD_VALUE));
        }
        try {
            HitResult trace = ProjectileUtil.getHitResultOnViewVector(player, TARGETS, REACH);
            if (trace instanceof EntityHitResult entityHit) {
                // Player.attack plays the hit's own cue -- strong, weak, crit or no-damage -- so
                // adding one here made a landed strike sound twice.
                player.attack(entityHit.getEntity());
            } else {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.PLAYER_ATTACK_NODAMAGE, SoundSource.PLAYERS, 1.0F, 0.8F);
            }
        } finally {
            if (boosted) {
                damage.removeModifier(CHARGE_BONUS_ID);
            }
        }
        // The swing itself is started by beginSwing, on both sides and before this runs, so that
        // the client never has a tick with neither clip playing.
        player.getCooldowns().addCooldown(this, RECOVERY_TICKS);
    }
}
