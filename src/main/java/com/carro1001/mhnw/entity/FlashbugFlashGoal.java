package com.carro1001.mhnw.entity;

/**
 * A flashbug's whole behaviour: notice something close or being hurt, flare for well under a
 * second, then blind whatever can actually see the flash, once, and go dark for a while. See
 * {@link EndemicAreaEffectGoal} for the shared trigger/telegraph/cooldown machinery.
 *
 * <p>Since R2 the effect itself lives in {@link FlashEffect}, shared verbatim with the thrown
 * flash bomb a bottled flashbug crafts into: same five-block radius, same line-of-sight and facing
 * rules, same players-never-affected exclusion, and the same 40 ticks of Blindness — now paired
 * with a bounded Movement Slowdown II, so a crafted bomb and the creature it came from cannot drift
 * apart. Nothing about <em>this</em> species' trigger changed: still hit-only, still an 11-tick
 * telegraph, still one release and then gone.
 *
 * <p>Like the toad, this only ever triggers from being hit/interacted with, never from mere
 * proximity ({@link #allowsProximityTrigger} overridden false): the default proximity trigger with
 * a short fuse and a 5-second cooldown meant anything simply standing near it saw it flash over and
 * over the whole time, which read as constant flashing rather than a deliberate reaction. And, also
 * like the toad, releasing now ends the flashbug's life: a smoke-poof burst and
 * {@link Flashbug#discard()}, not a cooldown it recovers from.
 */
public class FlashbugFlashGoal extends EndemicAreaEffectGoal {

    private static final double TRIGGER_RANGE = 4.0D;
    /** Matches the 0.54 s one-shot {@code flashfly.fly} clip, rounded up. */
    private static final int FUSE_TICKS = 11;
    private static final int COOLDOWN_TICKS = 100;

    private final Flashbug flashbug;

    public FlashbugFlashGoal(Flashbug flashbug) {
        super(flashbug, TRIGGER_RANGE, FUSE_TICKS, COOLDOWN_TICKS);
        this.flashbug = flashbug;
    }

    @Override
    protected boolean isProvoked() {
        return this.flashbug.provoked;
    }

    @Override
    protected void clearProvoked() {
        this.flashbug.provoked = false;
    }

    @Override
    protected void setPresenting(boolean presenting) {
        this.flashbug.setFlashing(presenting);
    }

    /** Only a hit provokes a flashbug now; proximity alone must not (see the class doc). */
    @Override
    protected boolean allowsProximityTrigger() {
        return false;
    }

    @Override
    protected void release() {
        FlashEffect.flash(this.flashbug.level(), this.flashbug, this.flashbug.getEyePosition());
        this.flashbug.discard();
    }
}
