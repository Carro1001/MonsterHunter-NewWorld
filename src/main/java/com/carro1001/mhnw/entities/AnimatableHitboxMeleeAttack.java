package com.carro1001.mhnw.entities;

import com.carro1001.mhnw.entities.helpers.MonsterBreakablePartEntity;
import com.carro1001.mhnw.entities.interfaces.IMonsterBreakablePart;
import com.carro1001.mhnw.registration.ModDamageTypes;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.phys.Vec3;
import net.tslat.smartbrainlib.api.core.behaviour.DelayedBehaviour;
import net.tslat.smartbrainlib.util.BrainUtils;
import net.tslat.smartbrainlib.util.EntityRetrievalUtil;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

public class AnimatableHitboxMeleeAttack<E extends IzuchiEntity> extends DelayedBehaviour<E> {

    String controller,animation;
    IMonsterBreakablePart.PART partForHitbox;
    MonsterBreakablePartEntity<NewWorldMonsterEntity> partHitbox;
    protected int maxHitCount;
    private HashMap<UUID,Integer> hitIDToEnemyUUID;
    private int animInterval;
    protected int ticksBetweenAttacksInAnimation;
    private int ticksUntilNextAttack;
    private int ticksUntilNextAnimation;
    private int ticksCounterForAttacksEnding;
    private boolean animating = false;
    protected Function<E, Integer> attackIntervalSupplier = (entity) -> {
        return 20;
    };
    protected @Nullable LivingEntity target = null;

