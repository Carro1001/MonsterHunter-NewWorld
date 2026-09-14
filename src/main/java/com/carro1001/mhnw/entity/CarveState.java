package com.carro1001.mhnw.entity;

import com.carro1001.mhnw.registry.ModItems;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The R1 carving contract, in one place, for the three carvable species.
 *
 * <p>Composition rather than a base class, because {@link GreatIzuchi} is a {@code Monster} and
 * {@link Aptonoth} is an {@code Animal} and Java only allows one superclass. Each owner holds one
 * of these, forwards five things to it -- damage, interaction, death, save and load -- and keeps
 * everything else. There is no corpse entity, no capability and no loot service: three fixed
 * consumers do not need an extension point.
 *
 * <h2>Who may carve</h2>
 * Only a player whose own accepted hit actually reduced this creature's health, their projectile,
 * or their owned/tamed attacker. Standing nearby, being targeted, healing it, or an ownerless
 * environmental death grant nothing. Credit is recorded on the parent while it is still alive and
 * persists with it, so a chunk unload between the first hit and the kill does not erase who fought.
 *
 * <h2>How long the corpse lasts, and why there is no corpse counter in NBT</h2>
 * The window is 12,000 ticks of <em>this entity ticking</em>, which is exactly what vanilla's own
 * {@code deathTime} already counts: it advances in {@code tickDeath}, which only runs when the
 * entity is ticked, so an unloaded chunk or a stopped server pauses it for free, and vanilla
 * already saves and restores it as {@code DeathTime}. Holding the body means nothing more than
 * declining to call {@code super.tickDeath()} until that counter reaches the window. A second saved
 * counter of our own would be the same number written twice, with two chances to disagree. It fits:
 * vanilla stores {@code DeathTime} as a short, and 12,000 is well under 32,767.
 *
 * <p>R0b's death presentation reads that same {@code deathTime} to reconstruct its animation
 * anchor, so holding the body keeps the death clip holding its last authored frame rather than
 * replaying -- which is what {@code thenPlayAndHold} already does. Nothing here calls {@code die()},
 * drops loot or grants experience: those happen once, at the real death, through vanilla.
 *
 * <h2>Why the rewards are a table and not a roll</h2>
 * The next reward is a pure function of how many carves that player has already taken. A retry
 * after a full inventory, or a reload mid-corpse, therefore reproduces the identical stack with no
 * saved pending-roll state to get out of step. R1 ships reliable basics; a rarity roll is a later
 * product decision, not a missing subsystem.
 */
public final class CarveState {

    /** Entity-ticking ticks a carvable corpse remains before its one final removal. */
    public static final int CORPSE_TICKS = 12_000;

    /** Personal carves per eligible player, per corpse. */
    public static final int MAX_CARVES = 3;

    /** Per-player interaction debounce, in real ticks. Transient: it is spam control, not a rule. */
    public static final int DEBOUNCE_TICKS = 10;

    /**
     * How far a player may be from the corpse's origin to carve it. Generous on purpose -- a Great
     * Izuchi is longer than a player's reach, and a carve through the tail part is legitimate.
     * Vanilla's own packet handler already enforces the real reach limit before this is ever
     * reached; this is the backstop that makes the rule true for any caller.
     */
    public static final double CARVE_RANGE = 10.0D;
    private static final double CARVE_RANGE_SQR = CARVE_RANGE * CARVE_RANGE;

    /** The deterministic per-species reward sequence. */
    public enum Table {
        GREAT_IZUCHI,
        IZUCHI,
        APTONOTH;

