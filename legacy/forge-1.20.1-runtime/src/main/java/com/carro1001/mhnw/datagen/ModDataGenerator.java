package com.carro1001.mhnw.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.DatapackBuiltinEntriesProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.concurrent.CompletableFuture;

import static com.carro1001.mhnw.utils.MHNWReferences.MODID;

@Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModDataGenerator {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event){
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
        DatapackBuiltinEntriesProvider datapackProvider = new ModRegistryDataGenerator(packOutput, event.getLookupProvider());
        CompletableFuture<HolderLookup.Provider> lookupProvider = datapackProvider.getRegistryProvider();

        generator.addProvider(event.includeServer(), new ModRecipes(packOutput));
        generator.addProvider(event.includeServer(), ModLootTablesProvider.create(packOutput));

        generator.addProvider(event.includeClient(), new ModBlockStates(packOutput, existingFileHelper));
        generator.addProvider(event.includeClient(), new ModItemModels(packOutput, existingFileHelper));
        generator.addProvider(event.includeClient(), new ModLanguageProvider(packOutput, "en_us"));
        generator.addProvider(event.includeServer(), datapackProvider);

        ModBlockTags blockTagGenerator = generator.addProvider(event.includeServer(),
                new ModBlockTags(packOutput, lookupProvider, existingFileHelper));
        generator.addProvider(event.includeServer(), new ModItemTags(packOutput, lookupProvider, blockTagGenerator.contentsGetter(), existingFileHelper));
        generator.addProvider(event.includeServer(), new ModDamageTypeTagGenerator(packOutput, lookupProvider, existingFileHelper));
    }
}
