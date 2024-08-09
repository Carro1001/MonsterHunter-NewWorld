package com.carro1001.mhnw.entities;

import com.carro1001.mhnw.entities.interfaces.IGrows;
import com.carro1001.mhnw.registration.ModItems;
import com.carro1001.mhnw.utils.MHNWReferences;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;

import java.util.List;

public class RathalosEntity extends DragonEntity implements GeoEntity, IGrows {

    public RathalosEntity(EntityType<? extends LargeMonster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel, MHNWReferences.RATHALOS);
        minScale = 0.6f;
        maxScale = 1.1f;
    }

    @Override
    public void tick() {
        if (random.nextInt(250) == 0 && !level().isClientSide) {
            this.setState(State.values()[(getState().ordinal() + 1) % State.values().length]);
        }
        super.tick();

    }
    public List<Item> getDrops(){
        return List.of(ModItems.RATHALOS_PLATE_ITEM.get(),ModItems.RATHALOS_SCALE_ITEM.get(),
                ModItems.RATHALOS_TAIL_ITEM.get(),ModItems.RATHALOS_WEBBING_ITEM.get(),ModItems.FLAME_SACK_ITEM.get());
    }
}
