package com.carro1001.mhnw.registration;

import com.carro1001.mhnw.entities.helpers.BreakablePartHitboxType;
import com.mojang.serialization.Codec;
import de.dertoaster.multihitboxlib.Constants;
import de.dertoaster.multihitboxlib.entity.hitbox.type.IHitboxType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import static com.carro1001.mhnw.utils.MHNWReferences.MODID;

public class ModHitboxTypes {

    public static ResourceLocation loc = new ResourceLocation(Constants.MODID, "registry/dispatcher/hitboxtype");

    public static final DeferredRegister<Codec<? extends IHitboxType>> HITBOXES = DeferredRegister.create(loc, MODID);

    public static final RegistryObject<Codec<? extends IHitboxType>> BREAKABLE = HITBOXES.register("breakable", () -> BreakablePartHitboxType.CODEC);


}
