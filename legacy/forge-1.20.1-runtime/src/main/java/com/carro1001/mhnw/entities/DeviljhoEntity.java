package com.carro1001.mhnw.entities;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

import static com.carro1001.mhnw.utils.MHNWReferences.DEVILJHO;

public class DeviljhoEntity extends NewWorldMonsterEntity {

    public DeviljhoEntity(EntityType<? extends NewWorldMonsterEntity> p_27557_, Level p_27558_) {
        super(p_27557_, p_27558_);
        this.name = DEVILJHO;
    }
}
