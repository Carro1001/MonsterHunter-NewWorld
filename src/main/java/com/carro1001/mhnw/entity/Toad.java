package com.carro1001.mhnw.entity;

import com.carro1001.mhnw.registry.ModItems;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Bucketable;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;
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
public class Toad extends PathfinderMob implements GeoEntity, Bucketable {

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
    /** Whether this toad was released from a bucket, and so must not distance-despawn. */
    private static final EntityDataAccessor<Boolean> DATA_FROM_BUCKET =
            SynchedEntityData.defineId(Toad.class, EntityDataSerializers.BOOLEAN);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.toad.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.toad.walk");
    private static final RawAnimation FUSE = RawAnimation.begin().thenLoop("animation.toad.fuse");

    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);

    /** Set by {@link #hurt}, read and cleared by {@link ToadFuseGoal}. Transient, not saved. */
    boolean provoked;

    /**
     * The player behind the provocation that started the current fuse, if one resolved. Transient,
     * not saved, held as a uuid rather than a reference so a logout mid-fuse cannot keep a stale
     * player alive. Retained for the whole fuse -- a second hit does not steal credit -- and
     * cleared by {@link ToadFuseGoal#stop()}, so it is a provocation record, not an owner.
     *
     * <p>Only {@code BLAST} does anything with it: its explosion names that player as the causing
     * entity, which is the one path by which a deployed toad's damage can count toward that
     * player's carve eligibility, through exactly the same {@link CarveState} rule as a direct hit.
     */
    UUID provokerId;

    /** The player this toad is currently holding responsible for its fuse, if any. For tests. */
    public UUID provokerId() {
        return this.provokerId;
    }

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
        // A bucket release already knows its variant (it is a property of the bucket item), so a
        // random one here would silently overwrite it.
        if (spawnType != MobSpawnType.BUCKET) {
            setVariant(Variant.byId(this.random.nextInt(Variant.values().length)));
        }
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
        builder.define(DATA_FROM_BUCKET, false);
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

    /**
     * The only way to provoke a toad: unlike the flashbug, it does not react to mere proximity (see
     * {@link ToadFuseGoal#allowsProximityTrigger}). It cannot be killed by being hit, only by its
     * own release, which discards it directly rather than by taking damage: the hit still lands
     * (real vanilla feedback, sound and knockback included) but at zero damage, so health never
     * drops and there is nothing for a player to grind down for no reason.
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide) {
            this.provoked = true;
            if (this.provokerId == null) {
                Player provoker = CarveState.resolvePlayer(source);
                if (provoker != null) {
                    this.provokerId = provoker.getUUID();
                }
            }
        }
        return super.hurt(source, 0.0F);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("Variant", (byte) getVariant().ordinal());
        tag.putBoolean("FromBucket", fromBucket());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Variant")) {
            setVariant(Variant.byId(tag.getByte("Variant")));
        }
        setFromBucket(tag.getBoolean("FromBucket"));
    }

    // ---------------------------------------------------------------- capture and release (R2)

    /**
     * A vanilla water bucket catches a live toad; everything else keeps its existing behaviour.
     * {@code Bucketable.bucketMobPickup} owns the whole transaction -- consume the water bucket,
     * hand back the filled one through vanilla's filled-container rule, discard the entity -- so
     * nothing here does slot arithmetic, and a capture never routes through {@link #hurt}, which is
     * why catching a toad cannot light its fuse.
     */
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        return Bucketable.bucketMobPickup(player, hand, this).orElse(super.mobInteract(player, hand));
    }

    @Override
    public boolean fromBucket() {
        return this.entityData.get(DATA_FROM_BUCKET);
    }

    @Override
    public void setFromBucket(boolean fromBucket) {
        this.entityData.set(DATA_FROM_BUCKET, fromBucket);
    }

    /** Vanilla's own bucket payload (custom name, health, the no-AI style flags) plus the variant. */
    @Override
    public void saveToBucketTag(ItemStack stack) {
        Bucketable.saveDefaultDataToBucketTag(this, stack);
        CustomData.update(DataComponents.BUCKET_ENTITY_DATA, stack,
                tag -> tag.putByte("Variant", (byte) getVariant().ordinal()));
    }

    @Override
    public void loadFromBucketTag(CompoundTag tag) {
        Bucketable.loadDefaultDataFromBucketTag(this, tag);
        if (tag.contains("Variant")) {
            setVariant(Variant.byId(tag.getByte("Variant")));
        }
    }

    @Override
    public ItemStack getBucketItemStack() {
        return new ItemStack(ModItems.toadBucket(getVariant()).get());
    }

    @Override
    public SoundEvent getPickupSound() {
        return SoundEvents.BUCKET_FILL_FISH;
    }

    /** Same pair of rules vanilla's fish use, and for the same reason: a deliberately placed
     * creature must not vanish because the player walked away. */
    @Override
    public boolean requiresCustomPersistence() {
        return super.requiresCustomPersistence() || fromBucket();
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return !fromBucket() && !hasCustomName();
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
