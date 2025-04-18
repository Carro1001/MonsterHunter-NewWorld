package com.carro1001.mhnw.entities;

import com.carro1001.mhnw.MHNW;
import com.carro1001.mhnw.entities.ai.ExhaustedStallGoal;
import com.carro1001.mhnw.entities.ai.MonsterAggressionStateGoal;
import com.carro1001.mhnw.entities.helpers.MonsterBreakablePartEntity;
import com.carro1001.mhnw.entities.interfaces.IAttributes;
import com.carro1001.mhnw.entities.interfaces.IMonsterBreakablePart;
import com.carro1001.mhnw.registration.ModEntities;
import de.dertoaster.multihitboxlib.api.IMultipartEntity;
import de.dertoaster.multihitboxlib.entity.MHLibPartEntity;
import de.dertoaster.multihitboxlib.entity.hitbox.SubPartConfig;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

import java.util.*;
import java.util.concurrent.LinkedTransferQueue;

public abstract class NewWorldMonsterEntity extends NewWorldGrowingEntity implements Enemy, IAttributes, IMultipartEntity<NewWorldMonsterEntity> {

    private static final EntityDataAccessor<Integer> AGGRESSION_STATE = SynchedEntityData.defineId(NewWorldMonsterEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DEATH_STATE = SynchedEntityData.defineId(NewWorldMonsterEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> LIMPING = SynchedEntityData.defineId(NewWorldMonsterEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> RALLY_STATE = SynchedEntityData.defineId(NewWorldMonsterEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> SLEEPING = SynchedEntityData.defineId(NewWorldMonsterEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> TAIL_CUT = SynchedEntityData.defineId(NewWorldMonsterEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Boolean> RAGE = SynchedEntityData.defineId(NewWorldMonsterEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> RAGE_BUILDUP = SynchedEntityData.defineId(NewWorldMonsterEntity.class, EntityDataSerializers.FLOAT);

    private static final EntityDataAccessor<Boolean> EXHAUSTED = SynchedEntityData.defineId(NewWorldMonsterEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> EXHAUST_BUILDUP = SynchedEntityData.defineId(NewWorldMonsterEntity.class, EntityDataSerializers.FLOAT);

    private static final EntityDataAccessor<Boolean> ATTACKING = SynchedEntityData.defineId(NewWorldMonsterEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> ATTACK_ANIMATION_ID = SynchedEntityData.defineId(NewWorldMonsterEntity.class, EntityDataSerializers.INT);

    protected List<Elements> MonsterWeakness = List.of(Elements.NONE);
    protected List<Elements> MonsterAttackElements = List.of(Elements.NONE);

    protected List<Blights> MonsterBlightWeakness = List.of(Blights.NONE);
    protected List<Blights> MonsterBlightResistance = List.of(Blights.NONE);
    protected List<Blights> MonsterPossibleAttackingBlights = List.of(Blights.NONE);

    public Map<IMonsterBreakablePart.PART, List<MonsterBreakablePartEntity<NewWorldMonsterEntity>>> partTypeMap;
    private final Queue<UUID> trackerQueue = new LinkedTransferQueue<>();
    private int _mhTicksSinceLastSync = 0;

    protected int rallyCooldownTime = 0;

    protected String name;
    public int deathTickTime = 40;

    protected int maxExhaustBuildUp = 150;
    protected int maxRageBuildUp = 150;
    protected boolean shouldRage = false;

    protected int currentAnimationTick = 0;

    public NewWorldMonsterEntity(EntityType<? extends NewWorldGrowingEntity> entityType, Level level) {
        super(entityType, level);
        if (!level.isClientSide) {
            this.setRallyState(RallyState.READY);
        }
    }

    @Override
    protected InteractionResult mobInteract(Player pPlayer, InteractionHand pHand) {
        if(pPlayer.getItemInHand(pHand).is(Items.STICK) && pPlayer.isCreative()){
            pPlayer.playSound(SoundEvents.ALLAY_DEATH, 3.0F, 2.0F);
            this.discard();
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        if(getDeathState() >= 1 && pPlayer.getItemInHand(pHand).isEmpty() && pPlayer.isCrouching()){
            pPlayer.playSound(SoundEvents.AXE_STRIP, 1.0F, 1.0F);
            List<Item> lootTable = getDrops();
            ItemStack itemstack1 = new ItemStack(lootTable.get(getRandom().nextInt(0,lootTable.size())),getRandom().nextInt(2,5));
            ItemEntity etity = new ItemEntity(pPlayer.level(), pPlayer.getX(),pPlayer.getY(),pPlayer.getZ(),itemstack1);
            pPlayer.level().addFreshEntity(etity);
            setDeathState(getDeathState()+1);
            if(getDeathState() >= 4){
                this.discard();
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.mobInteract(pPlayer, pHand);
    }

    @Override
    public boolean hurt(DamageSource pSource, float pAmount) {
        boolean hurt = super.hurt(pSource, pAmount);
        if(hurt && getHealth() <= getMaxHealth()/4){
            setLimping(true);
            MHNW.debugLog(name + ": hurt() limping");
        }
        if(isSleeping() && pSource.getDirectEntity() instanceof LivingEntity livingEntity){
            setSleeping(false);
            setTarget(livingEntity);
            setAggressive(true);
            MHNW.debugLog(name + ": hurt() waking up");
        }
        if(shouldRage && !isRaging() && hurt){
            tickRageBuildUp(pAmount);
        }
        if(!isExhausted() && hurt){
            tickExhaustBuildUp(pAmount);
        }
        return hurt;
    }

    @Override
    public void die(DamageSource pDamageSource) {
        this.setHealth(1);
        int state = getDeathState();
        if(state == 0){
            triggerAnim("main_controller","death");
            MHNW.debugLog(name + "@"+this.position()+": i guess i will die");
            setDeathState(1);
            setNoAi(true);
        }
    }

    @Override
    public MHLibPartEntity<NewWorldMonsterEntity> createNewPartFrom(SubPartConfig spc, NewWorldMonsterEntity parentEntity, int subPartNumber) {
        MHLibPartEntity<NewWorldMonsterEntity> newHitBox = IMultipartEntity.super.createNewPartFrom(spc, parentEntity, subPartNumber);
        if(newHitBox instanceof MonsterBreakablePartEntity<NewWorldMonsterEntity> partEntity){
            if(partTypeMap ==  null){
                partTypeMap = new HashMap<>();
            }
            List<MonsterBreakablePartEntity<NewWorldMonsterEntity>> list = partTypeMap.getOrDefault(partEntity.getPartType(), List.of(partEntity));
            partTypeMap.put(partEntity.getPartType(),list);

        }
        return newHitBox;
    }

    @Override
    public void tick() {
        super.tick();
        if(isSleeping()){
            this.moveControl.setWantedPosition(getX(),getY(),getZ(),0);
            this.level().addParticle(ParticleTypes.HEART, this.getRandomX(1.0D), this.getRandomY()-0.5, this.getRandomZ(1.0D), 0.0D, 0.0D, 0.0D);
        }
        else if(isLimpining()){
            this.level().addParticle(ParticleTypes.DRIPPING_WATER, this.getRandomX(1.0D), this.getRandomY(), this.getRandomZ(1.0D), 0.0D, -0.5D, 0.0D);
        }
        else if(!isSleeping()){
            //TODO actually check this is helping
            this.yBodyRot = Mth.approachDegrees(this.yBodyRotO, yBodyRot, getHeadRotSpeed());
        }
        if (!level().isClientSide) {
            if(isRaging()){
                this.tickRageBuildUp(-1);
            }
            if (getRallyState() == RallyState.COOL_DOWN) {
                if (this.rallyCooldownTime >= 0) {
                    this.rallyCooldownTime--;
                } else {
                    setRallyState(RallyState.READY);
                }
            }
        }
    }

    @Override
    public void heal(float pHealAmount) {
        super.heal(pHealAmount);
        if(getHealth() > getMaxHealth()/3){
            setLimping(false);
            setSleeping(false);
            MHNW.debugLog(name + " healed: setLimping(false); setSleeping(false);");
        }
    }

    public void aiStep() {
        this.updateSwingTime();
        super.aiStep();
        if(currentAnimationTick> 0){
            currentAnimationTick--;
        }
    }

    @Override
    protected void tickDeath() {
        if(this.deathTime < deathTickTime){
            ++this.deathTime;
        }
        if (this.deathTime >= deathTickTime && !this.level().isClientSide() && !this.isRemoved() && getDeathState() > 4 ) {
            this.level().broadcastEntityEvent(this, (byte)60);
            this.remove(Entity.RemovalReason.KILLED);
        }
    }

    @Override
    public boolean isDeadOrDying() {
        return super.isDeadOrDying() || getDeathState() >= 1;
    }

    //TODO change per mob, make generic
    public EntityType<TailEntity> getTailEntity(){
        return ModEntities.RATHALOS_TAIL.get();
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData, @Nullable CompoundTag pDataTag) {
        //partTypeMap.forEach((key, value) -> MHNW.LOGGER.debug(this+ "spawned with hitbox: " + key + ": " + value));
        return super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData, pDataTag);
    }

    //region Goals
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true)); // This finds the closest player to target
        this.goalSelector.addGoal(1, new MonsterAggressionStateGoal(this)); // This handles the aggression state between passive, roar, and aggressive
        this.goalSelector.addGoal(2, new ExhaustedStallGoal(this)); // This handles the stall for when a mob is exhausted
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.7D){
            @Override
            public boolean canUse() {
                return super.canUse() && !mob.isSleeping();
            }
        });
        //this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 6.0F));
        //this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));

        this.addBehaviourGoals();
    }

    protected void addBehaviourGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
    }
    //endregion

//region Animation

    public boolean isAnimating(){
        return currentAnimationTick > 0;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(getNewWorldMonsterEntityAnimationController());
    }

    protected AnimationController<NewWorldMonsterEntity> getNewWorldMonsterEntityAnimationController() {
        return new AnimationController<>(this, "main_controller", 16, this::poseBody)
                .triggerableAnim("death", getDeathAnimation()).triggerableAnim("roar", getRoarAnimation())
                .triggerableAnim("rally", getRallyAnimation()).triggerableAnim("sleep", getSleepAnimation());
    }

    protected PlayState poseBody(AnimationState<NewWorldMonsterEntity> animationState) {
        if(getDeathState() >= 1){
            MHNW.debugLog("poseBody: death");
            return animationState.setAndContinue(getDeathAnimation());
        }
        if (animationState.isMoving()) {
            MHNW.debugLog("poseBody: getMovementAnimation");
            return animationState.setAndContinue(getMovementAnimation());
        }
        MHNW.debugLog("poseBody: getIdleAnimation");
        return animationState.setAndContinue(getIdleAnimation());
    }

    protected RawAnimation getIdleAnimation(){
        return RawAnimation.begin().thenLoop("animation."+name+".idle");
    }
    protected RawAnimation getMovementAnimation() {
        return RawAnimation.begin().thenLoop("animation."+name+".walk");
    }
    protected RawAnimation getRoarAnimation() {
        return RawAnimation.begin().thenPlay("animation."+name+".roar");
    }
    protected RawAnimation getDeathAnimation() {
        return RawAnimation.begin().thenPlayAndHold("animation."+name+".death");
    }
    protected RawAnimation getRallyAnimation() {
        return RawAnimation.begin().thenPlay("animation."+name+".rally");
    }
    protected RawAnimation getSleepAnimation() {
        return RawAnimation.begin().thenPlay("animation."+name+".sleep");
    }
//endregion

//region Data
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DEATH_STATE, 0);
        this.entityData.define(RALLY_STATE, RallyState.READY.ordinal());
        this.entityData.define(SLEEPING, false);
        this.entityData.define(TAIL_CUT, false);
        this.entityData.define(LIMPING, false);
        this.entityData.define(ATTACKING, false);
        this.entityData.define(ATTACK_ANIMATION_ID, 0);
        this.entityData.define(RAGE, false);
        this.entityData.define(RAGE_BUILDUP, 0F);
        this.entityData.define(EXHAUSTED, false);
        this.entityData.define(EXHAUST_BUILDUP, 0F);
        this.entityData.define(AGGRESSION_STATE, AggressionState.PASSIVE.ordinal());
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putInt("mon_aggro_state", getAggressionState().ordinal());
        pCompound.putBoolean("Limping", isLimpining());
        pCompound.putBoolean("Attacking", isAttacking());
        pCompound.putFloat("AttackID", this.getAttackingID());
        pCompound.putBoolean("Raging", isRaging());
        pCompound.putFloat("RageBuildUp", this.getRageBuildUp());
        pCompound.putBoolean("Exhausted", isExhausted());
        pCompound.putFloat("ExhaustBuildUp", this.getExhaustBuildUp());
        pCompound.putInt("DeathState", this.getDeathState());
        pCompound.putBoolean("Sleeping", isSleeping());
        pCompound.putBoolean("TailCut", isTailCut());
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        if (pCompound.contains("mon_aggro_state", Tag.TAG_INT)) {
            this.setAggressionState(DragonEntity.AggressionState.values()[pCompound.getInt("mon_aggro_state")]);
        }
        setDeathState(pCompound.getInt("DeathState"));
        setLimping(pCompound.getBoolean("Limping"));
        setSleeping(pCompound.getBoolean("Sleeping"));
        setTailCut(pCompound.getBoolean("TailCut"));

        setAttacking(pCompound.getBoolean("Attacking"));
        setAttackingID(pCompound.getInt("AttackID"));
        setRaging(pCompound.getBoolean("Raging"));
        setRageBuildUp(pCompound.getFloat("RageBuildUp"));
        setExhausted(pCompound.getBoolean("Exhausted"));
        setExhaustBuildUp(pCompound.getFloat("ExhaustBuildUp"));
    }
    //endregion

//region SET/GET
    public void setAggressionState(DragonEntity.AggressionState aggressionState) {
        this.entityData.set(AGGRESSION_STATE, aggressionState.ordinal());
    }

    public NewWorldMonsterEntity.AggressionState getAggressionState() {
        return NewWorldMonsterEntity.AggressionState.values()[this.entityData.get(AGGRESSION_STATE)];
    }

    public void setDeathState(int state){
        this.entityData.set(DEATH_STATE, state);
    }
    public int getDeathState(){
        return this.entityData.get(DEATH_STATE);
    }

    public void setLimping(boolean limping){
        this.entityData.set(LIMPING, limping);
    }
    public boolean isLimpining(){
        return this.entityData.get(LIMPING);
    }

    public void setSleeping(boolean sleeping){
        this.entityData.set(SLEEPING, sleeping);
    }
    public boolean isSleeping(){
        return this.entityData.get(SLEEPING);
    }
    public void setTailCut(boolean tailCut){
        this.entityData.set(TAIL_CUT, tailCut);
    }
    public boolean isTailCut(){
        return this.entityData.get(TAIL_CUT);
    }

    public void setRallyState(RallyState state) {
        if(!level().isClientSide && state == RallyState.COOL_DOWN){
            this.rallyCooldownTime = this.random.nextInt(500, 2000);
        }
        this.entityData.set(RALLY_STATE, state.ordinal());
    }
    public RallyState getRallyState() {
        return RallyState.values()[this.entityData.get(RALLY_STATE)];
    }

    public void setAttacking(boolean attacking){
        this.entityData.set(ATTACKING, attacking);
    }
    public boolean isAttacking(){
        return this.entityData.get(ATTACKING);
    }

    public void setAttackingID(int attackingID){
        this.entityData.set(ATTACK_ANIMATION_ID, attackingID);
    }
    public float getAttackingID(){
        return this.entityData.get(ATTACK_ANIMATION_ID);
    }

    public float getExhaustBuildUp(){
        return this.entityData.get(EXHAUST_BUILDUP);
    }
    public void setExhausted(boolean exhausted){
        this.entityData.set(EXHAUSTED, exhausted);
    }
    public boolean isExhausted(){
        return this.entityData.get(EXHAUSTED);
    }

    public void setExhaustBuildUp(float exhaustBuildUp){
        if(!isExhausted() && exhaustBuildUp >= maxExhaustBuildUp){
            setExhausted(true);
            maxExhaustBuildUp *= 2;
            exhaustBuildUp *= 2;
            MHNW.debugLog(name + ": setExhausted(true) for " + exhaustBuildUp +  "ticks");
        }
        if(isExhausted() && exhaustBuildUp <= 0 ){
            setExhausted(false);
            MHNW.debugLog(name + ": setExhausted(false)");
        }
        this.entityData.set(RAGE_BUILDUP, exhaustBuildUp);
    }

    public float getRageBuildUp(){
        return this.entityData.get(RAGE_BUILDUP);
    }
    public void setRaging(boolean raging){
        this.entityData.set(RAGE, shouldRage && raging);
    }
    public boolean isRaging(){
        return this.entityData.get(RAGE);
    }

    public void setRageBuildUp(float rageBuildUp){
        if(shouldRage && !isRaging() && rageBuildUp >= maxRageBuildUp){
            setRaging(true);
            rageBuildUp *= 2;
            MHNW.debugLog(name + ": setRaging(true) for " + rageBuildUp +  "ticks");
        }
        if(isRaging() && rageBuildUp <= 0 ){
            setRaging(false);
            setExhaustBuildUp(maxExhaustBuildUp);
            MHNW.debugLog(name + ": setRaging(false)");
        }
        this.entityData.set(RAGE_BUILDUP, rageBuildUp);
    }

    public void tickRageBuildUp(float amount){
        setRageBuildUp(getRageBuildUp()+amount);
    }

    public void tickExhaustBuildUp(float amount){
        setExhaustBuildUp(getExhaustBuildUp()+amount);
    }
//endregion


    protected boolean shouldDespawnInPeaceful() {
        return true;
    }

    public LivingEntity.Fallsounds getFallSounds() {
        return new LivingEntity.Fallsounds(SoundEvents.HOSTILE_SMALL_FALL, SoundEvents.HOSTILE_BIG_FALL);
    }

    public List<Elements> getMonsterWeakness() {
        return MonsterWeakness;
    }

    public List<Elements> getMonsterAttackElements() {

        return MonsterAttackElements;
    }

    public List<Blights> getMonsterBlightWeakness() {
        return MonsterBlightWeakness;
    }
    public List<Blights> getMonsterBlightResistance() {

        return MonsterBlightResistance;
    }

    public List<Blights> getMonsterPossibleAttackingBlights() {
        return MonsterPossibleAttackingBlights;
    }

    @Override
    public boolean callSuperHurt(DamageSource damageSource, float v) {
        return super.hurt(damageSource,v);
    }

    @Override
    public Queue<UUID> getTrackerQueue() {
        return trackerQueue;
    }

    @Override
    public int getTicksSinceLastSync() {
        return _mhTicksSinceLastSync;
    }

    public int getHeadRotSpeed() {
        return 15;
    }
    public enum AggressionState {
        PASSIVE,
        ROAR,
        AGGRESSIVE
    }
    public enum RallyState {
        READY,
        COOL_DOWN
    }


}
