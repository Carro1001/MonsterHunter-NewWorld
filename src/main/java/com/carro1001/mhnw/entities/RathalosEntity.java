package com.carro1001.mhnw.entities;

import com.carro1001.mhnw.utils.MHNWReferences;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

public class RathalosEntity extends DragonEntity {

    public RathalosEntity(EntityType<? extends PathfinderMob > p_27557_, Level p_27558_) {
        super(p_27557_, p_27558_, MHNWReferences.RATHALOS);
    }
    private static final RawAnimation FLY = RawAnimation.begin().thenLoop("animation.rathalos.fly");
    private static final RawAnimation HOVER = RawAnimation.begin().thenLoop("animation.rathalos.hover");
    private static final RawAnimation TAIL_SWIPE = RawAnimation.begin().thenPlay("animation.rathalos.tailswipe");
    private static final RawAnimation FIREBALL = RawAnimation.begin().thenPlay("animation.rathalos.ground_fireball");
    private static final RawAnimation WALK_AGRO = RawAnimation.begin().thenLoop("animation.rathalos.walk_aggro");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.rathalos.walk_normal");
    private static final RawAnimation ROAR = RawAnimation.begin().thenPlayXTimes("animation.rathalos.roar", 1);
    private static final RawAnimation IDLE_AGRO = RawAnimation.begin().thenLoop("animation.rathalos.idle_aggro");
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.rathalos.idle_normal");

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(
                new AnimationController<>(this, "walkController", 1, this::poseBody)
        );
    }

    // Create the animation handler for the body segment
    protected PlayState poseBody(AnimationState<RathalosEntity> state) {
        if(state.getAnimatable().isFallFlying()){
            if (state.isMoving()) {
                state.getController().setAnimation(FLY);
            }else{
                state.getController().setAnimation(HOVER);
            }
            return PlayState.CONTINUE;
        }
        if (state.isMoving()) {
            if(hasRoared()){
                state.getController().setAnimation(WALK_AGRO);
            }else{
                state.getController().setAnimation(WALK);
            }
        }else if(!state.isMoving() && this.getSpeed() <= 0){
            if(hasRoared()){
                state.getController().setAnimation(IDLE_AGRO);
            }else{
                state.getController().setAnimation(IDLE);
            }
        }
        return PlayState.CONTINUE;
    }

    // Create the animation handler for each hand
    protected PlayState predicateRoarPose(AnimationState<RathalosEntity> state) {
        if(this.hasRoared()){
            return PlayState.STOP;
        }
        if(this.shouldRoar()){
            state.setAnimation(ROAR);
            setHasRoared(true);
            return PlayState.CONTINUE;
        }

        return PlayState.STOP;
    }
    protected PlayState predicateFireBallPose(AnimationState<RathalosEntity> state) {
        if(state.getAnimatable().isFallFlying()){
            return state.setAndContinue(FIREBALL);
        }
        return state.setAndContinue(FIREBALL);
    }
    public static AttributeSupplier.Builder prepareAttributes() {
        return Mob.createLivingAttributes()
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.MAX_HEALTH, 10)
                .add(Attributes.FOLLOW_RANGE, 128)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ARMOR, 1.0D)
                .add(Attributes.ARMOR_TOUGHNESS,1.0D);
    }

}
