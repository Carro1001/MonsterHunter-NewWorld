package com.carro1001.mhnw.entity;

/**
 * Small contract {@link RoarGoal} needs from any species with an MHW-style opening roar. Each
 * implementor owns its own synced countdown field and its own clip length (same convention as
 * {@code attackCooldown}/{@code DATA_ATTACK_ID} elsewhere in this package); {@link RoarGoal} owns
 * only the engage/re-arm state machine and the freeze-in-place presentation, generic over any
 * {@code Mob} implementing this the same way {@link MonsterPart} is generic over {@code Mob} and
 * {@code BoneProbe} over any {@code GeoAnimatable} -- a small interface, not a shared base class.
 */
public interface Roarable {
    /** Ticks remaining in the current roar, or 0 when not roaring. Server-authoritative and synced
     * so the client's own animation controller can read it. */
    int getRoarTicks();

    void setRoarTicks(int ticks);

    /**
     * Level game time at which the current roar began; meaningless while not roaring.
     *
     * <p>Synced alongside the countdown, and for a different job: the countdown is the server's own
     * clock (how much longer to stay frozen) while this is a stable <em>presentation anchor</em>, so
     * a client that starts tracking mid-roar can tell how far in the clip already is, and can tell
     * one roar instance from the next. Deliberately game time rather than a tick counter, for the
     * same reason {@code DATA_ATTACK_START} elsewhere in this package is: a client's own
     * {@code tickCount} starts when the spawn packet arrives and says nothing about the server's.
     * {@link RoarGoal} sets it once at {@code start()}; nothing reads it back to make a decision.
     */
    long getRoarStartTime();

    void setRoarStartTime(long gameTime);

    /** Length of this species' own roar clip, in ticks. */
    int roarDurationTicks();

    /** Whether this engagement has already had its opening roar. Plain entity state, not synced
     * (an AI decision, like {@code attackCooldown} elsewhere in this package, not presentation) --
     * kept on the entity rather than inside {@link RoarGoal} so a test can arm it directly to skip
     * the intro roar when it's testing something else about combat, not the roar itself. */
    boolean hasRoaredThisEngagement();

    void setRoaredThisEngagement(boolean roared);
}
