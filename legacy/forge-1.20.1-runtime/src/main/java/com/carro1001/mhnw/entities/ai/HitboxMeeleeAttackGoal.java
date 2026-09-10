package com.carro1001.mhnw.entities.ai;

import com.carro1001.mhnw.entities.NewWorldMonsterEntity;
import com.carro1001.mhnw.entities.helpers.MonsterBreakablePartEntity;
import com.carro1001.mhnw.registration.ModDamageTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.level.pathfinder.Path;

import java.util.List;

public class HitboxMeeleeAttackGoal  extends MeleeAttackGoal {
    private NewWorldMonsterEntity monster;
    private int ticksUntilNextAnimation;
    private int animInterval = 66;
    private boolean animatingAttack = false;
    int tickForAttackChecksToBegin = 25;

    boolean hitConnected = false;
    protected int maxHitCount = 4;
    protected int currentHitsConnected = 0;
    protected int ticksBetweenAttacksInAnimation = 5;
    private int ticksUntilNextAttack = 25;

    private Path path;
    // Inflating part Hitbox and entity hitbox found by this much to check if we hit
    float hitTriggerAABBInflation = 1.5f;
    //Inflating part Hitbox AABB by this much to check living entities in this area
    float hitCheckAABBInflation = 3;
    MonsterBreakablePartEntity<NewWorldMonsterEntity> partHitbox;
    String animation ="attack_scratch";
    String controller = "main_controller";

    public HitboxMeeleeAttackGoal(NewWorldMonsterEntity pMob, double pSpeedModifier, MonsterBreakablePartEntity<NewWorldMonsterEntity> Hitbox,
                                  String animController, String animTrigger, float hitCheckAABBInflation, float hitTriggerAABBInflation,  int maxHits,
                                  int ticksBetweenAttacksInAnimation, int tickForAttackChecksToBegin, int animationDuration) {
        super(pMob, pSpeedModifier, true);
        this.monster = pMob;
        this.partHitbox = Hitbox;
        animation = animTrigger;
        controller = animController;
        this.maxHitCount = maxHits;
        this.ticksBetweenAttacksInAnimation = ticksBetweenAttacksInAnimation;
        this.hitTriggerAABBInflation = hitTriggerAABBInflation;
        this.hitCheckAABBInflation = hitCheckAABBInflation;
        this.tickForAttackChecksToBegin = tickForAttackChecksToBegin;
        animInterval = animationDuration;
    }



    @Override
    public boolean canUse() {
        LivingEntity livingentity = this.mob.getTarget();
        if (livingentity == null || monster.isLimpining()) {
            return false;
        } else if (!livingentity.isAlive()) {
            return false;
        } else {
            this.path = this.mob.getNavigation().createPath(livingentity, 0);
            if (this.path != null) {
                return true;
            } else {
                return this.getAttackReachSqr(livingentity) >= this.mob.distanceToSqr(livingentity.getX(), livingentity.getY(), livingentity.getZ());
            }
        }
    }

    @Override
    public boolean canContinueToUse() {
        return super.canContinueToUse() && animatingAttack && !hitConnected;
    }

    @Override
    public void tick() {
        super.tick();

        LivingEntity livingentity = this.mob.getTarget();
        if (!monster.level().isClientSide && livingentity != null) {
            this.mob.getLookControl().setLookAt(livingentity, 15.0F, 15.0F);

            this.ticksUntilNextAnimation = Math.max(this.ticksUntilNextAnimation - 1, 0);
            this.ticksUntilNextAttack = Math.max(this.ticksUntilNextAttack - 1, 0);

            if( animatingAttack){
                if(!hitConnected && ticksUntilNextAttack <= 0){
                    List<LivingEntity> entities = mob.level().getEntitiesOfClass(LivingEntity.class, partHitbox.getBoundingBox().inflate(hitCheckAABBInflation));
                    for(LivingEntity entity: entities){
                        if(!(entity instanceof NewWorldMonsterEntity) && entity.getBoundingBox().inflate(hitTriggerAABBInflation).intersects(partHitbox.getBoundingBox().inflate(hitTriggerAABBInflation))){
                            entity.setInvulnerable(false);
                            float f = (float)mob.getAttributeValue(Attributes.ATTACK_DAMAGE);
                            float f1 = (float)mob.getAttributeValue(Attributes.ATTACK_KNOCKBACK);

                            boolean flag = entity.hurt(new DamageSource(mob.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(ModDamageTypes.RAW)), f);
                            if (flag) {
                                if (f1 > 0.0F && entity instanceof LivingEntity) {
                                    entity.knockback(f1 * 0.5F, Mth.sin(mob.getYRot() * ((float)Math.PI / 180F)), -Mth.cos(mob.getYRot() * ((float)Math.PI / 180F)));
                                }
                                mob.setLastHurtMob(entity);
                            }
                            this.monster.doHurtTarget(entity);
                        }
                    }
                    this.ticksUntilNextAttack = this.adjustedTickDelay(ticksBetweenAttacksInAnimation);

                    currentHitsConnected++;
                    hitConnected = currentHitsConnected >= maxHitCount;
                }
                if(this.ticksUntilNextAnimation <= 0){
                    animatingAttack = false;
                    //wait a bit until you try and attack again if the goal stays active
                    this.ticksUntilNextAnimation = this.adjustedTickDelay(animInterval/3);
                }
            }
            else if(!animatingAttack && this.ticksUntilNextAnimation <= 0){
                this.checkAndPerformAttack(livingentity, 0);
            }
        }
    }
    protected void checkAndPerformAttack(LivingEntity pEnemy, double pDistToEnemySqr) {
        if(!animatingAttack && this.ticksUntilNextAnimation <= 0){
            hitConnected = false;
            currentHitsConnected = 0;
            //wait a until anim is over until you try and attack again, in monster hunter fashion im trying to limit hits per animation
            monster.triggerAnim(controller,animation);
            this.ticksUntilNextAnimation = this.adjustedTickDelay(animInterval);
            this.ticksUntilNextAttack = this.adjustedTickDelay(tickForAttackChecksToBegin);

            animatingAttack = true;
        }
    }

}
