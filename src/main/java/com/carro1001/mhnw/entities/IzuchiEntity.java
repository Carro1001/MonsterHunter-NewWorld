package com.carro1001.mhnw.entities;

import com.carro1001.mhnw.registration.ModEntities;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.*;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.Level;

import java.util.Optional;

import static com.carro1001.mhnw.utils.MHNWReferences.IZUCHI;

public class IzuchiEntity extends Monster {
    protected static final ImmutableList<? extends SensorType<? extends Sensor<? super IzuchiEntity>>> SENSOR_TYPES = ImmutableList.of(SensorType.NEAREST_LIVING_ENTITIES, SensorType.NEAREST_PLAYERS);
    protected static final ImmutableList<? extends MemoryModuleType<?>> MEMORY_TYPES = ImmutableList.of(MemoryModuleType.NEAREST_LIVING_ENTITIES, MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryModuleType.NEAREST_VISIBLE_PLAYER, MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER, MemoryModuleType.LOOK_TARGET, MemoryModuleType.WALK_TARGET, MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE, MemoryModuleType.PATH, MemoryModuleType.ATTACK_TARGET, MemoryModuleType.ATTACK_COOLING_DOWN);

    public IzuchiEntity(EntityType<? extends Monster> p_27557_, Level p_27558_) {
        super(p_27557_, p_27558_);
        this.name = IZUCHI;
    }

    protected Brain.Provider<IzuchiEntity> brainProvider() {
        return Brain.provider(MEMORY_TYPES, SENSOR_TYPES);
    }

    protected Brain<?> makeBrain(Dynamic<?> pDynamic) {
        Brain<IzuchiEntity> brain = this.brainProvider().makeBrain(pDynamic);
        initCoreActivity(brain);
        initIdleActivity(brain);
        initFightActivity(brain);
        brain.setCoreActivities(ImmutableSet.of(Activity.CORE));
        brain.setDefaultActivity(Activity.IDLE);
        brain.useDefaultActivity();
        return brain;
    }

    private static void initCoreActivity(Brain<IzuchiEntity> pBrain) {
        pBrain.addActivity(Activity.CORE, 0, ImmutableList.of(new LookAtTargetSink(45, 90), new MoveToTargetSink()));
    }

    private static void initIdleActivity(Brain<IzuchiEntity> pBrain) {
        pBrain.addActivity(Activity.IDLE, 10, ImmutableList.of(StartAttacking.create(IzuchiEntity::findNearestValidAttackTarget), SetEntityLookTargetSometimes.create(8.0F, UniformInt.of(30, 60)), new RunOne<>(ImmutableList.of(Pair.of(RandomStroll.stroll(0.4F), 2), Pair.of(SetWalkTargetFromLookTarget.create(0.4F, 3), 2), Pair.of(new DoNothing(30, 60), 1)))));
    }

    private static void initFightActivity(Brain<IzuchiEntity> pBrain) {
        pBrain.addActivityAndRemoveMemoryWhenStopped(Activity.FIGHT, 10, ImmutableList.of(SetWalkTargetFromAttackTargetIfTargetOutOfReach.create(1.0F), MeleeAttack.create(40), StopAttackingIfTargetInvalid.create()), MemoryModuleType.ATTACK_TARGET);
    }

    private Optional<? extends LivingEntity> findNearestValidAttackTarget() {
        return this.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES).orElse(NearestVisibleLivingEntities.empty()).findClosest(this::isTargetable);
    }

    private boolean isTargetable(LivingEntity p_34253_) {
        EntityType<?> entitytype = p_34253_.getType();
        return entitytype != ModEntities.IZUCHI.get() && entitytype != ModEntities.GIZUCHI.get() && entitytype != EntityType.CREEPER && Sensor.isEntityAttackable(this, p_34253_);
    }

    public boolean hurt(DamageSource pSource, float pAmount) {
        boolean flag = super.hurt(pSource, pAmount);
        if (this.level().isClientSide) {
            return false;
        } else if (flag && pSource.getEntity() instanceof LivingEntity) {
            LivingEntity livingentity = (LivingEntity)pSource.getEntity();
            if (this.canAttack(livingentity) && !BehaviorUtils.isOtherTargetMuchFurtherAwayThanCurrentAttackTarget(this, livingentity, 4.0D)) {
                this.setAttackTarget(livingentity);
            }

            return flag;
        } else {
            return flag;
        }
    }

    private void setAttackTarget(LivingEntity pTarget) {
        this.brain.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
        this.brain.setMemoryWithExpiry(MemoryModuleType.ATTACK_TARGET, pTarget, 200L);
    }
    public Brain<IzuchiEntity> getBrain() {
        return (Brain<IzuchiEntity>) super.getBrain();
    }

    protected void updateActivity() {
        Activity activity = this.brain.getActiveNonCoreActivity().orElse((Activity)null);
        this.brain.setActiveActivityToFirstValid(ImmutableList.of(Activity.FIGHT, Activity.IDLE));
        Activity activity1 = this.brain.getActiveNonCoreActivity().orElse((Activity)null);
        if (activity1 == Activity.FIGHT && activity != Activity.FIGHT) {
            this.playAngrySound();
        }

        this.setAggressive(this.brain.hasMemoryValue(MemoryModuleType.ATTACK_TARGET));
    }

    protected void customServerAiStep() {
        this.level().getProfiler().push("izuchiBrain");
        this.getBrain().tick((ServerLevel)this.level(), this);
        this.level().getProfiler().pop();
        this.updateActivity();
    }
    public void handleEntityEvent(byte pId) {
        if (pId == 4) {
            this.playSound(SoundEvents.ZOGLIN_ATTACK, 1.0F, this.getVoicePitch());
        } else {
            super.handleEntityEvent(pId);
        }
    }
    protected void playAngrySound() {
        this.playSound(SoundEvents.ZOGLIN_ANGRY, 1.0F, this.getVoicePitch());
    }

    protected void sendDebugPackets() {
        super.sendDebugPackets();
        DebugPackets.sendEntityBrain(this);
    }

    public static AttributeSupplier.Builder prepareAttributes() {
        return Mob.createLivingAttributes()
                .add(Attributes.ATTACK_KNOCKBACK, 2.0)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.MAX_HEALTH, 10)
                .add(Attributes.FOLLOW_RANGE, 15.0)
                .add(Attributes.MOVEMENT_SPEED, 0.45)
                .add(Attributes.ARMOR, 1.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, (double)0.3F)
                .add(Attributes.ARMOR_TOUGHNESS,1.0D);
    }
}
