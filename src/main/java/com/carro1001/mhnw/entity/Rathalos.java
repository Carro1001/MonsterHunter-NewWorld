package com.carro1001.mhnw.entity;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Rathalos, ground-only, the second large wyvern (handoff section 5, P4). Same shape as
 * {@link Rathian} for the same reasons, with one difference worth naming: Rathalos is genuinely
 * a flying wyvern, more so than Rathian, so a ground-only slice undersells it more than Rathian's
 * did. It is still the correct scope for this pass: flight for this family is explicitly deferred
 * within Rathian's own packet in the handoff's text, and nothing about that changes for Rathalos.
 *
 * <h2>This species' attack clips are worse off than Rathian's</h2>
 * Rathian's attacks were merely untrustworthy to solve offline; every bone they reference actually
 * exists in the geometry. Rathalos's four melee attack clips ({@code attack_claw_scratch},
 * {@code attack_bite}, {@code attack_airsweep}, {@code attack_air_fireball}) each reference 14 to
 * 17 bone names (wing membrane and talon bones, mostly) that do not exist anywhere in this
 * species' own geometry at all, exactly the specific example the handoff's own audit named
 * (section 6.1, giving {@code rightwinglimb1} and {@code leftFoot2} as instances). That is not a
 * measurement problem the runtime bone probe can solve the way Great Izuchi's claw was fixed: there
 * is no bone to measure. It needs an actual retargeting pass against the real skeleton in a model
 * editor, or a new authored clip, before any attack presentation is possible. See
 * {@code docs/DEFERRED.md}.
 *
 * <p>Until then: ordinary vanilla {@link MeleeAttackGoal}, same interim {@link Rathian} and
 * {@link Izuchi} use for the same reason.
 *
 * <h2>Hurtboxes</h2>
 * Offline-solved from {@code idle_normal}, same trustworthy channel category (plain constants and
 * single sine waves) that validated well for Great Izuchi's tail; not yet checked live. Part sizes
 * come from the archived hitbox profile that is actually, correctly, named and pathed for this
 * species (unlike Rathian's, which pointed at the wrong file), so these are the real authored
 * values, not a borrowed approximation.
 */
public class Rathalos extends Monster implements GeoEntity {

    // Provisional balance constants, not yet tuned.
    public static final double MAX_HEALTH = 85.0D;
    public static final double MOVE_SPEED = 0.24D;
    public static final double ATTACK_DAMAGE = 6.0D;
    public static final double FOLLOW_RANGE = 28.0D;

    public static final float BODY_WIDTH = 2.2F;
    public static final float BODY_HEIGHT = 3.6F;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.rathalos.idle_normal");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.rathalos.walk_normal");
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop("animation.rathalos.walk_aggro");
    private static final RawAnimation DEATH = RawAnimation.begin().thenPlayAndHold("animation.rathalos.death");

    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);
    private final MonsterPart[] parts;

    /** Same guard and the same caveat as {@link Rathian#hurt}; see that copy's doc comment. */
    private DamageSource lastDamageSource;
    private int lastDamageTick = -1;

    public Rathalos(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        // Offline-solved from idle_normal; sizes from the archived hitbox profile genuinely
        // authored for this species. See the class doc for what is and is not verified.
        this.parts = new MonsterPart[] {
                //              name          width height  left    up      forward
                new MonsterPart(this, "torso",  2.1F, 2.1F,  0.08D, 2.35D,  2.31D),
                new MonsterPart(this, "neck",   1.75F, 1.75F, 0.03D, 3.14D,  4.94D),
                new MonsterPart(this, "head",   2.0F, 2.0F,  0.32D, 3.59D,  7.49D),
                new MonsterPart(this, "tail_base", 1.75F, 1.6F, 0.00D, 3.13D, -2.24D),
                new MonsterPart(this, "tail_mid",  1.6F, 1.3F, -0.11D, 3.88D, -4.31D),
                new MonsterPart(this, "tail_end",  2.0F, 2.0F, -0.50D, 4.05D, -6.26D),
        };
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, MOVE_SPEED)
                .add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE)
                .add(Attributes.FOLLOW_RANGE, FOLLOW_RANGE)
                .add(Attributes.STEP_HEIGHT, 1.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    // ---------------------------------------------------------------- multipart

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

    public MonsterPart part(String name) {
        for (MonsterPart p : this.parts) {
            if (p.partName.equals(name)) {
                return p;
            }
        }
        throw new IllegalArgumentException("no such part: " + name);
    }

    public Vec3 localToWorld(double left, double up, double forward) {
        double rad = Math.toRadians(this.yBodyRot);
        double sin = Math.sin(rad);
        double cos = Math.cos(rad);
        return new Vec3(
                getX() + left * cos - forward * sin,
                getY() + up,
                getZ() + left * sin + forward * cos);
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
            part.setPos(centre.x, centre.y - part.halfHeight(), centre.z);
        }
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        positionParts();
    }

    @Override
    public void tick() {
        super.tick();
        positionParts();
    }

    @Override
    public void aiStep() {
        super.aiStep();
        positionParts();
    }

    /** Same contract and the same fairness correction as {@link Rathian#hurt}. */
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

    // ---------------------------------------------------------------- presentation

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 16384.0D;
    }

    /** Same fix, same reason, as {@link Rathian#getBoundingBoxForCulling}. */
    @Override
    public net.minecraft.world.phys.AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(9.0D, 5.0D, 9.0D);
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, this::mainAnim));
    }

    private PlayState mainAnim(AnimationState<Rathalos> state) {
        if (isDeadOrDying()) {
            return state.setAndContinue(DEATH);
        }
        if (state.isMoving()) {
            return state.setAndContinue(isAggressive() ? RUN : WALK);
        }
        return state.setAndContinue(IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animCache;
    }
}
