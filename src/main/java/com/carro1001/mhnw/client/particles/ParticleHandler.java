package com.carro1001.mhnw.client.particles;

import com.carro1001.mhnw.client.particles.iceParticle.IceParticleType;
import com.carro1001.mhnw.client.particles.poisonParticle.PoisonParticleType;
import com.carro1001.mhnw.client.particles.sleepParticle.SleepParticleType;
import com.carro1001.mhnw.client.particles.thunderParticle.ThunderParticleType;
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
        Minecraft.getInstance().particleEngine.register(Registration.THUNDER_PARTICLE_TYPE.get(), ThunderParticleType.ThunderParticleFactory::new);
        Minecraft.getInstance().particleEngine.register(Registration.ICE_PARTICLE_TYPE.get(), IceParticleType.IceParticleFactory::new);
        Minecraft.getInstance().particleEngine.register(Registration.POISON_PARTICLE_TYPE.get(), PoisonParticleType.PoisonParticleFactory::new);
    }

}