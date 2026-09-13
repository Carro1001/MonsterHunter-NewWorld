package com.carro1001.mhnw.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
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
 * R3's one weapon: an ordinary iron-tier sword that is slower and harder than vanilla's, plus one
 * committed longer-reach strike.
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
 * <h2>The charged strike</h2>
 * Hold main-hand use for {@link #CHARGE_TICKS} real ticks; on completion the server traces the
 * player's own view vector out to {@link #REACH} blocks and, if the first thing on it is an entity,
 * calls {@link Player#attack} once. Hit or miss, the item goes on a {@link #RECOVERY_TICKS}
 * cooldown. There is no damage multiplier, no cone and no sweep: what it buys is reach after a
 * visible commitment, and what it costs is the recovery whether or not it connected.
 *
 * <p>Three things this deliberately does not have. There is <em>no state of ours</em>: vanilla's
 * active-use state is the charge, so release, swap, death and disconnect already cancel it and
 * nothing is saved, synched or attached. There is <em>no damage pipeline of ours</em>:
 * {@code Player.attack} keeps attack events, enchantments, knockback, durability, sounds, stats,
 * the player-caused damage source and therefore {@code CarveState} attribution on their ordinary
 * path -- including through a {@code MonsterPart}, which NeoForge's own patch to that method
 * resolves to its parent for durability. And there is <em>no target search of ours</em>:
 * {@link ProjectileUtil#getHitResultOnViewVector} is the same block-clipped trace vanilla's own
 * projectiles use, so a wall stops the strike because the trace stops, not because of a check.
 *
 * <p>{@code Level.getEntities} includes NeoForge {@code PartEntity} instances, so a big monster's
 * hurtbox is selectable here with no multipart-specific code at all.
 */
public class GiantJawbladeItem extends SwordItem {

    /** The hold, in real ticks. Longer than this weapon's own 25-tick full-strength delay (20/0.8),
     * so a completed charge always lands at full vanilla attack strength without touching the
     * private attack ticker. */
    public static final int CHARGE_TICKS = 30;

    /** Recovery after a completed charge, hit or miss. The commitment. */
    public static final int RECOVERY_TICKS = 30;

    /** Maximum reach of the charged strike, in blocks. */
    public static final double REACH = 4.5D;

    /** Added to the tier's own +2.0, on top of the player's 1.0 base, for a total of 9.0. */
    public static final float DAMAGE_MODIFIER = 6.0F;

    /** Added to the player's 4.0 base, for 0.8 attacks per second. */
    public static final float SPEED_MODIFIER = -3.2F;

    public static final Tier TIER = Tiers.IRON;

    /** Vanilla's own picking rule: anything a projectile could hit, minus spectators. */
    private static final Predicate<Entity> TARGETS = EntitySelector.NO_SPECTATORS.and(Entity::isPickable);

    public GiantJawbladeItem(Properties properties) {
        super(TIER, properties.attributes(createAttributes(TIER, DAMAGE_MODIFIER, SPEED_MODIFIER)));
    }

    /** Bone, not iron: the tier is iron only for its numbers. */
    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        return repairCandidate.is(Items.BONE);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.SPEAR;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return CHARGE_TICKS;
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
     * The completed charge. Called once, by vanilla, only after the full hold -- so there is no
     * cancellation path to write: an early release, a swap or a death never reaches this method.
     *
     * <p>Server only. The client may already be showing the pose; it may not decide the hit.
     */
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!(entity instanceof Player player) || level.isClientSide) {
            return stack;
        }
        HitResult trace = ProjectileUtil.getHitResultOnViewVector(player, TARGETS, REACH);
        if (trace instanceof EntityHitResult entityHit) {
            player.attack(entityHit.getEntity());
        }
        player.swing(InteractionHand.MAIN_HAND, true);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0F, 0.8F);
        player.getCooldowns().addCooldown(this, RECOVERY_TICKS);
        return stack;
    }
}
