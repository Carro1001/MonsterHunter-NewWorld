package com.carro1001.mhnw.entities;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

import static com.carro1001.mhnw.utils.MHNWReferences.BLANGO;

public class BlangoEntity extends LargeMonster {
    public BlangoEntity(EntityType<? extends LargeMonster> p_27557_, Level p_27558_) {
        super(p_27557_, p_27558_);
        this.name = BLANGO;
    }

}
