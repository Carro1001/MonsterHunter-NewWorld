package com.carro1001.mhnw.client.models.entities;

import com.carro1001.mhnw.entities.LagiacrusEntity;
import com.carro1001.mhnw.entities.NewWorldGrowingEntity;
import com.carro1001.mhnw.utils.MathHelpers;
import net.minecraft.util.Mth;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.data.EntityModelData;

import static com.carro1001.mhnw.utils.MHNWReferences.LAGIACRUZ;

public class LagiacrusModel extends NewWorldGrowingEntityModel {
    public LagiacrusModel() {
        super(LAGIACRUZ);
    }

    @Override
    public void setCustomAnimations(NewWorldGrowingEntity E, long uniqueID, AnimationState<NewWorldGrowingEntity> customPredicate) {
        EntityModelData extraData = customPredicate.getData(DataTickets.ENTITY_MODEL_DATA);

        //ik stuff START
        if (E instanceof LagiacrusEntity entity) {
            CoreGeoBone neck1 = this.getAnimationProcessor().getBone("neck1");
            CoreGeoBone neck2 = this.getAnimationProcessor().getBone("neck2");
            CoreGeoBone neck3 = this.getAnimationProcessor().getBone("neck3");

            CoreGeoBone tail1 = this.getAnimationProcessor().getBone("tail1");
            CoreGeoBone tail2 = this.getAnimationProcessor().getBone("tail2");
            CoreGeoBone tail3 = this.getAnimationProcessor().getBone("tail3");
            CoreGeoBone tail4 = this.getAnimationProcessor().getBone("tail4");

            neck1.setRotY((float) (Mth.PI + (MathHelpers.LerpDegrees((float) entity.currentTail1Yaw, (float) entity.tail1Yaw, 0.1))));
            neck2.setRotY((float) (Mth.PI + (MathHelpers.LerpDegrees((float) entity.currentTail2Yaw, (float) entity.tail2Yaw, 0.1))));
            neck3.setRotY((float) (Mth.PI + (MathHelpers.LerpDegrees((float) entity.currentTail3Yaw, (float) entity.tail3Yaw, 0.1))));

            //System.out.println((MathHelpers.getAngleForLinkTopDownFlat(entity.tail1Point, entity.tail0Point, entity.tail2Point, entity.leftRefPoint, entity.rightRefPoint)));
            tail1.setRotY((float) (Mth.PI - (MathHelpers.LerpDegrees((float) entity.currentTail1Yaw, (float) entity.tail1Yaw, 0.1))));
            tail2.setRotY((float) (Mth.PI - (MathHelpers.LerpDegrees((float) entity.currentTail2Yaw, (float) entity.tail2Yaw, 0.1))));
            tail3.setRotY((float) (Mth.PI - (MathHelpers.LerpDegrees((float) entity.currentTail3Yaw, (float) entity.tail3Yaw, 0.1))));
            tail4.setRotY((float) (Mth.PI - (MathHelpers.LerpDegrees((float) entity.currentTail4Yaw, (float) entity.tail4Yaw, 0.1))));
            entity.currentTail1Yaw = (float) MathHelpers.LerpDegrees((float) entity.currentTail1Yaw, (float) entity.tail1Yaw, 0.1);
            entity.currentTail2Yaw = (float) MathHelpers.LerpDegrees((float) entity.currentTail2Yaw, (float) entity.tail2Yaw, 0.1);
            entity.currentTail3Yaw = (float) MathHelpers.LerpDegrees((float) entity.currentTail3Yaw, (float) entity.tail3Yaw, 0.1);
            entity.currentTail4Yaw = (float) MathHelpers.LerpDegrees((float) entity.currentTail4Yaw, (float) entity.tail4Yaw, 0.1);
            //point, parent, child
            //No deg to rad because the arccos function used to return the angle
            //gotta set up UNIQUE NODES FOR EACH BONE

            //hips.setRotX((float) -(hips.getRotX() - (MathHelpers.angleFromYdiff(Math.hypot(entity.tail0Point.x - entity.tail1Point.x, entity.tail0Point.z - entity.tail1Point.z), entity.tail0Point, entity.tail1Point))));
            //root.setRotX((float) (root.getRotX() - (MathHelpers.angleFromYdiff(Math.hypot(entity.tail0Point.x - entity.tail1Point.x, entity.tail0Point.z - entity.tail1Point.z), entity.tail0Point, entity.tail1Point))));

            tail1.setRotX((float) (tail1.getRotX() - MathHelpers.LerpDegrees((float) entity.currentTail1Pitch, (float) entity.tail1Pitch, 0.1)));
            tail2.setRotX((float) (tail2.getRotX() - MathHelpers.LerpDegrees((float) entity.currentTail2Pitch, (float) entity.tail2Pitch, 0.1)));
            tail3.setRotX((float) (tail3.getRotX() - MathHelpers.LerpDegrees((float) entity.currentTail3Pitch, (float) entity.tail3Pitch, 0.1)));
            tail4.setRotX((float) (tail4.getRotX() - MathHelpers.LerpDegrees((float) entity.currentTail4Pitch, (float) entity.tail4Pitch, 0.1)));
            entity.currentTail1Pitch = (float) MathHelpers.LerpDegrees((float) entity.currentTail1Pitch, (float) entity.tail1Pitch, 0.1);
            entity.currentTail2Pitch = (float) MathHelpers.LerpDegrees((float) entity.currentTail2Pitch, (float) entity.tail2Pitch, 0.1);
            entity.currentTail3Pitch = (float) MathHelpers.LerpDegrees((float) entity.currentTail3Pitch, (float) entity.tail3Pitch, 0.1);
            entity.currentTail4Pitch = (float) MathHelpers.LerpDegrees((float) entity.currentTail4Pitch, (float) entity.tail4Pitch, 0.1);
            //positive RotX is DOWNWARDS, and increasing angle swings it forwards towards the head
        }
        //ik stuff END

    }
}
