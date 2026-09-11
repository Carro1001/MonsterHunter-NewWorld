package com.carro1001.mhnw.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.EatBlockGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
 * The P3 proof of reuse: a passive herbivore, not another combat monster.
 *
 * <p>Deliberately ordinary. A single fitted hurtbox (the whole entity's own bounding box), normal
 * Minecraft health/damage/death, and stock vanilla goals for all of its behaviour. None of the
 * multipart, attack-timeline or attack-profile machinery built for Great Izuchi is used here,
 * because none of it is needed: section 4.2 says a single fitted region is fine for a small
 * creature, and section 4.1 says ordinary Minecraft facilities come before inventing replacements.
 *
 * <p>{@link Animal} rather than {@link PathfinderMob} directly, purely so {@link EatBlockGoal}
 * (used for the grass-eating flavour the {@code eat} clip was authored for) has the convenience
 * base it expects. Breeding is not implemented: {@link #getBreedOffspring} throws, and nothing
 * calls it, because {@link #isFood} always returns false.
 *
 * <h2>Behaviour: roam, flee, defend</h2>
 * {@link PanicGoal} and the retaliation goal both react to being hurt, and Panic sits at a higher
 * priority and shares its move flag, so in practice a hurt Aptonoth almost always flees rather than
 * fights. The weak counter-attack still exists as the "defend when provoked" fallback the handoff
 * describes, for the rarer case where panic's own conditions have already lapsed while the target
 * memory has not. This is a deliberately soft, not-strongly-enforced distinction; nothing here
 * promises Aptonoth ever really turns and fights.
 */
public class Aptonoth extends Animal implements GeoEntity {

    // Provisional balance constants, not yet tuned.
    public static final double MAX_HEALTH = 12.0D;
    public static final double MOVE_SPEED = 0.22D;
    public static final double ATTACK_DAMAGE = 1.0D;

    public static final float BODY_WIDTH = 1.3F;
    public static final float BODY_HEIGHT = 1.4F;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.aptonoth.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.aptonoth.walk");
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop("animation.aptonoth.run");
    private static final RawAnimation EAT = RawAnimation.begin().thenLoop("animation.aptonoth.eat");
    private static final RawAnimation DEATH = RawAnimation.begin().thenPlayAndHold("animation.aptonoth.death");

    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);

    /**
     * Client-side countdown driving the eat animation, the same mechanism vanilla's own grazing
     * animals use (see {@code Sheep.eatAnimationTick}): the server's {@link EatBlockGoal} fires a
     * one-shot entity event on {@code start()}, and each tracking client counts its own copy down
     * from there. This is a cosmetic idle detail, not synchronized combat state, so a client that
     * starts tracking mid-graze simply does not see that particular cycle's animation; it is not
     * the "join in progress must see the current action" contract section 4.4 requires of combat.
     */
    private int eatAnimationTicks;

    public Aptonoth(EntityType<? extends Animal> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, MOVE_SPEED)
                .add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.5D));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(3, new EatBlockGoal(this));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));

        // Never seeks a fight; HurtByTargetGoal only means it can retaliate against whoever
        // actually hit it, and only while Panic is not itself already handling that same trigger.
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Override
    public net.minecraft.world.entity.AgeableMob getBreedOffspring(
            net.minecraft.server.level.ServerLevel level, net.minecraft.world.entity.AgeableMob other) {
        throw new UnsupportedOperationException("Aptonoth does not breed");
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 10) {
            this.eatAnimationTicks = 40;
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    public void aiStep() {
        if (level().isClientSide && this.eatAnimationTicks > 0) {
            this.eatAnimationTicks--;
        }
        super.aiStep();
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 4096.0D;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 6, this::mainAnim));
    }

    private PlayState mainAnim(AnimationState<Aptonoth> state) {
        if (isDeadOrDying()) {
            return state.setAndContinue(DEATH);
        }
        if (this.eatAnimationTicks > 0) {
            return state.setAndContinue(EAT);
        }
        if (state.isMoving()) {
            LivingEntity target = getTarget();
            boolean fleeingOrChasing = target != null || getLastHurtByMob() != null;
            return state.setAndContinue(fleeingOrChasing ? RUN : WALK);
        }
        return state.setAndContinue(IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animCache;
    }
}
