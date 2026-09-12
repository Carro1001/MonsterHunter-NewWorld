package com.carro1001.mhnw;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * {@code /mhnw debugcombat [true|false]}: toggles {@link MHNWConfig#DEBUG_COMBAT} in-game.
 *
 * <p>Exists because the alternative is editing {@code config/mhnw-common.toml} and restarting the
 * server, which is exactly the kind of thing that gets forgotten mid-session. {@code set()} both
 * changes the live value immediately and persists it to that same file, so this is not a separate
 * mechanism from the config, just a faster way to flip it.
 */
final class MHNWCommands {

    static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mhnw")
                .then(Commands.literal("debugcombat")
                        .requires(source -> source.hasPermission(2))
                        .executes(ctx -> {
                            boolean next = !MHNWConfig.DEBUG_COMBAT.get();
                            MHNWConfig.DEBUG_COMBAT.set(next);
                            report(ctx.getSource(), next);
                            return 1;
                        })
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(ctx -> {
                                    boolean enabled = BoolArgumentType.getBool(ctx, "enabled");
                                    MHNWConfig.DEBUG_COMBAT.set(enabled);
                                    report(ctx.getSource(), enabled);
                                    return 1;
                                }))));
    }

    private static void report(CommandSourceStack source, boolean enabled) {
        source.sendSuccess(() -> Component.literal(
                "mhnw debugCombat is now " + (enabled ? "ON" : "off")), true);
    }

    private MHNWCommands() {}
}
