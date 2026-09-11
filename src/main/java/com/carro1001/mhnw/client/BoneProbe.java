package com.carro1001.mhnw.client;

import com.carro1001.mhnw.MHNW;
import com.carro1001.mhnw.MHNWConfig;
import com.carro1001.mhnw.entity.GreatIzuchi;
import org.joml.Vector3d;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

/**
 * Development-only measuring tape.
 *
 * <p>Fitting hurtboxes by solving the geometry offline got the torso and tail right but left the
 * head visibly off, and no rotation convention explained the gap while keeping the torso and feet
 * correct. Rather than guess again from screenshots, this prints where GeckoLib actually places
 * each bone at runtime, expressed in the same local (left, up, forward) frame the part offsets
 * use, so the constants can be read straight off the log.
 *
 * <p>This is a measurement aid, not a runtime dependency. No gameplay decision reads it: the
 * server never asks a client where a bone is (handoff section 4.2). It runs only when
 * {@code debugCombat} is enabled, only on the client, and only every other second.
 */
final class BoneProbe {

    private static final String[] BONES = {
            "head", "snout", "jaw", "headHitbox",
            "neck", "torso", "body", "torsoHitbox",
            "tail1", "tail2", "tailblade",
            "baseTailHitbox", "midTailHitbox", "tailEndHitbox",
            "right_hand", "left_hand", "clawHitbox",
            "left_foot", "right_foot",
    };

    private static final int IDLE_INTERVAL_TICKS = 40;

    /** Guards against logging the same tick once per rendered frame. */
    private static int lastLoggedTick = Integer.MIN_VALUE;

    static void maybeLog(GreatIzuchi entity, GeoModel<GreatIzuchi> model) {
        if (!MHNWConfig.DEBUG_COMBAT.get()) {
            return;
        }
        // Every tick during an action, so one attack yields the complete swing rather than the
        // scattered samples a fixed interval produces; sparsely otherwise.
        boolean attacking = entity.getAttackId() != GreatIzuchi.ATTACK_NONE;
        if (!attacking && entity.tickCount % IDLE_INTERVAL_TICKS != 0) {
            return;
        }
        if (entity.tickCount == lastLoggedTick) {
            return;
        }
        lastLoggedTick = entity.tickCount;

        double rad = Math.toRadians(entity.yBodyRot);
        double sin = Math.sin(rad);
        double cos = Math.cos(rad);

        StringBuilder out = new StringBuilder("bone probe (local left/up/forward, blocks) attackId=")
                .append(entity.getAttackId()).append(" age=").append(entity.getAttackAge());

        for (String name : BONES) {
            GeoBone bone = model.getBone(name).orElse(null);
            if (bone == null) {
                continue;
            }
            Vector3d world = bone.getWorldPosition();
            double dx = world.x - entity.getX();
            double dy = world.y - entity.getY();
            double dz = world.z - entity.getZ();
            // Inverse of GreatIzuchi.localToWorld.
            double left = dx * cos + dz * sin;
            double forward = -dx * sin + dz * cos;
            out.append(String.format("%n    %-16s left=%6.2f up=%6.2f fwd=%6.2f", name, left, dy, forward));
        }
        MHNW.LOG.info("[great_izuchi] {}", out);
    }

    private BoneProbe() {}
}
