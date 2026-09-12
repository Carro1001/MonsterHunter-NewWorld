package com.carro1001.mhnw;

import com.carro1001.mhnw.animation.ServerTimedAnimationController;
import com.carro1001.mhnw.entity.Aptonoth;
import com.carro1001.mhnw.entity.AttackProfile;
import com.carro1001.mhnw.entity.GreatIzuchi;
import com.carro1001.mhnw.entity.GreatIzuchiCombatGoal;
import com.carro1001.mhnw.entity.HuntingSpawnRules;
import com.carro1001.mhnw.entity.Lagiacrus;
import com.carro1001.mhnw.entity.LagiacrusPursuitGoal;
import com.carro1001.mhnw.entity.MonsterPart;
import com.carro1001.mhnw.entity.Toad;
import com.carro1001.mhnw.registry.ModBiomes;
import com.carro1001.mhnw.registry.ModEntities;
import com.carro1001.mhnw.worldgen.HuntingGroundsRegion;
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

    /** Real-tick slack allowed when observing a goal-driven countdown: vanilla's goal selector
     * starts and stops goals on its every-other-tick poll, and a sequence step lands a tick after
     * the one that satisfied it. Deliberately far below the doubling a half-rate countdown
     * produces, so T01 still fails if {@code RoarGoal.requiresUpdateEveryTick()} is removed. */
    private static final int SCHEDULING_TOLERANCE = 2;

    /** Mirrors {@code RoarGoal.DISENGAGE_TICKS}: how long with no target before the next
     * engagement roars again. Not imported -- the goal keeps it private, and a test that read the
     * production constant could not tell a changed interval from a broken one. */
    private static final int REARM_TICKS = 100;

    /** Re-arming is decided inside {@code canUse()}, which vanilla polls only every other tick --
     * once to notice the loss and once to act on it. Four ticks of slack, no more (T02). */
    private static final int REARM_POLL_TOLERANCE = 4;

    private static GreatIzuchi spawnInert(GameTestHelper helper) {
        GreatIzuchi monster = helper.spawn(ModEntities.GREAT_IZUCHI.get(), 8, 2, 8);
        monster.setNoAi(true);
        monster.setInvulnerable(false);
        return monster;
    }

    /**
     * A summoned Great Izuchi (spawn egg, {@code /summon}) does not drag escorts along; only a
     * genuine wild spawn does (see {@link GreatIzuchi#finalizeSpawn}).
     */
    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void greatIzuchiMobSummonedSpawnBringsNoEscort(GameTestHelper helper) {
        GreatIzuchi monster = helper.spawn(ModEntities.GREAT_IZUCHI.get(), 8, 2, 8);
        monster.finalizeSpawn(helper.getLevel(),
                helper.getLevel().getCurrentDifficultyAt(monster.blockPosition()),
                net.minecraft.world.entity.MobSpawnType.MOB_SUMMONED, null);

        java.util.List<com.carro1001.mhnw.entity.Izuchi> escorts = helper.getLevel().getEntitiesOfClass(
                com.carro1001.mhnw.entity.Izuchi.class, monster.getBoundingBox().inflate(8.0D));
        helper.assertTrue(escorts.isEmpty(),
                "a spawn-egg/summoned Great Izuchi brought " + escorts.size() + " escorts along");
        helper.succeed();
    }

    /**
     * T01: the MHW-style opening roar fires the moment a target is first acquired, runs down at
     * exactly one tick per <em>real</em> tick for its species' whole authored clip, and blocks any
     * attack for that whole time -- the roar goal outranks the combat goal for exactly this reason
     * (see {@code RoarGoal}'s own doc).
     *
     * <p>This is the regression for the bug {@code RoarGoal.requiresUpdateEveryTick()} fixes.
     * Without that override, {@code Mob.serverAiStep} ticks a running goal only every other real
     * tick, so the countdown takes roughly twice the clip's length: the clip finishes and holds its
     * last authored frame long before the goal lets go, which reads live as "froze after the roar."
     * Nothing here calls {@code RoarGoal.tick()} directly -- a direct call ticks at whatever rate
     * the test chose and could never catch vanilla's own scheduling. The deadline is anchored to
     * the first countdown value actually observed, not to an assumed global test tick, and the
     * per-tick rate check means a doubled countdown fails long before any timeout expires.
     *
     * @param attacking species-specific "is mid-attack" probe; Rathalos has no synced attack id
     *                  (it uses vanilla melee), so it relies on the victim-health check instead.
     */
    private static <T extends net.minecraft.world.entity.Mob & com.carro1001.mhnw.entity.Roarable>
            void assertRoarRunsAtOneTickPerRealTick(GameTestHelper helper, T monster, Cow victim,
                    java.util.function.BooleanSupplier attacking) {
        long[] startTime = {0L};
        int[] startRemaining = {0};
        float[] victimHealth = {0.0F};
        long[] finishedAfter = {-1L};

        helper.startSequence()
                .thenWaitUntil(() -> {
                    monster.setTarget(victim);
                    helper.assertTrue(monster.getRoarTicks() > 0, "never started roaring on first engagement");
                    // Recorded on the one invocation that passes, so these are the first positive
                    // countdown actually observed and the real game time it was observed at.
                    startTime[0] = helper.getLevel().getGameTime();
                    startRemaining[0] = monster.getRoarTicks();
                    victimHealth[0] = victim.getHealth();
                })
                .thenExecute(() -> {
                    helper.assertTrue(monster.hasRoaredThisEngagement(),
                            "hasRoaredThisEngagement wasn't set once roaring started");
                    helper.assertTrue(startRemaining[0] >= monster.roarDurationTicks() - SCHEDULING_TOLERANCE,
                            "the roar started at " + startRemaining[0] + " ticks, not this species'"
                                    + " clip length of " + monster.roarDurationTicks());
                })
                .thenExecuteFor(monster.roarDurationTicks() + SCHEDULING_TOLERANCE, () -> {
                    long elapsed = helper.getLevel().getGameTime() - startTime[0];
                    int remaining = monster.getRoarTicks();
                    if (remaining <= 0) {
                        if (finishedAfter[0] < 0L) {
                            finishedAfter[0] = elapsed;
                        }
                        return;
                    }
                    helper.assertTrue(Math.abs((startRemaining[0] - elapsed) - remaining) <= SCHEDULING_TOLERANCE,
                            "the roar countdown is not running at one tick per real tick: " + elapsed
                                    + " real ticks in, " + remaining + " left of " + startRemaining[0]);
                    helper.assertTrue(!attacking.getAsBoolean(), "started an attack while still roaring");
                    helper.assertTrue(victim.getHealth() >= victimHealth[0] - EPSILON,
                            "damaged its target while still roaring: " + victimHealth[0] + " -> "
                                    + victim.getHealth());
                })
                .thenExecute(() -> helper.assertTrue(
                        finishedAfter[0] >= 0L
                                && Math.abs(finishedAfter[0] - startRemaining[0]) <= SCHEDULING_TOLERANCE,
                        "a roar with " + startRemaining[0] + " ticks left finished after "
                                + finishedAfter[0] + " real ticks (-1 means it never finished)"))
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void greatIzuchiRoarRunsForItsRealClipLength(GameTestHelper helper) {
        GreatIzuchi monster = helper.spawn(ModEntities.GREAT_IZUCHI.get(), 8, 2, 8);
        Cow victim = helper.spawn(EntityType.COW, 8, 2, 10);
        victim.setNoAi(true);
        monster.setTarget(victim);

        assertRoarRunsAtOneTickPerRealTick(helper, monster, victim,
                () -> monster.getAttackId() != GreatIzuchi.ATTACK_NONE);
    }

    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void rathianRoarRunsForItsRealClipLength(GameTestHelper helper) {
        com.carro1001.mhnw.entity.Rathian rathian = helper.spawn(ModEntities.RATHIAN.get(), 8, 2, 8);
        // Inside RathianCombatGoal.BITE_RIGHT's real 2.0-6.0 range band, so "no attack while
        // roaring" is a claim about the roar and not about the victim being out of reach.
        Cow victim = helper.spawn(EntityType.COW, 8, 2, 12);
        victim.setNoAi(true);
        rathian.setTarget(victim);

        assertRoarRunsAtOneTickPerRealTick(helper, rathian, victim,
                () -> rathian.getAttackId() != com.carro1001.mhnw.entity.Rathian.ATTACK_NONE);
    }

    /** Third real {@code Roarable}. Rathalos fights with vanilla melee and has no synced attack id
     * to inspect, so the victim's health is the whole "didn't attack while roaring" probe here --
     * inventing a {@code Rathalos.getAttackId()} for a test would be inventing production API. */
    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void rathalosRoarRunsForItsRealClipLength(GameTestHelper helper) {
        com.carro1001.mhnw.entity.Rathalos rathalos = helper.spawn(ModEntities.RATHALOS.get(), 8, 2, 8);
        Cow victim = helper.spawn(EntityType.COW, 8, 2, 9);
        victim.setNoAi(true);
        rathalos.setTarget(victim);

        assertRoarRunsAtOneTickPerRealTick(helper, rathalos, victim, () -> false);
    }

    /**
     * T02: the re-arm half of the same mechanic, at its real interval rather than "eventually."
     *
     * <p>Three things have to hold, and only the first was previously covered: an engaged monster
     * never re-arms; a brief loss and re-acquisition (a dodge, a moment out of line of sight) does
     * not re-arm <em>and restarts the clock</em>; and a genuinely sustained loss re-arms at the
     * real 100-tick interval, not at double it. The window is measured from the game time the
     * target was actually cleared, so a per-call tick counter inside {@code canUse()} -- which
     * vanilla polls every other tick, and which would therefore take ~200 real ticks -- fails the
     * upper bound instead of passing a generous timeout.
     */
    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void greatIzuchiReArmsRoarOnlyAfterTheRealDisengageInterval(GameTestHelper helper) {
        GreatIzuchi monster = helper.spawn(ModEntities.GREAT_IZUCHI.get(), 8, 2, 8);
        Cow victim = helper.spawn(EntityType.COW, 8, 2, 10);
        victim.setNoAi(true);
        victim.setInvulnerable(true); // the fight itself is not what this test is about
        monster.setRoaredThisEngagement(true); // a fight that already roared and is about to end
        monster.setTarget(victim);
        // Engaged and approaching, but never committing an attack. A committed action makes
        // GreatIzuchiCombatGoal non-interruptable, and vanilla's GoalSelector will not even call a
        // blocked goal's canUse() -- which is where RoarGoal's disengage clock lives, so the clock
        // simply does not start until that action finishes. Measured live at 161 ticks instead of
        // 100 when the target was dropped mid-swing; that costs at most one action's worth of extra
        // delay before re-arming and is recorded in docs/DEFERRED.md rather than patched here.
        monster.attackCooldown = 10_000;

        long[] lostAt = {0L};
        Runnable stillArmed = () -> helper.assertTrue(monster.hasRoaredThisEngagement(),
                "re-armed the opening roar too early");

        helper.startSequence()
                .thenExecuteFor(10, () -> {
                    monster.setTarget(victim);
                    stillArmed.run();
                })
                // A brief loss: long enough for vanilla's every-other-tick poll to see it, far
                // short of the disengage interval.
                .thenExecute(() -> monster.setTarget(null))
                .thenExecuteFor(40, stillArmed)
                // Re-acquired. If the clock does not restart here, the 40 ticks above still count
                // and the sustained-loss window below re-arms ~40 ticks early -- which the
                // no-early-re-arm check then fails.
                .thenExecute(() -> monster.setTarget(victim))
                .thenExecuteFor(6, stillArmed)
                .thenExecute(() -> {
                    monster.setTarget(null);
                    lostAt[0] = helper.getLevel().getGameTime();
                })
                .thenExecuteFor(REARM_TICKS - REARM_POLL_TOLERANCE, stillArmed)
                .thenWaitUntil(() -> helper.assertTrue(!monster.hasRoaredThisEngagement(),
                        "never re-armed after a sustained loss of target"))
                .thenExecute(() -> {
                    long elapsed = helper.getLevel().getGameTime() - lostAt[0];
                    helper.assertTrue(elapsed <= REARM_TICKS + REARM_POLL_TOLERANCE,
                            "re-arming took " + elapsed + " real ticks, not the " + REARM_TICKS
                                    + "-tick disengage interval (+" + REARM_POLL_TOLERANCE
                                    + " ticks of polling slack)");
                })
                .thenSucceed();
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

        monster.setRoaredThisEngagement(true); // skips the intro roar; this test is not about it
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

        monster.setRoaredThisEngagement(true); // skips the intro roar; this test is not about it
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

        monster.setRoaredThisEngagement(true); // skips the intro roar; this test is not about it
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

        monster.setRoaredThisEngagement(true); // skips the intro roar; this test is not about it
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

        monster.setRoaredThisEngagement(true); // skips the intro roar; this test is not about it
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
     * T04: the same contract through the <em>full</em> vanilla entity save/load path, not just the
     * mod's own additional save data.
     *
     * <p>Replaces an earlier {@code reloadCancelsTransientCombatState}, which round-tripped
     * {@code addAdditionalSaveData} alone and could not show whether a vanilla field survives; every
     * assertion it made is a subset of these. This goes through {@code saveWithoutId}/{@code load}
     * on a fresh instance that actually enters the level -- the same path
     * {@code lagiacrusReloadCancelsPursuitAndPreservesHealth} uses -- starting from a deliberately
     * reduced, non-default health and a live transient state, and disposes of the original first so
     * no duplicate UUID or duplicate part identity enters the level.
     *
     * <p>Section 4.3 rule 7: loading a creature must cancel transient combat and impose a short
     * cooldown rather than replaying an interrupted action. Parts are rebuilt fresh in the
     * constructor rather than read from NBT, so a genuine part-list bug (the old implementation's
     * list that only ever appended, never replaced, a later part of the same type) shows up here as
     * a wrong count or a wrong name.
     *
     * <p>The two fixtures deliberately save in <em>different</em> transient states: Great Izuchi
     * mid-attack, Rathian mid-roar. Saving both mid-attack would leave {@code getRoarTicks()}
     * already zero at save time, making the roar assertion unable to fail.
     *
     * <p>Not a claim about a real disk restart: that stays an R0b gate.
     */
    private static void assertPartsSurviveReload(GameTestHelper helper, MonsterPart[] original,
                                                 MonsterPart[] reloaded) {
        helper.assertTrue(original.length > 0, "the subject registered no parts at all");
        helper.assertTrue(reloaded.length == original.length,
                "reload produced " + reloaded.length + " parts, expected " + original.length);
        for (int i = 0; i < original.length; i++) {
            helper.assertTrue(reloaded[i].partName.equals(original[i].partName),
                    "reloaded part " + i + " is " + reloaded[i].partName + ", expected "
                            + original[i].partName);
            helper.assertTrue(helper.getLevel().getPartEntities().contains(reloaded[i]),
                    "reloaded part " + reloaded[i].partName + " never reached the NeoForge lookup");
        }
        assertPartsUnregistered(helper, original);
    }

    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void greatIzuchiFullReloadKeepsHealthAndPartsButNotTheAction(GameTestHelper helper) {
        GreatIzuchi original = helper.spawn(ModEntities.GREAT_IZUCHI.get(), 8, 2, 8);
        Cow victim = helper.spawn(EntityType.COW, 8, 2, 10);
        victim.setNoAi(true);
        victim.setInvulnerable(true);

        original.setRoaredThisEngagement(true); // skips the intro roar; T01 owns that
        original.setTarget(victim);
        original.attackCooldown = 0;
        original.setHealth(17.0F); // deliberately not the 40.0 default
        MonsterPart[] originalParts = original.monsterParts();

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(original.getAttackId() != GreatIzuchi.ATTACK_NONE,
                        "waiting for an attack to start"))
                .thenExecute(() -> {
                    helper.assertTrue(original.getHealth() == 17.0F,
                            "the fixture's own health changed before saving: " + original.getHealth());
                    CompoundTag saved = original.saveWithoutId(new CompoundTag());
                    original.discard();

                    GreatIzuchi reloaded = new GreatIzuchi(ModEntities.GREAT_IZUCHI.get(), helper.getLevel());
                    reloaded.load(saved);
                    helper.getLevel().addFreshEntity(reloaded);

                    helper.assertTrue(reloaded.getHealth() == 17.0F,
                            "reload lost parent health: " + reloaded.getHealth() + " instead of 17.0");
                    helper.assertTrue(reloaded.getAttackId() == GreatIzuchi.ATTACK_NONE,
                            "a reloaded monster resumed attack id " + reloaded.getAttackId());
                    helper.assertTrue(reloaded.getRoarTicks() == 0,
                            "a reloaded monster resumed a roar countdown of " + reloaded.getRoarTicks());
                    helper.assertTrue(reloaded.attackCooldown > 0,
                            "a reloaded monster had no cooldown at all, so it could attack instantly");
                    assertPartsSurviveReload(helper, originalParts, reloaded.monsterParts());
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void rathianFullReloadKeepsHealthAndPartsButNotTheRoar(GameTestHelper helper) {
        com.carro1001.mhnw.entity.Rathian original = helper.spawn(ModEntities.RATHIAN.get(), 8, 2, 8);
        Cow victim = helper.spawn(EntityType.COW, 8, 2, 12); // BITE_RIGHT's range band
        victim.setNoAi(true);
        victim.setInvulnerable(true);

        // Saved mid-ROAR, not mid-attack: an attack fixture has getRoarTicks() == 0 at save time,
        // so its roar assertion could not fail. Great Izuchi's sibling test covers the attack half.
        original.setTarget(victim);
        original.setHealth(41.0F); // deliberately not the 90.0 default
        MonsterPart[] originalParts = original.monsterParts();

        helper.startSequence()
                .thenWaitUntil(() -> {
                    original.setTarget(victim);
                    helper.assertTrue(original.getRoarTicks() > 0, "waiting for the opening roar");
                })
                .thenExecute(() -> {
                    helper.assertTrue(original.getRoarTicks() > 0,
                            "the roar ended before the fixture could save mid-roar");
                    helper.assertTrue(original.getHealth() == 41.0F,
                            "the fixture's own health changed before saving: " + original.getHealth());
                    CompoundTag saved = original.saveWithoutId(new CompoundTag());
                    original.discard();

                    com.carro1001.mhnw.entity.Rathian reloaded =
                            new com.carro1001.mhnw.entity.Rathian(ModEntities.RATHIAN.get(), helper.getLevel());
                    reloaded.load(saved);
                    helper.getLevel().addFreshEntity(reloaded);

                    helper.assertTrue(reloaded.getHealth() == 41.0F,
                            "reload lost parent health: " + reloaded.getHealth() + " instead of 41.0");
                    helper.assertTrue(reloaded.getRoarTicks() == 0,
                            "a reloaded Rathian resumed the roar it was saved mid-way through, with "
                                    + reloaded.getRoarTicks() + " ticks left");
                    helper.assertTrue(reloaded.getAttackId() == com.carro1001.mhnw.entity.Rathian.ATTACK_NONE,
                            "a reloaded Rathian resumed attack id " + reloaded.getAttackId());
                    helper.assertTrue(reloaded.attackCooldown > 0,
                            "a reloaded Rathian had no cooldown at all, so it could attack instantly");
                    assertPartsSurviveReload(helper, originalParts, reloaded.monsterParts());
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
            assertPartsUnregistered(helper, monster.monsterParts());
        });
    }

    /**
     * T05: a part is gone when NeoForge's own lookup no longer holds it, which is the thing melee
     * picking, projectiles and area damage actually scan. Native parts can be unregistered without
     * their own {@code isRemoved} flag ever being set, so flag inspection alone is a weaker claim
     * than it looks; this is the assertion {@code lagiacrusDeathStopsPursuitAndUnregistersParts}
     * already relied on, applied to every multipart species' removal scenarios.
     */
    private static void assertPartsUnregistered(GameTestHelper helper, MonsterPart[] parts) {
        helper.assertTrue(parts.length > 0, "the subject registered no parts at all");
        for (MonsterPart part : parts) {
            helper.assertTrue(!helper.getLevel().getPartEntities().contains(part),
                    "part outlived its parent in NeoForge lookup: " + part.partName);
        }
    }

    /**
     * T03: a dead monster is inert for the whole time its corpse is held, then leaves completely.
     *
     * <p>What this deliberately does <em>not</em> assert is that the synced attack id or roar
     * countdown clears itself. It cannot: {@code LivingEntity.travel()} stops calling
     * {@code serverAiStep()} once {@code isDeadOrDying()} is true, so from the instant death begins
     * no goal is ticked again -- not the combat goal, not {@code RoarGoal}, not their own
     * {@code isAlive()} guards. Those fields simply freeze at whatever they held. That is harmless
     * because {@code mainAnim} checks death before reading either one, and R1b will deliberately
     * change corpse retention anyway. What matters, and what is asserted here, is that the corpse
     * deals no further damage and that it and its parts really do leave.
     */
    private static void assertDeadSubjectGoesInert(GameTestHelper helper,
            net.minecraft.gametest.framework.GameTestSequence sequence,
            net.minecraft.world.entity.Mob subject, MonsterPart[] parts, Cow victim, int holdTicks) {
        float[] victimHealth = {0.0F};
        sequence
                .thenExecute(() -> {
                    helper.assertTrue(subject.isAlive(), "the fixture's subject was already dead");
                    subject.hurt(helper.getLevel().damageSources().genericKill(), Float.MAX_VALUE);
                    helper.assertTrue(subject.isDeadOrDying(), "the lethal hit did not start death");
                    // Only damage dealt *after* the lethal action counts; the victim is kept out of
                    // harm's way until this point so nothing else in the fixture can move its health.
                    victim.setInvulnerable(false);
                    victimHealth[0] = victim.getHealth();
                    helper.assertTrue(victim.isAlive(), "the fixture's victim died before the subject");
                })
                .thenExecuteFor(holdTicks, () -> helper.assertTrue(
                        victim.getHealth() >= victimHealth[0] - EPSILON,
                        "a dead subject kept dealing melee damage: its target went from "
                                + victimHealth[0] + " to " + victim.getHealth()))
                .thenWaitUntil(() -> helper.assertTrue(subject.isRemoved(),
                        "the corpse was never removed"))
                .thenExecute(() -> assertPartsUnregistered(helper, parts))
                .thenSucceed();
    }

    /** T03, Great Izuchi killed mid-roar, held for its own 38-tick death clip before vanilla
     * removal. Reached through ordinary AI, not by writing the roar countdown directly. */
    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void greatIzuchiKilledMidRoarLeavesAnInertCorpse(GameTestHelper helper) {
        GreatIzuchi monster = helper.spawn(ModEntities.GREAT_IZUCHI.get(), 8, 2, 8);
        Cow victim = helper.spawn(EntityType.COW, 8, 2, 10);
        victim.setNoAi(true);
        victim.setInvulnerable(true);
        monster.setTarget(victim);
        MonsterPart[] parts = monster.monsterParts();

        assertDeadSubjectGoesInert(helper,
                helper.startSequence().thenWaitUntil(() -> {
                    monster.setTarget(victim);
                    helper.assertTrue(monster.isRoaring(), "waiting for the opening roar");
                }),
                monster, parts, victim, GreatIzuchi.DEATH_ANIMATION_TICKS + 30);
    }

    /** T03, Rathian killed mid-attack: its measured bite timeline must not keep landing contacts
     * out of a corpse. The roar is armed off deliberately -- this one is about the attack. */
    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void rathianKilledMidAttackLeavesAnInertCorpse(GameTestHelper helper) {
        com.carro1001.mhnw.entity.Rathian rathian = helper.spawn(ModEntities.RATHIAN.get(), 8, 2, 8);
        Cow victim = helper.spawn(EntityType.COW, 8, 2, 12); // BITE_RIGHT's range band
        victim.setNoAi(true);
        victim.setInvulnerable(true);
        rathian.setRoaredThisEngagement(true);
        rathian.attackCooldown = 0;
        rathian.setTarget(victim);
        MonsterPart[] parts = rathian.monsterParts();

        assertDeadSubjectGoesInert(helper,
                helper.startSequence().thenWaitUntil(() -> {
                    rathian.setTarget(victim);
                    helper.assertTrue(rathian.getAttackId() != com.carro1001.mhnw.entity.Rathian.ATTACK_NONE,
                            "waiting for an attack to start");
                }),
                rathian, parts, victim, 40);
    }

    // ---------------------------------------------------------------- A10 navigation (Great Izuchi)

    /**
     * A10 baseline: open flat ground with nothing in the way. This is the control every other A10
     * test is compared against -- if this one doesn't reach, the others failing means nothing.
     */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void navigatesOpenGroundToReachTarget(GameTestHelper helper) {
        GreatIzuchi monster = helper.spawn(ModEntities.GREAT_IZUCHI.get(), 2, 2, 8);
        Cow victim = helper.spawn(EntityType.COW, 13, 2, 8);
        victim.setNoAi(true);

        monster.setRoaredThisEngagement(true); // skips the intro roar; this test is not about it
        monster.setTarget(victim);

        helper.succeedWhen(() -> helper.assertTrue(
                monster.distanceTo(victim) < 3.0F,
                "the monster never closed on a target across open ground (still "
                        + monster.distanceTo(victim) + " blocks away)"));
    }

    /**
     * A10: an outside corner. The straight line from monster to victim is blocked by a wall with a
     * 90-degree turn in it, so reaching the victim requires actually routing around the corner, not
     * just walking toward it.
     */
    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void navigatesAroundAnOutsideCorner(GameTestHelper helper) {
        GreatIzuchi monster = helper.spawn(ModEntities.GREAT_IZUCHI.get(), 2, 2, 4);
        Cow victim = helper.spawn(EntityType.COW, 10, 2, 12);
        victim.setNoAi(true);

        // An L-shaped wall between them: a leg blocking the direct line in Z, then a leg blocking the
        // direct line in X, so the only way through is around the outside corner at (6, 8).
        for (int z = 4; z <= 8; z++) {
            for (int y = 2; y <= 6; y++) {
                helper.setBlock(6, y, z, net.minecraft.world.level.block.Blocks.STONE);
            }
        }
        for (int x = 6; x <= 10; x++) {
            for (int y = 2; y <= 6; y++) {
                helper.setBlock(x, y, 8, net.minecraft.world.level.block.Blocks.STONE);
            }
        }

        monster.setRoaredThisEngagement(true); // skips the intro roar; this test is not about it
        monster.setTarget(victim);

        helper.succeedWhen(() -> helper.assertTrue(
                monster.distanceTo(victim) < 3.0F,
                "the monster never routed around the corner to reach its target (still "
                        + monster.distanceTo(victim) + " blocks away)"));
    }

    /**
     * A10: a passage exactly as wide as the body (2 blocks, since {@link GreatIzuchi#BODY_WIDTH} is
     * 1.6 and a passage narrower than that can't be occupied at all). Flanking walls on both sides
     * of a 2-wide corridor must not be treated as blocking.
     */
    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void navigatesABodyWidePassage(GameTestHelper helper) {
        GreatIzuchi monster = helper.spawn(ModEntities.GREAT_IZUCHI.get(), 8, 2, 2);
        Cow victim = helper.spawn(EntityType.COW, 8, 2, 13);
        victim.setNoAi(true);

        for (int z = 5; z <= 10; z++) {
            for (int y = 2; y <= 6; y++) {
                helper.setBlock(6, y, z, net.minecraft.world.level.block.Blocks.STONE);
                helper.setBlock(9, y, z, net.minecraft.world.level.block.Blocks.STONE);
            }
        }

        monster.setRoaredThisEngagement(true); // skips the intro roar; this test is not about it
        monster.setTarget(victim);

        helper.succeedWhen(() -> helper.assertTrue(
                monster.distanceTo(victim) < 3.0F,
                "the monster could not pass through a corridor exactly as wide as its own body (still "
                        + monster.distanceTo(victim) + " blocks away)"));
    }

    /**
     * A10: a passage narrower than the body (1 block, against a 1.6-wide body) genuinely cannot be
     * entered. This isn't asserting the monster reaches the victim -- it can't, physically -- it's
     * asserting the opposite failure mode: no crash, and no endless burst of failed repathing that
     * never lets the monster settle (the concrete bug the handoff's own A10 note names). A monster
     * that gives up and stands still, or wanders without ever entering the gap, both count as sane.
     */
    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void aNarrowerThanBodyPassageDoesNotSoftlockPathing(GameTestHelper helper) {
        GreatIzuchi monster = helper.spawn(ModEntities.GREAT_IZUCHI.get(), 8, 2, 2);
        Cow victim = helper.spawn(EntityType.COW, 8, 2, 13);
        victim.setNoAi(true);

        // A solid wall the width of the arena, one block wide gap at x=8 -- too narrow for a
        // 1.6-wide body to actually stand inside.
        for (int x = 4; x <= 12; x++) {
            if (x == 8) {
                continue;
            }
            for (int y = 2; y <= 6; y++) {
                helper.setBlock(x, y, 7, net.minecraft.world.level.block.Blocks.STONE);
            }
        }

        monster.setRoaredThisEngagement(true); // skips the intro roar; this test is not about it
        monster.setTarget(victim);

        helper.runAtTickTime(280, () -> {
            helper.assertTrue(monster.isAlive() && !monster.isRemoved(),
                    "the monster was removed or died just from failing to path through a gap");
            helper.succeed();
        });
    }

    /** A10: a single-block step, which {@link GreatIzuchi#createAttributes}'s step height of 1.0
     * should climb without needing to jump or path around. */
    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void navigatesOverASingleBlockStep(GameTestHelper helper) {
        GreatIzuchi monster = helper.spawn(ModEntities.GREAT_IZUCHI.get(), 8, 2, 2);
        Cow victim = helper.spawn(EntityType.COW, 8, 3, 10);
        victim.setNoAi(true);

        // Raise the floor by one block from z=6 onward, so the last stretch to the victim is a single
        // step up, not a ramp or staircase.
        for (int x = 4; x <= 12; x++) {
            for (int z = 6; z <= 13; z++) {
                helper.setBlock(x, 2, z, net.minecraft.world.level.block.Blocks.STONE);
            }
        }

        monster.setRoaredThisEngagement(true); // skips the intro roar; this test is not about it
        monster.setTarget(victim);

        helper.succeedWhen(() -> helper.assertTrue(
                monster.distanceTo(victim) < 3.0F,
                "the monster never climbed a single-block step to reach its target (still "
                        + monster.distanceTo(victim) + " blocks away, step height is "
                        + GreatIzuchi.createAttributes().build().getValue(
                                net.minecraft.world.entity.ai.attributes.Attributes.STEP_HEIGHT) + ")"));
    }

    /**
     * A10: a target that cannot be reached at all (fully enclosed) must not produce an unbounded
     * re-path loop -- the handoff calls this out by name as the specific failure mode to guard
     * against. Success here just means the monster survives a long window of continuously wanting
     * a target it can never reach, without crashing, dying, or being removed.
     */
    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void anUnreachableTargetDoesNotProduceAnUnboundedRepathLoop(GameTestHelper helper) {
        GreatIzuchi monster = helper.spawn(ModEntities.GREAT_IZUCHI.get(), 8, 2, 2);
        Cow victim = helper.spawn(EntityType.COW, 8, 2, 12);
        victim.setNoAi(true);

        // Seal the victim in a solid 3x3x3 box with no opening at all.
        for (int x = 7; x <= 9; x++) {
            for (int y = 1; y <= 3; y++) {
                for (int z = 11; z <= 13; z++) {
                    boolean shell = x == 7 || x == 9 || y == 1 || y == 3 || z == 11 || z == 13;
                    if (shell) {
                        helper.setBlock(x, y, z, net.minecraft.world.level.block.Blocks.STONE);
                    }
                }
            }
        }

        monster.setRoaredThisEngagement(true); // skips the intro roar; this test is not about it
        monster.setTarget(victim);

        helper.runAtTickTime(280, () -> {
            helper.assertTrue(monster.isAlive() && !monster.isRemoved(),
                    "the monster was removed or died just from wanting an unreachable target");
            helper.assertTrue(monster.distanceTo(victim) > 2.0F,
                    "the monster reached a target sealed in solid blocks on every side");
            helper.succeed();
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

    /**
     * A03: the same shared {@code MonsterPart} contract Great Izuchi/Rathian/Rathalos already prove,
     * checked again for the first passive, non-{@code Monster} owner (the box shape a single AABB
     * cannot fit is the same problem, not a combat concern; see {@link Aptonoth}'s class doc).
     */
    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void aptonothPartDamageReachesParent(GameTestHelper helper) {
        Aptonoth aptonoth = helper.spawn(ModEntities.APTONOTH.get(), 8, 2, 8);
        aptonoth.setNoAi(true);
        float before = aptonoth.getHealth();

        MonsterPart head = aptonoth.part("head");
        head.hurt(helper.getLevel().damageSources().generic(), PROBE_DAMAGE);

        float lost = before - aptonoth.getHealth();
        helper.assertTrue(Math.abs(lost - PROBE_DAMAGE) < EPSILON,
                "hitting Aptonoth's head should cost the parent exactly " + PROBE_DAMAGE
                        + " health, but it lost " + lost);
        helper.succeed();
    }

    /** A03/A06: one source touching several of Aptonoth's six parts is still one hit. */
    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void aptonothOneSourceAcrossManyPartsCountsOnce(GameTestHelper helper) {
        Aptonoth aptonoth = helper.spawn(ModEntities.APTONOTH.get(), 8, 2, 8);
        aptonoth.setNoAi(true);
        float before = aptonoth.getHealth();

        DamageSource source = helper.getLevel().damageSources().generic();
        aptonoth.part("chest").hurt(source, PROBE_DAMAGE);
        aptonoth.part("head").hurt(source, PROBE_DAMAGE);
        aptonoth.part("tail_1").hurt(source, PROBE_DAMAGE);
        aptonoth.part("tail_3").hurt(source, PROBE_DAMAGE);
        aptonoth.part("tail_4").hurt(source, PROBE_DAMAGE);

        float lost = before - aptonoth.getHealth();
        helper.assertTrue(Math.abs(lost - PROBE_DAMAGE) < EPSILON,
                "one source touching several of Aptonoth's parts should cost " + PROBE_DAMAGE
                        + " health once, but the parent lost " + lost);
        helper.succeed();
    }

    /** A02/A13: death removes Aptonoth and every one of its six parts, exactly once: chest, head,
     * and a four-segment tail. */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void aptonothDeathRemovesTheWholeCreature(GameTestHelper helper) {
        Aptonoth aptonoth = helper.spawn(ModEntities.APTONOTH.get(), 8, 2, 8);
        aptonoth.setNoAi(true);
        int partCount = aptonoth.getParts().length;
        helper.assertTrue(partCount == 6, "Aptonoth registered " + partCount + " parts, expected 6");

        aptonoth.hurt(helper.getLevel().damageSources().genericKill(), Float.MAX_VALUE);

        helper.succeedWhen(() -> {
            helper.assertTrue(aptonoth.isRemoved(), "Aptonoth was not removed after dying");
            assertPartsUnregistered(helper, aptonoth.monsterParts());
        });
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
     * Release is a one-way trip, not a cooldown: {@link ToadFuseGoal#release} discards the toad the
     * same tick it applies its effect, "blowing up" metaphorically rather than going quiet and
     * eventually firing again (that was the old cooldown design; a released toad no longer exists to
     * refire at all).
     */
    @GameTest(template = ARENA, timeoutTicks = 140)
    public static void toadDiscardsItselfAfterReleasing(GameTestHelper helper) {
        Toad toad = helper.spawn(ModEntities.TOAD.get(), 8, 2, 8);
        toad.setVariant(Toad.Variant.POISON);

        toad.hurt(helper.getLevel().damageSources().generic(), 1.0F);

        helper.succeedWhen(() -> helper.assertTrue(
                toad.isRemoved(), "the toad was not discarded after releasing its cloud"));
    }

    /**
     * A toad cannot be killed by being hit: {@link Toad#hurt} always forwards at zero damage, so
     * ordinary damage never drops its health, only its own release ever removes it. This is the
     * regression test for "invincible except to its own explosion" (handoff feedback: make toads
     * un-killable by hitting them, only their release "kills" them).
     */
    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void toadIsNotKilledByBeingHit(GameTestHelper helper) {
        Toad toad = helper.spawn(ModEntities.TOAD.get(), 8, 2, 8);
        float before = toad.getHealth();

        toad.hurt(helper.getLevel().damageSources().generic(), 1000.0F);

        helper.assertTrue(toad.getHealth() == before,
                "a toad's health dropped from being hit; it should be invulnerable to that damage");
        helper.succeed();
    }

    /**
     * Proximity alone must not provoke a toad any more (handoff feedback: only interacting with or
     * hitting it should). {@link EndemicAreaEffectGoal#allowsProximityTrigger} is overridden false
     * for this reason; this is the regression test that override actually reaches {@code canUse()}.
     */
    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void toadDoesNotFuseFromProximityAlone(GameTestHelper helper) {
        Toad toad = helper.spawn(ModEntities.TOAD.get(), 8, 2, 8);
        Cow bystander = helper.spawn(EntityType.COW, 8, 2, 9);
        bystander.setNoAi(true);

        helper.startSequence()
                .thenExecuteFor(80, () -> helper.assertTrue(!toad.isFusing(),
                        "a toad started fusing from mere proximity, with nothing hitting it"))
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
        // north (yaw 180) is -Z, i.e. facing back toward the flashbug at z=8: blinding now requires
        // actually looking at it, not merely being able to see it (see FlashbugFlashGoal).
        victim.setYRot(180.0F);
        victim.setXRot(0.0F);

        flashbug.hurt(helper.getLevel().damageSources().generic(), 1.0F);

        helper.succeedWhen(() -> helper.assertTrue(
                victim.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS),
                "a nearby victim facing the flashbug never went blind after it was provoked"));
    }

    /** Players are excluded from blinding entirely, however they are looking (see the goal doc). */
    @GameTest(template = ARENA, timeoutTicks = 60)
    public static void flashbugNeverBlindsAFacingNonLookingBystander(GameTestHelper helper) {
        com.carro1001.mhnw.entity.Flashbug flashbug = helper.spawn(ModEntities.FLASHBUG.get(), 8, 2, 8);
        Cow victim = helper.spawn(EntityType.COW, 8, 2, 9);
        victim.setNoAi(true);
        // south (yaw 0) is +Z, i.e. facing away from the flashbug: it can still see the bug (nothing
        // blocks line of sight at this range) but is not looking toward it.
        victim.setYRot(0.0F);
        victim.setXRot(0.0F);

        flashbug.hurt(helper.getLevel().damageSources().generic(), 1.0F);

        helper.runAtTickTime(50, () -> {
            helper.assertTrue(!victim.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS),
                    "a victim looking away from the flashbug was blinded anyway");
            helper.succeed();
        });
    }

    /**
     * Release is a one-way trip, same as the toad's now (see {@link FlashbugFlashGoal}): it
     * discards itself the same tick it applies its flash, rather than going quiet and eventually
     * firing again.
     */
    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void flashbugDiscardsItselfAfterReleasing(GameTestHelper helper) {
        com.carro1001.mhnw.entity.Flashbug flashbug = helper.spawn(ModEntities.FLASHBUG.get(), 8, 2, 8);

        flashbug.hurt(helper.getLevel().damageSources().generic(), 1.0F);

        helper.succeedWhen(() -> helper.assertTrue(
                flashbug.isRemoved(), "the flashbug was not discarded after releasing its flash"));
    }

    /**
     * Proximity alone must not provoke a flashbug (handoff feedback: it was flashing constantly
     * just from something standing nearby, because the default proximity trigger kept re-firing
     * every cooldown window). {@link EndemicAreaEffectGoal#allowsProximityTrigger} is overridden
     * false for this reason; regression test that the override actually reaches {@code canUse()}.
     */
    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void flashbugDoesNotFlashFromProximityAlone(GameTestHelper helper) {
        com.carro1001.mhnw.entity.Flashbug flashbug = helper.spawn(ModEntities.FLASHBUG.get(), 8, 2, 8);
        Cow bystander = helper.spawn(EntityType.COW, 8, 2, 9);
        bystander.setNoAi(true);

        helper.startSequence()
                .thenExecuteFor(80, () -> helper.assertTrue(!flashbug.isFlashing(),
                        "a flashbug started flashing from mere proximity, with nothing hitting it"))
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

    /**
     * A bitterbug/godbug is meant to be collected, not fought: a single point of any damage, even a
     * bare-handed punch, must be enough to kill it (handoff feedback: "should be a 1 tap kill").
     */
    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void bugDiesToASinglePointOfDamage(GameTestHelper helper) {
        com.carro1001.mhnw.entity.Bug bug = helper.spawn(ModEntities.BUG.get(), 8, 2, 8);
        bug.hurt(helper.getLevel().damageSources().generic(), 1.0F);

        helper.succeedWhen(() -> helper.assertTrue(
                bug.isRemoved(), "a bug survived a single point of damage; it should be a one-hit kill"));
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

    /** Same pillager-targeting goal as Great Izuchi/Rathian/Rathalos; see {@code rathianTargetsAPillagerOnSight}. */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void izuchiTargetsAPillagerOnSight(GameTestHelper helper) {
        com.carro1001.mhnw.entity.Izuchi izuchi = helper.spawn(ModEntities.IZUCHI.get(), 8, 2, 8);
        net.minecraft.world.entity.monster.Pillager pillager =
                helper.spawn(net.minecraft.world.entity.EntityType.PILLAGER, 8, 2, 10);

        helper.succeedWhen(() -> helper.assertTrue(izuchi.getTarget() == pillager,
                "Izuchi never acquired a nearby pillager as a target"));
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

    /** A05: genuinely hostile, now via {@link com.carro1001.mhnw.entity.RathianCombatGoal}'s own
     * bite timeline rather than ordinary vanilla melee. */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void rathianAttacksAndDamagesTarget(GameTestHelper helper) {
        com.carro1001.mhnw.entity.Rathian rathian = helper.spawn(ModEntities.RATHIAN.get(), 8, 2, 8);
        // Within RathianCombatGoal.BITE_RIGHT's real range band (2.0-6.0), not touching distance: this
        // attack now deliberately does not fire from point-blank (see that profile's own doc).
        Cow victim = helper.spawn(EntityType.COW, 8, 2, 12);
        victim.setNoAi(true);

        rathian.setRoaredThisEngagement(true); // skips the intro roar; this test is not about it
        rathian.setTarget(victim);
        float startingHealth = victim.getHealth();

        helper.succeedWhen(() -> helper.assertTrue(victim.getHealth() < startingHealth,
                "Rathian never damaged a target standing right next to it"));
    }

    /** The specific fix for "body-slams and only then plays the bite": damage must land inside the
     * bite's own active window (see {@code RathianCombatGoal.BITE_RIGHT}), not the instant contact is
     * made the way plain vanilla {@code MeleeAttackGoal} worked before this. */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void rathianBiteDamagesOnlyDuringActiveWindow(GameTestHelper helper) {
        com.carro1001.mhnw.entity.Rathian rathian = helper.spawn(ModEntities.RATHIAN.get(), 8, 2, 8);
        Cow victim = helper.spawn(EntityType.COW, 8, 2, 12);
        victim.setNoAi(true);

        rathian.setRoaredThisEngagement(true); // skips the intro roar; this test is not about it
        rathian.setTarget(victim);
        float startingHealth = victim.getHealth();
        int[] ageAtFirstHit = {Integer.MIN_VALUE};

        helper.startSequence()
                .thenExecuteFor(150, () -> {
                    if (ageAtFirstHit[0] == Integer.MIN_VALUE && victim.getHealth() < startingHealth) {
                        ageAtFirstHit[0] = rathian.getAttackAge();
                    }
                })
                .thenExecute(() -> {
                    helper.assertTrue(ageAtFirstHit[0] != Integer.MIN_VALUE,
                            "the bite never connected with a target standing in front of it");
                    helper.assertTrue(
                            ageAtFirstHit[0] >= com.carro1001.mhnw.entity.RathianCombatGoal.BITE_RIGHT.activeStart()
                                    && ageAtFirstHit[0] <= com.carro1001.mhnw.entity.RathianCombatGoal.BITE_RIGHT.activeEnd(),
                            "damage landed at action age " + ageAtFirstHit[0] + ", outside the active window "
                                    + com.carro1001.mhnw.entity.RathianCombatGoal.BITE_RIGHT.activeStart() + ".."
                                    + com.carro1001.mhnw.entity.RathianCombatGoal.BITE_RIGHT.activeEnd());
                })
                .thenSucceed();
    }

    /** A pillager is a valid target on its own, the same reasoning as Great Izuchi's identical goal:
     * it makes the attack observable from outside the fight, and a large monster does not care who
     * you are. Nothing else provokes it here -- the target must come from acquisition, not retaliation. */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void rathianTargetsAPillagerOnSight(GameTestHelper helper) {
        com.carro1001.mhnw.entity.Rathian rathian = helper.spawn(ModEntities.RATHIAN.get(), 8, 2, 8);
        net.minecraft.world.entity.monster.Pillager pillager =
                helper.spawn(net.minecraft.world.entity.EntityType.PILLAGER, 8, 2, 10);

        helper.succeedWhen(() -> helper.assertTrue(rathian.getTarget() == pillager,
                "Rathian never acquired a nearby pillager as a target"));
    }

    /** A02/A13: death removes Rathian and every one of its seven parts, exactly once. */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void rathianDeathRemovesTheWholeCreature(GameTestHelper helper) {
        com.carro1001.mhnw.entity.Rathian rathian = helper.spawn(ModEntities.RATHIAN.get(), 8, 2, 8);
        rathian.setNoAi(true);
        int partCount = rathian.getParts().length;
        helper.assertTrue(partCount == 9, "Rathian registered " + partCount + " parts, expected 9");

        rathian.hurt(helper.getLevel().damageSources().genericKill(), Float.MAX_VALUE);

        helper.succeedWhen(() -> {
            helper.assertTrue(rathian.isRemoved(), "Rathian was not removed after dying");
            assertPartsUnregistered(helper, rathian.monsterParts());
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

        rathalos.setRoaredThisEngagement(true); // skips the intro roar; this test is not about it
        rathalos.setTarget(victim);
        float startingHealth = victim.getHealth();

        helper.succeedWhen(() -> helper.assertTrue(victim.getHealth() < startingHealth,
                "Rathalos never damaged a target standing right next to it"));
    }

    /** Same pillager-targeting goal as Great Izuchi/Rathian/Izuchi; see {@code rathianTargetsAPillagerOnSight}. */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void rathalosTargetsAPillagerOnSight(GameTestHelper helper) {
        com.carro1001.mhnw.entity.Rathalos rathalos = helper.spawn(ModEntities.RATHALOS.get(), 8, 2, 8);
        net.minecraft.world.entity.monster.Pillager pillager =
                helper.spawn(net.minecraft.world.entity.EntityType.PILLAGER, 8, 2, 10);

        helper.succeedWhen(() -> helper.assertTrue(rathalos.getTarget() == pillager,
                "Rathalos never acquired a nearby pillager as a target"));
    }

    /** A02/A13: death removes Rathalos and every one of its six parts, exactly once. */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void rathalosDeathRemovesTheWholeCreature(GameTestHelper helper) {
        com.carro1001.mhnw.entity.Rathalos rathalos = helper.spawn(ModEntities.RATHALOS.get(), 8, 2, 8);
        rathalos.setNoAi(true);
        int partCount = rathalos.getParts().length;
        helper.assertTrue(partCount == 7, "Rathalos registered " + partCount + " parts, expected 7");

        rathalos.hurt(helper.getLevel().damageSources().genericKill(), Float.MAX_VALUE);

        helper.succeedWhen(() -> {
            helper.assertTrue(rathalos.isRemoved(), "Rathalos was not removed after dying");
            assertPartsUnregistered(helper, rathalos.monsterParts());
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

    // ---------------------------------------------------------------- Lagiacrus (P5a, movement only)

    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void lagiacrusRegistersWithSevenStaticPartsAndAnEgg(GameTestHelper helper) {
        Lagiacrus monster = helper.spawn(ModEntities.LAGIACRUS.get(), 8, 2, 8);
        monster.setNoAi(true);
        helper.assertTrue(monster.getBbWidth() == Lagiacrus.BODY_WIDTH
                        && monster.getBbHeight() == Lagiacrus.BODY_HEIGHT,
                "Lagiacrus root dimensions differ from the declared adult footprint");
        helper.assertTrue(Math.abs(monster.getMaxHealth() - Lagiacrus.MAX_HEALTH) < EPSILON,
                "Lagiacrus attributes were not registered");
        var egg = (net.minecraft.world.item.SpawnEggItem) ModEntities.LAGIACRUS_SPAWN_EGG.get();
        helper.assertTrue(egg.getType(egg.getDefaultInstance()) == ModEntities.LAGIACRUS.get(),
                "Lagiacrus egg points at a different entity type");
        String[] names = { "jawHitbox", "neckMidHitbox", "neckBaseHitbox", "tailBaseHitbox",
                "tailMidHitbox", "tailLastHitbox", "tailEndHitbox" };
        helper.assertTrue(monster.isMultipartEntity() && monster.getParts().length == names.length,
                "Lagiacrus must have exactly seven parts");
        for (int i = 0; i < names.length; i++) {
            MonsterPart part = monster.monsterParts()[i];
            helper.assertTrue(part.partName.equals(names[i]) && part.getParent() == monster,
                    "wrong part order or owner at " + i);
            helper.assertTrue(helper.getLevel().getPartEntities().contains(part),
                    "part missing from NeoForge lookup: " + part.partName);
            var centre = monster.localToWorld(part.localLeft, part.localUp, part.localForward);
            helper.assertTrue(part.getBoundingBox().getCenter().distanceTo(centre) < EPSILON,
                    "part was not positioned on spawn: " + part.partName);
        }
        helper.succeed();
    }

    /** Both root-first and part-first enumeration must cost the parent one ordinary hit. */
    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void lagiacrusRootAndPartsCountOneSourceOnce(GameTestHelper helper) {
        for (boolean rootFirst : new boolean[] { true, false }) {
            Lagiacrus monster = helper.spawn(ModEntities.LAGIACRUS.get(), rootFirst ? 4 : 12, 2, 8);
            monster.setNoAi(true);
            float before = monster.getHealth();
            DamageSource source = helper.getLevel().damageSources().generic();
            if (rootFirst) {
                helper.assertTrue(monster.hurt(source, PROBE_DAMAGE), "root rejected an ordinary hit");
            } else {
                helper.assertTrue(monster.monsterParts()[0].hurt(source, PROBE_DAMAGE),
                        "jaw rejected an ordinary hit");
            }
            for (MonsterPart part : monster.monsterParts()) {
                part.hurt(source, PROBE_DAMAGE);
            }
            monster.hurt(source, PROBE_DAMAGE);
            helper.assertTrue(Math.abs(before - monster.getHealth() - PROBE_DAMAGE) < EPSILON,
                    "root/parts multiplied one damage source (rootFirst=" + rootFirst + ")");
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void lagiacrusDistinctAttackersRetainParentDamage(GameTestHelper helper) {
        Lagiacrus monster = helper.spawn(ModEntities.LAGIACRUS.get(), 8, 2, 8);
        monster.setNoAi(true);
        Cow first = helper.spawn(EntityType.COW, 2, 2, 2);
        Cow second = helper.spawn(EntityType.COW, 13, 2, 13);
        first.setNoAi(true);
        second.setNoAi(true);
        float before = monster.getHealth();
        monster.monsterParts()[0].hurt(helper.getLevel().damageSources().mobAttack(first), PROBE_DAMAGE * 2);
        monster.monsterParts()[6].hurt(helper.getLevel().damageSources().mobAttack(second), PROBE_DAMAGE);
        helper.assertTrue(Math.abs(before - monster.getHealth() - PROBE_DAMAGE * 3) < EPSILON,
                "the smaller second attack did not reach parent health in full");
        helper.assertTrue(monster.getLastHurtByMob() == second, "parent lost damage attribution");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 80)
    public static void lagiacrusDeathStopsPursuitAndUnregistersParts(GameTestHelper helper) {
        Lagiacrus monster = helper.spawn(ModEntities.LAGIACRUS.get(), 3, 2, 8);
        Cow target = helper.spawn(EntityType.COW, 13, 2, 8);
        target.setNoAi(true);
        monster.setTarget(target);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(monster.isAggressive(), "waiting for pursuit"))
                .thenExecute(() -> {
                    monster.hurt(helper.getLevel().damageSources().genericKill(), Float.MAX_VALUE);
                    assertLagiacrusPursuitStopped(helper, monster);
                })
                .thenWaitUntil(() -> {
                    helper.assertTrue(monster.isRemoved(), "vanilla death did not remove Lagiacrus");
                    assertLagiacrusPartsUnregistered(helper, monster);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void lagiacrusDiscardStopsPursuitAndUnregistersParts(GameTestHelper helper) {
        Lagiacrus monster = helper.spawn(ModEntities.LAGIACRUS.get(), 3, 2, 8);
        Cow target = helper.spawn(EntityType.COW, 13, 2, 8);
        target.setNoAi(true);
        monster.setTarget(target);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(monster.isAggressive(), "waiting for pursuit"))
                .thenExecute(() -> {
                    monster.discard();
                    assertLagiacrusPursuitStopped(helper, monster);
                    assertLagiacrusPartsUnregistered(helper, monster);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 220)
    public static void lagiacrusPursuesOnLandWithoutOutgoingDamage(GameTestHelper helper) {
        Lagiacrus monster = helper.spawn(ModEntities.LAGIACRUS.get(), 3, 2, 8);
        Cow target = helper.spawn(EntityType.COW, 13, 2, 8);
        target.setNoAi(true);
        float health = target.getHealth();
        monster.setTarget(target);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(monster.distanceTo(target) < 3.0F,
                        "Lagiacrus did not close on its land target"))
                .thenExecuteFor(40, () -> helper.assertTrue(target.getHealth() == health,
                        "movement-only Lagiacrus dealt outgoing damage"))
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void lagiacrusSwimsAndBreathesUnderwater(GameTestHelper helper) {
        // A contained source-water pool: no currents or random terrain to affect the assertion.
        fillLagiacrusPool(helper, 14, 5);
        Lagiacrus monster = helper.spawn(ModEntities.LAGIACRUS.get(), 3, 2, 8);
        var target = helper.spawn(EntityType.AXOLOTL, 12, 3, 8);
        target.setNoAi(true);
        target.setNoGravity(true);
        monster.setAirSupply(1);
        float health = monster.getHealth();
        float targetHealth = target.getHealth();
        monster.setTarget(target);
        var start = monster.position();
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(monster.isUnderWater()
                                && monster.position().distanceTo(start) > 2.0D
                                && monster.distanceTo(target) < 3.0F,
                        "Lagiacrus did not swim toward its submerged target"))
                .thenExecuteFor(40, () -> {
                    helper.assertTrue(monster.isUnderWater() && monster.getAirSupply() > 0
                                    && monster.getHealth() == health,
                            "submerged Lagiacrus lost air or health");
                    helper.assertTrue(target.getHealth() == targetHealth, "swim pursuit damaged its target");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 540)
    public static void lagiacrusEntersWaterAndBoundsBankExit(GameTestHelper helper) {
        // The arena floor is y=0, not y=1. Supply a continuous pool floor and a one-block bank:
        // source water at y=2 stays contained, and dry land starts at y=3. An open boundary floods
        // the land; the old two-block bank also left an undercut at y=1. This covers a shallow
        // shore. With this wide root, native swimming can stall against even this bank: the
        // swimming controller has no jump handling, and a floating root cannot ground-step.
        // Require entry and bounded exit pursuit; do not claim bidirectional bank traversal.
        for (int x = 0; x <= 15; x++) {
            for (int z = 0; z <= 15; z++) {
                helper.setBlock(x, 1, z, net.minecraft.world.level.block.Blocks.STONE);
                if (x >= 8) {
                    helper.setBlock(x, 2, z, net.minecraft.world.level.block.Blocks.STONE);
                }
            }
        }
        fillLagiacrusPool(helper, 7, 2);
        Lagiacrus monster = helper.spawn(ModEntities.LAGIACRUS.get(), 12, 3, 8);
        Cow landTarget = helper.spawn(EntityType.COW, 12, 3, 8);
        landTarget.setNoAi(true);
        var waterTarget = helper.spawn(EntityType.AXOLOTL, 3, 2, 8);
        waterTarget.setNoAi(true);
        waterTarget.setNoGravity(true);
        var navigation = monster.getNavigation();
        var control = monster.getMoveControl();
        var lookControl = monster.getLookControl();
        float landHealth = landTarget.getHealth();
        float waterHealth = waterTarget.getHealth();
        monster.setTarget(waterTarget);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(monster.isInWater() && monster.distanceTo(waterTarget) < 3.0F,
                        "Lagiacrus failed to enter shallow water from land"))
                .thenExecute(() -> monster.setTarget(null))
                .thenWaitUntil(() -> assertLagiacrusPursuitStopped(helper, monster))
                .thenIdle(LagiacrusPursuitGoal.RETRY_COOLDOWN_TICKS + 5)
                .thenExecute(() -> monster.setTarget(landTarget))
                .thenWaitUntil(() -> helper.assertTrue(monster.isAggressive(), "bank-exit pursuit never started"))
                .thenExecuteAfter(LagiacrusPursuitGoal.MAX_PURSUIT_TICKS + 5,
                        () -> assertLagiacrusPursuitStopped(helper, monster))
                .thenExecute(() -> {
                    helper.assertTrue(monster.getNavigation() == navigation
                                && monster.getMoveControl() == control
                                && monster.getLookControl() == lookControl,
                            "shoreline transition replaced the native navigation/controls");
                    helper.assertTrue(landTarget.getHealth() == landHealth && waterTarget.getHealth() == waterHealth,
                            "shoreline pursuit damaged a target");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 180)
    public static void lagiacrusAbandonsSealedTargetAndHonorsRetryCooldown(GameTestHelper helper) {
        Lagiacrus monster = helper.spawn(ModEntities.LAGIACRUS.get(), 3, 2, 8);
        Cow target = helper.spawn(EntityType.COW, 12, 2, 8);
        target.setNoAi(true);
        for (int x = 11; x <= 13; x++) {
            for (int y = 1; y <= 5; y++) {
                for (int z = 7; z <= 9; z++) {
                    if (x == 11 || x == 13 || y == 1 || y == 5 || z == 7 || z == 9) {
                        helper.setBlock(x, y, z, net.minecraft.world.level.block.Blocks.STONE);
                    }
                }
            }
        }
        monster.setTarget(target);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(monster.isAggressive(), "waiting for pursuit"))
                .thenExecuteAfter(LagiacrusPursuitGoal.REPATH_INTERVAL_TICKS
                        * LagiacrusPursuitGoal.MAX_FAILED_PATHS + 5, () -> {
                    assertLagiacrusPursuitStopped(helper, monster);
                    // Simulate a new target assignment during the cooldown; it must not restart.
                    monster.setTarget(target);
                })
                .thenExecuteAfter(5, () -> assertLagiacrusPursuitStopped(helper, monster))
                .thenExecuteFor(20, () -> assertLagiacrusPursuitStopped(helper, monster))
                .thenSucceed();
    }

    /** A valid path that never advances still has an absolute deadline, independent of failures. */
    @GameTest(template = ARENA, timeoutTicks = 240)
    public static void lagiacrusStalledReachablePursuitHasAHardDeadline(GameTestHelper helper) {
        Lagiacrus monster = helper.spawn(ModEntities.LAGIACRUS.get(), 3, 2, 8);
        monster.setNoAi(true);
        Cow target = helper.spawn(EntityType.COW, 12, 2, 8);
        target.setNoAi(true);
        monster.setTarget(target);
        // Drive just the real goal; disabling AI deliberately prevents the navigator moving it.
        LagiacrusPursuitGoal goal = new LagiacrusPursuitGoal(monster);
        helper.startSequence().thenIdle(2).thenExecute(() -> {
                    helper.assertTrue(goal.canUse(), "goal rejected the valid target");
                    goal.start();
                    goal.tick();
                    helper.assertTrue(!monster.getNavigation().isDone()
                                    && monster.getNavigation().getPath().canReach(),
                            "deadline fixture must start with a reachable path");
                })
                .thenExecuteFor(LagiacrusPursuitGoal.MAX_PURSUIT_TICKS + 1, goal::tick)
                .thenExecute(() -> {
                    assertLagiacrusPursuitStopped(helper, monster);
                    monster.setTarget(target);
                    helper.assertTrue(!goal.canUse(), "expired pursuit immediately restarted");
                }).thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 80)
    public static void lagiacrusTargetLossClearsMovement(GameTestHelper helper) {
        Lagiacrus monster = helper.spawn(ModEntities.LAGIACRUS.get(), 3, 2, 8);
        Cow target = helper.spawn(EntityType.COW, 13, 2, 8);
        target.setNoAi(true);
        monster.setTarget(target);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(monster.isAggressive(), "waiting for pursuit"))
                .thenExecute(target::discard)
                .thenExecuteAfter(5, () -> assertLagiacrusPursuitStopped(helper, monster))
                .thenExecuteFor(20, () -> assertLagiacrusPursuitStopped(helper, monster))
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 80)
    public static void lagiacrusReloadCancelsPursuitAndPreservesHealth(GameTestHelper helper) {
        Lagiacrus original = helper.spawn(ModEntities.LAGIACRUS.get(), 3, 2, 8);
        Cow target = helper.spawn(EntityType.COW, 13, 2, 8);
        target.setNoAi(true);
        original.setHealth(37.0F);
        original.setTarget(target);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(original.isAggressive(), "waiting for pursuit"))
                .thenExecute(() -> {
                    CompoundTag saved = original.saveWithoutId(new CompoundTag());
                    original.discard();
                    Lagiacrus reloaded = new Lagiacrus(ModEntities.LAGIACRUS.get(), helper.getLevel());
                    reloaded.load(saved);
                    helper.getLevel().addFreshEntity(reloaded);
                    assertLagiacrusPursuitStopped(helper, reloaded);
                    helper.assertTrue(reloaded.getHealth() == 37.0F && reloaded.getParts().length == 7,
                            "reload lost parent health or changed the part count");
                    assertLagiacrusPartsUnregistered(helper, original);
                    for (MonsterPart part : reloaded.monsterParts()) {
                        helper.assertTrue(helper.getLevel().getPartEntities().contains(part),
                                "reloaded part missing from NeoForge lookup");
                    }
                    reloaded.setTarget(target);
                    helper.runAfterDelay(10, () -> {
                        assertLagiacrusPursuitStopped(helper, reloaded);
                        helper.succeed();
                    });
                });
    }

    private static void assertLagiacrusPursuitStopped(GameTestHelper helper, Lagiacrus monster) {
        helper.assertTrue(monster.getTarget() == null && !monster.isAggressive()
                        && monster.getNavigation().isDone() && monster.getSpeed() == 0.0F
                        && monster.xxa == 0.0F && monster.yya == 0.0F && monster.zza == 0.0F,
                "Lagiacrus retained target, aggression, path or movement input after pursuit ended");
    }

    private static void assertLagiacrusPartsUnregistered(GameTestHelper helper, Lagiacrus monster) {
        assertPartsUnregistered(helper, monster.monsterParts());
    }

    private static void fillLagiacrusPool(GameTestHelper helper, int waterMaxX, int topY) {
        for (int x = 0; x <= 15; x++) {
            for (int z = 0; z <= 15; z++) {
                for (int y = 2; y <= topY; y++) {
                    if (x == 0 || x == 15 || z == 0 || z == 15) {
                        helper.setBlock(x, y, z, net.minecraft.world.level.block.Blocks.STONE);
                    } else if (x <= waterMaxX) {
                        helper.setBlock(x, y, z, net.minecraft.world.level.block.Blocks.WATER);
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------------------------------
    // R1a: Verdant Hunting Grounds. Registry, resolved spawn data, the automatic-spawn guard and
    // terrain-safe escort placement.
    //
    // These deliberately stop where a flat GameTest arena stops being evidence. `setBiome` paints a
    // biome key onto test chunks, which is exactly right for asking "does the guard read the tag"
    // and wrong for asking "does the Overworld generate this biome" -- the latter needs real scratch
    // worlds and lives in docs/TEST_PLAN.md's recorded seed runs, not here.
    // ------------------------------------------------------------------------------------------

    /** Every MH mob R1a gives an automatic spawn entry to, plus small Izuchi for placement validation. */
    private static final EntityType<?>[] HABITAT_TYPES = {
            ModEntities.GREAT_IZUCHI.get(), ModEntities.IZUCHI.get(), ModEntities.APTONOTH.get(),
            ModEntities.TOAD.get(), ModEntities.FLASHBUG.get(), ModEntities.BUG.get(),
    };

    /** H01: the biome, its sparse tree placement and the habitat selector all resolve at runtime. */
    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void huntingGroundsRegistryResolves(GameTestHelper helper) {
        net.minecraft.server.level.ServerLevel level = helper.getLevel();
        helper.assertTrue(level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME)
                        .containsKey(ModBiomes.VERDANT_HUNTING_GROUNDS.location()),
                "biome mhnw:verdant_hunting_grounds did not load from the datapack");
        helper.assertTrue(level.registryAccess()
                        .registryOrThrow(net.minecraft.core.registries.Registries.PLACED_FEATURE)
                        .containsKey(net.minecraft.resources.ResourceLocation
                                .fromNamespaceAndPath(MHNW.MOD_ID, "trees_hunting_grounds")),
                "placed feature mhnw:trees_hunting_grounds did not load");
        // The selector must actually contain our biome -- an empty tag would silently switch every
        // habitat check off rather than fail loudly.
        helper.assertTrue(level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME)
                        .getHolderOrThrow(ModBiomes.VERDANT_HUNTING_GROUNDS)
                        .is(ModBiomes.SPAWNS_HUNTING_WILDLIFE),
                "the habitat biome is not in #mhnw:spawns_hunting_wildlife");
        // Every species this packet spawns automatically needs a registered placement, or the natural
        // spawner silently refuses it.
        for (EntityType<?> type : HABITAT_TYPES) {
            helper.assertTrue(net.minecraft.world.entity.SpawnPlacements.getPlacementType(type)
                            == net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                    "no ON_GROUND spawn placement registered for " + type.getDescriptionId());
            helper.assertTrue(net.minecraft.world.entity.SpawnPlacements.getHeightmapType(type)
                            == ModEntities.SPAWN_HEIGHTMAP,
                    "wrong spawn heightmap for " + type.getDescriptionId());
        }
        helper.succeed();
    }

    /**
     * H02: our TerraBlender region really is registered for the Overworld and really does map the
     * habitat key, without replacing the Overworld's other climate slots.
     *
     * <p>This walks the region's own output rather than trusting the JSON: {@code addBiomes} is asked
     * for its parameter list and the result is checked for both our key and a sample of vanilla keys
     * that must survive. A global replacement would show up here as vanilla keys going missing.
     */
    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void huntingGroundsRegionMapsPlainsAndForestOnly(GameTestHelper helper) {
        helper.assertTrue(terrablender.api.Regions.get(terrablender.api.RegionType.OVERWORLD).stream()
                        .anyMatch(r -> r.getName().equals(HuntingGroundsRegion.NAME)),
                "mhnw:overworld_hunting_grounds is not registered as an Overworld region");

        java.util.List<net.minecraft.resources.ResourceKey<net.minecraft.world.level.biome.Biome>> mapped =
                new java.util.ArrayList<>();
        new HuntingGroundsRegion().addBiomes(
                helper.getLevel().registryAccess()
                        .registryOrThrow(net.minecraft.core.registries.Registries.BIOME),
                pair -> mapped.add(pair.getSecond()));

        helper.assertTrue(mapped.contains(ModBiomes.VERDANT_HUNTING_GROUNDS),
                "our region never maps mhnw:verdant_hunting_grounds");
        helper.assertTrue(!mapped.contains(net.minecraft.world.level.biome.Biomes.PLAINS)
                        && !mapped.contains(net.minecraft.world.level.biome.Biomes.FOREST),
                "plains/forest slots were not replaced inside our own region");
        // A representative spread of untouched mappings. If replaceBiome ever went global these
        // would vanish along with plains and forest.
        for (net.minecraft.resources.ResourceKey<net.minecraft.world.level.biome.Biome> kept :
                java.util.List.of(net.minecraft.world.level.biome.Biomes.OCEAN,
                        net.minecraft.world.level.biome.Biomes.DESERT,
                        net.minecraft.world.level.biome.Biomes.TAIGA,
                        net.minecraft.world.level.biome.Biomes.DARK_FOREST,
                        net.minecraft.world.level.biome.Biomes.SNOWY_PLAINS,
                        net.minecraft.world.level.biome.Biomes.JUNGLE)) {
            helper.assertTrue(mapped.contains(kept),
                    "our region dropped the vanilla mapping for " + kept.location());
        }
        helper.succeed();
    }

    /**
     * H03: after biome modifiers run, the habitat holds exactly one entry per independent species, in
     * the right category and with the agreed counts -- and the unfinished wyverns and the escort-only
     * small Izuchi hold none.
     */
    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void huntingGroundsHasOneEntryPerSpecies(GameTestHelper helper) {
        net.minecraft.world.level.biome.Biome habitat = helper.getLevel().registryAccess()
                .registryOrThrow(net.minecraft.core.registries.Registries.BIOME)
                .getOrThrow(ModBiomes.VERDANT_HUNTING_GROUNDS);

        assertOneSpawnEntry(helper, habitat, net.minecraft.world.entity.MobCategory.MONSTER,
                ModEntities.GREAT_IZUCHI.get(), 2, 1, 1);
        assertOneSpawnEntry(helper, habitat, net.minecraft.world.entity.MobCategory.CREATURE,
                ModEntities.APTONOTH.get(), 8, 2, 3);
        assertOneSpawnEntry(helper, habitat, net.minecraft.world.entity.MobCategory.CREATURE,
                ModEntities.TOAD.get(), 2, 1, 2);
        assertOneSpawnEntry(helper, habitat, net.minecraft.world.entity.MobCategory.CREATURE,
                ModEntities.FLASHBUG.get(), 2, 1, 2);
        assertOneSpawnEntry(helper, habitat, net.minecraft.world.entity.MobCategory.AMBIENT,
                ModEntities.BUG.get(), 2, 1, 2);

        // No independent small-Izuchi population: it arrives as an escort or not at all.
        assertNoSpawnEntry(helper, habitat, ModEntities.IZUCHI.get());
        // Registered, egg-usable, but deliberately not part of R1a's habitat.
        assertNoSpawnEntry(helper, habitat, ModEntities.RATHIAN.get());
        assertNoSpawnEntry(helper, habitat, ModEntities.RATHALOS.get());
        assertNoSpawnEntry(helper, habitat, ModEntities.LAGIACRUS.get());

        // And the retargeting really moved Great Izuchi off the vanilla forest tag it used to sit on.
        net.minecraft.world.level.biome.Biome vanillaForest = helper.getLevel().registryAccess()
                .registryOrThrow(net.minecraft.core.registries.Registries.BIOME)
                .getOrThrow(net.minecraft.world.level.biome.Biomes.FOREST);
        for (EntityType<?> type : HABITAT_TYPES) {
            assertNoSpawnEntry(helper, vanillaForest, type);
        }
        helper.succeed();
    }

    private static java.util.List<net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData> entriesFor(
            net.minecraft.world.level.biome.Biome biome, net.minecraft.world.entity.MobCategory category,
            EntityType<?> type) {
        return biome.getMobSettings().getMobs(category).unwrap().stream()
                .filter(entry -> entry.type == type).toList();
    }

    private static void assertOneSpawnEntry(GameTestHelper helper, net.minecraft.world.level.biome.Biome biome,
                                            net.minecraft.world.entity.MobCategory category, EntityType<?> type,
                                            int weight, int min, int max) {
        java.util.List<net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData> found =
                entriesFor(biome, category, type);
        helper.assertTrue(found.size() == 1, "expected exactly one " + category + " entry for "
                + type.getDescriptionId() + ", found " + found.size());
        net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData data = found.get(0);
        helper.assertTrue(data.getWeight().asInt() == weight && data.minCount == min && data.maxCount == max,
                type.getDescriptionId() + " entry is weight " + data.getWeight().asInt() + " count "
                        + data.minCount + "-" + data.maxCount + ", expected " + weight + " / " + min + "-" + max);
        helper.assertTrue(type.getCategory() == category,
                type.getDescriptionId() + " is registered in category " + type.getCategory()
                        + " but its spawn entry is under " + category);
    }

    private static void assertNoSpawnEntry(GameTestHelper helper, net.minecraft.world.level.biome.Biome biome,
                                          EntityType<?> type) {
        for (net.minecraft.world.entity.MobCategory category : net.minecraft.world.entity.MobCategory.values()) {
            helper.assertTrue(entriesFor(biome, category, type).isEmpty(),
                    "unexpected " + category + " spawn entry for " + type.getDescriptionId());
        }
    }

    /**
     * H04: the automatic-spawn guard, for every relevant type and both automatic sources.
     *
     * <p>Three things are checked together because they are one decision: inside the habitat with the
     * config on, an automatic spawn may pass; outside it, or with the config off, it may not; and a
     * manual origin passes regardless of either. {@code CHUNK_GENERATION} is included deliberately --
     * before R1a it slipped past the config entirely, which is how a worldgen-seeded animal
     * population would have ignored the off switch.
     *
     * <p>The config is restored in a {@code finally} in this one synchronous callback rather than
     * across ticks, so a failure cannot leave the shared server setting flipped for the tests that
     * run after it.
     */
    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void huntingGuardGatesAutomaticSpawnsOnHabitatAndConfig(GameTestHelper helper) {
        net.minecraft.world.entity.MobSpawnType[] automatic = {
                net.minecraft.world.entity.MobSpawnType.NATURAL,
                net.minecraft.world.entity.MobSpawnType.CHUNK_GENERATION,
        };
        net.minecraft.world.entity.MobSpawnType[] manual = {
                net.minecraft.world.entity.MobSpawnType.SPAWN_EGG,
                net.minecraft.world.entity.MobSpawnType.COMMAND,
                net.minecraft.world.entity.MobSpawnType.SPAWNER,
                net.minecraft.world.entity.MobSpawnType.MOB_SUMMONED,
        };
        net.minecraft.core.BlockPos pos = helper.absolutePos(new net.minecraft.core.BlockPos(8, 3, 8));
        boolean restore = MHNWConfig.NATURAL_SPAWNING.get();
        try {
            // In the habitat, switch on: automatic spawning is permitted.
            helper.setBiome(ModBiomes.VERDANT_HUNTING_GROUNDS);
            MHNWConfig.NATURAL_SPAWNING.set(true);
            for (net.minecraft.world.entity.MobSpawnType type : automatic) {
                helper.assertTrue(HuntingSpawnRules.habitatAllows(helper.getLevel(), type, pos),
                        "guard rejected " + type + " inside the habitat with the config on");
            }

            // Switch off: both automatic sources are refused, in the very same biome.
            MHNWConfig.NATURAL_SPAWNING.set(false);
            for (net.minecraft.world.entity.MobSpawnType type : automatic) {
                helper.assertTrue(!HuntingSpawnRules.habitatAllows(helper.getLevel(), type, pos),
                        "guard allowed " + type + " with naturalSpawning=false");
            }
            // ...but development access is untouched by the off switch.
            for (net.minecraft.world.entity.MobSpawnType type : manual) {
                helper.assertTrue(HuntingSpawnRules.habitatAllows(helper.getLevel(), type, pos),
                        "guard rejected manual origin " + type + " with naturalSpawning=false");
            }

            // Wrong biome, switch back on: still refused, and still not for manual origins.
            MHNWConfig.NATURAL_SPAWNING.set(true);
            helper.setBiome(net.minecraft.world.level.biome.Biomes.FOREST);
            for (net.minecraft.world.entity.MobSpawnType type : automatic) {
                helper.assertTrue(!HuntingSpawnRules.habitatAllows(helper.getLevel(), type, pos),
                        "guard allowed " + type + " in a vanilla forest outside the habitat");
            }
            for (net.minecraft.world.entity.MobSpawnType type : manual) {
                helper.assertTrue(HuntingSpawnRules.habitatAllows(helper.getLevel(), type, pos),
                        "guard rejected manual origin " + type + " outside the habitat");
            }

            // And the same, driven through each species' real registered predicate rather than the
            // shared helper, so a species wired to the wrong one is caught.
            MHNWConfig.NATURAL_SPAWNING.set(false);
            helper.setBiome(ModBiomes.VERDANT_HUNTING_GROUNDS);
            for (EntityType<?> type : HABITAT_TYPES) {
                for (net.minecraft.world.entity.MobSpawnType source : automatic) {
                    helper.assertTrue(!checkPlacement(helper, type, source, pos),
                            type.getDescriptionId() + " accepted " + source + " with naturalSpawning=false");
                }
            }
        } finally {
            MHNWConfig.NATURAL_SPAWNING.set(restore);
        }
        helper.succeed();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static boolean checkPlacement(GameTestHelper helper, EntityType<?> type,
                                         net.minecraft.world.entity.MobSpawnType source,
                                         net.minecraft.core.BlockPos pos) {
        return net.minecraft.world.entity.SpawnPlacements.checkSpawnRules((EntityType) type,
                helper.getLevel(), source, pos, helper.getLevel().getRandom());
    }

    /**
     * H04, the other half: a valid position inside the habitat with the switch on actually passes,
     * so the rejection test above cannot be satisfied by a guard that simply refuses everything.
     *
     * <p>Aptonoth is the species that can be driven end to end here: its predicate is habitat plus
     * vanilla's animal rules, neither of which minds being indoors. The three surface species cannot
     * be, and the reason is worth knowing before someone "fixes" it: the GameTest framework encloses
     * every test structure in a barrier cage <em>with a lid</em>, so the entire arena interior sits
     * below the spawn heightmap and {@code isSurface} correctly answers "this is not the surface".
     * Building a fixture above that lid would mean writing blocks outside the test's own bounds, into
     * a world shared with the tests running beside it -- which is exactly the flakiness that got this
     * test rewritten. The surface rule is therefore covered in three parts instead: its own logic in
     * {@link #huntingSurfaceRuleFollowsTheSpawnHeightmap}, its ground/clearance components below, and
     * the whole composite accept path by the real-world spawn evidence in docs/TEST_PLAN.md.
     *
     * <p>Great Izuchi is excluded for a different and ordinary reason: it is a {@code MONSTER}, so
     * vanilla's darkness and difficulty rules apply on top of the guard, and a lit arena is
     * legitimately not a valid spot for it.
     */
    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void huntingGuardAcceptsValidHabitatPositions(GameTestHelper helper) {
        boolean restore = MHNWConfig.NATURAL_SPAWNING.get();
        try {
            helper.setBiome(ModBiomes.VERDANT_HUNTING_GROUNDS);
            MHNWConfig.NATURAL_SPAWNING.set(true);
            // Grass underfoot is what vanilla's animal rules want; the arena is lit, so the light
            // half of those rules is satisfied too.
            helper.setBlock(8, 1, 8, net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
            net.minecraft.core.BlockPos pos = helper.absolutePos(new net.minecraft.core.BlockPos(8, 2, 8));
            helper.assertTrue(checkPlacement(helper, ModEntities.APTONOTH.get(),
                            net.minecraft.world.entity.MobSpawnType.NATURAL, pos),
                    "Aptonoth refused a valid habitat position at " + pos);

            // The two physical components every surface species shares, accepted on the same spot.
            for (EntityType<?> type : new EntityType<?>[]{ModEntities.TOAD.get(),
                    ModEntities.FLASHBUG.get(), ModEntities.BUG.get()}) {
                helper.assertTrue(HuntingSpawnRules.hasSolidGround(helper.getLevel(), type, pos),
                        type.getDescriptionId() + " refused solid grass underfoot");
                helper.assertTrue(HuntingSpawnRules.isFree(helper.getLevel(), type, pos),
                        type.getDescriptionId() + " refused a clear body-sized volume");
            }
        } finally {
            MHNWConfig.NATURAL_SPAWNING.set(restore);
        }
        helper.succeed();
    }

    /**
     * The surface rule in isolation: at or just under the column's own heightmap counts as the
     * surface, and well below it does not. Asserted against the arena's real heightmap rather than a
     * hardcoded Y, because what that value is depends on the GameTest cage, not on our code.
     */
    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void huntingSurfaceRuleFollowsTheSpawnHeightmap(GameTestHelper helper) {
        net.minecraft.core.BlockPos anchor = helper.absolutePos(new net.minecraft.core.BlockPos(8, 2, 8));
        int top = helper.getLevel().getHeight(ModEntities.SPAWN_HEIGHTMAP, anchor.getX(), anchor.getZ());
        helper.assertTrue(HuntingSpawnRules.isSurface(helper.getLevel(),
                        new net.minecraft.core.BlockPos(anchor.getX(), top, anchor.getZ())),
                "the heightmap position itself was not treated as the surface");
        helper.assertTrue(HuntingSpawnRules.isSurface(helper.getLevel(),
                        new net.minecraft.core.BlockPos(anchor.getX(), top - 1, anchor.getZ())),
                "one block under the heightmap was not treated as the surface (canopy tolerance)");
        helper.assertTrue(!HuntingSpawnRules.isSurface(helper.getLevel(),
                        new net.minecraft.core.BlockPos(anchor.getX(), top - 6, anchor.getZ())),
                "six blocks under the heightmap was treated as the surface");
        // And the whole composite check refuses the arena interior for exactly that reason, which is
        // also the cave case: a position well below its column's surface.
        helper.assertTrue(!HuntingSpawnRules.checkSurfaceWildlife(ModEntities.TOAD.get(), helper.getLevel(),
                        net.minecraft.world.entity.MobSpawnType.NATURAL, anchor,
                        helper.getLevel().getRandom()),
                "surface wildlife accepted a position far below its column's surface");
        helper.succeed();
    }

    /** Surface wildlife must not be placed in liquid, in a solid block, or in unsupported air. */
    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void huntingSurfaceWildlifeRejectsLiquidAndBlockedSpace(GameTestHelper helper) {
        boolean restore = MHNWConfig.NATURAL_SPAWNING.get();
        try {
            helper.setBiome(ModBiomes.VERDANT_HUNTING_GROUNDS);
            MHNWConfig.NATURAL_SPAWNING.set(true);
            EntityType<Toad> toad = ModEntities.TOAD.get();
            net.minecraft.server.level.ServerLevel level = helper.getLevel();

            // Grass at 1, the body volume at 2: the control the three rejections below are variations
            // on. Only the component under test is broken each time.
            helper.setBlock(8, 1, 8, net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
            net.minecraft.core.BlockPos good = helper.absolutePos(new net.minecraft.core.BlockPos(8, 2, 8));
            helper.assertTrue(HuntingSpawnRules.hasSolidGround(level, toad, good)
                            && HuntingSpawnRules.isFree(level, toad, good),
                    "the control position was already rejected, so the rejections below prove nothing");

            // Nothing underneath.
            helper.setBlock(4, 1, 4, net.minecraft.world.level.block.Blocks.AIR);
            helper.assertTrue(!HuntingSpawnRules.hasSolidGround(level, toad,
                            helper.absolutePos(new net.minecraft.core.BlockPos(4, 2, 4))),
                    "surface wildlife accepted a position with nothing underneath");

            // Standing in water.
            helper.setBlock(6, 1, 6, net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
            helper.setBlock(6, 2, 6, net.minecraft.world.level.block.Blocks.WATER);
            helper.assertTrue(!HuntingSpawnRules.isFree(level, toad,
                            helper.absolutePos(new net.minecraft.core.BlockPos(6, 2, 6))),
                    "surface wildlife accepted a position in water");

            // A solid block where the body would go.
            helper.setBlock(10, 1, 10, net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
            helper.setBlock(10, 2, 10, net.minecraft.world.level.block.Blocks.STONE);
            helper.assertTrue(!HuntingSpawnRules.isFree(level, toad,
                            helper.absolutePos(new net.minecraft.core.BlockPos(10, 2, 10))),
                    "surface wildlife accepted a position occupied by a solid block");

            // Dry feet, submerged head. The escort placement shares this helper, and an Izuchi is
            // 1.1 blocks tall, so a feet-only fluid test would accept this and drown it.
            EntityType<com.carro1001.mhnw.entity.Izuchi> izuchi = ModEntities.IZUCHI.get();
            helper.setBlock(12, 1, 12, net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
            helper.setBlock(12, 3, 12, net.minecraft.world.level.block.Blocks.WATER);
            net.minecraft.core.BlockPos wetHead =
                    helper.absolutePos(new net.minecraft.core.BlockPos(12, 2, 12));
            helper.assertTrue(level.getFluidState(wetHead).isEmpty(),
                    "fixture is wrong: the feet block should be dry for this case to mean anything");
            helper.assertTrue(!HuntingSpawnRules.isFree(level, izuchi, wetHead),
                    "a position with dry feet and the upper body in water was accepted");
        } finally {
            MHNWConfig.NATURAL_SPAWNING.set(restore);
        }
        helper.succeed();
    }

    /**
     * H05: an open, valid habitat gives the leader a full 1-4 pack, and every member lands on real
     * ground rather than at the leader's own Y.
     */
    @GameTest(template = ARENA, timeoutTicks = 60)
    public static void escortsLandOnGroundInOpenHabitat(GameTestHelper helper) {
        helper.setBiome(ModBiomes.VERDANT_HUNTING_GROUNDS);
        fillFloor(helper, 1, net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
        GreatIzuchi leader = helper.spawn(ModEntities.GREAT_IZUCHI.get(), 8, 2, 8);
        leader.finalizeSpawn(helper.getLevel(),
                helper.getLevel().getCurrentDifficultyAt(leader.blockPosition()),
                net.minecraft.world.entity.MobSpawnType.NATURAL, null);

        java.util.List<com.carro1001.mhnw.entity.Izuchi> escorts = escortsNear(helper, leader);
        helper.assertTrue(escorts.size() >= 1 && escorts.size() <= 4,
                "expected 1-4 escorts in open valid habitat, got " + escorts.size());
        for (com.carro1001.mhnw.entity.Izuchi escort : escorts) {
            helper.assertTrue(!escort.level().getBlockState(escort.blockPosition()).isSolid(),
                    "escort spawned inside a solid block at " + escort.blockPosition());
            helper.assertTrue(escort.level().getBlockState(escort.blockPosition().below()).isSolid(),
                    "escort spawned with nothing underneath at " + escort.blockPosition());
        }
        helper.succeed();
    }

    /**
     * H05: escorts resolve their own surface Y rather than inheriting the leader's. This is the
     * regression for the fixed-Y loop R1a replaced -- under that code every escort here is created
     * buried in the step.
     *
     * <p>The fixture raises the entire escort ring by exactly one block and leaves only the leader's
     * own 3x3 at the lower level, so <em>every</em> valid escort position is a block above the leader
     * and the assertion can be exact rather than "nothing is buried". An earlier version raised the
     * step by three blocks, which put its surface at {@code leaderY + 3} -- outside
     * {@code ESCORT_MAX_RISE} -- so no escort ever stood on it and the test was really only checking
     * the untouched flat half, which the old fixed-Y code would have passed too.
     */
    @GameTest(template = ARENA, timeoutTicks = 60)
    public static void escortsResolveTheirOwnSurfaceOnSlopedGround(GameTestHelper helper) {
        helper.setBiome(ModBiomes.VERDANT_HUNTING_GROUNDS);
        fillFloor(helper, 1, net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
        // One step up everywhere except the leader's own footing.
        for (int x = 0; x <= 15; x++) {
            for (int z = 0; z <= 15; z++) {
                if (Math.abs(x - 8) <= 1 && Math.abs(z - 8) <= 1) {
                    continue;
                }
                helper.setBlock(x, 2, z, net.minecraft.world.level.block.Blocks.STONE);
            }
        }
        GreatIzuchi leader = helper.spawn(ModEntities.GREAT_IZUCHI.get(), 8, 2, 8);
        leader.finalizeSpawn(helper.getLevel(),
                helper.getLevel().getCurrentDifficultyAt(leader.blockPosition()),
                net.minecraft.world.entity.MobSpawnType.NATURAL, null);

        java.util.List<com.carro1001.mhnw.entity.Izuchi> escorts = escortsNear(helper, leader);
        helper.assertTrue(!escorts.isEmpty(), "no escort survived sloped terrain at all");
        int leaderY = leader.blockPosition().getY();
        for (com.carro1001.mhnw.entity.Izuchi escort : escorts) {
            helper.assertTrue(escort.blockPosition().getY() == leaderY + 1,
                    "escort inherited the leader's Y instead of resolving the step: escort at "
                            + escort.blockPosition() + ", leader Y " + leaderY);
            helper.assertTrue(!escort.level().getBlockState(escort.blockPosition()).isSolid()
                            && escort.level().getBlockState(escort.blockPosition().below()).isSolid(),
                    "escort on sloped ground is buried or floating at " + escort.blockPosition());
        }
        helper.succeed();
    }

    /**
     * H05: with nowhere valid in the ring, the bounded attempts give up and the leader stands alone.
     * Zero escorts is the correct outcome here -- suffocating four of them in stone is not.
     */
    @GameTest(template = ARENA, timeoutTicks = 60)
    public static void escortsAreSkippedWhenTheRingIsBlocked(GameTestHelper helper) {
        helper.setBiome(ModBiomes.VERDANT_HUNTING_GROUNDS);
        fillFloor(helper, 1, net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
        // Solid stone everywhere except a one-block-wide slot for the leader itself.
        for (int x = 0; x <= 15; x++) {
            for (int z = 0; z <= 15; z++) {
                if (x == 8 && z == 8) {
                    continue;
                }
                for (int y = 2; y <= 6; y++) {
                    helper.setBlock(x, y, z, net.minecraft.world.level.block.Blocks.STONE);
                }
            }
        }
        GreatIzuchi leader = helper.spawn(ModEntities.GREAT_IZUCHI.get(), 8, 2, 8);
        leader.finalizeSpawn(helper.getLevel(),
                helper.getLevel().getCurrentDifficultyAt(leader.blockPosition()),
                net.minecraft.world.entity.MobSpawnType.NATURAL, null);

        for (com.carro1001.mhnw.entity.Izuchi escort : escortsNear(helper, leader)) {
            helper.fail("escort forced into blocked terrain at " + escort.blockPosition());
        }
        helper.succeed();
    }

    /** H05: a wild leader's pack stays inside the habitat even when the leader stands at its edge. */
    @GameTest(template = ARENA, timeoutTicks = 60)
    public static void naturalEscortsStayInsideTheHabitat(GameTestHelper helper) {
        // The whole test chunk is a vanilla forest: the leader is at the extreme edge case, entirely
        // outside the selector. A NATURAL leader therefore gets no escorts...
        helper.setBiome(net.minecraft.world.level.biome.Biomes.FOREST);
        fillFloor(helper, 1, net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
        GreatIzuchi wild = helper.spawn(ModEntities.GREAT_IZUCHI.get(), 4, 2, 4);
        wild.finalizeSpawn(helper.getLevel(),
                helper.getLevel().getCurrentDifficultyAt(wild.blockPosition()),
                net.minecraft.world.entity.MobSpawnType.NATURAL, null);
        helper.assertTrue(escortsNear(helper, wild).isEmpty(),
                "a natural leader produced escorts outside the habitat selector");

        // ...while a mob spawner keeps its existing unrestricted behaviour, so a development or
        // adventure-map spawner still works anywhere.
        GreatIzuchi placed = helper.spawn(ModEntities.GREAT_IZUCHI.get(), 12, 2, 12);
        placed.finalizeSpawn(helper.getLevel(),
                helper.getLevel().getCurrentDifficultyAt(placed.blockPosition()),
                net.minecraft.world.entity.MobSpawnType.SPAWNER, null);
        helper.assertTrue(!escortsNear(helper, placed).isEmpty(),
                "a spawner-placed leader lost its escorts to the habitat restriction");
        helper.succeed();
    }

    /** H05: reloading a saved leader does not re-run the pack spawn. */
    @GameTest(template = ARENA, timeoutTicks = 60)
    public static void reloadDoesNotRecreateEscorts(GameTestHelper helper) {
        helper.setBiome(ModBiomes.VERDANT_HUNTING_GROUNDS);
        fillFloor(helper, 1, net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
        GreatIzuchi leader = helper.spawn(ModEntities.GREAT_IZUCHI.get(), 8, 2, 8);
        leader.finalizeSpawn(helper.getLevel(),
                helper.getLevel().getCurrentDifficultyAt(leader.blockPosition()),
                net.minecraft.world.entity.MobSpawnType.NATURAL, null);
        int before = escortsNear(helper, leader).size();

        CompoundTag saved = leader.saveWithoutId(new CompoundTag());
        leader.discard();
        GreatIzuchi reloaded = new GreatIzuchi(ModEntities.GREAT_IZUCHI.get(), helper.getLevel());
        reloaded.load(saved);
        helper.getLevel().addFreshEntity(reloaded);

        helper.assertTrue(escortsNear(helper, reloaded).size() == before,
                "reloading a saved Great Izuchi changed the escort count from " + before
                        + " to " + escortsNear(helper, reloaded).size());
        helper.succeed();
    }

    private static java.util.List<com.carro1001.mhnw.entity.Izuchi> escortsNear(GameTestHelper helper,
                                                                               GreatIzuchi leader) {
        return helper.getLevel().getEntitiesOfClass(com.carro1001.mhnw.entity.Izuchi.class,
                leader.getBoundingBox().inflate(10.0D));
    }

    // ================================================================ R0b: presentation clock
    //
    // What can honestly be tested headlessly, and what cannot. A dedicated server never runs
    // GeckoLib's animation system at all, so nothing here samples a keyframe or looks at a bone --
    // that half of R0b needs a client and a person, and docs/TEST_PLAN.md names it. What a
    // dedicated server *can* prove, and what these cover, is everything the client reads: that the
    // age-to-clip-time contract is the one an on-time observer has always been on, and that every
    // synced presentation anchor exists, is stable for the life of its instance, distinguishes one
    // instance from the next, and survives a save/load as a reconstructed age rather than a restart
    // -- plus that the adapter itself loads and constructs on a server with no client classes.

    /** Great Izuchi's blend length and its scratch clip, so the numbers below are real ones. */
    private static final double IZUCHI_TRANSITION = GreatIzuchi.TRANSITION_TICKS;
    private static final double SCRATCH_CLIP_TICKS = 65.0D;

    /**
     * R0b-01: the clock contract itself.
     *
     * <p>An action of age {@code a} samples clip time {@code a - L}, which is not a new convention:
     * it is exactly what a controller with an {@code L}-tick transition has always shown an observer
     * who was already watching when the action began. That is the whole reason aging a late observer
     * does not move anybody else's contact timing, so it is worth a test that fails if someone
     * "simplifies" it to feeding raw action age straight in as clip time -- the specific mistake the
     * handoff's GeckoLib notes warn about, because it silently shifts an already-measured attack by
     * five ticks.
     *
     * <p>Also pins the terminal clamp: past the end, the sample must stay inside the clip. Landing
     * on zero there is the one-shot looping back to its first frame; landing on or past the length
     * is GeckoLib taking its end branch and dropping the pose to the base skeleton.
     */
    @GameTest(template = ARENA, timeoutTicks = 20)
    public static void animationClockMapsActionAgeToClipTime(GameTestHelper helper) {
        double blend = ServerTimedAnimationController.clipTimeFor(0.0D, IZUCHI_TRANSITION, SCRATCH_CLIP_TICKS);
        helper.assertTrue(blend == 0.0D, "age 0 should still be blending, not at clip time " + blend);

        double atBlendEnd = ServerTimedAnimationController.clipTimeFor(
                IZUCHI_TRANSITION, IZUCHI_TRANSITION, SCRATCH_CLIP_TICKS);
        helper.assertTrue(atBlendEnd == 0.0D,
                "the clip should start exactly when the blend ends, not at " + atBlendEnd);

        double midAction = ServerTimedAnimationController.clipTimeFor(20.0D, IZUCHI_TRANSITION, SCRATCH_CLIP_TICKS);
        helper.assertTrue(Math.abs(midAction - 15.0D) < 1.0E-6D,
                "age 20 should sample clip time 15 (age minus the " + IZUCHI_TRANSITION
                        + "-tick blend), not " + midAction + "; feeding raw action age straight in"
                        + " as clip time shifts every measured attack");

        double fractional = ServerTimedAnimationController.clipTimeFor(20.5D, IZUCHI_TRANSITION, SCRATCH_CLIP_TICKS);
        helper.assertTrue(fractional > midAction && fractional < midAction + 1.0D,
                "a partial tick should interpolate between clip times, not snap: " + fractional);

        double negative = ServerTimedAnimationController.clipTimeFor(-4.0D, IZUCHI_TRANSITION, SCRATCH_CLIP_TICKS);
        helper.assertTrue(negative == 0.0D,
                "a clock reading as ahead of its own start must floor at zero, not " + negative);

        double expired = ServerTimedAnimationController.clipTimeFor(500.0D, IZUCHI_TRANSITION, SCRATCH_CLIP_TICKS);
        helper.assertTrue(expired > SCRATCH_CLIP_TICKS - 1.0D && expired < SCRATCH_CLIP_TICKS,
                "an expired one-shot must hold its last frame, not loop to zero or run off the end: "
                        + expired);
        helper.succeed();
    }

    /**
     * R0b-02: the client/server boundary, proved by the only process that can prove it.
     *
     * <p>{@code ServerTimedAnimationController} is named from common entity registration, so if it
     * ever acquired a {@code net.minecraft.client} import, a static {@code Minecraft} reference or a
     * link back to {@code MHNWClient}, a dedicated server would die class-loading it. This test runs
     * on a dedicated server and forces exactly that: class initialization, and a real construction
     * against a real entity. A compile-time check could not catch it -- the client classes are on
     * the compile classpath either way.
     */
    @GameTest(template = ARENA, timeoutTicks = 20)
    public static void animationControllerLoadsOnADedicatedServer(GameTestHelper helper) {
        GreatIzuchi monster = spawnInert(helper);
        ServerTimedAnimationController<GreatIzuchi> controller =
                new ServerTimedAnimationController<>(monster, "r0b_probe", GreatIzuchi.TRANSITION_TICKS,
                        state -> software.bernie.geckolib.animation.PlayState.STOP);
        helper.assertTrue(controller.getName().equals("r0b_probe"),
                "the adapter did not construct on a dedicated server");
        helper.assertTrue(controller.getAnimationState()
                        == software.bernie.geckolib.animation.AnimationController.State.STOPPED,
                "a freshly constructed adapter should be STOPPED, not " + controller.getAnimationState());
        helper.succeed();
    }

    /**
     * R0b-03: the roar's presentation anchor, and the age convention around its first tick.
     *
     * <p>The countdown is the server's clock -- how much longer to stay frozen -- and it can neither
     * identify an instance nor be joined late. The anchor is the presentation clock. Both have to
     * hold together, so this asserts the relationship between them rather than a hard-coded offset:
     * whatever {@code duration - remaining - age} is on the first tick the roar is observed, it must
     * be that same number for the entire roar. A stale anchor, an anchor re-stamped every tick, and
     * a countdown drifting against game time all break that identity on some tick, and none of them
     * is visible by watching the countdown alone.
     *
     * <p>R0a recorded 70 ticks remaining of a 71-tick Great Izuchi roar and 99 of the 100-tick wyvern
     * roars at first observation. That is a scheduling fact about when the goal's own {@code tick()}
     * first runs, not a shorter clip, so it is accounted for here rather than hidden by changing
     * anybody's duration: the offset is observed, then held to.
     */
    private static <T extends net.minecraft.world.entity.Mob & com.carro1001.mhnw.entity.Roarable>
            void assertRoarAnchorIsStableForTheWholeRoar(GameTestHelper helper, T monster) {
        long[] anchor = {Long.MIN_VALUE};
        int[] offset = {Integer.MIN_VALUE};
        int[] maxAge = {-1};
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(monster.getRoarTicks() > 0, "never started roaring"))
                .thenExecute(() -> {
                    anchor[0] = monster.getRoarStartTime();
                    long age = helper.getLevel().getGameTime() - anchor[0];
                    helper.assertTrue(age >= 0L && age <= SCHEDULING_TOLERANCE,
                            "the roar anchor is not this roar's own start: age " + age
                                    + " on the first tick it was seen roaring");
                    offset[0] = (int) (monster.roarDurationTicks() - monster.getRoarTicks() - age);
                })
                .thenExecuteFor(monster.roarDurationTicks() - SCHEDULING_TOLERANCE, () -> {
                    if (monster.getRoarTicks() <= 0) {
                        return;
                    }
                    helper.assertTrue(monster.getRoarStartTime() == anchor[0],
                            "the roar anchor moved mid-roar, from " + anchor[0] + " to "
                                    + monster.getRoarStartTime() + " -- a presentation instance whose"
                                    + " identity changes restarts its clip every tick");
                    int age = (int) (helper.getLevel().getGameTime() - anchor[0]);
                    maxAge[0] = Math.max(maxAge[0], age);
                    int nowOffset = monster.roarDurationTicks() - monster.getRoarTicks() - age;
                    helper.assertTrue(nowOffset == offset[0],
                            "the anchor and the countdown disagree at age " + age + ": offset was "
                                    + offset[0] + ", now " + nowOffset);
                })
                .thenExecute(() -> helper.assertTrue(
                        maxAge[0] >= monster.roarDurationTicks() - 1 - SCHEDULING_TOLERANCE,
                        "the anchor only ever aged to " + maxAge[0] + " ticks, but this species' roar"
                                + " is " + monster.roarDurationTicks() + " ticks -- a late observer"
                                + " joining near the end would be told the wrong phase"))
                .thenSucceed();
    }

    private static Cow inertVictim(GameTestHelper helper) {
        Cow victim = helper.spawn(EntityType.COW, 8, 2, 9);
        victim.setNoAi(true);
        victim.setInvulnerable(true);
        return victim;
    }

    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void greatIzuchiRoarAnchorIsStableForTheWholeRoar(GameTestHelper helper) {
        GreatIzuchi monster = helper.spawn(ModEntities.GREAT_IZUCHI.get(), 8, 2, 8);
        monster.setTarget(inertVictim(helper));
        assertRoarAnchorIsStableForTheWholeRoar(helper, monster);
    }

    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void rathianRoarAnchorIsStableForTheWholeRoar(GameTestHelper helper) {
        com.carro1001.mhnw.entity.Rathian rathian = helper.spawn(ModEntities.RATHIAN.get(), 8, 2, 8);
        rathian.setTarget(inertVictim(helper));
        assertRoarAnchorIsStableForTheWholeRoar(helper, rathian);
    }

    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void rathalosRoarAnchorIsStableForTheWholeRoar(GameTestHelper helper) {
        com.carro1001.mhnw.entity.Rathalos rathalos = helper.spawn(ModEntities.RATHALOS.get(), 8, 2, 8);
        rathalos.setTarget(inertVictim(helper));
        assertRoarAnchorIsStableForTheWholeRoar(helper, rathalos);
    }

    /**
     * R0b-04: two roars are two instances.
     *
     * <p>A client joining the second roar must not be handed the first one's identity, or the
     * controller sees no change and keeps playing whatever it already had. Anchors are game times,
     * so the second must be strictly later; sharing a value is the failure. Re-arming itself is
     * T02's subject -- the engagement is reset directly here, because this only cares that the
     * anchor moves when a second roar does happen.
     */
    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void greatIzuchiSecondRoarIsADistinctInstance(GameTestHelper helper) {
        GreatIzuchi monster = helper.spawn(ModEntities.GREAT_IZUCHI.get(), 8, 2, 8);
        Cow victim = inertVictim(helper);
        monster.setTarget(victim);

        long[] first = {Long.MIN_VALUE};
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(monster.getRoarTicks() > 0, "never roared"))
                .thenExecute(() -> first[0] = monster.getRoarStartTime())
                .thenWaitUntil(() -> helper.assertTrue(monster.getRoarTicks() <= 0,
                        "the first roar is still running"))
                .thenExecute(() -> monster.setRoaredThisEngagement(false))
                .thenWaitUntil(() -> helper.assertTrue(monster.getRoarTicks() > 0, "never roared again"))
                .thenExecute(() -> helper.assertTrue(monster.getRoarStartTime() > first[0],
                        "the second roar reused the first roar's anchor (" + first[0]
                                + "): a late client cannot tell the two apart"))
                .thenSucceed();
    }

    /**
     * R0b-05: the death anchor exists, is stamped once, and ages one tick per real tick.
     *
     * <p>Vanilla's {@code deathTime} already counts the corpse hold, but it is never synced, so a
     * client that starts tracking a body already part way through its death clip would restart that
     * clip. This is the anchor that fixes it, and the two ways it can be wrong are opposite: never
     * stamped (a late client sees no death clock at all), or re-stamped every tick (the clip never
     * advances, which looks exactly like a frozen corpse). Holding the age to real elapsed ticks
     * catches both.
     */
    private static void assertDeathAnchorAgesWithRealTicks(GameTestHelper helper,
                                                          net.minecraft.world.entity.LivingEntity body,
                                                          java.util.function.LongSupplier anchor,
                                                          long none, int observeTicks) {
        long[] stamped = {none};
        long[] ageWhenStamped = {-1L};
        helper.startSequence()
                .thenExecute(() -> helper.assertTrue(anchor.getAsLong() == none,
                        "a living creature already carries a death anchor: " + anchor.getAsLong()))
                .thenExecute(() -> body.hurt(helper.getLevel().damageSources().genericKill(), 1000.0F))
                .thenWaitUntil(() -> helper.assertTrue(anchor.getAsLong() != none,
                        "death was never stamped with a presentation anchor"))
                .thenExecute(() -> {
                    stamped[0] = anchor.getAsLong();
                    ageWhenStamped[0] = helper.getLevel().getGameTime() - stamped[0];
                    helper.assertTrue(ageWhenStamped[0] >= 0L && ageWhenStamped[0] <= SCHEDULING_TOLERANCE,
                            "the death anchor is not this death's own start: age " + ageWhenStamped[0]);
                })
                .thenIdle(observeTicks)
                .thenExecute(() -> {
                    helper.assertTrue(anchor.getAsLong() == stamped[0],
                            "the death anchor was re-stamped mid-death, from " + stamped[0] + " to "
                                    + anchor.getAsLong() + " -- the clip would never advance");
                    long age = helper.getLevel().getGameTime() - stamped[0];
                    long elapsed = age - ageWhenStamped[0];
                    helper.assertTrue(elapsed >= observeTicks && elapsed <= observeTicks + SCHEDULING_TOLERANCE,
                            "the death age is not running at one tick per real tick: it advanced "
                                    + elapsed + " over " + observeTicks + " ticks");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void greatIzuchiDeathAnchorAgesWithRealTicks(GameTestHelper helper) {
        GreatIzuchi monster = spawnInert(helper);
        assertDeathAnchorAgesWithRealTicks(helper, monster, monster::getDeathStartTime,
                GreatIzuchi.NO_DEATH, 20);
    }

    /**
     * Rathian, Rathalos and Aptonoth keep vanilla's own corpse lifetime (removal at {@code deathTime}
     * 20); only Great Izuchi holds longer, for its authored 38-tick clip. R0b synchronizes what is
     * visible while a body exists and deliberately extends no body's lifetime, so these three are
     * observed inside their real window rather than given a longer one.
     */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void rathianDeathAnchorAgesWithRealTicks(GameTestHelper helper) {
        com.carro1001.mhnw.entity.Rathian rathian = helper.spawn(ModEntities.RATHIAN.get(), 8, 2, 8);
        rathian.setNoAi(true);
        assertDeathAnchorAgesWithRealTicks(helper, rathian, rathian::getDeathStartTime,
                com.carro1001.mhnw.entity.Rathian.NO_DEATH, 12);
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void rathalosDeathAnchorAgesWithRealTicks(GameTestHelper helper) {
        com.carro1001.mhnw.entity.Rathalos rathalos = helper.spawn(ModEntities.RATHALOS.get(), 8, 2, 8);
        rathalos.setNoAi(true);
        assertDeathAnchorAgesWithRealTicks(helper, rathalos, rathalos::getDeathStartTime,
                com.carro1001.mhnw.entity.Rathalos.NO_DEATH, 12);
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void aptonothDeathAnchorAgesWithRealTicks(GameTestHelper helper) {
        Aptonoth aptonoth = helper.spawn(ModEntities.APTONOTH.get(), 8, 2, 8);
        aptonoth.setNoAi(true);
        assertDeathAnchorAgesWithRealTicks(helper, aptonoth, aptonoth::getDeathStartTime,
                Aptonoth.NO_DEATH, 12);
    }

    /**
     * R0b-06: a reloaded body resumes its death, it does not restart it.
     *
     * <p>The anchor is deliberately not persisted. It is reconstructed on the first tick after load
     * from {@code gameTime - deathTime}, and {@code deathTime} is a field vanilla already saves --
     * so there is no second death state machine, no second saved field, and above all no second call
     * into {@code die()}. The failure this catches is the obvious shortcut: stamping plain
     * {@code gameTime} at load, which silently rewinds a half-finished death to frame zero. It also
     * pins that the anchor is genuinely absent in the saved data, which is what lets a body restored
     * into a world at a different game time still be the right age.
     *
     * <p>Not a claim about a real disk restart -- that stays a named R0b client-side gate, and an
     * in-memory round trip does not substitute for it.
     */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void greatIzuchiReloadedBodyResumesItsDeathInsteadOfRestartingIt(GameTestHelper helper) {
        GreatIzuchi original = spawnInert(helper);
        long[] ageAtSave = {-1L};
        GreatIzuchi[] reloadedBody = new GreatIzuchi[1];
        helper.startSequence()
                .thenExecute(() -> original.hurt(helper.getLevel().damageSources().genericKill(), 1000.0F))
                .thenWaitUntil(() -> helper.assertTrue(original.getDeathStartTime() != GreatIzuchi.NO_DEATH,
                        "death was never stamped"))
                .thenIdle(10)
                .thenExecute(() -> {
                    ageAtSave[0] = helper.getLevel().getGameTime() - original.getDeathStartTime();
                    helper.assertTrue(ageAtSave[0] >= 8L,
                            "the fixture saved at death age " + ageAtSave[0] + ", too early to tell a"
                                    + " resumed death from a restarted one");

                    CompoundTag saved = original.saveWithoutId(new CompoundTag());
                    original.discard();
                    GreatIzuchi reloaded = new GreatIzuchi(ModEntities.GREAT_IZUCHI.get(), helper.getLevel());
                    reloaded.load(saved);
                    helper.getLevel().addFreshEntity(reloaded);
                    reloadedBody[0] = reloaded;

                    helper.assertTrue(reloaded.getDeathStartTime() == GreatIzuchi.NO_DEATH,
                            "the death anchor was persisted; it is meant to be reconstructed, so a"
                                    + " body restored into a world at a different game time is still"
                                    + " the right age");
                    helper.assertTrue(reloaded.isDeadOrDying(),
                            "a dead body came back alive across a reload");
                    helper.assertTrue(reloaded.getAttackId() == GreatIzuchi.ATTACK_NONE,
                            "a reloaded body resumed a transient action");
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    GreatIzuchi reloaded = reloadedBody[0];
                    helper.assertTrue(reloaded.getDeathStartTime() != GreatIzuchi.NO_DEATH,
                            "the reloaded body never reconstructed its death anchor");
                    long age = helper.getLevel().getGameTime() - reloaded.getDeathStartTime();
                    helper.assertTrue(age >= ageAtSave[0],
                            "the reloaded body restarted its death clip: age went from " + ageAtSave[0]
                                    + " back to " + age);
                    helper.assertTrue(age - ageAtSave[0] <= 2L + SCHEDULING_TOLERANCE,
                            "the reloaded body's death age jumped from " + ageAtSave[0] + " to " + age);
                    helper.assertTrue(reloaded.getHealth() == 0.0F,
                            "the reloaded body regained health: " + reloaded.getHealth());
                    helper.assertTrue(helper.getLevel().getPartEntities().containsAll(
                                    java.util.Arrays.asList(reloaded.getParts())),
                            "the reloaded body's parts never reached the NeoForge lookup");
                })
                .thenSucceed();
    }

    /**
     * R0b-07: death presentation has a clock of its own to win with.
     *
     * <p>R0a established that a monster killed mid-action freezes its synced action state rather
     * than clearing it, because vanilla stops ticking every goal the instant {@code isDeadOrDying()}
     * becomes true -- so an in-goal guard never runs. That is only harmless because the death branch
     * is checked first. R0b adds the requirement that the death branch also carries a real age, so
     * it is a death clip at the right phase and not merely a death clip. This asserts both halves at
     * once: the stale attack still sitting there, and a genuine death anchor beside it. Choosing the
     * clip and the clock from the same decision is what stops a death pose being driven by that
     * stale attack clock.
     */
    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void greatIzuchiDeathAnchorOutranksAFrozenAttack(GameTestHelper helper) {
        GreatIzuchi monster = helper.spawn(ModEntities.GREAT_IZUCHI.get(), 8, 2, 8);
        Cow victim = inertVictim(helper);
        monster.setRoaredThisEngagement(true);
        monster.attackCooldown = 0;
        monster.setTarget(victim);

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(monster.getAttackId() != GreatIzuchi.ATTACK_NONE,
                        "never started an attack"))
                .thenExecute(() -> monster.hurt(helper.getLevel().damageSources().genericKill(), 1000.0F))
                .thenWaitUntil(() -> helper.assertTrue(monster.getDeathStartTime() != GreatIzuchi.NO_DEATH,
                        "death was never stamped on a monster killed mid-attack"))
                .thenExecute(() -> {
                    helper.assertTrue(monster.getAttackId() != GreatIzuchi.ATTACK_NONE,
                            "the frozen-attack precondition no longer holds, so this test would pass"
                                    + " for the wrong reason; see R0a on goals not ticking once dead");
                    long deathAge = helper.getLevel().getGameTime() - monster.getDeathStartTime();
                    helper.assertTrue(deathAge >= 0L && deathAge <= SCHEDULING_TOLERANCE,
                            "the death clock did not start at the death: age " + deathAge);
                })
                .thenSucceed();
    }

    private static void fillFloor(GameTestHelper helper, int y, net.minecraft.world.level.block.Block block) {
        for (int x = 0; x <= 15; x++) {
            for (int z = 0; z <= 15; z++) {
                helper.setBlock(x, y, z, block);
            }
        }
    }
}
