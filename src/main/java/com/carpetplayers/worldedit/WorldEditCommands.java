package com.carpetplayers.worldedit;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;

public final class WorldEditCommands {

    private static final String[] DIRECTIONS = {"up", "down", "north", "south", "east", "west"};

    private WorldEditCommands() {}

    public static void registerWorldEditCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("carpetplayers")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("we")
                                .then(Commands.literal("wand").executes(WorldEditCommands::wand))
                                .then(Commands.literal("pos1")
                                        .executes(WorldEditCommands::pos1)
                                        .then(Commands.argument("x", IntegerArgumentType.integer())
                                                .then(Commands.argument("y", IntegerArgumentType.integer())
                                                        .then(Commands.argument("z", IntegerArgumentType.integer())
                                                                .executes(WorldEditCommands::pos1XYZ)))))
                                .then(Commands.literal("pos2")
                                        .executes(WorldEditCommands::pos2)
                                        .then(Commands.argument("x", IntegerArgumentType.integer())
                                                .then(Commands.argument("y", IntegerArgumentType.integer())
                                                        .then(Commands.argument("z", IntegerArgumentType.integer())
                                                                .executes(WorldEditCommands::pos2XYZ)))))
                                .then(Commands.literal("expand")
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                .executes(ctx -> expand(ctx, null))
                                                .then(Commands.argument("direction", StringArgumentType.word())
                                                        .suggests((ctx, builder) ->
                                                                SharedSuggestionProvider.suggest(Arrays.asList(DIRECTIONS), builder))
                                                        .executes(ctx -> expand(ctx, StringArgumentType.getString(ctx, "direction"))))))
                                .then(Commands.literal("contract")
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                .executes(ctx -> contract(ctx, null))
                                                .then(Commands.argument("direction", StringArgumentType.word())
                                                        .suggests((ctx, builder) ->
                                                                SharedSuggestionProvider.suggest(Arrays.asList(DIRECTIONS), builder))
                                                        .executes(ctx -> contract(ctx, StringArgumentType.getString(ctx, "direction"))))))
                                .then(Commands.literal("sel").executes(WorldEditCommands::sel))
                                .then(Commands.literal("set")
                                        .then(Commands.argument("block", StringArgumentType.word())
                                                .executes(WorldEditCommands::set)))
                                .then(Commands.literal("replace")
                                        .then(Commands.argument("from", StringArgumentType.word())
                                                .then(Commands.argument("to", StringArgumentType.word())
                                                        .executes(WorldEditCommands::replace))))
                                .then(Commands.literal("overlay")
                                        .then(Commands.argument("block", StringArgumentType.word())
                                                .executes(WorldEditCommands::overlay)))
                                .then(Commands.literal("smooth")
                                        .executes(ctx -> smooth(ctx, 1))
                                        .then(Commands.argument("iterations", IntegerArgumentType.integer(1, 10))
                                                .executes(ctx -> smooth(ctx, IntegerArgumentType.getInteger(ctx, "iterations")))))
                                .then(Commands.literal("sphere")
                                        .then(Commands.argument("block", StringArgumentType.word())
                                                .then(Commands.argument("radius", IntegerArgumentType.integer(1, 100))
                                                        .executes(WorldEditCommands::sphere))))
                                .then(Commands.literal("hsphere")
                                        .then(Commands.argument("block", StringArgumentType.word())
                                                .then(Commands.argument("radius", IntegerArgumentType.integer(1, 100))
                                                        .executes(WorldEditCommands::hsphere))))
                                .then(Commands.literal("cyl")
                                        .then(Commands.argument("block", StringArgumentType.word())
                                                .then(Commands.argument("radius", IntegerArgumentType.integer(1, 100))
                                                        .executes(ctx -> cyl(ctx, 1))
                                                        .then(Commands.argument("height", IntegerArgumentType.integer(1, 256))
                                                                .executes(ctx -> cyl(ctx, IntegerArgumentType.getInteger(ctx, "height")))))))
                                .then(Commands.literal("hcyl")
                                        .then(Commands.argument("block", StringArgumentType.word())
                                                .then(Commands.argument("radius", IntegerArgumentType.integer(1, 100))
                                                        .executes(ctx -> hcyl(ctx, 1))
                                                        .then(Commands.argument("height", IntegerArgumentType.integer(1, 256))
                                                                .executes(ctx -> hcyl(ctx, IntegerArgumentType.getInteger(ctx, "height")))))))
                                .then(Commands.literal("pyramid")
                                        .then(Commands.argument("block", StringArgumentType.word())
                                                .then(Commands.argument("size", IntegerArgumentType.integer(1, 100))
                                                        .executes(WorldEditCommands::pyramid))))
                                .then(Commands.literal("hpyramid")
                                        .then(Commands.argument("block", StringArgumentType.word())
                                                .then(Commands.argument("size", IntegerArgumentType.integer(1, 100))
                                                        .executes(WorldEditCommands::hpyramid))))
                                .then(Commands.literal("copy").executes(WorldEditCommands::copy))
                                .then(Commands.literal("cut").executes(WorldEditCommands::cut))
                                .then(Commands.literal("paste").executes(WorldEditCommands::paste))
                                .then(Commands.literal("rotate")
                                        .then(Commands.argument("angle", IntegerArgumentType.integer())
                                                .executes(WorldEditCommands::rotate)))
                                .then(Commands.literal("flip")
                                        .then(Commands.argument("direction", StringArgumentType.word())
                                                .suggests((ctx, builder) ->
                                                        SharedSuggestionProvider.suggest(Arrays.asList("horizontal", "vertical"), builder))
                                                .executes(WorldEditCommands::flip)))
                                .then(Commands.literal("stack")
                                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 100))
                                                .executes(ctx -> stack(ctx, null))
                                                .then(Commands.argument("direction", StringArgumentType.word())
                                                        .suggests((ctx, builder) ->
                                                                SharedSuggestionProvider.suggest(Arrays.asList(DIRECTIONS), builder))
                                                        .executes(ctx -> stack(ctx, StringArgumentType.getString(ctx, "direction"))))))
                                .then(Commands.literal("drain")
                                        .executes(ctx -> drain(ctx, 10))
                                        .then(Commands.argument("radius", IntegerArgumentType.integer(1, 100))
                                                .executes(ctx -> drain(ctx, IntegerArgumentType.getInteger(ctx, "radius")))))
                                .then(Commands.literal("butcher")
                                        .executes(ctx -> butcher(ctx, 10))
                                        .then(Commands.argument("radius", IntegerArgumentType.integer(1, 100))
                                                .executes(ctx -> butcher(ctx, IntegerArgumentType.getInteger(ctx, "radius")))))
                                .then(Commands.literal("fill")
                                        .then(Commands.argument("block", StringArgumentType.word())
                                                .executes(ctx -> fill(ctx, 5))
                                                .then(Commands.argument("radius", IntegerArgumentType.integer(1, 100))
                                                        .executes(ctx -> fill(ctx, IntegerArgumentType.getInteger(ctx, "radius"))))))
                                .then(Commands.literal("naturalize").executes(WorldEditCommands::naturalize))
                                .then(Commands.literal("size").executes(WorldEditCommands::size))
                                .then(Commands.literal("count")
                                        .then(Commands.argument("block", StringArgumentType.word())
                                                .executes(WorldEditCommands::count)))
                                .then(Commands.literal("undo").executes(WorldEditCommands::undo))
                                .then(Commands.literal("redo").executes(WorldEditCommands::redo))
                        )
        );
    }

    // ------------------------------------------------------------------
    // Handlers
    // ------------------------------------------------------------------

    private static ServerPlayer getPlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return context.getSource().getPlayerOrException();
    }

    private static void sendResult(ServerPlayer player, String message) {
        player.sendMessage(new TextComponent("[WE] " + message), player.getUUID());
    }

    private static int wand(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = getPlayer(context);
        sendResult(player, WorldEditTools.wand(player));
        return 1;
    }

    private static int pos1(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = getPlayer(context);
        sendResult(player, WorldEditTools.pos1(player, null));
        return 1;
    }

    private static int pos1XYZ(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = getPlayer(context);
        int x = IntegerArgumentType.getInteger(context, "x");
        int y = IntegerArgumentType.getInteger(context, "y");
        int z = IntegerArgumentType.getInteger(context, "z");
        sendResult(player, WorldEditTools.pos1(player, new BlockPos(x, y, z)));
        return 1;
    }

    private static int pos2(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = getPlayer(context);
        sendResult(player, WorldEditTools.pos2(player, null));
        return 1;
    }

    private static int pos2XYZ(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = getPlayer(context);
        int x = IntegerArgumentType.getInteger(context, "x");
        int y = IntegerArgumentType.getInteger(context, "y");
        int z = IntegerArgumentType.getInteger(context, "z");
        sendResult(player, WorldEditTools.pos2(player, new BlockPos(x, y, z)));
        return 1;
    }

    private static int expand(CommandContext<CommandSourceStack> context, String direction) throws CommandSyntaxException {
        ServerPlayer player = getPlayer(context);
        int amount = IntegerArgumentType.getInteger(context, "amount");
        sendResult(player, WorldEditTools.expand(player, amount, direction));
        return 1;
    }

    private static int contract(CommandContext<CommandSourceStack> context, String direction) throws CommandSyntaxException {
        ServerPlayer player = getPlayer(context);
        int amount = IntegerArgumentType.getInteger(context, "amount");
        sendResult(player, WorldEditTools.contract(player, amount, direction));
        return 1;
    }

    private static int sel(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = getPlayer(context);
        sendResult(player, WorldEditTools.selDesel(player));
        return 1;
    }

    private static int set(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = getPlayer(context);
        String block = StringArgumentType.getString(context, "block");
        sendResult(player, WorldEditTools.set(player, block));
        return 1;
    }

    private static int replace(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = getPlayer(context);
        String from = StringArgumentType.getString(context, "from");
        String to = StringArgumentType.getString(context, "to");
        sendResult(player, WorldEditTools.replace(player, from, to));
        return 1;
    }

    private static int overlay(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = getPlayer(context);
        String block = StringArgumentType.getString(context, "block");
        sendResult(player, WorldEditTools.overlay(player, block));
        return 1;
    }

    private static int smooth(CommandContext<CommandSourceStack> context, int iterations) throws CommandSyntaxException {
        ServerPlayer player = getPlayer(context);
        sendResult(player, WorldEditTools.smooth(player, iterations));
        return 1;
    }

    private static int sphere(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = getPlayer(context);
        String block = StringArgumentType.getString(context, "block");
        int radius = IntegerArgumentType.getInteger(context, "radius");
        sendResult(player, WorldEditTools.sphere(player, block, radius));
        return 1;
    }

    private static int hsphere(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = getPlayer(context);
        String block = StringArgumentType.getString(context, "block");
        int radius = IntegerArgumentType.getInteger(context, "radius");
        sendResult(player, WorldEditTools.hollowSphere(player, block, radius));
        return 1;
    }

    private static int cyl(CommandContext<CommandSourceStack> context, int height) throws CommandSyntaxException {
        ServerPlayer player = getPlayer(context);
        String block = StringArgumentType.getString(context, "block");
        int radius = IntegerArgumentType.getInteger(context, "radius");
        sendResult(player, WorldEditTools.cylinder(player, block, radius, height));
        return 1;
    }

    private static int hcyl(CommandContext<CommandSourceStack> context, int height) throws CommandSyntaxException {
        ServerPlayer player = getPlayer(context);
        String block = StringArgumentType.getString(context, "block");
        int radius = IntegerArgumentType.getInteger(context, "radius");
        sendResult(player, WorldEditTools.hollowCylinder(player, block, radius, height));
        return 1;
    }

    private static int pyramid(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = getPlayer(context);
        String block = StringArgumentType.getString(context, "block");
        int size = IntegerArgumentType.getInteger(context, "size");
        sendResult(player, WorldEditTools.pyramid(player, block, size));
        return 1;
    }

    private static int hpyramid(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = getPlayer(context);
        String block = StringArgumentType.getString(context, "block");
        int size = IntegerArgumentType.getInteger(context, "size");
        sendResult(player, WorldEditTools.hollowPyramid(player, block, size));
        return 1;
    }

    private static int copy(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = getPlayer(context);
        sendResult(player, WorldEditTools.copy(player));
        return 1;
    }

    private static int cut(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = getPlayer(context);
        sendResult(player, WorldEditTools.cut(player));
        return 1;
    }

    private static int paste(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = getPlayer(context);
        sendResult(player, WorldEditTools.paste(player));
        return 1;
    }

    private static int rotate(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = getPlayer(context);
        int angle = IntegerArgumentType.getInteger(context, "angle");
        sendResult(player, WorldEditTools.rotate(player, angle));
        return 1;
    }

    private static int flip(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = getPlayer(context);
        String direction = StringArgumentType.getString(context, "direction");
        sendResult(player, WorldEditTools.flip(player, direction));
        return 1;
    }

    private static int stack(CommandContext<CommandSourceStack> context, String direction) throws CommandSyntaxException {
        ServerPlayer player = getPlayer(context);
        int count = IntegerArgumentType.getInteger(context, "count");
        sendResult(player, WorldEditTools.stack(player, count, direction));
        return 1;
    }

    private static int drain(CommandContext<CommandSourceStack> context, int radius) throws CommandSyntaxException {
        ServerPlayer player = getPlayer(context);
        sendResult(player, WorldEditTools.drain(player, radius));
        return 1;
    }

    private static int butcher(CommandContext<CommandSourceStack> context, int radius) throws CommandSyntaxException {
        ServerPlayer player = getPlayer(context);
        sendResult(player, WorldEditTools.butcher(player, radius));
        return 1;
    }

    private static int fill(CommandContext<CommandSourceStack> context, int radius) throws CommandSyntaxException {
        ServerPlayer player = getPlayer(context);
        String block = StringArgumentType.getString(context, "block");
        sendResult(player, WorldEditTools.fill(player, block, radius));
        return 1;
    }

    private static int naturalize(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = getPlayer(context);
        sendResult(player, WorldEditTools.naturalize(player));
        return 1;
    }

    private static int size(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = getPlayer(context);
        sendResult(player, WorldEditTools.size(player));
        return 1;
    }

    private static int count(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = getPlayer(context);
        String block = StringArgumentType.getString(context, "block");
        sendResult(player, WorldEditTools.count(player, block));
        return 1;
    }

    private static int undo(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = getPlayer(context);
        sendResult(player, WorldEditTools.undo(player));
        return 1;
    }

    private static int redo(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = getPlayer(context);
        sendResult(player, WorldEditTools.redo(player));
        return 1;
    }
}