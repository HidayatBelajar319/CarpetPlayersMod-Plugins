package com.carpetplayers.nms;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

/**
 * Pabrik untuk membuat dan menghapus FakePlayer di server Paper 1.21.11 (Mojang-mapped).
 * Bot didaftarkan ke PlayerList, ServerLevel, dan disiarkan ke semua pemain online
 * lewat packet ADD_PLAYER + AddEntity.
 */
public final class FakePlayerFactory {

    private FakePlayerFactory() {
    }

    public static FakePlayer spawn(String name, World bukkitWorld,
                                   double x, double y, double z, float yaw, float pitch) {
        CraftServer craftServer = (CraftServer) Bukkit.getServer();
        MinecraftServer server = craftServer.getServer();
        ServerLevel world = ((CraftWorld) bukkitWorld).getHandle();
        GameProfile profile = new GameProfile(UUID.randomUUID(), name);
        FakePlayer fake = new FakePlayer(server, world, profile);
        fake.moveLocation(x, y, z, yaw, pitch);
        fake.isFake = true;

        // Registrasi ke server & world
        server.getPlayerList().players.add(fake);
        world.addFreshEntity(fake);

        // Broadcast agar client melihat bot
        broadcastPacket(new ClientboundPlayerInfoUpdatePacket(
                ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER, fake));
        broadcastPacket(buildSpawnPacket(fake));

        return fake;
    }

    /**
     * Packet spawn untuk player. Catatan: ClientboundAddPlayerPacket sudah digabung ke
     * ClientboundAddEntityPacket sejak 1.21.2 — ctor (ServerPlayer) yang disarankan oleh
     * recon lama TIDAK ADA lagi di 1.21.11. Dipakai ctor mentah (id, uuid, posisi, rotasi,
     * EntityType.PLAYER, data, movement, yHeadRot) yang independen dari state tracker.
     */
    private static Packet<?> buildSpawnPacket(FakePlayer fake) {
        return new ClientboundAddEntityPacket(
                fake.getId(), fake.getUUID(),
                fake.getX(), fake.getY(), fake.getZ(),
                fake.getXRot(), fake.getYRot(),
                EntityType.PLAYER, 0,
                fake.getDeltaMovement(), fake.getYHeadRot());
    }

    public static void despawn(FakePlayer fake) {
        if (fake == null) {
            return;
        }
        ServerLevel world = fake.level(); // ServerPlayer.level() -> ServerLevel
        MinecraftServer server = ((CraftServer) Bukkit.getServer()).getServer();

        // Broadcast penghapusan ke semua pemain online
        broadcastPacket(new ClientboundRemoveEntitiesPacket(fake.getId()));
        broadcastPacket(new ClientboundPlayerInfoRemovePacket(List.of(fake.getUUID())));

        // Hapus dari world & player list
        if (world != null) {
            world.players().remove(fake);
            fake.discard();
        }
        server.getPlayerList().players.remove(fake);
        if (fake.isAlive() && world != null) {
            fake.kill(world);
        }
    }

    public static void despawnAll() {
        for (World world : Bukkit.getWorlds()) {
            ServerLevel ws = ((CraftWorld) world).getHandle();
            for (int i = ws.players().size() - 1; i >= 0; i--) {
                ServerPlayer p = ws.players().get(i);
                if (p instanceof FakePlayer) {
                    despawn((FakePlayer) p);
                }
            }
        }
        // Fallback: hapus dari playerList bila masih ada
        MinecraftServer server = ((CraftServer) Bukkit.getServer()).getServer();
        for (int i = server.getPlayerList().players.size() - 1; i >= 0; i--) {
            ServerPlayer p = server.getPlayerList().players.get(i);
            if (p instanceof FakePlayer) {
                despawn((FakePlayer) p);
            }
        }
    }

    private static void broadcastPacket(Packet<?> packet) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            ServerPlayer handle = ((CraftPlayer) player).getHandle();
            if (handle.connection != null) {
                handle.connection.send(packet);
            }
        }
    }
}
