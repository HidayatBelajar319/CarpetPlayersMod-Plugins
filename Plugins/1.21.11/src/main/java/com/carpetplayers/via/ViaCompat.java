package com.carpetplayers.via;

import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Helper integrasi ViaVersion/ViaBackwards.
 * Mendeteksi versi protokol client yang sebenarnya (misal 1.16.5 join server 1.21.11
 * lewat ViaBackwards) sehingga plugin/AI bisa beradaptasi terhadap versi client.
 */
public final class ViaCompat {

    private ViaCompat() {
    }

    /** true jika ViaVersion terpasang dan API tersedia. */
    public static boolean isAvailable() {
        return Bukkit.getPluginManager().getPlugin("ViaVersion") != null;
    }

    /**
     * Versi protokol client (int, misal 754 = 1.16.5, 774 = 1.21.11).
     * 0 jika ViaVersion tidak tersedia atau player belum ter-inject.
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
     * Versi protokol client berdasarkan UUID (aman dipanggil dari thread mana pun).
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
     * Nama versi client yang terbaca (misal "1.16.5", "1.21.11").
     * "unknown" jika tidak terdeteksi.
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

    /** true jika client lebih lama dari versi server (pakai ViaBackwards). */
    public static boolean isLegacyClient(Player player) {
        int protocol = getProtocolVersion(player);
        if (protocol == 0) {
            return false;
        }
        // 767 = 1.21.0; di bawah itu dianggap versi lama vs server 1.21.11
        return protocol < 767;
    }
}
