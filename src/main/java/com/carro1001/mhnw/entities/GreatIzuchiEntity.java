package com.carro1001.mhnw.entities;

import com.carro1001.mhnw.entities.ai.RallyGoal;
import com.carro1001.mhnw.entities.ai.SleepGoal;
import com.carro1001.mhnw.entities.interfaces.IMonsterBreakablePart;
import com.carro1001.mhnw.registration.ModEntities;
import com.carro1001.mhnw.registration.ModItems;
import de.dertoaster.multihitboxlib.api.IMultipartEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;

import java.util.List;

import static com.carro1001.mhnw.utils.MHNWReferences.GREAT;
import static com.carro1001.mhnw.utils.MHNWReferences.IZUCHI;

public class GreatIzuchiEntity extends NewWorldMonsterEntity implements IMultipartEntity<NewWorldMonsterEntity>{


    public GreatIzuchiEntity(EntityType<? extends NewWorldMonsterEntity> p_27557_, Level p_27558_) {
        super(p_27557_, p_27558_);
        this.name = GREAT + "_" + IZUCHI;
        shouldRage = true;
        MonsterWeakness = List.of(Elements.THUNDER);

    }

    protected AnimationController<NewWorldMonsterEntity> getNewWorldMonsterEntityAnimationController() {
        return super.getNewWorldMonsterEntityAnimationController().setAnimationSpeed(1.5).triggerableAnim("attack_scratch", getClawAnimation())
                .triggerableAnim("tailswipe", getSwipeAnimation()).triggerableAnim("tailslam", getSlamAnimation());
    }

    protected RawAnimation getClawAnimation() {
        return RawAnimation.begin().thenPlay("animation.great_izuchi.attack_scratch");
    }
    protected RawAnimation getSwipeAnimation() {
        return RawAnimation.begin().thenPlay("animation.great_izuchi.tailswipe");
    }
    protected RawAnimation getSlamAnimation() {
        return RawAnimation.begin().thenPlay("animation.great_izuchi.tailslam");
    }


