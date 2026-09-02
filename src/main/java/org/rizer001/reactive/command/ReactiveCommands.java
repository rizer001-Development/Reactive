package org.rizer001.reactive.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import org.rizer001.reactive.config.ReactiveConfig;
import org.rizer001.reactive.gamerules.ReactiveGameRuleManager;

import java.util.ArrayList;
import java.util.List;

/**
 * /reactive command — help, reload, etc.
 */
public class ReactiveCommands {

    private static final int PAGE_SIZE = 8;

    private static final String[][] HELP_PAGES = {
        {
            "/reactive help [page] — Show this help",
            "/reactive reload     — Reload all configs (except server.toml)",
            "/reactive gamerules  — Reload game rules from gamerules.toml"
        }
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("reactive")
                .executes(ctx -> showHelp(ctx, 1))
                .then(Commands.literal("help")
                    .executes(ctx -> showHelp(ctx, 1))
                    .then(Commands.argument("page", IntegerArgumentType.integer(1))
                        .executes(ctx -> {
                            int page = IntegerArgumentType.getInteger(ctx, "page");
                            return showHelp(ctx, page);
                        })
                    )
                )
                .then(Commands.literal("reload")
                    .executes(ctx -> reload(ctx.getSource()))
                )
                .then(Commands.literal("gamerules")
                    .executes(ctx -> reloadGameRules(ctx.getSource()))
                )
        );
    }

    private static int showHelp(CommandContext<CommandSourceStack> ctx, int requestedPage) {
        List<String> allLines = new ArrayList<>();
        for (String[] section : HELP_PAGES) {
            for (String line : section) {
                allLines.add(line);
            }
        }

        int totalPages = Math.max(1, (int) Math.ceil((double) allLines.size() / PAGE_SIZE));
        final int page = Math.max(1, Math.min(requestedPage, totalPages));

        int start = (page - 1) * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, allLines.size());

        CommandSourceStack source = ctx.getSource();

        source.sendSuccess(() -> Component.literal("--- Reactive Help (page " + page + "/" + totalPages + ") ---").withStyle(ChatFormatting.GOLD), false);

        for (int i = start; i < end; i++) {
            final String line = allLines.get(i);
            source.sendSuccess(() -> Component.literal(line).withStyle(ChatFormatting.YELLOW), false);
        }

        if (totalPages > 1) {
            final int fPage = page;
            final int fTotal = totalPages;
            source.sendSuccess(() -> {
                MutableComponent nav = Component.empty();
                if (fPage > 1) {
                    nav.append(Component.literal("<< Prev ").withStyle(ChatFormatting.AQUA));
                }
                nav.append(Component.literal("[" + fPage + "/" + fTotal + "]").withStyle(ChatFormatting.WHITE));
                if (fPage < fTotal) {
                    nav.append(Component.literal(" Next >>").withStyle(ChatFormatting.AQUA));
                }
                return nav;
            }, false);
        }

        return 1;
    }

    private static int reload(CommandSourceStack source) {
        MinecraftServer server = source.getServer();

        ReactiveConfig.reload();

        var playerList = server.getPlayerList();
        if (playerList != null) {
            playerList.getOps().reload();
            playerList.getWhiteList().reload();
            playerList.getBans().reload();
            playerList.getIpBans().reload();
        }

        source.sendSuccess(() -> Component.literal("[Reactive] Configs reloaded!").withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int reloadGameRules(CommandSourceStack source) {
        MinecraftServer server = source.getServer();

        ReactiveGameRuleManager.reload(server);

        source.sendSuccess(() -> Component.literal("[Reactive] Game rules reloaded from gamerules.toml!").withStyle(ChatFormatting.GREEN), true);
        return 1;
    }
}
