package com.carro1001.mhnw.item;

import com.carro1001.mhnw.MHNW;
import com.carro1001.mhnw.registry.ModItems;
import java.util.function.Consumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * All four bone-armor pieces, one class.
 *
 * <h2>Stats</h2>
 * "Iron-equivalent protection, durability and enchantability, zero extra toughness and zero
 * built-in knockback resistance" is, exactly, {@link ArmorMaterials#IRON}. Registering a private
 * armor material that copies iron's numbers would be the same values behind a second name, plus a
 * datapack registry entry to keep in sync; the trait that actually makes this set worth wearing is
 * the set bonus below, not a bespoke defense curve.
 *
 * <h2>The set bonus</h2>
 * +0.1 knockback resistance while all four exact pieces are worn, under the stable id
 * {@code mhnw:bone_armor_set_bonus}. It is a <em>transient</em> modifier, recomputed from scratch by
 * {@link #refreshSetBonus} whenever an armor slot changes (see {@code MHNW.onEquipmentChange}).
 * Transient is the load-bearing word: a permanent modifier is written into the player's saved
 * attribute data, so a set removed while the game was closed would leave the bonus behind forever.
 * A transient one cannot outlive the session, and {@code addOrUpdateTransientModifier} replaces
 * rather than appends, so re-equipping or rejoining cannot stack it. Recomputing the whole set on
 * any armor change also means unequip, death and rejoin need no separate hook: each of those ends
 * with an equipment-change event per slot.
 *
 * <h2>Rendering</h2>
 * The worn model is {@code geo/entity/bone_armor.geo.json} -- the original complete export, with
 * all eight bones including the two boots. The later {@code geo/item/armor/} migration copy renamed
 * its bones to GeckoLib's default {@code armor*} convention but dropped {@code armorRightBoot} and
 * {@code armorLeftBoot} on the way, so a stock {@link GeoArmorRenderer} pointed at it renders the
 * feet slot empty. Mapping eight real bone names in the renderer is a handful of overrides; losing
 * a slot is a shipped bug. The held/inventory icons are ordinary 2D item models and are unrelated
 * to either geometry.
 */
public class BoneArmorItem extends ArmorItem implements GeoItem {

    /** Stable, version-independent id for the set bonus, per the R1 contract. */
    public static final ResourceLocation SET_BONUS_ID =
            ResourceLocation.fromNamespaceAndPath(MHNW.MOD_ID, "bone_armor_set_bonus");

    public static final double SET_BONUS_KNOCKBACK_RESISTANCE = 0.1D;

    private static final AttributeModifier SET_BONUS = new AttributeModifier(
            SET_BONUS_ID, SET_BONUS_KNOCKBACK_RESISTANCE, AttributeModifier.Operation.ADD_VALUE);

    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);

    public BoneArmorItem(ArmorItem.Type type) {
        super(ArmorMaterials.IRON, type, new Item.Properties().durability(type.getDurability(15)));
    }

    /** True only when every one of the four slots holds this set's own exact piece. */
    public static boolean hasFullSet(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.BONE_HEAD.get())
                && entity.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.BONE_CHESTPLATE.get())
                && entity.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.BONE_LEGGING.get())
                && entity.getItemBySlot(EquipmentSlot.FEET).is(ModItems.BONE_BOOTS.get());
    }

    /** Server-side: bring the set bonus into line with what is currently worn. Idempotent. */
    public static void refreshSetBonus(LivingEntity entity) {
        AttributeInstance resistance = entity.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (resistance == null) {
            return;
        }
        if (hasFullSet(entity)) {
            resistance.addOrUpdateTransientModifier(SET_BONUS);
        } else {
            resistance.removeModifier(SET_BONUS_ID);
        }
    }

    // ---------------------------------------------------------------- GeckoLib

    /** Static armor: no controllers, nothing to animate, nothing to tick. */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {}

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animCache;
    }

    /**
     * The one bridge to client-only code, and it is safe on a dedicated server precisely because it
     * is a bridge: the anonymous provider below is its own class file, and nothing loads it until
     * GeckoLib asks for a render provider, which only ever happens while rendering. The
     * {@code MHNWClient.BoneArmorRenderer} it names is never resolved on a server. The renderer is
     * built once and kept, not allocated per frame.
     */
    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private GeoArmorRenderer<BoneArmorItem> renderer;

            @Override
            public <T extends LivingEntity> HumanoidModel<?> getGeoArmorRenderer(
                    T livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot,
                    HumanoidModel<T> original) {
                if (this.renderer == null) {
                    this.renderer = new com.carro1001.mhnw.client.MHNWClient.BoneArmorRenderer();
                }
                return this.renderer;
            }
        });
    }
}
