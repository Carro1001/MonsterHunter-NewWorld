package com.carro1001.mhnw;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server-side settings. Deliberately tiny: the handoff's "small data budget" rule
 * means we add a value here only when a packet actually needs it.
 */
public final class MHNWConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    /** A11: natural spawning must be disableable. */
    public static final ModConfigSpec.BooleanValue NATURAL_SPAWNING = BUILDER
            .comment("Whether MHNW monsters spawn naturally. Disable to stop all natural spawns.")
            .define("naturalSpawning", true);

    /** Sparse combat diagnostics: attack transitions and contact decisions, never per tick. */
    public static final ModConfigSpec.BooleanValue DEBUG_COMBAT = BUILDER
            .comment("Log monster attack transitions and accepted/rejected contact. Development aid.")
            .define("debugCombat", false);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private MHNWConfig() {}
}
