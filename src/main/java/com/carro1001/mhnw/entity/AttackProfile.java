package com.carro1001.mhnw.entity;

/**
 * Everything that differs between one Great Izuchi attack and another.
 *
 * <p>This exists because there are now three real attacks, not as speculative structure. The
 * previous implementation of this mod had three same-priority melee goals and a TODO noting that
 * it kept choosing the same one; the fix is one goal that owns the timeline and a small table of
 * per-attack numbers, not a goal per attack. There is deliberately no JSON, no registry and no
 * scripting: it is a handful of typed values with names.
 *
 * <p>All tick fields are ages within the action, counted from its first tick. All geometry is in
 * the local (left, up, forward) frame documented on {@link GreatIzuchi}, in blocks.
 *
 * @param id             synchronized attack id; {@link GreatIzuchi#ATTACK_NONE} is reserved
 * @param windupEnd      last tick of the telegraph; facing is committed at the end of it
 * @param activeStart    first tick on which the volume is evaluated
 * @param activeEnd      last tick on which the volume is evaluated
 * @param actionEnd      last tick of the action; the clip length minus one
 * @param cooldown       ticks after the action before any attack may start again
 * @param strikes        how many times this attack may hit one victim, one per strike window
 * @param volumeSize     edge length of the cubic damage volume
 * @param damage         damage per strike, before the entity's attack damage attribute scales it
 * @param aimOffsetDeg   degrees the body squares up off-axis so the arc crosses the target
 * @param lungeSpeed     blocks per tick the monster paces forward through the active window
 * @param minRange       closest distance at which this attack may be chosen
 * @param maxRange       furthest distance at which this attack may be chosen
 * @param path           {tick, left, up, forward} keyframes of the damaging limb, interpolated
 */
