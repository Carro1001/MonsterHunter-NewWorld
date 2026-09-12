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

    /** Length of this species' own roar clip, in ticks. */
    int roarDurationTicks();

    /** Whether this engagement has already had its opening roar. Plain entity state, not synced
     * (an AI decision, like {@code attackCooldown} elsewhere in this package, not presentation) --
     * kept on the entity rather than inside {@link RoarGoal} so a test can arm it directly to skip
     * the intro roar when it's testing something else about combat, not the roar itself. */
    boolean hasRoaredThisEngagement();

    void setRoaredThisEngagement(boolean roared);
}
