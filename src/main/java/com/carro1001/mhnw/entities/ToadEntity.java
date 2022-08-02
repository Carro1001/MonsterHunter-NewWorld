package com.carro1001.mhnw.entities;

import com.carro1001.mhnw.entities.ai.ToadSwellGoal;
import com.carro1001.mhnw.setup.ModConfig;
import com.carro1001.mhnw.setup.Registration;
import com.carro1001.mhnw.utils.MHNWReferences;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.JumpControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Bucketable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Random;

public class ToadEntity extends PathfinderMob implements Bucketable{
    private static final EntityDataAccessor<Integer> TYPE = SynchedEntityData.defineId(ToadEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> TYPEASSIGNED = SynchedEntityData.defineId(ToadEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> FROM_BUCKET = SynchedEntityData.defineId(ToadEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Integer> DATA_SWELL_DIR = SynchedEntityData.defineId(ToadEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_IS_IGNITED = SynchedEntityData.defineId(ToadEntity.class, EntityDataSerializers.BOOLEAN);
    private int oldSwell;
    private int swell;
    private int maxSwell = 30;
    private int explosionRadius = 3;
    public int counterAnim = 0;
    private int jumpTicks;
    private int jumpDuration;
    private boolean wasOnGround;
    private int jumpDelayTicks;
    public ToadEntity(EntityType<? extends PathfinderMob> p_27557_, Level p_27558_) {
        super(p_27557_, p_27558_);
        this.jumpControl = new ToadJumpControl(this);
        this.moveControl = new ToadMoveControl(this);
        this.setSpeedModifier(0.0D);

    }

    public static String getVariantName(int i) {
        switch (i) {
            case 0 -> {
                return MHNWReferences.POISON+MHNWReferences.TOAD;
            }
            case 1 -> {
                return MHNWReferences.SLEEP+MHNWReferences.TOAD;
            }
            case 2 -> {
                return MHNWReferences.PARALIZE+MHNWReferences.TOAD;
            }
            default -> {
                return MHNWReferences.BLAST+MHNWReferences.TOAD;
            }
        }
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new ToadSwellGoal(this));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.6D));

        super.registerGoals();
    }

    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(TYPEASSIGNED, false);
        this.entityData.define(TYPE, 0);
        this.entityData.define(FROM_BUCKET, false);
        this.entityData.define(DATA_SWELL_DIR, -1);
        this.entityData.define(DATA_IS_IGNITED, false);

    }
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);

        pCompound.putShort("Fuse", (short)this.maxSwell);
        pCompound.putByte("ExplosionRadius", (byte)this.explosionRadius);
        pCompound.putBoolean("ignited", this.isIgnited());

        pCompound.putBoolean("type_assign", getTypeAssignedDir());
        pCompound.putInt("type", getTypeDir());
        pCompound.putBoolean("FromBucket", this.fromBucket());

    }
    public boolean isIgnited() {
        return this.entityData.get(DATA_IS_IGNITED);
    }
    public boolean getTypeAssignedDir() {
        return this.entityData.get(TYPEASSIGNED);
    }
    public int getTypeDir() {
        return this.entityData.get(TYPE);
    }
    public void setTypeAssignedDir(boolean pState) {
        this.entityData.set(TYPEASSIGNED, pState);
    }
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        if (pCompound.contains("Fuse", 99)) {
            this.maxSwell = pCompound.getShort("Fuse");
        }
        if (pCompound.contains("ExplosionRadius", 99)) {
            this.explosionRadius = pCompound.getByte("ExplosionRadius");
        }
        if (pCompound.contains("ExplosionRadius", 99)) {
            this.explosionRadius = pCompound.getByte("ExplosionRadius");
        }
        this.setFromBucket(pCompound.getBoolean("FromBucket"));

        this.setTypeDir(pCompound.getInt("type"));

    }
    public void setTypeDir(int pState) {
        if(!getTypeAssignedDir()){
            this.entityData.set(TYPE, pState);
            this.setTypeAssignedDir(true);
        }
    }
    public void setType() {

        while (!getTypeAssignedDir()){
            Random random = new Random();
            float chance = random.nextFloat();
            switch(random.nextInt(4)){
                case 0:
                    if(chance < ModConfig.POISON_CHANCE.get())setTypeDir(0);
                    break;
                case 1:
                    if(chance < ModConfig.SLEEP_CHANCE.get())setTypeDir(1);
                    break;
                case 2:
                    if(chance < ModConfig.PARA_CHANCE.get())setTypeDir(2);
                    break;
                case 3:
                    if(chance < ModConfig.BLAST_CHANCE.get())setTypeDir(3);
                    break;
            }
        }
    }
    @Override
    public boolean fromBucket() {
        return this.entityData.get(FROM_BUCKET);
    }

    @Override
    public void setFromBucket(boolean p_148834_) {
        this.entityData.set(FROM_BUCKET, p_148834_);
    }

    @Override
    public void saveToBucketTag(ItemStack p_148833_) {
        if (this.hasCustomName()) {
            p_148833_.setHoverName(this.getCustomName());
        }
        Bucketable.saveDefaultDataToBucketTag(this, p_148833_);
        CompoundTag compoundtag = p_148833_.getOrCreateTag();
        compoundtag.putInt("BucketVariantTag", this.getTypeDir());
    }

    @Override
    public void loadFromBucketTag(CompoundTag p_148832_) {
        Bucketable.loadDefaultDataFromBucketTag(this, p_148832_);
        if (p_148832_.contains("BucketVariantTag", 3)) {
            this.setTypeDir(p_148832_.getInt("BucketVariantTag"));
        }

    }

    public ItemStack getBucketItemStack() {
        switch(getTypeDir()){
            case 0 -> {
                return new ItemStack(Registration.BUCKET_POISONTOAD_ITEM.get());
            }
            case 1 -> {
                return new ItemStack(Registration.BUCKET_SLEEPTOAD_ITEM.get());
            }
            case 2 -> {
                return new ItemStack(Registration.BUCKET_PARATOAD_ITEM.get());
            }
            default -> {
                return new ItemStack(Registration.BUCKET_NITROTOAD_ITEM.get());
            }
        }
    }

    @Override
    public SoundEvent getPickupSound() {
        return null;
    }

    public InteractionResult mobInteract(Player p_149155_, InteractionHand p_149156_) {
        ItemStack itemstack = p_149155_.getItemInHand(p_149156_);
        if (itemstack.is(Items.FLINT_AND_STEEL)) {
            this.level.playSound(p_149155_, this.getX(), this.getY(), this.getZ(), SoundEvents.FLINTANDSTEEL_USE, this.getSoundSource(), 1.0F, this.random.nextFloat() * 0.4F + 0.8F);
            if (!this.level.isClientSide) {
                this.ignite();
                itemstack.hurtAndBreak(1, p_149155_, (p_32290_) -> {
                    p_32290_.broadcastBreakEvent(p_149156_);
                });
            }
            return InteractionResult.sidedSuccess(this.level.isClientSide);
        }
        return Bucketable.bucketMobPickup(p_149155_, p_149156_, this).orElse(super.mobInteract(p_149155_, p_149156_));
    }
    @Override
    public void tick() {
        super.tick();
        counterAnim++;
        if(counterAnim >= 20*5){
            counterAnim = 0;
            float xSpeed = this.random.nextFloat(0.4f);
            float zSpeed = this.random.nextFloat(0.4f);
            float ySpeed = this.random.nextFloat(0.7f,1.4f);
            double newXPos = (this.position().x +  this.random.nextFloat(-0.2f,0.3f));
            double newYPos = (this.position().y + 0.35f +  this.random.nextFloat(-0.2f,0.3f));
            double newZPos = (this.position().z +  this.random.nextFloat(-0.2f,0.3f));
            switch (getTypeDir()) {
                case 0 ->
                        Minecraft.getInstance().levelRenderer.addParticle(Registration.POISON_PARTICLE_TYPE.get(), true, newXPos, newYPos, newZPos, xSpeed, ySpeed, zSpeed);
                case 1 ->
                        Minecraft.getInstance().levelRenderer.addParticle(Registration.SLEEP_PARTICLE_TYPE.get(), true, newXPos, newYPos, newZPos, xSpeed, ySpeed, zSpeed);
                case 2 ->
                        Minecraft.getInstance().levelRenderer.addParticle(Registration.THUNDER_PARTICLE_TYPE.get(), true, newXPos, newYPos, newZPos, xSpeed, ySpeed, zSpeed);
                case 3 ->
                        Minecraft.getInstance().levelRenderer.addParticle(Registration.ICE_PARTICLE_TYPE.get(), true, newXPos, newYPos, newZPos, xSpeed, ySpeed, zSpeed);
            }
        }
        if (this.isAlive()) {
            this.oldSwell = this.swell;
            if (this.isIgnited()) {
                this.setSwellDir(1);
            }

            int i = this.getSwellDir();
            if (i > 0 && this.swell == 0) {
                this.playSound(SoundEvents.CREEPER_PRIMED, 1.0F, 0.5F);
                this.gameEvent(GameEvent.PRIME_FUSE);
            }

            this.swell += i;
            if (this.swell < 0) {
                this.swell = 0;
            }

            if (this.swell >= this.maxSwell) {
                this.swell = this.maxSwell;
                this.explodeCreeper();
            }
        }
    }

    public SpawnGroupData finalizeSpawn(ServerLevelAccessor p_149132_, DifficultyInstance p_149133_, MobSpawnType p_149134_, @Nullable SpawnGroupData p_149135_, @Nullable CompoundTag p_149136_) {
        if (p_149134_ == MobSpawnType.BUCKET) {
            return p_149135_;
        } else {
            if(ModConfig.TOADS_WEIGHT.get() == 0){
                this.dead = true;
            }
            this.setType();
            return super.finalizeSpawn(p_149132_, p_149133_, p_149134_, p_149135_, p_149136_);
        }
    }
    private void explodeCreeper() {
        if (!this.level.isClientSide) {
            this.dead = true;
            this.discard();
            this.spawnLingeringCloud();
        }

    }

    public ArrayList<MobEffectInstance> getToadEffects() {
        ArrayList<MobEffectInstance> effects = new ArrayList<>();
        switch(getTypeDir()){
            case 0:
                effects.add(new MobEffectInstance(MobEffects.POISON,200,5,true,false));
                break;
            case 1:
                effects.add(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,200,12,true,false));
                effects.add(new MobEffectInstance(MobEffects.BLINDNESS,200,5,true,false));
                break;
            case 2:
                effects.add(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,200,8,true,false));
                break;
            case 3:
                break;
        }
        return effects;
    }

    private void spawnLingeringCloud() {
        ArrayList<MobEffectInstance> effects = getToadEffects();
        if(!effects.isEmpty()){
            AreaEffectCloud areaeffectcloud = new AreaEffectCloud(this.level, this.getX(), this.getY(), this.getZ());
            areaeffectcloud.setRadius(2.5F);
            areaeffectcloud.setRadiusOnUse(-0.5F);
            areaeffectcloud.setWaitTime(10);
            areaeffectcloud.setDuration(areaeffectcloud.getDuration() / 2);
            areaeffectcloud.setRadiusPerTick(-areaeffectcloud.getRadius() / (float)areaeffectcloud.getDuration());
            for(MobEffectInstance mobeffectinstance : getToadEffects()) {
                areaeffectcloud.addEffect(new MobEffectInstance(mobeffectinstance));
            }
            this.level.addFreshEntity(areaeffectcloud);

        }
        if(getTypeDir()==3){
            Explosion.BlockInteraction explosion$blockinteraction = Explosion.BlockInteraction.NONE;
            this.level.explode(this, this.getX(), this.getY(), this.getZ(), (float)this.explosionRadius/2, explosion$blockinteraction);

        }
    }
    private void setLandingDelay() {
        if (this.moveControl.getSpeedModifier() < 2.2D) {
            this.jumpDelayTicks = 10;
        } else {
            this.jumpDelayTicks = 1;
        }
    }

    private void disableJumpControl() {
        ((ToadJumpControl)this.jumpControl).setCanJump(false);
    }
    private void enableJumpControl() {
        ((ToadJumpControl)this.jumpControl).setCanJump(true);
    }

    private void checkLandingDelay() {
        this.setLandingDelay();
        this.disableJumpControl();
    }
    public void customServerAiStep() {
        if (this.jumpDelayTicks > 0) {
            --this.jumpDelayTicks;
        }

        if (this.onGround) {
            if (!this.wasOnGround) {
                this.setJumping(false);
                this.checkLandingDelay();
            }

            ToadJumpControl toad$toadjumpcontrol = (ToadJumpControl)this.jumpControl;
            if (!toad$toadjumpcontrol.wantJump()) {
                if (this.moveControl.hasWanted() && this.jumpDelayTicks == 0) {
                    Path path = this.navigation.getPath();
                    Vec3 vec3 = new Vec3(this.moveControl.getWantedX(), this.moveControl.getWantedY(), this.moveControl.getWantedZ());
                    if (path != null && !path.isDone()) {
                        vec3 = path.getNextEntityPos(this);
                    }

                    this.facePoint(vec3.x, vec3.z);
                    this.startJumping();
                }
            } else if (!toad$toadjumpcontrol.canJump()) {
                this.enableJumpControl();
            }
        }

        this.wasOnGround = this.onGround;
    }
    public void handleEntityEvent(byte pId) {
        if (pId == 1) {
            this.spawnSprintParticle();
            this.jumpDuration = 10;
            this.jumpTicks = 0;
        } else {
            super.handleEntityEvent(pId);
        }

    }
    private void facePoint(double pX, double pZ) {
        this.setYRot((float)(Mth.atan2(pZ - this.getZ(), pX - this.getX()) * (double)(180F / (float)Math.PI)) - 90.0F);
    }
    public static AttributeSupplier.Builder prepareAttributes() {
        return Mob.createLivingAttributes()
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.MAX_HEALTH, 10)
                .add(Attributes.FOLLOW_RANGE, 15.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ARMOR, 1.0D)
                .add(Attributes.ARMOR_TOUGHNESS,1.0D);
    }
    public void ignite() {
        this.entityData.set(DATA_IS_IGNITED, true);
    }
    /**
     * Returns the current state of creeper, -1 is idle, 1 is 'in fuse'
     */
    public int getSwellDir() {
        return this.entityData.get(DATA_SWELL_DIR);
    }

    /**
     * Sets the state of creeper, -1 to idle and 1 to be 'in fuse'
     */
    public void setSwellDir(int pState) {
        this.entityData.set(DATA_SWELL_DIR, pState);
    }
    public float getSwelling(float pPartialTicks) {
        return Mth.lerp(pPartialTicks, (float)this.oldSwell, (float)this.swell) / (float)(this.maxSwell - 2);
    }
    public void setJumping(boolean pJumping) {
        super.setJumping(pJumping);
        if (pJumping) {
            this.playSound(this.getJumpSound(), this.getSoundVolume(), ((this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F) * 0.8F);
        }
    }
    protected SoundEvent getJumpSound() {
        return SoundEvents.RABBIT_JUMP;
    }

    public void startJumping() {
        this.setJumping(true);
        this.jumpDuration = 10;
        this.jumpTicks = 0;
    }
    protected float getJumpPower() {
        if (!this.horizontalCollision && (!this.moveControl.hasWanted() || !(this.moveControl.getWantedY() > this.getY() + 0.5D))) {
            Path path = this.navigation.getPath();
            if (path != null && !path.isDone()) {
                Vec3 vec3 = path.getNextEntityPos(this);
                if (vec3.y > this.getY() + 0.5D) {
                    return 0.5F;
                }
            }

            return this.moveControl.getSpeedModifier() <= 0.6D ? 0.2F : 0.3F;
        } else {
            return 0.5F;
        }
    }

    protected void jumpFromGround() {
        super.jumpFromGround();
        double d0 = this.moveControl.getSpeedModifier();
        if (d0 > 0.0D) {
            double d1 = this.getDeltaMovement().horizontalDistanceSqr();
            if (d1 < 0.01D) {
                this.moveRelative(0.1F, new Vec3(0.0D, 0.0D, 1.0D));
            }
        }

        if (!this.level.isClientSide) {
            this.level.broadcastEntityEvent(this, (byte)1);
        }

    }
    public float getJumpCompletion(float pPartialTick) {
        return this.jumpDuration == 0 ? 0.0F : ((float)this.jumpTicks + pPartialTick) / (float)this.jumpDuration;
    }
    public void setSpeedModifier(double pSpeedModifier) {
        this.getNavigation().setSpeedModifier(pSpeedModifier);
        this.moveControl.setWantedPosition(this.moveControl.getWantedX(), this.moveControl.getWantedY(), this.moveControl.getWantedZ(), pSpeedModifier);
    }

    public static class ToadJumpControl extends JumpControl {
        private final ToadEntity toad;
        private boolean canJump;

        public ToadJumpControl(ToadEntity p_186229_) {
            super(p_186229_);
            this.toad = p_186229_;
        }

        public boolean wantJump() {
            return this.jump;
        }

        public boolean canJump() {
            return this.canJump;
        }

        public void setCanJump(boolean pCanJump) {
            this.canJump = pCanJump;
        }

        /**
         * Called to actually make the entity jump if isJumping is true.
         */
        public void tick() {
            if (this.jump) {
                this.toad.startJumping();
                this.jump = false;
            }

        }
    }
    static class ToadMoveControl extends MoveControl {
        private final ToadEntity toad;
        private double nextJumpSpeed;

        public ToadMoveControl(ToadEntity toad) {
            super(toad);
            this.toad = toad;
        }

        public void tick() {
            if (this.toad.onGround && !this.toad.jumping && !((ToadJumpControl)this.toad.jumpControl).wantJump()) {
                this.toad.setSpeedModifier(0.0D);
            } else if (this.hasWanted()) {
                this.toad.setSpeedModifier(this.nextJumpSpeed);
            }

            super.tick();
        }

        /**
         * Sets the speed and location to move to
         */
        public void setWantedPosition(double pX, double pY, double pZ, double pSpeed) {
            if (this.toad.isInWater()) {
                pSpeed = 1.5D;
            }

            super.setWantedPosition(pX, pY, pZ, pSpeed);
            if (pSpeed > 0.0D) {
                this.nextJumpSpeed = pSpeed;
            }

        }
    }

}
