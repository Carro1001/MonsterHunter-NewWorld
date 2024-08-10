package com.carro1001.mhnw.entities;

import com.carro1001.mhnw.registration.ModItems;
import com.carro1001.mhnw.utils.MHNWReferences;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

import java.util.List;

public class RathianEntity extends DragonEntity {

    public RathianEntity(EntityType<? extends NewWorldMonsterEntity> p_27557_, Level p_27558_) {
        super(p_27557_, p_27558_, MHNWReferences.RATHIAN);
        minScale = 0.5f;
        maxScale = 1f;
    }

    public List<Item> getDrops(){
        return List.of(ModItems.RATHIAN_PLATE_ITEM.get(),ModItems.RATHIAN_SCALE_ITEM.get(),
                ModItems.RATHIAN_TAIL_ITEM.get(),ModItems.RATHIAN_WEBBING_ITEM.get(),ModItems.FLAME_SACK_ITEM.get());
    }

}
