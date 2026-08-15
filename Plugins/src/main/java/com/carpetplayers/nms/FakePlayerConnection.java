package com.carpetplayers.nms;

import io.netty.util.concurrent.GenericFutureListener;
import net.minecraft.server.v1_16_R3.IChatBaseComponent;
import net.minecraft.server.v1_16_R3.MinecraftServer;
import net.minecraft.server.v1_16_R3.NetworkManager;
import net.minecraft.server.v1_16_R3.Packet;
import net.minecraft.server.v1_16_R3.PlayerConnection;

/**
 * Koneksi palsu untuk bot. Semua packet yang menuju "klien" bot diabaikan
 * sehingga bot tidak crash ketika world mencoba mengirim data ke pemain.
 */
public class FakePlayerConnection extends PlayerConnection {

    public FakePlayerConnection(MinecraftServer server,
                                NetworkManager networkManager,
                                net.minecraft.server.v1_16_R3.EntityPlayer player) {
        super(server, networkManager, player);
    }

    @Override
    public void sendPacket(Packet<?> packet) {
        // no-op: bot tidak punya klien sungguhan
    }

    @Override
    public void a(Packet<?> packet,
                  GenericFutureListener<? extends io.netty.util.concurrent.Future<? super Void>> listener) {
        // no-op: overload sendPacket dengan listener
    }

    @Override
    public void disconnect(String reason) {
        // no-op: jangan biarkan server mengeluarkan bot dari daftar player
    }

    @Override
    public void disconnect(IChatBaseComponent reason) {
        // no-op
    }
}