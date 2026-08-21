package com.carpetplayers.waypoint;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Client-side renderer for waypoints.
 * Shows a HUD overlay with waypoint name, distance, direction, and colored indicator.
 * Rendered using Fabric's HudRenderCallback (1.16.5 compatible).
 */
public final class WaypointRenderer {
    private static boolean enabled = true;

    private WaypointRenderer() {}

    public static void init() {
        HudRenderCallback.EVENT.register((matrices, tickDelta) -> {
            if (!enabled) return;
            renderWaypointHud(matrices);
        });
    }

    public static void setEnabled(boolean e) { enabled = e; }
    public static boolean isEnabled() { return enabled; }

    private static void renderWaypointHud(PoseStack matrices) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return;

        // Get enabled waypoints from WaypointManager (client has same data via loadPlayer)
        List<Waypoint> waypoints = new ArrayList<>(WaypointManager.getEnabledWaypoints(
                player.getUUID()));

        if (waypoints.isEmpty()) return;

        // Sort by distance (closest first)
        waypoints.sort(Comparator.comparingDouble(wp -> player.distanceToSqr(
                wp.getX(), wp.getY(), wp.getZ())));

        Font font = mc.font;
        int startX = 5;  // left edge
        int startY = 5;  // top edge
        int lineH = 12;
        int maxDisplay = Math.min(waypoints.size(), 10);

        // Background
        int bgWidth = 0;
        for (int i = 0; i < maxDisplay; i++) {
            Waypoint wp = waypoints.get(i);
            double dist = player.distanceToSqr(wp.getX(), wp.getY(), wp.getZ());
            String line = "  " + wp.getName() + " [" + (int) Math.sqrt(dist) + "m]";
            bgWidth = Math.max(bgWidth, font.width(line) + 20);
        }

        // Draw semi-transparent background
        matrices.pushPose();
        net.minecraft.client.gui.GuiComponent.fill(matrices,
                startX - 2, startY - 2,
                startX + bgWidth + 4, startY + maxDisplay * lineH + 4,
                0x80000000);

        // Draw title
        font.drawShadow(matrices, "\u00a76Waypoints", startX, startY, 0xFFFFFF);
        startY += lineH;

        for (int i = 0; i < maxDisplay; i++) {
            Waypoint wp = waypoints.get(i);
            int y = startY + i * lineH;

            double dist = Math.sqrt(player.distanceToSqr(wp.getX(), wp.getY(), wp.getZ()));

            // Direction arrow
            String dir = getDirection(player, wp);

            // Color indicator (small colored square via text)
            String colorCode = "\u00a7" + Integer.toHexString(colorToMinecraft(wp.getColor()));
            String distStr = String.format("%.0f", dist);

            // Format: "▸ Name [dist]m dir"
            String namePart = wp.getName();
            String distPart = " [" + distStr + "m]";
            String dirPart = " " + dir;

            int x = startX + 2;

            // Colored dot indicator
            font.drawShadow(matrices, "\u25cf", x, y, wp.getColor());
            x += font.width("\u25cf") + 2;

            // Name
            font.drawShadow(matrices, colorCode + namePart, x, y, wp.getColor());
            x += font.width(colorCode + namePart);

            // Distance
            font.drawShadow(matrices, "\u00a77" + distPart, x, y, 0x777777);
            x += font.width("\u00a77" + distPart);

            // Direction
            font.drawShadow(matrices, "\u00a7e" + dirPart, x, y, 0xFFFF55);
        }

        matrices.popPose();
    }

    /**
     * Get direction arrow from player to waypoint.
     */
    private static String getDirection(LocalPlayer player, Waypoint wp) {
        double dx = wp.getX() - player.getX();
        double dz = wp.getZ() - player.getZ();
        double dy = wp.getY() - player.getY();

        // Cardinal direction based on angle
        double angle = Math.toDegrees(Math.atan2(-dx, dz));
        String horizontal;
        if (angle >= -22.5 && angle < 22.5)      horizontal = "N";
        else if (angle >= 22.5 && angle < 67.5)   horizontal = "NW";
        else if (angle >= 67.5 && angle < 112.5)  horizontal = "W";
        else if (angle >= 112.5 && angle < 157.5) horizontal = "SW";
        else if (angle >= 157.5 || angle < -157.5) horizontal = "S";
        else if (angle >= -157.5 && angle < -112.5) horizontal = "SE";
        else if (angle >= -112.5 && angle < -67.5) horizontal = "E";
        else                                       horizontal = "NE";

        // Vertical indicator
        String vertical = "";
        if (dy > 3) vertical = " \u2191";       // up arrow
        else if (dy < -3) vertical = " \u2193"; // down arrow

        return horizontal + vertical;
    }

    /**
     * Convert a 0xRRGGBB color to the nearest Minecraft formatting color index (0-15).
     * Returns white (15) for unknown.
     */
    private static int colorToMinecraft(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        if (r > 200 && g < 80 && b < 80) return 4;  // red
        if (r < 80 && g > 200 && b < 80) return 2;  // green
        if (r < 80 && g < 80 && b > 200) return 1;  // blue
        if (r > 200 && g > 200 && b < 80) return 11; // yellow
        if (r > 200 && g < 80 && b > 200) return 5;  // purple
        if (r > 200 && g > 120 && b < 80) return 7;  // orange
        if (r < 80 && g > 200 && b > 200) return 3;  // cyan
        if (r > 200 && g > 200 && b > 200) return 15; // white
        return 15;
    }
}
