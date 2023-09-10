package com.carro1001.mhnw.registration;

import com.carro1001.mhnw.items.BoneArmorItem;
import com.carro1001.mhnw.utils.MHNWReferences;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.core.registries.Registries;

import static com.carro1001.mhnw.utils.MHNWReferences.*;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final Item.Properties basicItem = (new Item.Properties());
    public static final RegistryObject<Item> CARBALITE_ITEM = ITEMS.register(MHNWReferences.CARBALITE_ITEM, () -> new Item(basicItem));
    public static final RegistryObject<Item> DRAGONITE_ITEM = ITEMS.register(MHNWReferences.DRAGONITE_ITEM, () -> new Item(basicItem));
    public static final RegistryObject<Item> MACHALITE_ITEM = ITEMS.register(MHNWReferences.MACHALITE_ITEM, () -> new Item(basicItem));
    public static final RegistryObject<Item> RAW_CARBALITE_ITEM = ITEMS.register(MHNWReferences.RAW_CARBALITE_ITEM, () -> new Item(basicItem));
    public static final RegistryObject<Item> RAW_DRAGONITE_ITEM = ITEMS.register(MHNWReferences.RAW_DRAGONITE_ITEM, () -> new Item(basicItem));
    public static final RegistryObject<Item> RAW_MACHALITE_ITEM = ITEMS.register(MHNWReferences.RAW_MACHALITE_ITEM, () -> new Item(basicItem));
    public static final RegistryObject<Item> EARTH_CRYSTAL_ITEM = ITEMS.register(MHNWReferences.EARTH_CRYSTAL_ITEM, () -> new Item(basicItem));
    public static final RegistryObject<Item> ICE_CRYSTAL_ITEM = ITEMS.register(MHNWReferences.ICE_CRYSTAL_ITEM, () -> new Item(basicItem));

    public static final FoodProperties RAW_MEAT = (new FoodProperties.Builder()).nutrition(4).saturationMod(0.1F).effect(new MobEffectInstance(MobEffects.HUNGER, 600, 0), 0.8F).meat().build();
    public static final FoodProperties RARE_MEAT = (new FoodProperties.Builder()).nutrition(6).saturationMod(0.6F).meat().build();
    public static final FoodProperties WELL_DONE_MEAT = (new FoodProperties.Builder()).nutrition(8).saturationMod(0.8F).meat().build();

    public static final RegistryObject<Item> RAW_MEAT_ITEM = ITEMS.register(MHNWReferences.RAW_MEAT_ITEM, () -> new Item(basicItem.food(RAW_MEAT)));
    public static final RegistryObject<Item> RARE_MEAT_ITEM = ITEMS.register(MHNWReferences.RARE_MEAT_ITEM, () -> new Item(basicItem.food(RARE_MEAT)));
    public static final RegistryObject<Item> WELL_DONE_MEAT_ITEM = ITEMS.register(MHNWReferences.WELL_DONE_MEAT_ITEM, () -> new Item(basicItem.food(WELL_DONE_MEAT)));
    public static final RegistryObject<Item> MONSTER_FECES_ITEM = ITEMS.register(MHNWReferences.MONSTER_FECES_ITEM, () -> new Item(basicItem));

    public static final RegistryObject<Item> RATHIAN_PLATE_ITEM = ITEMS.register(MHNWReferences.RATHIAN+"_"+ PLATE_ITEM, () -> new Item(basicItem));
    public static final RegistryObject<Item> RATHIAN_SCALE_ITEM = ITEMS.register(MHNWReferences.RATHIAN+"_"+ SCALE_ITEM, () -> new Item(basicItem));
    public static final RegistryObject<Item> RATHIAN_TAIL_ITEM = ITEMS.register(MHNWReferences.RATHIAN+"_"+ TAIL_ITEM, () -> new Item(basicItem));
    public static final RegistryObject<Item> RATHIAN_WEBBING_ITEM = ITEMS.register(MHNWReferences.RATHIAN+"_"+ WEBBING_ITEM, () -> new Item(basicItem));

    public static final RegistryObject<Item> RATHALOS_PLATE_ITEM = ITEMS.register(MHNWReferences.RATHALOS+"_"+ PLATE_ITEM, () -> new Item(basicItem));
    public static final RegistryObject<Item> RATHALOS_SCALE_ITEM = ITEMS.register(MHNWReferences.RATHALOS+"_"+ SCALE_ITEM, () -> new Item(basicItem));
    public static final RegistryObject<Item> RATHALOS_TAIL_ITEM = ITEMS.register(MHNWReferences.RATHALOS+"_"+ TAIL_ITEM, () -> new Item(basicItem));
    public static final RegistryObject<Item> RATHALOS_WEBBING_ITEM = ITEMS.register(MHNWReferences.RATHALOS+"_"+ WEBBING_ITEM, () -> new Item(basicItem));

    public static final RegistryObject<Item> FLAME_SACK_ITEM = ITEMS.register(SACKS[0]+"_"+ SACK, () -> new Item(basicItem));
    public static final RegistryObject<Item> FREEZER_SACK_ITEM = ITEMS.register(SACKS[1]+"_"+ SACK, () -> new Item(basicItem));
    public static final RegistryObject<Item> SCREAMER_SACK_ITEM = ITEMS.register(SACKS[2]+"_"+ SACK, () -> new Item(basicItem));
    public static final RegistryObject<Item> SLEEP_SACK_ITEM = ITEMS.register(SACKS[3]+"_"+ SACK, () -> new Item(basicItem));

