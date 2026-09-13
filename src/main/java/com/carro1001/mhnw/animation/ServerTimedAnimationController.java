package com.carro1001.mhnw.animation;

import com.carro1001.mhnw.MHNW;
import com.carro1001.mhnw.MHNWConfig;
import net.minecraft.world.entity.Entity;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.state.BoneSnapshot;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

import java.util.Map;

/**
 * Plays a finite, server-timed clip at its <em>real</em> age, so a client that starts rendering a
 * monster part way through an attack, a roar or a death sees the current phase rather than the clip
 * restarting from frame zero while the server is already landing the hit (R0b).
 *
 * <h2>Why this exists at all</h2>
 * The action identity ({@code attackId}/{@code startTime}/{@code sequence}, the roar countdown, the
 * death anchor) has always been synched entity data, so a late client already learned <em>which</em>
 * clip was playing. What it could not do was start that clip anywhere but its beginning: GeckoLib
 * 4.9.2 has no public seek. Presentation therefore led contact by up to a clip length for exactly
 * the observers who joined mid-fight.
 *
 * <h2>What GeckoLib 4.9.2 actually offers</h2>
 * Verified against the published sources jar, SHA-256
 * {@code 009055c5d7b848a8bed826ed8887936d85c5f711d0c19f0fa2547950db99ee41}. There is no
 * {@code seek}/{@code setAnimationTick}; {@code forceAnimationReset()} requests a <em>reload</em>,
 * not a seek; and a speed modifier multiplies elapsed time rather than moving it (speed zero
 * collapses sampled time to zero instead of freezing the pose). The one real lever is
 * {@code adjustTick}:
 * <pre>{@code clipTime = speed * max(seekTime - tickOffset, 0)}</pre>
 * {@code tickOffset} is {@code protected}, so a subclass can move the clip's origin. That is the
 * whole mechanism here: nothing is reflected into, no private field is touched, no processor is
 * copied, and the library still performs every keyframe and MoLang evaluation itself.
 *
 * <h2>The convention, and why it does not move contact timing</h2>
 * A controller with transition length {@code L} spends its first {@code L} ticks blending into a
 * newly set clip and only then starts the clip at time zero, so an <em>ordinary, on-time</em>
 * observer has always shown {@code clipTime = age - L}. This class reproduces exactly that function
 * for every observer instead of inventing a new one:
 * <pre>{@code controllerAge == age;  clipTime == max(age - L, 0), clamped below the clip length}</pre>
 * An on-time observer's controller already satisfies that within a tick, so it is never touched --
 * the measured attack paths, the active windows and the accepted contact frames are unchanged by
 * construction rather than by promise. Only an observer whose controller disagrees is moved, and it
 * is moved onto the same curve the on-time one is already on.
 *
 * <h2>How a late entry is actually sampled</h2>
 * A cold controller cannot simply be started at a nonzero time: {@code setAnimation} only builds a
 * queue once {@code lastModel} exists, and {@code process} only polls that queue while the adjusted
 * tick is zero (or on the manager's first tick). Advancing it first leaves it TRANSITIONING with no
 * {@code currentAnimation} -- a correct number and no clip. So each frame runs GeckoLib's own
 * initialization first, and only then, if the result sits at the wrong age, re-anchors and lets
 * GeckoLib sample again within the same render call. The second pass clears and rewrites the bone
 * queues, so the first pass's zero-time output never reaches the model. In steady state the second
 * pass does not happen at all: once anchored the controller advances one tick per tick and stays on
 * the curve, so the check costs one subtraction per frame.
 *
 * <h2>Terminal hold</h2>
 * The desired age is clamped just below {@code L + clipLength}. That is deliberate and does two
 * things the library would not: an expired one-shot holds its final pose instead of collapsing to
 * the base pose (which is what {@code State.STOPPED} amounts to once the bone queues stop being
 * written), and a {@code hold_on_last_frame} clip stops advancing {@code query.anim_time}, so
 * MoLang-driven motion in a held death pose does not keep evolving forever.
 *
 * <h2>Boundaries</h2>
 * Common-loadable on purpose -- it is named from common entity registration, so it imports nothing
 * from {@code net.minecraft.client} and never touches {@code Minecraft} or {@code MHNWClient}. It is
 * presentation only: it applies no damage, positions no {@code MonsterPart}, picks no targets and
 * changes no server timing. Its clock is the entity's own synched action identity, held per
 * controller, so two of the same species mid-different-actions cannot contaminate each other even
 * though GeckoLib's {@code seekTime} is a counter shared by every instance of a model.
 */
