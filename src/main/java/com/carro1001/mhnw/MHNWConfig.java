package com.carro1001.mhnw;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Settings. Deliberately tiny: the handoff's "small data budget" rule means a value is added here
 * only when a packet actually needs it.
 */
public final class MHNWConfig {

    // --- Server: per-world gameplay settings. ---
    private static final ModConfigSpec.Builder SERVER_BUILDER = new ModConfigSpec.Builder();

    /**
     * A11: natural spawning must be disableable. As of R1a this covers the habitat's passive wildlife
     * as well as its monsters, and both automatic spawn sources -- see {@code HuntingSpawnRules}.
     */
    public static final ModConfigSpec.BooleanValue NATURAL_SPAWNING = SERVER_BUILDER
            .comment("Whether MHNW wildlife spawns naturally. Disable to stop all automatic MHNW"
                    + " spawns (monsters and passive creatures alike). Spawn eggs, /summon and mob"
                    + " spawners keep working, and vanilla wildlife is unaffected.")
            .define("naturalSpawning", true);

    public static final ModConfigSpec SERVER_SPEC = SERVER_BUILDER.build();

    // --- Common: per-instance development toggles, not per-world. ---
    private static final ModConfigSpec.Builder COMMON_BUILDER = new ModConfigSpec.Builder();

    /**
     * Sparse combat diagnostics: attack transitions and contact decisions, never per tick.
     * Common rather than server so it lives at {@code config/mhnw-common.toml} and can be set
     * before a world exists.
     */
    public static final ModConfigSpec.BooleanValue DEBUG_COMBAT = COMMON_BUILDER
            .comment("Log monster attack transitions and accepted/rejected contact. Development aid.")
            .define("debugCombat", false);

    public static final ModConfigSpec COMMON_SPEC = COMMON_BUILDER.build();

    private MHNWConfig() {}
}
