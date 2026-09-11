package com.carro1001.mhnw;

import com.carro1001.mhnw.entity.Aptonoth;
import com.carro1001.mhnw.entity.AttackProfile;
import com.carro1001.mhnw.entity.GreatIzuchi;
import com.carro1001.mhnw.entity.GreatIzuchiCombatGoal;
import com.carro1001.mhnw.entity.MonsterPart;
import com.carro1001.mhnw.entity.Toad;
import com.carro1001.mhnw.registry.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Server-side acceptance tests for the Great Izuchi slice, and (from the {@code aptonoth*}
 * methods on) the P3 passive-herbivore slice.
 *
 * <p>These cover the invariants a human at a screen cannot reliably check and that would regress
 * silently the next time the hurtbox geometry moves: that damage through a part reaches the parent
 * exactly once, that one area source touching several parts is not several hits, that two distinct
 * attackers are not conflated into one, and that the attack deals damage only inside its active
 * window.
 *
 * <p>What they deliberately do not cover: whether a texture renders, whether an animation looks
 * right, or whether a hurtbox visually sits on the body. Those need a client and a person
 * (handoff section 4.6).
 *
 * <p>Run with {@code gradlew runGameTestServer}.
 */
@GameTestHolder(MHNW.MOD_ID)
@PrefixGameTestTemplate(false)
public class MHNWGameTests {

    private static final String ARENA = "arena";
    private static final float PROBE_DAMAGE = 5.0F;
    private static final float EPSILON = 0.01F;

    private static GreatIzuchi spawnInert(GameTestHelper helper) {
        GreatIzuchi monster = helper.spawn(ModEntities.GREAT_IZUCHI.get(), 8, 2, 8);
        monster.setNoAi(true);
        monster.setInvulnerable(false);
        return monster;
    }

    /** A03: a hit on a named part reduces the parent's health, once. */
    @GameTest(template = ARENA, timeoutTicks = 120)
    public static void partDamageReachesParent(GameTestHelper helper) {
        GreatIzuchi monster = spawnInert(helper);
        float before = monster.getHealth();

        MonsterPart tail = monster.part("tail_4");
        tail.hurt(helper.getLevel().damageSources().generic(), PROBE_DAMAGE);

        float lost = before - monster.getHealth();
        helper.assertTrue(Math.abs(lost - PROBE_DAMAGE) < EPSILON,
                "hitting the tail should cost the parent exactly " + PROBE_DAMAGE
                        + " health, but it lost " + lost);
        helper.succeed();
    }

    /**
     * A03/A06: one damage source that enumerates several parts in a single tick, as an explosion
     * does, must cost exactly one application rather than one per part.
     */
    @GameTest(template = ARENA, timeoutTicks = 120)
    public static void oneSourceAcrossManyPartsCountsOnce(GameTestHelper helper) {
        GreatIzuchi monster = spawnInert(helper);
        float before = monster.getHealth();

        DamageSource explosion = helper.getLevel().damageSources().generic();
        monster.part("torso").hurt(explosion, PROBE_DAMAGE);
        monster.part("head").hurt(explosion, PROBE_DAMAGE);
        monster.part("tail_1").hurt(explosion, PROBE_DAMAGE);
        monster.part("tail_4").hurt(explosion, PROBE_DAMAGE);

        float lost = before - monster.getHealth();
        helper.assertTrue(Math.abs(lost - PROBE_DAMAGE) < EPSILON,
                "one source touching four parts should cost " + PROBE_DAMAGE
                        + " health once, but the parent lost " + lost);
        helper.succeed();
    }

    /**
     * A03: the de-duplication must key on the damage source, not merely the tick. Two different
     * attackers in the same tick are two legitimate hits and must not be collapsed into one.
     *
     * <p>The second hit is deliberately the SMALLER of the two, not larger. An earlier version of
     * this test made it larger, which passes whether or not source identity is actually honoured:
     * vanilla's own invulnerability lets a larger second hit through as the difference regardless
     * of who dealt it, so that version never actually exercised the fairness guarantee it claimed
     * to check. A smaller second hit from a genuinely different source is the case vanilla's own
     * amount-only comparison gets wrong on its own ({@code amount <= lastHurt} silently drops it),
     * which is what {@code GreatIzuchi.hurt}'s {@code invulnerableTime} reset exists to fix.
     */
    @GameTest(template = ARENA, timeoutTicks = 120)
    public static void distinctSourcesAreNotConflated(GameTestHelper helper) {
        GreatIzuchi monster = spawnInert(helper);
        float before = monster.getHealth();

        monster.part("torso").hurt(helper.getLevel().damageSources().generic(), PROBE_DAMAGE * 2);
        monster.part("head").hurt(helper.getLevel().damageSources().magic(), PROBE_DAMAGE);

        float lost = before - monster.getHealth();
        float expected = PROBE_DAMAGE * 2 + PROBE_DAMAGE;
        helper.assertTrue(Math.abs(lost - expected) < EPSILON,
                "two distinct attackers, the second dealing less than the first, should both land in"
                        + " full for " + expected + " total, but the parent lost " + lost);
        helper.succeed();
    }