public class ServerTimedAnimationController<T extends GeoAnimatable> extends AnimationController<T> {

    /** Kinds of finite presentation, in the priority order the state handlers apply them. */
    public static final int KIND_NONE = 0;
    public static final int KIND_DEATH = 1;
    public static final int KIND_ROAR = 2;
    public static final int KIND_ATTACK = 3;

    /** Small Izuchi's rally call. Its own kind rather than reusing {@link #KIND_ROAR}: the two are
     * different clips on the same species, and the kind is what stops one instance identifier being
     * mistaken for the other's. */
    public static final int KIND_RALLY = 4;

    /**
     * How far the controller may sit from its authoritative age before being re-anchored, in ticks.
     * One tick: below the handoff's two-tick acceptance target, and comfortably above the float
     * noise of a partial-tick-interpolated comparison, so a correctly tracking controller is never
     * re-anchored and a genuinely late one always is.
     */
    private static final double TOLERANCE_TICKS = 1.0D;

    /** Keeps the final sample strictly inside the clip so GeckoLib never takes its end branch. */
    private static final double END_EPSILON = 0.001D;

    private int requestedKind = KIND_NONE;
    private long requestedInstance;
    private double requestedAge;

    private int presentedKind = KIND_NONE;
    private long presentedInstance;
    /** Whether this instance has already been logged once, so diagnostics stay bounded. */
    private boolean loggedInstance;

    public ServerTimedAnimationController(T animatable, String name, int transitionTickTime,
                                          AnimationStateHandler<T> handler) {
        super(animatable, name, transitionTickTime, handler);
    }

    /** Convenience cast for a state handler that knows it registered one of these. */
    @SuppressWarnings("unchecked")
    public static <T extends GeoAnimatable> ServerTimedAnimationController<T> of(AnimationState<T> state) {
        return (ServerTimedAnimationController<T>) state.getController();
    }

    /**
     * Play a finite clip whose timing the server owns.
     *
     * <p>Call this from the animation state handler, from the <em>same</em> priority decision that
     * chose the clip: the kind, the instance and the clip must agree, or a death pose can end up
     * driven by an attack clock.
     *
     * @param animation the clip to present
     * @param kind      one of the {@code KIND_} constants; distinguishes a death from a roar whose
     *                  instance identifiers happen to be equal numbers
     * @param instance  identifies this occurrence of that kind -- an action sequence, or the game
     *                  time the roar/death began. A repeat of the same attack is a new instance; a
     *                  fresh controller or a resource reload is not
     * @param ageTicks  ticks since the server began it, partial tick included
     */
    public PlayState playTimed(AnimationState<T> state, RawAnimation animation,
                               int kind, long instance, double ageTicks) {
        this.requestedKind = kind;
        this.requestedInstance = instance;
        this.requestedAge = Math.max(ageTicks, 0.0D);

        if (this.presentedKind != kind || this.presentedInstance != instance) {
            this.presentedKind = kind;
            this.presentedInstance = instance;
            this.loggedInstance = false;
            // A genuinely new occurrence: make GeckoLib rebuild the queue so a repeat of the same
            // clip replays rather than continuing. The age correction below then places it.
            forceAnimationReset();
        }
        return state.setAndContinue(animation);
    }

    /** Ordinary idle/locomotion: hand playback back to GeckoLib's own clock, unchanged. */
    public PlayState playFree(AnimationState<T> state, RawAnimation animation) {
        this.requestedKind = KIND_NONE;
        this.presentedKind = KIND_NONE;
        return state.setAndContinue(animation);
    }

