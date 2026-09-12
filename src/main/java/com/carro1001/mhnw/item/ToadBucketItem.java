package com.carro1001.mhnw.item;

import com.carro1001.mhnw.entity.Toad;
import com.carro1001.mhnw.registry.ModEntities;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluids;

/**
 * One of the four preserved toad buckets. The variant is a property of the <em>item</em>, not of
 * the stack: {@code mhnw:nitrotoad_bucket} releases a BLAST toad whatever (or whether) it carries
 * bucket data, which is what makes a bare {@code /give} stack behave correctly.
 *
 * <h2>Why this extends {@link BucketItem} and not {@code MobBucketItem}</h2>
 * Two reasons, both concrete. Vanilla's {@code MobBucketItem} keeps its spawn seam private and lets
 * {@code finalizeSpawn} pick the creature's random state before the bucket data is applied, so a
 * stack with no data would release a random variant -- exactly the case this item exists to get
 * right. And it takes the {@code EntityType} in its constructor, which would force resolving the
 * deferred toad holder while items are still being registered. So the fixed part of its behaviour
 * (empty the water, then spawn) is reproduced here in the few lines below, and nothing else about
 * bucket behaviour is forked: emptying the water, the empty-bucket return and the creative
 * infinite-materials rule all stay {@link BucketItem}'s.
 */
public class ToadBucketItem extends BucketItem {

    private final Toad.Variant variant;

    public ToadBucketItem(Toad.Variant variant, Properties properties) {
        super(Fluids.WATER, properties);
        this.variant = variant;
    }

    public Toad.Variant variant() {
        return this.variant;
    }

    /**
     * Called by {@link BucketItem#use} after the water has been placed and before the empty bucket
     * is handed back, so one right-click yields exactly one toad and exactly one bucket.
     *
     * <p>The variant is set after the spawn, which is what makes {@code finalizeSpawn}'s random
     * choice irrelevant no matter how a stack reached the player's hand. {@code FromBucket} is set
     * for the same reason vanilla's fish do it: it makes the released creature survive the
     * distance-despawn rule.
     */
    @Override
    public void checkExtraContent(@Nullable Player player, Level level, ItemStack containerStack, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        Toad toad = ModEntities.TOAD.get().spawn(
                serverLevel, containerStack, player, pos, MobSpawnType.BUCKET, true, false);
        if (toad != null) {
            CustomData data = containerStack.getOrDefault(DataComponents.BUCKET_ENTITY_DATA, CustomData.EMPTY);
            toad.loadFromBucketTag(data.copyTag());
            toad.setFromBucket(true);
            toad.setVariant(this.variant);
        }
        level.gameEvent(player, GameEvent.ENTITY_PLACE, pos);
    }

    @Override
    protected void playEmptySound(@Nullable Player player, LevelAccessor level, BlockPos pos) {
        level.playSound(player, pos, SoundEvents.BUCKET_EMPTY_FISH, SoundSource.NEUTRAL, 1.0F, 1.0F);
    }
}
