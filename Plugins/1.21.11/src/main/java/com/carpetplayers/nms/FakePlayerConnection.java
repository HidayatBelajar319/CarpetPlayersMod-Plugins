package com.carpetplayers.nms;

import io.papermc.paper.connection.PaperPlayerGameConnection;
import io.papermc.paper.connection.PlayerCommonConnection;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

/**
 * Fake connection for bots. All packets destined for the bot's "client" are ignored
 * so the bot does not crash when the world tries to send data to the player.
 */
public class FakePlayerConnection extends ServerGamePacketListenerImpl {

    public FakePlayerConnection(MinecraftServer server,
                                Connection connection,
                                ServerPlayer player) {
        super(server, connection, player,
                CommonListenerCookie.createInitial(player.getGameProfile(), false));
    }

    @Override
    public void send(Packet<?> packet) {
        // no-op: the bot has no real client
    }

    @Override
    public void disconnect(Component reason) {
        // no-op: don't let the server remove the bot from the player list
    }

    @Override
    public void disconnectAsync(DisconnectionDetails details) {
        // no-op
    }

    @Override
    public PlayerCommonConnection getApiConnection() {
        return null;
    }

    @Override
    public PaperPlayerGameConnection paperConnection() {
        return null;
    }
}
