package com.carro1001.mhnw.entities;

import com.carro1001.mhnw.entities.ai.HitboxMeeleeAttackGoal;
import com.carro1001.mhnw.entities.ai.RallyGoal;
import com.carro1001.mhnw.entities.ai.SleepGoal;
import com.carro1001.mhnw.entities.interfaces.IMonsterBreakablePart;
import com.carro1001.mhnw.registration.ModItems;
import de.dertoaster.multihitboxlib.api.IMultipartEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Vindicator;
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
        return super.getNewWorldMonsterEntityAnimationController().triggerableAnim("attack_scratch", getClawAnimation())
                .triggerableAnim("tailswipe", getSwipeAnimation()).triggerableAnim("tailslam", getSlamAnimation());
    }

    protected RawAnimation getClawAnimation() {
        return RawAnimation.begin().thenPlay("animation.great_izuchi.attack_scratch");
    }
    protected RawAnimation getSwipeAnimation() {
        return RawAnimation.begin().thenPlay("animation.great_izuchi.attack_tailswipe");
    }
    protected RawAnimation getSlamAnimation() {
        return RawAnimation.begin().thenPlay("animation.great_izuchi.attack_tailslam");
    }


    protected void registerGoals() {
        super.registerGoals();

        this.goalSelector.addGoal(3, new HitboxMeeleeAttackGoal(this,1.1,partTypeMap.get(IMonsterBreakablePart.PART.CLAW).get(0),
                "main_controller","attack_scratch",3,1.5f,4,
                5,22,42,66, true));
        this.goalSelector.addGoal(3, new HitboxMeeleeAttackGoal(this,1.1,partTypeMap.get(IMonsterBreakablePart.PART.TAIL).get(0),
                "main_controller","tailslam",2f,2f,1,
                2,34,40,87, true));
        this.goalSelector.addGoal(3, new HitboxMeeleeAttackGoal(this,1.1,partTypeMap.get(IMonsterBreakablePart.PART.TAIL).get(0),
                "main_controller","tailswipe",1.5f,1.5f,1,
                2,21,40,48, true));

        this.goalSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Vindicator.class, true));

        this.goalSelector.addGoal(9, new RallyGoal(this));
        this.goalSelector.addGoal(10, new SleepGoal(this));
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData, @Nullable CompoundTag pDataTag) {

/*        for (int i = 0; i < 2; i++) {
            BlockPos pos = i == 0 ? getOnPos().east(4):getOnPos().west(4);

            IzuchiEntity izuchi = ModEntities.IZUCHI.get().create(pLevel.getLevel());
            izuchi.setPos(pos.getX(),pos.getY()+1,pos.getZ());
            pLevel.getLevel().addFreshEntity(izuchi);
        }*/
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

    public List<Item> getDrops(){
        return List.of(ModItems.IZUCHI_HIDE_ITEM.get(),ModItems.IZUCHI_TAIL_ITEM.get(),
                ModItems.IZUCHI_HIDE_ITEM.get(),ModItems.IZUCHI_HIDE_ITEM.get());
    }
}
