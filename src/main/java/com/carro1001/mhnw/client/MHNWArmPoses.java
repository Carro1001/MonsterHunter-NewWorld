package com.carro1001.mhnw.client;

import com.carro1001.mhnw.item.GiantJawbladeItem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.neoforged.neoforge.client.IArmPoseTransformer;

/**
 * The greatsword charge stance: the hunter's arms raise the weapon behind the shoulder as the
 * charge builds, in third person, for every player -- not just the local one.
 *
 * <p><b>No animation library is involved.</b> {@link HumanoidModel.ArmPose} is an extensible enum,
 * and NeoForge's {@link net.neoforged.neoforge.client.extensions.common.IClientItemExtensions#getArmPose}
 * hands a custom one back per item; {@link IArmPoseTransformer} then gets the real
 * {@code HumanoidModel} to pose. {@code docs/WEAPON_POSING.md} section 5 has the full map of what
 * can and cannot be reached this way -- the short version is that the arms can, and the body
 * cannot.
 *
 * <p><b>This class is client-only and must stay unreferenced from common code.</b> Reaching it from
 * a common class's own method body would link {@code HumanoidModel} on a dedicated server and
 * NeoForge's {@code RuntimeDistCleaner} would refuse it. {@link GiantJawbladeItem} names it only
 * from inside an anonymous class, which is a separate class file and so loads only when a client
 * actually runs it -- the same rule, and for the same reason, as
 * {@link GiantJawbladeItem}'s own animation predicate.
 *
 * <p>The enum constant itself is declared in {@code META-INF/enumextensions.json} and reaches the
 * game through {@link #mhnw_greatsword_charge}. FML requires that field's name to start with the
 * mod id, which is why it is spelled in snake case rather than as an ordinary constant.
 */
public final class MHNWArmPoses {

    /**
     * How far back the leading arm swings at a full charge, in radians, on top of the resting item
     * pose. About 64 degrees: a high guard rather than a full overhead wind-up.
     *
     * <p><b>Cut to three fifths of the first pass after playtesting.</b> At 1.85 radians the arm
     * came out past horizontal, the blade swung flat across the screen, and at full charge it left
     * the first-person view entirely -- too wide to read, and wider than a person could hold a
     * greatsword. The clip's own rotations in
     * {@code animations/item/giant_jawblade.animation.json} were scaled by the same three fifths in
     * the same pass, so the arm and the weapon stay in proportion; scale both together or they
     * will disagree.
     *
     * <p>These are meant to be tuned by eye and are gathered here rather than scattered through
     * {@link #pose} so that tuning them is a one-place edit.
     */
    private static final float LEAD_ARM_LIFT = -1.11F;

    /** The off hand comes across onto the grip rather than staying at the hunter's side. */
    private static final float OFF_ARM_LIFT = -1.02F;

    /** A small outward splay, so the two arms do not occupy the same space at full charge. */
    private static final float LEAD_ARM_SPLAY = -0.13F;
    private static final float OFF_ARM_SPLAY = 0.18F;

    /**
     * The extended {@code ArmPose} constant. Two-handed: that is what tells vanilla to pose the off
     * arm as well instead of leaving it swinging.
     */
    public static final EnumProxy<HumanoidModel.ArmPose> mhnw_greatsword_charge =
            new EnumProxy<>(HumanoidModel.ArmPose.class, true,
                    (IArmPoseTransformer) MHNWArmPoses::pose);

    /** Set once if the extension did not take, so the warning is logged a single time, not per frame. */
    private static boolean warnedMissing;

    /**
     * The extended pose, or {@code null} if the enum extension did not take.
     *
     * <p>{@link EnumProxy#getValue()} throws when the constant was never registered, and this is
     * called from the render path -- so an unregistered pose would crash the game on right-click
     * rather than simply look wrong. It did, once: the {@code enumExtensions} key was at the root of
     * {@code neoforge.mods.toml} instead of inside {@code [[mods]]}, where FML actually reads it,
     * and nothing warned. A cosmetic stance must never be able to take rendering down, so the
     * failure degrades to the ordinary item pose and says so once.
     */
    public static HumanoidModel.ArmPose greatswordCharge() {
        try {
            return mhnw_greatsword_charge.getValue();
        } catch (RuntimeException notRegistered) {
            if (!warnedMissing) {
                warnedMissing = true;
                com.mojang.logging.LogUtils.getLogger().error(
                        "MHNW's greatsword ArmPose was never registered, so the charge will not pose"
                                + " the hunter's arms. Check that enumExtensions sits inside [[mods]]"
                                + " in neoforge.mods.toml and that META-INF/enumextensions.json is"
                                + " packaged.", notRegistered);
            }
            return null;
        }
    }

    /**
     * Raise both arms in proportion to the charge.
     *
     * <p>Driven by {@link GiantJawbladeItem#chargeProgress}, which derives from vanilla's own use
     * countdown -- the same number the damage tiers and the weapon's own pose come from, so the
     * stance cannot disagree with the hit. {@link IArmPoseTransformer}'s own javadoc points at
     * {@code getUseItemRemainingTicks()} for exactly this.
     *
     * <p>Called once per arm. Each call poses only the arm it was given, so that a left-handed
     * hunter gets the mirror rather than a copy.
     */
    private static void pose(HumanoidModel<?> model, LivingEntity entity, HumanoidArm arm) {
        ItemStack held = entity.getUseItem();
        float charge = GiantJawbladeItem.chargeProgress(held, entity);
        if (charge <= 0.0F) {
            return;
        }

        boolean leading = arm == entity.getMainArm();
        float lift = (leading ? LEAD_ARM_LIFT : OFF_ARM_LIFT) * charge;
        float splay = (leading ? LEAD_ARM_SPLAY : OFF_ARM_SPLAY) * charge;
        // Mirror the sideways components for a left-handed hunter; the lift is the same either way.
        float mirror = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;

        net.minecraft.client.model.geom.ModelPart limb =
                arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
        limb.xRot = lift;
        limb.yRot = splay * mirror;
        limb.zRot = 0.0F;
    }

    private MHNWArmPoses() {}
}
