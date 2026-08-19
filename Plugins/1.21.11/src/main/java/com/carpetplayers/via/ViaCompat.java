package com.carpetplayers.via;

import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * ViaVersion/ViaBackwards integration helper.
 * Detects the actual client protocol version (e.g. a 1.16.5 client joining a 1.21.11
 * server through ViaBackwards) so the plugin/AI can adapt to the client version.
 */
public final class ViaCompat {

    private ViaCompat() {
    }

    /** true if ViaVersion is installed and its API is available. */
    public static boolean isAvailable() {
        return Bukkit.getPluginManager().getPlugin("ViaVersion") != null;
    }

    /**
     * Client protocol version (int, e.g. 754 = 1.16.5, 774 = 1.21.11).
     * 0 if ViaVersion is unavailable or the player has not been injected yet.
     */
    public static int getProtocolVersion(Player player) {
        if (!isAvailable() || player == null) {
            return 0;
        }
        try {
            return Via.getAPI().getPlayerVersion(player);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Client protocol version by UUID (safe to call from any thread).
     */
    public static int getProtocolVersion(UUID uuid) {
        if (!isAvailable() || uuid == null) {
            return 0;
        }
        try {
            return Via.getAPI().getPlayerVersion(uuid);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Readable client version name (e.g. "1.16.5", "1.21.11").
     * "unknown" if it cannot be detected.
     */
    public static String getClientVersionName(Player player) {
        if (!isAvailable() || player == null) {
            return "unknown";
        }
        try {
            ProtocolVersion pv = Via.getAPI().getPlayerProtocolVersion(player);
            return pv != null && pv.isKnown() ? pv.getName() : "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }

    /** true if the client is older than the server version (uses ViaBackwards). */
    public static boolean isLegacyClient(Player player) {
        int protocol = getProtocolVersion(player);
        if (protocol == 0) {
            return false;
        }
        // 767 = 1.21.0; anything below is considered a legacy version vs the 1.21.11 server
        return protocol < 767;
    }
}
