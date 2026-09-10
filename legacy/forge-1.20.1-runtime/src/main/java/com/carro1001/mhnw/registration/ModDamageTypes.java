package com.carro1001.mhnw.registration;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;

import static com.carro1001.mhnw.utils.MHNWReferences.MODID;

public class ModDamageTypes {
    public static final ResourceKey<DamageType> RAW = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(MODID, "raw"));
    public static final ResourceKey<DamageType> THUNDER = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(MODID, "elemental_thunder"));
    public static final ResourceKey<DamageType> FIRE = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(MODID, "elemental_fire"));
    public static final ResourceKey<DamageType> WATER = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(MODID, "elemental_water"));
    public static final ResourceKey<DamageType> ICE = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(MODID, "elemental_ice"));
    public static final ResourceKey<DamageType> DRAGON = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(MODID, "elemental_dragon"));
    private static final float DEFAULT_EXHAUSTION = 0.1f;

    public static void bootstrap(BootstapContext<DamageType> ctx)
    {
        ctx.register(RAW, new DamageType("raw", DEFAULT_EXHAUSTION));
        ctx.register(THUNDER, new DamageType("elemental_thunder", DEFAULT_EXHAUSTION));
        ctx.register(FIRE, new DamageType("elemental_fire", DEFAULT_EXHAUSTION));
        ctx.register(WATER, new DamageType("elemental_water", DEFAULT_EXHAUSTION));
        ctx.register(ICE, new DamageType("elemental_ice", DEFAULT_EXHAUSTION));
        ctx.register(DRAGON, new DamageType("elemental_dragon", DEFAULT_EXHAUSTION));
    }
}
