package com.carro1001.mhnw.entities;

import com.carro1001.mhnw.utils.MHNWReferences;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;

public class RathianEntity extends DragonEntity {
    public RathianEntity(EntityType<? extends PathfinderMob > p_27557_, Level p_27558_) {
        super(p_27557_, p_27558_, MHNWReferences.RATHIAN);
        minScale = 0.5f;
        maxScale = 1f;
    }
}
