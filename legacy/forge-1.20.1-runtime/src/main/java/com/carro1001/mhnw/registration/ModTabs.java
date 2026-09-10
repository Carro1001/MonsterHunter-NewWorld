package com.carro1001.mhnw.registration;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import static com.carro1001.mhnw.utils.MHNWReferences.MODID;

public class ModTabs {

    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final RegistryObject<CreativeModeTab> ITEMS_TABS = TABS.register("mhnw",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.BONE_HEAD.get()))
                    .title(Component.translatable("creativetab.all_items_tab"))
                    .displayItems((pParameters, pOutput) -> {
                        pOutput.accept(ModItems.BONE_HEAD.get());
                        pOutput.accept(ModItems.BONE_CHEST.get());
                        pOutput.accept(ModItems.BONE_LEGGINGS.get());
                        pOutput.accept(ModItems.BONE_BOOTS.get());
                        pOutput.accept(ModBlocks.CARBALITE_ORE_ITEM.get());
                        pOutput.accept(ModBlocks.DRAGONITE_ORE_ITEM.get());
                        pOutput.accept(ModBlocks.MACHALITE_ORE_ITEM.get());
                        pOutput.accept(ModBlocks.EARTH_CRYSTAL_CLUSTER_ITEM.get());
                        pOutput.accept(ModBlocks.ICE_CRYSTAL_CLUSTER_ITEM.get());
                        pOutput.accept(ModItems.CARBALITE_ITEM.get());
                        pOutput.accept(ModItems.DRAGONITE_ITEM.get());
                        pOutput.accept(ModItems.MACHALITE_ITEM.get());
                        pOutput.accept(ModItems.RAW_CARBALITE_ITEM.get());
                        pOutput.accept(ModItems.RAW_DRAGONITE_ITEM.get());
                        pOutput.accept(ModItems.RAW_MACHALITE_ITEM.get());
                        pOutput.accept(ModItems.EARTH_CRYSTAL_ITEM.get());
                        pOutput.accept(ModItems.ICE_CRYSTAL_ITEM.get());
                        pOutput.accept(ModItems.RAW_MEAT_ITEM.get());
                        pOutput.accept(ModItems.RARE_MEAT_ITEM.get());
                        pOutput.accept(ModItems.WELL_DONE_MEAT_ITEM.get());
                        pOutput.accept(ModItems.MONSTER_FECES_ITEM.get());
                        pOutput.accept(ModItems.WYVERN_GEM_ITEM.get());
                        pOutput.accept(ModItems.SHARP_CLAW_ITEM.get());
                        pOutput.accept(ModItems.IZUCHI_TAIL_ITEM.get());
                        pOutput.accept(ModItems.IZUCHI_HIDE_ITEM.get());
                        pOutput.accept(ModItems.RATHIAN_PLATE_ITEM.get());
                        pOutput.accept(ModItems.RATHIAN_SCALE_ITEM.get());
                        pOutput.accept(ModItems.RATHIAN_TAIL_ITEM.get());
                        pOutput.accept(ModItems.RATHIAN_WEBBING_ITEM.get());
                        pOutput.accept(ModItems.RATHALOS_PLATE_ITEM.get());
                        pOutput.accept(ModItems.RATHALOS_SCALE_ITEM.get());
                        pOutput.accept(ModItems.RATHALOS_TAIL_ITEM.get());
                        pOutput.accept(ModItems.RATHALOS_WEBBING_ITEM.get());
                        pOutput.accept(ModItems.FLAME_SACK_ITEM.get());
                        pOutput.accept(ModItems.FREEZER_SACK_ITEM.get());
                        pOutput.accept(ModItems.SCREAMER_SACK_ITEM.get());
                        pOutput.accept(ModItems.SLEEP_SACK_ITEM.get());
                        pOutput.accept(ModEntities.APTONOTH_EGG_ITEM.get());
                        pOutput.accept(ModEntities.RATHIAN_EGG_ITEM.get());
                        pOutput.accept(ModEntities.RATHALOS_EGG_ITEM.get());
                        pOutput.accept(ModEntities.BITTERBUG_ITEM.get());
                        pOutput.accept(ModEntities.BLANGO_EGG_ITEM.get());
                        pOutput.accept(ModEntities.BLANGONGA_EGG_ITEM.get());
                        pOutput.accept(ModEntities.IZUCHI_EGG_ITEM.get());
                        pOutput.accept(ModEntities.GIZUCHI_EGG_ITEM.get());
                        pOutput.accept(ModEntities.DEVILJOE_EGG_ITEM.get());
                        pOutput.accept(ModEntities.LAGIACRUS_EGG_ITEM.get());
                        pOutput.accept(ModEntities.TOAD_EGG_ITEM.get());
                        pOutput.accept(ModEntities.BUCKET_NITROTOAD_ITEM.get());
                        pOutput.accept(ModEntities.BUCKET_POISONTOAD_ITEM.get());
                        pOutput.accept(ModEntities.BUCKET_PARATOAD_ITEM.get());
                        pOutput.accept(ModEntities.BUCKET_SLEEPTOAD_ITEM.get());
                        pOutput.accept(ModEntities.ZINOGRE_EGG_ITEM.get());
                        pOutput.accept(ModEntities.FLASHFLY_EGG_ITEM.get());
                    })
                    .build());

}
