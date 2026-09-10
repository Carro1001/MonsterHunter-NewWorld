package com.carro1001.mhnw.datagen;

import com.carro1001.mhnw.registration.ModDamageTypes;
import com.carro1001.mhnw.utils.MHNWReferences;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraftforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;

public class ModDamageTypeTagGenerator  extends TagsProvider<DamageType> {
    public ModDamageTypeTagGenerator(PackOutput output, CompletableFuture<HolderLookup.Provider> future, ExistingFileHelper helper) {
        super(output, Registries.DAMAGE_TYPE, future, MHNWReferences.MODID, helper);
    }
    @Override
    protected void addTags(HolderLookup.Provider provider) {
        this.tag(ModDamageTypes.RAW, DamageTypeTags.BYPASSES_COOLDOWN, DamageTypeTags.BYPASSES_RESISTANCE);
        this.tag(ModDamageTypes.THUNDER, DamageTypeTags.BYPASSES_COOLDOWN, DamageTypeTags.IS_LIGHTNING);
        this.tag(ModDamageTypes.FIRE, DamageTypeTags.BYPASSES_COOLDOWN, DamageTypeTags.IS_FIRE, DamageTypeTags.IGNITES_ARMOR_STANDS);
        this.tag(ModDamageTypes.WATER, DamageTypeTags.BYPASSES_COOLDOWN, DamageTypeTags.IS_DROWNING);
        this.tag(ModDamageTypes.ICE, DamageTypeTags.BYPASSES_COOLDOWN, DamageTypeTags.IS_FREEZING);
        this.tag(ModDamageTypes.DRAGON, DamageTypeTags.BYPASSES_COOLDOWN, DamageTypeTags.IS_EXPLOSION);

    }

    @SafeVarargs
    private void tag(ResourceKey<DamageType> type, TagKey<DamageType>... tags) {
        for (TagKey<DamageType> key : tags) {
            tag(key).add(type);
        }
    }
}
