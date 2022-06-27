package com.carro1001.mhnw.entities.rathian;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.entity.PartEntity;

import java.util.List;

public class RathianPart extends PartEntity<RathianEntity> {
    private final EntityDimensions size;
    public float scale = 1;

    public RathianPart(RathianEntity parent, float sizeX, float sizeY) {
        super(parent);
        this.size = EntityDimensions.scalable(sizeX, sizeY);
        this.refreshDimensions();
    }

    public RathianPart(RathianEntity parent, EntityDimensions size) {
        super(parent);
        this.size = size;
    }

    protected void collideWithNearbyEntities() {
        List<Entity> entities = this.level.getEntities(this, this.getBoundingBox().expandTowards(0.20000000298023224D, 0.0D, 0.20000000298023224D));
        Entity parent = this.getParent();
        if (parent != null) {
            entities.stream().filter(entity -> entity != parent && !(entity instanceof RathianPart && ((RathianPart) entity).getParent() == parent) && entity.isPushable()).forEach(entity -> entity.push(parent));

        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if(level.isClientSide && this.getParent() != null){
            //LOG
        }
        return this.getParent() == null ? InteractionResult.PASS : this.getParent().mobInteract(player, hand);
    }


    protected void collideWithEntity(Entity entityIn) {
        entityIn.push(this);
    }

    public boolean isPickable() {
        return true;
    }

    public boolean hurt(DamageSource source, float amount) {
        if(level.isClientSide && this.getParent() != null && !this.getParent().isInvulnerableTo(source)){
            //LOG
        }
        return !this.isInvulnerableTo(source) && this.getParent().attackEntityPartFrom(this, source, amount);
    }

    public boolean is(Entity entityIn) {
        return this == entityIn || this.getParent() == entityIn;
    }

    public Packet<?> getAddEntityPacket() {
        throw new UnsupportedOperationException();
    }

    public EntityDimensions getDimensions(Pose poseIn) {
        return this.size.scale(scale);
    }

    @Override
    protected void defineSynchedData() {

    }

    public void tick(){
        super.tick();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {

    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {

    }
}
