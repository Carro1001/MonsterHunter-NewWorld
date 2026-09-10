package com.carro1001.mhnw.entities;

import com.carro1001.mhnw.entities.ai.ExhaustedStallGoal;
import com.carro1001.mhnw.entities.ai.MonsterAggressionStateGoal;
import com.carro1001.mhnw.entities.ai.RallyGoal;
import com.carro1001.mhnw.entities.ai.SleepGoal;
import com.carro1001.mhnw.entities.ai.brain.HitboxAnimationAttack;
import com.carro1001.mhnw.entities.interfaces.IMonsterBreakablePart;
import com.carro1001.mhnw.registration.ModItems;
import de.dertoaster.multihitboxlib.api.IMultipartEntity;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import net.tslat.smartbrainlib.api.SmartBrainOwner;
import net.tslat.smartbrainlib.api.core.BrainActivityGroup;
import net.tslat.smartbrainlib.api.core.SmartBrainProvider;
import net.tslat.smartbrainlib.api.core.behaviour.FirstApplicableBehaviour;
import net.tslat.smartbrainlib.api.core.behaviour.OneRandomBehaviour;
import net.tslat.smartbrainlib.api.core.behaviour.custom.look.LookAtTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.misc.Idle;
import net.tslat.smartbrainlib.api.core.behaviour.custom.move.MoveToWalkTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.move.StayWithinDistanceOfAttackTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.path.SetRandomWalkTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.target.InvalidateAttackTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.target.SetPlayerLookTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.target.SetRandomLookTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.target.TargetOrRetaliate;
import net.tslat.smartbrainlib.api.core.sensor.ExtendedSensor;
import net.tslat.smartbrainlib.api.core.sensor.vanilla.HurtBySensor;
import net.tslat.smartbrainlib.api.core.sensor.vanilla.NearbyLivingEntitySensor;

import java.util.List;

import static com.carro1001.mhnw.utils.MHNWReferences.GREAT;
import static com.carro1001.mhnw.utils.MHNWReferences.IZUCHI;

/**
 * Targeting and attacking run on SmartBrainLib's Brain system (see getSensors/getCoreTasks/
 * getIdleTasks/getFightTasks below); the rest of the moveset (rage/exhaustion, rallying, sleeping,
 * part-breaking) still comes from {@link NewWorldMonsterEntity} and a handful of small Goals that
 * don't overlap with what the Brain now owns - see the comment on {@link #registerGoals()}.
 */
public class GreatIzuchiEntity extends NewWorldMonsterEntity implements IMultipartEntity<NewWorldMonsterEntity>, SmartBrainOwner<GreatIzuchiEntity> {

