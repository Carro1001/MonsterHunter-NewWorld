package com.carro1001.mhnw.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * A flashbug's whole behaviour: notice something close or being hurt, flare for well under a
 * second, then blind whatever can actually see the flash, once, and go dark for a while. See
 * {@link EndemicAreaEffectGoal} for the shared trigger/telegraph/cooldown machinery.
 *
 * <p>A flash is a strictly line-of-sight effect by its own nature, not merely by borrowed
 * convention: something that cannot see the bug cannot be blinded by it, so the same
 * {@code hasLineOfSight} check the base class already uses for the trigger is reused for who gets
 * hit, and there is no separate "must not reach through a wall" concern to bolt on here the way
 * there was for the toad's gas cloud.
 */
public class FlashbugFlashGoal extends EndemicAreaEffectGoal {

    private static final double TRIGGER_RANGE = 4.0D;
    /** Matches the 0.54 s one-shot {@code flashfly.fly} clip, rounded up. */
    private static final int FUSE_TICKS = 11;
    private static final double FLASH_RADIUS = 5.0D;
    private static final int COOLDOWN_TICKS = 100;
    /** A startling burst, not a sustained ailment: brief on purpose. */
    private static final int BLINDNESS_DURATION_TICKS = 40;

    private final Flashbug flashbug;

    public FlashbugFlashGoal(Flashbug flashbug) {
        super(flashbug, TRIGGER_RANGE, FUSE_TICKS, COOLDOWN_TICKS);
        this.flashbug = flashbug;
    }

    @Override
    protected boolean isProvoked() {
        return this.flashbug.provoked;
    }

    @Override
    protected void clearProvoked() {
        this.flashbug.provoked = false;
    }

    @Override
    protected void setPresenting(boolean presenting) {
        this.flashbug.setFlashing(presenting);
    }

    @Override
    protected void release() {
        if (this.flashbug.level().isClientSide) {
            return;
        }
        AABB range = this.flashbug.getBoundingBox().inflate(FLASH_RADIUS);
        List<LivingEntity> nearby = this.flashbug.level().getEntitiesOfClass(LivingEntity.class, range,
                e -> e != this.flashbug && e.isAlive());
        for (LivingEntity victim : nearby) {
            if (!this.flashbug.hasLineOfSight(victim)) {
                continue;
            }
            victim.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BLINDNESS_DURATION_TICKS, 0));
        }
        if (this.flashbug.level() instanceof ServerLevelAccessor serverLevel) {
            serverLevel.getLevel().sendParticles(ParticleTypes.FLASH,
                    this.flashbug.getX(), this.flashbug.getY(), this.flashbug.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
        }
    }
}
