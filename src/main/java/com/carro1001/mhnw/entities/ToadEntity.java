package com.carro1001.mhnw.entities;

import com.carro1001.mhnw.setup.Registration;
import com.carro1001.mhnw.utils.MHNWReferences;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Bucketable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

import javax.annotation.Nullable;
import java.util.Random;

public class ToadEntity extends Mob implements Bucketable{
    private static final EntityDataAccessor<Integer> TYPE = SynchedEntityData.defineId(ToadEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> TYPEASSIGNED = SynchedEntityData.defineId(ToadEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_IS_POWERED = SynchedEntityData.defineId(ToadEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> FROM_BUCKET = SynchedEntityData.defineId(ToadEntity.class, EntityDataSerializers.BOOLEAN);

    public int counterAnim = 0;
    public ToadEntity(EntityType<? extends Mob> p_27557_, Level p_27558_) {
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

    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(TYPEASSIGNED, false);
        this.entityData.define(TYPE, 0);
        this.entityData.define(FROM_BUCKET, false);

        this.entityData.define(DATA_IS_POWERED, false);
    }
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        if (this.entityData.get(DATA_IS_POWERED)) {
            pCompound.putBoolean("powered", true);
        }
        pCompound.putBoolean("type_assign", getTypeAssignedDir());
        pCompound.putInt("type", getTypeDir());
        pCompound.putBoolean("FromBucket", this.fromBucket());

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

        this.entityData.set(DATA_IS_POWERED, pCompound.getBoolean("powered"));
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
        if (!getTypeAssignedDir()){
            Random random = new Random();
            setTypeDir(random.nextInt(4));
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
        return Bucketable.bucketMobPickup(p_149155_, p_149156_, this).orElse(super.mobInteract(p_149155_, p_149156_));
    }
    @Override
    public void tick() {
        super.tick();
        BlockPos pos = new BlockPos(this.position());
        counterAnim++;
        if(counterAnim >= 20*5){
            counterAnim = 0;
            float xSpeed = this.random.nextFloat(0.4f);
            float zSpeed = this.random.nextFloat(0.4f);
            float ySpeed = this.random.nextFloat(0.7f,1.4f);
            double newXPos = (this.position().x +  this.random.nextFloat(-0.2f,0.3f));
            double newYPos = (this.position().y + 0.35f +  this.random.nextFloat(-0.2f,0.3f));
            double newZPos = (this.position().z +  this.random.nextFloat(-0.2f,0.3f));
            switch(getTypeDir()){
                case 0:
                    Minecraft.getInstance().levelRenderer.addParticle(Registration.POISON_PARTICLE_TYPE.get(), true,newXPos,newYPos,newZPos,xSpeed,ySpeed,zSpeed);
                    break;
                case 1:
                    Minecraft.getInstance().levelRenderer.addParticle(Registration.SLEEP_PARTICLE_TYPE.get(), true,newXPos,newYPos,newZPos,xSpeed,ySpeed,zSpeed);
                    break;
                case 2:
                    Minecraft.getInstance().levelRenderer.addParticle(Registration.THUNDER_PARTICLE_TYPE.get(), true,newXPos,newYPos,newZPos,xSpeed,ySpeed,zSpeed);
                    break;
                case 3:
                    Minecraft.getInstance().levelRenderer.addParticle(Registration.ICE_PARTICLE_TYPE.get(), true,newXPos,newYPos,newZPos,xSpeed,ySpeed,zSpeed);
                    break;
            }
        }
    }
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor p_149132_, DifficultyInstance p_149133_, MobSpawnType p_149134_, @Nullable SpawnGroupData p_149135_, @Nullable CompoundTag p_149136_) {
        if (p_149134_ == MobSpawnType.BUCKET) {
            return p_149135_;
        } else {
            this.setType();
            return super.finalizeSpawn(p_149132_, p_149133_, p_149134_, p_149135_, p_149136_);
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
}
