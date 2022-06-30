package com.carro1001.mhnw.setup;

import com.carro1001.mhnw.entities.aptonoth.AptonothEntity;
import com.carro1001.mhnw.entities.bitterbug.BitterbugEntity;
import com.carro1001.mhnw.entities.blango.BlangoEntity;
import com.carro1001.mhnw.entities.blangonga.BlangongaEntity;
import com.carro1001.mhnw.entities.greatIzuchi.GreatIzuchiEntity;
import com.carro1001.mhnw.entities.izuchi.IzuchiEntity;
import com.carro1001.mhnw.entities.rathalos.RathalosEntity;
import com.carro1001.mhnw.entities.rathian.RathianEntity;
import com.carro1001.mhnw.entities.toad.ToadEntity;
import com.carro1001.mhnw.entities.zamite.ZamiteEntity;
import com.carro1001.mhnw.entities.zinogre.ZinogreEntity;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static com.carro1001.mhnw.utils.MHNWReferences.MODID;

@Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class RegistrationHelper {
    @SubscribeEvent
    public static void onAttributeCreate(EntityAttributeCreationEvent event) {
        event.put(Registration.APTONOTH.get(), AptonothEntity.prepareAttributes().build());
        event.put(Registration.RATHIAN.get(), RathianEntity.prepareAttributes().build());
        event.put(Registration.RATHALOS.get(), RathalosEntity.prepareAttributes().build());
        event.put(Registration.BITTERBUG.get(), BitterbugEntity.prepareAttributes().build());
        event.put(Registration.TOAD.get(), ToadEntity.prepareAttributes().build());
        event.put(Registration.BLANGO.get(), BlangoEntity.prepareAttributes().build());
        event.put(Registration.BLANGONGA.get(), BlangongaEntity.prepareAttributes().build());
        event.put(Registration.IZUCHI.get(), IzuchiEntity.prepareAttributes().build());
        event.put(Registration.GIZUCHI.get(), GreatIzuchiEntity.prepareAttributes().build());
        event.put(Registration.ZAMITE.get(), ZamiteEntity.prepareAttributes().build());
        event.put(Registration.ZINOGRE.get(), ZinogreEntity.prepareAttributes().build());

    }
}
