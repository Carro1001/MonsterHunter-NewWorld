package com.carro1001.mhnw.item;

import com.carro1001.mhnw.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * R2 portable field cooking: hold for four seconds, get one cooked meat. Not a block, not a GUI and
 * not a timing minigame -- one fixed server-authoritative held use with a visible time cost.
 *
 * <p>The whole transaction rides on vanilla's own held-use machinery rather than a counter of ours.
 * {@code use} starts the use, vanilla counts down {@link #getUseDuration} on both sides and calls
 * {@link #finishUsingItem} exactly once, on the server, only if the player held it all the way. A
 * release, a swap, a death or a disconnect simply never reaches that call, so "cancelling consumes
 * nothing" needs no cancellation code at all.
 *
 * <p>{@code stacksTo(1)} is what keeps the result transactional without any inventory service: the
 * held stack is always exactly one spit, so returning the cooked meat from {@link #finishUsingItem}
 * replaces the input in its own slot. There is no overflow case to handle and no partial-stack
 * arithmetic to get wrong.
 *
 * <p>{@link UseAnim#BLOCK} is the pose, per the packet: the pinned API has no "hold something out
 * over a fire" animation, and BLOCK is the one built-in pose that keeps the item visible in front
 * of the player without reading as eating or drinking.
 */
public class BarbecueSpitItem extends Item {

    /** Four seconds. The single fixed cost of portable cooking. */
    public static final int COOK_TICKS = 80;

    /** Applied on completion, so a stack of spits cannot be chain-cooked instantly. */
    public static final int COOLDOWN_TICKS = 20;

    public BarbecueSpitItem(Properties properties) {
        super(properties);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BLOCK;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return COOK_TICKS;
    }

    /**
     * Starts the hold. The sound is the whole feedback that a hold has begun: with only the
     * half-second smoke puffs of {@link #onUseTick}, a tap or a short hold looked exactly like an
     * item that does nothing at all, which is how the four-second cost was first read as a bug.
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.6F, 1.6F);
        }
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    /** Presentation only, four times a second: the server's completed use is the authority. */
    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        if (level instanceof ServerLevel serverLevel && remainingUseDuration % 5 == 0) {
            serverLevel.sendParticles(ParticleTypes.SMOKE,
                    entity.getX(), entity.getEyeY() - 0.1D, entity.getZ(), 2, 0.15D, 0.05D, 0.15D, 0.005D);
            serverLevel.sendParticles(ParticleTypes.SMALL_FLAME,
                    entity.getX(), entity.getEyeY() - 0.2D, entity.getZ(), 1, 0.1D, 0.05D, 0.1D, 0.0D);
        }
    }

    /**
     * Called once, server-side, only on a completed hold. Creative keeps the spit under vanilla's
     * own infinite-materials convention ({@link ItemUtils#createFilledResult}), which is also what
     * stops a creative player running the conversion twice for one hold.
     */
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!(entity instanceof Player player) || stack.isEmpty()) {
            // The empty check is belt and braces: vanilla stops a use the moment its stack runs
            // out, so this cannot happen in play. It matters anyway, because without it a caller
            // holding an already-spent stack would be handed a second cooked meat out of nothing.
            return stack;
        }
        ItemStack cooked = new ItemStack(ModItems.COOKED_MEAT.get());
        if (!level.isClientSide) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.CAMPFIRE_CRACKLE, SoundSource.PLAYERS, 1.0F, 1.0F);
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.FLAME,
                        player.getX(), player.getEyeY() - 0.1D, player.getZ(), 8, 0.2D, 0.1D, 0.2D, 0.01D);
            }
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        return ItemUtils.createFilledResult(stack, player, cooked);
    }
}
