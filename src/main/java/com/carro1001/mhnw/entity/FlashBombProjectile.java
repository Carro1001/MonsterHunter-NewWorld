package com.carro1001.mhnw.entity;

import com.carro1001.mhnw.registry.ModEntities;
import com.carro1001.mhnw.registry.ModItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The thrown half of the R2 flash bomb: a snowball-shaped projectile that carries no damage at all.
 *
 * <p>It deliberately does not override {@code onHitEntity}. {@link ThrowableItemProjectile}'s own
 * impact path deals nothing -- vanilla's {@code Snowball} adds its damage itself -- so leaving that
 * method alone is what makes "deals no direct or terrain damage" true, rather than a subtraction
 * that could be removed by accident.
 *
 * <p>{@link #onHit} runs once and then discards, so the flash can never be released twice and the
 * bomb can never be recovered. The eligibility rules live in {@link FlashEffect}, shared verbatim
 * with the wild {@link Flashbug}.
 */
public class FlashBombProjectile extends ThrowableItemProjectile {

    /** Nudges a block impact off the face it hit, so the block cannot occlude its own flash. */
    private static final double FACE_CLEARANCE = 0.25D;

    public FlashBombProjectile(EntityType<? extends FlashBombProjectile> type, Level level) {
        super(type, level);
    }

    public FlashBombProjectile(Level level, LivingEntity shooter) {
        super(ModEntities.FLASH_BOMB.get(), shooter, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.FLASH_BOMB.get();
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level().isClientSide) {
            return;
        }
        FlashEffect.flash(level(), this, flashOrigin(result));
        discard();
    }

    private Vec3 flashOrigin(HitResult result) {
        if (result instanceof BlockHitResult blockHit) {
            Vec3 outward = Vec3.atLowerCornerOf(blockHit.getDirection().getNormal());
            return result.getLocation().add(outward.scale(FACE_CLEARANCE));
        }
        return result.getLocation();
    }
}