        /** The stack for a player's {@code carveIndex}-th carve, counting from zero. */
        public ItemStack reward(int carveIndex) {
            return switch (this) {
                case GREAT_IZUCHI -> switch (carveIndex) {
                    case 0 -> new ItemStack(ModItems.MONSTER_HIDE.get(), 4);
                    case 1 -> new ItemStack(ModItems.MONSTER_CLAW.get(), 2);
                    default -> new ItemStack(Items.BONE, 4);
                };
                case IZUCHI -> switch (carveIndex) {
                    case 0 -> new ItemStack(ModItems.MONSTER_HIDE.get(), 1);
                    case 1 -> new ItemStack(ModItems.MONSTER_CLAW.get(), 1);
                    default -> new ItemStack(Items.BONE, 1);
                };
                case APTONOTH -> switch (carveIndex) {
                    case 1 -> new ItemStack(ModItems.MONSTER_HIDE.get(), 2);
                    // The habitat's own bone source. Bone armor's other two materials are Izuchi
                    // carves, so the first hunt is the gate; bone itself should not also be a trip
                    // back to a skeleton somewhere else.
                    case 2 -> new ItemStack(Items.BONE, 2);
                    default -> new ItemStack(ModItems.RAW_MEAT.get(), 2);
                };
            };
        }
    }

    private static final String TAG_ROOT = "MhnwCarve";
    private static final String TAG_PARTICIPANTS = "Participants";
    private static final String TAG_CARVES = "Carves";
    private static final String TAG_PLAYER = "Player";
    private static final String TAG_USED = "Used";

    private final Table table;
    private final Set<UUID> participants = new LinkedHashSet<>();
    private final Map<UUID, Integer> carves = new LinkedHashMap<>();
    /** Transient: parent tick counts, purely to stop a held right-click spamming the action bar. */
    private final Map<UUID, Integer> lastInteraction = new LinkedHashMap<>();

    public CarveState(Table table) {
        this.table = table;
    }

    // ---------------------------------------------------------------- attribution

    /**
     * Record a participant, if this damage was both accepted and actually harmful.
     *
     * <p>Called by the owner from its own {@code hurt} override, after {@code super.hurt} has
     * returned and only when health genuinely fell. That ordering is the whole point: a hit
     * rejected by invulnerability, absorbed entirely, or duplicated across several hurtboxes in one
     * tick never reaches here, so it never grants eligibility.
     */
    public void creditDamage(DamageSource source) {
        Player player = resolvePlayer(source);
        if (player != null) {
            this.participants.add(player.getUUID());
        }
    }

    /**
     * The player behind a damage source, or null. Direct hit first, then an owned attacker whose
     * owner actually resolves to a player. A projectile needs no case of its own: vanilla already
     * reports the shooter as the source's causing entity. Nothing here guesses from a name, the
     * nearest player or the last target.
     *
     * <p>Package-visible rather than private since R2, so {@link Toad}'s provoker attribution can
     * ask the identical question instead of inventing a second, subtly different rule.
     */
    static Player resolvePlayer(DamageSource source) {
        Entity attacker = source.getEntity();
        if (attacker instanceof Player player) {
            return player;
        }
        if (attacker instanceof OwnableEntity owned && owned.getOwner() instanceof Player owner) {
            return owner;
        }
        return null;
    }

    public boolean isParticipant(UUID player) {
        return this.participants.contains(player);
    }

    public int participantCount() {
        return this.participants.size();
    }

    public int carvesUsedBy(UUID player) {
        return this.carves.getOrDefault(player, 0);
    }

    public int carvesRemainingFor(UUID player) {
        return MAX_CARVES - carvesUsedBy(player);
    }

    // ---------------------------------------------------------------- interaction

