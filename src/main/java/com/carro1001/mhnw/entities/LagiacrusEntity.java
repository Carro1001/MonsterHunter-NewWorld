package com.carro1001.mhnw.entities;

import com.carro1001.mhnw.entities.ai.MonsterAggressionStateGoal;
import com.carro1001.mhnw.entities.ai.MonsterRandomSwim;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.FollowBoatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.fluids.FluidType;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.core.animation.RawAnimation;

import static com.carro1001.mhnw.utils.MHNWReferences.LAGIACRUZ;

public class LagiacrusEntity extends Monster {

    public LagiacrusEntity(EntityType<? extends Monster> p_27557_, Level p_27558_) {
        super(p_27557_, p_27558_);
        this.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
        this.moveControl = new SmoothSwimmingMoveControl(this, 85, 10, 0.1F, 0.5F, false);
        this.lookControl = new SmoothSwimmingLookControl(this, 20);
        this.name = LAGIACRUZ;
        this.setMaxUpStep(1.0F);
    }

    protected RawAnimation getIdleAnimation(){
        return RawAnimation.begin().thenLoop("animation."+name+"."+ (isInWater() ? "swim_idle":"idle"));
    }

    protected RawAnimation getMovementAnimation() {
        return RawAnimation.begin().thenLoop("animation."+name+"."+ (isInWater() ? "swim":"walk"));
    }

    protected void registerGoals() {
        this.goalSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true)); // This finds the closest player to target
        this.goalSelector.addGoal(1, new MonsterAggressionStateGoal(this)); // This handles the aggression state between passive, roar, and aggressive
        this.goalSelector.addGoal(3, new MonsterRandomSwim(this, 1.0D, 120));
        this.goalSelector.addGoal(4, new RandomStrollGoal(this, 0.7D){
            @Override
            public boolean canUse() {
                return super.canUse() && !mob.isInWater();
            }

            @Override
            public boolean canContinueToUse() {
                return super.canContinueToUse() && !mob.isInWater();
            }

            @Override
            public void start() {
                super.start();
                setWalking(true);
            }

            @Override
            public void stop() {
                super.stop();
                setWalking(false);
            }
        });
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(8, new FollowBoatGoal(this));

    }
    protected PathNavigation createNavigation(Level pLevel) {
        return new AmphibiousPathNavigation(this, pLevel);
    }
    public void travel(Vec3 pTravelVector) {
        if (this.isControlledByLocalInstance() && this.isInWater()) {
            this.moveRelative(this.getSpeed(), pTravelVector);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.9D));
        } else {
            super.travel(pTravelVector);
        }

    }

    @Override
    public boolean isPushedByFluid(FluidType type) {
        return type != ForgeMod.WATER_TYPE.get() && super.isPushedByFluid(type);
    }

    @Override
    public @NotNull MobType getMobType() {
        return MobType.WATER;
    }

    @Override
    public boolean canDrownInFluidType(FluidType type) {
        return type != ForgeMod.WATER_TYPE.get() && super.canDrownInFluidType(type);
    }
    public boolean canBreatheUnderwater() {
        return true;
    }

    public boolean isPushedByFluid() {
        return false;
    }
}
