package com.carro1001.mhnw.registry;

import com.carro1001.mhnw.MHNW;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The mod's data components. Exactly one so far.
 *
 * <h2>Why this exists at all, given R3's "nothing is stored" rule</h2>
 * That rule governs the <em>charge</em>: no field, component, attachment or packet may let a reload
 * resume or cash in a charge, which is why the tier and the fizzle are both derived from vanilla's
 * own use countdown and nothing else. It still holds -- nothing here can resume a charge.
 *
 * <p>{@link #SWING_TIER} is a different thing: how hard the <em>last</em> swing was, so the blade's
 * arc can match it. Every client that can see the holder needs that number, and the alternatives
 * were worse. GeckoLib's own {@code triggerAnim} syncs but bypasses the animation predicate, which
 * is what keeps the inventory icon still (see {@code GiantJawbladeItem.animatesIn}) -- a triggered
 * swing would have started the hotbar icon animating again. A component rides along with the stack
 * for free, reaches every viewer, and leaves the predicate the single place that decides what plays.
 *
 * <p>It surviving a reload is harmless: it is overwritten by the next swing and only ever read while
 * vanilla says the holder is mid-swing.
 */
public final class ModDataComponents {

    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MHNW.MOD_ID);

    /**
     * Which charge tier the last swing came from: {@code 0} for an uncharged or failed release,
     * rising to {@code TIER_TICKS.length - 1}. Read only while the holder is mid-swing.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> SWING_TIER =
            COMPONENTS.register("swing_tier", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    public static void register(IEventBus modBus) {
        COMPONENTS.register(modBus);
    }

    private ModDataComponents() {}
}
