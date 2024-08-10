package com.carro1001.mhnw.client.models.entities;

import com.carro1001.mhnw.entities.LagiacrusEntity;
import com.carro1001.mhnw.entities.NewWorldMonsterEntity;
import com.carro1001.mhnw.utils.MathHelpers;
import net.minecraft.util.Mth;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.data.EntityModelData;

import static com.carro1001.mhnw.utils.MHNWReferences.LAGIACRUZ;

public class LagiacrusModel extends MonsterModel {
    public LagiacrusModel() {
        super(LAGIACRUZ);
    }

    @Override
    public void setCustomAnimations(NewWorldMonsterEntity E, long uniqueID, AnimationState<NewWorldMonsterEntity> customPredicate) {
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

            neck1.setRotY((float) (neck1.getRotY() + Mth.PI + MathHelpers.angleClamp(MathHelpers.getAngleForLinkTopDownFlat(entity.tail1Point, entity.tail0Point, entity.tail2Point, entity.leftRefPoint, entity.rightRefPoint), Mth.PI*0.75)));
            neck2.setRotY((float) (neck2.getRotY() + Mth.PI + MathHelpers.angleClamp(MathHelpers.getAngleForLinkTopDownFlat(entity.tail2Point, entity.tail1Point, entity.tail3Point, entity.leftRefPoint, entity.rightRefPoint), Mth.PI*0.75)));
            neck3.setRotY((float) (neck3.getRotY() + Mth.PI + MathHelpers.angleClamp(MathHelpers.getAngleForLinkTopDownFlat(entity.tail3Point, entity.tail2Point, entity.tail4Point, entity.leftRefPoint, entity.rightRefPoint), Mth.PI*0.75)));

            //System.out.println((MathHelpers.getAngleForLinkTopDownFlat(entity.tail1Point, entity.tail0Point, entity.tail2Point, entity.leftRefPoint, entity.rightRefPoint)));
            tail1.setRotY((float) (tail1.getRotY() + Mth.PI - MathHelpers.angleClamp(MathHelpers.getAngleForLinkTopDownFlat(entity.tail1Point, entity.tail0Point, entity.tail2Point, entity.leftRefPoint, entity.rightRefPoint), Mth.PI*0.75)));
            tail2.setRotY((float) (tail2.getRotY() + Mth.PI - MathHelpers.angleClamp(MathHelpers.getAngleForLinkTopDownFlat(entity.tail2Point, entity.tail1Point, entity.tail3Point, entity.leftRefPoint, entity.rightRefPoint), Mth.PI*0.75)));
            tail3.setRotY((float) (tail3.getRotY() + Mth.PI - MathHelpers.angleClamp(MathHelpers.getAngleForLinkTopDownFlat(entity.tail3Point, entity.tail2Point, entity.tail4Point, entity.leftRefPoint, entity.rightRefPoint), Mth.PI*0.75)));
            tail4.setRotY((float) (tail4.getRotY() + Mth.PI - MathHelpers.angleClamp(MathHelpers.getAngleForLinkTopDownFlat(entity.tail4Point, entity.tail3Point, entity.tail5Point, entity.leftRefPoint, entity.rightRefPoint), Mth.PI*0.75)));
            //point, parent, child
            //No deg to rad because the arccos function used to return the angle
            //gotta set up UNIQUE NODES FOR EACH BONE

            //hips.setRotX((float) -(hips.getRotX() - (MathHelpers.angleFromYdiff(Math.hypot(entity.tail0Point.x - entity.tail1Point.x, entity.tail0Point.z - entity.tail1Point.z), entity.tail0Point, entity.tail1Point))));
            //root.setRotX((float) (root.getRotX() - (MathHelpers.angleFromYdiff(Math.hypot(entity.tail0Point.x - entity.tail1Point.x, entity.tail0Point.z - entity.tail1Point.z), entity.tail0Point, entity.tail1Point))));

            tail1.setRotX((float) (tail1.getRotX() - (MathHelpers.angleFromYdiff(Math.hypot(entity.tail1Point.x - entity.tail2Point.x, entity.tail1Point.z - entity.tail2Point.z), entity.tail1Point, entity.tail2Point))));
            tail2.setRotX((float) (tail2.getRotX() - (MathHelpers.angleFromYdiff(Math.hypot(entity.tail2Point.x - entity.tail3Point.x, entity.tail2Point.z - entity.tail3Point.z), entity.tail2Point, entity.tail3Point))));
            tail3.setRotX((float) (tail3.getRotX() - (MathHelpers.angleFromYdiff(Math.hypot(entity.tail3Point.x - entity.tail4Point.x, entity.tail3Point.z - entity.tail4Point.z), entity.tail3Point, entity.tail4Point))));
            tail4.setRotX((float) (tail4.getRotX() - (MathHelpers.angleFromYdiff(Math.hypot(entity.tail4Point.x - entity.tail5Point.x, entity.tail4Point.z - entity.tail5Point.z), entity.tail4Point, entity.tail5Point))));
            //positive RotX is DOWNWARDS, and increasing angle swings it forwards towards the head
        }
        //ik stuff END

    }
}
