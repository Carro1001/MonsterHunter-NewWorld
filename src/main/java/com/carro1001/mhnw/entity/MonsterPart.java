package com.carro1001.mhnw.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.neoforged.neoforge.entity.PartEntity;

/**
 * One named hurtbox of a large monster.
 *
 * <p>A part is <em>not</em> a living entity: it has no health of its own and never dies. It only
 * forwards damage to its parent, which owns health, mitigation, attribution, death and loot
 * (handoff section 4.1). NeoForge registers/unregisters parts in the server and client id maps
 * automatically from {@link Entity#isMultipartEntity()}, and {@code Level#getEntities} scans them,
 * so melee picking, projectiles and area damage all reach this class without any custom packet.
 *
 * <p>Local frame used by {@link GreatIzuchi#positionParts()}: +X = the monster's left,
 * +Y = up, +Z = the direction it faces. Units are blocks. See {@link GreatIzuchi} for the
 * model-to-world derivation.
 */
public class MonsterPart extends PartEntity<GreatIzuchi> {
    public final String partName;
    private final EntityDimensions size;
    /** Centre of this part in the parent's local frame (left, up, forward), in blocks. */
    public final double localLeft, localUp, localForward;

    public MonsterPart(GreatIzuchi parent, String partName, float width, float height,
                       double localLeft, double localUp, double localForward) {
        super(parent);
        this.partName = partName;
        this.size = EntityDimensions.scalable(width, height);
        this.localLeft = localLeft;
        this.localUp = localUp;
        this.localForward = localForward;
        refreshDimensions();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    /**
     * Damage dealt to a part is the parent's damage. The parent applies its own de-duplication so
     * that one area effect touching several parts still costs exactly one hit (handoff A03/A06).
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        return !isInvulnerableTo(source) && getParent().hurt(source, amount);
    }

    /** Makes the parent and all of its parts count as "the same entity" for targeting checks. */
    @Override
    public boolean is(Entity entity) {
        return this == entity || getParent() == entity;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return this.size;
    }

    /** Half the height, so {@link #positionAt} can be given a centre rather than a foot position. */
    public double halfHeight() {
        return this.size.height() / 2.0;
    }
}
