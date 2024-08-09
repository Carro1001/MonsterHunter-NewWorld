package com.carro1001.mhnw.entities;

import com.carro1001.mhnw.entities.ai.MonsterAggressionStateGoal;
import com.carro1001.mhnw.entities.ai.MonsterRandomSwim;
import com.carro1001.mhnw.utils.MathHelpers;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.FollowBoatGoal;
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

public class LagiacrusEntity extends LargeMonster {

    //START of necessary IK shit
    public Vec3 rightRefPoint;
    public Vec3 rightRefOffset = new Vec3(1, 0, 0);

    public Vec3 leftRefPoint;
    public Vec3 leftRefOffset = new Vec3(-1, 0, 0);

    public Vec3 upRefPoint;
    public Vec3 upRefOffset = new Vec3(0, -1, 0);

    public Vec3 downRefPoint;
    public Vec3 downRefOffset = new Vec3(0, 1, 0);



    public Vec3 tail0Point;
    public Vec3 tail1Point;
    public Vec3 tail2Point;
    public Vec3 tail3Point;
    public Vec3 tail4Point;
    public Vec3 tail5Point;


    //Offset to the points relative to their parent point
    public Vec3 tail0Offset = new Vec3(0.0, 0.0, 1);
    public Vec3 tail1Offset = new Vec3(0.0, 0.0, 1);
    //technically the second segment's bone position offset, but affects the segment before it
    public Vec3 tail2Offset = new Vec3(0.0, 0.0, 1);
    public Vec3 tail3Offset = new Vec3(0.0, 0.0, 1);
    public Vec3 tail4Offset = new Vec3(0.0, 0.0, 1);
    public Vec3 tail5Offset = new Vec3(0.0, 0.0, 1);
    //x = side to side offset
    //y = vert offset
    //z = fore to back offset(pos is back)

    //END of necessary IK shit


    public LagiacrusEntity(EntityType<? extends LargeMonster> p_27557_, Level p_27558_) {
        super(p_27557_, p_27558_);
        this.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
        this.moveControl = new SmoothSwimmingMoveControl(this, 85, 4, 0.1F, 0.5F, false);
        //original maxturny = 10
        this.lookControl = new SmoothSwimmingLookControl(this, 4);
        this.name = LAGIACRUZ;
        this.setMaxUpStep(1.0F);

        leftRefPoint = MathHelpers.rotateAroundCenterFlatDeg(this.position(), this.position().subtract(leftRefOffset), (double) -this.getYRot());
        rightRefPoint = MathHelpers.rotateAroundCenterFlatDeg(this.position(), this.position().subtract(rightRefOffset), (double) -this.getYRot());
        upRefPoint = MathHelpers.rotateAroundCenterFlatDeg(this.position(), this.position().subtract(upRefOffset), (double) -this.getYRot());
        downRefPoint = MathHelpers.rotateAroundCenterFlatDeg(this.position(), this.position().subtract(downRefOffset), (double) -this.getYRot());

        tail0Point = MathHelpers.rotateAroundCenterFlatDeg(this.position(), this.position().subtract(tail0Offset), (double) -this.getYRot());
        tail1Point = MathHelpers.rotateAroundCenterFlatDeg(tail0Point, tail0Point.subtract(tail1Offset), (double) -this.getYRot());
        tail2Point = MathHelpers.rotateAroundCenterFlatDeg(tail1Point, tail1Point.subtract(tail2Offset), (double) -this.getYRot());
        tail3Point = MathHelpers.rotateAroundCenterFlatDeg(tail2Point, tail2Point.subtract(tail3Offset), (double) -this.getYRot());
        tail4Point = MathHelpers.rotateAroundCenterFlatDeg(tail3Point, tail3Point.subtract(tail4Offset), (double) -this.getYRot());
        tail5Point = MathHelpers.rotateAroundCenterFlatDeg(tail4Point, tail4Point.subtract(tail5Offset), (double) -this.getYRot());

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
        this.goalSelector.addGoal(3, new MonsterRandomSwim(this, 1.0D, 1));
        //120
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
            }

            @Override
            public void stop() {
                super.stop();
            }
        });
        //this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 6.0F));
        //this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
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

    public void tick() {
        //START of IK
        //the entity rotations must be negativized because we want the points to be transformed relative to the entity
        tail0Point = MathHelpers.rotateAroundCenter3dDeg(this.position(), this.position().subtract(tail0Offset), -this.getYRot(), -this.getXRot());
        tail1Point = MathHelpers.rotateAroundCenter3dDeg(tail0Point, tail0Point.subtract(tail1Offset), -MathHelpers.angleTo(tail0Point, tail1Point).y, -MathHelpers.angleTo(tail0Point, tail1Point).x);
        tail2Point = MathHelpers.rotateAroundCenter3dDeg(tail1Point, tail1Point.subtract(tail2Offset), -MathHelpers.angleTo(tail1Point, tail2Point).y, -MathHelpers.angleTo(tail1Point, tail2Point).x);
        tail3Point = MathHelpers.rotateAroundCenter3dDeg(tail2Point, tail2Point.subtract(tail3Offset), -MathHelpers.angleTo(tail2Point, tail3Point).y, -MathHelpers.angleTo(tail2Point, tail3Point).x);
        tail4Point = MathHelpers.rotateAroundCenter3dDeg(tail3Point, tail3Point.subtract(tail4Offset), -MathHelpers.angleTo(tail3Point, tail4Point).y, -MathHelpers.angleTo(tail3Point, tail4Point).x);
        tail5Point = MathHelpers.rotateAroundCenter3dDeg(tail4Point, tail4Point.subtract(tail5Offset), -MathHelpers.angleTo(tail4Point, tail5Point).y, -MathHelpers.angleTo(tail4Point, tail5Point).x);


        //side refs don't move vertically
        leftRefPoint = MathHelpers.rotateAroundCenterFlatDeg(this.position(), this.position().subtract(leftRefOffset), (double) -this.getYRot());
        rightRefPoint = MathHelpers.rotateAroundCenterFlatDeg(this.position(), this.position().subtract(rightRefOffset), (double) -this.getYRot());
        upRefPoint = MathHelpers.rotateAroundCenterFlatDeg(this.position(), this.position().subtract(upRefOffset), (double) -this.getYRot());
        downRefPoint = MathHelpers.rotateAroundCenterFlatDeg(this.position(), this.position().subtract(downRefOffset), (double) -this.getYRot());
        //END of IK

        super.tick();
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
