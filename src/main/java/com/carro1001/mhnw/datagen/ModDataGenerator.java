package com.carro1001.mhnw.datagen;

import net.minecraft.data.DataGenerator;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static com.carro1001.mhnw.utils.MHNWReferences.MODID;

@Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModDataGenerator {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event){
        DataGenerator generator = event.getGenerator();
        if(event.includeServer()){
            //generator.addProvider(true,new ModRecipes(generator));
            //generator.addProvider(true,new ModLootTablesProvider(generator));
            //ModBlockTags blocktags = new ModBlockTags(generator,event.getExistingFileHelper());
            //generator.addProvider(true,blocktags);
            //generator.addProvider(true,new ModItemTags(generator,blocktags,event.getExistingFileHelper()));
            //generator.addProvider(event.includeServer(), new BiomeModifierProvider(generator));
        }
        if(event.includeClient()){
            //generator.addProvider(true,new ModBlockStates(generator,event.getExistingFileHelper()));
            //generator.addProvider(true,new ModItemModels(generator,event.getExistingFileHelper()));
            //generator.addProvider(true,new ModLanguageProvider(generator,"en_us"));
        }
    }
}
