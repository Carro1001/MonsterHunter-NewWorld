package com.carro1001.mhnw.entities;

import com.carro1001.mhnw.MHNW;
import com.carro1001.mhnw.entities.ai.MonsterAggressionStateGoal;
import com.carro1001.mhnw.entities.interfaces.IGrows;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Random;

public abstract class LargeMonster extends Monster implements GeoEntity, IGrows {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final EntityDataAccessor<Boolean> WALKING = SynchedEntityData.defineId(LargeMonster.class, EntityDataSerializers.BOOLEAN);
    protected static final EntityDataAccessor<Boolean> SCALESSIGNED = SynchedEntityData.defineId(LargeMonster.class, EntityDataSerializers.BOOLEAN);
    protected static final EntityDataAccessor<Float> MONSTER_SCALE = SynchedEntityData.defineId(LargeMonster.class, EntityDataSerializers.FLOAT);
    protected static final EntityDataAccessor<Integer> AGGRESSION_STATE = SynchedEntityData.defineId(LargeMonster.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DEATH_STATE = SynchedEntityData.defineId(LargeMonster.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> LIMPING = SynchedEntityData.defineId(LargeMonster.class, EntityDataSerializers.BOOLEAN);
    protected static final EntityDataAccessor<Integer> RALLY_STATE = SynchedEntityData.defineId(LargeMonster.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> SLEEPING = SynchedEntityData.defineId(LargeMonster.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<BlockPos> HOME_POS = SynchedEntityData.defineId(LargeMonster.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<Boolean> RAGE = SynchedEntityData.defineId(LargeMonster.class, EntityDataSerializers.BOOLEAN);
    protected static final EntityDataAccessor<Float> RAGE_BUILDUP = SynchedEntityData.defineId(LargeMonster.class, EntityDataSerializers.FLOAT);

    private static final EntityDataAccessor<Boolean> ATTACKING = SynchedEntityData.defineId(LargeMonster.class, EntityDataSerializers.BOOLEAN);
    protected static final EntityDataAccessor<Integer> ATTACK_ANIMATION_ID = SynchedEntityData.defineId(LargeMonster.class, EntityDataSerializers.INT);

    protected int rallyCooldownTime = 0;

    protected float minScale = 0.6f, maxScale = 1f;

    protected String name;

    protected int maxRageBuildUp = 150;
    protected boolean shouldRage = false;

    //TEMP FOR TESTING
    @Override
    public boolean removeWhenFarAway(double pDistanceToClosestPlayer) {
        return pDistanceToClosestPlayer > 64;
    }
    public LargeMonster(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.noCulling = true;
        if (!level.isClientSide) {
            this.setRallyState(RallyState.READY);
        }
        fixupDimensions();
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
    public void tick() {
        super.tick();
        if(isSleeping()){
            this.moveControl.setWantedPosition(getX(),getY(),getZ(),0);
            this.level().addParticle(ParticleTypes.HEART, this.getRandomX(1.0D), this.getRandomY()-0.5, this.getRandomZ(1.0D), 0.0D, 0.0D, 0.0D);
        }
        else if(isLimpining()){
            this.level().addParticle(ParticleTypes.DRIPPING_WATER, this.getRandomX(1.0D), this.getRandomY(), this.getRandomZ(1.0D), 0.0D, -0.5D, 0.0D);
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

    protected void registerGoals() {
        this.goalSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true)); // This finds the closest player to target
        this.goalSelector.addGoal(1, new MonsterAggressionStateGoal(this)); // This handles the aggression state between passive, roar, and aggressive
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.7D){
            @Override
            public void start() {
                super.start();
                setWalking(true);
            }

            @Override
            public void stop() {
                super.stop();
                setWalking(false);
            }
        });
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));

        this.addBehaviourGoals();
    }

    protected void addBehaviourGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main_controller", 16, this::poseBody)
                .triggerableAnim("death", getDeathAnimation()).triggerableAnim("roar", getRoarAnimation())
                .triggerableAnim("rally", getRallyAnimation()).triggerableAnim("sleep", getSleepAnimation()));
    }

