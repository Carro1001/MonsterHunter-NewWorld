package com.carro1001.mhnw.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * One bounded flash, from a point in the world. Extracted in R2 because there are now two real
 * callers -- the wild {@link Flashbug} and the thrown {@link FlashBombProjectile} -- and they must
 * agree on eligibility exactly, or the crafted bomb would quietly behave differently from the
 * creature it was made out of.
 *
 * <p>This is a helper, not an effect registry: one static method, four constants, no registration
 * and no per-effect subclassing. What each caller still owns for itself is when it fires, what
 * telegraphs it, and what happens to the source afterwards.
 *
 * <h2>Who is eligible</h2>
 * Alive, not a {@link Player} (a flash that blinds the person playing is a punishing surprise, not
 * a readable hazard), within {@link #RADIUS} blocks of the flash point -- a real distance from the
 * victim's position, not merely inside the box the broad-phase query uses -- with an unobstructed
 * view of it, and looking roughly toward it. Looking away, putting cover in between, or leaving the
 * radius is the counterplay, and all three are things a player can see and do.
 *
 * <p>It never deals damage and never touches blocks. The particles are presentation; this method
 * is the authority.
 */
public final class FlashEffect {

    public static final double RADIUS = 5.0D;
    private static final double RADIUS_SQR = RADIUS * RADIUS;

    /** A startling burst, not a sustained ailment: brief on purpose, for both callers. */
    public static final int DURATION_TICKS = 40;

    /** Movement Slowdown II. Amplifier 1 is level 2, per {@link MobEffectInstance}. */
    public static final int SLOWDOWN_AMPLIFIER = 1;

    /** Dot product of a victim's look vector with the direction to the flash; ~50 degree cone. */
    public static final double FACING_DOT_THRESHOLD = 0.65D;

    private FlashEffect() {}

    /**
     * Blind and slow every eligible target around {@code origin}, once.
     *
     * @param source the flashing entity, excluded from its own flash; may be null
     */
    public static void flash(Level level, Entity source, Vec3 origin) {
        if (level.isClientSide) {
            return;
        }
        // The inflated box is the broad phase only. It is a 10-cube, so its corners reach ~8.7
        // blocks; without the squared-distance guard below, a target on the diagonal at (+4,+4)
        // sits 5.7 blocks away and would still be flashed. The radius is the contract, the box is
        // just the cheap query that feeds it.
        AABB range = new AABB(origin, origin).inflate(RADIUS);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, range,
                e -> e != source && e.isAlive() && !(e instanceof Player))) {
            if (victim.distanceToSqr(origin) > RADIUS_SQR) {
                continue;
            }
            if (!hasLineOfSight(level, origin, victim) || !isFacing(victim, origin)) {
                continue;
            }
            victim.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, DURATION_TICKS, 0));
            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, DURATION_TICKS, SLOWDOWN_AMPLIFIER));
        }
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.FLASH, origin.x, origin.y, origin.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            serverLevel.sendParticles(ParticleTypes.POOF, origin.x, origin.y, origin.z, 12, 0.25D, 0.25D, 0.25D, 0.04D);
        }
    }

    /**
     * Whether nothing solid sits between the flash point and the victim's eyes. Point-based rather
     * than {@code Entity.hasLineOfSight}, because a thrown bomb's flash point is a spot in the air
     * or just off a block face, not an entity that still exists by the time this runs.
     */
    private static boolean hasLineOfSight(Level level, Vec3 origin, LivingEntity victim) {
        return level.clip(new ClipContext(origin, victim.getEyePosition(),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, victim)).getType() == HitResult.Type.MISS;
    }

    /**
     * Whether the victim is facing roughly toward the flash, not merely able to see it. Horizontal
     * (yaw) only, deliberately ignoring pitch: a flash point near the ground sits well below most
     * victims' eye height, so a full 3D look-angle comparison would reject anything standing right
     * next to it and looking straight at it, purely from that vertical offset -- which is not the
     * question this asks.
     */
    private static boolean isFacing(LivingEntity victim, Vec3 origin) {
        Vec3 toFlash = new Vec3(origin.x - victim.getX(), 0.0D, origin.z - victim.getZ());
        if (toFlash.lengthSqr() < 1.0E-6) {
            return true;
        }
        Vec3 look = victim.getLookAngle();
        Vec3 lookFlat = new Vec3(look.x, 0.0D, look.z);
        if (lookFlat.lengthSqr() < 1.0E-6) {
            return false;
        }
        return lookFlat.normalize().dot(toFlash.normalize()) > FACING_DOT_THRESHOLD;
    }
}
