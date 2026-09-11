package com.carro1001.mhnw.client;

import com.carro1001.mhnw.MHNW;
import com.carro1001.mhnw.MHNWConfig;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector3d;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

/**
 * Development-only measuring tape.
 *
 * <p>Fitting hurtboxes by solving the geometry offline got Great Izuchi's torso and tail right but
 * left the head visibly off, and no rotation convention explained the gap while keeping the torso
 * and feet correct. Rather than guess again from screenshots, this prints where GeckoLib actually
 * places each named bone at runtime, expressed in the same local (left, up, forward) frame every
 * species' {@code localToWorld} uses, so hurtbox constants can be read straight off the log instead
 * of solved offline or eyeballed from a screenshot.
 *
 * <p>Generic over any {@link LivingEntity} using the {@code yBodyRot}-based local frame convention
 * every multipart species here shares (see {@code GreatIzuchi}/{@code Rathian}/{@code Rathalos}/
 * {@code Aptonoth}'s own {@code localToWorld}): originally hardcoded to Great Izuchi only, widened
 * once Rathian/Rathalos's own offline-solved hurtboxes were confirmed wrong by screenshots and a
 * second species needed the same measurement loop.
 *
 * <p>This is a measurement aid, not a runtime dependency. No gameplay decision reads it: the
 * server never asks a client where a bone is (handoff section 4.2). It runs only when
 * {@code debugCombat} is enabled, only on the client, and only every other second (or every tick
 * while {@code activeNow} is true, for species that pass one — see {@link #maybeLog}).
 */
final class BoneProbe {

    /** Bones worth reading for Great Izuchi's own hurtbox/attack-volume work. */
    static final String[] GREAT_IZUCHI_BONES = {
            "head", "snout", "jaw", "headHitbox",
            "neck", "torso", "body", "torsoHitbox",
            "tail1", "tail2", "tailblade",
            "baseTailHitbox", "midTailHitbox", "tailEndHitbox",
            "right_hand", "left_hand", "clawHitbox",
            "left_foot", "right_foot",
    };

    /** Rathian's and Rathalos's own hitbox bones plus their unsuffixed neighbours, for comparison;
     * both species share this skeleton layout (see their .geo.json files). */
    static final String[] WYVERN_BONES = {
            "Torso", "torsoHitbox", "Neck1", "Neck2", "neckHitbox", "Head", "headHitbox", "Jaw",
            "tail1", "baseTailHitbox", "tail2", "midTailHitbox", "tailclub", "tailEndHitbox",
            "stingerbase", "stingerHitbox", "hips",
    };

    /** Aptonoth has no dedicated hitbox bones; these are the base skeleton bones its four
     * MonsterPart offsets are meant to track. */
    static final String[] APTONOTH_BONES = {
            "body", "neck", "head", "hips", "tail1", "tail2",
    };

    /** P5a fixed hurtboxes await a live land/swim range-midpoint fitting pass. */
    static final String[] LAGIACRUS_BONES = {
            "jawHitbox", "neckMidHitbox", "neckBaseHitbox", "tailBaseHitbox",
            "tailMidHitbox", "tailLastHitbox", "tailEndHitbox",
    };

    private static final int IDLE_INTERVAL_TICKS = 40;

    /** Guards against logging the same tick twice for the same species in one rendered frame. */
    private static int lastLoggedTick = Integer.MIN_VALUE;
    private static String lastLoggedTag = null;

    /**
     * @param tag       short species label for the log line, e.g. {@code "rathian"}
     * @param bones     bone names to look up in {@code model}; missing ones are silently skipped,
     *                  so one list can be reused across species with slightly different skeletons
     * @param activeNow whether this species is in a state worth sampling every tick right now (an
     *                  attack mid-swing, say); pass a constant {@code false} for a species with no
     *                  such notion of "active" and it will just log sparsely on the idle interval
     */
    static <T extends LivingEntity & GeoAnimatable> void maybeLog(String tag, T entity, GeoModel<T> model,
                                                                    String[] bones, boolean activeNow) {
        if (!MHNWConfig.DEBUG_COMBAT.get()) {
            return;
        }
        if (!activeNow && entity.tickCount % IDLE_INTERVAL_TICKS != 0) {
            return;
        }
        // Different species render in the same frame; the tick-dedup guard must be per-tag or the
        // second species logged each frame would be silently dropped as a false "already logged".
        if (entity.tickCount == lastLoggedTick && tag.equals(lastLoggedTag)) {
            return;
        }
        lastLoggedTick = entity.tickCount;
        lastLoggedTag = tag;

        double rad = Math.toRadians(entity.yBodyRot);
        double sin = Math.sin(rad);
        double cos = Math.cos(rad);

        StringBuilder out = new StringBuilder("bone probe (local left/up/forward, blocks)");

        for (String name : bones) {
            GeoBone bone = model.getBone(name).orElse(null);
            if (bone == null) {
                continue;
            }
            Vector3d world = bone.getWorldPosition();
            double dx = world.x - entity.getX();
            double dy = world.y - entity.getY();
            double dz = world.z - entity.getZ();
            // Inverse of every species' own localToWorld (left/up/forward -> world).
            double left = dx * cos + dz * sin;
            double forward = -dx * sin + dz * cos;
            out.append(String.format("%n    %-16s left=%6.2f up=%6.2f fwd=%6.2f", name, left, dy, forward));
        }
        MHNW.LOG.info("[{}] {}", tag, out);
    }

    private BoneProbe() {}
}
