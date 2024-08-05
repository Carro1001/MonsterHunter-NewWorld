package com.carro1001.mhnw.client.models.entities;

import com.carro1001.mhnw.entities.LagiacrusEntity;
import com.carro1001.mhnw.entities.LargeMonster;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

import static com.carro1001.mhnw.utils.MHNWReferences.MODID;

public abstract class MonsterModel extends DefaultedEntityGeoModel<LargeMonster> {
    public MonsterModel(String name) {
        super(new ResourceLocation(MODID, name), false);
    }


    public abstract void setCustomAnimations(LargeMonster entity, long uniqueID, AnimationState<LargeMonster> customPredicate);
}
