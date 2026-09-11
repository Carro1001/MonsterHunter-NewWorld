package com.carro1001.mhnw.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * A flashbug's whole behaviour: notice something close or being hurt, flare for well under a
 * second, then blind whatever can actually see the flash, once, and go dark for a while. See
 * {@link EndemicAreaEffectGoal} for the shared trigger/telegraph/cooldown machinery.
 *
 * <p>Blinding is narrower than plain line-of-sight: players are never affected at all (a flash
 * that could blind the person playing would be a punishing surprise, not a readable hazard), and
 * a non-player victim is only blinded if it is actually looking toward the bug, not merely able to
 * see it — the flash is a startle reaction to something in view, not an omnidirectional pulse.
 */
public class FlashbugFlashGoal extends EndemicAreaEffectGoal {

    private static final double TRIGGER_RANGE = 4.0D;
    /** Matches the 0.54 s one-shot {@code flashfly.fly} clip, rounded up. */
    private static final int FUSE_TICKS = 11;
    private static final double FLASH_RADIUS = 5.0D;
    private static final int COOLDOWN_TICKS = 100;
    /** A startling burst, not a sustained ailment: brief on purpose. */
    private static final int BLINDNESS_DURATION_TICKS = 40;
    /** Dot product of a victim's look vector with the direction to the bug; ~50 degree cone. */
    private static final double FACING_DOT_THRESHOLD = 0.65D;

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
                e -> e != this.flashbug && e.isAlive() && !(e instanceof Player));
        for (LivingEntity victim : nearby) {
            if (!this.flashbug.hasLineOfSight(victim) || !isFacingFlashbug(victim)) {
                continue;
            }
            victim.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BLINDNESS_DURATION_TICKS, 0));
        }
        if (this.flashbug.level() instanceof ServerLevelAccessor serverLevel) {
            serverLevel.getLevel().sendParticles(ParticleTypes.FLASH,
                    this.flashbug.getX(), this.flashbug.getY(), this.flashbug.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    /**
     * Whether the victim is facing roughly toward the bug, not merely able to see it. Horizontal
     * (yaw) only, deliberately ignoring pitch: the bug hovers low and most victims' eye height sits
     * well above it, so a full 3D look-angle comparison would fail this for anything standing right
     * next to a low-hovering bug and looking straight at it, purely from the vertical offset between
     * eye height and the bug's low altitude — not the "is it facing this way" question this asks.
     */
    private boolean isFacingFlashbug(LivingEntity victim) {
        Vec3 toBug = new Vec3(
                this.flashbug.getX() - victim.getX(), 0.0D, this.flashbug.getZ() - victim.getZ());
        if (toBug.lengthSqr() < 1.0E-6) {
            return true;
        }
        Vec3 look = victim.getLookAngle();
        Vec3 lookFlat = new Vec3(look.x, 0.0D, look.z);
        if (lookFlat.lengthSqr() < 1.0E-6) {
            return false;
        }
        return lookFlat.normalize().dot(toBug.normalize()) > FACING_DOT_THRESHOLD;
    }
}
