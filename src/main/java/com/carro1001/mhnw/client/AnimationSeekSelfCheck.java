package com.carro1001.mhnw.client;

import com.carro1001.mhnw.MHNW;
import com.carro1001.mhnw.MHNWConfig;
import com.carro1001.mhnw.animation.ServerTimedAnimationController;
import com.carro1001.mhnw.entity.GreatIzuchi;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.keyframe.AnimationPoint;
import software.bernie.geckolib.animation.keyframe.BoneAnimation;
import software.bernie.geckolib.animation.keyframe.BoneAnimationQueue;
import software.bernie.geckolib.animation.state.BoneSnapshot;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.loading.math.MathParser;
import software.bernie.geckolib.loading.math.MolangQueries;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.model.GeoModel;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The R0b acceptance probe for the one thing a headless test cannot reach: whether a late observer
 * genuinely <em>samples</em> the right part of a clip, through GeckoLib's real initialization,
 * transition and keyframe walk, rather than merely computing the right number.
 *
 * <h2>Why this lives on the client and not in {@code MHNWGameTests}</h2>
 * It was written as a GameTest first. It cannot run there: {@link GeoModel} references
 * {@code Minecraft}, so NeoForge's {@code RuntimeDistCleaner} refuses to load it on a dedicated
 * server ("Attempted to load class net/minecraft/client/multiplayer/ClientLevel for invalid dist
 * DEDICATED_SERVER"), which is the same architectural fact the rest of this mod is built around --
 * a dedicated server never runs GeckoLib's animation system at all. So the clock contract is tested
 * headlessly in {@code MHNWGameTests}, and the sampler it drives is checked here, on the only
 * process that can run it.
 *
 * <h2>What it actually does</h2>
 * Nothing synthetic: it loads Great Izuchi's own baked {@code attack_scratch} clip out of the live
 * animation cache and drives two controllers over it.
 * <ul>
 *   <li>an <em>on-time</em> observer, processed every tick from action age 0 to
 *       {@value #JOIN_AGE} -- exactly what a client already watching the fight gets;</li>
 *   <li>a <em>late</em> observer, processed for the very first time at age {@value #JOIN_AGE},
 *       cold: no model, no queue, no current animation. That is the state in which naively advancing
 *       a controller produces a correct age and no clip at all.</li>
 * </ul>
 * Their <em>first</em> comparable frames are then compared -- not the second, not after settling.
 * A stock {@link AnimationController} is run through the identical situation as a control, so a
 * pass cannot be a pass by accident: if unmodified GeckoLib ever samples the right phase on its
 * first frame, this reports that too, and the adapter can be deleted rather than kept.
 *
 * <p>Read-only and self-disabling: it runs once, only while {@code debugCombat} is on, touches no
 * entity, no world and no renderer, and logs one line per check. It is a measuring instrument in
 * the same family as {@link BoneProbe}, not a runtime dependency.
 */
@EventBusSubscriber(modid = MHNW.MOD_ID, value = Dist.CLIENT)
final class AnimationSeekSelfCheck {

    /** Action age the late observer joins at: past the windup, inside the accepted contact window. */
    private static final int JOIN_AGE = 35;

    /** Phase agreement required between the two observers, in ticks (the R0b acceptance target). */
    private static final double PHASE_TOLERANCE_TICKS = 2.0D;

    private static final String CLIP = "animation.great_izuchi.attack_scratch";
    private static final RawAnimation SCRATCH = RawAnimation.begin().thenPlay(CLIP);

    private static boolean done;

    private AnimationSeekSelfCheck() {}

    /**
     * Runs the check once, as soon as the animation cache holds the clip. Called every client tick;
     * everything after the first successful run is a single boolean test.
     */
    @SubscribeEvent
    private static void onClientTick(ClientTickEvent.Post event) {
        maybeRun();
    }

    static void maybeRun() {
        // isLoaded() guards the title screen: a common config value throws if read before its file
        // has been loaded, and this runs from the very first client tick.
        if (done || !MHNWConfig.COMMON_SPEC.isLoaded() || !MHNWConfig.DEBUG_COMBAT.get()) {
            return;
        }
        GeoModel<GreatIzuchi> model = new DefaultedEntityGeoModel<>(
                ResourceLocation.fromNamespaceAndPath(MHNW.MOD_ID, "great_izuchi"));
        Animation clip;
        try {
            // Null animatable on purpose: a DefaultedEntityGeoModel resolves its resources from its
            // own asset id, and nothing on the sampling path reads the animatable for a PLAY_ONCE
            // clip with no keyframe handlers. This keeps the probe independent of a loaded world.
            clip = model.getAnimation(null, CLIP);
        } catch (RuntimeException notLoadedYet) {
            return;
        }
        if (clip == null) {
            return;
        }
        done = true;
        try {
            check(model, clip);
        } catch (RuntimeException | LinkageError failure) {
            MHNW.LOG.error("[anim-selfcheck] FAIL the probe itself threw", failure);
        }
    }

    private static void check(GeoModel<GreatIzuchi> model, Animation clip) {
        Map<String, GeoBone> bones = new LinkedHashMap<>();
        for (BoneAnimation boneAnimation : clip.boneAnimations()) {
            bones.put(boneAnimation.boneName(),
                    new GeoBone(null, boneAnimation.boneName(), false, null, false, false));
        }
        if (bones.isEmpty()) {
            MHNW.LOG.warn("[anim-selfcheck] SKIP {} animates no bones", CLIP);
            return;
        }

        // Three observers over the same real clip. The whole pose is captured each time rather than
        // one bone: which single bone happens to sit on a keyframe boundary at a given clip time is
        // an accident of the authoring, and a comparison that depends on it proves nothing.
        Map<String, AnimationPoint> onTime = runTo(model, bones, JOIN_AGE);
        double onTimeAnimTime = animTime();
        Map<String, AnimationPoint> frameZero = runTo(model, bones, GreatIzuchi.TRANSITION_TICKS);
        double frameZeroAnimTime = animTime();

        ServerTimedAnimationController<GreatIzuchi> late = new ServerTimedAnimationController<>(
                null, "selfcheck_late", GreatIzuchi.TRANSITION_TICKS,
                state -> ServerTimedAnimationController.of(state).playTimed(state, SCRATCH,
                        ServerTimedAnimationController.KIND_ATTACK, 1L, JOIN_AGE));
        Map<String, AnimationPoint> lateFirstFrame = sample(late, model, bones, JOIN_AGE);
        double lateAnimTime = animTime();

        AnimationController<GreatIzuchi> stock = new AnimationController<>(null, "selfcheck_stock",
                GreatIzuchi.TRANSITION_TICKS, state -> state.setAndContinue(SCRATCH));
        Map<String, AnimationPoint> stockFirstFrame = sample(stock, model, bones, JOIN_AGE);

        // Name a bone the clip actually rotates, so the logged lines carry a real pose rather
        // than "no sample" from a bone that only has position keyframes.
        String probe = onTime.isEmpty() ? bones.keySet().iterator().next()
                : onTime.keySet().iterator().next();
        MHNW.LOG.info("[anim-selfcheck] clip={} length={} bones={} joinAge={} transition={}",
                clip.name(), clip.length(), bones.size(), JOIN_AGE, GreatIzuchi.TRANSITION_TICKS);
        MHNW.LOG.info("[anim-selfcheck] on-time   {} {} animTime={}", probe,
                describe(onTime.get(probe)), onTimeAnimTime);
        MHNW.LOG.info("[anim-selfcheck] late      {} {} animTime={}", probe,
                describe(lateFirstFrame.get(probe)), lateAnimTime);
        MHNW.LOG.info("[anim-selfcheck] frame 0   {} {} animTime={}", probe,
                describe(frameZero.get(probe)), frameZeroAnimTime);
        MHNW.LOG.info("[anim-selfcheck] stock     {} {}", probe, describe(stockFirstFrame.get(probe)));

        report("on-time observer produced a pose", !onTime.isEmpty());
        report("the pose at age " + JOIN_AGE + " is distinguishable from the clip's first frame, so"
                        + " the comparisons below can tell a seek from a replay",
                differs(onTime, frameZero));
        report("late observer's FIRST frame produced a pose at all", !lateFirstFrame.isEmpty());
        report("late observer's first frame matches the on-time pose exactly",
                !lateFirstFrame.isEmpty() && !differs(lateFirstFrame, onTime));
        report("late observer did NOT replay the clip's first frame",
                !lateFirstFrame.isEmpty() && differs(lateFirstFrame, frameZero));
        report("query.anim_time agrees between the two observers (" + lateAnimTime + " vs "
                        + onTimeAnimTime + ")",
                Math.abs(lateAnimTime - onTimeAnimTime) <= PHASE_TOLERANCE_TICKS / 20.0D);
        report("control: an unmodified GeckoLib 4.9.2 controller does NOT reach that pose cold, so"
                        + " the comparisons above are meaningful",
                stockFirstFrame.isEmpty() || differs(stockFirstFrame, onTime));
    }

    /** An observer that has been rendering since the action began, driven tick by tick to an age. */
    private static Map<String, AnimationPoint> runTo(GeoModel<GreatIzuchi> model,
                                                     Map<String, GeoBone> bones, int targetAge) {
        int[] age = {0};
        ServerTimedAnimationController<GreatIzuchi> controller = new ServerTimedAnimationController<>(
                null, "selfcheck_ontime_" + targetAge, GreatIzuchi.TRANSITION_TICKS,
                state -> ServerTimedAnimationController.of(state).playTimed(state, SCRATCH,
                        ServerTimedAnimationController.KIND_ATTACK, 1L, age[0]));
        Map<String, AnimationPoint> pose = Map.of();
        for (age[0] = 0; age[0] <= targetAge; age[0]++) {
            pose = sample(controller, model, bones, age[0]);
        }
        return pose;
    }

    /** Whether any bone's sampled rotation differs, which is what "a different pose" means here. */
    private static boolean differs(Map<String, AnimationPoint> left, Map<String, AnimationPoint> right) {
        for (Map.Entry<String, AnimationPoint> entry : left.entrySet()) {
            AnimationPoint other = right.get(entry.getKey());
            AnimationPoint mine = entry.getValue();
            if (other == null
                    || Math.abs(mine.currentTick() - other.currentTick()) > 1.0E-6D
                    || mine.animationStartValue() != other.animationStartValue()
                    || mine.animationEndValue() != other.animationEndValue()) {
                return true;
            }
        }
        return left.size() != right.size();
    }

    private static Map<String, AnimationPoint> sample(AnimationController<GreatIzuchi> controller,
                                                      GeoModel<GreatIzuchi> model,
                                                      Map<String, GeoBone> bones, double seekTime) {
        AnimationState<GreatIzuchi> state = new AnimationState<>(null, 0.0F, 0.0F, 0.0F, false);
        state.withController(controller);
        controller.process(model, state, bones, Map.<String, BoneSnapshot>of(), seekTime, false);
        Map<String, AnimationPoint> pose = new LinkedHashMap<>();
        for (String boneName : bones.keySet()) {
            BoneAnimationQueue queue = controller.getBoneAnimationQueues().get(boneName);
            AnimationPoint point = queue == null ? null : queue.rotationXQueue().poll();
            if (point != null) {
                pose.put(boneName, point);
            }
        }
        return pose;
    }

    private static double animTime() {
        return MathParser.getVariableFor(MolangQueries.ANIM_TIME).get();
    }

    private static String describe(AnimationPoint point) {
        return point == null ? "no sample" : "tick=" + point.currentTick()
                + " start=" + point.animationStartValue() + " end=" + point.animationEndValue();
    }

    private static void report(String what, boolean ok) {
        MHNW.LOG.info("[anim-selfcheck] {} {}", ok ? "PASS" : "FAIL", what);
    }
}
