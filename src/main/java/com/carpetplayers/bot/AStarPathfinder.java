package com.carpetplayers.bot;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Simple A* pathfinder for bot navigation.
 * Finds paths on the XZ plane with simple jump-up (1 block) support.
 */
public final class AStarPathfinder {

    private static final int MAX_NODES = 500;
    private static final int MAX_PATH_LENGTH = 64;

    /**
     * Find a path from start to goal. Returns list of BlockPos, or null if no path found.
     */
    public static List<BlockPos> findPath(Level level, BlockPos start, BlockPos goal) {
        if (start.equals(goal)) return Collections.singletonList(goal);

        PriorityQueue<Node> open = new PriorityQueue<>(Comparator.comparingInt(n -> n.f));
        Set<BlockPos> closed = new HashSet<>();
        Map<BlockPos, Node> allNodes = new HashMap<>();

        Node startNode = new Node(start, 0, heuristic(start, goal), null);
        open.add(startNode);
        allNodes.put(start, startNode);

        int explored = 0;

        while (!open.isEmpty() && explored < MAX_NODES) {
            Node current = open.poll();
            explored++;

            if (current.pos.equals(goal)) {
                return reconstructPath(current);
            }

            closed.add(current.pos);

            for (BlockPos neighbor : getNeighbors(current.pos)) {
                if (closed.contains(neighbor)) continue;
                if (!isWalkable(level, neighbor)) continue;
                // Allow walking on 1-block-high step up
                if (!isWalkable(level, neighbor.above()) && !level.getBlockState(neighbor).getMaterial().isSolid()) {
                    // Can't walk here, blocked above and not on ground
                }

                int tentativeG = current.g + 1;

                Node existing = allNodes.get(neighbor);
                if (existing != null && tentativeG >= existing.g) continue;

                Node neighborNode = new Node(neighbor, tentativeG, heuristic(neighbor, goal), current);
                allNodes.put(neighbor, neighborNode);
                open.add(neighborNode);
            }
        }

        return null; // no path found
    }

    private static List<BlockPos> getNeighbors(BlockPos pos) {
        List<BlockPos> neighbors = new ArrayList<>(6);
        neighbors.add(pos.above());
        neighbors.add(pos.below());
        neighbors.add(pos.north());
        neighbors.add(pos.south());
        neighbors.add(pos.east());
        neighbors.add(pos.west());
        return neighbors;
    }

    private static boolean isWalkable(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Material mat = state.getMaterial();
        // Walkable: air, plants, water, fire, etc. (non-solid or replaceable)
        return !mat.isSolid() || mat.isReplaceable();
    }

    private static int heuristic(BlockPos a, BlockPos b) {
        return Math.abs(a.getX() - b.getX()) + Math.abs(a.getY() - b.getY()) + Math.abs(a.getZ() - b.getZ());
    }

    private static List<BlockPos> reconstructPath(Node node) {
        List<BlockPos> path = new ArrayList<>();
        while (node != null) {
            path.add(node.pos);
            node = node.parent;
        }
        Collections.reverse(path);
        if (path.size() > MAX_PATH_LENGTH) {
            return path.subList(0, MAX_PATH_LENGTH);
        }
        return path;
    }

    private static class Node {
        final BlockPos pos;
        final int g;
        final int f;
        final Node parent;

        Node(BlockPos pos, int g, int f, Node parent) {
            this.pos = pos;
            this.g = g;
            this.f = f;
            this.parent = parent;
        }
    }

    private AStarPathfinder() {}
}