    int tickForAttackChecksToBegin;
    int tickForAttackChecksToEnd;
    private static final List<Pair<MemoryModuleType<?>, MemoryStatus>> MEMORY_REQUIREMENTS = ObjectArrayList.of(Pair.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT), Pair.of( MemoryModuleType.ATTACK_COOLING_DOWN, MemoryStatus.VALUE_ABSENT), Pair.of(MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED));
    private Vec3 spinVelocity;

    public AnimatableHitboxMeleeAttack(int delayTicks, String pController, String p_Anim,  int maxHits,
                                       int ticksBetweenAttacksInAnimation, int tickForAttackChecksToBegin,int tickForAttackChecksToEnd,IMonsterBreakablePart.PART part) {
        super(delayTicks);
        controller=pController;
        animation=p_Anim;
        partForHitbox = part;
        this.maxHitCount = maxHits;
        this.ticksBetweenAttacksInAnimation = ticksBetweenAttacksInAnimation;
        this.tickForAttackChecksToBegin = tickForAttackChecksToBegin;
        this.tickForAttackChecksToEnd = tickForAttackChecksToEnd;
        this.animInterval = delayTicks;
        this.ticksUntilNextAnimation = delayTicks;
        attackInterval((entity) -> delayTicks);
        this.ticksUntilNextAttack = tickForAttackChecksToBegin;
        ticksCounterForAttacksEnding = ticksUntilNextAnimation - tickForAttackChecksToEnd;
        this.runFor((entity) -> delayTicks);
    }

    @Override
    protected boolean canStillUse(ServerLevel level, E entity, long gameTime) {
        return super.canStillUse(level, entity, gameTime) && animating;
    }

    @Override
    protected List<Pair<MemoryModuleType<?>, MemoryStatus>> getMemoryRequirements() {
        return MEMORY_REQUIREMENTS;
    }
    protected boolean checkExtraStartConditions(ServerLevel level, E entity) {
        this.target = BrainUtils.getTargetOfEntity(entity);
        return !entity.isSleeping() && entity.hasRoared() && entity.getSensing().hasLineOfSight(this.target) && entity.isWithinMeleeAttackRange(this.target) && entity.partTypeMap.containsKey(partForHitbox);
    }

    public AnimatableHitboxMeleeAttack<E> attackInterval(Function<E, Integer> supplier) {
        this.attackIntervalSupplier = supplier;
        return this;
    }

    protected void start(E entity) {
        if(!animating){
            BrainUtils.clearMemories(entity, MemoryModuleType.WALK_TARGET, MemoryModuleType.LOOK_TARGET, MemoryModuleType.PATH);
            entity.setDeltaMovement(0, entity.getDeltaMovement().y, 0);
            entity.triggerAnim(controller,animation);
            this.ticksUntilNextAnimation = animInterval;
            this.ticksUntilNextAttack = tickForAttackChecksToBegin;
            this.ticksCounterForAttacksEnding = ticksUntilNextAnimation - tickForAttackChecksToEnd;
            partHitbox = entity.partTypeMap.get(partForHitbox).get(0);
            hitIDToEnemyUUID = new HashMap<>();
            Vec3 targetPos = new Vec3(target.getX(0.5f), 0, target.getZ(0.5f));
            Vec3 entityPos = new Vec3(entity.getX(0.5f), 0, entity.getZ(0.5f));
            double moveSpeed = Math.max(entity.getSpeed(), entity.getAttributeValue(Attributes.MOVEMENT_SPEED)) * 2;
            this.spinVelocity = targetPos.subtract(entityPos).normalize().multiply(moveSpeed, 0, moveSpeed);
            entity.lookAt(EntityAnchorArgument.Anchor.FEET,spinVelocity);
            animating = true;
        }

    }

    @Override
    protected void tick(E monster) {
        super.tick(monster);
        if (animating && ticksUntilNextAnimation <= 0) {
            animating = false;
            return;
        }
        this.ticksUntilNextAttack = Math.max(this.ticksUntilNextAttack - 1, 0);
        this.ticksUntilNextAnimation = Math.max(this.ticksUntilNextAnimation - 1, 0);
        if(ticksUntilNextAttack == 0 && ticksUntilNextAnimation > ticksCounterForAttacksEnding){
            float f = (float)monster.getAttributeValue(Attributes.ATTACK_DAMAGE);
            float f1 = (float)monster.getAttributeValue(Attributes.ATTACK_KNOCKBACK);
            for (LivingEntity target : EntityRetrievalUtil.<LivingEntity>getEntities(monster.level(), partHitbox.getBoundingBox().expandTowards(this.spinVelocity), target -> target != monster && target.isAlive())) {

                boolean flag = target.hurt(new DamageSource(monster.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(ModDamageTypes.RAW)), f);
                target.knockback(f1 * 0.45F, Mth.sin(monster.getYRot() * ((float)Math.PI / 180F)), -Mth.cos(monster.getYRot() * ((float)Math.PI / 180F)));
            }
            /*List<LivingEntity> entities = monster.level().getEntitiesOfClass(LivingEntity.class, partHitbox.getBoundingBox().inflate(1.15));
            for(LivingEntity entity: entities){
                if(!(entity instanceof NewWorldMonsterEntity) && entity.getBoundingBox().intersects(partHitbox.getBoundingBox())){
                    float f = (float)monster.getAttributeValue(Attributes.ATTACK_DAMAGE);
                    boolean flag = entity.hurt(new DamageSource(monster.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(ModDamageTypes.RAW)), f);
                    if (flag) {
                        float f1 = (float)monster.getAttributeValue(Attributes.ATTACK_KNOCKBACK);
                        if (f1 > 0.0F && entity instanceof LivingEntity) {
                            entity.knockback(f1 * 0.45F, Mth.sin(monster.getYRot() * ((float)Math.PI / 180F)), -Mth.cos(monster.getYRot() * ((float)Math.PI / 180F)));
                        }
                        monster.setLastHurtMob(entity);
                    }
                }
            }*/
            this.ticksUntilNextAttack = ticksBetweenAttacksInAnimation;
        }



    }

    protected void stop(E entity) {
        this.target = null;
    }

    protected void doDelayedAction(E entity) {
        BrainUtils.setForgettableMemory(entity, MemoryModuleType.ATTACK_COOLING_DOWN, true, (Integer)this.attackIntervalSupplier.apply(entity));
    }

}