    @Override
    public void process(GeoModel<T> model, AnimationState<T> state, Map<String, GeoBone> bones,
                        Map<String, BoneSnapshot> snapshots, double seekTime,
                        boolean crashWhenCantFindBone) {
        this.requestedKind = KIND_NONE;

        // Pass one: GeckoLib's normal frame. This is what calls the state handler (and therefore
        // playTimed/playFree), builds the animation queue, polls the current animation on a fresh
        // or reset controller, and completes a transition. Nothing below can run before it.
        super.process(model, state, bones, snapshots, seekTime, crashWhenCantFindBone);

        if (this.requestedKind == KIND_NONE || getCurrentAnimation() == null) {
            return;
        }

        double clipLength = getCurrentAnimation().animation().length();
        double desired = desiredAge(this.requestedAge, this.transitionLength, clipLength);
        double speed = getAnimationSpeed();
        if (speed <= 0.0D) {
            return; // No meaningful clip time to seek to; leave the library's own behaviour alone.
        }

        // A still-set shouldResetTick means pass one has queued a restart for the next frame (a
        // resource reload, a forced reset); re-anchor rather than let it land back on frame zero.
        double actual = controllerAge(seekTime, speed);
        if (!this.shouldResetTick && Math.abs(actual - desired) <= TOLERANCE_TICKS) {
            return;
        }

        seekTo(seekTime, clipLength, speed);
        // Pass two: the real sample. super.process clears and rewrites the bone queues, evaluates
        // the keyframes at the seeked time and sets query.anim_time from it, so this is GeckoLib's
        // own sampler running at the corrected age -- not a number that was logged and hoped for.
        super.process(model, state, bones, snapshots, seekTime, crashWhenCantFindBone);

        if (!this.loggedInstance && MHNWConfig.DEBUG_COMBAT.get()) {
            this.loggedInstance = true;
            MHNW.LOG.info("[anim] seek id={} uuid={} kind={} instance={} age={} wasAge={} -> clip={}/{} state={}",
                    this.animatable instanceof Entity entity ? entity.getId() : -1,
                    this.animatable instanceof Entity entity ? entity.getUUID() : "?",
                    this.requestedKind, this.requestedInstance,
                    fmt(this.requestedAge), fmt(actual),
                    fmt(controllerAge(seekTime, speed) - this.transitionLength), fmt(clipLength),
                    getAnimationState());
        }
    }

    private static String fmt(double value) {
        return String.format("%.2f", value);
    }

    /**
     * Time since this controller began presenting the current instance: the blend counts, because
     * the server's action age counts it too (see the class doc's convention).
     */
    private double controllerAge(double seekTime, double speed) {
        double clipTime = speed * Math.max(seekTime - this.tickOffset, 0.0D);
        return getAnimationState() == State.TRANSITIONING ? clipTime : this.transitionLength + clipTime;
    }

    /**
     * Move the clip's origin so the next sample lands at the requested action age.
     *
     * <p>The arithmetic itself lives in {@link #controllerTickFor}, which is also what the GameTest
     * asserts -- deliberately, so the tested formula and the one actually used at runtime cannot
     * drift apart. An earlier version duplicated the subtraction here and a change to this copy
     * would have left every test green.
     */
    private void seekTo(double seekTime, double clipLength, double speed) {
        // Still inside the initial blend means the controller's own tick is blend progress, not clip
        // time, so GeckoLib has to stay in TRANSITIONING for it to be interpreted that way.
        boolean blending = desiredAge(this.requestedAge, this.transitionLength, clipLength)
                < this.transitionLength;
        this.animationState = blending ? State.TRANSITIONING : State.RUNNING;
        this.shouldResetTick = false;
        this.tickOffset = seekTime
                - controllerTickFor(this.requestedAge, this.transitionLength, clipLength) / speed;
    }

    // ------------------------------------------------------------------ the clock contract

    /**
     * The tick this controller is driven to for a given action age -- the whole timing contract, in
     * one place, called by {@link #seekTo} at runtime and asserted directly by a GameTest on a
     * dedicated server where no controller, model or bone exists.
     *
     * <p>Two regimes, because GeckoLib's adjusted tick means two different things. While the initial
     * blend is still running it is progress through that blend, so the answer is the action age
     * itself. Afterwards it is time into the clip, so the answer is the age minus the blend -- which
     * is exactly what an ordinary on-time observer has always been shown. Past the end it is held
     * just inside the clip rather than looping to zero or running off it.
     */
    public static double controllerTickFor(double ageTicks, double transitionLength, double clipLength) {
        double desired = desiredAge(ageTicks, transitionLength, clipLength);
        return desired < transitionLength ? desired : desired - transitionLength;
    }

    /** Action age the controller is driven to: the request, floored at zero and held inside the clip. */
    private static double desiredAge(double ageTicks, double transitionLength, double clipLength) {
        return Math.min(Math.max(ageTicks, 0.0D),
                Math.max(transitionLength + clipLength - END_EPSILON, 0.0D));
    }
}
