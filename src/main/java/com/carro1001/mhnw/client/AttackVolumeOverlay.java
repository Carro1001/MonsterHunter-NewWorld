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
 * <p>It deliberately calls {@link GreatIzuchiCombatGoal#attackVolume} rather than recomputing the
 * geometry, so the box drawn here is the box the server hits with; if they ever disagreed, the
 * overlay would be lying about exactly the thing it exists to show. It appears exactly on the ticks
 * that can deal damage and at no other time, so the active window is directly observable and a
 * visible box is always a box that can hit (handoff A08).
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
        // Drawn only while contact is actually being evaluated. Outside the active window the limb
        // path has no value to show: it clamps to its first or last keyframe, so the box would sit
        // frozen somewhere plausible-looking for the rest of the action and read as a volume that
        // is present but inexplicably not hitting anything. Box visible means box can hit.
        int age = entity.getAttackAge();
        if (age < profile.activeStart() || age > profile.activeEnd()) {
            return;
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
        LevelRenderer.renderLineBox(poseStack, lines, volume.move(-ox, -oy, -oz), 1.0F, 0.15F, 0.1F, 1.0F);
    }

    private AttackVolumeOverlay() {}
}
