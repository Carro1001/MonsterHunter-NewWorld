package com.carro1001.mhnw.entities.rathalos;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.IAnimationTickable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

import java.util.Random;

public class
RathalosEntity extends PathfinderMob implements IAnimatable, IAnimationTickable {
    private AnimationFactory factory = new AnimationFactory(this);
    public boolean hover = false;
    public boolean roaring = false;
    public boolean agro = false;
    public boolean fly = false;
    public boolean tailswipe = false;

    public int counter = 0;
    public RathalosEntity(EntityType<? extends PathfinderMob > p_27557_, Level p_27558_) {
        super(p_27557_, p_27558_);
        this.noCulling = true;
    }
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.2D));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.addBehaviourGoals();
    }
    protected void addBehaviourGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
    }
    private <E extends IAnimatable> PlayState predicate(AnimationEvent<E> event) {

        if (!event.isMoving() && this.getSpeed() <= 0) {
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
                event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.rathalos.idle_aggro", true));
                return PlayState.CONTINUE;
            }
            event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.rathalos.walk_aggro", true));
            return PlayState.CONTINUE;
        }

        event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.rathalos.idle_normal", true));
        return PlayState.CONTINUE;
    }
    @Override
    public void registerControllers(AnimationData data) {
        data.addAnimationController(new AnimationController<>(this, "controller", 6, this::predicate));
    }

    @Override
    public AnimationFactory getFactory() {
        return this.factory;
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
        super.tick();
    }

    @Override
    public int tickTimer() {
        return tickCount;
    }

}
