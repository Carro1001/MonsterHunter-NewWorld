package com.carro1001.mhnw.entities;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

import static com.carro1001.mhnw.utils.MHNWReferences.LAGIACRUZ;

public class LagiacrusEntity extends Monster {

    public LagiacrusEntity(EntityType<? extends Monster> p_27557_, Level p_27558_) {
        super(p_27557_, p_27558_);
        this.name = LAGIACRUZ;
    }
}
