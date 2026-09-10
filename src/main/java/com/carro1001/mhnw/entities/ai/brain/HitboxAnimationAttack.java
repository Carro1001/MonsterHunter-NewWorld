package com.carro1001.mhnw.entities.ai.brain;

import com.carro1001.mhnw.MHNW;
import com.carro1001.mhnw.entities.NewWorldMonsterEntity;
import com.carro1001.mhnw.entities.helpers.MonsterBreakablePartEntity;
import com.carro1001.mhnw.entities.interfaces.IMonsterBreakablePart;
import com.carro1001.mhnw.registration.ModDamageTypes;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.phys.AABB;
import net.tslat.smartbrainlib.api.core.behaviour.ExtendedBehaviour;
import net.tslat.smartbrainlib.util.BrainUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Brain replacement for {@link com.carro1001.mhnw.entities.ai.HitboxMeeleeAttackGoal}: plays a
 * GeckoLib animation and checks a breakable-part hitbox (claw, tail, ...) against nearby living
 * entities for the whole swing, each target hittable once. No hand-picked hit-check tick window -
 * the hitbox is bone-synced to the animation, so checking every tick just follows whatever it's
 * actually doing instead of needing frame numbers re-tuned per asset (the old tick-window version
 * never matched the real animation).
 * <p>
 * One instance = one attack. Wire a few into {@code getFightTasks()} (e.g. via
 * {@code OneRandomBehaviour}) for a moveset instead of a single move.
 */