///////////////////ARMORS/////////////////////////////
    public static final RegistryObject<BoneArmorItem> BONE_HEAD = ITEMS.register(BONE_ARMOR_HEAD_ITEM,
            () -> new BoneArmorItem(ArmorMaterials.CHAIN, ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistryObject<BoneArmorItem> BONE_CHEST = ITEMS.register(BONE_ARMOR_CHESTPLATE_ITEM,
            () -> new BoneArmorItem(ArmorMaterials.CHAIN, ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistryObject<BoneArmorItem> BONE_LEGGINGS = ITEMS.register(BONE_ARMOR_LEGGING_ITEM,
            () -> new BoneArmorItem(ArmorMaterials.CHAIN, ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final RegistryObject<BoneArmorItem> BONE_BOOTS = ITEMS.register(BONE_ARMOR_BOOT_ITEM,
            () -> new BoneArmorItem(ArmorMaterials.CHAIN, ArmorItem.Type.BOOTS, new Item.Properties()));

/*    public static final RegistryObject<CreativeModeTab> MHNW_TAB = TABS.register("mhnw_items", () -> CreativeModeTab.builder()
            .m_257941_(Component.translatable("itemGroup." + MODID + ".mhnw_items"))
            .icon(() -> new ItemStack(BONE_HEAD.get()))
            .displayItems((enabledFeatures, entries) -> {
                entries.accept(BONE_HEAD.get());
                entries.accept(ItemRegistry.GECKO_ARMOR_CHESTPLATE.get());
                entries.accept(ItemRegistry.GECKO_ARMOR_LEGGINGS.get());
                entries.accept(ItemRegistry.GECKO_ARMOR_BOOTS.get());
                entries.accept(ItemRegistry.WOLF_ARMOR_HELMET.get());
                entries.accept(ItemRegistry.WOLF_ARMOR_CHESTPLATE.get());
                entries.accept(ItemRegistry.WOLF_ARMOR_LEGGINGS.get());
                entries.accept(ItemRegistry.WOLF_ARMOR_BOOTS.get());
                entries.accept(ItemRegistry.GECKO_HABITAT.get());
                entries.accept(ItemRegistry.FERTILIZER.get());
                entries.accept(ItemRegistry.BAT_SPAWN_EGG.get());
                entries.accept(ItemRegistry.BIKE_SPAWN_EGG.get());
                entries.accept(ItemRegistry.RACE_CAR_SPAWN_EGG.get());
                entries.accept(ItemRegistry.PARASITE_SPAWN_EGG.get());
                entries.accept(ItemRegistry.MUTANT_ZOMBIE_SPAWN_EGG.get());
                entries.accept(ItemRegistry.GREMLIN_SPAWN_EGG.get());
                entries.accept(ItemRegistry.FAKE_GLASS_SPAWN_EGG.get());
                entries.accept(ItemRegistry.COOL_KID_SPAWN_EGG.get());
            })
            .build());*/

}
