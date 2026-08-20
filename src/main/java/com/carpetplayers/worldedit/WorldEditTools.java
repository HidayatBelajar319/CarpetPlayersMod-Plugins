package com.carpetplayers.worldedit;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * All WorldEdit tool implementations. Each method takes a ServerPlayer and
 * returns a descriptive String result message.
 */
public final class WorldEditTools {

    private WorldEditTools() {}

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Parse a block name. Accepts "minecraft:stone" or "stone" (adds the
     * minecraft: prefix automatically). Returns null if the block is unknown.
     */
    public static Block parseBlock(String name) {
        if (name == null) return null;
        String n = name.trim().toLowerCase();
        if (!n.contains(":")) n = "minecraft:" + n;
        try {
            ResourceLocation id = new ResourceLocation(n);
            if (Registry.BLOCK.containsKey(id)) {
                return Registry.BLOCK.get(id);
            }
        } catch (Exception ignored) {
            // invalid resource location
        }
        return null;
    }

    private static void sendMessage(ServerPlayer player, String message) {
        player.sendMessage(new TextComponent("[WE] " + message), player.getUUID());
    }

    private static String defaultDirection(ServerPlayer player) {
        return player.getDirection().getName();
    }

    private static Level level(ServerPlayer player) {
        return player.getLevel();
    }

    // ------------------------------------------------------------------
    // Selection
    // ------------------------------------------------------------------

    public static String wand(ServerPlayer player) {
        ItemStack axe = new ItemStack(Items.WOODEN_AXE);
        if (!player.inventory.add(axe)) {
            player.drop(axe, false);
        }
        return "Wooden axe given - left click to set pos1, right click to set pos2";
    }

    public static String pos1(ServerPlayer player, BlockPos pos) {
        BlockPos p = pos != null ? pos : player.blockPosition();
        WorldEditManager.getSelection(player).pos1 = p;
        return "Position 1 set to (" + p.getX() + ", " + p.getY() + ", " + p.getZ() + ")";
    }

    public static String pos2(ServerPlayer player, BlockPos pos) {
        BlockPos p = pos != null ? pos : player.blockPosition();
        WorldEditManager.getSelection(player).pos2 = p;
        return "Position 2 set to (" + p.getX() + ", " + p.getY() + ", " + p.getZ() + ")";
    }

    public static String expand(ServerPlayer player, int amount, String direction) {
        WorldEditManager.SelectionData sel = WorldEditManager.getSelection(player);
        if (!sel.isComplete()) return "Selection incomplete - set both positions first";
        BlockPos min = sel.getMin();
        BlockPos max = sel.getMax();
        String dir = direction == null || direction.isEmpty() ? defaultDirection(player) : direction.toLowerCase();
        switch (dir) {
            case "up": max = max.offset(0, amount, 0); break;
            case "down": min = min.offset(0, -amount, 0); break;
            case "north": min = min.offset(0, 0, -amount); break;
            case "south": max = max.offset(0, 0, amount); break;
            case "west": min = min.offset(-amount, 0, 0); break;
            case "east": max = max.offset(amount, 0, 0); break;
            default: return "Invalid direction: " + direction + " (use up/down/north/south/east/west)";
        }
        sel.pos1 = min;
        sel.pos2 = max;
        return "Selection expanded by " + amount + " toward " + dir;
    }

    public static String contract(ServerPlayer player, int amount, String direction) {
        WorldEditManager.SelectionData sel = WorldEditManager.getSelection(player);
        if (!sel.isComplete()) return "Selection incomplete - set both positions first";
        BlockPos min = sel.getMin();
        BlockPos max = sel.getMax();
        String dir = direction == null || direction.isEmpty() ? defaultDirection(player) : direction.toLowerCase();
        switch (dir) {
            case "up": max = max.offset(0, -amount, 0); break;
            case "down": min = min.offset(0, amount, 0); break;
            case "north": min = min.offset(0, 0, amount); break;
            case "south": max = max.offset(0, 0, -amount); break;
            case "west": min = min.offset(amount, 0, 0); break;
            case "east": max = max.offset(-amount, 0, 0); break;
            default: return "Invalid direction: " + direction + " (use up/down/north/south/east/west)";
        }
        if (min.getX() > max.getX() || min.getY() > max.getY() || min.getZ() > max.getZ()) {
            return "Cannot contract further in that direction";
        }
        sel.pos1 = min;
        sel.pos2 = max;
        return "Selection contracted by " + amount + " toward " + dir;
    }