public class HitboxAnimationAttack<E extends NewWorldMonsterEntity> extends ExtendedBehaviour<E> {
    private static final List<Pair<MemoryModuleType<?>, MemoryStatus>> MEMORY_REQUIREMENTS =
            ObjectArrayList.of(
                    Pair.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT),
                    // REGISTERED here, not VALUE_ABSENT - this just gets the brain to allocate a slot
                    // for ATTACK_COOLING_DOWN at all (nothing else on this entity declares it). the
                    // real absence check happens in checkExtraStartConditions. skip this and checking
                    // an unregistered memory type just fails quietly - looks like the monster forgot
                    // how to attack.
                    Pair.of(MemoryModuleType.ATTACK_COOLING_DOWN, MemoryStatus.REGISTERED));

    private final String debugName;
    private final String animController;
    private final String animTrigger;
    private final IMonsterBreakablePart.PART hitboxPart;
    private final int animationLengthTicks;
    private final int maxTargetsPerSwing;
    private final double attackRangeSqr;
    private final float hitCheckInflation;

    private MonsterBreakablePartEntity<NewWorldMonsterEntity> partHitbox;
    private LivingEntity target;
    private int ticksElapsed;
    private final Set<UUID> alreadyHitThisSwing = new HashSet<>();

    /**
     * @param debugName            short name for log lines, e.g. "claw"
     * @param animController       GeckoLib controller name the trigger lives on
     * @param animTrigger          triggerableAnim name to play
     * @param hitboxPart           which breakable part's hitbox to check overlap against
     * @param animationLengthTicks total ticks the attack occupies before the behaviour ends
     * @param maxTargetsPerSwing   max distinct entities this swing can damage (1 for single-target, more for AOE)
     * @param attackRange          distance (blocks) to the target required to start the attack
     * @param hitCheckInflation    how much to inflate the part hitbox's AABB when checking overlap - a
     *                             small fairness margin, not a substitute for the hitbox being in the
     *                             right place
     */
    public HitboxAnimationAttack(String debugName, String animController, String animTrigger, IMonsterBreakablePart.PART hitboxPart,
                                  int animationLengthTicks, int maxTargetsPerSwing, double attackRange, float hitCheckInflation) {
        this.debugName = debugName;
        this.animController = animController;
        this.animTrigger = animTrigger;
        this.hitboxPart = hitboxPart;
        this.animationLengthTicks = animationLengthTicks;
        this.maxTargetsPerSwing = maxTargetsPerSwing;
        this.attackRangeSqr = attackRange * attackRange;
        this.hitCheckInflation = hitCheckInflation;
    }

    @Override
    protected List<Pair<MemoryModuleType<?>, MemoryStatus>> getMemoryRequirements() {
        return MEMORY_REQUIREMENTS;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, E entity) {
        LivingEntity attackTarget = BrainUtils.getTargetOfEntity(entity);

        // ATTACK_COOLING_DOWN is shared brain state, not per-instance - it's what stops a different
        // sibling attack (claw/tailslam/tailswipe are separate instances) from cutting in the instant
        // this one ends. .cooldownFor() alone only stops the same attack repeating itself.
        return attackTarget != null && attackTarget.isAlive() && !entity.isSleeping() && !entity.isAnimating()
                && entity.getBrain().checkMemory(MemoryModuleType.ATTACK_COOLING_DOWN, MemoryStatus.VALUE_ABSENT)
                && entity.partTypeMap != null && entity.partTypeMap.getOrDefault(hitboxPart, List.of()).size() > 0
                && entity.distanceToSqr(attackTarget) <= attackRangeSqr;
    }

    @Override
    protected boolean shouldKeepRunning(E entity) {
        return target != null && target.isAlive();
    }

    // vanilla Behavior hardcodes a 60-tick duration and .runFor() doesn't actually override it, so
    // tailslam (87 ticks) and claw (66) were both getting cut off before the follow-through played.
    // track our own elapsed count instead.
    @Override
    protected boolean timedOut(long gameTime) {
        return ticksElapsed >= animationLengthTicks;
    }

    @Override
    protected void start(E entity) {
        target = BrainUtils.getTargetOfEntity(entity);
        partHitbox = entity.partTypeMap.get(hitboxPart).get(0);
        ticksElapsed = 0;
        alreadyHitThisSwing.clear();

        entity.getLookControl().setLookAt(target, 30F, 30F);
        entity.setAttacking(true);
        entity.triggerAnim(animController, animTrigger);

        // one line per swing, not per tick. .info() on purpose - .debug() is already a firehose
        // elsewhere in this codebase (every poseBody() call logs), so bumping the log level to see
        // this would drown it out.
        MHNW.LOGGER.info("{} [{}]: attack start, target={}", entity.getName().getString(), debugName, target.getName().getString());
    }

    @Override
    protected void tick(E entity) {
        if (target == null || !target.isAlive())
            return;

        entity.getLookControl().setLookAt(target, 30F, 30F);
        ticksElapsed++;

        if (alreadyHitThisSwing.size() >= maxTargetsPerSwing)
            return;

        AABB hitBox = partHitbox.getBoundingBox().inflate(hitCheckInflation);

        for (LivingEntity hit : entity.level().getEntitiesOfClass(LivingEntity.class, hitBox,
                le -> le != entity && le.isAlive() && !(le instanceof NewWorldMonsterEntity) && !alreadyHitThisSwing.contains(le.getUUID()))) {
            if (alreadyHitThisSwing.size() >= maxTargetsPerSwing)
                break;

            float damage = (float) entity.getAttributeValue(Attributes.ATTACK_DAMAGE);
            float knockback = (float) entity.getAttributeValue(Attributes.ATTACK_KNOCKBACK);
            DamageSource source = new DamageSource(entity.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(ModDamageTypes.RAW));

            float healthBefore = hit.getHealth();
            alreadyHitThisSwing.add(hit.getUUID());
            if (hit.hurt(source, damage)) {
                hit.knockback(knockback * 0.5F, Mth.sin(entity.getYRot() * Mth.DEG_TO_RAD), -Mth.cos(entity.getYRot() * Mth.DEG_TO_RAD));
                entity.setLastHurtMob(hit);
                // health before/after so there's no doubt this actually landed, not just triggered
                // without visible feedback
                MHNW.LOGGER.info("{} [{}]: hit {} for {} (tick {}), health {}->{}", entity.getName().getString(), debugName, hit.getName().getString(), damage, ticksElapsed, healthBefore, hit.getHealth());
            }
        }
    }

    @Override
    protected void stop(E entity) {
        // ticksElapsed vs animationLengthTicks shows if this ran full length or got cut short
        // (target died mid-swing, etc - see shouldKeepRunning)
        MHNW.LOGGER.info("{} [{}]: attack end, hits={}, ticks={}/{}", entity.getName().getString(), debugName, alreadyHitThisSwing.size(), ticksElapsed, animationLengthTicks);
        // shared cooldown (see checkExtraStartConditions) so a sibling attack doesn't cut in the
        // second this one ends - gives the animation room to actually finish playing
        BrainUtils.setForgettableMemory(entity, MemoryModuleType.ATTACK_COOLING_DOWN, true, 10);
        entity.setAttacking(false);
        target = null;
        partHitbox = null;
    }
}
