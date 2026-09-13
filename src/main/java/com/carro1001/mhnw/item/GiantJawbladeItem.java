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
 * cooldown, no wear. Hold past {@link #OVERCHARGE_TICKS} and the weapon swings by itself, back down
 * at tier one's damage: the charge is wasted, not banked.
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
public class GiantJawbladeItem extends SwordItem {

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
    public static final int[] TIER_TICKS = {25, 45, 75};

    /** Total attack damage each tier lands, in the same units the tooltip shows. Tier one is simply
     * the weapon's own 9.0, so a short charge buys reach and a long one buys damage as well. */
    public static final float[] TIER_DAMAGE = {9.0F, 12.5F, 16.0F};

    /** Hold this long without releasing and the weapon swings itself, back at tier one's damage. */
    public static final int OVERCHARGE_TICKS = 100;

    /**
     * How many discrete lean poses the charge ramps through, matching the generated
     * {@code giant_jawblade_charge_N} models.
     *
     * <p>48 steps across a 75-tick wind-up is a pose change roughly every one and a half ticks,
     * which reads as a raise rather than as three lurches; 16 steps (every ~4.7 ticks) visibly
     * stepped. Raise this further if it still catches the eye -- the models are generated, so the
     * only cost is a few more small files.
     *
     * <p>The ramp is stepped rather than truly continuous because the only render hook that knows
     * <em>which entity</em> is holding the weapon is an item property function; a custom renderer
     * and a baked-model wrapper both get the stack without its holder, and would pose every
     * player's weapon from the local player's charge. Property functions select whole models, so a
     * smooth lean is spelled as many small steps -- the same way vanilla spells a drawing bow, just
     * finer. Regenerate the models with {@code node tools/gen_jawblade_charge_models.js} if this
     * changes; a GameTest fails if the two disagree.
     */
    public static final int CHARGE_POSE_STEPS = 48;

    /** Recovery after any completed swing, hit or miss. The commitment. */
    public static final int RECOVERY_TICKS = 30;

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
        return OVERCHARGE_TICKS;
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
        entity.setDeltaMovement(entity.getDeltaMovement()
                .multiply(CHARGE_MOVEMENT_SCALE, 1.0D, CHARGE_MOVEMENT_SCALE));
        if (level.isClientSide) {
            return;
        }
        int charged = OVERCHARGE_TICKS - remainingUseDuration;
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
        if (!(entity instanceof Player player) || level.isClientSide) {
            return;
        }
        int tier = tierFor(OVERCHARGE_TICKS - timeCharged);
        if (tier < 0) {
            return;
        }
        strike(level, player, TIER_DAMAGE[tier] - TIER_DAMAGE[0]);
    }

    /**
     * Held past the overcharge point. Vanilla calls this exactly once, on the server, only on a
     * hold that ran the whole way -- so this is the "you waited too long" swing, and it lands at
     * tier one's damage rather than tier three's.
     */
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof Player player && !level.isClientSide) {
            strike(level, player, 0.0D);
        }
        return stack;
    }

    /**
     * How far along a charge this stack is for whoever is holding it: 0.0 when it is not being
     * charged, rising smoothly to 1.0 at the top tier and staying there while overcharging.
     *
     * <p>Deliberately continuous, unlike the damage, which steps at {@link #TIER_TICKS}. The lean
     * should look like winding up; the cues and the damage are what mark the tiers.
     *
     * <p>This is what the client's model predicate reads to lean the weapon back as the charge
     * builds, and it is deliberately derived from vanilla's own use countdown here in common code
     * rather than tracked separately on the client -- the pose and the damage cannot disagree
     * because they are the same number.
     */
    public static float chargeProgress(ItemStack stack, LivingEntity holder) {
        if (holder == null || !holder.isUsingItem() || holder.getUseItem() != stack) {
            return 0.0F;
        }
        int charged = OVERCHARGE_TICKS - holder.getUseItemRemainingTicks();
        int fullyWound = TIER_TICKS[TIER_TICKS.length - 1];
        return Math.min(1.0F, charged / (float) fullyWound);
    }

    /** Which tier a given number of charged ticks has reached, or -1 for "not even tier one". */
    public static int tierFor(int chargedTicks) {
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
        player.swing(InteractionHand.MAIN_HAND, true);
        player.getCooldowns().addCooldown(this, RECOVERY_TICKS);
    }
}
