package com.carro1001.mhnw;

import com.carro1001.mhnw.setup.Registration;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import static com.carro1001.mhnw.utils.MHNWReferences.MODID;

@Mod(MODID)
public class MHNW {

    public MHNW(){
        Registration.init();

    }

}
