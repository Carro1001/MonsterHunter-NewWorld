package com.carro1001.mhnw.registry;

import com.carro1001.mhnw.MHNW;
import com.carro1001.mhnw.entity.Toad;
import com.carro1001.mhnw.item.BarbecueSpitItem;
import com.carro1001.mhnw.item.BoneArmorItem;
import com.carro1001.mhnw.item.FlashBombItem;
import com.carro1001.mhnw.item.GiantJawbladeItem;
import com.carro1001.mhnw.item.ToadBucketItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.food.Foods;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The R1 survival economy -- the three carve materials, the two meats, and the four bone-armor
 * pieces -- plus R2's field-preparation items: the BBQ spit, the bottled Flashbug and its flash
 * bomb, and the four preserved toad buckets.
 *
 * <p>Separate from {@link ModEntities}, which keeps the spawn eggs. They could share one
 * {@code DeferredRegister<Item>}, but the eggs are working, shipped registrations whose only fault
 * is living in a class named after entities; moving them buys a prettier name and risks a rename of
 * things already on disk in someone's world. Two registers, one registry.
 *
 * <p>Item ids and texture names deliberately disagree. The ids are the R1 product names
 * ({@code monster_hide}); the textures are the preserved legacy art ({@code izuchi_hide.png}). The
 * model JSON under {@code assets/mhnw/models/item/} is where the two are joined, which is the
 * ordinary vanilla way and costs nothing. The four armor ids are the established legacy ones
 * ({@code bone_head}, not {@code bone_helmet}) for the same reason.
 */
public final class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(BuiltInRegistries.ITEM, MHNW.MOD_ID);

    public static final DeferredHolder<Item, Item> MONSTER_HIDE =
            ITEMS.register("monster_hide", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, Item> MONSTER_CLAW =
            ITEMS.register("monster_claw", () -> new Item(new Item.Properties()));

    /** Raw and cooked meat use vanilla beef's food values verbatim as the agreed R1 baseline. */
    public static final DeferredHolder<Item, Item> RAW_MEAT =
            ITEMS.register("raw_meat", () -> new Item(new Item.Properties().food(Foods.BEEF)));

    public static final DeferredHolder<Item, Item> COOKED_MEAT =
            ITEMS.register("cooked_meat", () -> new Item(new Item.Properties().food(Foods.COOKED_BEEF)));

    public static final DeferredHolder<Item, Item> BONE_HEAD =
            ITEMS.register("bone_head", () -> new BoneArmorItem(ArmorItem.Type.HELMET));

    public static final DeferredHolder<Item, Item> BONE_CHESTPLATE =
            ITEMS.register("bone_chestplate", () -> new BoneArmorItem(ArmorItem.Type.CHESTPLATE));

    public static final DeferredHolder<Item, Item> BONE_LEGGING =
            ITEMS.register("bone_legging", () -> new BoneArmorItem(ArmorItem.Type.LEGGINGS));

    public static final DeferredHolder<Item, Item> BONE_BOOTS =
            ITEMS.register("bone_boots", () -> new BoneArmorItem(ArmorItem.Type.BOOTS));

    // ---------------------------------------------------------------- R2 field preparation

    /**
     * Stacks to one on purpose: that is what lets a completed use replace its own input slot
     * without any inventory arithmetic (see {@link BarbecueSpitItem}). Its sprite is the preserved
     * {@code rare_monster_meat} art, which already reads as meat on a skewer -- reusing it is why
     * R2 needs no new bitmap and why there is no intermediate "rare meat" food tier.
     */
    public static final DeferredHolder<Item, Item> BBQ_SPIT =
            ITEMS.register("bbq_spit", () -> new BarbecueSpitItem(new Item.Properties().stacksTo(1)));

    /**
     * One caught Flashbug. {@code craftRemainder} is the whole of "the bottle comes back": vanilla's
     * crafting already returns a remainder item exactly once per craft, so nothing here counts
     * bottles.
     */
    public static final DeferredHolder<Item, Item> BOTTLED_FLASHBUG =
            ITEMS.register("bottled_flashbug", () -> new Item(
                    new Item.Properties().stacksTo(16).craftRemainder(Items.GLASS_BOTTLE)));

    public static final DeferredHolder<Item, Item> FLASH_BOMB =
            ITEMS.register("flash_bomb", () -> new FlashBombItem(new Item.Properties().stacksTo(16)));

    /**
     * The four preserved legacy bucket ids and icons, one per {@link Toad.Variant}. The ids are the
     * creature names the art was drawn for ({@code nitrotoad_bucket} for BLAST), not the enum
     * spelling; {@link #toadBucket} is the one place the two are joined.
     */
    public static final DeferredHolder<Item, Item> POISONTOAD_BUCKET =
            ITEMS.register("poisontoad_bucket", () -> new ToadBucketItem(Toad.Variant.POISON, bucketProperties()));

    public static final DeferredHolder<Item, Item> SLEEPTOAD_BUCKET =
            ITEMS.register("sleeptoad_bucket", () -> new ToadBucketItem(Toad.Variant.SLEEP, bucketProperties()));

    public static final DeferredHolder<Item, Item> PARATOAD_BUCKET =
            ITEMS.register("paratoad_bucket", () -> new ToadBucketItem(Toad.Variant.PARALYSIS, bucketProperties()));

    public static final DeferredHolder<Item, Item> NITROTOAD_BUCKET =
            ITEMS.register("nitrotoad_bucket", () -> new ToadBucketItem(Toad.Variant.BLAST, bucketProperties()));

    /**
     * R3's one weapon. Iron tier for its numbers, bone for its repair; see {@link GiantJawbladeItem}
     * for why the damage and speed live in vanilla's attribute modifiers rather than as constants
     * read back out of the item.
     */
    public static final DeferredHolder<Item, Item> GIANT_JAWBLADE =
            ITEMS.register("giant_jawblade", () -> new GiantJawbladeItem(new Item.Properties()));

    /** Vanilla's own filled-bucket properties: one per stack, and an empty bucket on use. */
    private static Item.Properties bucketProperties() {
        return new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET);
    }

    /** The filled bucket a toad of this variant is caught into, and released from. */
    public static DeferredHolder<Item, Item> toadBucket(Toad.Variant variant) {
        return switch (variant) {
            case POISON -> POISONTOAD_BUCKET;
            case SLEEP -> SLEEPTOAD_BUCKET;
            case PARALYSIS -> PARATOAD_BUCKET;
            case BLAST -> NITROTOAD_BUCKET;
        };
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }

    private ModItems() {}
}
