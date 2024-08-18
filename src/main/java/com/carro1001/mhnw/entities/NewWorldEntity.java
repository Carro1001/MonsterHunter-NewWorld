package com.carro1001.mhnw.entities;

import com.carro1001.mhnw.registration.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

public abstract class NewWorldEntity extends PathfinderMob implements GeoEntity{
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private static final EntityDataAccessor<BlockPos> HOME_POS = SynchedEntityData.defineId(NewWorldEntity.class, EntityDataSerializers.BLOCK_POS);

    //TEMP FOR TESTING
    @Override
    public boolean removeWhenFarAway(double pDistanceToClosestPlayer) {
        return pDistanceToClosestPlayer > 64;
    }
//

    protected NewWorldEntity(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.noCulling = true;
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData, @Nullable CompoundTag pDataTag) {
        this.setHomePos(this.blockPosition());
        return super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData, pDataTag);
    }

    public static AttributeSupplier.Builder prepareAttributes() {
        return Monster.createLivingAttributes()
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.MAX_HEALTH, 250)
                .add(Attributes.FOLLOW_RANGE, 15.0)
                .add(Attributes.MOVEMENT_SPEED, 0.45)
                .add(Attributes.ARMOR, 1.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, (double)0.6F)
                .add(Attributes.ATTACK_KNOCKBACK, (double)1F)
                .add(Attributes.ARMOR_TOUGHNESS,1.0D);
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

    public List<Item> getDrops(){
        return List.of(ModItems.RAW_MEAT_ITEM.get());
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(HOME_POS, BlockPos.ZERO);

    }
    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putInt("HomePosX", this.getHomePos().getX());
        pCompound.putInt("HomePosY", this.getHomePos().getY());
        pCompound.putInt("HomePosZ", this.getHomePos().getZ());
    }
    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        int i = pCompound.getInt("HomePosX");
        int j = pCompound.getInt("HomePosY");
        int k = pCompound.getInt("HomePosZ");
        this.setHomePos(new BlockPos(i, j, k));
    }

    public void setHomePos(BlockPos pHomePos) {
        this.entityData.set(HOME_POS, pHomePos);
    }

    BlockPos getHomePos() {
        return this.entityData.get(HOME_POS);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

}
