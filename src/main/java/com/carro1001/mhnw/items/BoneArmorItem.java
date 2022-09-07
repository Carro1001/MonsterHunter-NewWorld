package com.carro1001.mhnw.items;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;
import software.bernie.geckolib3.item.GeoArmorItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static com.carro1001.mhnw.MHNW.GROUP;
import static com.carro1001.mhnw.registration.ModItems.*;

public class BoneArmorItem extends GeoArmorItem implements IAnimatable {
    private AnimationFactory factory = new AnimationFactory(this);

    public BoneArmorItem(ArmorMaterial materialIn, EquipmentSlot slot, @NotNull Properties builder) {
        super(materialIn, slot, builder.tab(GROUP));
    }


    private <P extends IAnimatable> PlayState predicate(AnimationEvent<P> event) {
        List<EquipmentSlot> slotData = event.getExtraDataOfType(EquipmentSlot.class);
        List<ItemStack> stackData = event.getExtraDataOfType(ItemStack.class);
        LivingEntity livingEntity = event.getExtraDataOfType(LivingEntity.class).get(0);

        event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.bone_armor.new", true));
        if (livingEntity instanceof ArmorStand) {
            return PlayState.CONTINUE;
        }

        List<Item> armorList = new ArrayList<>(4);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.ARMOR) {
                livingEntity.getItemBySlot(slot);
                armorList.add(livingEntity.getItemBySlot(slot).getItem());
            }
        }

        boolean isWearingAll = new HashSet<>(armorList).containsAll(Arrays.asList(BONE_BOOTS.get(),
                BONE_LEGGINGS.get(), BONE_CHEST.get(), BONE_HEAD.get()));
        return isWearingAll ? PlayState.CONTINUE : PlayState.STOP;
    }

    @Override
    public void registerControllers(AnimationData data) {
        data.addAnimationController(new AnimationController(this, "controller", 20, this::predicate));
    }

    @Override
    public AnimationFactory getFactory() {
        return this.factory;
    }
}
