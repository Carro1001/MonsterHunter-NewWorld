package com.carro1001.mhnw.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.entity.PartEntity;
import net.neoforged.neoforge.fluids.FluidType;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * P5a movement baseline: native amphibious movement and bounded pursuit, with no attacks.
 * Parent health/death and native multipart tracking follow the existing concrete species.
 * The preserved asset has no death clip; the renderer keeps vanilla's death rotation/removal.
 */
public class Lagiacrus extends Monster implements GeoEntity {
    // Provisional adult footprint and balance, pending live fitting (not geometry-pivot values).
    public static final float BODY_WIDTH = 2.0F;
    public static final float BODY_HEIGHT = 1.5F;
    public static final double MAX_HEALTH = 80.0D;
    public static final double MOVE_SPEED = 0.25D;
    public static final double FOLLOW_RANGE = 24.0D;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.lagiacrus.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.lagiacrus.walk");
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop("animation.lagiacrus.run");
    private static final RawAnimation SWIM_IDLE = RawAnimation.begin().thenLoop("animation.lagiacrus.swim_idle");
    private static final RawAnimation SWIM = RawAnimation.begin().thenLoop("animation.lagiacrus.swim");
    private static final RawAnimation SWIM_FAST = RawAnimation.begin().thenLoop("animation.lagiacrus.swim_fast");

    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);
    private final MonsterPart[] parts;
    // Assigned by registerGoals during server Mob construction; absent on clients.
    private LagiacrusPursuitGoal pursuitGoal;
    private DamageSource lastDamageSource;
    private int lastDamageTick = -1;

    public Lagiacrus(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        setPathfindingMalus(PathType.WATER, 0.0F);
        // One controller/navigation pair on both sides of a shoreline. Native axolotl arrangement,
        // with land speed at the species attribute and no automatic upward buoyancy while idle.
        this.moveControl = new SmoothSwimmingMoveControl(this, 85, 10, 0.1F, 1.0F, false);
        // Match the native swimming controls: ordinary LookControl resets swim pitch every tick.
        this.lookControl = new SmoothSwimmingLookControl(this, 20);

        // ponytail: provisional fixed envelopes; replace with live range-midpoint measurements
        // when fitting the model. These are design estimates, NOT raw pivots or solved bones.
        // Seven preserved locator names, in stable head-to-tail order. The root covers the body.
        // Centres in blocks from entity feet: +left, +up, +forward. yBodyRot=0 faces world +Z;
        // yaw=90 faces -X. No pitch/animation transform is applied, on land OR in water.
        this.parts = new MonsterPart[] {
                //                         name           width height left  up    forward
                new MonsterPart(this, "jawHitbox",         1.2F, 1.0F, 0.0, 1.0,  3.6),
                new MonsterPart(this, "neckMidHitbox",     1.4F, 1.2F, 0.0, 1.0,  2.4),
                new MonsterPart(this, "neckBaseHitbox",    1.6F, 1.4F, 0.0, 0.9,  1.2),
                new MonsterPart(this, "tailBaseHitbox",    1.6F, 1.2F, 0.0, 0.8, -1.4),
                new MonsterPart(this, "tailMidHitbox",     1.4F, 1.0F, 0.0, 0.8, -2.8),
                new MonsterPart(this, "tailLastHitbox",    1.2F, 0.8F, 0.0, 0.8, -4.0),
                new MonsterPart(this, "tailEndHitbox",     1.0F, 0.8F, 0.0, 0.8, -5.0),
        };
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, MOVE_SPEED)
                .add(Attributes.FOLLOW_RANGE, FOLLOW_RANGE)
                .add(Attributes.STEP_HEIGHT, 1.0D);
    }

    @Override
    protected void registerGoals() {
        this.pursuitGoal = new LagiacrusPursuitGoal(this);
        this.goalSelector.addGoal(1, this.pursuitGoal);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new AmphibiousPathNavigation(this, level);
    }

    @Override
    public boolean canDrownInFluidType(FluidType type) {
        return type != NeoForgeMod.WATER_TYPE.value() && super.canDrownInFluidType(type);
    }

    /** Native axolotl travel: move() retains collision/step handling; land uses vanilla travel. */
    @Override
    public void travel(Vec3 input) {
        if (isControlledByLocalInstance() && isInWater()) {
            moveRelative(getSpeed(), input);
            move(MoverType.SELF, getDeltaMovement());
            setDeltaMovement(getDeltaMovement().scale(0.9D));
        } else {
            super.travel(input);
        }
    }

    @Override
    public boolean isMultipartEntity() {
        return true;
    }

    @Override
    public PartEntity<?>[] getParts() {
        return this.parts;
    }

    public MonsterPart[] monsterParts() {
        return this.parts;
    }

    public Vec3 localToWorld(double left, double up, double forward) {
        double rad = Math.toRadians(this.yBodyRot);
        double sin = Math.sin(rad);
        double cos = Math.cos(rad);
        return new Vec3(getX() + left * cos - forward * sin,
                getY() + up, getZ() + left * sin + forward * cos);
    }

    @Override
    public void setId(int id) {
        super.setId(id);
        for (int i = 0; i < this.parts.length; i++) {
            this.parts[i].setId(id + i + 1);
        }
    }

    private void positionParts() {
        for (MonsterPart part : this.parts) {
            part.setOldPosAndRot();
            Vec3 centre = localToWorld(part.localLeft, part.localUp, part.localForward);
            part.setPos(centre.x, part.restingY(centre.y), centre.z);
        }
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        positionParts();
    }

    /** Exactly once, after super.tick(), preserving each part's interpolation history. */
    @Override
    public void tick() {
        super.tick();
        positionParts();
    }

    /** Same source-identity deduplication/fairness contract as Rathian and Rathalos. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide) {
            if (source == this.lastDamageSource && this.tickCount == this.lastDamageTick) {
                return false;
            }
            if (source != this.lastDamageSource) {
                this.invulnerableTime = 0;
            }
            this.lastDamageSource = source;
            this.lastDamageTick = this.tickCount;
        }
        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource source) {
        if (this.pursuitGoal != null) {
            this.pursuitGoal.stop();
        }
        super.die(source);
    }

    @Override
    public void onRemovedFromLevel() {
        if (this.pursuitGoal != null) {
            this.pursuitGoal.stop();
        }
        super.onRemovedFromLevel();
        // NeoForge removes the parts from its tracking/lookup maps with the parent.
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (this.pursuitGoal != null) {
            this.pursuitGoal.stop();
        }
        this.lastDamageSource = null;
        this.lastDamageTick = -1;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(7.0D, 3.0D, 7.0D);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, this::mainAnim));
    }

    private PlayState mainAnim(AnimationState<Lagiacrus> state) {
        if (isDeadOrDying()) {
            return PlayState.STOP;
        }
        if (isInWater()) {
            return state.setAndContinue(state.isMoving() ? (isAggressive() ? SWIM_FAST : SWIM) : SWIM_IDLE);
        }
        return state.setAndContinue(state.isMoving() ? (isAggressive() ? RUN : WALK) : IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animCache;
    }
}
