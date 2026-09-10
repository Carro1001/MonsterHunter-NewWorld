package com.carro1001.mhnw.registration;

import com.carro1001.mhnw.client.particles.iceParticle.IceParticleType;
import com.carro1001.mhnw.client.particles.poisonParticle.PoisonParticleType;
import com.carro1001.mhnw.client.particles.sleepParticle.SleepParticleType;
import com.carro1001.mhnw.client.particles.thunderParticle.ThunderParticleType;
import com.carro1001.mhnw.utils.MHNWReferences;
import net.minecraft.core.particles.ParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import static com.carro1001.mhnw.utils.MHNWReferences.*;

@Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModParticle {
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, MODID);

    public static final RegistryObject<SleepParticleType> SLEEP_PARTICLE_TYPE = PARTICLES.register(MHNWReferences.SLEEP_PARTICLE_REGNAME, SleepParticleType::new);
    public static final RegistryObject<ThunderParticleType> THUNDER_PARTICLE_TYPE = PARTICLES.register(THUNDER_PARTICLE_REGNAME, ThunderParticleType::new);
    public static final RegistryObject<PoisonParticleType> POISON_PARTICLE_TYPE = PARTICLES.register(POISON_PARTICLE_REGNAME, PoisonParticleType::new);
    public static final RegistryObject<IceParticleType> ICE_PARTICLE_TYPE = PARTICLES.register(ICE_PARTICLE_REGNAME, IceParticleType::new);

    @SubscribeEvent
    public static void registerFactories(RegisterParticleProvidersEvent evt) {
        evt.registerSpriteSet(SLEEP_PARTICLE_TYPE.get(), SleepParticleType.SleepParticleFactory::new);
        evt.registerSpriteSet(THUNDER_PARTICLE_TYPE.get(), ThunderParticleType.ThunderParticleFactory::new);
        evt.registerSpriteSet(ICE_PARTICLE_TYPE.get(), IceParticleType.IceParticleFactory::new);
        evt.registerSpriteSet(POISON_PARTICLE_TYPE.get(), PoisonParticleType.PoisonParticleFactory::new);
    }

}