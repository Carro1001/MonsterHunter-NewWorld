package com.carro1001.mhnw.client.particles;

import com.carro1001.mhnw.client.particles.sleepParticle.SleepParticleType;
import com.carro1001.mhnw.setup.Registration;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ParticleFactoryRegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static com.carro1001.mhnw.utils.MHNWReferences.MODID;

@Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ParticleHandler {

    @SubscribeEvent
    public static void registerFactories(ParticleFactoryRegisterEvent evt) {
        Minecraft.getInstance().particleEngine.register(Registration.SLEEP_PARTICLE_TYPE.get(), SleepParticleType.SleepParticleFactory::new);
    }

}