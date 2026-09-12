package com.carro1001.mhnw.registry;

import com.carro1001.mhnw.MHNW;
import com.carro1001.mhnw.item.BoneArmorItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.food.Foods;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The R1 survival economy: the three carve materials, the two meats, and the four bone-armor
 * pieces.
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

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }

    private ModItems() {}
}
