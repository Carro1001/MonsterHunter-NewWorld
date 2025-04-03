package com.carro1001.mhnw.entities.ai;

import com.carro1001.mhnw.entities.NewWorldMonsterEntity;
import com.carro1001.mhnw.entities.helpers.MonsterBreakablePartEntity;
import com.carro1001.mhnw.registration.ModDamageTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Path;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class HitboxMeeleeAttackGoal  extends Goal {
    private NewWorldMonsterEntity monster;
    private int ticksUntilNextAnimation;
    private int animInterval = 66;
    private boolean animatingAttack = false;
    int tickForAttackChecksToBegin = 25;
    int tickForAttackChecksToEnd = 25;

    private HashMap<UUID,Integer> hitIDToEnemyUUID;

    protected int maxHitCount = 4;
    protected int ticksBetweenAttacksInAnimation = 5;
    private int ticksUntilNextAttack = 25;
    private int ticksCounterForAttacksEnding = 25;

    // Inflating part Hitbox and entity hitbox found by this much to check if we hit
    float hitTriggerAABBInflation = 1.5f;
    //Inflating part Hitbox AABB by this much to check living entities in this area
    float hitCheckAABBInflation = 3;
    MonsterBreakablePartEntity<NewWorldMonsterEntity> partHitbox;
    String animation ="attack_scratch";
    String controller = "main_controller";

    private final double speedModifier;
    private final boolean followingTargetEvenIfNotSeen;
    private double pathedTargetX;
    private double pathedTargetY;
    private double pathedTargetZ;
    private int ticksUntilNextPathRecalculation;
    private Path path;
    private long lastCanUseCheck;
    private long cooldownBetweenCanUseChecks;
    private int failedPathFindingPenalty = 0;
    private boolean canPenalize = false;

    private boolean hasLooped = false;

    public HitboxMeeleeAttackGoal(NewWorldMonsterEntity pMob, double pSpeedModifier, MonsterBreakablePartEntity<NewWorldMonsterEntity> Hitbox,
                                  String animController, String animTrigger, float hitCheckAABBInflation, float hitTriggerAABBInflation,  int maxHits,
                                  int ticksBetweenAttacksInAnimation, int tickForAttackChecksToBegin,int tickForAttackChecksToEnd, int animationDuration, boolean pFollowingTargetEvenIfNotSeen) {
        this.monster = pMob;
        this.partHitbox = Hitbox;
        animation = animTrigger;
        controller = animController;
        this.maxHitCount = maxHits;
        this.ticksBetweenAttacksInAnimation = ticksBetweenAttacksInAnimation;
        this.tickForAttackChecksToBegin = tickForAttackChecksToBegin;
        this.tickForAttackChecksToEnd = tickForAttackChecksToEnd;
        this.hitTriggerAABBInflation = hitTriggerAABBInflation;
        this.hitCheckAABBInflation = hitCheckAABBInflation;
        animInterval = animationDuration;
        cooldownBetweenCanUseChecks = animInterval * 2L;
        this.speedModifier = pSpeedModifier;
        this.followingTargetEvenIfNotSeen = pFollowingTargetEvenIfNotSeen;
        hitIDToEnemyUUID = new HashMap<>();
    }



    @Override
    public boolean canUse() {
        long i = this.monster.level().getGameTime();
        if (i - this.lastCanUseCheck < cooldownBetweenCanUseChecks || monster.isAttacking()) {
            return false;
        }
        this.lastCanUseCheck = i;
        LivingEntity livingentity = this.monster.getTarget();
        if (livingentity == null || monster.isLimpining()) {
            return false;
        } else if (!livingentity.isAlive()) {
            return false;
        } else {
            if (canPenalize) {
                if (--this.ticksUntilNextPathRecalculation <= 0) {
                    this.path = this.monster.getNavigation().createPath(livingentity, 0);
                    this.ticksUntilNextPathRecalculation = 4 + this.monster.getRandom().nextInt(7);
                    return this.path != null;
                } else {
                    return true;
                }
            }
            this.path = this.monster.getNavigation().createPath(livingentity, 0);
            if (this.path != null) {
                return true;
            } else {
                return this.getAttackReachSqr(livingentity) >= this.monster.distanceToSqr(livingentity.getX(), livingentity.getY(), livingentity.getZ());
            }
        }
    }

    @Override
    public boolean canContinueToUse() {
        if(hasLooped){
            return false;
        }
        if (animatingAttack) {
            return true;
        }
        LivingEntity livingentity = this.monster.getTarget();
        if (livingentity == null) {
            return false;
        } else if (!this.followingTargetEvenIfNotSeen) {
            return !this.monster.getNavigation().isDone();
        } else if (!this.monster.isWithinRestriction(livingentity.blockPosition())) {
            return false;
        } else {
            return !(livingentity instanceof Player) || !livingentity.isSpectator() && !((Player)livingentity).isCreative();
        }
    }

    public void start() {
        this.monster.getNavigation().moveTo(this.path, this.speedModifier);
        this.monster.setAggressive(true);
        this.monster.setAttacking(true);
        this.ticksUntilNextPathRecalculation = 0;
        this.ticksUntilNextAttack = 0;
        this.ticksUntilNextAnimation = 0;
        animatingAttack = false;
        hitIDToEnemyUUID.clear();
    }

    public void stop() {
        LivingEntity livingentity = this.monster.getTarget();
        if (!EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(livingentity)) {
            this.monster.setTarget(null);
        }

        this.monster.setAggressive(false);
        this.monster.setAttacking(false);
        this.monster.getNavigation().stop();
        this.lastCanUseCheck = this.monster.level().getGameTime();
        hasLooped = false;
    }

    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        super.tick();

        LivingEntity livingentity = this.monster.getTarget();
        if (!monster.level().isClientSide && livingentity != null) {
            if(!animatingAttack){
                this.monster.getLookControl().setLookAt(livingentity, 10.0F, 10.0F);
            }
            double d0 = this.monster.getPerceivedTargetDistanceSquareForMeleeAttack(livingentity);

            if ((this.followingTargetEvenIfNotSeen || this.monster.getSensing().hasLineOfSight(livingentity)) && this.ticksUntilNextPathRecalculation <= 0 && (this.pathedTargetX == 0.0D && this.pathedTargetY == 0.0D && this.pathedTargetZ == 0.0D || livingentity.distanceToSqr(this.pathedTargetX, this.pathedTargetY, this.pathedTargetZ) >= 1.0D || this.monster.getRandom().nextFloat() < 0.05F)) {
                this.pathedTargetX = livingentity.getX();
                this.pathedTargetY = livingentity.getY();
                this.pathedTargetZ = livingentity.getZ();
                this.ticksUntilNextPathRecalculation = 4 + this.monster.getRandom().nextInt(7);
                if (this.canPenalize) {
                    this.ticksUntilNextPathRecalculation += failedPathFindingPenalty;
                    if (this.monster.getNavigation().getPath() != null) {
                        net.minecraft.world.level.pathfinder.Node finalPathPoint = this.monster.getNavigation().getPath().getEndNode();
                        if (finalPathPoint != null && livingentity.distanceToSqr(finalPathPoint.x, finalPathPoint.y, finalPathPoint.z) < getAttackReachSqr(livingentity))
                            failedPathFindingPenalty = 0;
                        else
                            failedPathFindingPenalty += 10;
                    } else {
                        failedPathFindingPenalty += 10;
                    }
                }
                if (d0 > 1024.0D) {
                    this.ticksUntilNextPathRecalculation += 10;
                } else if (d0 > 256.0D) {
                    this.ticksUntilNextPathRecalculation += 5;
                }

                if (!this.monster.getNavigation().moveTo(livingentity, this.speedModifier)) {
                    this.ticksUntilNextPathRecalculation += 30;
                }

                this.ticksUntilNextPathRecalculation = this.adjustedTickDelay(this.ticksUntilNextPathRecalculation);
            }
            this.ticksUntilNextAnimation = Math.max(this.ticksUntilNextAnimation - 1, 0);
            this.ticksUntilNextAttack = Math.max(this.ticksUntilNextAttack - 1, 0);

            if(animatingAttack){
                if(ticksUntilNextAttack <= 0 && ticksUntilNextAnimation > ticksCounterForAttacksEnding){
                    List<LivingEntity> entities = monster.level().getEntitiesOfClass(LivingEntity.class, partHitbox.getBoundingBox().inflate(hitCheckAABBInflation));
                    for(LivingEntity entity: entities){
                        if(!(entity instanceof NewWorldMonsterEntity) && entity.getBoundingBox().inflate(hitTriggerAABBInflation).intersects(partHitbox.getBoundingBox().inflate(hitTriggerAABBInflation))){
                            if(hitIDToEnemyUUID.getOrDefault(entity.getUUID(), 0) >= maxHitCount){
                                continue;
                            }

                            float f = (float)monster.getAttributeValue(Attributes.ATTACK_DAMAGE);
                            boolean flag = entity.hurt(new DamageSource(monster.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(ModDamageTypes.RAW)), f);
                            if (flag) {
                                hitIDToEnemyUUID.replace(entity.getUUID(),hitIDToEnemyUUID.getOrDefault(entity.getUUID(), 0) + 1);
                                float f1 = (float)monster.getAttributeValue(Attributes.ATTACK_KNOCKBACK);
                                if (f1 > 0.0F && entity instanceof LivingEntity) {
                                    entity.knockback(f1 * 0.7F, Mth.sin(monster.getYRot() * ((float)Math.PI / 180F)), -Mth.cos(monster.getYRot() * ((float)Math.PI / 180F)));
                                }
                                monster.setLastHurtMob(entity);
                            }
                        }
                    }
                    this.ticksUntilNextAttack = this.adjustedTickDelay(ticksBetweenAttacksInAnimation);

                }
                if(this.ticksUntilNextAnimation <= 0){
                    animatingAttack = false;
                    hasLooped = true;
                    //wait a bit until you try and attack again if the goal stays active
                    this.ticksUntilNextAnimation = this.adjustedTickDelay(animInterval/3);
                }
            }
            else if(!animatingAttack && this.ticksUntilNextAnimation <= 0){
                this.checkAndPerformAnimation();
            }
        }
    }

    protected void checkAndPerformAnimation() {
        if(!animatingAttack && this.ticksUntilNextAnimation <= 0){
            //wait a until anim is over until you try and attack again, in monster hunter fashion im trying to limit hits per animation
            monster.triggerAnim(controller,animation);
            this.ticksUntilNextAnimation = this.adjustedTickDelay(animInterval);
            this.ticksUntilNextAttack = this.adjustedTickDelay(tickForAttackChecksToBegin);
            ticksCounterForAttacksEnding = ticksUntilNextAnimation - tickForAttackChecksToEnd;
            animatingAttack = true;
        }
    }
    protected double getAttackReachSqr(LivingEntity pAttackTarget) {
        return this.partHitbox.getBbWidth() * 2.0F * this.partHitbox.getBbWidth() * 2.0F + pAttackTarget.getBbWidth();
    }
}