    /**
     * A05: the scratch must connect, and must only deal damage inside its active window. Recording
     * the age at which the victim first loses health tests both at once, and does so without
     * depending on where the hurtboxes happen to sit.
     */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void scratchDamagesOnlyDuringActiveWindow(GameTestHelper helper) {
        GreatIzuchi monster = helper.spawn(ModEntities.GREAT_IZUCHI.get(), 8, 2, 8);
        Cow victim = helper.spawn(EntityType.COW, 8, 2, 10);
        victim.setNoAi(true);

        monster.setTarget(victim);
        monster.attackCooldown = 0;

        final float startingHealth = victim.getHealth();
        final int[] ageAtFirstHit = {Integer.MIN_VALUE};

        helper.startSequence()
                .thenExecuteFor(150, () -> {
                    if (ageAtFirstHit[0] == Integer.MIN_VALUE && victim.getHealth() < startingHealth) {
                        ageAtFirstHit[0] = monster.getAttackAge();
                    }
                })
                .thenExecute(() -> {
                    helper.assertTrue(ageAtFirstHit[0] != Integer.MIN_VALUE,
                            "the scratch never connected with a target standing in front of it");
                    helper.assertTrue(ageAtFirstHit[0] >= AttackProfile.SCRATCH.activeStart()
                                    && ageAtFirstHit[0] <= AttackProfile.SCRATCH.activeEnd(),
                            "damage landed at action age " + ageAtFirstHit[0]
                                    + ", outside the active window "
                                    + AttackProfile.SCRATCH.activeStart() + ".."
                                    + AttackProfile.SCRATCH.activeEnd());
                })
                .thenSucceed();
    }

    /**
     * A05: a victim that stays inside the swing for its whole duration is struck at most
     * STRIKES times by that action, not once per tick of overlap. Asserted before the cooldown
     * could allow a second action, so any extra damage is a genuine duplicate within one swing.
     */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void atMostThreeStrikesPerActionDespiteContinuousOverlap(GameTestHelper helper) {
        GreatIzuchi monster = helper.spawn(ModEntities.GREAT_IZUCHI.get(), 8, 2, 8);
        Cow victim = helper.spawn(EntityType.COW, 8, 2, 10);
        victim.setNoAi(true);
        victim.setInvulnerable(false);

        monster.setTarget(victim);
        monster.attackCooldown = 0;

        final float startingHealth = victim.getHealth();
        final float strike = (float) GreatIzuchiCombatGoal.SCRATCH_DAMAGE;

        helper.runAtTickTime(85, () -> {
            float lost = startingHealth - victim.getHealth();
            helper.assertTrue(lost > 0.0F, "the scratch never connected");
            helper.assertTrue(lost <= strike * AttackProfile.SCRATCH.strikes() + EPSILON,
                    "one swing cost the victim " + lost + " health, more than the "
                            + AttackProfile.SCRATCH.strikes() + " strikes of " + strike
                            + " it is designed to deal");
            helper.succeed();
        });
    }

    /**
     * A05: whichever attack lands a hit, that hit falls within that attack's own active window,
     * no matter which of the three attacks the selector happened to choose.
     *
     * <p>This deliberately does not isolate one attack by distance. The swipe's range band
     * (2.0-4.6) sits entirely inside the slam's (2.0-5.2), so no distance selects one without the
     * other being equally eligible; attack choice among eligible candidates has a random
     * component, and an early version of this test that tried to force the slam via distance was
     * flaky for exactly that reason, sometimes observing the swipe instead. Checking the invariant
     * against whichever attack actually fires sidesteps that: it is the regression test for the
     * bug the swipe shipped with, an active window that did not match where its limb path landed,
     * and it would have caught that bug on the very first hit, whichever attack landed it.
     *
     * <p>The victim sits where all three attacks' bands overlap (about 2.25 blocks of box
     * distance), so every action the monster takes is a real attempt to land one, and the test
     * also confirms more than one distinct attack was actually observed rather than trivially
     * passing on zero hits. The overlap band is only 0.6 block wide, so the victim is re-pinned to
     * that offset every tick rather than left to drift: knockback from a landed hit or the
     * monster's own lunge is enough to push the real distance outside the shared band and lock the
     * rest of the fight onto whichever wider-banded attack that drift lands in.
     */
    @GameTest(template = ARENA, timeoutTicks = 500)
    public static void everyAttackOnlyDamagesWithinItsOwnActiveWindow(GameTestHelper helper) {
        GreatIzuchi monster = helper.spawn(ModEntities.GREAT_IZUCHI.get(), 8, 2, 8);
        Cow victim = helper.spawn(EntityType.COW, 8.0F, 2.0F, 10.7F);
        victim.setNoAi(true);
        victim.setInvulnerable(false);
        victim.setHealth(victim.getMaxHealth() * 30.0F);

        monster.setTarget(victim);
        monster.attackCooldown = 0;

        final float[] lastHealth = {victim.getHealth()};
        final java.util.Set<Byte> observedIds = new java.util.HashSet<>();

        helper.startSequence()
                .thenExecuteFor(450, () -> {
                    victim.teleportTo(monster.getX(), monster.getY(), monster.getZ() + 2.7);
                    victim.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                    float health = victim.getHealth();
                    if (health < lastHealth[0]) {
                        byte id = monster.getAttackId();
                        int age = monster.getAttackAge();
                        AttackProfile profile = AttackProfile.byId(id);
                        helper.assertTrue(profile != null,
                                "damage landed with attack id " + id + ", which has no profile");
                        helper.assertTrue(age >= profile.activeStart() && age <= profile.activeEnd(),
                                "attack id " + id + " dealt damage at action age " + age
                                        + ", outside its own active window " + profile.activeStart()
                                        + ".." + profile.activeEnd());
                        observedIds.add(id);
                    }
                    lastHealth[0] = health;
                })
                .thenExecute(() -> helper.assertTrue(observedIds.size() >= 2,
                        "only observed damage from " + observedIds.size()
                                + " distinct attack(s) in 450 ticks; this test needs to see more than"
                                + " one kind of attack to be checking anything"))
                .thenSucceed();
    }

