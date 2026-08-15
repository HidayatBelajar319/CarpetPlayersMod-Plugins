package com.carpetplayers.nms;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.v1_16_R3.EntityPlayer;
import net.minecraft.server.v1_16_R3.MinecraftServer;
import net.minecraft.server.v1_16_R3.Packet;
import net.minecraft.server.v1_16_R3.PacketPlayOutEntityDestroy;
import net.minecraft.server.v1_16_R3.PacketPlayOutNamedEntitySpawn;
import net.minecraft.server.v1_16_R3.PacketPlayOutPlayerInfo;
import net.minecraft.server.v1_16_R3.PlayerConnection;
import net.minecraft.server.v1_16_R3.PlayerInteractManager;
import net.minecraft.server.v1_16_R3.PlayerList;
import net.minecraft.server.v1_16_R3.WorldServer;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_16_R3.CraftServer;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Pabrik untuk membuat dan menghapus FakePlayer di server Paper 1.16.5.
 * Bot didaftarkan ke PlayerList, WorldServer, dan disiarkan ke semua
 * pemain online lewat packet ADD_PLAYER + NamedEntitySpawn.
 */
public final class FakePlayerFactory {

    private FakePlayerFactory() {
    }

    public static FakePlayer spawn(String name, World bukkitWorld,
                                   double x, double y, double z, float yaw, float pitch) {
        CraftServer craftServer = (CraftServer) Bukkit.getServer();
        MinecraftServer server = craftServer.getServer();
        WorldServer world = ((CraftWorld) bukkitWorld).getHandle();
        GameProfile profile = new GameProfile(UUID.randomUUID(), name);
        PlayerInteractManager interactManager = new PlayerInteractManager(world);
        FakePlayer fake = new FakePlayer(server, world, profile, interactManager);
        interactManager.player = fake;
        fake.moveLocation(x, y, z, yaw, pitch);
        fake.isFake = true;

        // Registrasi ke server & world
        server.getPlayerList().players.add(fake);
        world.players.add(fake);
        world.addEntity(fake);

        // Broadcast agar client melihat bot
        broadcastPacket(new PacketPlayOutPlayerInfo(
                PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, fake));
        broadcastPacket(new PacketPlayOutNamedEntitySpawn(fake));

        return fake;
    }

    public static void despawn(FakePlayer fake) {
        if (fake == null) {
            return;
        }
        WorldServer world = fake.getWorldServer();
        MinecraftServer server = world != null
                ? ((CraftServer) Bukkit.getServer()).getServer() : null;

        // Broadcast penghapusan ke semua pemain online
        broadcastPacket(new PacketPlayOutEntityDestroy(fake.getId()));
        if (server != null) {
            broadcastPacket(new PacketPlayOutPlayerInfo(
                    PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, fake));
        }

        // Hapus dari world & player list
        if (world != null) {
            world.players.remove(fake);
            world.removeEntity(fake);
        }
        if (server != null) {
            server.getPlayerList().players.remove(fake);
        }
        if (fake.isAlive()) {
            fake.killEntity();
        }
    }

    public static void despawnAll() {
        for (World world : Bukkit.getWorlds()) {
            WorldServer ws = ((CraftWorld) world).getHandle();
            for (int i = ws.players.size() - 1; i >= 0; i--) {
                EntityPlayer p = ws.players.get(i);
                if (p instanceof FakePlayer) {
                    despawn((FakePlayer) p);
                }
            }
        }
        // Fallback: hapus dari playerList bila masih ada
        MinecraftServer server = ((CraftServer) Bukkit.getServer()).getServer();
        for (int i = server.getPlayerList().players.size() - 1; i >= 0; i--) {
            EntityPlayer p = server.getPlayerList().players.get(i);
            if (p instanceof FakePlayer) {
                despawn((FakePlayer) p);
            }
        }
    }

    private static void broadcastPacket(Packet<?> packet) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerConnection connection = ((CraftPlayer) player).getHandle().playerConnection;
            if (connection != null) {
                connection.sendPacket(packet);
            }
        }
    }
}