    protected void registerGoals() {
        super.registerGoals();
        IMonsterBreakablePart torseHitBox = null;
        for (IMonsterBreakablePart part: BreakableParts){
            if(part.getPartName().equals("torsoHitbox")){
                torseHitBox =part;
                break;
            }
        }
        this.goalSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Ravager.class, true));
        this.goalSelector.addGoal(3, new ClawAttack(this,1.2,torseHitBox));
        this.goalSelector.addGoal(3, new TailAttackGoal(this));
        this.goalSelector.addGoal(9, new RallyGoal(this));
        this.goalSelector.addGoal(10, new SleepGoal(this));
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData, @Nullable CompoundTag pDataTag) {

        for (int i = 0; i < 2; i++) {
            BlockPos pos = i == 0 ? getOnPos().east(4):getOnPos().west(4);

            IzuchiEntity izuchi = ModEntities.IZUCHI.get().create(pLevel.getLevel());
            izuchi.setPos(pos.getX(),pos.getY()+1,pos.getZ());
            pLevel.getLevel().addFreshEntity(izuchi);
        }
        return super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData, pDataTag);
    }

    public static AttributeSupplier.Builder prepareAttributes() {
        return NewWorldMonsterEntity.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 350)
                .add(Attributes.FOLLOW_RANGE, 15.0)
                .add(Attributes.MOVEMENT_SPEED, 0.45)
                .add(Attributes.ARMOR, 8.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, (double)0.7F)
                .add(Attributes.ATTACK_KNOCKBACK, (double)1F)
                .add(Attributes.ATTACK_DAMAGE , (double)3F)
                .add(Attributes.ARMOR_TOUGHNESS,6.0D);
    }

    public enum ATTACK_ID{
        NONE,
        TAIL_SWIPE,
        TAIL_SLAM
    }

    public List<Item> getDrops(){
        return List.of(ModItems.IZUCHI_HIDE_ITEM.get(),ModItems.IZUCHI_TAIL_ITEM.get(),
                ModItems.IZUCHI_HIDE_ITEM.get(),ModItems.IZUCHI_HIDE_ITEM.get());
    }

    public class TailAttackGoal extends MeleeAttackGoal {

        GreatIzuchiEntity monster;
        //3 sec anim
        int animTicks = 0;
        int maxTicks = 20*3;
        public TailAttackGoal (GreatIzuchiEntity monster){
            super(monster, 1.2f,true);
            this.monster = monster;
        }

        @Override
        public boolean canUse() {
            return !monster.isLimpining() && !monster.isAnimating() && monster.getTarget() != null && super.canUse();
        }

        @Override
        public void start() {
            super.start();
            int state = monster.random.nextIntBetweenInclusive(1,2);
            maxTicks = state == 1? 60 : 100;
            monster.triggerAnim("main_controller", state == 1 ? "attack_tailswipe" : "attack_tailslam");
            monster.currentAnimationTick = maxTicks;
        }


        @Override
        public boolean canContinueToUse() {
            return animTicks < maxTicks && super.canContinueToUse();
        }

        @Override
        public void tick() {
            super.tick();
            if(!monster.level().isClientSide){
                animTicks++;
            }
        }
    }

    class ClawAttack extends MeleeAttackGoal{
        private GreatIzuchiEntity greatIzuchiEntity;
        private int ticksUntilNextAttack;
        private final int attackInterval = 70;
        private boolean animatingAttack = false;
        int animTicks = 0;
        int maxTicks = 20*3;
        IMonsterBreakablePart hitboxOrigin;
        public ClawAttack(GreatIzuchiEntity pMob, double pSpeedModifier,IMonsterBreakablePart hitbox) {
            super(pMob, pSpeedModifier, true);
            this.greatIzuchiEntity = pMob;
            this.hitboxOrigin = hitbox;
        }


        @Override
        public boolean canUse() {
            return super.canUse() && !greatIzuchiEntity.isLimpining() && !greatIzuchiEntity.isAnimating();
        }

        @Override
        public boolean canContinueToUse() {
            return super.canContinueToUse() && greatIzuchiEntity.getTarget() != null && animTicks < maxTicks && greatIzuchiEntity.getTarget().isAlive();
        }


        @Override
        public void tick() {
            super.tick();
            LivingEntity livingentity = this.mob.getTarget();
            if (livingentity != null) {
                this.mob.getLookControl().setLookAt(livingentity, 5.0F, 5.0F);
                double d0 = this.mob.getPerceivedTargetDistanceSquareForMeleeAttack(livingentity);
                this.ticksUntilNextAttack = Math.max(this.ticksUntilNextAttack - 1, 0);
                if(!greatIzuchiEntity.level().isClientSide && animatingAttack){
                    if(this.ticksUntilNextAttack <= 0){
                        animatingAttack = false;
                        //wait a bit until you try and attack again if the goal stays active
                        this.ticksUntilNextAttack = this.adjustedTickDelay(attackInterval/3);
                    }
                }else if(!animatingAttack){
                    this.checkAndPerformAttack(livingentity, d0);
                }
            }
        }
        protected void checkAndPerformAttack(LivingEntity pEnemy, double pDistToEnemySqr) {
            if(hitboxOrigin.getPart().closerThan(pEnemy,this.getAttackReachSqr(pEnemy)) && this.ticksUntilNextAttack <= 0){
                //wait a until anim is over until you try and attack again, in monster hunter fashion im trying to limit hits per animation
                greatIzuchiEntity.triggerAnim("main_controller","attack_scratch");
                this.ticksUntilNextAttack = this.adjustedTickDelay(attackInterval);
                this.mob.doHurtTarget(pEnemy);
                animatingAttack = true;
            }
        }
    }

}
