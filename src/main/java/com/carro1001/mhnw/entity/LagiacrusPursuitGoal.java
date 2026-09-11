package com.carro1001.mhnw.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Path;

import java.util.EnumSet;

/**
 * P5a has no outgoing attack. This goal owns acquisition, movement, look and abandonment together;
 * no independent target goal can defeat the retry cooldown. Explicitly assigned living targets
 * are supported; otherwise only visible, non-creative/non-spectator players are acquired.
 */
public class LagiacrusPursuitGoal extends Goal {
    public static final int MAX_PURSUIT_TICKS = 200;
    public static final int REPATH_INTERVAL_TICKS = 20;
    public static final int MAX_FAILED_PATHS = 3;
    public static final int RETRY_COOLDOWN_TICKS = 100;
    public static final double CLOSE_RANGE = 2.5D;

    private final Lagiacrus monster;
    private LivingEntity pursuedTarget;
    private long pursuitEndsAt;
    private long nextPathAt;
    private long retryAfter;
    private int failedPaths;

    public LagiacrusPursuitGoal(Lagiacrus monster) {
        this.monster = monster;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private boolean validTarget(LivingEntity target) {
        return target != null && target != this.monster && target.isAlive()
                && target.level() == this.monster.level()
                && !(target instanceof Player player && (player.isCreative() || player.isSpectator()))
                && this.monster.distanceToSqr(target) <= Lagiacrus.FOLLOW_RANGE * Lagiacrus.FOLLOW_RANGE;
    }

    @Override
    public boolean canUse() {
        if (this.monster.level().isClientSide || !this.monster.isAlive()) {
            return false;
        }
        if (this.monster.level().getGameTime() < this.retryAfter) {
            this.monster.setTarget(null);
            return false;
        }
        if (!validTarget(this.monster.getTarget())) {
            this.monster.setTarget(this.monster.level().getNearestPlayer(
                    this.monster.getX(), this.monster.getY(), this.monster.getZ(), Lagiacrus.FOLLOW_RANGE,
                    candidate -> candidate instanceof Player player && validTarget(player)
                            && this.monster.hasLineOfSight(player)));
        }
        return validTarget(this.monster.getTarget());
    }

    @Override
    public void start() {
        this.pursuedTarget = this.monster.getTarget();
        this.pursuitEndsAt = this.monster.level().getGameTime() + MAX_PURSUIT_TICKS;
        this.nextPathAt = 0;
        this.failedPaths = 0;
        this.monster.setAggressive(true);
    }

    @Override
    public boolean canContinueToUse() {
        return this.monster.isAlive() && validTarget(this.pursuedTarget)
                && this.monster.getTarget() == this.pursuedTarget
                && this.monster.level().getGameTime() < this.pursuitEndsAt;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (!canContinueToUse()) {
            stop();
            return;
        }
        // Swim pitch steers vertical movement toward path nodes, not the target's eyes.
        this.monster.getLookControl().setLookAt(this.pursuedTarget, 30.0F,
                this.monster.isInWater() ? 0.0F : 30.0F);
        if (this.monster.distanceToSqr(this.pursuedTarget) <= CLOSE_RANGE * CLOSE_RANGE
                && this.monster.hasLineOfSight(this.pursuedTarget)) {
            this.monster.getNavigation().stop();
            return;
        }
        long now = this.monster.level().getGameTime();
        if (now < this.nextPathAt) {
            return;
        }
        this.nextPathAt = now + REPATH_INTERVAL_TICKS;
        Path path = this.monster.getNavigation().createPath(this.pursuedTarget, 0);
        // A partial path can return true from moveTo but still end at a sealed wall. Count that
        // as a failed attempt, and bound even apparently reachable/stalled paths by the deadline.
        if (path == null || !path.canReach() || !this.monster.getNavigation().moveTo(path, 1.0D)) {
            this.monster.getNavigation().stop();
            if (++this.failedPaths >= MAX_FAILED_PATHS) {
                stop();
            }
        } else {
            this.failedPaths = 0;
        }
    }

    /** Goal stop, target loss, death, removal and NBT load all discard transient pursuit. */
    @Override
    public void stop() {
        this.pursuedTarget = null;
        this.pursuitEndsAt = 0;
        this.nextPathAt = 0;
        this.failedPaths = 0;
        this.retryAfter = this.monster.level().getGameTime() + RETRY_COOLDOWN_TICKS;
        this.monster.setTarget(null);
        this.monster.setAggressive(false);
        this.monster.getNavigation().stop();
        this.monster.getMoveControl().setWantedPosition(
                this.monster.getX(), this.monster.getY(), this.monster.getZ(), 0.0D);
        this.monster.setSpeed(0.0F);
        this.monster.setXxa(0.0F);
        this.monster.setYya(0.0F);
        this.monster.setZza(0.0F);
        this.monster.setJumping(false);
    }
}