    /**
     * A07: losing the target mid-action must not wedge the state machine. The action already
     * committed is allowed to finish, but it must end on its own and release the goal into a normal
     * cooldown rather than looping or leaving the attack id stuck forever.
     */
    @GameTest(template = ARENA, timeoutTicks = 220)
    public static void losingTargetDuringActionEndsItCleanly(GameTestHelper helper) {
        GreatIzuchi monster = helper.spawn(ModEntities.GREAT_IZUCHI.get(), 8, 2, 8);
        Cow victim = helper.spawn(EntityType.COW, 8, 2, 10);
        victim.setNoAi(true);

        monster.setTarget(victim);
        monster.attackCooldown = 0;

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        monster.getAttackId() != GreatIzuchi.ATTACK_NONE, "waiting for an attack to start"))
                .thenExecute(() -> monster.setTarget(null))
                .thenIdle(150)
                .thenExecute(() -> helper.assertTrue(monster.getAttackId() == GreatIzuchi.ATTACK_NONE,
                        "the attack never ended after its target vanished; the goal is wedged"))
                .thenSucceed();
    }

    /**
     * A07: repeating an attack must allocate a new action sequence each time, not reuse the one
     * from the previous action. This is the exact bug that shipped earlier: a read-modify-write of
     * the synced sequence value always reported 1, so a repeated attack dealt its damage without
     * ever restarting its swing animation, and every attack after the first looked like nothing was
     * happening even though the log showed hits landing.
     */
    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void repeatedAttacksGetNewSequenceNumbers(GameTestHelper helper) {
        GreatIzuchi monster = helper.spawn(ModEntities.GREAT_IZUCHI.get(), 8, 2, 8);
        Cow victim = helper.spawn(EntityType.COW, 8, 2, 10);
        victim.setNoAi(true);
        victim.setInvulnerable(false);
        victim.setHealth(victim.getMaxHealth() * 4);

        monster.setTarget(victim);
        monster.attackCooldown = 0;

        final int[] firstSequence = {-1};
        final int[] seenSecondSequence = {-1};

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        monster.getAttackId() != GreatIzuchi.ATTACK_NONE, "waiting for the first attack"))
                .thenExecute(() -> firstSequence[0] = monster.getActionSequence())
                .thenWaitUntil(() -> helper.assertTrue(
                        monster.getAttackId() == GreatIzuchi.ATTACK_NONE, "waiting for it to end"))
                .thenWaitUntil(() -> helper.assertTrue(
                        monster.getAttackId() != GreatIzuchi.ATTACK_NONE, "waiting for a second attack"))
                .thenExecute(() -> seenSecondSequence[0] = monster.getActionSequence())
                .thenExecute(() -> helper.assertTrue(firstSequence[0] >= 0 && seenSecondSequence[0] >= 0,
                        "never observed two separate actions"))
                .thenExecute(() -> helper.assertTrue(seenSecondSequence[0] != firstSequence[0],
                        "the second attack reused sequence " + firstSequence[0]
                                + " instead of allocating a new one"))
                .thenSucceed();
    }

    /**
     * Every attack's limb path must span its own active window.
     *
     * <p>This is a pure data check, but it is the cheapest guard against the exact bug that shipped
     * in the tail swipe: its active window was ticks 6 to 13 while its path keyframes covered a
     * different range entirely. Outside its keyframes the path clamps to an endpoint, so the volume
     * sat frozen at the end of its arc for most of the action and could not hit anything. Any
     * future attack whose window and path disagree fails here instead of in a play session.
     */
    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void everyAttackPathCoversItsActiveWindow(GameTestHelper helper) {
        for (AttackProfile profile : AttackProfile.all()) {
            String name = "attack id " + profile.id();
            helper.assertTrue(profile.volumeCount() >= 1, name + " has no damage volume at all");
            helper.assertTrue(profile.windupEnd() < profile.activeStart(),
                    name + " has a windup that overlaps its active window");
            helper.assertTrue(profile.activeStart() <= profile.activeEnd(),
                    name + " has an empty active window");
            helper.assertTrue(profile.activeEnd() <= profile.actionEnd(),
                    name + " keeps hitting after the action has ended");
            helper.assertTrue(profile.strikes() >= 1, name + " cannot strike at all");
            helper.assertTrue(profile.minRange() < profile.maxRange(),
                    name + " has an empty range band");

            for (int v = 0; v < profile.volumeCount(); v++) {
                double[][] path = profile.paths()[v];
                String track = name + " volume " + v;

                helper.assertTrue(path.length >= 2, track + " needs at least two path keyframes");
                helper.assertTrue(path[0][0] <= profile.activeStart(),
                        track + " starts hitting at tick " + profile.activeStart()
                                + " but its path does not begin until " + path[0][0]);
                helper.assertTrue(path[path.length - 1][0] >= profile.activeEnd(),
                        track + " hits until tick " + profile.activeEnd()
                                + " but its path ends at " + path[path.length - 1][0]);

                for (int i = 1; i < path.length; i++) {
                    helper.assertTrue(path[i][0] > path[i - 1][0],
                            track + " has out-of-order path keyframes at index " + i);
                }
            }
        }
        helper.succeed();
    }

    /**
     * Once the monster has closed the distance, something must be usable.
     *
     * <p>A monster that walks to its preferred range and then finds no attack whose band contains
     * that distance stands there forever. That deadlock has already happened once here, when the
     * collidable torso held victims just outside a too-small reach, and it is invisible in a build
     * that compiles perfectly. Asserting that at least one attack covers the distance the approach
     * actually stops at turns it into a test failure.
     */
    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void somethingIsUsableAtClosingRange(GameTestHelper helper) {
        boolean any = false;
        for (AttackProfile profile : AttackProfile.all()) {
            if (profile.inRange(GreatIzuchiCombatGoal.CLOSE_RANGE)) {
                any = true;
                break;
            }
        }
        helper.assertTrue(any,
                "the monster closes to " + GreatIzuchiCombatGoal.CLOSE_RANGE
                        + " blocks but no attack covers that distance, so it would stand there");
        helper.succeed();
    }

    /**
     * A02: a reload cancels transient combat instead of resuming it.
     *
     * <p>This cannot exercise an actual save-quit-reload of the world; a GameTest structure has no
     * such cycle to trigger. What it does exercise is the exact code path a real reload goes
     * through for this entity: {@code addAdditionalSaveData} writing NBT from a live, mid-fight
     * monster, and {@code readAdditionalSaveData} reading it back into a freshly constructed one,
     * which is what disk persistence actually calls. Section 4.3 rule 7 says loading a creature
     * must cancel any transient combat and impose a short cooldown rather than replaying an
     * interrupted attack; this is that promise, checked at the boundary this mod owns.
     *
     * <p>Also checks that the reloaded entity has exactly as many parts as it started with. Parts
     * are always rebuilt fresh in the constructor rather than read from NBT, so a genuine part-list
     * bug (the previous implementation's parts list that only ever appended, never replaced, a
     * later part of the same type) would show up here as an unexpected count.
     */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void reloadCancelsTransientCombatState(GameTestHelper helper) {
        GreatIzuchi original = helper.spawn(ModEntities.GREAT_IZUCHI.get(), 8, 2, 8);
        Cow victim = helper.spawn(EntityType.COW, 8, 2, 10);
        victim.setNoAi(true);

        original.setTarget(victim);
        original.attackCooldown = 0;
        int originalPartCount = original.monsterParts().length;

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        original.getAttackId() != GreatIzuchi.ATTACK_NONE, "waiting for an attack to start"))
                .thenExecute(() -> {
                    CompoundTag saved = new CompoundTag();
                    original.addAdditionalSaveData(saved);

                    GreatIzuchi reloaded = new GreatIzuchi(ModEntities.GREAT_IZUCHI.get(), helper.getLevel());
                    reloaded.readAdditionalSaveData(saved);

                    helper.assertTrue(reloaded.getAttackId() == GreatIzuchi.ATTACK_NONE,
                            "a reloaded monster resumed attack id " + reloaded.getAttackId()
                                    + " instead of starting idle");
                    helper.assertTrue(reloaded.attackCooldown > 0,
                            "a reloaded monster had no cooldown at all, so it could attack instantly");
                    helper.assertTrue(reloaded.monsterParts().length == originalPartCount,
                            "reload produced " + reloaded.monsterParts().length + " parts, expected "
                                    + originalPartCount);
                })
                .thenSucceed();
    }

    /** A02/A13: death removes the creature and every one of its parts, exactly once. */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void deathRemovesTheWholeCreature(GameTestHelper helper) {
        GreatIzuchi monster = spawnInert(helper);
        int partCount = monster.getParts().length;
        helper.assertTrue(partCount > 0, "the monster registered no parts at all");

        monster.hurt(helper.getLevel().damageSources().genericKill(), Float.MAX_VALUE);

        helper.succeedWhen(() -> {
            helper.assertTrue(monster.isRemoved(), "the monster was not removed after dying");
            for (MonsterPart part : monster.monsterParts()) {
                helper.assertTrue(part.isRemoved() || !part.isAddedToLevel(),
                        "part " + part.partName + " outlived its parent");
            }
        });
    }

    // ---------------------------------------------------------------- Aptonoth (P3)

    /**
     * Aptonoth must not be a hostile boss wearing a herbivore's texture: with nothing having hurt
     * it, it must never acquire a target purely from a nearby living entity being present. Only
     * {@link net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal} is registered on its
     * target selector, so this is really just confirming that registration is what it appears to
     * be, but it is the one automatable check of the "passive" half of "passive herbivore."
     */
    @GameTest(template = ARENA, timeoutTicks = 120)
    public static void aptonothIsNotHostileByDefault(GameTestHelper helper) {
        Aptonoth aptonoth = helper.spawn(ModEntities.APTONOTH.get(), 8, 2, 8);
        Cow bystander = helper.spawn(EntityType.COW, 8, 2, 9);
        bystander.setNoAi(true);

        helper.startSequence()
                .thenExecuteFor(100, () -> helper.assertTrue(aptonoth.getTarget() == null,
                        "an unprovoked Aptonoth acquired a target of its own accord"))
                .thenSucceed();
    }

    /**
     * When hurt by another entity, Aptonoth moves away from where that happened rather than
     * standing its ground. This is the automatable half of "flee when provoked"; whether the rare
     * defend-instead-of-flee fallback ever actually triggers is not asserted, since {@link Aptonoth}
     * itself documents that Panic dominates in practice and does not promise otherwise.
     */
    @GameTest(template = ARENA, timeoutTicks = 120)
    public static void aptonothFleesWhenHurtByAnEntity(GameTestHelper helper) {
        Aptonoth aptonoth = helper.spawn(ModEntities.APTONOTH.get(), 8, 2, 8);
        Cow attacker = helper.spawn(EntityType.COW, 8, 2, 9);
        attacker.setNoAi(true);

        double startX = aptonoth.getX();
        double startZ = aptonoth.getZ();
        aptonoth.hurt(helper.getLevel().damageSources().mobAttack(attacker), 1.0F);

        helper.succeedWhen(() -> {
            double dx = aptonoth.getX() - startX;
            double dz = aptonoth.getZ() - startZ;
            double moved = Math.sqrt(dx * dx + dz * dz);
            helper.assertTrue(moved > 1.5D,
                    "a hurt Aptonoth only moved " + moved + " blocks from where it was attacked");
        });
    }

    /** A13: an ordinary passive mob death removes it, exactly once, same as any vanilla animal. */
    @GameTest(template = ARENA, timeoutTicks = 120)
    public static void aptonothDeathRemovesIt(GameTestHelper helper) {
        Aptonoth aptonoth = helper.spawn(ModEntities.APTONOTH.get(), 8, 2, 8);
        aptonoth.hurt(helper.getLevel().damageSources().genericKill(), Float.MAX_VALUE);

        helper.succeedWhen(() -> helper.assertTrue(
                aptonoth.isRemoved(), "the Aptonoth was not removed after dying"));
    }

    // ---------------------------------------------------------------- Toad (P3, endemic life)

    /**
     * A02: the variant is a saved gameplay fact, unlike Great Izuchi's transient combat state, so
     * it must survive the same NBT round trip that a real reload goes through, not be reset by it.
     */
    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void toadVariantPersistsAcrossReload(GameTestHelper helper) {
        Toad original = helper.spawn(ModEntities.TOAD.get(), 8, 2, 8);
        original.setVariant(Toad.Variant.BLAST);

        CompoundTag saved = new CompoundTag();
        original.addAdditionalSaveData(saved);

        Toad reloaded = new Toad(ModEntities.TOAD.get(), helper.getLevel());
        reloaded.readAdditionalSaveData(saved);

        helper.assertTrue(reloaded.getVariant() == Toad.Variant.BLAST,
                "reloaded toad had variant " + reloaded.getVariant() + ", expected BLAST");
        helper.succeed();
    }

    /**
     * Provocation (being hurt) starts the fuse, and the fuse eventually releases the variant's
     * effect on a nearby victim. This is the one concrete, deterministic check that the whole
     * chain, trigger through release, actually delivers what it promises for one variant; the
     * other three variants share the same trigger and release path and differ only in which
     * {@code MobEffectInstance} or explosion is applied, which is not worth re-testing per variant.
     */
    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void toadReleasesPoisonWhenProvoked(GameTestHelper helper) {
        Toad toad = helper.spawn(ModEntities.TOAD.get(), 8, 2, 8);
        toad.setVariant(Toad.Variant.POISON);
        Cow victim = helper.spawn(EntityType.COW, 8, 2, 9);
        victim.setNoAi(true);

        toad.hurt(helper.getLevel().damageSources().generic(), 1.0F);

        helper.succeedWhen(() -> helper.assertTrue(
                victim.hasEffect(net.minecraft.world.effect.MobEffects.POISON),
                "a nearby victim never received poison after the toad was provoked"));
    }

    /**
     * The release is bounded: it cannot happen again immediately. Re-provoking right after a
     * release must not restart the fuse until the cooldown has elapsed, or the cloud could fire
     * every tick indefinitely (handoff P3 exit: "effects cannot trigger indefinitely").
     */
    @GameTest(template = ARENA, timeoutTicks = 140)
    public static void toadCannotRefireDuringCooldown(GameTestHelper helper) {
        Toad toad = helper.spawn(ModEntities.TOAD.get(), 8, 2, 8);
        toad.setVariant(Toad.Variant.POISON);

        toad.hurt(helper.getLevel().damageSources().generic(), 1.0F);

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        toad.isFusing(), "waiting for the first fuse to actually start"))
                .thenWaitUntil(() -> helper.assertTrue(
                        !toad.isFusing(), "waiting for the first fuse to finish releasing"))
                .thenExecute(() -> toad.hurt(helper.getLevel().damageSources().generic(), 1.0F))
                .thenExecuteFor(60, () -> helper.assertTrue(!toad.isFusing(),
                        "the toad started a second fuse while still on cooldown from the first"))
                .thenSucceed();
    }

    /** A cloud must not reach through a wall any more than a melee swing may (section 4.2). */
    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void toadCloudDoesNotReachThroughWalls(GameTestHelper helper) {
        Toad toad = helper.spawn(ModEntities.TOAD.get(), 8, 2, 8);
        toad.setVariant(Toad.Variant.POISON);
        Cow victim = helper.spawn(EntityType.COW, 8, 2, 10);
        victim.setNoAi(true);

        for (int y = 2; y <= 5; y++) {
            helper.setBlock(8, y, 9, net.minecraft.world.level.block.Blocks.STONE);
        }

        toad.hurt(helper.getLevel().damageSources().generic(), 1.0F);

        helper.runAtTickTime(90, () -> {
            helper.assertTrue(!victim.hasEffect(net.minecraft.world.effect.MobEffects.POISON),
                    "a victim behind a wall received the toad's cloud effect anyway");
            helper.succeed();
        });
    }

    /**
     * The cooldown must actually expire, not just hold for a while. This is the regression test
     * for a real bug: the cooldown was originally a counter decremented inside the goal's own
     * {@code tick()}, but {@code tick()} only ever runs while the goal selector considers the goal
     * running, which is precisely the window a cooldown is not active in. That counter would sit
     * at its starting value forever, and every toad would permanently go quiet after its first
     * release. {@code toadCannotRefireDuringCooldown} alone would never have caught this: it only
     * asserts the negative (no refire too soon), never that a refire eventually happens at all.
     */
    @GameTest(template = ARENA, timeoutTicks = 340)
    public static void toadCanFireAgainAfterCooldownElapses(GameTestHelper helper) {
        Toad toad = helper.spawn(ModEntities.TOAD.get(), 8, 2, 8);
        toad.setVariant(Toad.Variant.POISON);

        toad.hurt(helper.getLevel().damageSources().generic(), 1.0F);

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        toad.isFusing(), "waiting for the first fuse to actually start"))
                .thenWaitUntil(() -> helper.assertTrue(
                        !toad.isFusing(), "waiting for the first fuse to finish releasing"))
                .thenIdle(210)
                .thenExecute(() -> toad.hurt(helper.getLevel().damageSources().generic(), 1.0F))
                .thenWaitUntil(() -> helper.assertTrue(toad.isFusing(),
                        "the toad never fused a second time once its cooldown had actually elapsed"))
                .thenSucceed();
    }

    // ---------------------------------------------------------------- Flashbug (P3, endemic life)

    /**
     * The second caller of {@code EndemicAreaEffectGoal}: provocation reaches release the same way
     * it does for the toad, through the shared base rather than a second hand-rolled timeline.
     */
    @GameTest(template = ARENA, timeoutTicks = 60)
    public static void flashbugBlindsOnProvocation(GameTestHelper helper) {
        com.carro1001.mhnw.entity.Flashbug flashbug = helper.spawn(ModEntities.FLASHBUG.get(), 8, 2, 8);
        Cow victim = helper.spawn(EntityType.COW, 8, 2, 9);
        victim.setNoAi(true);

        flashbug.hurt(helper.getLevel().damageSources().generic(), 1.0F);

        helper.succeedWhen(() -> helper.assertTrue(
                victim.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS),
                "a nearby victim never went blind after the flashbug was provoked"));
    }

    /** Bounded the same way the toad's cloud is: one release, then quiet until the cooldown ends. */
    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void flashbugCannotRefireDuringCooldown(GameTestHelper helper) {
        com.carro1001.mhnw.entity.Flashbug flashbug = helper.spawn(ModEntities.FLASHBUG.get(), 8, 2, 8);

        flashbug.hurt(helper.getLevel().damageSources().generic(), 1.0F);

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        flashbug.isFlashing(), "waiting for the first flash to actually start"))
                .thenWaitUntil(() -> helper.assertTrue(
                        !flashbug.isFlashing(), "waiting for the first flash to finish releasing"))
                .thenExecute(() -> flashbug.hurt(helper.getLevel().damageSources().generic(), 1.0F))
                .thenExecuteFor(40, () -> helper.assertTrue(!flashbug.isFlashing(),
                        "the flashbug flashed again while still on cooldown from the first"))
                .thenSucceed();
    }

    // ---------------------------------------------------------------- Bug (P3, ambient life)

    /** A02: the variant is a saved gameplay fact and must survive the same NBT round trip a real
     * reload goes through, same as the toad's. */
    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void bugVariantPersistsAcrossReload(GameTestHelper helper) {
        com.carro1001.mhnw.entity.Bug original = helper.spawn(ModEntities.BUG.get(), 8, 2, 8);
        original.setVariant(com.carro1001.mhnw.entity.Bug.Variant.GODBUG);

        CompoundTag saved = new CompoundTag();
        original.addAdditionalSaveData(saved);

        com.carro1001.mhnw.entity.Bug reloaded =
                new com.carro1001.mhnw.entity.Bug(ModEntities.BUG.get(), helper.getLevel());
        reloaded.readAdditionalSaveData(saved);

        helper.assertTrue(reloaded.getVariant() == com.carro1001.mhnw.entity.Bug.Variant.GODBUG,
                "reloaded bug had variant " + reloaded.getVariant() + ", expected GODBUG");
        helper.succeed();
    }

    /** A13: an ordinary ambient mob death removes it, exactly once. */
    @GameTest(template = ARENA, timeoutTicks = 120)
    public static void bugDeathRemovesIt(GameTestHelper helper) {
        com.carro1001.mhnw.entity.Bug bug = helper.spawn(ModEntities.BUG.get(), 8, 2, 8);
        bug.hurt(helper.getLevel().damageSources().genericKill(), Float.MAX_VALUE);

        helper.succeedWhen(() -> helper.assertTrue(
                bug.isRemoved(), "the bug was not removed after dying"));
    }

    // ---------------------------------------------------------------- Izuchi (P4, small monster)

    /**
     * A03/A05: unlike every P3 species, Izuchi is genuinely hostile: ordinary vanilla
     * {@code MeleeAttackGoal} against a player-shaped target, dealing damage through
     * {@code Mob.doHurtTarget}, no custom timeline. This is the one concrete proof that "simple
     * independent targeting" actually connects.
     */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void izuchiAttacksAndDamagesTarget(GameTestHelper helper) {
        com.carro1001.mhnw.entity.Izuchi izuchi = helper.spawn(ModEntities.IZUCHI.get(), 8, 2, 8);
        Cow victim = helper.spawn(EntityType.COW, 8, 2, 9);
        victim.setNoAi(true);

        izuchi.setTarget(victim);
        float startingHealth = victim.getHealth();

        helper.succeedWhen(() -> helper.assertTrue(victim.getHealth() < startingHealth,
                "Izuchi never damaged a target standing right next to it"));
    }

    /** A13: death removes it, same as every other species. */
    @GameTest(template = ARENA, timeoutTicks = 120)
    public static void izuchiDeathRemovesIt(GameTestHelper helper) {
        com.carro1001.mhnw.entity.Izuchi izuchi = helper.spawn(ModEntities.IZUCHI.get(), 8, 2, 8);
        izuchi.hurt(helper.getLevel().damageSources().genericKill(), Float.MAX_VALUE);

        helper.succeedWhen(() -> helper.assertTrue(
                izuchi.isRemoved(), "Izuchi was not removed after dying"));
    }

    /**
     * The sleep goal's precondition, checked directly rather than waiting on its 1-in-1200 random
     * trigger to fire inside a bounded test: it must never offer to start while a target already
     * exists, which is what lets ordinary goal-selector preemption wake a sleeping Izuchi the
     * moment {@code NearestAttackableTargetGoal} or {@code HurtByTargetGoal} sets one, with no
     * custom "check for threats" logic of its own (see the goal's own doc comment).
     */
    // ---------------------------------------------------------------- Rathian (P4, ground wyvern)

    /**
     * A03: the same multipart contract Great Izuchi proved works generically for the shared
     * {@code MonsterPart} class, checked again for a second, independently constructed owner
     * rather than assumed to still hold.
     */
    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void rathianPartDamageReachesParent(GameTestHelper helper) {
        com.carro1001.mhnw.entity.Rathian rathian = helper.spawn(ModEntities.RATHIAN.get(), 8, 2, 8);
        rathian.setNoAi(true);
        float before = rathian.getHealth();

        MonsterPart head = rathian.part("head");
        head.hurt(helper.getLevel().damageSources().generic(), PROBE_DAMAGE);

        float lost = before - rathian.getHealth();
        helper.assertTrue(Math.abs(lost - PROBE_DAMAGE) < EPSILON,
                "hitting Rathian's head should cost the parent exactly " + PROBE_DAMAGE
                        + " health, but it lost " + lost);
        helper.succeed();
    }

    /** A03/A06: one source touching several of Rathian's seven parts is still one hit. */
    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void rathianOneSourceAcrossManyPartsCountsOnce(GameTestHelper helper) {
        com.carro1001.mhnw.entity.Rathian rathian = helper.spawn(ModEntities.RATHIAN.get(), 8, 2, 8);
        rathian.setNoAi(true);
        float before = rathian.getHealth();

        DamageSource explosion = helper.getLevel().damageSources().generic();
        rathian.part("torso").hurt(explosion, PROBE_DAMAGE);
        rathian.part("head").hurt(explosion, PROBE_DAMAGE);
        rathian.part("tail_end").hurt(explosion, PROBE_DAMAGE);
        rathian.part("stinger").hurt(explosion, PROBE_DAMAGE);

        float lost = before - rathian.getHealth();
        helper.assertTrue(Math.abs(lost - PROBE_DAMAGE) < EPSILON,
                "one source touching four of Rathian's parts should cost " + PROBE_DAMAGE
                        + " health once, but the parent lost " + lost);
        helper.succeed();
    }

    /**
     * A03/A06: this is the regression test for a real gap found on review, not a hypothetical, and
     * the second hit is deliberately the smaller one for the same reason the Great Izuchi version
     * of this test now is: a larger second hit passes regardless of whether source identity is
     * honoured, since vanilla's own invulnerability lets the difference through either way. A
     * smaller second hit from a genuinely different attacker is the case vanilla's own amount-only
     * comparison silently drops on its own, which {@code Rathian.hurt}'s {@code invulnerableTime}
     * reset exists to fix.
     */
    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void rathianDistinctSourcesAreNotConflated(GameTestHelper helper) {
        com.carro1001.mhnw.entity.Rathian rathian = helper.spawn(ModEntities.RATHIAN.get(), 8, 2, 8);
        rathian.setNoAi(true);
        float before = rathian.getHealth();

        rathian.part("torso").hurt(helper.getLevel().damageSources().generic(), PROBE_DAMAGE * 2);
        rathian.part("head").hurt(helper.getLevel().damageSources().magic(), PROBE_DAMAGE);

        float lost = before - rathian.getHealth();
        float expected = PROBE_DAMAGE * 2 + PROBE_DAMAGE;
        helper.assertTrue(Math.abs(lost - expected) < EPSILON,
                "two distinct attackers, the second dealing less than the first, should both land in"
                        + " full for " + expected + " total, but Rathian lost " + lost);
        helper.succeed();
    }

    /** A05: genuinely hostile, unlike every P3 species, using ordinary vanilla melee. */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void rathianAttacksAndDamagesTarget(GameTestHelper helper) {
        com.carro1001.mhnw.entity.Rathian rathian = helper.spawn(ModEntities.RATHIAN.get(), 8, 2, 8);
        Cow victim = helper.spawn(EntityType.COW, 8, 2, 9);
        victim.setNoAi(true);

        rathian.setTarget(victim);
        float startingHealth = victim.getHealth();

        helper.succeedWhen(() -> helper.assertTrue(victim.getHealth() < startingHealth,
                "Rathian never damaged a target standing right next to it"));
    }

    /** A02/A13: death removes Rathian and every one of its seven parts, exactly once. */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void rathianDeathRemovesTheWholeCreature(GameTestHelper helper) {
        com.carro1001.mhnw.entity.Rathian rathian = helper.spawn(ModEntities.RATHIAN.get(), 8, 2, 8);
        rathian.setNoAi(true);
        int partCount = rathian.getParts().length;
        helper.assertTrue(partCount == 7, "Rathian registered " + partCount + " parts, expected 7");

        rathian.hurt(helper.getLevel().damageSources().genericKill(), Float.MAX_VALUE);

        helper.succeedWhen(() -> {
            helper.assertTrue(rathian.isRemoved(), "Rathian was not removed after dying");
            for (MonsterPart part : rathian.monsterParts()) {
                helper.assertTrue(part.isRemoved() || !part.isAddedToLevel(),
                        "part " + part.partName + " outlived its parent");
            }
        });
    }

    // ---------------------------------------------------------------- Rathalos (P4, ground wyvern)

    /** A03: the shared multipart contract, checked for a third independently constructed owner. */
    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void rathalosPartDamageReachesParent(GameTestHelper helper) {
        com.carro1001.mhnw.entity.Rathalos rathalos = helper.spawn(ModEntities.RATHALOS.get(), 8, 2, 8);
        rathalos.setNoAi(true);
        float before = rathalos.getHealth();

        MonsterPart head = rathalos.part("head");
        head.hurt(helper.getLevel().damageSources().generic(), PROBE_DAMAGE);

        float lost = before - rathalos.getHealth();
        helper.assertTrue(Math.abs(lost - PROBE_DAMAGE) < EPSILON,
                "hitting Rathalos's head should cost the parent exactly " + PROBE_DAMAGE
                        + " health, but it lost " + lost);
        helper.succeed();
    }

    /**
     * A03/A06: the same fairness case Rathian's version guards, with the second hit again the
     * smaller of the two so the test actually discriminates the fix from vanilla's default.
     */
    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void rathalosDistinctSourcesAreNotConflated(GameTestHelper helper) {
        com.carro1001.mhnw.entity.Rathalos rathalos = helper.spawn(ModEntities.RATHALOS.get(), 8, 2, 8);
        rathalos.setNoAi(true);
        float before = rathalos.getHealth();

        rathalos.part("torso").hurt(helper.getLevel().damageSources().generic(), PROBE_DAMAGE * 2);
        rathalos.part("head").hurt(helper.getLevel().damageSources().magic(), PROBE_DAMAGE);

        float lost = before - rathalos.getHealth();
        float expected = PROBE_DAMAGE * 2 + PROBE_DAMAGE;
        helper.assertTrue(Math.abs(lost - expected) < EPSILON,
                "two distinct attackers, the second dealing less than the first, should both land in"
                        + " full for " + expected + " total, but Rathalos lost " + lost);
        helper.succeed();
    }

    /** A05: genuinely hostile using ordinary vanilla melee, same interim Rathian and Izuchi use. */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void rathalosAttacksAndDamagesTarget(GameTestHelper helper) {
        com.carro1001.mhnw.entity.Rathalos rathalos = helper.spawn(ModEntities.RATHALOS.get(), 8, 2, 8);
        Cow victim = helper.spawn(EntityType.COW, 8, 2, 9);
        victim.setNoAi(true);

        rathalos.setTarget(victim);
        float startingHealth = victim.getHealth();

        helper.succeedWhen(() -> helper.assertTrue(victim.getHealth() < startingHealth,
                "Rathalos never damaged a target standing right next to it"));
    }

    /** A02/A13: death removes Rathalos and every one of its six parts, exactly once. */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void rathalosDeathRemovesTheWholeCreature(GameTestHelper helper) {
        com.carro1001.mhnw.entity.Rathalos rathalos = helper.spawn(ModEntities.RATHALOS.get(), 8, 2, 8);
        rathalos.setNoAi(true);
        int partCount = rathalos.getParts().length;
        helper.assertTrue(partCount == 6, "Rathalos registered " + partCount + " parts, expected 6");

        rathalos.hurt(helper.getLevel().damageSources().genericKill(), Float.MAX_VALUE);

        helper.succeedWhen(() -> {
            helper.assertTrue(rathalos.isRemoved(), "Rathalos was not removed after dying");
            for (MonsterPart part : rathalos.monsterParts()) {
                helper.assertTrue(part.isRemoved() || !part.isAddedToLevel(),
                        "part " + part.partName + " outlived its parent");
            }
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void izuchiWontStartSleepingWithATarget(GameTestHelper helper) {
        com.carro1001.mhnw.entity.Izuchi izuchi = helper.spawn(ModEntities.IZUCHI.get(), 8, 2, 8);
        Cow victim = helper.spawn(EntityType.COW, 8, 2, 9);
        izuchi.setTarget(victim);

        com.carro1001.mhnw.entity.IzuchiSleepGoal goal =
                new com.carro1001.mhnw.entity.IzuchiSleepGoal(izuchi);
        helper.assertTrue(!goal.canUse(),
                "the sleep goal was willing to start while Izuchi already had a target");
        helper.succeed();
    }
}
