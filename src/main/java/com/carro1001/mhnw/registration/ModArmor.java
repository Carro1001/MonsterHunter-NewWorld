package com.carro1001.mhnw.registration;

import com.carro1001.mhnw.items.BoneArmorItem;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;

import static com.carro1001.mhnw.registration.ModItems.ITEMS;
import static com.carro1001.mhnw.utils.MHNWReferences.*;

public class ModArmor {

    public static final RegistryObject<BoneArmorItem> BONE_HEAD = ITEMS.register(BONE_ARMOR_HEAD_ITEM,
            () -> new BoneArmorItem(ArmorMaterials.CHAIN, EquipmentSlot.HEAD, new Item.Properties()));
    public static final RegistryObject<BoneArmorItem> BONE_CHEST = ITEMS.register(BONE_ARMOR_CHESTPLATE_ITEM,
            () -> new BoneArmorItem(ArmorMaterials.CHAIN, EquipmentSlot.CHEST, new Item.Properties()));
    public static final RegistryObject<BoneArmorItem> BONE_LEGGINGS = ITEMS.register(BONE_ARMOR_LEGGING_ITEM,
            () -> new BoneArmorItem(ArmorMaterials.CHAIN, EquipmentSlot.LEGS, new Item.Properties()));
    public static final RegistryObject<BoneArmorItem> BONE_BOOTS = ITEMS.register(BONE_ARMOR_BOOT_ITEM,
            () -> new BoneArmorItem(ArmorMaterials.CHAIN, EquipmentSlot.FEET, new Item.Properties()));


}
