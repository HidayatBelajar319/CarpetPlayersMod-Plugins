package com.carpetplayers.waypoint;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Registers all /carpetplayers waypoint subcommands.
 * Also accessible via /cp waypoint and /cps waypoint.
 */
public final class WaypointCommands {

    private WaypointCommands() {}

    /**
     * Build the "waypoint" literal branch — called from BotManager.registerCommands.
     */
    public static void registerWaypointCommands(
            CommandDispatcher<CommandSourceStack> dispatcher,
            com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> parent) {

        parent.then(Commands.literal("waypoint")
                .then(Commands.literal("add")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(WaypointCommands::addAtPlayer)
                                .then(Commands.argument("x", DoubleArgumentType.doubleArg())
                                        .then(Commands.argument("y", DoubleArgumentType.doubleArg())
                                                .then(Commands.argument("z", DoubleArgumentType.doubleArg())
                                                        .executes(WaypointCommands::addAtCoords))))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests((ctx, b) -> waypointSuggestions(ctx, b))
                                .executes(WaypointCommands::remove)))
                .then(Commands.literal("list")
                        .executes(WaypointCommands::list))
                .then(Commands.literal("color")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests((ctx, b) -> waypointSuggestions(ctx, b))
                                .then(Commands.argument("color", StringArgumentType.word())
                                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                                                Arrays.asList("red", "green", "blue", "yellow", "purple", "orange", "white", "cyan"), b))
                                        .executes(WaypointCommands::color))))
                .then(Commands.literal("enable")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests((ctx, b) -> waypointSuggestions(ctx, b))
                                .executes(WaypointCommands::enable)))
                .then(Commands.literal("disable")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests((ctx, b) -> waypointSuggestions(ctx, b))
                                .executes(WaypointCommands::disable)))
                .then(Commands.literal("tp")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests((ctx, b) -> waypointSuggestions(ctx, b))
                                .executes(WaypointCommands::teleport)))
                .then(Commands.literal("here")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests((ctx, b) -> waypointSuggestions(ctx, b))
                                .executes(WaypointCommands::setHere)))
        );
    }

    // ======================== COMMAND HANDLERS ========================

    /** /cp waypoint add <name> — add at player's current position */
    private static int addAtPlayer(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(ctx, "name");
        UUID uuid = player.getUUID();
        String dim = WaypointManager.dimensionToString(player.getLevel().dimension());

        Waypoint wp = new Waypoint(name, player.getX(), player.getY(), player.getZ(), dim);
        if (WaypointManager.addWaypoint(uuid, wp)) {
            ctx.getSource().sendSuccess(
                    new TextComponent("[Waypoint] Added '" + name + "' at " + wp.coordString()), true);
            return 1;
        } else {
            ctx.getSource().sendFailure(new TextComponent("[Waypoint] A waypoint named '" + name + "' already exists"));
            return 0;
        }
    }

    /** /cp waypoint add <name> <x> <y> <z> — add at specific coords */
    private static int addAtCoords(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(ctx, "name");
        double x = DoubleArgumentType.getDouble(ctx, "x");
        double y = DoubleArgumentType.getDouble(ctx, "y");
        double z = DoubleArgumentType.getDouble(ctx, "z");
        UUID uuid = player.getUUID();
        String dim = WaypointManager.dimensionToString(player.getLevel().dimension());

        Waypoint wp = new Waypoint(name, x, y, z, dim);
        if (WaypointManager.addWaypoint(uuid, wp)) {
            ctx.getSource().sendSuccess(
                    new TextComponent("[Waypoint] Added '" + name + "' at " + wp.coordString()), true);
            return 1;
        } else {
            ctx.getSource().sendFailure(new TextComponent("[Waypoint] A waypoint named '" + name + "' already exists"));
            return 0;
        }
    }

    /** /cp waypoint remove <name> */
    private static int remove(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(ctx, "name");
        Waypoint removed = WaypointManager.removeWaypoint(player.getUUID(), name);
        if (removed != null) {
            ctx.getSource().sendSuccess(
                    new TextComponent("[Waypoint] Removed '" + removed.getName() + "'"), true);
            return 1;
        } else {
            ctx.getSource().sendFailure(new TextComponent("[Waypoint] Waypoint '" + name + "' not found"));
            return 0;
        }
    }

    /** /cp waypoint list */
    private static int list(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        List<Waypoint> waypoints = WaypointManager.getWaypoints(player.getUUID());
        if (waypoints.isEmpty()) {
            ctx.getSource().sendSuccess(new TextComponent("[Waypoint] No waypoints set"), false);
            return 0;
        }
        StringBuilder sb = new StringBuilder("[Waypoint] Your waypoints (" + waypoints.size() + "):");
        for (int i = 0; i < waypoints.size(); i++) {
            Waypoint wp = waypoints.get(i);
            String status = wp.isEnabled() ? "a" : "7";
            String deathTag = wp.isDeath() ? " c[Death]" : "";
            sb.append("\n  \u00a7").append(status)
              .append(i + 1).append(". ").append(wp.getName())
              .append(" \u00a7r[").append(Waypoint.colorName(wp.getColor())).append("]")
              .append(deathTag)
              .append(" ").append(wp.coordString());
        }
        ctx.getSource().sendSuccess(new TextComponent(sb.toString()), false);
        return waypoints.size();
    }

    /** /cp waypoint color <name> <color> */
    private static int color(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(ctx, "name");
        String colorStr = StringArgumentType.getString(ctx, "color");
        int color = Waypoint.resolveColor(colorStr);
        if (color == -1) {
            ctx.getSource().sendFailure(
                    new TextComponent("[Waypoint] Unknown color '" + colorStr + "'. Use: red, green, blue, yellow, purple, orange, white, cyan"));
            return 0;
        }
        if (WaypointManager.setColor(player.getUUID(), name, color)) {
            ctx.getSource().sendSuccess(
                    new TextComponent("[Waypoint] Color of '" + name + "' set to " + colorStr), true);
            return 1;
        } else {
            ctx.getSource().sendFailure(new TextComponent("[Waypoint] Waypoint '" + name + "' not found"));
            return 0;
        }
    }

    /** /cp waypoint enable <name> */
    private static int enable(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(ctx, "name");
        if (WaypointManager.setEnabled(player.getUUID(), name, true)) {
            ctx.getSource().sendSuccess(
                    new TextComponent("[Waypoint] '" + name + "' enabled"), true);
            return 1;
        } else {
            ctx.getSource().sendFailure(new TextComponent("[Waypoint] Waypoint '" + name + "' not found"));
            return 0;
        }
    }

    /** /cp waypoint disable <name> */
    private static int disable(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(ctx, "name");
        if (WaypointManager.setEnabled(player.getUUID(), name, false)) {
            ctx.getSource().sendSuccess(
                    new TextComponent("[Waypoint] '" + name + "' disabled"), true);
            return 1;
        } else {
            ctx.getSource().sendFailure(new TextComponent("[Waypoint] Waypoint '" + name + "' not found"));
            return 0;
        }
    }

    /** /cp waypoint tp <name> */
    private static int teleport(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(ctx, "name");
        Waypoint wp = WaypointManager.findWaypoint(player.getUUID(), name);
        if (wp == null) {
            ctx.getSource().sendFailure(new TextComponent("[Waypoint] Waypoint '" + name + "' not found"));
            return 0;
        }
        if (WaypointManager.teleportTo(player, wp)) {
            ctx.getSource().sendSuccess(
                    new TextComponent("[Waypoint] Teleported to '" + wp.getName() + "'"), true);
            return 1;
        }
        return 0;
    }

    /** /cp waypoint here <name> — move waypoint to current position */
    private static int setHere(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(ctx, "name");
        Waypoint wp = WaypointManager.findWaypoint(player.getUUID(), name);
        if (wp == null) {
            ctx.getSource().sendFailure(new TextComponent("[Waypoint] Waypoint '" + name + "' not found"));
            return 0;
        }
        wp.setX(player.getX());
        wp.setY(player.getY());
        wp.setZ(player.getZ());
        wp.setDimension(WaypointManager.dimensionToString(player.getLevel().dimension()));
        WaypointManager.savePlayer(player.getUUID());
        ctx.getSource().sendSuccess(
                new TextComponent("[Waypoint] Moved '" + name + "' to your position " + wp.coordString()), true);
        return 1;
    }

    // ======================== SUGGESTIONS ========================

    private static CompletableFuture<Suggestions> waypointSuggestions(
            CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            List<Waypoint> waypoints = WaypointManager.getWaypoints(player.getUUID());
            String remaining = builder.getRemaining().toLowerCase();
            for (Waypoint wp : waypoints) {
                if (wp.getName().toLowerCase().startsWith(remaining)) {
                    builder.suggest(wp.getName());
                }
            }
        } catch (CommandSyntaxException ignored) {}
        return builder.buildFuture();
    }
}
