package com.carro1001.mhnw.entities;

import com.carro1001.mhnw.utils.MHNWReferences;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.builder.ILoopType;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;

public class RathalosEntity extends DragonEntity {

    public RathalosEntity(EntityType<? extends PathfinderMob > p_27557_, Level p_27558_) {
        super(p_27557_, p_27558_, MHNWReferences.RATHALOS);
    }

    private <E extends IAnimatable> PlayState predicate(AnimationEvent<E> event) {
        //event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.rathalos.fly", true));
        //event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.rathalos.hover", true));
        //event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.rathalos.tailswipe", false));
        //event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.rathalos.ground_fireball", false));
        if (event.isMoving() || getStateDir() == 2 ) {
            if (getTarget() != null) {
                event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.rathalos.walk_aggro", ILoopType.EDefaultLoopTypes.LOOP));
            }else{
                event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.rathalos.walk_normal", ILoopType.EDefaultLoopTypes.LOOP));
            }
        }else if(getStateDir() == 3){
            event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.rathalos.roar",  ILoopType.EDefaultLoopTypes.PLAY_ONCE));

        }else{
            if (getTarget() != null) {
                event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.rathalos.idle_aggro",  ILoopType.EDefaultLoopTypes.LOOP));
            }else{
                event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.rathalos.idle_normal",  ILoopType.EDefaultLoopTypes.LOOP));
            }
        }
        return PlayState.CONTINUE;
    }

    @Override
    public void registerControllers(AnimationData data) {
        data.addAnimationController(new AnimationController<>(this, "controller", 5, this::predicate));
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
