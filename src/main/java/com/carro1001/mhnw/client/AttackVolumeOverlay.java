package com.carro1001.mhnw.client;

import com.carro1001.mhnw.MHNWConfig;
import com.carro1001.mhnw.entity.GreatIzuchi;
import com.carro1001.mhnw.entity.AttackProfile;
import com.carro1001.mhnw.entity.GreatIzuchiCombatGoal;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.AABB;

/**
 * Draws the attack volume, which F3+B cannot show.
 *
 * <p>F3+B already outlines the root envelope and every hurtbox, because those are real entities.
 * The claw volume is not an entity: it is a body-local path evaluated for the current action age.
 * Without this, the one thing you cannot see is the thing that decides whether an attack connects.
 *
 * <p>It deliberately calls {@link GreatIzuchiCombatGoal#clawVolume} rather than recomputing the
 * geometry, so the box drawn here is the box the server hits with; if they ever disagreed, the
 * overlay would be lying about exactly the thing it exists to show. The colour is the phase, which
 * makes the windup/active/recovery boundaries directly observable (handoff A08):
 *
 * <pre>
 *   yellow  windup    telegraphing, no contact is evaluated
 *   red     active    the only ticks that can hit
 *   blue    recovery  committed, no contact
 * </pre>
 *
 * <p>Presentation only. It reads server-driven state and never feeds anything back.
 */
final class AttackVolumeOverlay {

    static void render(GreatIzuchi entity, PoseStack poseStack, MultiBufferSource bufferSource,
                       float partialTick) {
        if (!MHNWConfig.DEBUG_COMBAT.get() || entity.getAttackId() == GreatIzuchi.ATTACK_NONE) {
            return;
        }
        AttackProfile profile = AttackProfile.byId(entity.getAttackId());
        if (profile == null) {
            return;
        }
        int age = entity.getAttackAge();
        if (age < 0 || age > profile.actionEnd()) {
            return;
        }

        float r;
        float g;
        float b;
        if (age < profile.activeStart()) {
            r = 1.0F;
            g = 0.85F;
            b = 0.1F;
        } else if (age <= profile.activeEnd()) {
            r = 1.0F;
            g = 0.15F;
            b = 0.1F;
        } else {
            r = 0.3F;
            g = 0.6F;
            b = 1.0F;
        }

        AABB volume = GreatIzuchiCombatGoal.attackVolume(entity, age);
        if (volume == null) {
            return;
        }

        // The pose stack arrives translated to the entity's interpolated position, unrotated, so
        // the world-space volume is rebased onto that origin.
        double ox = net.minecraft.util.Mth.lerp(partialTick, entity.xOld, entity.getX());
        double oy = net.minecraft.util.Mth.lerp(partialTick, entity.yOld, entity.getY());
        double oz = net.minecraft.util.Mth.lerp(partialTick, entity.zOld, entity.getZ());

        VertexConsumer lines = bufferSource.getBuffer(RenderType.lines());
        LevelRenderer.renderLineBox(poseStack, lines, volume.move(-ox, -oy, -oz), r, g, b, 1.0F);
    }

    private AttackVolumeOverlay() {}
}
