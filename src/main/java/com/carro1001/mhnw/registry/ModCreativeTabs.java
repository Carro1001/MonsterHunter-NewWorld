package com.carro1001.mhnw.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The mod's own creative tab. Every MHNW item and spawn egg used to be scattered across vanilla
 * tabs (ingredients, food, combat, tools, spawn eggs) -- {@code onBuildCreativeTabs} in
 * {@link com.carro1001.mhnw.MHNW} still populates this one the same way, just against
 * {@link #MAIN} instead of five separate {@code CreativeModeTabs} keys.
 */
public final class ModCreativeTabs {
    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, com.carro1001.mhnw.MHNW.MOD_ID);

    /** Icon is the Great Izuchi spawn egg: the roster's first species, and immediately readable as
     * "this is the Monster Hunter tab" the way an armor piece or a material would not be. */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN =
            TABS.register("main", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.mhnw.main"))
                    .icon(() -> new ItemStack(ModEntities.GREAT_IZUCHI_SPAWN_EGG.get()))
                    .build());

    public static void register(IEventBus modBus) {
        TABS.register(modBus);
    }

    private ModCreativeTabs() {}
}
