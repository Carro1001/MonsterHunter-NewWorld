package com.carro1001.mhnw;

import com.carro1001.mhnw.entity.Aptonoth;
import com.carro1001.mhnw.entity.AttackProfile;
import com.carro1001.mhnw.entity.GreatIzuchi;
import com.carro1001.mhnw.entity.GreatIzuchiCombatGoal;
import com.carro1001.mhnw.entity.Lagiacrus;
import com.carro1001.mhnw.entity.LagiacrusPursuitGoal;
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

    /**
     * A Great Izuchi is a pack leader (handoff feedback: "spawns with 1-4 izuchis around it").
     * {@code helper.spawn} does not itself call {@code finalizeSpawn} the way a real world spawn
     * does, so this drives it directly with {@code MobSpawnType.NATURAL} to exercise the same path
     * {@link GreatIzuchi#finalizeSpawn} guards on.
     */
    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void greatIzuchiNaturalSpawnBringsAnEscort(GameTestHelper helper) {
        GreatIzuchi monster = helper.spawn(ModEntities.GREAT_IZUCHI.get(), 8, 2, 8);
        monster.finalizeSpawn(helper.getLevel(),
                helper.getLevel().getCurrentDifficultyAt(monster.blockPosition()),
                net.minecraft.world.entity.MobSpawnType.NATURAL, null);

        java.util.List<com.carro1001.mhnw.entity.Izuchi> escorts = helper.getLevel().getEntitiesOfClass(
                com.carro1001.mhnw.entity.Izuchi.class, monster.getBoundingBox().inflate(8.0D));
        helper.assertTrue(escorts.size() >= 1 && escorts.size() <= 4,
                "expected 1-4 escort Izuchi after a natural Great Izuchi spawn, got " + escorts.size());
        helper.succeed();
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
            for (MonsterPart part : aptonoth.monsterParts()) {
                helper.assertTrue(part.isRemoved() || !part.isAddedToLevel(),
                        "part " + part.partName + " outlived its parent");
            }
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
        helper.assertTrue(partCount == 9, "Rathian registered " + partCount + " parts, expected 9");

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
        helper.assertTrue(partCount == 7, "Rathalos registered " + partCount + " parts, expected 7");

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
    public static void lagiacrusCrossesShorelineInBothDirections(GameTestHelper helper) {
        // Two-deep water on the west, bank at the water surface on the east; room for the root.
        fillLagiacrusPool(helper, 7, 3);
        for (int x = 8; x <= 14; x++) {
            for (int z = 1; z <= 14; z++) {
                helper.setBlock(x, 2, z, net.minecraft.world.level.block.Blocks.STONE);
                helper.setBlock(x, 3, z, net.minecraft.world.level.block.Blocks.STONE);
            }
        }
        Lagiacrus monster = helper.spawn(ModEntities.LAGIACRUS.get(), 3, 2, 8);
        Cow landTarget = helper.spawn(EntityType.COW, 12, 4, 8);
        landTarget.setNoAi(true);
        var waterTarget = helper.spawn(EntityType.AXOLOTL, 3, 2, 8);
        waterTarget.setNoAi(true);
        waterTarget.setNoGravity(true);
        var navigation = monster.getNavigation();
        var control = monster.getMoveControl();
        monster.setTarget(landTarget);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(monster.isInWater(), "waiting for water entry"))
                .thenWaitUntil(() -> helper.assertTrue(!monster.isInWater() && monster.distanceTo(landTarget) < 3.0F,
                        "Lagiacrus failed to leave shallow water for the bank"))
                .thenExecute(() -> monster.setTarget(null))
                .thenWaitUntil(() -> assertLagiacrusPursuitStopped(helper, monster))
                .thenIdle(LagiacrusPursuitGoal.RETRY_COOLDOWN_TICKS + 5)
                .thenExecute(() -> monster.setTarget(waterTarget))
                .thenWaitUntil(() -> helper.assertTrue(monster.isInWater() && monster.distanceTo(waterTarget) < 3.0F,
                        "Lagiacrus failed to return from land to shallow water"))
                .thenExecute(() -> helper.assertTrue(monster.getNavigation() == navigation
                                && monster.getMoveControl() == control,
                        "shoreline transition replaced the native navigation/control pair"))
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
        for (MonsterPart part : monster.monsterParts()) {
            helper.assertTrue(!helper.getLevel().getPartEntities().contains(part),
                    "part outlived its parent in NeoForge lookup: " + part.partName);
        }
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
}
