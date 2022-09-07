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
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;

import java.util.Random;

public class RathalosEntity extends DragonEntity {
    public boolean hover = false;
    public boolean roaring = false;
    public boolean agro = false;
    public boolean fly = false;
    public boolean tailswipe = false;

    public RathalosEntity(EntityType<? extends PathfinderMob > p_27557_, Level p_27558_) {
        super(p_27557_, p_27558_, MHNWReferences.RATHALOS);
    }

    private <E extends IAnimatable> PlayState predicate(AnimationEvent<E> event) {
        if (!event.isMoving()) {
            if (hover) {
                event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.rathalos.hover", true));
                return PlayState.CONTINUE;
            }
            if (roaring) {
                roaring = false;
                event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.rathalos.roar", false));
                return PlayState.CONTINUE;
            }
            if (agro) {
                event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.rathalos.idle_aggro", true));
                return PlayState.CONTINUE;
            }
            if (fly) {
                event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.rathalos.fly", false));
                return PlayState.CONTINUE;
            }
            if (tailswipe) {
                tailswipe = false;
                event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.rathalos.tailswipe", false));
                return PlayState.CONTINUE;
            }
            event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.rathalos.idle_normal", true));
            return PlayState.CONTINUE;
        }
        if (event.isMoving()) {
            if (agro) {
                event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.rathalos.walk_aggro", true));
                return PlayState.CONTINUE;
            }
            event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.rathalos.walk_normal", true));
            return PlayState.CONTINUE;
        }
        return PlayState.STOP;
    }
    @Override
    public void registerControllers(AnimationData data) {
        data.addAnimationController(new AnimationController<>(this, "controller", 3, this::predicate));
    }

    public static AttributeSupplier.Builder prepareAttributes() {
        return Mob.createLivingAttributes()
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.MAX_HEALTH, 10)
                .add(Attributes.FOLLOW_RANGE, 15.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ARMOR, 1.0D)
                .add(Attributes.ARMOR_TOUGHNESS,1.0D);
    }

    @Override
    public void tick() {
        if (getSpeed() > 0) {
            counter++;
            if(counter >= 20*10){
                counter = 0;
                hover = false;
                roaring = false;
                agro = false;
                fly = false;
                tailswipe = false;
                switch(new Random().nextInt(8)){
                    case 0:
                        hover = true;
                        break;
                    case 1:
                        roaring = true;
                        break;
                    case 2:
                        agro = true;
                        break;
                    case 3:
                        fly = true;
                        break;
                    case 4:
                        tailswipe = true;
                        break;
                }
            }
        }
        super.tick();
    }

}