    public static String selDesel(ServerPlayer player) {
        WorldEditManager.clearSelection(player);
        return "Selection cleared";
    }

    // ------------------------------------------------------------------
    // Generation
    // ------------------------------------------------------------------

    public static String set(ServerPlayer player, String blockName) {
        Block block = parseBlock(blockName);
        if (block == null) return "Unknown block: " + blockName;
        WorldEditManager.SelectionData sel = WorldEditManager.getSelection(player);
        if (!sel.isComplete()) return "Selection incomplete - set both positions first";
        Level world = level(player);
        BlockPos min = sel.getMin();
        BlockPos max = sel.getMax();
        List<CompoundTag> before = WorldEditManager.snapshotRegion(world, min, max);
        BlockState state = block.defaultBlockState();
        int count = 0;
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            world.setBlockAndUpdate(pos, state);
            count++;
        }
        WorldEditManager.pushUndo(player, before, min, max);
        return "Set " + count + " blocks to " + blockName;
    }

    public static String replace(ServerPlayer player, String fromName, String toName) {
        Block from = parseBlock(fromName);
        if (from == null) return "Unknown block: " + fromName;
        Block to = parseBlock(toName);
        if (to == null) return "Unknown block: " + toName;
        WorldEditManager.SelectionData sel = WorldEditManager.getSelection(player);
        if (!sel.isComplete()) return "Selection incomplete - set both positions first";
        Level world = level(player);
        BlockPos min = sel.getMin();
        BlockPos max = sel.getMax();
        List<CompoundTag> before = WorldEditManager.snapshotRegion(world, min, max);
        int count = 0;
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (world.getBlockState(pos).getBlock() == from) {
                world.setBlockAndUpdate(pos, to.defaultBlockState());
                count++;
            }
        }
        WorldEditManager.pushUndo(player, before, min, max);
        return "Replaced " + count + " " + fromName + " with " + toName;
    }

    public static String overlay(ServerPlayer player, String blockName) {
        Block block = parseBlock(blockName);
        if (block == null) return "Unknown block: " + blockName;
        WorldEditManager.SelectionData sel = WorldEditManager.getSelection(player);
        if (!sel.isComplete()) return "Selection incomplete - set both positions first";
        Level world = level(player);
        BlockPos min = sel.getMin();
        BlockPos max = sel.getMax();
        List<CompoundTag> before = WorldEditManager.snapshotRegion(world, min, max);
        int count = 0;
        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int z = min.getZ(); z <= max.getZ(); z++) {
                int topY = world.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                if (topY >= min.getY() && topY <= max.getY()) {
                    world.setBlockAndUpdate(new BlockPos(x, topY, z).above(), block.defaultBlockState());
                    count++;
                }
            }
        }
        WorldEditManager.pushUndo(player, before, min, max);
        return "Overlaid " + count + " blocks with " + blockName;
    }

    public static String smooth(ServerPlayer player, int iterations) {
        WorldEditManager.SelectionData sel = WorldEditManager.getSelection(player);
        if (!sel.isComplete()) return "Selection incomplete - set both positions first";
        Level world = level(player);
        BlockPos min = sel.getMin();
        BlockPos max = sel.getMax();
        int w = max.getX() - min.getX() + 1;
        int d = max.getZ() - min.getZ() + 1;
        List<CompoundTag> before = WorldEditManager.snapshotRegion(world, min, max);
        int[][] heights = new int[w][d];
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < d; z++) {
                heights[x][z] = world.getHeight(Heightmap.Types.MOTION_BLOCKING, min.getX() + x, min.getZ() + z);
            }
        }
        for (int iter = 0; iter < iterations; iter++) {
            int[][] next = new int[w][d];
            for (int x = 0; x < w; x++) {
                for (int z = 0; z < d; z++) {
                    int sum = 0;
                    int count = 0;
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            int nx = x + dx;
                            int nz = z + dz;
                            if (nx >= 0 && nx < w && nz >= 0 && nz < d) {
                                sum += heights[nx][nz];
                                count++;
                            }
                        }
                    }
                    next[x][z] = count > 0 ? sum / count : heights[x][z];
                }
            }
            heights = next;
        }
        int changed = 0;
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < d; z++) {
                int target = heights[x][z];
                int current = world.getHeight(Heightmap.Types.MOTION_BLOCKING, min.getX() + x, min.getZ() + z);
                if (target > current) {
                    for (int y = current + 1; y <= target; y++) {
                        world.setBlockAndUpdate(new BlockPos(min.getX() + x, y, min.getZ() + z), Blocks.DIRT.defaultBlockState());
                        changed++;
                    }
                } else if (target < current) {
                    for (int y = target + 1; y <= current; y++) {
                        world.setBlockAndUpdate(new BlockPos(min.getX() + x, y, min.getZ() + z), Blocks.AIR.defaultBlockState());
                        changed++;
                    }
                }
            }
        }
        WorldEditManager.pushUndo(player, before, min, max);
        return "Smoothed terrain (" + iterations + " iteration(s), " + changed + " blocks changed)";
    }

    public static String deform(ServerPlayer player, String expression) {
        WorldEditManager.SelectionData sel = WorldEditManager.getSelection(player);
        if (!sel.isComplete()) return "Selection incomplete - set both positions first";
        Level world = level(player);
        BlockPos min = sel.getMin();
        BlockPos max = sel.getMax();
        String expr = expression == null ? "" : expression.trim().toLowerCase();
        int dx = 0, dy = 0, dz = 0;
        if (expr.equals("+x") || expr.equals("x")) dx = 1;
        else if (expr.equals("-x")) dx = -1;
        else if (expr.equals("+y") || expr.equals("y")) dy = 1;
        else if (expr.equals("-y")) dy = -1;
        else if (expr.equals("+z") || expr.equals("z")) dz = 1;
        else if (expr.equals("-z")) dz = -1;
        else return "Unsupported deform expression: " + expression + " (use +x, -x, +y, -y, +z, -z)";

        List<CompoundTag> before = WorldEditManager.snapshotRegion(world, min, max);
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            blocks.put(pos.immutable(), world.getBlockState(pos));
        }
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            world.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
        int count = 0;
        for (Map.Entry<BlockPos, BlockState> e : blocks.entrySet()) {
            BlockPos shifted = e.getKey().offset(dx, dy, dz);
            if (shifted.getX() >= min.getX() && shifted.getX() <= max.getX()
                    && shifted.getY() >= min.getY() && shifted.getY() <= max.getY()
                    && shifted.getZ() >= min.getZ() && shifted.getZ() <= max.getZ()) {
                world.setBlockAndUpdate(shifted, e.getValue());
                count++;
            }
        }
        WorldEditManager.pushUndo(player, before, min, max);
        return "Deformed selection by (" + dx + ", " + dy + ", " + dz + ") - moved " + count + " blocks";
    }

    // ------------------------------------------------------------------
    // Shapes (from player position)
    // ------------------------------------------------------------------

    public static String sphere(ServerPlayer player, String blockName, int radius) {
        Block block = parseBlock(blockName);
        if (block == null) return "Unknown block: " + blockName;
        Level world = level(player);
        BlockPos center = player.blockPosition();
        BlockPos min = center.offset(-radius, -radius, -radius);
        BlockPos max = center.offset(radius, radius, radius);
        List<CompoundTag> before = WorldEditManager.snapshotRegion(world, min, max);
        int count = 0;
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            double dist = Math.sqrt(Math.pow(pos.getX() - center.getX(), 2)
                    + Math.pow(pos.getY() - center.getY(), 2)
                    + Math.pow(pos.getZ() - center.getZ(), 2));
            if (dist <= radius) {
                world.setBlockAndUpdate(pos, block.defaultBlockState());
                count++;
            }
        }
        WorldEditManager.pushUndo(player, before, min, max);
        return "Created sphere radius " + radius + " (" + count + " blocks)";
    }

    public static String hollowSphere(ServerPlayer player, String blockName, int radius) {
        Block block = parseBlock(blockName);
        if (block == null) return "Unknown block: " + blockName;
        Level world = level(player);
        BlockPos center = player.blockPosition();
        BlockPos min = center.offset(-radius, -radius, -radius);
        BlockPos max = center.offset(radius, radius, radius);
        List<CompoundTag> before = WorldEditManager.snapshotRegion(world, min, max);
        int count = 0;
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            double dist = Math.sqrt(Math.pow(pos.getX() - center.getX(), 2)
                    + Math.pow(pos.getY() - center.getY(), 2)
                    + Math.pow(pos.getZ() - center.getZ(), 2));
            if (dist <= radius && dist > radius - 1) {
                world.setBlockAndUpdate(pos, block.defaultBlockState());
                count++;
            }
        }
        WorldEditManager.pushUndo(player, before, min, max);
        return "Created hollow sphere radius " + radius + " (" + count + " blocks)";
    }

    public static String cylinder(ServerPlayer player, String blockName, int radius, int height) {
        Block block = parseBlock(blockName);
        if (block == null) return "Unknown block: " + blockName;
        Level world = level(player);
        BlockPos center = player.blockPosition();
        BlockPos min = center.offset(-radius, 0, -radius);
        BlockPos max = center.offset(radius, height - 1, radius);
        List<CompoundTag> before = WorldEditManager.snapshotRegion(world, min, max);
        int count = 0;
        for (int y = 0; y < height; y++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dz * dz <= radius * radius) {
                        world.setBlockAndUpdate(center.offset(dx, y, dz), block.defaultBlockState());
                        count++;
                    }
                }
            }
        }
        WorldEditManager.pushUndo(player, before, min, max);
        return "Created cylinder radius " + radius + " height " + height + " (" + count + " blocks)";
    }

    public static String hollowCylinder(ServerPlayer player, String blockName, int radius, int height) {
        Block block = parseBlock(blockName);
        if (block == null) return "Unknown block: " + blockName;
        Level world = level(player);
        BlockPos center = player.blockPosition();
        BlockPos min = center.offset(-radius, 0, -radius);
        BlockPos max = center.offset(radius, height - 1, radius);
        List<CompoundTag> before = WorldEditManager.snapshotRegion(world, min, max);
        int count = 0;
        for (int y = 0; y < height; y++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    int distSq = dx * dx + dz * dz;
                    if (distSq <= radius * radius && distSq > (radius - 1) * (radius - 1)) {
                        world.setBlockAndUpdate(center.offset(dx, y, dz), block.defaultBlockState());
                        count++;
                    }
                }
            }
        }
        WorldEditManager.pushUndo(player, before, min, max);
        return "Created hollow cylinder radius " + radius + " height " + height + " (" + count + " blocks)";
    }

    public static String pyramid(ServerPlayer player, String blockName, int size) {
        Block block = parseBlock(blockName);
        if (block == null) return "Unknown block: " + blockName;
        Level world = level(player);
        BlockPos center = player.blockPosition();
        BlockPos min = center.offset(-(size - 1), 0, -(size - 1));
        BlockPos max = center.offset(size - 1, size - 1, size - 1);
        List<CompoundTag> before = WorldEditManager.snapshotRegion(world, min, max);
        int count = 0;
        for (int y = 0; y < size; y++) {
            int half = size - 1 - y;
            for (int dx = -half; dx <= half; dx++) {
                for (int dz = -half; dz <= half; dz++) {
                    world.setBlockAndUpdate(center.offset(dx, y, dz), block.defaultBlockState());
                    count++;
                }
            }
        }
        WorldEditManager.pushUndo(player, before, min, max);
        return "Created pyramid size " + size + " (" + count + " blocks)";
    }

    public static String hollowPyramid(ServerPlayer player, String blockName, int size) {
        Block block = parseBlock(blockName);
        if (block == null) return "Unknown block: " + blockName;
        Level world = level(player);
        BlockPos center = player.blockPosition();
        BlockPos min = center.offset(-(size - 1), 0, -(size - 1));
        BlockPos max = center.offset(size - 1, size - 1, size - 1);
        List<CompoundTag> before = WorldEditManager.snapshotRegion(world, min, max);
        int count = 0;
        for (int y = 0; y < size; y++) {
            int half = size - 1 - y;
            for (int dx = -half; dx <= half; dx++) {
                for (int dz = -half; dz <= half; dz++) {
                    if (dx == -half || dx == half || dz == -half || dz == half) {
                        world.setBlockAndUpdate(center.offset(dx, y, dz), block.defaultBlockState());
                        count++;
                    }
                }
            }
        }
        WorldEditManager.pushUndo(player, before, min, max);
        return "Created hollow pyramid size " + size + " (" + count + " blocks)";
    }

    // ------------------------------------------------------------------
    // Clipboard
    // ------------------------------------------------------------------

    public static String copy(ServerPlayer player) {
        WorldEditManager.SelectionData sel = WorldEditManager.getSelection(player);
        if (!sel.isComplete()) return "Selection incomplete - set both positions first";
        Level world = level(player);
        BlockPos min = sel.getMin();
        BlockPos max = sel.getMax();
        List<CompoundTag> clip = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            BlockState state = world.getBlockState(pos);
            CompoundTag tag = new CompoundTag();
            tag.putInt("x", pos.getX() - min.getX());
            tag.putInt("y", pos.getY() - min.getY());
            tag.putInt("z", pos.getZ() - min.getZ());
            tag.put("state", NbtUtils.writeBlockState(state));
            clip.add(tag);
        }
        List<CompoundTag> stored = WorldEditManager.getClipboard(player);
        stored.clear();
        stored.addAll(clip);
        WorldEditManager.setClipboardSize(player, new int[]{
                max.getX() - min.getX() + 1,
                max.getY() - min.getY() + 1,
                max.getZ() - min.getZ() + 1});
        return "Copied " + clip.size() + " blocks to clipboard";
    }

    public static String cut(ServerPlayer player) {
        String copyResult = copy(player);
        if (copyResult.startsWith("Selection")) return copyResult;
        WorldEditManager.SelectionData sel = WorldEditManager.getSelection(player);
        Level world = level(player);
        BlockPos min = sel.getMin();
        BlockPos max = sel.getMax();
        List<CompoundTag> before = WorldEditManager.snapshotRegion(world, min, max);
        int count = 0;
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            world.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            count++;
        }
        WorldEditManager.pushUndo(player, before, min, max);
        return "Cut " + count + " blocks to clipboard";
    }

    public static String paste(ServerPlayer player) {
        List<CompoundTag> clip = WorldEditManager.getClipboard(player);
        if (clip.isEmpty()) return "Clipboard is empty";
        Level world = level(player);
        BlockPos origin = player.blockPosition();
        int[] size = WorldEditManager.getClipboardSize(player);
        BlockPos min = origin;
        BlockPos max;
        if (size != null) {
            max = origin.offset(size[0] - 1, size[1] - 1, size[2] - 1);
        } else {
            max = origin;
            for (CompoundTag tag : clip) {
                max = new BlockPos(
                        Math.max(max.getX(), origin.getX() + tag.getInt("x")),
                        Math.max(max.getY(), origin.getY() + tag.getInt("y")),
                        Math.max(max.getZ(), origin.getZ() + tag.getInt("z")));
            }
        }
        List<CompoundTag> before = WorldEditManager.snapshotRegion(world, min, max);
        int count = 0;
        for (CompoundTag tag : clip) {
            BlockPos pos = origin.offset(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
            BlockState state = NbtUtils.readBlockState(tag.getCompound("state"));
            world.setBlockAndUpdate(pos, state);
            count++;
        }
        WorldEditManager.pushUndo(player, before, min, max);
        return "Pasted " + count + " blocks";
    }

    public static String rotate(ServerPlayer player, int angle) {
        List<CompoundTag> clip = WorldEditManager.getClipboard(player);
        if (clip.isEmpty()) return "Clipboard is empty";
        int a = ((angle % 360) + 360) % 360;
        if (a != 90 && a != 180 && a != 270) return "Angle must be 90, 180, or 270";
        int[] size = WorldEditManager.getClipboardSize(player);
        if (size == null) return "Clipboard has no size";
        int w = size[0], h = size[1], d = size[2];
        for (CompoundTag tag : clip) {
            int x = tag.getInt("x");
            int z = tag.getInt("z");
            int nx, nz;
            if (a == 90) { nx = z; nz = (d - 1) - x; }
            else if (a == 180) { nx = (w - 1) - x; nz = (d - 1) - z; }
            else { nx = (d - 1) - z; nz = x; }
            tag.putInt("x", nx);
            tag.putInt("z", nz);
        }
        if (a == 90 || a == 270) {
            WorldEditManager.setClipboardSize(player, new int[]{d, h, w});
        }
        return "Rotated clipboard by " + a + " degrees";
    }

    public static String flip(ServerPlayer player, String direction) {
        List<CompoundTag> clip = WorldEditManager.getClipboard(player);
        if (clip.isEmpty()) return "Clipboard is empty";
        int[] size = WorldEditManager.getClipboardSize(player);
        if (size == null) return "Clipboard has no size";
        String dir = direction == null ? "" : direction.toLowerCase();
        for (CompoundTag tag : clip) {
            int x = tag.getInt("x");
            int z = tag.getInt("z");
            if (dir.equals("horizontal") || dir.equals("east") || dir.equals("west")) {
                tag.putInt("x", (size[0] - 1) - x);
            } else if (dir.equals("vertical") || dir.equals("north") || dir.equals("south")) {
                tag.putInt("z", (size[2] - 1) - z);
            } else {
                return "Invalid flip direction: " + direction + " (use horizontal or vertical)";
            }
        }
        return "Flipped clipboard " + dir;
    }

    public static String stack(ServerPlayer player, int count, String direction) {
        WorldEditManager.SelectionData sel = WorldEditManager.getSelection(player);
        if (!sel.isComplete()) return "Selection incomplete - set both positions first";
        Level world = level(player);
        BlockPos min = sel.getMin();
        BlockPos max = sel.getMax();
        String dir = direction == null || direction.isEmpty() ? defaultDirection(player) : direction.toLowerCase();
        int dx = 0, dy = 0, dz = 0;
        switch (dir) {
            case "up": dy = 1; break;
            case "down": dy = -1; break;
            case "north": dz = -1; break;
            case "south": dz = 1; break;
            case "west": dx = -1; break;
            case "east": dx = 1; break;
            default: return "Invalid direction: " + direction + " (use up/down/north/south/east/west)";
        }
        int stepX = (max.getX() - min.getX() + 1) * dx;
        int stepY = (max.getY() - min.getY() + 1) * dy;
        int stepZ = (max.getZ() - min.getZ() + 1) * dz;
        BlockPos snapMin = new BlockPos(
                Math.min(min.getX(), min.getX() + stepX * count),
                Math.min(min.getY(), min.getY() + stepY * count),
                Math.min(min.getZ(), min.getZ() + stepZ * count));
        BlockPos snapMax = new BlockPos(
                Math.max(max.getX(), max.getX() + stepX * count),
                Math.max(max.getY(), max.getY() + stepY * count),
                Math.max(max.getZ(), max.getZ() + stepZ * count));
        List<CompoundTag> before = WorldEditManager.snapshotRegion(world, snapMin, snapMax);
        Map<BlockPos, BlockState> original = new HashMap<>();
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            original.put(pos.immutable(), world.getBlockState(pos));
        }
        int placed = 0;
        for (int i = 1; i <= count; i++) {
            for (Map.Entry<BlockPos, BlockState> e : original.entrySet()) {
                world.setBlockAndUpdate(e.getKey().offset(stepX * i, stepY * i, stepZ * i), e.getValue());
                placed++;
            }
        }
        WorldEditManager.pushUndo(player, before, snapMin, snapMax);
        return "Stacked selection " + count + " times toward " + dir + " (" + placed + " blocks)";
    }

    // ------------------------------------------------------------------
    // Environment
    // ------------------------------------------------------------------

    public static String drain(ServerPlayer player, int radius) {
        Level world = level(player);
        BlockPos center = player.blockPosition();
        BlockPos min = center.offset(-radius, -radius, -radius);
        BlockPos max = center.offset(radius, radius, radius);
        List<CompoundTag> before = WorldEditManager.snapshotRegion(world, min, max);
        int count = 0;
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            BlockState state = world.getBlockState(pos);
            if (state.getFluidState().is(FluidTags.WATER) || state.getFluidState().is(FluidTags.LAVA)) {
                world.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                count++;
            }
        }
        WorldEditManager.pushUndo(player, before, min, max);
        return "Drained " + count + " fluid blocks";
    }

    public static String butcher(ServerPlayer player, int radius) {
        Level world = level(player);
        BlockPos center = player.blockPosition();
        AABB aabb = new AABB(center).inflate(radius);
        List<LivingEntity> entities = world.getEntitiesOfClass(LivingEntity.class, aabb,
                e -> !(e instanceof Player) && e.isAlive());
        int count = 0;
        for (LivingEntity e : entities) {
            e.kill();
            count++;
        }
        return "Butchered " + count + " entities";
    }

    public static String fill(ServerPlayer player, String blockName, int radius) {
        Block block = parseBlock(blockName);
        if (block == null) return "Unknown block: " + blockName;
        Level world = level(player);
        BlockPos center = player.blockPosition();
        BlockPos min = center.offset(-radius, -radius, -radius);
        BlockPos max = center.offset(radius, radius, radius);
        List<CompoundTag> before = WorldEditManager.snapshotRegion(world, min, max);
        int count = 0;
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            world.setBlockAndUpdate(pos, block.defaultBlockState());
            count++;
        }
        WorldEditManager.pushUndo(player, before, min, max);
        return "Filled " + count + " blocks with " + blockName;
    }

    public static String naturalize(ServerPlayer player) {
        WorldEditManager.SelectionData sel = WorldEditManager.getSelection(player);
        if (!sel.isComplete()) return "Selection incomplete - set both positions first";
        Level world = level(player);
        BlockPos min = sel.getMin();
        BlockPos max = sel.getMax();
        List<CompoundTag> before = WorldEditManager.snapshotRegion(world, min, max);
        int count = 0;
        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int z = min.getZ(); z <= max.getZ(); z++) {
                int topY = world.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                if (topY < min.getY() || topY > max.getY()) continue;
                BlockPos top = new BlockPos(x, topY, z);
                world.setBlockAndUpdate(top, Blocks.GRASS_BLOCK.defaultBlockState());
                world.setBlockAndUpdate(top.below(), Blocks.DIRT.defaultBlockState());
                world.setBlockAndUpdate(top.below(2), Blocks.STONE.defaultBlockState());
                count += 3;
            }
        }
        WorldEditManager.pushUndo(player, before, min, max);
        return "Naturalized " + count + " blocks";
    }

    // ------------------------------------------------------------------
    // Info
    // ------------------------------------------------------------------

    public static String size(ServerPlayer player) {
        WorldEditManager.SelectionData sel = WorldEditManager.getSelection(player);
        if (!sel.isComplete()) return "Selection incomplete - set both positions first";
        BlockPos min = sel.getMin();
        BlockPos max = sel.getMax();
        int w = max.getX() - min.getX() + 1;
        int h = max.getY() - min.getY() + 1;
        int d = max.getZ() - min.getZ() + 1;
        return "Selection size: " + w + " x " + h + " x " + d + " (" + sel.getVolume() + " blocks)";
    }

    public static String count(ServerPlayer player, String blockName) {
        Block block = parseBlock(blockName);
        if (block == null) return "Unknown block: " + blockName;
        WorldEditManager.SelectionData sel = WorldEditManager.getSelection(player);
        if (!sel.isComplete()) return "Selection incomplete - set both positions first";
        Level world = level(player);
        BlockPos min = sel.getMin();
        BlockPos max = sel.getMax();
        int count = 0;
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (world.getBlockState(pos).getBlock() == block) count++;
        }
        return "Found " + count + " " + blockName + " blocks";
    }

    // ------------------------------------------------------------------
    // History
    // ------------------------------------------------------------------

    public static String undo(ServerPlayer player) {
        WorldEditManager.HistoryEntry entry = WorldEditManager.popUndo(player);
        if (entry == null) return "Nothing to undo";
        Level world = level(player);
        List<CompoundTag> current = WorldEditManager.snapshotRegion(world, entry.min, entry.max);
        WorldEditManager.pushRedo(player, new WorldEditManager.HistoryEntry(current, entry.min, entry.max));
        WorldEditManager.restoreSnapshot(world, entry.snapshot);
        return "Undid last operation";
    }

    public static String redo(ServerPlayer player) {
        WorldEditManager.HistoryEntry entry = WorldEditManager.popRedo(player);
        if (entry == null) return "Nothing to redo";
        Level world = level(player);
        List<CompoundTag> current = WorldEditManager.snapshotRegion(world, entry.min, entry.max);
        WorldEditManager.pushUndo(player, current, entry.min, entry.max);
        WorldEditManager.restoreSnapshot(world, entry.snapshot);
        return "Redid operation";
    }
}