    // how close the claw/tail can actually hit - shared by the "stay near target" behaviour and the
    // attacks so they don't disagree. real collision decides if a hit lands now, this just gets it
    // roughly in range.
    private static final double ATTACK_RANGE = 4.0;

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
        return RawAnimation.begin().thenPlayXTimes("animation.great_izuchi.attack_scratch",1);
    }
    protected RawAnimation getSwipeAnimation() {
        return RawAnimation.begin().thenPlayXTimes("animation.great_izuchi.attack_tailswipe",1);
    }
    protected RawAnimation getSlamAnimation() {
        return RawAnimation.begin().thenPlayXTimes("animation.great_izuchi.attack_tailslam",1);
    }

    //region Brain
    @Override
    protected Brain.Provider<?> brainProvider() {
        return new SmartBrainProvider<>(this);
    }

    @Override
    protected void customServerAiStep() {
        tickBrain(this);
        // don't sync Mob#setTarget() here yourself - TargetOrRetaliate already does it via
        // BrainUtils.setTargetOfEntity() when it grabs a target. tried force-syncing it every tick
        // once and it nulled the field whenever that read came back empty for a frame, which kept
        // resetting MonsterAggressionStateGoal to PASSIVE and re-triggering the roar mid-fight.
        // that's the roar-loop bug, don't bring it back.
    }

    @Override
    public List<ExtendedSensor<GreatIzuchiEntity>> getSensors() {
        return ObjectArrayList.of(
                new NearbyLivingEntitySensor<>(),
                new HurtBySensor<>());
    }

    @Override
    public BrainActivityGroup<GreatIzuchiEntity> getCoreTasks() {
        return BrainActivityGroup.coreTasks(
                new LookAtTarget<>(),
                new MoveToWalkTarget<>());
    }

    @Override
    public BrainActivityGroup<GreatIzuchiEntity> getIdleTasks() {
        return BrainActivityGroup.idleTasks(
                new FirstApplicableBehaviour<GreatIzuchiEntity>(
                        // villagers/pillagers count as targets too, mainly so you can watch it fight
                        // something that isn't you
                new TargetOrRetaliate<GreatIzuchiEntity>().attackablePredicate(entity ->
                        entity instanceof Player || entity instanceof Villager || entity instanceof AbstractIllager),
                        new SetPlayerLookTarget<>(),
                        new SetRandomLookTarget<>()),
                new OneRandomBehaviour<>(
                        new SetRandomWalkTarget<>(),
                        new Idle<GreatIzuchiEntity>().runFor(entity -> entity.getRandom().nextInt(30, 60))));
    }

    @Override
    public BrainActivityGroup<GreatIzuchiEntity> getFightTasks() {
        return BrainActivityGroup.fightTasks(
                new InvalidateAttackTarget<>(),
                // izuchi's a raptor - fast, close-quarters, aggressive, not a big reach/AOE brute -
                // so hugging the target is correct here, not a bug. keep it tight.
                new StayWithinDistanceOfAttackTarget<GreatIzuchiEntity>()
                        .minDistance(1.0f)
                        .maxDistance(2.0f),
                // animationLengthTicks match each clip's real length. hitCheckInflation is just a
                // small fairness margin now, not a crutch for a wrong hitbox (see HitboxAnimationAttack)
                new OneRandomBehaviour<>(
                        // claw needs extra reach because torsoHitbox/headHitbox are solid
                        // (collidable: true) and physically block the target from getting close
                        // enough otherwise
                        new HitboxAnimationAttack<GreatIzuchiEntity>("claw", "main_controller", "attack_scratch",
                                IMonsterBreakablePart.PART.CLAW, 66, 1, ATTACK_RANGE, 2.5f),
                        new HitboxAnimationAttack<GreatIzuchiEntity>("tailslam", "main_controller", "tailslam",
                                IMonsterBreakablePart.PART.TAIL, 87, 2, ATTACK_RANGE, 1.5f),
                        new HitboxAnimationAttack<GreatIzuchiEntity>("tailswipe", "main_controller", "tailswipe",
                                IMonsterBreakablePart.PART.TAIL, 47, 2, ATTACK_RANGE, 1.5f)));
    }

    // not calling super.registerGoals() on purpose - it adds NearestAttackableTargetGoal and
    // WaterAvoidingRandomStrollGoal, and the brain already owns targeting/movement (see
    // getIdleTasks/getFightTasks above). running both would just have them fight each other.
    // everything below is stuff the brain doesn't do.
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MonsterAggressionStateGoal(this));
        this.goalSelector.addGoal(2, new ExhaustedStallGoal(this));
        this.goalSelector.addGoal(9, new RallyGoal(this));
        this.goalSelector.addGoal(10, new SleepGoal(this));
    }
    //endregion

    // escort izuchi spawn is off for now, they were mobbing the tester during combat testing. pull
    // the loop back from git history once the moveset itself is solid.
    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData, @Nullable CompoundTag pDataTag) {
        return super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData, pDataTag);
    }

    public static AttributeSupplier.Builder prepareAttributes() {
        return NewWorldMonsterEntity.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 350)
                .add(Attributes.FOLLOW_RANGE, 15.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
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