    /**
     * Shift + right-click with the main hand on a corpse: one carve, server-authoritative.
     *
     * <p>Every gate is checked here rather than split between the caller and this class, so all
     * three species enforce the identical rule and one test path drives all of them.
     */
    public InteractionResult interact(Mob parent, Player player, InteractionHand hand) {
        if (parent.isAlive() || hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (parent.level().isClientSide) {
            // Let the client swallow the click so it does not also swing the held item. Every
            // decision that matters is made below, on the server.
            return player.isShiftKeyDown() ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        if (player.distanceToSqr(parent) > CARVE_RANGE_SQR) {
            return InteractionResult.PASS;
        }

        UUID id = player.getUUID();
        Integer last = this.lastInteraction.get(id);
        if (last != null && parent.tickCount - last < DEBOUNCE_TICKS) {
            return InteractionResult.CONSUME;
        }
        this.lastInteraction.put(id, parent.tickCount);

        if (!player.isShiftKeyDown()) {
            return say(player, "message.mhnw.carve.hint");
        }
        if (!this.participants.contains(id)) {
            return say(player, "message.mhnw.carve.ineligible");
        }
        int used = carvesUsedBy(id);
        if (used >= MAX_CARVES) {
            return say(player, "message.mhnw.carve.exhausted");
        }

        ItemStack reward = this.table.reward(used);
        if (!fits(player.getInventory(), reward)) {
            // Neither the carve nor its result is consumed: the same stack is waiting on retry.
            return say(player, "message.mhnw.carve.full");
        }
        player.getInventory().add(reward);
        this.carves.put(id, used + 1);
        player.displayClientMessage(Component.translatable("message.mhnw.carve.success",
                MAX_CARVES - used - 1), true);
        return InteractionResult.CONSUME;
    }

    private static InteractionResult say(Player player, String key) {
        player.displayClientMessage(Component.translatable(key), true);
        return InteractionResult.CONSUME;
    }

    /**
     * Whether the whole stack would fit, without inserting any of it.
     *
     * <p>{@code Inventory.add} inserts what it can and leaves the rest, which is the wrong shape
     * for an all-or-nothing grant: a half-inserted reward would already have changed the inventory
     * by the time we learned it did not fit. So the capacity is counted first, over the same 36
     * main slots that {@code add} itself uses.
     */
    static boolean fits(Inventory inventory, ItemStack stack) {
        int remaining = stack.getCount();
        for (int slot = 0; slot < inventory.items.size() && remaining > 0; slot++) {
            ItemStack existing = inventory.items.get(slot);
            if (existing.isEmpty()) {
                remaining -= stack.getMaxStackSize();
            } else if (ItemStack.isSameItemSameComponents(existing, stack)) {
                remaining -= Math.max(0, existing.getMaxStackSize() - existing.getCount());
            }
        }
        return remaining <= 0;
    }

    // ---------------------------------------------------------------- corpse lifetime

    /**
     * Whether the owner should now hand this body back to vanilla's removal.
     *
     * <p>The owner's {@code tickDeath()} consults this instead of letting vanilla remove the body
     * at 20 ticks; while it returns false the body stays, inert and visible, with its death counter
     * advancing by one per tick it is actually ticked.
     */
    public static boolean corpseExpired(Mob parent) {
        return parent.deathTime >= CORPSE_TICKS;
    }

    // ---------------------------------------------------------------- persistence

    public void save(CompoundTag tag) {
        if (this.participants.isEmpty() && this.carves.isEmpty()) {
            return;
        }
        CompoundTag root = new CompoundTag();
        ListTag ids = new ListTag();
        for (UUID id : this.participants) {
            ids.add(NbtUtils.createUUID(id));
        }
        root.put(TAG_PARTICIPANTS, ids);
        ListTag counts = new ListTag();
        for (Map.Entry<UUID, Integer> entry : this.carves.entrySet()) {
            CompoundTag row = new CompoundTag();
            row.put(TAG_PLAYER, NbtUtils.createUUID(entry.getKey()));
            row.putInt(TAG_USED, entry.getValue());
            counts.add(row);
        }
        root.put(TAG_CARVES, counts);
        tag.put(TAG_ROOT, root);
    }

    public void load(CompoundTag tag) {
        this.participants.clear();
        this.carves.clear();
        this.lastInteraction.clear();
        if (!tag.contains(TAG_ROOT, Tag.TAG_COMPOUND)) {
            return;
        }
        CompoundTag root = tag.getCompound(TAG_ROOT);
        ListTag ids = root.getList(TAG_PARTICIPANTS, Tag.TAG_INT_ARRAY);
        for (int i = 0; i < ids.size(); i++) {
            this.participants.add(NbtUtils.loadUUID(ids.get(i)));
        }
        ListTag counts = root.getList(TAG_CARVES, Tag.TAG_COMPOUND);
        for (int i = 0; i < counts.size(); i++) {
            CompoundTag row = counts.getCompound(i);
            this.carves.put(NbtUtils.loadUUID(row.get(TAG_PLAYER)), row.getInt(TAG_USED));
        }
    }
}
