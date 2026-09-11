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
     * Tail swipe. 2.375 s clip, 48 ticks. PATH IS PROVISIONAL, pending runtime measurement.
     *
     * <p>The speed profile shows one coherent burst over ticks 6 to 13: the tail tip sweeps at
     * ground height through a radius of about 4 blocks, from behind the monster around to its
     * front. That is a long-reach, low, horizontal sweep, so the range band is wider than the
     * claw's and the volume is bigger.
     *
     * <p>The numbers below come from an offline solve of the clip, which is known unreliable for a
     * heavily rotated chain: it puts the tail tip slightly below ground here, and produces frank
     * nonsense for the later part of this clip (tip 5.7 blocks in the air). Only the ticks whose
     * output is physically plausible are used, and they must be replaced with probe measurements.
     */
    public static final AttackProfile TAIL_SWIPE = new AttackProfile(
            GreatIzuchi.ATTACK_TAIL_SWIPE,
            5, 6, 13, 47,
            40, 1, 1.3D, 4.0D, 0.0F, 0.0D,
            0.0D, 4.5D,
            new double[][] {
                    // age    left      up   forward
                    {6, -0.33D, 1.85D, -4.16D},
                    {7, -1.17D, 0.80D, -3.60D},
                    {8, -2.09D, 0.30D, -2.93D},
                    {9, -3.13D, 0.25D, -1.92D},
                    {10, -3.82D, 0.25D, -0.60D},
                    {11, -3.99D, 0.36D, 0.87D},
                    {12, -3.05D, 0.34D, 2.24D},
                    {13, -1.99D, 0.31D, 2.66D},
            });

    /**
     * Tail slam. 4.375 s clip, 88 ticks. PATH IS PROVISIONAL, pending runtime measurement.
     *
     * <p>A long commitment: the tail rears overhead through a long windup and comes down in front.
     * The offline solve of the slam itself is erratic enough that these keys should be treated as
     * placeholders that merely put the volume roughly where the tail goes, not as calibration.
     * Measure with the bone probe before trusting the contact.
     */
    public static final AttackProfile TAIL_SLAM = new AttackProfile(
            GreatIzuchi.ATTACK_TAIL_SLAM,
            28, 29, 40, 87,
            60, 1, 1.5D, 6.0D, 0.0F, 0.02D,
            0.0D, 4.0D,
            new double[][] {
                    // age    left      up   forward
                    {29, -1.36D, 1.18D, 1.10D},
                    {32, 1.97D, 3.50D, -0.50D},
                    {35, 0.07D, 2.90D, 3.00D},
                    {36, 0.39D, 0.40D, 1.99D},
                    {38, 0.00D, 1.20D, 3.00D},
                    {40, 0.00D, 1.00D, 3.00D},
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
