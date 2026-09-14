package com.carro1001.mhnw.entity;

import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

/** Vanilla melee with Rathalos's deliberate peaceful-survival contract. */
public class RathalosCombatGoal extends MeleeAttackGoal {
    private final Rathalos monster;

    public RathalosCombatGoal(Rathalos monster) {
        super(monster, 1.0D, true);
        this.monster = monster;
    }

    @Override
    public boolean canUse() {
        return canFight(this.monster, this.monster.getTarget(), this.monster.level().getDifficulty())
                && super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return canFight(this.monster, this.monster.getTarget(), this.monster.level().getDifficulty())
                && super.canContinueToUse();
    }

    /** Testable precondition because changing the shared GameTest world's difficulty is unsafe. */
    public static boolean canFight(Rathalos monster, LivingEntity target, Difficulty difficulty) {
        return difficulty != Difficulty.PEACEFUL
                && monster.isAlive() && target != null && target.isAlive();
    }
}
