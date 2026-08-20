package com.carpetplayers.worldedit;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public final class WorldEditManager {
    private static final Map<UUID, SelectionData> selections = new HashMap<>();
    private static final Map<UUID, List<CompoundTag>> clipboard = new HashMap<>();
    private static final Map<UUID, int[]> clipboardSize = new HashMap<>();
    private static final Map<UUID, Deque<HistoryEntry>> undoStack = new HashMap<>();
    private static final Map<UUID, Deque<HistoryEntry>> redoStack = new HashMap<>();
    private static final int MAX_HISTORY = 20;

    public static SelectionData getSelection(ServerPlayer player) {
        return selections.computeIfAbsent(player.getUUID(), k -> new SelectionData());
    }

    public static List<CompoundTag> getClipboard(ServerPlayer player) {
        return clipboard.computeIfAbsent(player.getUUID(), k -> new ArrayList<>());
    }

    public static int[] getClipboardSize(ServerPlayer player) {
        return clipboardSize.get(player.getUUID());
    }

    public static void setClipboardSize(ServerPlayer player, int[] size) {
        clipboardSize.put(player.getUUID(), size);
    }

    public static void pushUndo(ServerPlayer player, List<CompoundTag> before, BlockPos min, BlockPos max) {
        Deque<HistoryEntry> stack = undoStack.computeIfAbsent(player.getUUID(), k -> new ArrayDeque<>());
        stack.push(new HistoryEntry(before, min, max));
        if (stack.size() > MAX_HISTORY) stack.removeLast();
        redoStack.computeIfAbsent(player.getUUID(), k -> new ArrayDeque<>()).clear();
    }

    public static HistoryEntry popUndo(ServerPlayer player) {
        Deque<HistoryEntry> stack = undoStack.getOrDefault(player.getUUID(), new ArrayDeque<>());
        return stack.isEmpty() ? null : stack.pop();
    }

    public static void pushRedo(ServerPlayer player, HistoryEntry entry) {
        Deque<HistoryEntry> stack = redoStack.computeIfAbsent(player.getUUID(), k -> new ArrayDeque<>());
        stack.push(entry);
    }

    public static HistoryEntry popRedo(ServerPlayer player) {
        Deque<HistoryEntry> stack = redoStack.getOrDefault(player.getUUID(), new ArrayDeque<>());
        return stack.isEmpty() ? null : stack.pop();
    }

    public static void clearSelection(ServerPlayer player) {
        selections.remove(player.getUUID());
    }

    // Snapshot a region for undo
    public static List<CompoundTag> snapshotRegion(Level level, BlockPos min, BlockPos max) {
        List<CompoundTag> snapshot = new ArrayList<>();
        int sx = Math.min(min.getX(), max.getX());
        int sy = Math.min(min.getY(), max.getY());
        int sz = Math.min(min.getZ(), max.getZ());
        int ex = Math.max(min.getX(), max.getX());
        int ey = Math.max(min.getY(), max.getY());
        int ez = Math.max(min.getZ(), max.getZ());
        for (int x = sx; x <= ex; x++) {
            for (int y = sy; y <= ey; y++) {
                for (int z = sz; z <= ez; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    CompoundTag tag = new CompoundTag();
                    tag.putInt("x", x);
                    tag.putInt("y", y);
                    tag.putInt("z", z);
                    tag.putString("block", net.minecraft.core.Registry.BLOCK.getKey(state.getBlock()).toString());
                    snapshot.add(tag);
                }
            }
        }
        return snapshot;
    }

    // Restore a snapshot
    public static void restoreSnapshot(Level level, List<CompoundTag> snapshot) {
        for (CompoundTag tag : snapshot) {
            BlockPos pos = new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
            // Simple restore - set to the block type (without block state properties)
            // For full fidelity we'd need NBT, but this is good enough for undo
            net.minecraft.resources.ResourceLocation id = new net.minecraft.resources.ResourceLocation(tag.getString("block"));
            if (net.minecraft.core.Registry.BLOCK.containsKey(id)) {
                BlockState state = net.minecraft.core.Registry.BLOCK.get(id).defaultBlockState();
                level.setBlockAndUpdate(pos, state);
            }
        }
    }

    public static class SelectionData {
        public BlockPos pos1;
        public BlockPos pos2;

        public boolean isComplete() { return pos1 != null && pos2 != null; }

        public BlockPos getMin() {
            if (!isComplete()) return null;
            return new BlockPos(
                Math.min(pos1.getX(), pos2.getX()),
                Math.min(pos1.getY(), pos2.getY()),
                Math.min(pos1.getZ(), pos2.getZ())
            );
        }

        public BlockPos getMax() {
            if (!isComplete()) return null;
            return new BlockPos(
                Math.max(pos1.getX(), pos2.getX()),
                Math.max(pos1.getY(), pos2.getY()),
                Math.max(pos1.getZ(), pos2.getZ())
            );
        }

        public int getVolume() {
            if (!isComplete()) return 0;
            BlockPos min = getMin();
            BlockPos max = getMax();
            return (max.getX() - min.getX() + 1) * (max.getY() - min.getY() + 1) * (max.getZ() - min.getZ() + 1);
        }
    }

    public static class HistoryEntry {
        public final List<CompoundTag> snapshot;
        public final BlockPos min, max;
        public HistoryEntry(List<CompoundTag> snapshot, BlockPos min, BlockPos max) {
            this.snapshot = snapshot;
            this.min = min;
            this.max = max;
        }
    }

    private WorldEditManager() {}
}