    protected PlayState poseBody(AnimationState<LargeMonster> animationState) {
        if(getDeathState() >= 1){
            MHNW.debugLog("poseBody: death");
            return PlayState.STOP;
        }
        if (animationState.isMoving() || isWalking()) {
            MHNW.debugLog("poseBody: getMovementAnimation");
            return animationState.setAndContinue(getMovementAnimation());
        }
        MHNW.debugLog("poseBody: getIdleAnimation");
        return animationState.setAndContinue(getIdleAnimation());
    }


    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    public static AttributeSupplier.Builder prepareAttributes() {
        return Monster.createLivingAttributes()
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.MAX_HEALTH, 100)
                .add(Attributes.FOLLOW_RANGE, 15.0)
                .add(Attributes.MOVEMENT_SPEED, 0.45)
                .add(Attributes.ARMOR, 1.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, (double)0.6F)
                .add(Attributes.ATTACK_KNOCKBACK, (double)1F)
                .add(Attributes.ARMOR_TOUGHNESS,1.0D);
    }
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DEATH_STATE, 0);
        this.entityData.define(RALLY_STATE, RallyState.READY.ordinal());
        this.entityData.define(SLEEPING, false);
        this.entityData.define(LIMPING, false);
        this.entityData.define(WALKING, false);
        this.entityData.define(SCALESSIGNED, false);
        this.entityData.define(ATTACKING, false);
        this.entityData.define(ATTACK_ANIMATION_ID, 0);
        this.entityData.define(RAGE, false);
        this.entityData.define(RAGE_BUILDUP, 0F);
        this.entityData.define(MONSTER_SCALE, 1F);
        this.entityData.define(HOME_POS, BlockPos.ZERO);
        this.entityData.define(AGGRESSION_STATE, AggressionState.PASSIVE.ordinal());

    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putFloat("Scale", this.getMonsterScale());
        pCompound.putInt("mon_aggro_state", getAggressionState().ordinal());
        pCompound.putBoolean("Walking", isWalking());
        pCompound.putBoolean("Limping", isLimpining());
        pCompound.putBoolean("Attacking", isAttacking());
        pCompound.putFloat("AttackID", this.getAttackingID());
        pCompound.putBoolean("Raging", isRaging());
        pCompound.putFloat("RageBuildUp", this.getRageBuildUp());
        pCompound.putInt("DeathState", this.getDeathState());
        pCompound.putBoolean("Sleeping", isSleeping());

        pCompound.putInt("HomePosX", this.getHomePos().getX());
        pCompound.putInt("HomePosY", this.getHomePos().getY());
        pCompound.putInt("HomePosZ", this.getHomePos().getZ());
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        if (pCompound.contains("Scale", Tag.TAG_FLOAT)) {
            this.setMonsterScale(pCompound.getFloat("Scale"));
        }

        if (pCompound.contains("mon_aggro_state", Tag.TAG_INT)) {
            this.setAggressionState(DragonEntity.AggressionState.values()[pCompound.getInt("mon_aggro_state")]);
        }
        setDeathState(pCompound.getInt("DeathState"));
        setWalking(pCompound.getBoolean("Walking"));
        setLimping(pCompound.getBoolean("Limping"));
        setSleeping(pCompound.getBoolean("Sleeping"));

        setAttacking(pCompound.getBoolean("Attacking"));
        setAttackingID(pCompound.getInt("AttackID"));
        setRaging(pCompound.getBoolean("Raging"));
        setRageBuildUp(pCompound.getFloat("RageBuildUp"));

        int i = pCompound.getInt("HomePosX");
        int j = pCompound.getInt("HomePosY");
        int k = pCompound.getInt("HomePosZ");
        this.setHomePos(new BlockPos(i, j, k));
    }

    public void GenerateScale(){
        if(!getScaleAssignedDir()){
            setMonsterScale((float) new Random().nextDouble(minScale, maxScale));
            setScaleAssignedDir(true);
        }
    }

    public void setScaleAssignedDir(boolean pState) {
        this.entityData.set(SCALESSIGNED, pState);
    }
    public boolean getScaleAssignedDir() {
        return this.entityData.get(SCALESSIGNED);
    }

    @Override
    public float getMonsterScale() {
        return this.entityData.get(MONSTER_SCALE);
    }

    @Override
    public void setMonsterScale(float scale) {
        if(!getScaleAssignedDir()){
            this.entityData.set(MONSTER_SCALE, scale);
            this.setScaleAssignedDir(true);
            this.reapplyPosition();
            this.refreshDimensions();
        }
    }

    public void refreshDimensions() {
        double d0 = this.getX();
        double d1 = this.getY();
        double d2 = this.getZ();
        super.refreshDimensions();
        this.setPos(d0, d1, d2);
    }

    public void onSyncedDataUpdated(EntityDataAccessor<?> pKey) {
        if (MONSTER_SCALE.equals(pKey)) {
            this.refreshDimensions();
            this.setYRot(this.yHeadRot);
            this.yBodyRot = this.yHeadRot;
        }
        super.onSyncedDataUpdated(pKey);
    }
    public EntityDimensions getDimensions(Pose pPose) {
        return super.getDimensions(pPose).scale(getMonsterScale());
    }

    public void setAggressionState(DragonEntity.AggressionState aggressionState) {
        this.entityData.set(AGGRESSION_STATE, aggressionState.ordinal());
    }

    public LargeMonster.AggressionState getAggressionState() {
        return LargeMonster.AggressionState.values()[this.entityData.get(AGGRESSION_STATE)];
    }

    protected RawAnimation getIdleAnimation(){
        return RawAnimation.begin().thenLoop("animation."+name+".idle");
    }

    protected RawAnimation getMovementAnimation() {
        return RawAnimation.begin().thenLoop("animation."+name+".walk");
    }

    protected RawAnimation getRoarAnimation() {
        return RawAnimation.begin().thenLoop("animation."+name+".roar");
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

    public void setWalking(boolean walking){
        this.entityData.set(WALKING, walking);
    }

    public boolean isWalking(){
        return this.entityData.get(WALKING);
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

    public void setRallyState(RallyState state)
    {
        if(!level().isClientSide && state == RallyState.COOL_DOWN){
            this.rallyCooldownTime = this.random.nextInt(500, 2000);
        }
        this.entityData.set(RALLY_STATE, state.ordinal());
    }

    public RallyState getRallyState() {
        return RallyState.values()[this.entityData.get(RALLY_STATE)];
    }

    public void setHomePos(BlockPos pHomePos) {
        this.entityData.set(HOME_POS, pHomePos);
    }

    BlockPos getHomePos() {
        return this.entityData.get(HOME_POS);
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
            MHNW.debugLog(name + ": setRaging(false)");
        }
        this.entityData.set(RAGE_BUILDUP, rageBuildUp);
    }
    public void tickRageBuildUp(float amount){
        setRageBuildUp(getRageBuildUp()+amount);
    }
    public float getAttackingID(){
        return this.entityData.get(ATTACK_ANIMATION_ID);
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
        return hurt;
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

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData, @Nullable CompoundTag pDataTag) {
        GenerateScale();
        this.setHomePos(this.blockPosition());
        return super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData, pDataTag);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double pDistance) {
        double d0 = this.getBoundingBox().getSize();
        if (Double.isNaN(d0)) {
            d0 = 1.0D;
        }

        d0 *= 64.0D * 4;
        return pDistance < d0 * d0;
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
