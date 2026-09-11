package com.carro1001.mhnw.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * A toad's whole behaviour: notice something close or being hurt, telegraph for 2 seconds, then
 * release its variant's cloud once and go quiet for 10 seconds. See {@link EndemicAreaEffectGoal}
 * for the shared trigger/telegraph/cooldown machinery this only supplies the release for.
 *
 * <h2>Provisional effects</h2>
 * None of these are real Monster Hunter ailments; each is a labelled vanilla stand-in, exactly as
 * the runtime contract asks for (section 3):
 * <ul>
 *   <li>{@code POISON} uses {@link MobEffects#POISON} directly. This one is not really provisional;
 *       vanilla poison and the intended effect are the same thing.
 *   <li>{@code PARALYSIS} uses a strong, short {@link MobEffects#MOVEMENT_SLOWDOWN}. This is the
 *       handoff's own named example of an acceptable placeholder, not a paralysis system.
 *   <li>{@code SLEEP} uses {@link MobEffects#CONFUSION} plus a milder slowdown, standing in for
 *       grogginess. It deliberately does not touch player input in any way: the handoff explicitly
 *       forbids silently disabling a player's controls, and real sleep/control-lock semantics need
 *       their own later design, not a shortcut taken here.
 *   <li>{@code BLAST} is a real, small vanilla explosion with block interaction forced to
 *       {@link Level.ExplosionInteraction#NONE}, so it can never destroy terrain (section 4.5).
 * </ul>
 */
public class ToadFuseGoal extends EndemicAreaEffectGoal {

    private static final double TRIGGER_RANGE = 3.0D;
    /** Matches the 2 s {@code fuse} clip. */
    private static final int FUSE_TICKS = 40;
    private static final double CLOUD_RADIUS = 3.0D;
    /** Bounds it to one release at a time (handoff P3 exit: "cannot trigger indefinitely"). */
    private static final int COOLDOWN_TICKS = 200;

    private final Toad toad;

    public ToadFuseGoal(Toad toad) {
        super(toad, TRIGGER_RANGE, FUSE_TICKS, COOLDOWN_TICKS);
        this.toad = toad;
    }

    @Override
    protected boolean isProvoked() {
        return this.toad.provoked;
    }

    @Override
    protected void clearProvoked() {
        this.toad.provoked = false;
    }

    @Override
    protected void setPresenting(boolean presenting) {
        this.toad.setFusing(presenting);
    }

    /** One intentional application, to whatever is in range at the moment of release, once each. */
    @Override
    protected void release() {
        if (this.toad.level().isClientSide) {
            return;
        }
        Toad.Variant variant = this.toad.getVariant();
        Level level = this.toad.level();

        if (variant == Toad.Variant.BLAST) {
            level.explode(this.toad, this.toad.getX(), this.toad.getY(), this.toad.getZ(),
                    (float) CLOUD_RADIUS * 0.6F, false, Level.ExplosionInteraction.NONE);
            return;
        }

        AABB cloud = this.toad.getBoundingBox().inflate(CLOUD_RADIUS);
        List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class, cloud,
                e -> e != this.toad && e.isAlive());
        for (LivingEntity victim : victims) {
            // A cloud must not reach through a wall any more than a melee swing may (section 4.2).
            if (!this.toad.hasLineOfSight(victim)) {
                continue;
            }
            switch (variant) {
                case POISON -> victim.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0));
                case PARALYSIS -> victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 3));
                case SLEEP -> {
                    victim.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0));
                    victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
                }
                case BLAST -> throw new IllegalStateException("handled above");
            }
        }
        if (level instanceof ServerLevelAccessor serverLevel) {
            serverLevel.getLevel().sendParticles(ParticleTypes.CLOUD,
                    this.toad.getX(), this.toad.getY() + 0.2, this.toad.getZ(), 12, 0.6, 0.2, 0.6, 0.01);
        }
    }
}
