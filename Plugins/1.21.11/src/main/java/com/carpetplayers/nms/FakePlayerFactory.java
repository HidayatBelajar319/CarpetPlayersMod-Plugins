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
 * Factory for creating and removing FakePlayers on a Paper 1.21.11 server (Mojang-mapped).
 * Bots are registered into the PlayerList and ServerLevel, and broadcast to all online
 * players via ADD_PLAYER + AddEntity packets.
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

        // Register into the server & world
        server.getPlayerList().players.add(fake);
        world.addFreshEntity(fake);

        // Broadcast so clients can see the bot
        broadcastPacket(new ClientboundPlayerInfoUpdatePacket(
                ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER, fake));
        broadcastPacket(buildSpawnPacket(fake));

        return fake;
    }

    /**
     * Spawn packet for a player. Note: ClientboundAddPlayerPacket was merged into
     * ClientboundAddEntityPacket since 1.21.2 - the (ServerPlayer) constructor suggested
     * by the old recon NO LONGER exists in 1.21.11. The raw constructor (id, uuid, position,
     * rotation, EntityType.PLAYER, data, movement, yHeadRot) is used, which is independent
     * of the state tracker.
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

        // Broadcast removal to all online players
        broadcastPacket(new ClientboundRemoveEntitiesPacket(fake.getId()));
        broadcastPacket(new ClientboundPlayerInfoRemovePacket(List.of(fake.getUUID())));

        // Remove from the world & player list
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
        // Fallback: remove from the playerList if still present
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
