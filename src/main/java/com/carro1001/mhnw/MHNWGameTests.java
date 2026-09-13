package com.carro1001.mhnw;

import com.carro1001.mhnw.animation.ServerTimedAnimationController;
import com.carro1001.mhnw.entity.Aptonoth;
import com.carro1001.mhnw.entity.AttackProfile;
import com.carro1001.mhnw.entity.CarveState;
import com.carro1001.mhnw.entity.Izuchi;
import com.carro1001.mhnw.entity.IzuchiHarassGoal;
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

    /**
     * The corpse's hurtboxes sit on the fallen body, not in the pose it died standing in. Carving
     * is only reachable through a part, so a head box left floating where the living head was means
     * a player has to hunt for an invisible box above a body lying flat.
     */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void r1CorpsePartsDropToTheGround(GameTestHelper helper) {
        GreatIzuchi monster = spawnInert(helper);
        double standingTop = 0.0;
        for (net.neoforged.neoforge.entity.PartEntity<?> part : monster.getParts()) {
            standingTop = Math.max(standingTop, part.getBoundingBox().maxY - monster.getY());
        }
        helper.assertTrue(standingTop > 1.0,
                "a living Great Izuchi's parts were already flat, so this test proves nothing");

        monster.hurt(helper.getLevel().damageSources().genericKill(), Float.MAX_VALUE);
        helper.runAfterDelay(5, () -> {
            for (net.neoforged.neoforge.entity.PartEntity<?> part : monster.getParts()) {
                double bottom = part.getBoundingBox().minY - monster.getY();
                helper.assertTrue(Math.abs(bottom) < 1.0E-4,
                        "corpse part " + part + " sat " + bottom + " blocks off the ground");
            }
            helper.succeed();
        });
    }

    /** A02/A13: death removes the creature and every one of its parts, exactly once. */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void deathRemovesTheWholeCreature(GameTestHelper helper) {
        GreatIzuchi monster = spawnInert(helper);
        int partCount = monster.getParts().length;
        helper.assertTrue(partCount > 0, "the monster registered no parts at all");

        monster.hurt(helper.getLevel().damageSources().genericKill(), Float.MAX_VALUE);
        // R1 holds this body for the carving window; that window's own contract is proven by the
        // r1Corpse* tests. What is checked here, unchanged, is that the final removal takes the
        // whole creature with it.
        expireCorpse(monster);

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
    /**
     * Jump a corpse to the end of its hold so a test can observe the one final removal without
     * idling for it.
     *
     * <p>R1 holds Great Izuchi, small Izuchi and Aptonoth bodies for {@link CarveState#CORPSE_TICKS}
     * carvable ticks, which no test may sit through. Every species' hold is counted in vanilla's own
     * {@code deathTime}, so writing that field is exactly the state a body reaches by waiting --
     * there is no second counter to get out of step with. Harmless on the species that keep a
     * shorter hold: vanilla removes at 20 either way.
     */
    private static void expireCorpse(net.minecraft.world.entity.LivingEntity subject) {
        subject.deathTime = Math.max(subject.deathTime, CarveState.CORPSE_TICKS);
    }

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
                .thenExecute(() -> expireCorpse(subject))
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
        expireCorpse(aptonoth);

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
        expireCorpse(aptonoth);

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
     * A03/A05: unlike every P3 species, Izuchi is genuinely hostile. Its recovered tail swipe is
     * server-timed and applies damage only through the shared sweep volumes during their active
     * window. This is the one concrete proof that "simple independent targeting" actually
     * connects.
     *
     * <p>R1 replaced the vanilla {@code MeleeAttackGoal} with {@link IzuchiHarassGoal}, so the first
     * hit no longer lands immediately: the Izuchi circles for a randomized 40-80 tick opportunity
     * window before its first dart, and the dart itself has to cross the circling distance. The
     * timeout is sized for that, not for a rusher. What is asserted is unchanged -- that damage
     * actually reaches a target in the authored active window -- and {@code r1IzuchiHarass*}
     * owns the shape of the approach.
     */
    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void izuchiAttacksAndDamagesTarget(GameTestHelper helper) {
        com.carro1001.mhnw.entity.Izuchi izuchi = helper.spawn(ModEntities.IZUCHI.get(), 8, 2, 8);
        Cow victim = helper.spawn(EntityType.COW, 8, 2, 9);
        victim.setNoAi(true);

        izuchi.setTarget(victim);
        float startingHealth = victim.getHealth();

        helper.succeedWhen(() -> {
            helper.assertTrue(victim.getHealth() < startingHealth,
                    "Izuchi never damaged a target standing right next to it");
            helper.assertTrue(izuchi.getAttackId() == Izuchi.ATTACK_TAIL_SWIPE,
                    "Izuchi damaged a target outside its tail-swipe action");
            helper.assertTrue(izuchi.getAttackAge() >= IzuchiHarassGoal.ACTIVE_START
                            && izuchi.getAttackAge() <= IzuchiHarassGoal.ACTIVE_END,
                    "Izuchi damaged a target at action age " + izuchi.getAttackAge()
                            + ", outside the tail-swipe active window");
        });
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

    /** The long neck/tail model is pickable through three native NeoForge parts from spawn. */
    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void izuchiRegistersHeadAndTailParts(GameTestHelper helper) {
        Izuchi izuchi = helper.spawn(ModEntities.IZUCHI.get(), 8, 2, 8);
        String[] names = {"head", "tail_base", "tail_mid"};
        helper.assertTrue(izuchi.isMultipartEntity() && izuchi.getParts().length == names.length,
                "Izuchi must have a head and two tail parts");
        for (int i = 0; i < names.length; i++) {
            MonsterPart part = izuchi.monsterParts()[i];
            helper.assertTrue(part.partName.equals(names[i]) && part.getParent() == izuchi,
                    "wrong Izuchi part at " + i);
            helper.assertTrue(helper.getLevel().getPartEntities().contains(part),
                    "Izuchi part missing from NeoForge lookup: " + part.partName);
            var centre = izuchi.localToWorld(part.localLeft, part.localUp, part.localForward);
            helper.assertTrue(part.getBoundingBox().getCenter().distanceTo(centre) < EPSILON,
                    "Izuchi part was not positioned on spawn: " + part.partName);
        }
        helper.succeed();
    }

    /** One area source may see root and every part, but must damage the parent exactly once. */
    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void izuchiOneSourceAcrossPartsCountsOnce(GameTestHelper helper) {
        Izuchi izuchi = helper.spawn(ModEntities.IZUCHI.get(), 8, 2, 8);
        float before = izuchi.getHealth();
        DamageSource source = helper.getLevel().damageSources().generic();
        izuchi.hurt(source, PROBE_DAMAGE);
        for (MonsterPart part : izuchi.monsterParts()) {
            part.hurt(source, PROBE_DAMAGE);
        }
        helper.assertTrue(Math.abs(before - izuchi.getHealth() - PROBE_DAMAGE) < EPSILON,
                "one source across Izuchi root and parts caused more than one hit");
        helper.succeed();
    }

    /** A13: death removes it, same as every other species. */
    @GameTest(template = ARENA, timeoutTicks = 120)
    public static void izuchiDeathRemovesIt(GameTestHelper helper) {
        com.carro1001.mhnw.entity.Izuchi izuchi = helper.spawn(ModEntities.IZUCHI.get(), 8, 2, 8);
        izuchi.hurt(helper.getLevel().damageSources().genericKill(), Float.MAX_VALUE);
        expireCorpse(izuchi);

        helper.succeedWhen(() -> {
            helper.assertTrue(izuchi.isRemoved(), "Izuchi was not removed after dying");
            assertPartsUnregistered(helper, izuchi.monsterParts());
        });
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
     * <p>Asserts {@code controllerTickFor}, which is the method runtime seeking actually calls --
     * not a parallel copy of the same arithmetic. An earlier version tested a separate helper that
     * {@code process} never invoked, so changing the production subtraction could have left every
     * test green.
     *
     * <p>An action of age {@code a} samples clip time {@code a - L}, which is not a new convention:
     * it is exactly what a controller with an {@code L}-tick transition has always shown an observer
     * who was already watching when the action began. That is the whole reason aging a late observer
     * does not move anybody else's contact timing, so it is worth a test that fails if someone
     * "simplifies" it to feeding raw action age straight in as clip time -- the specific mistake the
     * handoff's GeckoLib notes warn about, because it silently shifts an already-measured attack by
     * five ticks.
     *
     * <p>Also pins the two regimes and the terminal clamp. Below {@code L} the controller's tick is
     * blend progress rather than clip time, so it must equal the age. Past the end, the sample must
     * stay inside the clip: landing on zero there is the one-shot looping back to its first frame,
     * and landing on or past the length is GeckoLib taking its end branch and dropping the pose to
     * the base skeleton.
     */
    @GameTest(template = ARENA, timeoutTicks = 20)
    public static void animationClockMapsActionAgeToClipTime(GameTestHelper helper) {
        double blend = tickFor(0.0D);
        helper.assertTrue(blend == 0.0D, "age 0 should still be blending, not at clip time " + blend);

        double midBlend = tickFor(3.0D);
        helper.assertTrue(Math.abs(midBlend - 3.0D) < 1.0E-6D,
                "inside the blend the controller tick is blend progress, so age 3 should be 3, not "
                        + midBlend);

        double atBlendEnd = tickFor(IZUCHI_TRANSITION);
        helper.assertTrue(atBlendEnd == 0.0D,
                "the clip should start exactly when the blend ends, not at " + atBlendEnd);

        double midAction = tickFor(20.0D);
        helper.assertTrue(Math.abs(midAction - 15.0D) < 1.0E-6D,
                "age 20 should sample clip time 15 (age minus the " + IZUCHI_TRANSITION
                        + "-tick blend), not " + midAction + "; feeding raw action age straight in"
                        + " as clip time shifts every measured attack");

        double fractional = tickFor(20.5D);
        helper.assertTrue(fractional > midAction && fractional < midAction + 1.0D,
                "a partial tick should interpolate between clip times, not snap: " + fractional);

        double negative = tickFor(-4.0D);
        helper.assertTrue(negative == 0.0D,
                "a clock reading as ahead of its own start must floor at zero, not " + negative);

        double expired = tickFor(500.0D);
        helper.assertTrue(expired > SCRATCH_CLIP_TICKS - 1.0D && expired < SCRATCH_CLIP_TICKS,
                "an expired one-shot must hold its last frame, not loop to zero or run off the end: "
                        + expired);
        helper.succeed();
    }

    private static double tickFor(double ageTicks) {
        return ServerTimedAnimationController.controllerTickFor(
                ageTicks, IZUCHI_TRANSITION, SCRATCH_CLIP_TICKS);
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
     * Rathian and Rathalos keep vanilla's own corpse lifetime (removal at {@code deathTime} 20); the
     * three carvable species hold for {@link CarveState#CORPSE_TICKS}. R0b synchronized what is
     * visible while a body exists and extended no lifetime; R1's longer hold is the carving window,
     * not a presentation change. Each of these is still observed well inside its own real window
     * rather than given a longer one.
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
     * Small Izuchi got its own authored death clip with the retargeted animation set, so it now
     * needs the same anchor the other four have. Before this it had none and fell through to the
     * idle branch, leaving a corpse standing and breathing for the whole ten-minute carve window.
     */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void izuchiDeathAnchorAgesWithRealTicks(GameTestHelper helper) {
        Izuchi izuchi = helper.spawn(ModEntities.IZUCHI.get(), 8, 2, 8);
        izuchi.setNoAi(true);
        assertDeathAnchorAgesWithRealTicks(helper, izuchi, izuchi::getDeathStartTime,
                Izuchi.NO_DEATH, 12);
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


    // ================================================================ R1: the first hunting loop
    //
    // Gates R1-01..R1-08 of docs/R1_FIRST_HUNTING_LOOP_HANDOFF.md. What is deliberately NOT here:
    // whether an item icon or the worn armor actually renders. A GameTest server never loads
    // assets/, and R0b already established that a dedicated server refuses to load GeoModel at all
    // -- so the model/texture/geometry side stays a named human gate in docs/TEST_PLAN.md, and what
    // is proven headlessly is the registration, data and state contract underneath it.

    private static final String[] R1_ITEM_IDS = {
            "monster_hide", "monster_claw", "raw_meat", "cooked_meat",
            "bone_head", "bone_chestplate", "bone_legging", "bone_boots"};

    /** A survival-mode carver standing on the corpse, sneaking, with an empty inventory. */
    private static net.minecraft.world.entity.player.Player carver(
            GameTestHelper helper, net.minecraft.world.entity.Entity at) {
        net.minecraft.world.entity.player.Player player =
                helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.setPos(at.getX(), at.getY(), at.getZ());
        player.setShiftKeyDown(true);
        return player;
    }

    private static net.minecraft.world.InteractionResult carve(
            net.minecraft.world.entity.Mob corpse, net.minecraft.world.entity.player.Player player) {
        return corpse.interactAt(player, net.minecraft.world.phys.Vec3.ZERO,
                net.minecraft.world.InteractionHand.MAIN_HAND);
    }

    private static void hurtBy(GameTestHelper helper, net.minecraft.world.entity.Mob mob,
                               net.minecraft.world.entity.player.Player player, float amount) {
        mob.hurt(helper.getLevel().damageSources().playerAttack(player), amount);
    }

    // ---------------------------------------------------------------- R1-01 registry and economy

    /** R1-01: every new item id actually resolves in the item registry. */
    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void r1ItemIdsResolve(GameTestHelper helper) {
        for (String id : R1_ITEM_IDS) {
            net.minecraft.resources.ResourceLocation key =
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MHNW.MOD_ID, id);
            helper.assertTrue(net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(key),
                    "item id never registered: " + key);
        }
        helper.assertTrue(com.carro1001.mhnw.registry.ModItems.MONSTER_HIDE.get()
                        != net.minecraft.world.item.Items.AIR,
                "the hide holder resolved to air");
        helper.succeed();
    }

    /**
     * R1-01: raw meat cooks in all three stations, by actually matching the recipe the way the
     * furnace does, not by reading the file back.
     */
    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void r1RawMeatCooksInEveryStation(GameTestHelper helper) {
        net.minecraft.world.item.crafting.SingleRecipeInput input =
                new net.minecraft.world.item.crafting.SingleRecipeInput(
                        new net.minecraft.world.item.ItemStack(
                                com.carro1001.mhnw.registry.ModItems.RAW_MEAT.get()));
        assertCooks(helper, net.minecraft.world.item.crafting.RecipeType.SMELTING, input, "furnace");
        assertCooks(helper, net.minecraft.world.item.crafting.RecipeType.SMOKING, input, "smoker");
        assertCooks(helper, net.minecraft.world.item.crafting.RecipeType.CAMPFIRE_COOKING, input, "campfire");
        helper.succeed();
    }

    private static <T extends net.minecraft.world.item.crafting.AbstractCookingRecipe> void assertCooks(
            GameTestHelper helper, net.minecraft.world.item.crafting.RecipeType<T> type,
            net.minecraft.world.item.crafting.SingleRecipeInput input, String station) {
        java.util.Optional<net.minecraft.world.item.crafting.RecipeHolder<T>> found =
                helper.getLevel().getServer().getRecipeManager().getRecipeFor(type, input, helper.getLevel());
        helper.assertTrue(found.isPresent(), "raw meat has no " + station + " recipe");
        helper.assertTrue(found.get().value()
                        .getResultItem(helper.getLevel().registryAccess())
                        .is(com.carro1001.mhnw.registry.ModItems.COOKED_MEAT.get()),
                "the " + station + " recipe for raw meat does not produce cooked meat");
    }

    /**
     * R1-01: each armor piece is craftable from its own shaped pattern, matched through the real
     * crafting lookup. A recipe whose declared keys and pattern disagree does not load at all, so
     * this is also the guard on that.
     */
    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void r1BoneArmorRecipesCraft(GameTestHelper helper) {
        assertCrafts(helper, 3, 2, "BHB" + "B B", com.carro1001.mhnw.registry.ModItems.BONE_HEAD.get());
        assertCrafts(helper, 3, 3, "B B" + "BCB" + "HBH",
                com.carro1001.mhnw.registry.ModItems.BONE_CHESTPLATE.get());
        assertCrafts(helper, 3, 3, "HCH" + "B B" + "B B",
                com.carro1001.mhnw.registry.ModItems.BONE_LEGGING.get());
        assertCrafts(helper, 3, 2, "C C" + "B B", com.carro1001.mhnw.registry.ModItems.BONE_BOOTS.get());
        helper.succeed();
    }

    private static void assertCrafts(GameTestHelper helper, int width, int height, String grid,
                                     net.minecraft.world.item.Item expected) {
        java.util.List<net.minecraft.world.item.ItemStack> items = new java.util.ArrayList<>();
        for (int i = 0; i < grid.length(); i++) {
            items.add(switch (grid.charAt(i)) {
                case 'H' -> new net.minecraft.world.item.ItemStack(
                        com.carro1001.mhnw.registry.ModItems.MONSTER_HIDE.get());
                case 'C' -> new net.minecraft.world.item.ItemStack(
                        com.carro1001.mhnw.registry.ModItems.MONSTER_CLAW.get());
                case 'B' -> new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BONE);
                case 'S' -> new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STICK);
                default -> net.minecraft.world.item.ItemStack.EMPTY;
            });
        }
        net.minecraft.world.item.crafting.CraftingInput input =
                net.minecraft.world.item.crafting.CraftingInput.of(width, height, items);
        java.util.Optional<net.minecraft.world.item.crafting.RecipeHolder<
                net.minecraft.world.item.crafting.CraftingRecipe>> found =
                helper.getLevel().getServer().getRecipeManager().getRecipeFor(
                        net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, helper.getLevel());
        helper.assertTrue(found.isPresent(), "no crafting recipe matched the pattern for " + expected);
        helper.assertTrue(found.get().value().assemble(input, helper.getLevel().registryAccess()).is(expected),
                "the matched recipe did not produce " + expected);
    }

    /**
     * R1-01: carving is the only item-reward path, so none of the three carvable species may drop
     * ordinary death loot. Checked behaviourally -- an empty loot table and a missing one look the
     * same from the outside, and what matters is that nothing lands on the ground.
     */
    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void r1CarvableSpeciesDropNoDeathItems(GameTestHelper helper) {
        GreatIzuchi great = spawnInert(helper);
        Izuchi small = helper.spawn(ModEntities.IZUCHI.get(), 6, 2, 6);
        Aptonoth aptonoth = helper.spawn(ModEntities.APTONOTH.get(), 10, 2, 10);
        net.minecraft.world.entity.player.Player hunter = carver(helper, great);
        net.minecraft.world.entity.Mob[] bodies = {great, small, aptonoth};

        helper.startSequence()
                .thenExecute(() -> {
                    for (net.minecraft.world.entity.Mob mob : bodies) {
                        mob.setNoAi(true);
                        hurtBy(helper, mob, hunter, Float.MAX_VALUE);
                        helper.assertTrue(mob.isDeadOrDying(),
                                "the fixture failed to kill " + mob.getName().getString());
                    }
                })
                .thenIdle(40)
                .thenExecute(() -> {
                    for (net.minecraft.world.entity.Mob mob : bodies) {
                        java.util.List<net.minecraft.world.entity.item.ItemEntity> dropped =
                                helper.getLevel().getEntitiesOfClass(
                                        net.minecraft.world.entity.item.ItemEntity.class,
                                        mob.getBoundingBox().inflate(6.0D));
                        helper.assertTrue(dropped.isEmpty(),
                                mob.getName().getString() + " dropped " + dropped.size()
                                        + " death items; carving is meant to be the only reward path");
                    }
                })
                .thenSucceed();
    }

    // ---------------------------------------------------------------- R1-02 attribution

    /**
     * R1-02: only an accepted hit that actually took health off, from a player, grants eligibility.
     * A bystander, an ownerless environmental source and one source enumerating several hurtboxes
     * all fail to add anything they should not.
     */
    @GameTest(template = ARENA, timeoutTicks = 60)
    public static void r1AttributionCreditsOnlyRealPlayerDamage(GameTestHelper helper) {
        GreatIzuchi monster = spawnInert(helper);
        net.minecraft.world.entity.player.Player hunter = carver(helper, monster);
        net.minecraft.world.entity.player.Player bystander = carver(helper, monster);
        CarveState state = monster.carveState();

        helper.assertTrue(state.participantCount() == 0, "a fresh monster already had participants");

        // Proximity alone: the bystander is standing right here and never swings.
        monster.hurt(helper.getLevel().damageSources().cactus(), 3.0F);
        helper.assertTrue(state.participantCount() == 0,
                "ownerless environmental damage granted carving rights");

        hurtBy(helper, monster, hunter, 4.0F);
        helper.assertTrue(state.isParticipant(hunter.getUUID()), "a direct player hit granted nothing");
        helper.assertTrue(!state.isParticipant(bystander.getUUID()),
                "a bystander who never attacked was credited");

        // One source touching several hurtboxes in a tick is one hit; the forwards that the parent
        // rejects must not each credit a participant of their own.
        int before = state.participantCount();
        net.minecraft.world.damagesource.DamageSource splash =
                helper.getLevel().damageSources().playerAttack(bystander);
        for (MonsterPart part : monster.monsterParts()) {
            part.hurt(splash, 2.0F);
        }
        helper.assertTrue(state.participantCount() == before + 1,
                "one multi-part source produced " + (state.participantCount() - before)
                        + " new participants instead of one");
        helper.succeed();
    }

    /** R1-02: a player's thrown projectile credits the player, not the projectile. */
    @GameTest(template = ARENA, timeoutTicks = 60)
    public static void r1AttributionCreditsAProjectilesOwner(GameTestHelper helper) {
        GreatIzuchi monster = spawnInert(helper);
        net.minecraft.world.entity.player.Player hunter = carver(helper, monster);
        net.minecraft.world.entity.projectile.Snowball ball =
                new net.minecraft.world.entity.projectile.Snowball(helper.getLevel(), hunter);

        monster.hurt(helper.getLevel().damageSources().thrown(ball, hunter), 3.0F);

        helper.assertTrue(monster.carveState().isParticipant(hunter.getUUID()),
                "a player's projectile did not credit its owner");
        helper.succeed();
    }

    /**
     * R1-02: an owned attacker credits its owner, and this is the one case that genuinely needs a
     * player in the level -- vanilla's own {@code OwnableEntity.getOwner()} resolves the saved UUID
     * through the level's player list, so a detached mock player could never be found. The player is
     * removed again on the way out so nothing else in the run sees it.
     */
    @GameTest(template = ARENA, timeoutTicks = 60)
    public static void r1AttributionResolvesAnOwnedAttackersPlayer(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer owner = helper.makeMockServerPlayerInLevel();
        try {
            net.minecraft.world.entity.animal.Wolf wolf =
                    helper.spawn(net.minecraft.world.entity.EntityType.WOLF, 6, 2, 6);
            wolf.setNoAi(true);
            wolf.tame(owner);
            helper.assertTrue(wolf.getOwner() == owner,
                    "the fixture's wolf has no resolvable owner, so this test would prove nothing");

            GreatIzuchi monster = spawnInert(helper);
            monster.hurt(helper.getLevel().damageSources().mobAttack(wolf), 4.0F);

            helper.assertTrue(monster.carveState().isParticipant(owner.getUUID()),
                    "a tamed wolf's hit did not credit its owner");
            helper.assertTrue(!monster.carveState().isParticipant(wolf.getUUID()),
                    "the wolf itself was recorded as a participant");
        } finally {
            owner.getServer().getPlayerList().remove(owner);
        }
        helper.succeed();
    }

    /** R1-02: an untamed attacker with no owner at all grants nothing. */
    @GameTest(template = ARENA, timeoutTicks = 60)
    public static void r1AttributionIgnoresAnOwnerlessAttacker(GameTestHelper helper) {
        GreatIzuchi monster = spawnInert(helper);
        net.minecraft.world.entity.animal.Wolf stray =
                helper.spawn(net.minecraft.world.entity.EntityType.WOLF, 6, 2, 6);
        stray.setNoAi(true);

        monster.hurt(helper.getLevel().damageSources().mobAttack(stray), 4.0F);

        helper.assertTrue(monster.carveState().participantCount() == 0,
                "an ownerless wolf granted carving rights");
        helper.succeed();
    }

    // ---------------------------------------------------------------- R1-03 personal quota

    /**
     * R1-03: two players each get their own three carves off one body, in the deterministic order,
     * and neither consumes the other's allowance. A fourth attempt grants nothing.
     */
    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void r1TwoPlayersEachGetTheirOwnThreeCarves(GameTestHelper helper) {
        GreatIzuchi monster = spawnInert(helper);
        net.minecraft.world.entity.player.Player first = carver(helper, monster);
        net.minecraft.world.entity.player.Player second = carver(helper, monster);
        net.minecraft.world.entity.player.Player[] both = {first, second};

        net.minecraft.gametest.framework.GameTestSequence sequence = helper.startSequence()
                .thenExecute(() -> {
                    hurtBy(helper, monster, first, 4.0F);
                    hurtBy(helper, monster, second, 4.0F);
                    monster.hurt(helper.getLevel().damageSources().genericKill(), Float.MAX_VALUE);
                    helper.assertTrue(monster.isDeadOrDying(), "the fixture never killed the monster");
                });

        for (int carveIndex = 0; carveIndex < CarveState.MAX_CARVES; carveIndex++) {
            int index = carveIndex;
            sequence = sequence
                    .thenIdle(CarveState.DEBOUNCE_TICKS + 1)
                    .thenExecute(() -> {
                        for (net.minecraft.world.entity.player.Player player : both) {
                            carve(monster, player);
                            helper.assertTrue(
                                    monster.carveState().carvesUsedBy(player.getUUID()) == index + 1,
                                    "carve " + (index + 1) + " was not counted for one of the two players");
                        }
                    });
        }

        sequence.thenExecute(() -> {
            // The R1 Great Izuchi table, in order: 4 hide, 2 claws, 4 bones.
            for (net.minecraft.world.entity.player.Player player : both) {
                assertHolds(helper, player, com.carro1001.mhnw.registry.ModItems.MONSTER_HIDE.get(), 4);
                assertHolds(helper, player, com.carro1001.mhnw.registry.ModItems.MONSTER_CLAW.get(), 2);
                assertHolds(helper, player, net.minecraft.world.item.Items.BONE, 4);
            }
        }).thenIdle(CarveState.DEBOUNCE_TICKS + 1).thenExecute(() -> {
            carve(monster, first);
            helper.assertTrue(monster.carveState().carvesUsedBy(first.getUUID()) == CarveState.MAX_CARVES,
                    "a fourth carve was granted");
            assertHolds(helper, first, net.minecraft.world.item.Items.BONE, 4);
        }).thenSucceed();
    }

    private static void assertHolds(GameTestHelper helper, net.minecraft.world.entity.player.Player player,
                                    net.minecraft.world.item.Item item, int expected) {
        int actual = player.getInventory().countItem(item);
        helper.assertTrue(actual == expected,
                "expected " + expected + " x " + item + " in the carver's inventory, found " + actual);
    }

    /**
     * R1-03: the gates around a carve, each checked on its own so a failure names the rule it broke.
     * All of them are enforced server-side, inside the shared state, not by the caller.
     */
    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void r1CarveGatesAreEnforcedServerSide(GameTestHelper helper) {
        GreatIzuchi monster = spawnInert(helper);
        net.minecraft.world.entity.player.Player hunter = carver(helper, monster);
        net.minecraft.world.entity.player.Player stranger = carver(helper, monster);

        hurtBy(helper, monster, hunter, 4.0F);
        helper.assertTrue(carve(monster, hunter) == net.minecraft.world.InteractionResult.PASS,
                "a living monster was carvable");

        monster.hurt(helper.getLevel().damageSources().genericKill(), Float.MAX_VALUE);
        CarveState state = monster.carveState();

        monster.interactAt(hunter, net.minecraft.world.phys.Vec3.ZERO,
                net.minecraft.world.InteractionHand.OFF_HAND);
        helper.assertTrue(state.carvesUsedBy(hunter.getUUID()) == 0, "an off-hand interaction carved");

        hunter.setShiftKeyDown(false);
        carve(monster, hunter);
        helper.assertTrue(state.carvesUsedBy(hunter.getUUID()) == 0, "a non-sneaking interaction carved");
        hunter.setShiftKeyDown(true);

        carve(monster, stranger);
        helper.assertTrue(state.carvesUsedBy(stranger.getUUID()) == 0,
                "a player who never damaged the monster carved it");

        double far = CarveState.CARVE_RANGE * 2.0D;
        hunter.setPos(monster.getX() + far, monster.getY(), monster.getZ());
        helper.assertTrue(carve(monster, hunter) == net.minecraft.world.InteractionResult.PASS,
                "a carve landed from " + far + " blocks away");
        helper.assertTrue(state.carvesUsedBy(hunter.getUUID()) == 0, "an out-of-range carve was counted");
        helper.succeed();
    }

    /** R1-03: two interactions inside the debounce window are one carve, not two. */
    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void r1DebounceRejectsARepeatedClick(GameTestHelper helper) {
        GreatIzuchi monster = spawnInert(helper);
        net.minecraft.world.entity.player.Player hunter = carver(helper, monster);

        helper.startSequence()
                .thenExecute(() -> {
                    hurtBy(helper, monster, hunter, 4.0F);
                    monster.hurt(helper.getLevel().damageSources().genericKill(), Float.MAX_VALUE);
                    carve(monster, hunter);
                    carve(monster, hunter);
                    helper.assertTrue(monster.carveState().carvesUsedBy(hunter.getUUID()) == 1,
                            "a held right-click carved twice inside the "
                                    + CarveState.DEBOUNCE_TICKS + "-tick debounce");
                })
                .thenIdle(CarveState.DEBOUNCE_TICKS + 1)
                .thenExecute(() -> {
                    carve(monster, hunter);
                    helper.assertTrue(monster.carveState().carvesUsedBy(hunter.getUUID()) == 2,
                            "the debounce never released");
                })
                .thenSucceed();
    }

    // ---------------------------------------------------------------- R1-04 atomic inventory

    /**
     * R1-04: a reward that cannot fit changes nothing -- not the inventory, not the carve count --
     * and the retry produces the identical deterministic stack.
     */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void r1FullInventoryConsumesNothing(GameTestHelper helper) {
        GreatIzuchi monster = spawnInert(helper);
        net.minecraft.world.entity.player.Player hunter = carver(helper, monster);
        net.minecraft.world.entity.player.Inventory inventory = hunter.getInventory();
        for (int slot = 0; slot < inventory.items.size(); slot++) {
            inventory.items.set(slot, new net.minecraft.world.item.ItemStack(
                    net.minecraft.world.item.Items.STONE, 64));
        }

        helper.startSequence()
                .thenExecute(() -> {
                    hurtBy(helper, monster, hunter, 4.0F);
                    monster.hurt(helper.getLevel().damageSources().genericKill(), Float.MAX_VALUE);
                    carve(monster, hunter);
                    helper.assertTrue(monster.carveState().carvesUsedBy(hunter.getUUID()) == 0,
                            "a carve into a full inventory was still counted");
                    assertHolds(helper, hunter, com.carro1001.mhnw.registry.ModItems.MONSTER_HIDE.get(), 0);
                    helper.assertTrue(inventory.countItem(net.minecraft.world.item.Items.STONE) == 64 * 36,
                            "the full-inventory attempt disturbed what was already carried");
                })
                .thenIdle(CarveState.DEBOUNCE_TICKS + 1)
                .thenExecute(() -> {
                    inventory.items.set(0, net.minecraft.world.item.ItemStack.EMPTY);
                    carve(monster, hunter);
                    helper.assertTrue(monster.carveState().carvesUsedBy(hunter.getUUID()) == 1,
                            "the retry after making room did not carve");
                    // Identical to what the first attempt would have given: the first entry of the
                    // Great Izuchi table, chosen from the carve count and nothing else.
                    assertHolds(helper, hunter, com.carro1001.mhnw.registry.ModItems.MONSTER_HIDE.get(), 4);
                })
                .thenSucceed();
    }

    // ---------------------------------------------------------------- R1-05 persistence

    /**
     * R1-05: participation earned while the creature is alive survives a save/load round trip, so a
     * chunk unload between the first hit and the kill does not erase who fought it.
     */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void r1ParticipationSurvivesALiveRoundTrip(GameTestHelper helper) {
        GreatIzuchi original = spawnInert(helper);
        net.minecraft.world.entity.player.Player hunter = carver(helper, original);
        GreatIzuchi[] reloaded = new GreatIzuchi[1];

        helper.startSequence()
                .thenExecute(() -> {
                    hurtBy(helper, original, hunter, 4.0F);
                    helper.assertTrue(original.isAlive(),
                            "the fixture killed the monster, so this proves nothing about a live round trip");
                    helper.assertTrue(original.carveState().isParticipant(hunter.getUUID()),
                            "the hit was never credited in the first place");

                    CompoundTag saved = original.saveWithoutId(new CompoundTag());
                    original.discard();
                    GreatIzuchi body = new GreatIzuchi(ModEntities.GREAT_IZUCHI.get(), helper.getLevel());
                    body.load(saved);
                    body.setNoAi(true);
                    helper.getLevel().addFreshEntity(body);
                    reloaded[0] = body;

                    helper.assertTrue(body.carveState().isParticipant(hunter.getUUID()),
                            "a live round trip lost the participant list");
                    body.hurt(helper.getLevel().damageSources().genericKill(), Float.MAX_VALUE);
                })
                .thenIdle(CarveState.DEBOUNCE_TICKS + 1)
                .thenExecute(() -> {
                    net.minecraft.world.entity.player.Player latecomer = carver(helper, reloaded[0]);
                    // The same UUID as the original hunter: eligibility is about who fought, not
                    // which Player object happens to be holding the mouse now.
                    carve(reloaded[0], hunter);
                    helper.assertTrue(reloaded[0].carveState().carvesUsedBy(hunter.getUUID()) == 1,
                            "the reloaded body refused a carve to the player who earned it");
                    helper.assertTrue(!reloaded[0].carveState().isParticipant(latecomer.getUUID()),
                            "a reload granted eligibility to somebody who never fought");
                })
                .thenSucceed();
    }

    /**
     * R1-05: a corpse round trip keeps the exact per-player counts and its remaining time, and a
     * reconnect renews neither. The remaining time is vanilla's own {@code deathTime}, so what is
     * really asserted is that nothing re-derives it from wall clock or game time on load.
     */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void r1CorpseRoundTripKeepsCountsAndRemainingTicks(GameTestHelper helper) {
        GreatIzuchi original = spawnInert(helper);
        net.minecraft.world.entity.player.Player hunter = carver(helper, original);
        int[] deathTimeAtSave = {-1};
        GreatIzuchi[] reloaded = new GreatIzuchi[1];

        helper.startSequence()
                .thenExecute(() -> {
                    hurtBy(helper, original, hunter, 4.0F);
                    original.hurt(helper.getLevel().damageSources().genericKill(), Float.MAX_VALUE);
                    carve(original, hunter);
                    helper.assertTrue(original.carveState().carvesUsedBy(hunter.getUUID()) == 1,
                            "the fixture never carved once, so a preserved count would be vacuous");
                })
                .thenIdle(30)
                .thenExecute(() -> {
                    deathTimeAtSave[0] = original.deathTime;
                    helper.assertTrue(deathTimeAtSave[0] >= 25,
                            "the corpse clock barely advanced (" + deathTimeAtSave[0]
                                    + "), too early to tell a preserved timer from a reset one");

                    CompoundTag saved = original.saveWithoutId(new CompoundTag());
                    original.discard();
                    GreatIzuchi body = new GreatIzuchi(ModEntities.GREAT_IZUCHI.get(), helper.getLevel());
                    body.load(saved);
                    helper.getLevel().addFreshEntity(body);
                    reloaded[0] = body;

                    helper.assertTrue(body.isDeadOrDying(), "a corpse came back alive");
                    helper.assertTrue(body.carveState().carvesUsedBy(hunter.getUUID()) == 1,
                            "the reloaded corpse forgot how many carves were already taken: "
                                    + body.carveState().carvesUsedBy(hunter.getUUID()));
                    helper.assertTrue(body.deathTime == deathTimeAtSave[0],
                            "the corpse timer jumped across the round trip, from "
                                    + deathTimeAtSave[0] + " to " + body.deathTime
                                    + "; offline time must not advance it");
                    helper.assertTrue(!body.isRemoved(), "the reloaded corpse removed itself immediately");
                })
                .thenIdle(CarveState.DEBOUNCE_TICKS + 1)
                .thenExecute(() -> {
                    carve(reloaded[0], hunter);
                    helper.assertTrue(reloaded[0].carveState().carvesUsedBy(hunter.getUUID()) == 2,
                            "a reconnect renewed the quota instead of continuing it");
                    assertHolds(helper, hunter, com.carro1001.mhnw.registry.ModItems.MONSTER_CLAW.get(), 2);
                })
                .thenSucceed();
    }

    // ---------------------------------------------------------------- R1-06 corpse lifecycle

    /** R1-06: the configured window, asserted as a value rather than waited out. */
    @GameTest(template = ARENA, timeoutTicks = 20)
    public static void r1CorpseWindowIsTwelveThousandTicks(GameTestHelper helper) {
        helper.assertTrue(CarveState.CORPSE_TICKS == 12_000,
                "the corpse window is " + CarveState.CORPSE_TICKS + ", not the agreed 12,000 ticks");
        helper.assertTrue(CarveState.MAX_CARVES == 3, "the personal allowance is not three carves");
        helper.succeed();
    }

    /**
     * R1-06: all three species stay as inert, visible, part-owning bodies well past vanilla's own
     * 20-tick removal, then leave exactly once at expiry with their parts.
     *
     * <p>Expiry is reached by writing the corpse's own counter rather than by idling 12,000 ticks;
     * see {@link #expireCorpse}. Stopping two ticks short means the removal itself still has to
     * happen through the real {@code tickDeath} path, not by the test doing it.
     */
    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void r1CorpsesStayInertThenExpireExactlyOnce(GameTestHelper helper) {
        GreatIzuchi great = spawnInert(helper);
        Izuchi small = helper.spawn(ModEntities.IZUCHI.get(), 5, 2, 5);
        Aptonoth aptonoth = helper.spawn(ModEntities.APTONOTH.get(), 11, 2, 11);
        small.setNoAi(true);
        aptonoth.setNoAi(true);
        net.minecraft.world.entity.Mob[] bodies = {great, small, aptonoth};
        MonsterPart[] greatParts = great.monsterParts();
        MonsterPart[] aptonothParts = aptonoth.monsterParts();

        helper.startSequence()
                .thenExecute(() -> {
                    for (net.minecraft.world.entity.Mob mob : bodies) {
                        mob.hurt(helper.getLevel().damageSources().genericKill(), Float.MAX_VALUE);
                        helper.assertTrue(mob.isDeadOrDying(), "the fixture failed to kill a subject");
                    }
                })
                .thenIdle(60)
                .thenExecute(() -> {
                    for (net.minecraft.world.entity.Mob mob : bodies) {
                        helper.assertTrue(!mob.isRemoved(),
                                mob.getName().getString() + "'s body was removed inside the carving window");
                        helper.assertTrue(mob.deathTime >= 55,
                                "the corpse clock is not advancing: " + mob.deathTime);
                    }
                    for (MonsterPart part : greatParts) {
                        helper.assertTrue(helper.getLevel().getPartEntities().contains(part),
                                "a Great Izuchi hurtbox left the lookup while its corpse still exists: "
                                        + part.partName);
                    }
                    for (MonsterPart part : aptonothParts) {
                        helper.assertTrue(helper.getLevel().getPartEntities().contains(part),
                                "an Aptonoth hurtbox left the lookup while its corpse still exists: "
                                        + part.partName);
                    }
                    for (net.minecraft.world.entity.Mob mob : bodies) {
                        mob.deathTime = CarveState.CORPSE_TICKS - 2;
                    }
                })
                .thenIdle(6)
                .thenExecute(() -> {
                    for (net.minecraft.world.entity.Mob mob : bodies) {
                        helper.assertTrue(mob.isRemoved(),
                                mob.getName().getString() + "'s body outlived its carving window");
                    }
                    assertPartsUnregistered(helper, greatParts);
                    assertPartsUnregistered(helper, aptonothParts);
                })
                .thenSucceed();
    }

    /**
     * R1-06: one reward owner, one death. The corpse's experience is dropped once, at the real
     * death, and holding the body for the carving window does not re-run any of it.
     */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void r1CorpseGrantsExperienceOnlyOnce(GameTestHelper helper) {
        GreatIzuchi monster = spawnInert(helper);
        net.minecraft.world.entity.player.Player hunter = carver(helper, monster);
        int[] firstTotal = {-1};

        helper.startSequence()
                .thenExecute(() -> hurtBy(helper, monster, hunter, Float.MAX_VALUE))
                .thenIdle(10)
                .thenExecute(() -> {
                    firstTotal[0] = experienceNear(helper, monster);
                    helper.assertTrue(firstTotal[0] > 0,
                            "the kill dropped no experience at all, so a duplicate would be invisible");
                })
                .thenIdle(80)
                .thenExecute(() -> {
                    int now = experienceNear(helper, monster);
                    helper.assertTrue(now == firstTotal[0],
                            "the held corpse granted experience again: " + firstTotal[0] + " -> " + now);
                    helper.assertTrue(!monster.isRemoved(),
                            "the body left before the window, so nothing was really held");
                })
                .thenSucceed();
    }

    private static int experienceNear(GameTestHelper helper, net.minecraft.world.entity.Entity at) {
        int total = 0;
        for (net.minecraft.world.entity.ExperienceOrb orb : helper.getLevel().getEntitiesOfClass(
                net.minecraft.world.entity.ExperienceOrb.class, at.getBoundingBox().inflate(8.0D))) {
            total += orb.getValue();
        }
        return total;
    }

    // ---------------------------------------------------------------- R1-07 armor

    /** R1-07: every slot's protection, durability and enchantability equals iron's, with no
     * toughness and no built-in knockback resistance of its own. */
    @GameTest(template = ARENA, timeoutTicks = 20)
    public static void r1BoneArmorMatchesIronStats(GameTestHelper helper) {
        assertMatchesIron(helper, com.carro1001.mhnw.registry.ModItems.BONE_HEAD.get(),
                net.minecraft.world.item.Items.IRON_HELMET);
        assertMatchesIron(helper, com.carro1001.mhnw.registry.ModItems.BONE_CHESTPLATE.get(),
                net.minecraft.world.item.Items.IRON_CHESTPLATE);
        assertMatchesIron(helper, com.carro1001.mhnw.registry.ModItems.BONE_LEGGING.get(),
                net.minecraft.world.item.Items.IRON_LEGGINGS);
        assertMatchesIron(helper, com.carro1001.mhnw.registry.ModItems.BONE_BOOTS.get(),
                net.minecraft.world.item.Items.IRON_BOOTS);
        helper.succeed();
    }

    private static void assertMatchesIron(GameTestHelper helper, net.minecraft.world.item.Item bone,
                                          net.minecraft.world.item.Item iron) {
        net.minecraft.world.item.ArmorItem boneArmor = (net.minecraft.world.item.ArmorItem) bone;
        net.minecraft.world.item.ArmorItem ironArmor = (net.minecraft.world.item.ArmorItem) iron;
        helper.assertTrue(boneArmor.getDefense() == ironArmor.getDefense(),
                bone + " defends for " + boneArmor.getDefense() + ", iron for " + ironArmor.getDefense());
        helper.assertTrue(boneArmor.getToughness() == 0.0F,
                bone + " carries armor toughness " + boneArmor.getToughness() + "; R1 agreed on zero");
        helper.assertTrue(boneArmor.getMaterial().value().knockbackResistance() == 0.0F,
                bone + " has built-in knockback resistance; the set bonus is meant to be the only source");
        helper.assertTrue(boneArmor.getEnchantmentValue() == ironArmor.getEnchantmentValue(),
                bone + "'s enchantability does not match iron's");
        helper.assertTrue(new net.minecraft.world.item.ItemStack(bone).getMaxDamage()
                        == new net.minecraft.world.item.ItemStack(iron).getMaxDamage(),
                bone + "'s durability does not match iron's");
    }

    /**
     * R1-07: the full-set trait, driven through the real equipment-change hook on a ticking entity
     * rather than by calling the recompute directly. Exactly +0.1, gone the moment the set is
     * incomplete or mixed, and un-stackable across a re-equip.
     */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void r1BoneArmorFullSetAddsOneKnockbackModifier(GameTestHelper helper) {
        net.minecraft.world.entity.monster.Zombie wearer =
                helper.spawn(net.minecraft.world.entity.EntityType.ZOMBIE, 8, 2, 8);
        wearer.setNoAi(true);
        double base = wearer.getAttributeBaseValue(
                net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE);

        helper.startSequence()
                .thenExecute(() -> equipFullSet(wearer))
                .thenIdle(4)
                .thenExecute(() -> assertSetBonus(helper, wearer, base, true, "a full set"))
                .thenExecute(() -> wearer.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET,
                        net.minecraft.world.item.ItemStack.EMPTY))
                .thenIdle(4)
                .thenExecute(() -> assertSetBonus(helper, wearer, base, false, "a set missing its boots"))
                .thenExecute(() -> wearer.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET,
                        new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_BOOTS)))
                .thenIdle(4)
                .thenExecute(() -> assertSetBonus(helper, wearer, base, false, "a set mixed with iron boots"))
                .thenExecute(() -> equipFullSet(wearer))
                .thenIdle(4)
                .thenExecute(() -> assertSetBonus(helper, wearer, base, true, "a re-equipped full set"))
                .thenExecute(() -> equipFullSet(wearer))
                .thenIdle(4)
                .thenExecute(() -> assertSetBonus(helper, wearer, base, true, "a twice-equipped full set"))
                .thenSucceed();
    }

    private static void equipFullSet(net.minecraft.world.entity.LivingEntity wearer) {
        wearer.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,
                new net.minecraft.world.item.ItemStack(com.carro1001.mhnw.registry.ModItems.BONE_HEAD.get()));
        wearer.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST,
                new net.minecraft.world.item.ItemStack(com.carro1001.mhnw.registry.ModItems.BONE_CHESTPLATE.get()));
        wearer.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS,
                new net.minecraft.world.item.ItemStack(com.carro1001.mhnw.registry.ModItems.BONE_LEGGING.get()));
        wearer.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET,
                new net.minecraft.world.item.ItemStack(com.carro1001.mhnw.registry.ModItems.BONE_BOOTS.get()));
    }

    private static void assertSetBonus(GameTestHelper helper, net.minecraft.world.entity.LivingEntity wearer,
                                       double base, boolean expected, String what) {
        net.minecraft.world.entity.ai.attributes.AttributeInstance resistance = wearer.getAttribute(
                net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE);
        helper.assertTrue(resistance != null, "the wearer has no knockback-resistance attribute at all");
        boolean present = resistance.hasModifier(com.carro1001.mhnw.item.BoneArmorItem.SET_BONUS_ID);
        helper.assertTrue(present == expected,
                what + (expected ? " did not add" : " still carries") + " the set bonus");
        double want = base + (expected
                ? com.carro1001.mhnw.item.BoneArmorItem.SET_BONUS_KNOCKBACK_RESISTANCE : 0.0D);
        helper.assertTrue(Math.abs(resistance.getValue() - want) < 1.0E-6D,
                what + " produced knockback resistance " + resistance.getValue() + ", expected " + want
                        + " -- a doubled value means the modifier stacked");
    }

    // ---------------------------------------------------------------- R1-08 Izuchi harassment

    /**
     * R1-08: under ordinary goal scheduling an Izuchi circles, takes a bounded dart, and pulls back.
     * Nothing here calls the goal's own methods: the phases are observed from outside, one sample
     * per real tick, and the assertions are bounds rather than an expected script.
     */
    @GameTest(template = ARENA, timeoutTicks = 600)
    public static void r1IzuchiCirclesDartsAndRetreats(GameTestHelper helper) {
        fillFloor(helper, 1, net.minecraft.world.level.block.Blocks.STONE);
        Izuchi izuchi = helper.spawn(ModEntities.IZUCHI.get(), 8, 2, 8);
        Cow victim = helper.spawn(EntityType.COW, 8, 2, 9);
        victim.setNoAi(true);

        java.util.EnumSet<IzuchiHarassGoal.Phase> seen =
                java.util.EnumSet.noneOf(IzuchiHarassGoal.Phase.class);
        int[] dartRun = {0};
        int[] longestDart = {0};
        int[] hitsThisAttack = {0};
        float[] victimHealth = {victim.getHealth()};

        helper.startSequence()
                .thenExecute(() -> izuchi.setTarget(victim))
                .thenExecuteFor(500, () -> {
                    izuchi.setTarget(victim);
                    IzuchiHarassGoal.Phase phase = izuchi.harassPhase();
                    if (phase != null) {
                        seen.add(phase);
                    }
                    if (victim.getHealth() < victimHealth[0] - EPSILON) {
                        victimHealth[0] = victim.getHealth();
                        hitsThisAttack[0]++;
                        helper.assertTrue(phase == IzuchiHarassGoal.Phase.ATTACK,
                                "an Izuchi landed a hit while in phase " + phase
                                        + "; damage is meant to come from its tail swipe");
                    }
                    if (phase == IzuchiHarassGoal.Phase.DART) {
                        dartRun[0]++;
                        longestDart[0] = Math.max(longestDart[0], dartRun[0]);
                    } else {
                        dartRun[0] = 0;
                    }
                    if (phase == IzuchiHarassGoal.Phase.ATTACK) {
                        helper.assertTrue(hitsThisAttack[0] <= 1,
                                "one tail swipe landed " + hitsThisAttack[0] + " hits");
                    } else {
                        hitsThisAttack[0] = 0;
                    }
                })
                .thenExecute(() -> {
                    helper.assertTrue(seen.contains(IzuchiHarassGoal.Phase.CIRCLE), "it never circled");
                    helper.assertTrue(seen.contains(IzuchiHarassGoal.Phase.DART), "it never darted in");
                    helper.assertTrue(seen.contains(IzuchiHarassGoal.Phase.ATTACK),
                            "it never committed a tail swipe after darting in");
                    helper.assertTrue(seen.contains(IzuchiHarassGoal.Phase.RETREAT),
                            "it never backed off after a dart; this is the constant-melee regression");
                    helper.assertTrue(longestDart[0] > 0
                                    && longestDart[0] <= IzuchiHarassGoal.DART_MAX_TICKS + SCHEDULING_TOLERANCE,
                            "a dart ran for " + longestDart[0] + " ticks, past its "
                                    + IzuchiHarassGoal.DART_MAX_TICKS + "-tick bound");
                })
                .thenSucceed();
    }


    /**
     * R1-08: a dart that never arrives still ends on its own deadline, and hands the pack's slot
     * back when it does.
     *
     * <p>Separate from {@link #r1IzuchiCirclesDartsAndRetreats} on purpose. There, the dart always
     * reaches melee range and ends on its hit, so the {@value IzuchiHarassGoal#DART_MAX_TICKS}-tick
     * deadline is never the thing that stops it -- verified by mutation: multiplying the deadline by
     * a hundred leaves that test passing. Here the target is sealed in stone and unreachable, so the
     * deadline is the only way out, and a member that could otherwise hold the slot forever is the
     * exact failure being guarded against.
     *
     * <p>Everything asserted is about darts that were observed to <em>end</em>. An earlier version
     * also required the Izuchi not to be darting at the 500-tick boundary, which samples a
     * repeating randomized loop at an arbitrary instant: a perfectly legal dart that happened to
     * start just before the window closed failed the test, and it did, once, in review. The
     * completed-dart count is the deterministic form of the same claim -- and it is still what
     * catches a broken deadline, which produces one dart that never ends and therefore none that
     * completed.
     */
    @GameTest(template = ARENA, timeoutTicks = 600)
    public static void r1IzuchiUnreachableDartEndsOnItsDeadline(GameTestHelper helper) {
        fillFloor(helper, 1, net.minecraft.world.level.block.Blocks.STONE);
        Izuchi izuchi = helper.spawn(ModEntities.IZUCHI.get(), 3, 2, 8);
        Cow victim = helper.spawn(EntityType.COW, 12, 2, 8);
        victim.setNoAi(true);
        victim.setInvulnerable(true);
        for (int x = 11; x <= 13; x++) {
            for (int y = 1; y <= 5; y++) {
                for (int z = 7; z <= 9; z++) {
                    if (x == 11 || x == 13 || y == 1 || y == 5 || z == 7 || z == 9) {
                        helper.setBlock(x, y, z, net.minecraft.world.level.block.Blocks.STONE);
                    }
                }
            }
        }
        int[] dartRun = {0};
        int[] completedDarts = {0};
        int[] longestCompletedDart = {0};

        helper.startSequence()
                .thenExecute(() -> izuchi.setTarget(victim))
                .thenExecuteFor(500, () -> {
                    izuchi.setTarget(victim);
                    if (izuchi.isDarting()) {
                        dartRun[0]++;
                    } else if (dartRun[0] > 0) {
                        completedDarts[0]++;
                        longestCompletedDart[0] = Math.max(longestCompletedDart[0], dartRun[0]);
                        dartRun[0] = 0;
                    }
                })
                .thenExecute(() -> {
                    helper.assertTrue(completedDarts[0] > 0,
                            "no dart at an unreachable target ever ended: a member that cannot reach"
                                    + " its target would hold the pack's one dart slot indefinitely");
                    helper.assertTrue(longestCompletedDart[0]
                                    <= IzuchiHarassGoal.DART_MAX_TICKS + SCHEDULING_TOLERANCE,
                            "a dart at an unreachable target ran " + longestCompletedDart[0]
                                    + " ticks before releasing, past its "
                                    + IzuchiHarassGoal.DART_MAX_TICKS + "-tick deadline");
                    helper.assertTrue(victim.getHealth() >= victim.getMaxHealth() - EPSILON,
                            "the sealed target was reached after all, so this proves nothing");
                })
                .thenSucceed();
    }

    /** R1-08: at most one member of a nearby pack owns the dart-and-swipe turn at once. */
    @GameTest(template = ARENA, timeoutTicks = 600)
    public static void r1IzuchiPackKeepsOneDarterAtATime(GameTestHelper helper) {
        fillFloor(helper, 1, net.minecraft.world.level.block.Blocks.STONE);
        Cow victim = helper.spawn(EntityType.COW, 8, 2, 8);
        victim.setNoAi(true);
        victim.setInvulnerable(true);
        java.util.List<Izuchi> pack = java.util.List.of(
                helper.spawn(ModEntities.IZUCHI.get(), 6, 2, 6),
                helper.spawn(ModEntities.IZUCHI.get(), 10, 2, 6),
                helper.spawn(ModEntities.IZUCHI.get(), 6, 2, 10));
        boolean[] sawADart = {false};

        helper.startSequence()
                .thenExecuteFor(500, () -> {
                    int takingTurn = 0;
                    for (Izuchi member : pack) {
                        member.setTarget(victim);
                        if (member.isTakingAttackTurn()) {
                            takingTurn++;
                        }
                    }
                    sawADart[0] |= takingTurn > 0;
                    helper.assertTrue(takingTurn <= 1,
                            takingTurn + " Izuchi took an attack turn at once; the pack is meant to take turns");
                })
                .thenExecute(() -> helper.assertTrue(sawADart[0],
                        "no member of the pack ever darted, so the one-darter rule was never exercised"))
                .thenSucceed();
    }

    /** R1-08: losing the target stops the goal and clears the phase, within vanilla's own poll. */
    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void r1IzuchiTargetLossClearsTheHarassment(GameTestHelper helper) {
        fillFloor(helper, 1, net.minecraft.world.level.block.Blocks.STONE);
        Izuchi izuchi = helper.spawn(ModEntities.IZUCHI.get(), 8, 2, 8);
        Cow victim = helper.spawn(EntityType.COW, 8, 2, 9);
        victim.setNoAi(true);
        victim.setInvulnerable(true);

        helper.startSequence()
                .thenExecute(() -> izuchi.setTarget(victim))
                .thenWaitUntil(() -> {
                    izuchi.setTarget(victim);
                    helper.assertTrue(izuchi.harassPhase() != null, "the harassment goal never started");
                })
                .thenExecute(() -> {
                    izuchi.setTarget(null);
                    victim.discard();
                })
                .thenIdle(SCHEDULING_TOLERANCE + 2)
                .thenExecute(() -> helper.assertTrue(izuchi.harassPhase() == null,
                        "the harassment phase outlived its target: " + izuchi.harassPhase()))
                .thenSucceed();
    }


    /**
     * R1-08: peaceful difficulty cancels the harassment.
     *
     * <p>Not redundant with vanilla. Peaceful discards hostile mobs through {@code Mob.checkDespawn}
     * only when their {@code shouldDespawnInPeaceful()} agrees, and {@link Izuchi} deliberately
     * returns false, so a world switched to peaceful mid-fight keeps both the Izuchi and its target.
     * Without the difficulty clause in the goal's own precondition it keeps circling and darting at
     * a player who is supposed to be safe.
     *
     * <p>The difficulty is passed in rather than set on the level. A GameTest world is shared with
     * every test running beside it, and difficulty is global: flipping it to peaceful for even a few
     * ticks discards other tests' vanilla hostile mobs, which is exactly the cross-test interference
     * the arena fixture note in {@code docs/TEST_PLAN.md} already warns about. So this asserts the
     * rule against the real precondition and asserts that {@code canUse()} routes through it at the
     * live difficulty; a genuine in-world peaceful switch stays a named human check.
     */
    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void r1IzuchiHarassmentStopsOnPeaceful(GameTestHelper helper) {
        Izuchi izuchi = helper.spawn(ModEntities.IZUCHI.get(), 8, 2, 8);
        Cow victim = helper.spawn(EntityType.COW, 8, 2, 9);
        victim.setNoAi(true);
        victim.setInvulnerable(true);
        izuchi.setTarget(victim);

        helper.assertTrue(
                !IzuchiHarassGoal.canHarass(izuchi, victim, net.minecraft.world.Difficulty.PEACEFUL),
                "an Izuchi would keep harassing a live target on peaceful difficulty");
        helper.assertTrue(
                IzuchiHarassGoal.canHarass(izuchi, victim, net.minecraft.world.Difficulty.EASY),
                "the peaceful guard also refused an ordinary difficulty");
        helper.assertTrue(
                !IzuchiHarassGoal.canHarass(izuchi, null, net.minecraft.world.Difficulty.EASY),
                "the precondition accepted a null target");

        // And the running goal really does consult it, at whatever the level's difficulty is.
        net.minecraft.world.Difficulty live = helper.getLevel().getDifficulty();
        helper.assertTrue(live != net.minecraft.world.Difficulty.PEACEFUL,
                "the test world is already peaceful, so the next assertion would be vacuous");
        helper.assertTrue(IzuchiHarassGoal.canHarass(izuchi, izuchi.getTarget(), live),
                "the live precondition disagrees with the goal's own inputs");
        helper.succeed();
    }

    /**
     * R1-08: death clears the harassment state, rather than freezing it for the corpse window.
     *
     * <p>This is the case an in-goal guard cannot cover, and the repository's own lifecycle note
     * says why: vanilla stops ticking every goal the instant {@code isDeadOrDying()} is true, so
     * {@code IzuchiHarassGoal.stop()} never runs for a mob killed mid-dart. Clearing it in
     * {@code die()} is the only hook left. The kill is deliberately delivered while a dart is
     * genuinely in flight, so a passing run cannot be one where there was nothing to clear.
     */
    @GameTest(template = ARENA, timeoutTicks = 600)
    public static void r1IzuchiDeathClearsTheHarassmentState(GameTestHelper helper) {
        fillFloor(helper, 1, net.minecraft.world.level.block.Blocks.STONE);
        Izuchi izuchi = helper.spawn(ModEntities.IZUCHI.get(), 8, 2, 8);
        Cow victim = helper.spawn(EntityType.COW, 8, 2, 9);
        victim.setNoAi(true);
        victim.setInvulnerable(true);

        helper.startSequence()
                .thenExecute(() -> izuchi.setTarget(victim))
                .thenWaitUntil(() -> {
                    izuchi.setTarget(victim);
                    helper.assertTrue(izuchi.isDarting(),
                            "waiting for a dart to be genuinely in flight before the kill");
                })
                .thenExecute(() -> izuchi.hurt(helper.getLevel().damageSources().genericKill(),
                        Float.MAX_VALUE))
                .thenIdle(20)
                .thenExecute(() -> {
                    helper.assertTrue(izuchi.isDeadOrDying() && !izuchi.isRemoved(),
                            "the fixture needs a held corpse to check, not a removed entity");
                    helper.assertTrue(izuchi.harassPhase() == null,
                            "a corpse is still carrying harassment phase " + izuchi.harassPhase());
                    helper.assertTrue(!izuchi.isDarting(),
                            "a corpse is still holding the pack's dart slot");
                    helper.assertTrue(!izuchi.isAggressive(),
                            "a corpse is still flagged aggressive");
                })
                .thenSucceed();
    }

    /** R1-08: a dart in flight is transient -- a reload starts from nothing, never mid-lunge. */
    @GameTest(template = ARENA, timeoutTicks = 600)
    public static void r1IzuchiReloadDoesNotResumeADart(GameTestHelper helper) {
        fillFloor(helper, 1, net.minecraft.world.level.block.Blocks.STONE);
        Izuchi original = helper.spawn(ModEntities.IZUCHI.get(), 8, 2, 8);
        Cow victim = helper.spawn(EntityType.COW, 8, 2, 9);
        victim.setNoAi(true);
        victim.setInvulnerable(true);

        helper.startSequence()
                .thenExecute(() -> original.setTarget(victim))
                .thenWaitUntil(() -> {
                    original.setTarget(victim);
                    helper.assertTrue(original.isDarting(),
                            "waiting for a dart to be genuinely in flight before saving");
                })
                .thenExecute(() -> {
                    CompoundTag saved = original.saveWithoutId(new CompoundTag());
                    original.discard();
                    Izuchi reloaded = new Izuchi(ModEntities.IZUCHI.get(), helper.getLevel());
                    reloaded.load(saved);
                    helper.getLevel().addFreshEntity(reloaded);
                    helper.assertTrue(reloaded.harassPhase() == null,
                            "a reloaded Izuchi resumed a half-finished dart: " + reloaded.harassPhase());
                    helper.assertTrue(!reloaded.isDarting(),
                            "a reloaded Izuchi still held the pack's dart slot");
                })
                .thenSucceed();
    }

    // ================================================================ R2: field preparation
    //
    // Gates R2-01..R2-11 of docs/R2_FIELD_PREPARATION_HANDOFF.md. What is deliberately NOT here,
    // for the same reason as the R1 block: whether the two temporary vanilla icons read clearly,
    // whether the thrown bomb renders through its flight, and whether a filled bucket's art matches
    // the creature. A GameTest server loads no assets at all, so those stay named human gates in
    // docs/TEST_PLAN.md and what is proven headlessly is the transaction and state contract.

    private static final String[] R2_ITEM_IDS = {
            "bbq_spit", "bottled_flashbug", "flash_bomb",
            "poisontoad_bucket", "sleeptoad_bucket", "paratoad_bucket", "nitrotoad_bucket"};

    private static net.minecraft.world.entity.player.Player preparer(
            GameTestHelper helper, double x, double y, double z) {
        net.minecraft.world.entity.player.Player player =
                helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.setPos(helper.absoluteVec(new net.minecraft.world.phys.Vec3(x, y, z)));
        return player;
    }

    /**
     * A real, level-resident player. Needed wherever vanilla's own code casts to {@code ServerPlayer}
     * to award a criterion -- {@code Bucketable.bucketMobPickup} does, so a detached mock player
     * cannot catch a toad at all. Callers must remove it again; see {@link #retire}.
     */
    private static net.minecraft.server.level.ServerPlayer realPreparer(
            GameTestHelper helper, double x, double y, double z) {
        net.minecraft.server.level.ServerPlayer player = helper.makeMockServerPlayerInLevel();
        net.minecraft.world.phys.Vec3 at = helper.absoluteVec(new net.minecraft.world.phys.Vec3(x, y, z));
        player.moveTo(at.x, at.y, at.z, 0.0F, 0.0F);
        // The fixture arrives with creative abilities, under which vanilla's filled-container rule
        // deliberately keeps the input stack -- correct behaviour, but not the survival transaction
        // most of these tests are about. Callers that want the creative path ask for it explicitly.
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        return player;
    }

    private static void retire(net.minecraft.server.level.ServerPlayer player) {
        player.getServer().getPlayerList().remove(player);
    }

    private static int countInInventory(net.minecraft.world.entity.player.Player player,
                                        net.minecraft.world.item.Item item) {
        int total = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            net.minecraft.world.item.ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    // ---------------------------------------------------------------- R2-01 registry and data

    /** R2-01: every new preparation item id, and the projectile entity type, actually resolve. */
    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void r2ItemIdsResolve(GameTestHelper helper) {
        for (String id : R2_ITEM_IDS) {
            net.minecraft.resources.ResourceLocation key =
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MHNW.MOD_ID, id);
            helper.assertTrue(net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(key),
                    "item id never registered: " + key);
        }
        helper.assertTrue(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.containsKey(
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MHNW.MOD_ID, "flash_bomb")),
                "the flash bomb projectile entity type never registered");
        helper.succeed();
    }

    /**
     * R2-01: the four bucket items map to the four variants one-for-one and with no collisions.
     * This is the mapping the preserved icons were drawn for, so getting it backwards would ship a
     * bucket whose picture disagrees with what comes out of it.
     */
    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void r2ToadBucketVariantMappingIsExact(GameTestHelper helper) {
        String[] expected = {"poisontoad_bucket", "sleeptoad_bucket", "paratoad_bucket", "nitrotoad_bucket"};
        Toad.Variant[] variants = {Toad.Variant.POISON, Toad.Variant.SLEEP,
                Toad.Variant.PARALYSIS, Toad.Variant.BLAST};
        for (int i = 0; i < variants.length; i++) {
            net.minecraft.world.item.Item bucket =
                    com.carro1001.mhnw.registry.ModItems.toadBucket(variants[i]).get();
            net.minecraft.resources.ResourceLocation id =
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(bucket);
            helper.assertTrue(id.getPath().equals(expected[i]),
                    variants[i] + " mapped to " + id + ", expected " + expected[i]);
            helper.assertTrue(bucket instanceof com.carro1001.mhnw.item.ToadBucketItem toadBucket
                            && toadBucket.variant() == variants[i],
                    expected[i] + " does not itself carry variant " + variants[i]);
        }
        helper.succeed();
    }

    /**
     * R2-01/R2-04: both shapeless recipes are packaged, match their exact inputs and yield exactly
     * one result. The flash-bomb recipe additionally has to give the glass bottle back exactly once
     * -- proven through vanilla's own remaining-items path, not by trusting the item property.
     */
    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void r2PreparationRecipesMatchTheirExactInputs(GameTestHelper helper) {
        assertShapeless(helper, "bbq_spit",
                new net.minecraft.world.item.ItemStack(com.carro1001.mhnw.registry.ModItems.RAW_MEAT.get()),
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STICK),
                com.carro1001.mhnw.registry.ModItems.BBQ_SPIT.get(),
                null);
        assertShapeless(helper, "flash_bomb",
                new net.minecraft.world.item.ItemStack(com.carro1001.mhnw.registry.ModItems.BOTTLED_FLASHBUG.get()),
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.PAPER),
                com.carro1001.mhnw.registry.ModItems.FLASH_BOMB.get(),
                net.minecraft.world.item.Items.GLASS_BOTTLE);
        helper.succeed();
    }

    private static void assertShapeless(GameTestHelper helper, String recipeId,
                                        net.minecraft.world.item.ItemStack first,
                                        net.minecraft.world.item.ItemStack second,
                                        net.minecraft.world.item.Item result,
                                        net.minecraft.world.item.Item expectedRemainder) {
        net.minecraft.resources.ResourceLocation key =
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MHNW.MOD_ID, recipeId);
        net.minecraft.world.item.crafting.RecipeHolder<?> holder =
                helper.getLevel().getServer().getRecipeManager().byKey(key).orElse(null);
        helper.assertTrue(holder != null, "recipe not packaged: " + key);
        helper.assertTrue(holder.value() instanceof net.minecraft.world.item.crafting.CraftingRecipe,
                key + " is not a crafting recipe");
        net.minecraft.world.item.crafting.CraftingRecipe recipe =
                (net.minecraft.world.item.crafting.CraftingRecipe) holder.value();

        net.minecraft.world.item.crafting.CraftingInput input =
                net.minecraft.world.item.crafting.CraftingInput.of(2, 1, java.util.List.of(first, second));
        helper.assertTrue(recipe.matches(input, helper.getLevel()),
                key + " did not match its own documented inputs");

        net.minecraft.world.item.ItemStack assembled =
                recipe.assemble(input, helper.getLevel().registryAccess());
        helper.assertTrue(assembled.is(result) && assembled.getCount() == 1,
                key + " yielded " + assembled + " instead of exactly one " + result);

        // The wrong ingredient must not also satisfy it, or "exact inputs" would mean nothing.
        net.minecraft.world.item.crafting.CraftingInput wrong =
                net.minecraft.world.item.crafting.CraftingInput.of(2, 1, java.util.List.of(
                        new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIRT), second));
        helper.assertTrue(!recipe.matches(wrong, helper.getLevel()),
                key + " matched an unrelated ingredient");

        net.minecraft.core.NonNullList<net.minecraft.world.item.ItemStack> remaining =
                recipe.getRemainingItems(input);
        int bottles = 0;
        for (net.minecraft.world.item.ItemStack stack : remaining) {
            if (!stack.isEmpty()) {
                bottles += stack.getCount();
                helper.assertTrue(expectedRemainder != null && stack.is(expectedRemainder),
                        key + " left an unexpected remainder: " + stack);
            }
        }
        helper.assertTrue(bottles == (expectedRemainder == null ? 0 : 1),
                key + " returned " + bottles + " remainder items, expected "
                        + (expectedRemainder == null ? 0 : 1));
    }

    // ---------------------------------------------------------------- R2-02 BBQ transaction

    /**
     * R2-02: the hold is a fixed 80 ticks and cancelling it costs and grants nothing.
     *
     * <p>Vanilla owns the countdown, so what is checked here is that the item declares the agreed
     * duration and that stopping short leaves the world exactly as it was -- no cooked meat, the
     * spit still in hand, no cooldown started. That is the whole of "cancellation changes nothing":
     * there is no cancellation code to test, only the absence of an effect.
     */
    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void r2BbqCancelledHoldChangesNothing(GameTestHelper helper) {
        net.minecraft.world.entity.player.Player cook = preparer(helper, 8, 2, 8);
        net.minecraft.world.item.ItemStack spit =
                new net.minecraft.world.item.ItemStack(com.carro1001.mhnw.registry.ModItems.BBQ_SPIT.get());
        cook.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, spit);

        helper.assertTrue(spit.getUseDuration(cook) == 80,
                "the spit's use duration is " + spit.getUseDuration(cook) + " ticks, expected 80");
        helper.assertTrue(spit.getMaxStackSize() == 1,
                "the spit must stack to one for the completed use to be transactional");

        cook.startUsingItem(net.minecraft.world.InteractionHand.MAIN_HAND);
        helper.assertTrue(cook.getUseItemRemainingTicks() == 80,
                "starting the hold did not arm the full 80 ticks");
        cook.stopUsingItem();

        helper.assertTrue(cook.getMainHandItem().is(com.carro1001.mhnw.registry.ModItems.BBQ_SPIT.get())
                        && cook.getMainHandItem().getCount() == 1,
                "a cancelled hold did not leave exactly one spit in hand");
        helper.assertTrue(countInInventory(cook, com.carro1001.mhnw.registry.ModItems.COOKED_MEAT.get()) == 0,
                "a cancelled hold produced cooked meat anyway");
        helper.assertTrue(!cook.getCooldowns().isOnCooldown(com.carro1001.mhnw.registry.ModItems.BBQ_SPIT.get()),
                "a cancelled hold started the cooldown");
        helper.succeed();
    }

    /**
     * R2-02: one completed hold consumes one survival spit, yields exactly one cooked meat and
     * starts the cooldown. Driven through {@code ItemStack.finishUsingItem}, the same seam
     * {@code LivingEntity.completeUsingItem} calls -- a held client input is not a meaningful thing
     * to simulate on a server, but the conversion it triggers is, and that is what is checked.
     */
    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void r2BbqCompletedHoldYieldsExactlyOneCookedMeat(GameTestHelper helper) {
        net.minecraft.world.entity.player.Player cook = preparer(helper, 8, 2, 8);
        net.minecraft.world.item.ItemStack spit =
                new net.minecraft.world.item.ItemStack(com.carro1001.mhnw.registry.ModItems.BBQ_SPIT.get());
        cook.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, spit);

        net.minecraft.world.item.ItemStack result = spit.finishUsingItem(helper.getLevel(), cook);

        helper.assertTrue(result.is(com.carro1001.mhnw.registry.ModItems.COOKED_MEAT.get())
                        && result.getCount() == 1,
                "a completed hold produced " + result + " instead of exactly one cooked meat");
        helper.assertTrue(spit.isEmpty(),
                "the survival spit was not consumed: " + spit.getCount() + " left");
        helper.assertTrue(cook.getCooldowns().isOnCooldown(com.carro1001.mhnw.registry.ModItems.BBQ_SPIT.get()),
                "a completed hold did not start the spit cooldown");
        helper.succeed();
    }

    /**
     * R2-02: the conversion cannot run twice off one spit. The second attempt has no spit left to
     * consume, so it must not mint a second cooked meat out of nothing.
     */
    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void r2BbqCannotDoubleComplete(GameTestHelper helper) {
        net.minecraft.world.entity.player.Player cook = preparer(helper, 8, 2, 8);
        net.minecraft.world.item.ItemStack spit =
                new net.minecraft.world.item.ItemStack(com.carro1001.mhnw.registry.ModItems.BBQ_SPIT.get());
        cook.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, spit);

        net.minecraft.world.item.ItemStack first = spit.finishUsingItem(helper.getLevel(), cook);
        cook.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, first);
        net.minecraft.world.item.ItemStack second = spit.finishUsingItem(helper.getLevel(), cook);

        helper.assertTrue(second.isEmpty(),
                "a spent spit completed a second time and produced " + second);
        int cooked = countInInventory(cook, com.carro1001.mhnw.registry.ModItems.COOKED_MEAT.get());
        helper.assertTrue(cooked == 1,
                "one spit ended up producing " + cooked + " cooked meats");
        helper.succeed();
    }

    // ---------------------------------------------------------------- R2-03 Flashbug capture

    /**
     * R2-03: a glass bottle takes one live Flashbug out of the world without firing its flash. The
     * bystander check is the point -- capture must not be a disguised way of triggering the thing
     * you were trying to catch.
     */
    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void r2BottleCapturesAFlashbugWithoutFlashing(GameTestHelper helper) {
        com.carro1001.mhnw.entity.Flashbug flashbug = helper.spawn(ModEntities.FLASHBUG.get(), 8, 2, 8);
        Cow bystander = helper.spawn(EntityType.COW, 8, 2, 9);
        bystander.setNoAi(true);
        bystander.setYRot(180.0F);
        net.minecraft.world.entity.player.Player catcher = preparer(helper, 8, 2, 7);
        catcher.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE));

        net.minecraft.world.InteractionResult result =
                flashbug.interact(catcher, net.minecraft.world.InteractionHand.MAIN_HAND);

        helper.assertTrue(result.consumesAction(), "the bottle interaction was not accepted");
        helper.assertTrue(flashbug.isRemoved(), "the captured flashbug was not removed");
        helper.assertTrue(!flashbug.isFlashing(), "capturing the flashbug started its telegraph");
        helper.assertTrue(!bystander.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS),
                "capturing the flashbug flashed a bystander");
        helper.assertTrue(catcher.getMainHandItem()
                        .is(com.carro1001.mhnw.registry.ModItems.BOTTLED_FLASHBUG.get())
                        && catcher.getMainHandItem().getCount() == 1,
                "capture left " + catcher.getMainHandItem() + " in hand, expected one bottled flashbug");
        helper.succeed();
    }

    /**
     * R2-03: a partial stack of bottles neither loses the capture nor duplicates anything --
     * exactly one bottle is spent and exactly one bottled bug arrives, through vanilla's own
     * filled-container rule rather than slot arithmetic of ours.
     */
    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void r2BottleCaptureFromAStackSpendsExactlyOneBottle(GameTestHelper helper) {
        com.carro1001.mhnw.entity.Flashbug flashbug = helper.spawn(ModEntities.FLASHBUG.get(), 8, 2, 8);
        net.minecraft.world.entity.player.Player catcher = preparer(helper, 8, 2, 7);
        catcher.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE, 3));

        flashbug.interact(catcher, net.minecraft.world.InteractionHand.MAIN_HAND);

        int bottlesLeft = countInInventory(catcher, net.minecraft.world.item.Items.GLASS_BOTTLE);
        helper.assertTrue(bottlesLeft == 2,
                "capturing from a stack of three bottles left " + bottlesLeft + ", expected two");
        helper.assertTrue(countInInventory(catcher,
                        com.carro1001.mhnw.registry.ModItems.BOTTLED_FLASHBUG.get()) == 1,
                "capturing from a stack did not add exactly one bottled flashbug");
        helper.succeed();
    }

    /**
     * R2-03: in creative the bottle is kept, under vanilla's own infinite-materials convention --
     * but exactly one bottled flashbug still arrives and the bug is still removed. The fixture is
     * deliberately left in its default creative state here; that is the whole point of the test.
     */
    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void r2CreativeBottleCaptureNeitherLosesNorDuplicates(GameTestHelper helper) {
        com.carro1001.mhnw.entity.Flashbug flashbug = helper.spawn(ModEntities.FLASHBUG.get(), 8, 2, 8);
        net.minecraft.server.level.ServerPlayer catcher = helper.makeMockServerPlayerInLevel();
        try {
            helper.assertTrue(catcher.hasInfiniteMaterials(),
                    "this fixture is meant to be creative; it is not, so the test proves nothing");
            catcher.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                    new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE));

            flashbug.interact(catcher, net.minecraft.world.InteractionHand.MAIN_HAND);

            helper.assertTrue(flashbug.isRemoved(), "a creative capture did not remove the flashbug");
            helper.assertTrue(countInInventory(catcher,
                            com.carro1001.mhnw.registry.ModItems.BOTTLED_FLASHBUG.get()) == 1,
                    "a creative capture produced " + countInInventory(catcher,
                            com.carro1001.mhnw.registry.ModItems.BOTTLED_FLASHBUG.get())
                            + " bottled flashbugs, expected exactly one");
            helper.assertTrue(countInInventory(catcher, net.minecraft.world.item.Items.GLASS_BOTTLE) == 1,
                    "a creative capture changed the bottle count");
        } finally {
            retire(catcher);
        }
        helper.succeed();
    }

    /** R2-03: an unrelated held item still does nothing; only the bottle interaction captures. */
    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void r2NonBottleInteractionDoesNotCaptureAFlashbug(GameTestHelper helper) {
        com.carro1001.mhnw.entity.Flashbug flashbug = helper.spawn(ModEntities.FLASHBUG.get(), 8, 2, 8);
        net.minecraft.world.entity.player.Player catcher = preparer(helper, 8, 2, 7);
        catcher.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STICK));

        flashbug.interact(catcher, net.minecraft.world.InteractionHand.MAIN_HAND);

        helper.assertTrue(!flashbug.isRemoved(), "a stick captured a flashbug");
        helper.assertTrue(countInInventory(catcher,
                        com.carro1001.mhnw.registry.ModItems.BOTTLED_FLASHBUG.get()) == 0,
                "a stick produced a bottled flashbug");
        helper.succeed();
    }

    // ---------------------------------------------------------------- R2-05 flash impact

    /**
     * R2-05/R2-06: the one eligibility matrix both the wild flashbug and the thrown bomb obey.
     * Facing, cover, range and the player exclusion are all checked against a single flash, so a
     * change to {@code FlashEffect} that broke any one of them fails here regardless of which
     * caller triggered it.
     */
    @GameTest(template = ARENA, timeoutTicks = 60)
    public static void r2FlashAffectsOnlyEligibleTargets(GameTestHelper helper) {
        fillFloor(helper, 1, net.minecraft.world.level.block.Blocks.STONE);
        net.minecraft.world.phys.Vec3 origin =
                helper.absoluteVec(new net.minecraft.world.phys.Vec3(8.0D, 2.5D, 8.0D));

        // Facing the flash from one block north: must be blinded and slowed.
        Cow facing = helper.spawn(EntityType.COW, 8, 2, 10);
        facing.setNoAi(true);
        facing.setYRot(180.0F);
        // Same spot, looking the other way: must be untouched.
        Cow lookingAway = helper.spawn(EntityType.COW, 8, 2, 6);
        lookingAway.setNoAi(true);
        lookingAway.setYRot(180.0F);
        // Well outside the five-block radius, facing it.
        Cow outOfRange = helper.spawn(EntityType.COW, 14, 2, 8);
        outOfRange.setNoAi(true);
        outOfRange.setYRot(90.0F);
        // Facing it, but behind a solid wall raised between the two.
        Cow covered = helper.spawn(EntityType.COW, 8, 2, 5);
        covered.setNoAi(true);
        covered.setYRot(180.0F);
        for (int y = 2; y <= 4; y++) {
            for (int x = 6; x <= 10; x++) {
                helper.setBlock(x, y, 6, net.minecraft.world.level.block.Blocks.STONE);
            }
        }

        net.minecraft.server.level.ServerPlayer bystander = helper.makeMockServerPlayerInLevel();
        try {
            bystander.moveTo(origin.x, origin.y, origin.z + 1.0D, 180.0F, 0.0F);
            float[] healthBefore = {facing.getHealth()};

            com.carro1001.mhnw.entity.FlashEffect.flash(helper.getLevel(), null, origin);

            helper.assertTrue(facing.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS),
                    "a facing target in range was not blinded");
            net.minecraft.world.effect.MobEffectInstance slow =
                    facing.getEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN);
            helper.assertTrue(slow != null && slow.getAmplifier() == 1,
                    "a facing target did not get Movement Slowdown II: " + slow);
            helper.assertTrue(slow.getDuration() <= 40 && slow.getDuration() > 35,
                    "the slowdown ran for " + slow.getDuration() + " ticks, expected 40");
            helper.assertTrue(facing.getHealth() == healthBefore[0],
                    "the flash damaged a target");

            helper.assertTrue(!lookingAway.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS),
                    "a target looking away was flashed anyway");
            helper.assertTrue(!outOfRange.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS),
                    "a target outside the radius was flashed anyway");
            helper.assertTrue(!covered.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS),
                    "a target behind solid cover was flashed anyway");
            helper.assertTrue(!bystander.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS)
                            && !bystander.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN),
                    "a player was affected by the flash");
        } finally {
            bystander.getServer().getPlayerList().remove(bystander);
        }
        helper.succeed();
    }

    /**
     * R2-05: the radius is a radius, not the broad-phase box.
     *
     * <p>PR #7 review, finding 1. {@code FlashEffect} queries an AABB inflated by {@code RADIUS},
     * which is a 10-cube whose corners reach ~8.7 blocks; the eligibility test has to be a real
     * distance on top of that. This cow sits at a diagonal 6.38 blocks from the flash while still
     * being comfortably inside the cube ({@code |dx| = |dz| = 4.5}), and it faces the flash with
     * clear line of sight -- so the only thing that can exclude it is the distance guard. The
     * existing out-of-range cow in {@code r2FlashAffectsOnlyEligibleTargets} cannot catch this: at
     * {@code |dx| = 6.5} it is outside the cube and so never reaches the guard at all.
     */
    @GameTest(template = ARENA, timeoutTicks = 60)
    public static void r2FlashRadiusIsARadiusNotABoundingBox(GameTestHelper helper) {
        fillFloor(helper, 1, net.minecraft.world.level.block.Blocks.STONE);
        net.minecraft.world.phys.Vec3 origin =
                helper.absoluteVec(new net.minecraft.world.phys.Vec3(8.0D, 2.5D, 8.0D));

        Cow diagonal = helper.spawn(EntityType.COW, 12, 2, 12);
        diagonal.setNoAi(true);
        // Yaw 135 looks toward -X/-Z, i.e. back at the flash point, so the facing rule passes.
        diagonal.setYRot(135.0F);
        diagonal.setXRot(0.0F);

        double distance = Math.sqrt(diagonal.distanceToSqr(origin));
        helper.assertTrue(distance > com.carro1001.mhnw.entity.FlashEffect.RADIUS,
                "fixture error: the diagonal cow is " + distance + " blocks away, inside the radius");
        helper.assertTrue(new net.minecraft.world.phys.AABB(origin, origin)
                        .inflate(com.carro1001.mhnw.entity.FlashEffect.RADIUS)
                        .intersects(diagonal.getBoundingBox()),
                "fixture error: the diagonal cow is outside the broad-phase box, so this test would"
                        + " pass without the distance guard it exists to check");

        com.carro1001.mhnw.entity.FlashEffect.flash(helper.getLevel(), null, origin);

        helper.assertTrue(!diagonal.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS)
                        && !diagonal.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN),
                "a target " + distance + " blocks away was flashed; the box is the query, the"
                        + " radius is the contract");
        helper.succeed();
    }

    /**
     * The fuse: a bomb that hits nothing still goes off, once, on time. Fired horizontally over a
     * hole with no floor under it, so nothing but the fuse can end it -- if the fuse were missing
     * this test would time out rather than pass for the wrong reason.
     */
    @GameTest(template = ARENA, timeoutTicks = 120)
    public static void r2FlashBombFuseReleasesWithoutAnImpact(GameTestHelper helper) {
        Cow victim = helper.spawn(EntityType.COW, 8, 3, 9);
        victim.setNoAi(true);
        victim.setYRot(180.0F);

        com.carro1001.mhnw.entity.FlashBombProjectile bomb =
                new com.carro1001.mhnw.entity.FlashBombProjectile(ModEntities.FLASH_BOMB.get(), helper.getLevel());
        bomb.moveTo(helper.absoluteVec(new net.minecraft.world.phys.Vec3(8.0D, 3.0D, 8.0D)));
        bomb.setNoGravity(true);
        helper.getLevel().addFreshEntity(bomb);

        helper.startSequence()
                .thenIdle(com.carro1001.mhnw.entity.FlashBombProjectile.FUSE_TICKS - 2)
                .thenExecute(() -> helper.assertTrue(!bomb.isRemoved(),
                        "the bomb went off before its fuse ran out"))
                .thenIdle(4)
                .thenExecute(() -> {
                    helper.assertTrue(bomb.isRemoved(), "the fuse never went off");
                    helper.assertTrue(victim.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS),
                            "the fuse discarded the bomb without flashing anything");
                })
                .thenSucceed();
    }

    /**
     * R2-05: a genuinely thrown bomb -- launched, flying, impacting a block on its own -- releases
     * once, flashes, breaks nothing and is gone. Nothing here calls the impact handler directly;
     * the projectile is added to the level and left to hit the floor by itself.
     */
    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void r2ThrownFlashBombReleasesOnceOnImpact(GameTestHelper helper) {
        fillFloor(helper, 1, net.minecraft.world.level.block.Blocks.STONE);
        Cow victim = helper.spawn(EntityType.COW, 8, 2, 9);
        victim.setNoAi(true);
        victim.setYRot(180.0F);
        float healthBefore = victim.getHealth();

        com.carro1001.mhnw.entity.FlashBombProjectile bomb =
                new com.carro1001.mhnw.entity.FlashBombProjectile(ModEntities.FLASH_BOMB.get(), helper.getLevel());
        bomb.moveTo(helper.absoluteVec(new net.minecraft.world.phys.Vec3(8.0D, 4.0D, 8.0D)));
        bomb.setDeltaMovement(0.0D, -0.6D, 0.0D);
        helper.getLevel().addFreshEntity(bomb);

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(bomb.isRemoved(),
                        "waiting for the thrown bomb to impact and discard itself"))
                .thenExecute(() -> {
                    helper.assertTrue(victim.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS),
                            "a facing target next to the impact was not flashed");
                    helper.assertTrue(victim.getHealth() == healthBefore,
                            "the thrown bomb damaged a target");
                    helper.assertBlockPresent(net.minecraft.world.level.block.Blocks.STONE, 8, 1, 8);
                })
                .thenSucceed();
    }

    /**
     * R2-05: the whole real throw, from a player's hand.
     *
     * <p>Added after a live crash report. The other impact test constructs the projectile through
     * its {@code EntityType} constructor and drops it, which never touches {@link FlashBombItem} or
     * the {@code (Level, LivingEntity)} shooter constructor -- so the item-to-projectile handoff a
     * player actually performs had no coverage at all. This drives it end to end: use the item,
     * consume one from the stack, start the cooldown, let the thing fly and hit the floor by
     * itself, and check it flashed and is gone.
     *
     * <p>(The crash itself was not this path: it was a {@code NoClassDefFoundError} from a
     * concurrent {@code clean build} deleting build/classes under a running dev client, which no
     * test can or should defend against. The coverage gap it exposed is real regardless.)
     */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void r2FlashBombThrownFromTheHandFliesAndFlashes(GameTestHelper helper) {
        fillFloor(helper, 1, net.minecraft.world.level.block.Blocks.STONE);
        net.minecraft.server.level.ServerPlayer thrower = realPreparer(helper, 8, 4, 8);
        try {
            Cow victim = helper.spawn(EntityType.COW, 8, 2, 9);
            victim.setNoAi(true);
            victim.setYRot(180.0F);
            // Straight down, so it reaches the floor without leaving the arena.
            thrower.setXRot(90.0F);
            net.minecraft.world.item.ItemStack bombs =
                    new net.minecraft.world.item.ItemStack(com.carro1001.mhnw.registry.ModItems.FLASH_BOMB.get(), 2);
            thrower.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, bombs);

            bombs.use(helper.getLevel(), thrower, net.minecraft.world.InteractionHand.MAIN_HAND);

            helper.assertTrue(bombs.getCount() == 1,
                    "throwing consumed " + (2 - bombs.getCount()) + " bombs, expected exactly one");
            helper.assertTrue(thrower.getCooldowns()
                            .isOnCooldown(com.carro1001.mhnw.registry.ModItems.FLASH_BOMB.get()),
                    "throwing did not start the flash bomb cooldown");

            java.util.List<com.carro1001.mhnw.entity.FlashBombProjectile> inFlight =
                    helper.getLevel().getEntitiesOfClass(com.carro1001.mhnw.entity.FlashBombProjectile.class,
                            thrower.getBoundingBox().inflate(12.0D));
            helper.assertTrue(inFlight.size() == 1,
                    "using the item put " + inFlight.size() + " projectiles in the world, expected one");
            com.carro1001.mhnw.entity.FlashBombProjectile bomb = inFlight.get(0);

            helper.startSequence()
                    .thenWaitUntil(() -> helper.assertTrue(bomb.isRemoved(),
                            "waiting for the thrown bomb to impact and discard itself"))
                    .thenExecute(() -> {
                        boolean blinded = victim.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS);
                        retire(thrower);
                        helper.assertTrue(blinded,
                                "a bomb thrown from the hand did not flash a facing target beside its impact");
                    })
                    .thenSucceed();
        } catch (RuntimeException | AssertionError failure) {
            retire(thrower);
            throw failure;
        }
    }

    // ---------------------------------------------------------------- R2-06 wild regression

    /**
     * R2-06: the wild flashbug's own contract is unchanged by the extraction -- still hit-only,
     * still discarded after one release -- and it now carries the same bounded slowdown the crafted
     * bomb does. If the shared helper ever stops being shared, the slowdown assertion fails here
     * while every pre-existing flashbug test still passes, which is exactly the regression this
     * guards.
     */
    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void r2WildFlashbugSharesTheBoundedFlash(GameTestHelper helper) {
        com.carro1001.mhnw.entity.Flashbug flashbug = helper.spawn(ModEntities.FLASHBUG.get(), 8, 2, 8);
        Cow victim = helper.spawn(EntityType.COW, 8, 2, 9);
        victim.setNoAi(true);
        victim.setYRot(180.0F);

        flashbug.hurt(helper.getLevel().damageSources().generic(), 1.0F);

        helper.succeedWhen(() -> {
            helper.assertTrue(victim.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS),
                    "the wild flashbug no longer blinds a facing victim");
            net.minecraft.world.effect.MobEffectInstance slow =
                    victim.getEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN);
            helper.assertTrue(slow != null && slow.getAmplifier() == 1,
                    "the wild flashbug did not apply the shared Movement Slowdown II: " + slow);
            helper.assertTrue(flashbug.isRemoved(),
                    "the wild flashbug was not discarded after its one release");
        });
    }

    // ---------------------------------------------------------------- R2-07 toad capture

    /**
     * R2-07: every variant plus one water bucket yields its exact filled id, keeps the custom name
     * and health, removes exactly that toad, and does not light a fuse.
     */
    @GameTest(template = ARENA, timeoutTicks = 60)
    public static void r2WaterBucketCapturesEveryToadVariant(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer catcher = realPreparer(helper, 8, 2, 7);
        try {
        for (Toad.Variant variant : Toad.Variant.values()) {
            Toad toad = helper.spawn(ModEntities.TOAD.get(), 8, 2, 8);
            toad.setVariant(variant);
            toad.setCustomName(net.minecraft.network.chat.Component.literal("Hopper"));
            toad.setHealth(2.5F);
            catcher.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                    new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WATER_BUCKET));

            toad.interact(catcher, net.minecraft.world.InteractionHand.MAIN_HAND);

            net.minecraft.world.item.ItemStack filled = catcher.getMainHandItem();
            helper.assertTrue(filled.is(com.carro1001.mhnw.registry.ModItems.toadBucket(variant).get())
                            && filled.getCount() == 1,
                    variant + " produced " + filled + " instead of its own filled bucket");
            helper.assertTrue(toad.isRemoved(), variant + " toad was not removed by the capture");
            helper.assertTrue(!toad.isFusing(), "capturing a " + variant + " toad lit its fuse");

            net.minecraft.nbt.CompoundTag bucketData = filled
                    .getOrDefault(net.minecraft.core.component.DataComponents.BUCKET_ENTITY_DATA,
                            net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
            helper.assertTrue(bucketData.getByte("Variant") == (byte) variant.ordinal(),
                    variant + " was not stored in its own bucket data");
            helper.assertTrue(Math.abs(bucketData.getFloat("Health") - 2.5F) < EPSILON,
                    variant + " lost its health through the bucket");
            helper.assertTrue(filled.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME) != null,
                    variant + " lost its custom name through the bucket");
        }
        } finally {
            retire(catcher);
        }
        helper.succeed();
    }

    /** R2-07: an empty hand or the wrong bucket still does nothing at all to a live toad. */
    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void r2EmptyBucketDoesNotCaptureAToad(GameTestHelper helper) {
        Toad toad = helper.spawn(ModEntities.TOAD.get(), 8, 2, 8);
        toad.setVariant(Toad.Variant.BLAST);
        net.minecraft.world.entity.player.Player catcher = preparer(helper, 8, 2, 7);
        catcher.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BUCKET));

        toad.interact(catcher, net.minecraft.world.InteractionHand.MAIN_HAND);

        helper.assertTrue(!toad.isRemoved(), "an empty bucket captured a toad");
        helper.assertTrue(catcher.getMainHandItem().is(net.minecraft.world.item.Items.BUCKET),
                "an empty bucket turned into something else");
        helper.succeed();
    }

    // ---------------------------------------------------------------- R2-08 release and round trip

    /**
     * Releases a filled bucket the way a player actually does: hold it, look straight down at the
     * floor, and use it. This runs the whole of {@code BucketItem.use} -- the water placement, the
     * spawn seam and the empty-bucket return -- rather than only the seam this mod overrides.
     */
    private static Toad releaseFrom(GameTestHelper helper, net.minecraft.world.item.ItemStack filled,
                                    net.minecraft.world.entity.player.Player player) {
        player.setXRot(90.0F);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, filled);
        java.util.List<Toad> before = helper.getLevel().getEntitiesOfClass(
                Toad.class, player.getBoundingBox().inflate(8.0D));
        // ServerPlayerGameMode.useItem puts the returned stack back in the hand; use() itself does
        // not, so the test has to do the same thing the game mode would.
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                filled.use(helper.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND)
                        .getObject());
        for (Toad candidate : helper.getLevel().getEntitiesOfClass(
                Toad.class, player.getBoundingBox().inflate(8.0D))) {
            if (!before.contains(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * R2-08: each filled id releases exactly one live toad of its own variant, marked FromBucket,
     * and hands back exactly one empty bucket. The stacks here carry no bucket data at all -- they
     * are what {@code /give} produces -- which is the case vanilla's own {@code MobBucketItem}
     * would get wrong, and the reason {@code ToadBucketItem} exists.
     */
    @GameTest(template = ARENA, timeoutTicks = 80)
    public static void r2BareFilledBucketReleasesItsOwnVariant(GameTestHelper helper) {
        fillFloor(helper, 1, net.minecraft.world.level.block.Blocks.STONE);
        for (Toad.Variant variant : Toad.Variant.values()) {
            net.minecraft.world.entity.player.Player keeper = preparer(helper, 8, 3, 8);
            Toad released = releaseFrom(helper, new net.minecraft.world.item.ItemStack(
                    com.carro1001.mhnw.registry.ModItems.toadBucket(variant).get()), keeper);

            helper.assertTrue(released != null, "a bare " + variant + " bucket released no toad");
            helper.assertTrue(released.getVariant() == variant,
                    "a bare " + variant + " bucket released a " + released.getVariant() + " toad");
            helper.assertTrue(released.fromBucket(), variant + " was not marked FromBucket");
            helper.assertTrue(released.requiresCustomPersistence(),
                    variant + " released from a bucket can still distance-despawn");
            helper.assertTrue(!released.isFusing(), variant + " lit its fuse on release");
            helper.assertTrue(keeper.getMainHandItem().is(net.minecraft.world.item.Items.BUCKET)
                            && keeper.getMainHandItem().getCount() == 1,
                    "releasing " + variant + " returned " + keeper.getMainHandItem()
                            + " instead of exactly one empty bucket");
            released.discard();
        }
        helper.succeed();
    }

    /**
     * R2-08: catch, release, save/load and catch again -- the variant, the name, the health and
     * FromBucket all survive, and nothing multiplies along the way.
     */
    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void r2ToadSurvivesACaptureReleaseRoundTrip(GameTestHelper helper) {
        fillFloor(helper, 1, net.minecraft.world.level.block.Blocks.STONE);
        net.minecraft.server.level.ServerPlayer keeper = realPreparer(helper, 8, 3, 7);
        try {
        Toad original = helper.spawn(ModEntities.TOAD.get(), 8, 3, 8);
        original.setVariant(Toad.Variant.PARALYSIS);
        original.setCustomName(net.minecraft.network.chat.Component.literal("Hopper"));
        original.setHealth(3.0F);

        keeper.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WATER_BUCKET));
        original.interact(keeper, net.minecraft.world.InteractionHand.MAIN_HAND);
        net.minecraft.world.item.ItemStack filled = keeper.getMainHandItem().copy();
        helper.assertTrue(filled.is(com.carro1001.mhnw.registry.ModItems.PARATOAD_BUCKET.get()),
                "capture produced " + filled + " instead of a paratoad bucket");

        Toad released = releaseFrom(helper, filled, keeper);
        helper.assertTrue(released != null, "the round trip released no toad");
        helper.assertTrue(released.getVariant() == Toad.Variant.PARALYSIS,
                "the round trip changed the variant to " + released.getVariant());
        helper.assertTrue(released.hasCustomName(), "the round trip lost the custom name");
        helper.assertTrue(Math.abs(released.getHealth() - 3.0F) < EPSILON,
                "the round trip changed health to " + released.getHealth());

        // A real reload: save and restore, the same round trip a chunk unload performs.
        CompoundTag saved = released.saveWithoutId(new CompoundTag());
        Toad reloaded = new Toad(ModEntities.TOAD.get(), helper.getLevel());
        reloaded.load(saved);
        helper.assertTrue(reloaded.getVariant() == Toad.Variant.PARALYSIS,
                "a reloaded bucket toad had variant " + reloaded.getVariant());
        helper.assertTrue(reloaded.fromBucket(), "a reloaded bucket toad lost FromBucket");

        // And catching it again gives back the same bucket, not a different variant's.
        keeper.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WATER_BUCKET));
        released.interact(keeper, net.minecraft.world.InteractionHand.MAIN_HAND);
        helper.assertTrue(keeper.getMainHandItem()
                        .is(com.carro1001.mhnw.registry.ModItems.PARATOAD_BUCKET.get()),
                "recapturing gave " + keeper.getMainHandItem() + " instead of the same bucket");
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(
                        Toad.class, keeper.getBoundingBox().inflate(10.0D)).isEmpty(),
                "the capture/release round trip left extra toads behind");
        } finally {
            retire(keeper);
        }
        helper.succeed();
    }

    // ---------------------------------------------------------------- R2-09 deployed effects

    /**
     * R2-09: a deployed toad is still a toad -- it sits there until something hits it, then runs
     * the existing 40-tick fuse and releases exactly once. Release is not automatic and the bucket
     * did not make it so.
     */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void r2ReleasedToadStillNeedsAHitAndReleasesOnce(GameTestHelper helper) {
        fillFloor(helper, 1, net.minecraft.world.level.block.Blocks.STONE);
        net.minecraft.world.entity.player.Player keeper = preparer(helper, 8, 3, 7);
        Toad released = releaseFrom(helper, new net.minecraft.world.item.ItemStack(
                com.carro1001.mhnw.registry.ModItems.POISONTOAD_BUCKET.get()), keeper);
        helper.assertTrue(released != null, "the fixture released no toad");
        Cow victim = helper.spawn(EntityType.COW, 8, 3, 9);
        victim.setNoAi(true);

        helper.startSequence()
                .thenExecuteFor(60, () -> helper.assertTrue(!released.isFusing(),
                        "a released toad started fusing without being hit"))
                .thenExecute(() -> released.hurt(helper.getLevel().damageSources().generic(), 1.0F))
                .thenWaitUntil(() -> helper.assertTrue(released.isRemoved(),
                        "waiting for the provoked toad to release and discard itself"))
                .thenExecute(() -> helper.assertTrue(
                        victim.hasEffect(net.minecraft.world.effect.MobEffects.POISON),
                        "a released poison toad's cloud never reached a victim next to it"))
                .thenSucceed();
    }

    // ---------------------------------------------------------------- R2-10 Blastoad attribution

    /**
     * R2-10: a player who provokes a Blastoad owns the damage it deals. The blast has to actually
     * take health off the monster for anything to be credited -- that is the same rule a sword
     * swing goes through -- and an unrelated player standing right beside it gets nothing.
     */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void r2ProvokedBlastoadCreditsItsProvoker(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer hunter = helper.makeMockServerPlayerInLevel();
        GreatIzuchi quarry = spawnInert(helper);
        Toad blastoad = helper.spawn(ModEntities.TOAD.get(), 8, 2, 8);
        blastoad.setVariant(Toad.Variant.BLAST);
        hunter.moveTo(blastoad.getX(), blastoad.getY(), blastoad.getZ() - 1.0D, 0.0F, 0.0F);
        float healthBefore = quarry.getHealth();

        blastoad.hurt(helper.getLevel().damageSources().playerAttack(hunter), 1.0F);
        boolean recorded = blastoad.provokerId() != null && blastoad.provokerId().equals(hunter.getUUID());

        helper.startSequence()
                .thenExecute(() -> helper.assertTrue(recorded,
                        "the hit did not record the provoking player"))
                .thenWaitUntil(() -> helper.assertTrue(blastoad.isRemoved(),
                        "waiting for the provoked blastoad to detonate"))
                .thenExecute(() -> {
                    // Read everything, then take the fixture player back out of the level before
                    // asserting: a failed assertion aborts the sequence, and a mock player left in
                    // the player list would follow the rest of the run around.
                    boolean credited = quarry.carveState().isParticipant(hunter.getUUID());
                    int participants = quarry.carveState().participantCount();
                    float health = quarry.getHealth();
                    hunter.getServer().getPlayerList().remove(hunter);

                    helper.assertTrue(health < healthBefore,
                            "the blast did no damage to the monster, so this proves nothing");
                    helper.assertTrue(credited,
                            "the provoking player was not credited for the blast damage");
                    helper.assertTrue(participants == 1,
                            "the blast credited " + participants
                                    + " participants instead of only its provoker");
                })
                .thenSucceed();
    }

    /**
     * R2-10: an unprovoked blast, and a non-damaging variant, credit nobody. Releasing a bucket
     * next to a monster or standing near one going off is not participation.
     */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void r2UnprovokedAndStatusToadsCreditNobody(GameTestHelper helper) {
        fillFloor(helper, 1, net.minecraft.world.level.block.Blocks.STONE);
        net.minecraft.server.level.ServerPlayer bystander = helper.makeMockServerPlayerInLevel();
        GreatIzuchi quarry = spawnInert(helper);
        // Deployed from this player's own bucket, then set off by something that is not them.
        Toad blastoad = helper.spawn(ModEntities.TOAD.get(), 8, 2, 8);
        blastoad.setVariant(Toad.Variant.BLAST);
        blastoad.setFromBucket(true);
        bystander.moveTo(blastoad.getX(), blastoad.getY(), blastoad.getZ() - 1.0D, 0.0F, 0.0F);

        blastoad.hurt(helper.getLevel().damageSources().generic(), 1.0F);

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(blastoad.isRemoved(),
                        "waiting for the unprovoked blastoad to detonate"))
                .thenExecute(() -> {
                    int participants = quarry.carveState().participantCount();
                    bystander.getServer().getPlayerList().remove(bystander);
                    helper.assertTrue(participants == 0,
                            "an unprovoked blast credited " + participants + " participants");
                })
                .thenSucceed();
    }

    /**
     * R2-10: a player who hits an already-burning toad does not inherit somebody else's fuse.
     *
     * <p>PR #7 review, finding 2. The fuse is lit by an unattributed generic hit, so the correct
     * record is "nobody"; a player then hits the same toad while it is still burning. Attribution
     * belongs to the hit that started the fuse, so that player must not be credited when it goes
     * off. The earlier code tested {@code provokerId == null} to mean "nothing recorded yet", which
     * conflated it with a valid record of an environmental trigger and let exactly this happen.
     */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void r2LaterPlayerHitDoesNotStealARunningFuse(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer latecomer = helper.makeMockServerPlayerInLevel();
        GreatIzuchi quarry = spawnInert(helper);
        Toad blastoad = helper.spawn(ModEntities.TOAD.get(), 8, 2, 8);
        blastoad.setVariant(Toad.Variant.BLAST);
        latecomer.moveTo(blastoad.getX(), blastoad.getY(), blastoad.getZ() - 1.0D, 0.0F, 0.0F);

        // Something that is nobody lights the fuse.
        blastoad.hurt(helper.getLevel().damageSources().generic(), 1.0F);

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(blastoad.isFusing(),
                        "waiting for the unattributed fuse to actually be burning"))
                .thenExecute(() -> {
                    helper.assertTrue(blastoad.provokerId() == null,
                            "fixture error: the generic hit recorded a provoker, so this test would"
                                    + " prove nothing");
                    // ... and only now does a player turn up and hit it.
                    blastoad.hurt(helper.getLevel().damageSources().playerAttack(latecomer), 1.0F);
                    helper.assertTrue(blastoad.provokerId() == null,
                            "a player who hit an already-burning toad was recorded as its provoker");
                })
                .thenWaitUntil(() -> helper.assertTrue(blastoad.isRemoved(),
                        "waiting for the blastoad to detonate"))
                .thenExecute(() -> {
                    boolean credited = quarry.carveState().isParticipant(latecomer.getUUID());
                    int participants = quarry.carveState().participantCount();
                    latecomer.getServer().getPlayerList().remove(latecomer);

                    helper.assertTrue(!credited,
                            "a player who hit an already-burning toad was credited for its blast");
                    helper.assertTrue(participants == 0,
                            "an unattributed fuse credited " + participants + " participants");
                })
                .thenSucceed();
    }

    /**
     * R2-10: the first hit is latched before the goal scheduler has even run.
     *
     * <p>PR #7 follow-up review. Guarding on {@code !isFusing()} alone is not enough: that flag is
     * set by {@code ToadFuseGoal.start()}, which the goal selector runs on its own every-other-tick
     * cadence, so a second hit landing in the gap between the first hit and that call still saw
     * {@code !isFusing()} and overwrote the record. This test lands both hits back to back, in the
     * same tick, before anything has ticked the toad -- the exact window the {@code !provoked} half
     * of the guard exists for.
     */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void r2SecondHitBeforeTheFuseStartsDoesNotStealAttribution(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer latecomer = helper.makeMockServerPlayerInLevel();
        GreatIzuchi quarry = spawnInert(helper);
        Toad blastoad = helper.spawn(ModEntities.TOAD.get(), 8, 2, 8);
        blastoad.setVariant(Toad.Variant.BLAST);
        latecomer.moveTo(blastoad.getX(), blastoad.getY(), blastoad.getZ() - 1.0D, 0.0F, 0.0F);

        // Both in the same tick: nothing has run the goal selector in between, so isFusing() is
        // still false for the second hit. Only the latch on provoked can reject it.
        blastoad.hurt(helper.getLevel().damageSources().generic(), 1.0F);
        helper.assertTrue(!blastoad.isFusing(),
                "fixture error: the fuse already started, so this test would not cover the gap it"
                        + " was written for");
        blastoad.hurt(helper.getLevel().damageSources().playerAttack(latecomer), 1.0F);

        helper.assertTrue(blastoad.provokerId() == null,
                "a second hit landing before the goal started stole attribution from the first");

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(blastoad.isRemoved(),
                        "waiting for the blastoad to detonate"))
                .thenExecute(() -> {
                    boolean credited = quarry.carveState().isParticipant(latecomer.getUUID());
                    int participants = quarry.carveState().participantCount();
                    latecomer.getServer().getPlayerList().remove(latecomer);

                    helper.assertTrue(!credited,
                            "a player whose hit landed after the fuse was already lit was credited");
                    helper.assertTrue(participants == 0,
                            "an unattributed fuse credited " + participants + " participants");
                })
                .thenSucceed();
    }

    /** R2-10: a provoked status toad damages nothing, so it credits nobody either. */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void r2ProvokedStatusToadCreditsNobody(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer hunter = helper.makeMockServerPlayerInLevel();
        GreatIzuchi quarry = spawnInert(helper);
        Toad poisontoad = helper.spawn(ModEntities.TOAD.get(), 8, 2, 8);
        poisontoad.setVariant(Toad.Variant.POISON);
        hunter.moveTo(poisontoad.getX(), poisontoad.getY(), poisontoad.getZ() - 1.0D, 0.0F, 0.0F);

        poisontoad.hurt(helper.getLevel().damageSources().playerAttack(hunter), 1.0F);

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(poisontoad.isRemoved(),
                        "waiting for the provoked poison toad to release"))
                .thenExecute(() -> {
                    int participants = quarry.carveState().participantCount();
                    hunter.getServer().getPlayerList().remove(hunter);
                    helper.assertTrue(participants == 0,
                            "a non-damaging status toad credited carve participation");
                })
                .thenSucceed();
    }

    // ---------------------------------------------------------------- R3 Giant Jawblade

    /** A level-resident survival player holding the weapon. Level-resident because the charge is a
     * real held use: only a player the server actually ticks can complete one. */
    private static net.minecraft.server.level.ServerPlayer wielder(GameTestHelper helper,
                                                                  double x, double y, double z) {
        net.minecraft.server.level.ServerPlayer player = realPreparer(helper, x, y, z);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                new net.minecraft.world.item.ItemStack(com.carro1001.mhnw.registry.ModItems.GIANT_JAWBLADE.get()));
        return player;
    }

    /**
     * Point the player's eyes at something. The previous-tick rotation is set too, deliberately:
     * {@code ProjectileUtil.getHitResultOnViewVector} asks for the view vector at partial tick
     * zero, which interpolates from {@code yRotO}/{@code xRotO} -- a fixture that sets only the
     * current rotation aims one tick into the past and misses for the wrong reason.
     */
    private static void aimAt(net.minecraft.server.level.ServerPlayer player,
                              net.minecraft.world.phys.Vec3 target) {
        net.minecraft.world.phys.Vec3 to = target.subtract(player.getEyePosition());
        float yaw = (float) (net.minecraft.util.Mth.atan2(to.z, to.x) * 180.0D / Math.PI) - 90.0F;
        float pitch = (float) (-(net.minecraft.util.Mth.atan2(to.y, to.horizontalDistance()) * 180.0D / Math.PI));
        player.moveTo(player.getX(), player.getY(), player.getZ(), yaw, pitch);
        player.setYHeadRot(yaw);
        player.yRotO = yaw;
        player.xRotO = pitch;
        player.yHeadRotO = yaw;
    }

    private static net.minecraft.world.entity.animal.Cow inertCow(GameTestHelper helper,
                                                                  double x, double y, double z) {
        net.minecraft.world.entity.animal.Cow cow = helper.spawn(EntityType.COW, 8, 2, 8);
        cow.setNoAi(true);
        net.minecraft.world.phys.Vec3 at = helper.absoluteVec(new net.minecraft.world.phys.Vec3(x, y, z));
        cow.moveTo(at.x, at.y, at.z, 0.0F, 0.0F);
        return cow;
    }

    /**
     * One real tick of a fixture player.
     *
     * <p>{@code ServerPlayer.tick()} is the <em>connection</em> tick -- menus, camera, the game
     * mode -- and never runs the entity tick at all; {@code doTick()} is the one that calls
     * {@code super.tick()} and therefore counts down a held use, expires an item cooldown and
     * applies equipment attribute modifiers. A mock player is not in the server's ticking player
     * list either way, so a test that only idles leaves it frozen: three of these tests failed on
     * exactly that before this existed, and they failed in a way that looked like broken gameplay
     * rather than a still fixture.
     */
    private static void tickOnce(net.minecraft.server.level.ServerPlayer player) {
        player.doTick();
    }

    /**
     * Tick the player until its attack is fully cooled, and say so if it is not.
     *
     * <p>This is not housekeeping. Vanilla only sweeps at <em>full</em> attack strength, so a
     * fixture that drives the charge through the item seam without ticking first quietly tests a
     * weak attack: the adjacent-bystander case below passed against a genuinely sweeping weapon
     * until this existed. A real charge is always fully cooled -- 30 held ticks against a 25-tick
     * delay -- so the cooled state is the honest one to test in.
     */
    private static void coolDown(GameTestHelper helper, net.minecraft.server.level.ServerPlayer player) {
        for (int tick = 0; tick < 40; tick++) {
            tickOnce(player);
        }
        helper.assertTrue(player.getAttackStrengthScale(0.0F) >= 1.0F,
                "fixture error: the attack is only " + player.getAttackStrengthScale(0.0F)
                        + " cooled, so vanilla's own sweep could not fire either way");
    }

    /**
     * Hold the charge for real ticks and then let go, which is how a player swings this weapon.
     *
     * <p>Drives vanilla's own countdown and vanilla's own {@code releaseUsingItem}, so the tier that
     * lands is the one the elapsed ticks earned rather than one a test asserted into place.
     */
    private static void chargeAndRelease(net.minecraft.server.level.ServerPlayer player, int ticks) {
        player.startUsingItem(net.minecraft.world.InteractionHand.MAIN_HAND);
        for (int tick = 0; tick < ticks; tick++) {
            tickOnce(player);
        }
        player.releaseUsingItem();
    }

    /** A full-tier charge released, with the attack cooled first so the swing lands at full
     * strength. Used where the target geometry is what is under test, not the timing. */
    private static void completeCharge(GameTestHelper helper, net.minecraft.server.level.ServerPlayer player) {
        coolDown(helper, player);
        chargeAndRelease(player, maxTierTicks());
    }

    private static int maxTierTicks() {
        int[] tiers = com.carro1001.mhnw.item.GiantJawbladeItem.TIER_TICKS;
        return tiers[tiers.length - 1];
    }

    /** Health lost by a fresh cow to one charge held this long, or 0.0 if nothing was struck. */
    private static float chargeDamageAt(GameTestHelper helper, net.minecraft.server.level.ServerPlayer hunter,
                                        int holdTicks, double x, double y, double z) {
        net.minecraft.world.entity.animal.Cow target = inertCow(helper, x, y, z);
        // A cow holds 10 health and the upper tiers hit harder than that, so an unmodified one
        // reports "10.0 lost" for every tier above the first and the measurement says nothing.
        target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH)
                .setBaseValue(200.0D);
        target.setHealth(200.0F);
        coolDown(helper, hunter);
        aimAt(hunter, target.getBoundingBox().getCenter());
        float before = target.getHealth();
        chargeAndRelease(hunter, holdTicks);
        float lost = before - target.getHealth();
        target.discard();
        hunter.getCooldowns().removeCooldown(com.carro1001.mhnw.registry.ModItems.GIANT_JAWBLADE.get());
        return lost;
    }

    /**
     * R3-01: the item id resolves, its client resources are actually packaged, and the shaped
     * recipe matches its exact pattern and nothing else.
     */
    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void r3JawbladeRegistryAndResources(GameTestHelper helper) {
        helper.assertTrue(net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                                MHNW.MOD_ID, "giant_jawblade")),
                "mhnw:giant_jawblade did not resolve in the item registry");

        for (String path : new String[]{
                "/assets/mhnw/models/item/giant_jawblade.json",
                "/assets/mhnw/textures/item/giant_jawblade_model.png"}) {
            try (java.io.InputStream packaged = MHNW.class.getResourceAsStream(path)) {
                helper.assertTrue(packaged != null, path + " is not packaged");
            } catch (java.io.IOException failure) {
                helper.fail("could not read " + path + ": " + failure);
            }
        }
        helper.assertTrue(langContains(helper, "item.mhnw.giant_jawblade"),
                "the weapon has no en_us entry, so it would show its translation key");

        assertCrafts(helper, 3, 3, "BBC" + "BHC" + "BSH",
                com.carro1001.mhnw.registry.ModItems.GIANT_JAWBLADE.get());

        // The same nine ingredients with the stick and a hide swapped: the pattern is the
        // contract, not the bill of materials.
        java.util.List<net.minecraft.world.item.ItemStack> shuffled = new java.util.ArrayList<>();
        for (char slot : ("BBC" + "BSC" + "BHH").toCharArray()) {
            shuffled.add(switch (slot) {
                case 'H' -> new net.minecraft.world.item.ItemStack(
                        com.carro1001.mhnw.registry.ModItems.MONSTER_HIDE.get());
                case 'C' -> new net.minecraft.world.item.ItemStack(
                        com.carro1001.mhnw.registry.ModItems.MONSTER_CLAW.get());
                case 'S' -> new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STICK);
                default -> new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BONE);
            });
        }
        net.minecraft.world.item.crafting.CraftingInput wrong =
                net.minecraft.world.item.crafting.CraftingInput.of(3, 3, shuffled);
        helper.assertTrue(helper.getLevel().getServer().getRecipeManager().getRecipeFor(
                        net.minecraft.world.item.crafting.RecipeType.CRAFTING, wrong, helper.getLevel())
                        .isEmpty(),
                "a rearranged grid still crafted the weapon");
        helper.succeed();
    }

    private static boolean langContains(GameTestHelper helper, String key) {
        try (java.io.InputStream lang = MHNW.class.getResourceAsStream("/assets/mhnw/lang/en_us.json")) {
            if (lang == null) {
                return false;
            }
            return new String(lang.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).contains(key);
        } catch (java.io.IOException failure) {
            helper.fail("could not read the language file: " + failure);
            return false;
        }
    }

    /**
     * R3-02: the weapon's numbers are read off the player who is holding it, not off a constant.
     * Total attack damage 9.0, attack speed 0.8, 250 durability, bone repair -- and unequipping
     * takes the modifiers with it, so nothing can be left stacked or stale.
     */
    @GameTest(template = ARENA, timeoutTicks = 60)
    public static void r3JawbladeHasItsStatedNumbers(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer hunter = wielder(helper, 8, 2, 8);
        net.minecraft.world.item.ItemStack blade = hunter.getMainHandItem();

        helper.assertTrue(blade.getMaxDamage() == 250,
                "durability is " + blade.getMaxDamage() + ", expected 250");
        helper.assertTrue(blade.getItem().isValidRepairItem(blade, new net.minecraft.world.item.ItemStack(
                        net.minecraft.world.item.Items.BONE)),
                "bone does not repair the weapon");
        helper.assertTrue(!blade.getItem().isValidRepairItem(blade, new net.minecraft.world.item.ItemStack(
                        net.minecraft.world.item.Items.IRON_INGOT)),
                "iron still repairs the weapon; it is iron-tier for its numbers only");

        helper.startSequence()
                .thenExecuteFor(2, () -> tickOnce(hunter))
                .thenExecute(() -> {
                    double damage = hunter.getAttributeValue(
                            net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
                    double speed = hunter.getAttributeValue(
                            net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED);
                    helper.assertTrue(Math.abs(damage - 9.0D) < 1.0E-4,
                            "observed total attack damage " + damage + ", expected 9.0");
                    helper.assertTrue(Math.abs(speed - 0.8D) < 1.0E-4,
                            "observed attack speed " + speed + ", expected 0.8");
                    hunter.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                            net.minecraft.world.item.ItemStack.EMPTY);
                })
                .thenExecuteFor(2, () -> tickOnce(hunter))
                .thenExecute(() -> {
                    double bare = hunter.getAttributeValue(
                            net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
                    retire(hunter);
                    helper.assertTrue(Math.abs(bare - 1.0D) < 1.0E-4,
                            "an unequipped weapon left " + bare + " attack damage behind");
                })
                .thenSucceed();
    }

    /**
     * R3-03: an ordinary left-click is the ordinary path. Through a {@code MonsterPart}, because
     * that is the case a weapon could plausibly break: the parent takes the hit, the blade takes
     * exactly one point of wear, and carve participation is recorded the same way a bare hand
     * would record it.
     */
    @GameTest(template = ARENA, timeoutTicks = 60)
    public static void r3OrdinaryAttackRunsTheVanillaPath(GameTestHelper helper) {
        GreatIzuchi quarry = spawnInert(helper);
        net.minecraft.server.level.ServerPlayer hunter = wielder(helper, 8, 2, 8);
        net.neoforged.neoforge.entity.PartEntity<?> part = quarry.getParts()[0];
        float healthBefore = quarry.getHealth();

        hunter.attack(part);

        int wear = hunter.getMainHandItem().getDamageValue();
        int participants = quarry.carveState().participantCount();
        retire(hunter);
        helper.assertTrue(quarry.getHealth() < healthBefore,
                "a part hit did not reach the parent's health");
        helper.assertTrue(wear == 1,
                "the blade took " + wear + " durability for one hit, expected 1");
        helper.assertTrue(participants == 1,
                "a hit through the weapon recorded " + participants + " carve participants");
        helper.succeed();
    }

    /**
     * R3-04: nothing swings while the charge is still held, and a release swings exactly once.
     * Fully ticked -- item in hand, vanilla's own countdown, vanilla's own release -- so nothing
     * here can pass by calling an item seam directly.
     */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void r3ChargeSwingsOnReleaseNotWhileHeld(GameTestHelper helper) {
        net.minecraft.world.entity.animal.Cow target = inertCow(helper, 8, 2, 11);
        net.minecraft.server.level.ServerPlayer hunter = wielder(helper, 8, 2, 8);
        aimAt(hunter, target.getBoundingBox().getCenter());
        float healthBefore = target.getHealth();

        hunter.startUsingItem(net.minecraft.world.InteractionHand.MAIN_HAND);
        helper.assertTrue(hunter.getUseItemRemainingTicks()
                        == com.carro1001.mhnw.item.GiantJawbladeItem.OVERCHARGE_TICKS,
                "starting the charge did not arm the full overcharge window");

        helper.startSequence()
                .thenExecuteFor(maxTierTicks(), () -> tickOnce(hunter))
                .thenExecute(() -> {
                    helper.assertTrue(target.getHealth() == healthBefore,
                            "the weapon swung while the charge was still being held");
                    helper.assertTrue(hunter.isUsingItem(), "the charge was dropped part way");
                    hunter.releaseUsingItem();
                })
                .thenExecute(() -> {
                    boolean cooling = hunter.getCooldowns().isOnCooldown(
                            com.carro1001.mhnw.registry.ModItems.GIANT_JAWBLADE.get());
                    float after = target.getHealth();
                    retire(hunter);
                    helper.assertTrue(after < healthBefore, "releasing a full charge did not strike");
                    helper.assertTrue(cooling, "a completed swing started no recovery cooldown");
                })
                .thenSucceed();
    }

    /**
     * R3-04: each tier lands its own stated damage, measured on a real held-and-released charge.
     *
     * <p>Each case cools the attack first, so what is measured is the tier rather than vanilla's
     * attack-strength ramp, and the tiers are asserted as an increasing set as well as against
     * their stated numbers -- a bonus wired to the wrong tier passes the second check and fails the
     * first.
     */
    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void r3ChargeTiersLandTheirStatedDamage(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer hunter = wielder(helper, 8, 2, 8);
        int[] tierTicks = com.carro1001.mhnw.item.GiantJawbladeItem.TIER_TICKS;
        float[] tierDamage = com.carro1001.mhnw.item.GiantJawbladeItem.TIER_DAMAGE;

        float[] measured = new float[tierTicks.length];
        for (int tier = 0; tier < tierTicks.length; tier++) {
            measured[tier] = chargeDamageAt(helper, hunter, tierTicks[tier], 8, 2, 10);
            helper.assertTrue(Math.abs(measured[tier] - tierDamage[tier]) < 0.51F,
                    "tier " + (tier + 1) + " dealt " + measured[tier] + ", expected about "
                            + tierDamage[tier]);
        }
        for (int tier = 1; tier < measured.length; tier++) {
            helper.assertTrue(measured[tier] > measured[tier - 1],
                    "tier " + (tier + 1) + " (" + measured[tier] + ") did not out-damage tier "
                            + tier + " (" + measured[tier - 1] + ")");
        }
        retire(hunter);
        helper.succeed();
    }

    /**
     * R3-04: overcharging wastes the charge rather than banking it. Holding past the overcharge
     * point swings by itself -- vanilla's completion, which is the one path that still runs through
     * {@code finishUsingItem} -- and lands tier one's damage, not tier three's.
     */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void r3OverchargeSwingsItselfAtTierOne(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer hunter = wielder(helper, 8, 2, 8);
        float[] tierDamage = com.carro1001.mhnw.item.GiantJawbladeItem.TIER_DAMAGE;

        float overcharged = chargeDamageAt(helper, hunter,
                com.carro1001.mhnw.item.GiantJawbladeItem.OVERCHARGE_TICKS + 2, 8, 2, 10);

        retire(hunter);
        helper.assertTrue(overcharged > 0.0F,
                "holding past the overcharge point never swung at all");
        helper.assertTrue(Math.abs(overcharged - tierDamage[0]) < 0.51F,
                "an overcharged swing dealt " + overcharged + ", expected tier one's "
                        + tierDamage[0]);
        helper.assertTrue(overcharged < tierDamage[tierDamage.length - 1] - 0.5F,
                "an overcharged swing still landed full-tier damage, so overcharging costs nothing");
        helper.succeed();
    }

    /**
     * The charge is a commitment of the whole body: while it runs, horizontal movement is cut every
     * tick. Checked against a real held charge rather than the constant, and compared with the same
     * push applied while not charging, so vanilla's own friction cannot be mistaken for the effect.
     */
    @GameTest(template = ARENA, timeoutTicks = 120)
    public static void r3ChargingCutsMovement(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer hunter = wielder(helper, 8, 4, 8);
        net.minecraft.world.phys.Vec3 push = new net.minecraft.world.phys.Vec3(0.5D, 0.0D, 0.0D);

        hunter.setDeltaMovement(push);
        tickOnce(hunter);
        double freeSpeed = hunter.getDeltaMovement().horizontalDistance();

        hunter.startUsingItem(net.minecraft.world.InteractionHand.MAIN_HAND);
        hunter.setDeltaMovement(push);
        tickOnce(hunter);
        double chargingSpeed = hunter.getDeltaMovement().horizontalDistance();
        hunter.stopUsingItem();
        retire(hunter);

        helper.assertTrue(freeSpeed > 0.0D, "fixture error: the un-charged push produced no motion");
        helper.assertTrue(chargingSpeed < freeSpeed * 0.6D,
                "charging kept " + chargingSpeed + " of " + freeSpeed
                        + " horizontal speed; it is meant to be a heavy crawl");
        helper.succeed();
    }

    /**
     * R3-04: letting go before tier one is not a weaker strike, it is no strike. No damage, no
     * cooldown, no wear -- and there is no cancellation code behind that, only a release that finds
     * no tier to swing.
     */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void r3CancelledChargeChangesNothing(GameTestHelper helper) {
        net.minecraft.world.entity.animal.Cow target = inertCow(helper, 8, 2, 11);
        net.minecraft.server.level.ServerPlayer hunter = wielder(helper, 8, 2, 8);
        aimAt(hunter, target.getBoundingBox().getCenter());
        float healthBefore = target.getHealth();

        hunter.startUsingItem(net.minecraft.world.InteractionHand.MAIN_HAND);

        helper.startSequence()
                // Short of tier one, which is the only release that costs nothing at all.
                .thenExecuteFor(com.carro1001.mhnw.item.GiantJawbladeItem.TIER_TICKS[0] - 3,
                        () -> tickOnce(hunter))
                .thenExecute(hunter::releaseUsingItem)
                .thenExecuteFor(com.carro1001.mhnw.item.GiantJawbladeItem.OVERCHARGE_TICKS + 5,
                        () -> tickOnce(hunter))
                .thenExecute(() -> {
                    boolean cooling = hunter.getCooldowns().isOnCooldown(
                            com.carro1001.mhnw.registry.ModItems.GIANT_JAWBLADE.get());
                    int wear = hunter.getMainHandItem().getDamageValue();
                    float after = target.getHealth();
                    retire(hunter);
                    helper.assertTrue(after == healthBefore, "a released charge struck anyway");
                    helper.assertTrue(!cooling, "a released charge started the recovery cooldown");
                    helper.assertTrue(wear == 0, "a released charge cost " + wear + " durability");
                })
                .thenSucceed();
    }

    /**
     * R3-05: the strike reaches 4.5 blocks down the player's own view vector, and nothing else.
     * Each case proves its own geometry -- the distances are asserted, not assumed -- so a case
     * cannot pass because a cow happened to be somewhere other than where it was meant to be.
     */
    @GameTest(template = ARENA, timeoutTicks = 80)
    public static void r3ChargeRangeAndOcclusion(GameTestHelper helper) {
        double reach = com.carro1001.mhnw.item.GiantJawbladeItem.REACH;
        // Stated separately from the constant the cases are placed against: without this, widening
        // REACH moves the fixture with the code and the far case reports a fixture error instead of
        // the behaviour change it actually is (observed, mutating REACH to 50).
        helper.assertTrue(reach == 4.5D, "the charged strike's reach is " + reach + ", expected 4.5");
        net.minecraft.server.level.ServerPlayer hunter = wielder(helper, 8, 2, 8);

        net.minecraft.world.entity.animal.Cow near = inertCow(helper, 8, 2, 11);
        double nearDistance = hunter.getEyePosition().distanceTo(near.getBoundingBox().getCenter());
        helper.assertTrue(nearDistance < reach,
                "fixture error: the near cow is " + nearDistance + " blocks away, outside the reach");
        aimAt(hunter, near.getBoundingBox().getCenter());
        float nearBefore = near.getHealth();
        completeCharge(helper, hunter);
        helper.assertTrue(near.getHealth() < nearBefore,
                "a centred target " + nearDistance + " blocks away was not struck");
        near.discard();

        // Out of range: same line, further than the reach.
        net.minecraft.world.entity.animal.Cow far = inertCow(helper, 8, 2, 14);
        double farDistance = hunter.getEyePosition().distanceTo(far.getBoundingBox().getCenter());
        helper.assertTrue(farDistance > reach,
                "fixture error: the far cow is only " + farDistance + " blocks away");
        aimAt(hunter, far.getBoundingBox().getCenter());
        float farBefore = far.getHealth();
        completeCharge(helper, hunter);
        helper.assertTrue(far.getHealth() == farBefore,
                "a target " + farDistance + " blocks away was struck; the reach is " + reach);
        far.discard();

        // Off axis: well inside the reach, nowhere near the view vector.
        net.minecraft.world.entity.animal.Cow beside = inertCow(helper, 11, 2, 8);
        double besideDistance = hunter.getEyePosition().distanceTo(beside.getBoundingBox().getCenter());
        helper.assertTrue(besideDistance < reach,
                "fixture error: the flanking cow is " + besideDistance + " blocks away, out of reach anyway");
        aimAt(hunter, hunter.getEyePosition().add(0.0D, 0.0D, 4.0D));
        float besideBefore = beside.getHealth();
        completeCharge(helper, hunter);
        helper.assertTrue(beside.getHealth() == besideBefore,
                "a target " + besideDistance + " blocks off the view vector was struck");
        beside.discard();

        // Occluded: in range, on the line, behind a solid wall.
        net.minecraft.world.entity.animal.Cow walled = inertCow(helper, 8, 2, 11);
        for (int y = 2; y <= 4; y++) {
            for (int x = 6; x <= 10; x++) {
                helper.setBlock(x, y, 10, net.minecraft.world.level.block.Blocks.STONE);
            }
        }
        aimAt(hunter, walled.getBoundingBox().getCenter());
        float walledBefore = walled.getHealth();
        completeCharge(helper, hunter);
        boolean throughWall = walled.getHealth() < walledBefore;
        retire(hunter);
        helper.assertTrue(!throughWall, "the strike went through a solid wall");
        helper.succeed();
    }

    /**
     * R3-06: one strike, one target. Two cows on the same line and a whole multipart monster are
     * both single hits -- there is no cone, no sweep, and a part does not also hit its parent.
     */
    @GameTest(template = ARENA, timeoutTicks = 80)
    public static void r3ChargeStrikesExactlyOneTarget(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer hunter = wielder(helper, 8, 2, 8);
        net.minecraft.world.entity.animal.Cow first = inertCow(helper, 8, 2, 10);
        net.minecraft.world.entity.animal.Cow second = inertCow(helper, 8, 2, 12);
        float firstBefore = first.getHealth();
        float secondBefore = second.getHealth();

        aimAt(hunter, first.getBoundingBox().getCenter());
        completeCharge(helper, hunter);

        helper.assertTrue(first.getHealth() < firstBefore, "the nearest target was not struck");
        helper.assertTrue(second.getHealth() == secondBefore,
                "a second target behind the first was struck as well");
        first.discard();
        second.discard();

        // Beside the target, not behind it. This is the case vanilla's own sweep reaches and the
        // in-line pair above does not: sweep collects living entities within one block of the
        // struck target's box and three of the player, so a bystander at z=12 is never a witness.
        net.minecraft.world.entity.animal.Cow aimed = inertCow(helper, 8, 2, 10);
        net.minecraft.world.entity.animal.Cow bystander = inertCow(helper, 9, 2, 10);
        float aimedBefore = aimed.getHealth();
        float bystanderBefore = bystander.getHealth();
        helper.assertTrue(hunter.distanceToSqr(bystander) < 9.0D,
                "fixture error: the bystander is outside the sweep's own 3-block range, so this"
                        + " case would pass without proving anything");
        coolDown(helper, hunter);
        aimAt(hunter, aimed.getBoundingBox().getCenter());
        completeCharge(helper, hunter);
        helper.assertTrue(aimed.getHealth() < aimedBefore, "the aimed target was not struck");
        helper.assertTrue(bystander.getHealth() == bystanderBefore,
                "a bystander beside the target lost " + (bystanderBefore - bystander.getHealth())
                        + " health; the strike swept");
        aimed.discard();
        bystander.discard();

        // Against a multipart body, "exactly one hit" has to be a number rather than a hope, so an
        // ordinary attack is measured first and the charge is held to it.
        GreatIzuchi quarry = spawnInert(helper);
        float quarryBefore = quarry.getHealth();
        coolDown(helper, hunter);
        hunter.attack(quarry);
        float singleHit = quarryBefore - quarry.getHealth();
        quarry.invulnerableTime = 0;
        float beforeCharge = quarry.getHealth();

        coolDown(helper, hunter);
        aimAt(hunter, quarry.getBoundingBox().getCenter());
        completeCharge(helper, hunter);

        float chargeCost = beforeCharge - quarry.getHealth();
        // The reference is one ordinary hit, which vanilla guarantees is exactly one hit. A full
        // charge is legitimately harder than that by the tier ratio, so the ceiling scales with it;
        // a part and its parent both taking the same swing would land near twice this.
        float[] tierDamage = com.carro1001.mhnw.item.GiantJawbladeItem.TIER_DAMAGE;
        float oneChargedHit = singleHit * (tierDamage[tierDamage.length - 1] / tierDamage[0]);
        retire(hunter);
        helper.assertTrue(singleHit > 0.0F, "fixture error: the reference hit did no damage");
        helper.assertTrue(chargeCost > 0.0F, "the charge did not reach the monster at all");
        helper.assertTrue(chargeCost <= oneChargedHit * 1.2F,
                "the charge cost the monster " + chargeCost + " health against one charged hit's"
                        + " expected " + oneChargedHit + "; a part and its parent were both hit,"
                        + " or the volume swept");
        helper.succeed();
    }

    /**
     * R3-07/R3-08: a completed miss is still a commitment -- one cooldown, no wear, nothing hurt --
     * the cooldown genuinely refuses the next charge until it expires, and an item round trip
     * carries durability but no trace of a charge.
     */
    @GameTest(template = ARENA, timeoutTicks = 120)
    public static void r3ChargeMissRecoversAndCarriesNoState(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer hunter = wielder(helper, 8, 2, 8);
        aimAt(hunter, hunter.getEyePosition().add(0.0D, 0.0D, 4.0D));

        completeCharge(helper, hunter);
        helper.assertTrue(hunter.getMainHandItem().getDamageValue() == 0,
                "a missed charge cost durability");
        helper.assertTrue(hunter.getCooldowns().isOnCooldown(
                        com.carro1001.mhnw.registry.ModItems.GIANT_JAWBLADE.get()),
                "a missed charge started no recovery cooldown");

        net.minecraft.world.InteractionResultHolder<net.minecraft.world.item.ItemStack> refused =
                hunter.getMainHandItem().getItem().use(helper.getLevel(), hunter,
                        net.minecraft.world.InteractionHand.MAIN_HAND);
        helper.assertTrue(refused.getResult() == net.minecraft.world.InteractionResult.FAIL
                        && !hunter.isUsingItem(),
                "a recovering weapon started another charge");

        net.minecraft.world.item.ItemStack worn = hunter.getMainHandItem().copy();
        worn.setDamageValue(42);
        net.minecraft.world.item.ItemStack reloaded = net.minecraft.world.item.ItemStack
                .parse(helper.getLevel().registryAccess(),
                        worn.save(helper.getLevel().registryAccess()))
                .orElseThrow();
        helper.assertTrue(reloaded.getDamageValue() == 42,
                "an item round trip lost its durability");
        helper.assertTrue(reloaded.getComponents().equals(worn.getComponents()),
                "an item round trip changed the weapon's components, so something was stored on it");

        helper.startSequence()
                .thenExecuteFor(com.carro1001.mhnw.item.GiantJawbladeItem.RECOVERY_TICKS + 2, () -> tickOnce(hunter))
                .thenExecute(() -> {
                    boolean stillCooling = hunter.getCooldowns().isOnCooldown(
                            com.carro1001.mhnw.registry.ModItems.GIANT_JAWBLADE.get());
                    retire(hunter);
                    helper.assertTrue(!stillCooling, "the recovery cooldown never expired");
                })
                .thenSucceed();
    }

    // ---------------------------------------------------------------- creative tab

    /**
     * The mod's own creative tab resolves, is keyed correctly and points its icon at the intended
     * item. What actually lands in it (every material/food/armor/weapon/bucket/egg, in the chosen
     * order) is exercised live every time a human opens the tab in the client this packet also
     * builds -- reconstructing NeoForge's tab-population event by hand for a headless duplicate of
     * that would be more machinery than the wiring is worth.
     */
    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void modCreativeTabResolvesWithItsIcon(GameTestHelper helper) {
        net.minecraft.resources.ResourceLocation key =
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MHNW.MOD_ID, "main");
        helper.assertTrue(net.minecraft.core.registries.BuiltInRegistries.CREATIVE_MODE_TAB.containsKey(key),
                "mhnw:main did not resolve in the creative mode tab registry");
        net.minecraft.world.item.CreativeModeTab tab =
                net.minecraft.core.registries.BuiltInRegistries.CREATIVE_MODE_TAB.get(key);
        helper.assertTrue(tab != null && tab.getIconItem().is(ModEntities.GREAT_IZUCHI_SPAWN_EGG.get()),
                "the mod's creative tab icon is " + (tab == null ? "null" : tab.getIconItem())
                        + ", expected the Great Izuchi spawn egg");
        helper.succeed();
    }

    /**
     * The Giant Jawblade's model must never inherit from vanilla's flat-item chain.
     *
     * <p>This is a real bug that shipped twice and cost two rounds, and it is invisible to every
     * other check: the model loads, the item works, a dropped one still casts a shadow, and nothing
     * is logged. {@code item/handheld} parents {@code item/generated} parents
     * {@code builtin/generated}, and {@link net.minecraft.client.resources.model.ModelBakery} bakes
     * any model whose <em>root</em> parent is that marker through {@code ItemModelGenerator}, which
     * throws the model's own {@code elements} away and builds quads purely from {@code layer0} ..
     * {@code layer4}. A cuboid model names its texture {@code "0"}, not {@code "layer0"}, so the
     * generator finds no layers, emits no quads, and the weapon renders as nothing at all.
     *
     * <p>A 3D item model therefore declares no parent and carries its own {@code display} block --
     * which is exactly what the artist's export does, and what a well-meaning "fix" to inherit
     * vanilla's hand transforms undoes. Checked as text on purpose: model baking is client-only, so
     * a dedicated server cannot bake this model to count its quads, but it can read the file.
     */
    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void r3JawbladeModelDoesNotInheritTheFlatItemChain(GameTestHelper helper) {
        String model = readPackaged(helper, "/assets/mhnw/models/item/giant_jawblade.json");
        helper.assertTrue(!model.contains("\"parent\""),
                "the weapon model declares a parent; a 3D item model must not, because vanilla's"
                        + " item parents resolve to builtin/generated and discard its elements");
        helper.assertTrue(model.contains("\"elements\"") && model.contains("\"display\""),
                "the weapon model lost its own elements or display block");
        helper.assertTrue(model.contains("\"0\":") && !model.contains("layer0"),
                "the weapon model uses a layer texture key, which only means anything to the flat"
                        + " item generator this model must not go through");

        // The charge-tier models are the one place a parent is correct: they inherit this model's
        // geometry so only their held poses differ. That parent must still be ours -- pointing any
        // of them at a vanilla item model would hand the whole chain back to the flat generator.
        for (int tier = 1; tier <= com.carro1001.mhnw.item.GiantJawbladeItem.CHARGE_POSE_STEPS; tier++) {
            String path = "/assets/mhnw/models/item/giant_jawblade_charge_" + tier + ".json";
            String pose = readPackaged(helper, path);
            helper.assertTrue(pose.contains("\"mhnw:item/giant_jawblade\""),
                    path + " does not inherit the weapon's own model");
            helper.assertTrue(!pose.contains("item/generated") && !pose.contains("item/handheld"),
                    path + " parents a vanilla item model, which discards the geometry it inherits");
            helper.assertTrue(model.contains("giant_jawblade_charge_" + tier + "\""),
                    "the weapon model has no override pointing at charge pose " + tier
                            + ", so that pose can never be shown. If CHARGE_POSE_STEPS changed,"
                            + " rerun node tools/gen_jawblade_charge_models.js");
        }
        helper.succeed();
    }

    /** Read a packaged client resource as text, or fail the test saying which one was missing. */
    private static String readPackaged(GameTestHelper helper, String path) {
        try (java.io.InputStream packaged = MHNW.class.getResourceAsStream(path)) {
            if (packaged == null) {
                helper.fail(path + " is not packaged");
                return "";
            }
            return new String(packaged.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (java.io.IOException failure) {
            helper.fail("could not read " + path + ": " + failure);
            return "";
        }
    }

    private static void fillFloor(GameTestHelper helper, int y, net.minecraft.world.level.block.Block block) {
        for (int x = 0; x <= 15; x++) {
            for (int z = 0; z <= 15; z++) {
                helper.setBlock(x, y, z, block);
            }
        }
    }
}
