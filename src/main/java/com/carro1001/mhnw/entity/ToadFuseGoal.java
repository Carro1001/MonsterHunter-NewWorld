package com.carro1001.mhnw.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
import java.util.List;

/**
 * The whole of a toad's behaviour: notice something close or being hurt, telegraph, then release a
 * small bounded cloud once and go on cooldown.
 *
 * <p>Kept as one small concrete goal rather than a generalised species-effect framework, because
 * one caller does not justify one (handoff section 3's simplicity pass: "which behaviour fails
 * without it?"). If a second endemic species needs the same shape of trigger-telegraph-release
 * later, that is the point to extract a shared base, not before.
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
public class ToadFuseGoal extends Goal {

    /** How close something must get to count as proximity, in blocks. */
    private static final double TRIGGER_RANGE = 3.0D;
    /** Ticks of telegraph before the cloud releases; matches the 2 s {@code fuse} clip. */
    private static final int FUSE_TICKS = 40;
    /** Radius of the released effect, in blocks. */
    private static final double CLOUD_RADIUS = 3.0D;
    /** Ticks after a release before the toad can trigger again. Bounds it to one release at a time. */
    private static final int COOLDOWN_TICKS = 200;

    private final Toad toad;
    private int fuseTicksLeft = -1;
    private int cooldown;

    public ToadFuseGoal(Toad toad) {
        this.toad = toad;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.cooldown > 0) {
            return false;
        }
        return this.toad.provoked || nearestTrigger() != null;
    }

    @Override
    public boolean canContinueToUse() {
        // Once lit, the fuse burns down regardless of whether whatever set it off is still
        // around: the cloud is an area release evaluated at the end, not a tracked strike on one
        // victim, so there is no "target" to lose (compare rule 6, which is about a committed
        // strike, not applicable to an AoE that has not gone off yet).
        return this.fuseTicksLeft > 0;
    }

    @Override
    public boolean isInterruptable() {
        return this.fuseTicksLeft <= 0;
    }

    /**
     * Without this, vanilla only calls a non-running-tick-required goal's {@code tick()} on
     * roughly half of all server ticks (see {@code Mob.serverAiStep}, which alternates between a
     * full selection pass and a cheaper {@code tickRunningGoals(false)} pass by tick-and-entity-id
     * parity). Left unset, this countdown would run at an unpredictable, roughly-halved rate
     * rather than the 2-second telegraph the {@code fuse} clip was authored for. Found by a
     * GameTest whose timing assumptions only made sense once this was fixed, not by inspection.
     */
    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    private LivingEntity nearestTrigger() {
        AABB range = this.toad.getBoundingBox().inflate(TRIGGER_RANGE);
        List<LivingEntity> nearby = this.toad.level().getEntitiesOfClass(LivingEntity.class, range,
                e -> e != this.toad && e.isAlive());
        for (LivingEntity candidate : nearby) {
            if (this.toad.hasLineOfSight(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    @Override
    public void start() {
        this.fuseTicksLeft = FUSE_TICKS;
        this.toad.provoked = false;
        this.toad.setFusing(true);
        this.toad.getNavigation().stop();
    }

    @Override
    public void stop() {
        this.toad.setFusing(false);
        this.fuseTicksLeft = -1;
        this.cooldown = COOLDOWN_TICKS;
    }

    @Override
    public void tick() {
        if (this.cooldown > 0) {
            this.cooldown--;
        }
        if (this.fuseTicksLeft <= 0) {
            return;
        }
        this.fuseTicksLeft--;
        if (this.fuseTicksLeft == 0) {
            release();
        }
    }

    /** One intentional application, to whatever is in range at the moment of release, once each. */
    private void release() {
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
