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
 * Koneksi palsu untuk bot. Semua packet yang menuju "klien" bot diabaikan
 * sehingga bot tidak crash ketika world mencoba mengirim data ke pemain.
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
        // no-op: bot tidak punya klien sungguhan
    }

    @Override
    public void disconnect(Component reason) {
        // no-op: jangan biarkan server mengeluarkan bot dari daftar player
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
