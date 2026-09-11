package com.carro1001.mhnw.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.level.Level;

/**
 * Bitterbug/godbug: small ambient life, not a monster (handoff section 2.1). The last P3 species,
 * and the odd one out of the four: its model is hand-authored Java ({@link BugModel}, ported
 * unchanged from the archived runtime at
 * {@code legacy/forge-1.20.1-runtime/.../client/models/entities/BugModel.java}), not a GeckoLib
 * asset, so this entity does not implement {@code GeoEntity} and has no animation cache. Its
 * procedural leg/antennae motion lives in {@code BugModel.setupAnim}, driven by vanilla's own
 * limb-swing/age parameters the way any hand-modeled vanilla mob's walk cycle is, not by a
 * GeckoLib controller.
 *
 * <p>The old {@code BugEntity} this succeeds gave it combat stats: 3.0 attack damage, armor and
 * armor toughness, a follow range for chasing. None of that is carried over. The handoff
 * re-describes this creature as ambient/collectible, not hostile, so it has no target selector and
 * no attack attribute; only the preserved mesh, textures and credit come forward, not the old
 * implementation's idea of what the creature does.
 */
public class Bug extends PathfinderMob {

    /** Low enough that any hit, including a bare-handed punch, kills it in one: it's meant to be
     * collected for future item crafting, not fought. */
    public static final double MAX_HEALTH = 1.0D;
    public static final float BODY_WIDTH = 0.4F;
    public static final float BODY_HEIGHT = 0.4F;

    /** How rare the golden variant is. Old code had this as a config option; kept a plain constant
     * here for now, matching every other P3 species' balance constants. */
    public static final float GODBUG_CHANCE = 0.02F;

    public enum Variant {
        BITTERBUG("bitterbug"),
        GODBUG("godbug");

        public final String textureName;

        Variant(String textureName) {
            this.textureName = textureName;
        }

        public static Variant byId(int id) {
            Variant[] values = values();
            return values[Math.floorMod(id, values.length)];
        }
    }

    private static final EntityDataAccessor<Byte> DATA_VARIANT =
            SynchedEntityData.defineId(Bug.class, EntityDataSerializers.BYTE);

    public Bug(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, 0.25D);
    }

    @Override
    public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(
            net.minecraft.world.level.ServerLevelAccessor level,
            net.minecraft.world.DifficultyInstance difficulty,
            net.minecraft.world.entity.MobSpawnType spawnType,
            net.minecraft.world.entity.SpawnGroupData groupData) {
        setVariant(this.random.nextFloat() < GODBUG_CHANCE ? Variant.GODBUG : Variant.BITTERBUG);
        return super.finalizeSpawn(level, difficulty, spawnType, groupData);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new WaterAvoidingRandomStrollGoal(this, 0.5D));
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
        // No target selector: purely ambient, never fights, never flees.
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_VARIANT, (byte) 0);
    }

    public Variant getVariant() {
        return Variant.byId(this.entityData.get(DATA_VARIANT));
    }

    public void setVariant(Variant variant) {
        this.entityData.set(DATA_VARIANT, (byte) variant.ordinal());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("Variant", (byte) getVariant().ordinal());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Variant")) {
            setVariant(Variant.byId(tag.getByte("Variant")));
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 1024.0D;
    }
}
