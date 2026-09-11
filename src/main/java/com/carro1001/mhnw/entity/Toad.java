package com.carro1001.mhnw.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Endemic life: a stationary hazard, not a creature that hunts or flees. It proves variant
 * persistence and a bounded status effect without any multipart handling (handoff section 5, P3).
 *
 * <p>Deliberately has no target selector at all: it never picks a fight and never runs from one.
 * Its one behaviour is {@link ToadFuseGoal}, which telegraphs then releases a small area effect
 * when something gets close or hurts it, then goes on cooldown. See that class for the timeline
 * and for why each variant's effect is only a labelled provisional stand-in.
 */
public class Toad extends PathfinderMob implements GeoEntity {

    public static final double MAX_HEALTH = 4.0D;
    public static final float BODY_WIDTH = 0.8F;
    public static final float BODY_HEIGHT = 0.6F;

    /**
     * Which cloud this toad releases, and which of the four preserved textures it wears. The
     * variant is chosen once at spawn and persists across save/reload like any other saved
     * gameplay fact (unlike Great Izuchi's transient combat state, this is not transient).
     */
    public enum Variant {
        POISON("poisontoad"),
        SLEEP("sleeptoad"),
        PARALYSIS("paratoad"),
        BLAST("blastoad");

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
            SynchedEntityData.defineId(Toad.class, EntityDataSerializers.BYTE);
    /** Client presentation only: whether the fuse telegraph is currently playing. */
    private static final EntityDataAccessor<Boolean> DATA_FUSING =
            SynchedEntityData.defineId(Toad.class, EntityDataSerializers.BOOLEAN);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.toad.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.toad.walk");
    private static final RawAnimation FUSE = RawAnimation.begin().thenLoop("animation.toad.fuse");

    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);

    /** Set by {@link #hurt}, read and cleared by {@link ToadFuseGoal}. Transient, not saved. */
    boolean provoked;

    public Toad(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, 0.15D);
    }

    @Override
    public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(
            net.minecraft.world.level.ServerLevelAccessor level,
            net.minecraft.world.DifficultyInstance difficulty,
            net.minecraft.world.entity.MobSpawnType spawnType,
            net.minecraft.world.entity.SpawnGroupData groupData) {
        setVariant(Variant.byId(this.random.nextInt(Variant.values().length)));
        return super.finalizeSpawn(level, difficulty, spawnType, groupData);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new ToadFuseGoal(this));
        // Slow and infrequent: real toads mostly sit still. WaterAvoidingRandomStrollGoal's
        // interval parameter thins out how often it even tries to wander.
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6D, 0.02F));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        // No target selector at all: this creature never fights and never flees, it only
        // releases its cloud in place (handoff: "non-pursuing endemic life").
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_VARIANT, (byte) 0);
        builder.define(DATA_FUSING, false);
    }

    public Variant getVariant() {
        return Variant.byId(this.entityData.get(DATA_VARIANT));
    }

    public void setVariant(Variant variant) {
        this.entityData.set(DATA_VARIANT, (byte) variant.ordinal());
    }

    public boolean isFusing() {
        return this.entityData.get(DATA_FUSING);
    }

    void setFusing(boolean fusing) {
        this.entityData.set(DATA_FUSING, fusing);
    }

    /** Being hit counts as provocation, same as proximity; either can start the fuse. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide) {
            this.provoked = true;
        }
        return hurt;
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

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, this::mainAnim));
    }

    private PlayState mainAnim(AnimationState<Toad> state) {
        if (isFusing()) {
            return state.setAndContinue(FUSE);
        }
        return state.setAndContinue(state.isMoving() ? WALK : IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animCache;
    }
}
