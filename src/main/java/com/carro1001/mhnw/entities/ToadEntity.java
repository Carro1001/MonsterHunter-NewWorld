package com.carro1001.mhnw.entities;

import com.carro1001.mhnw.entities.ai.ToadSwellGoal;
import com.carro1001.mhnw.entities.ai.ToadWalkGoal;
import com.carro1001.mhnw.registration.ModEntities;
import com.carro1001.mhnw.setup.ModConfig;
import com.carro1001.mhnw.utils.MHNWReferences;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.animal.Bucketable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Random;

import static com.carro1001.mhnw.registration.ModParticle.*;

public class ToadEntity extends PathfinderMob implements Bucketable, GeoEntity {
    private static final EntityDataAccessor<Integer> TYPE = SynchedEntityData.defineId(ToadEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> TYPEASSIGNED = SynchedEntityData.defineId(ToadEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> FROM_BUCKET = SynchedEntityData.defineId(ToadEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Integer> DATA_SWELL_DIR = SynchedEntityData.defineId(ToadEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_IS_IGNITED = SynchedEntityData.defineId(ToadEntity.class, EntityDataSerializers.BOOLEAN);
    public boolean walking = false;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private int oldSwell;
    private int swell;
    private int maxSwell = 34;
    private int explosionRadius = 3;
    public int counterAnim = 0;
    public ToadEntity(EntityType<? extends PathfinderMob> p_27557_, Level p_27558_) {
        super(p_27557_, p_27558_);
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
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 2.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(3, new ToadWalkGoal(this,0.5f));

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
    public void addAdditionalSaveData(@NotNull CompoundTag pCompound) {
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
    public void readAdditionalSaveData(@NotNull CompoundTag pCompound) {
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
    public void saveToBucketTag(@NotNull ItemStack p_148833_) {
        if (this.hasCustomName()) {
            p_148833_.setHoverName(this.getCustomName());
        }
        Bucketable.saveDefaultDataToBucketTag(this, p_148833_);
        CompoundTag compoundtag = p_148833_.getOrCreateTag();
        compoundtag.putInt("BucketVariantTag", this.getTypeDir());
    }

    @Override
    public void loadFromBucketTag(@NotNull CompoundTag p_148832_) {
        Bucketable.loadDefaultDataFromBucketTag(this, p_148832_);
        if (p_148832_.contains("BucketVariantTag", 3)) {
            this.setTypeDir(p_148832_.getInt("BucketVariantTag"));
        }

    }

    public ItemStack getBucketItemStack() {
        switch(getTypeDir()){
            case 0 -> {
                return new ItemStack(ModEntities.BUCKET_POISONTOAD_ITEM.get());
            }
            case 1 -> {
                return new ItemStack(ModEntities.BUCKET_SLEEPTOAD_ITEM.get());
            }
            case 2 -> {
                return new ItemStack(ModEntities.BUCKET_PARATOAD_ITEM.get());
            }
            default -> {
                return new ItemStack(ModEntities.BUCKET_NITROTOAD_ITEM.get());
            }
        }
    }

    @Override
    public @NotNull SoundEvent getPickupSound() {
        return SoundEvents.BUCKET_FILL_FISH;
    }

    public @NotNull InteractionResult mobInteract(Player p_149155_, @NotNull InteractionHand p_149156_) {
        ItemStack itemstack = p_149155_.getItemInHand(p_149156_);
        if (itemstack.is(Items.FLINT_AND_STEEL)) {
            this.level().playSound(p_149155_, this.getX(), this.getY(), this.getZ(), SoundEvents.FLINTANDSTEEL_USE, this.getSoundSource(), 1.0F, this.random.nextFloat() * 0.4F + 0.8F);
            if (!this.level().isClientSide) {
                this.ignite();
                itemstack.hurtAndBreak(1, p_149155_, (p_32290_) -> {
                    p_32290_.broadcastBreakEvent(p_149156_);
                });
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return Bucketable.bucketMobPickup(p_149155_, p_149156_, this).orElse(super.mobInteract(p_149155_, p_149156_));
    }
    @Override
    public void tick() {
        super.tick();
        counterAnim++;
        if(counterAnim >= 20*5){
            counterAnim = 0;
            Random random1 = new Random();
            float xSpeed = (float) random1.nextDouble(0.4);
            float zSpeed = (float) random1.nextDouble(0.4);
            float ySpeed = (float) random1.nextDouble(0.7f,1.4f);
            double newXPos = (this.position().x +  (float) random1.nextDouble(-0.2f,0.3f));
            double newYPos = (this.position().y + 0.35f +  (float) random1.nextDouble(-0.2f,0.3f));
            double newZPos = (this.position().z +  (float) random1.nextDouble(-0.2f,0.3f));
            switch (getTypeDir()) {
                case 0 ->
                        Minecraft.getInstance().levelRenderer.addParticle(POISON_PARTICLE_TYPE.get(), true, newXPos, newYPos, newZPos, xSpeed, ySpeed, zSpeed);
                case 1 ->
                        Minecraft.getInstance().levelRenderer.addParticle(SLEEP_PARTICLE_TYPE.get(), true, newXPos, newYPos, newZPos, xSpeed, ySpeed, zSpeed);
                case 2 ->
                        Minecraft.getInstance().levelRenderer.addParticle(THUNDER_PARTICLE_TYPE.get(), true, newXPos, newYPos, newZPos, xSpeed, ySpeed, zSpeed);
                case 3 ->
                        Minecraft.getInstance().levelRenderer.addParticle(ICE_PARTICLE_TYPE.get(), true, newXPos, newYPos, newZPos, xSpeed, ySpeed, zSpeed);
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
                this.explode();
            }
        }
    }



    public SpawnGroupData finalizeSpawn(@NotNull ServerLevelAccessor p_149132_, @NotNull DifficultyInstance p_149133_, @NotNull MobSpawnType p_149134_, @Nullable SpawnGroupData p_149135_, @Nullable CompoundTag p_149136_) {
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
    private void explode() {
        if (!this.level().isClientSide) {
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
            AreaEffectCloud areaeffectcloud = new AreaEffectCloud(this.level(), this.getX(), this.getY(), this.getZ());
            areaeffectcloud.setRadius(2.5F);
            areaeffectcloud.setRadiusOnUse(-0.5F);
            areaeffectcloud.setWaitTime(10);
            areaeffectcloud.setDuration(areaeffectcloud.getDuration() / 2);
            areaeffectcloud.setRadiusPerTick(-areaeffectcloud.getRadius() / (float)areaeffectcloud.getDuration());
            for(MobEffectInstance mobeffectinstance : getToadEffects()) {
                areaeffectcloud.addEffect(new MobEffectInstance(mobeffectinstance));
            }
            this.level().addFreshEntity(areaeffectcloud);

        }
        if(getTypeDir()==3){
            this.level().explode(this, this.getX(), this.getY(), this.getZ(), (float)this.explosionRadius/2, Level.ExplosionInteraction.MOB);
        }
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

    private static final RawAnimation FUSE = RawAnimation.begin().thenPlay("animation.toad.fuse");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.toad.walk");
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.toad.idle");

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(
                new AnimationController<>(this, "Body", 1, this::poseBody)
        );
    }

    // Create the animation handler for the body segment
    protected PlayState poseBody(AnimationState<ToadEntity> state) {
        if (this.isIgnited()){
            state.setAnimation(FUSE);
        }
        else if (state.isMoving()) {
            state.setAnimation(WALK);
        }
        else {
            state.setAnimation(IDLE);
        }

        return PlayState.CONTINUE;
    }
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

}