public record AttackProfile(
        byte id,
        int windupEnd,
        int activeStart,
        int activeEnd,
        int actionEnd,
        int cooldown,
        int strikes,
        double volumeSize,
        double damage,
        float aimOffsetDeg,
        double lungeSpeed,
        double minRange,
        double maxRange,
        double[][] path) {

    /**
     * Claw scratch. 3.25 s clip, 65 ticks.
     *
     * <p>Phases measured from the asset: the claw rears from 0.88 to 2.37 blocks at under 0.18
     * blocks per tick through the windup, then sweeps at 0.2 to 2.1 for thirty ticks, then settles.
     * The path is the measured hand position from the runtime bone probe, smoothed over seven ticks
     * to remove the vertical bounce and pushed outward on a ramp so the claw reaches further with
     * each strike window. The 25.4 degree aim offset is the measured mean bearing of the arc: this
     * is a cross-body swipe, so a monster squared up dead centre sweeps past its target.
     */
    public static final AttackProfile SCRATCH = new AttackProfile(
            GreatIzuchi.ATTACK_SCRATCH,
            17, 18, 47, 64,
            30, 3, 0.9D, 2.5D, 25.4F, 0.11D,
            0.0D, 2.6D,
            new double[][] {
                    // age    left      up   forward
                    {18, -1.17D, 1.29D, 2.18D},
                    {21, -1.30D, 1.39D, 2.33D},
                    {24, -1.46D, 1.77D, 2.43D},
                    {27, -1.49D, 2.13D, 2.61D},
                    {30, -1.00D, 1.97D, 2.72D},
                    {33, -0.32D, 1.58D, 2.49D},
                    {36, -0.81D, 1.97D, 2.45D},
                    {39, -1.65D, 2.35D, 2.73D},
                    {42, -1.52D, 2.02D, 2.86D},
                    {45, -1.32D, 1.34D, 2.39D},
                    {47, -1.55D, 1.30D, 1.72D},
            });

    /**
     * Tail swipe. 2.375 s clip, 48 ticks. Path measured with the runtime bone probe.
     *
     * <p>A wide, low sweep. The tail first arcs up and over the monster through ticks 7 to 25,
     * peaking above 5 blocks where it can hit nothing, pauses, and only then comes down and
     * scythes around the body: that descent, ticks 28 to 43, is the part that can actually strike
     * something standing on the ground, at 0.4 to 1.4 blocks up and a radius of 3.4 to 4.9.
     *
     * <p>The sweep covers nearly a full circle around the monster, so unlike the claw there is no
     * aim offset to apply. It also has a minimum range: a victim standing underneath the body is
     * inside the arc and will not be touched.
     *
     * <p>An earlier offline solve put this window at ticks 6 to 13, when the tail is in fact still
     * behind the monster and rising. That is why the volume appeared frozen at the end of its path
     * for most of the action and hit nothing.
     */
    public static final AttackProfile TAIL_SWIPE = new AttackProfile(
            GreatIzuchi.ATTACK_TAIL_SWIPE,
            27, 28, 43, 47,
            40, 1, 1.3D, 4.0D, 0.0F, 0.0D,
            2.0D, 4.6D,
            new double[][] {
                    // age    left      up   forward
                    {28, -2.62D, 1.81D, 0.52D},
                    {29, -2.72D, 1.35D, -1.20D},
                    {30, -1.63D, 1.01D, -2.91D},
                    {31, 0.21D, 0.52D, -3.43D},
                    {32, 2.65D, 0.43D, -2.95D},
                    {33, 4.11D, 0.68D, -1.60D},
                    {34, 4.62D, 0.66D, -0.45D},
                    {35, 4.78D, 0.77D, 1.07D},
                    {36, 4.36D, 0.99D, 2.52D},
                    {37, 3.38D, 1.25D, 3.65D},
                    {38, 2.15D, 1.37D, 4.33D},
                    {39, 0.95D, 1.30D, 4.51D},
                    {40, -0.30D, 1.07D, 4.26D},
                    {41, -1.06D, 0.92D, 3.80D},
                    {42, -1.37D, 1.05D, 3.36D},
                    {43, -1.57D, 1.09D, 2.62D},
            });

    /**
     * Tail slam. 4.375 s clip, 88 ticks. Path measured with the runtime bone probe.
     *
     * <p>The longest commitment in the set: the tail rears overhead and hangs there for most of two
     * seconds, whips through a fast arc, and drives into the ground 4.4 blocks directly in front.
     * Only that impact can hurt anything, ticks 42 to 48, where the tip passes 0.2 to 0.7 up.
     *
     * <p>The whip from ticks 34 to 41 moves at three to four blocks per tick and reaches seven
     * blocks up. It is deliberately excluded: a volume moving that fast would teleport through a
     * victim between ticks, and it is above head height regardless.
     *
     * <p>Measured from one clean instance rather than an average of three. Averaging several
     * actions smeared the fast section, because the runs are not frame-aligned with each other.
     */
    public static final AttackProfile TAIL_SLAM = new AttackProfile(
            GreatIzuchi.ATTACK_TAIL_SLAM,
            41, 42, 48, 87,
            60, 1, 1.5D, 6.0D, 0.0F, 0.0D,
            3.0D, 5.2D,
            new double[][] {
                    // age    left      up   forward
                    {42, 0.73D, 5.10D, 2.52D},
                    {43, 0.85D, 2.49D, 4.33D},
                    {44, 0.14D, 0.23D, 4.42D},
                    {45, 0.01D, 0.70D, 4.59D},
                    {46, -0.18D, 0.71D, 4.59D},
                    {47, -0.18D, 0.54D, 4.52D},
                    {48, -0.10D, 0.50D, 4.50D},
            });

    private static final AttackProfile[] ALL = {SCRATCH, TAIL_SWIPE, TAIL_SLAM};

    /** Resolves a synchronized attack id back to its profile, or null for none/unknown. */
    public static AttackProfile byId(byte id) {
        for (AttackProfile profile : ALL) {
            if (profile.id == id) {
                return profile;
            }
        }
        return null;
    }

    /** Every attack that could be chosen at this distance. */
    public static AttackProfile[] all() {
        return ALL;
    }

    public boolean inRange(double distance) {
        return distance >= this.minRange && distance <= this.maxRange;
    }

    /** Which strike window this action age falls in, 0-based. */
    public int strikeIndexAt(int age) {
        int span = this.activeEnd - this.activeStart + 1;
        int index = (age - this.activeStart) * this.strikes / span;
        return Math.max(0, Math.min(this.strikes - 1, index));
    }

    /** Interpolates the limb path at this action age, in the local frame. */
    public double[] limbLocalAt(int age) {
        double[] first = this.path[0];
        if (age <= first[0]) {
            return new double[] {first[1], first[2], first[3]};
        }
        double[] last = this.path[this.path.length - 1];
        if (age >= last[0]) {
            return new double[] {last[1], last[2], last[3]};
        }
        for (int i = 0; i < this.path.length - 1; i++) {
            double[] a = this.path[i];
            double[] b = this.path[i + 1];
            if (age >= a[0] && age <= b[0]) {
                double f = (age - a[0]) / (b[0] - a[0]);
                return new double[] {
                        a[1] + (b[1] - a[1]) * f,
                        a[2] + (b[2] - a[2]) * f,
                        a[3] + (b[3] - a[3]) * f,
                };
            }
        }
        return new double[] {last[1], last[2], last[3]};
    }
}
