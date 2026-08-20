package com.carpetplayers.client;

import com.carpetplayers.client.gui.CarpetPlayersScreen;
import com.carpetplayers.network.ModPackets;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.FriendlyByteBuf;

public class CarpetPlayersClient implements ClientModInitializer {
    private static CarpetPlayersScreen pendingScreen = null;

    @Override
    public void onInitializeClient() {
        // Register handler for server telling us to open the menu
        ClientPlayNetworking.registerGlobalReceiver(ModPackets.OPEN_MENU, 
            (Minecraft client, ClientPacketListener handler, FriendlyByteBuf buf, PacketSender responseSender) -> {
                client.execute(() -> {
                    client.setScreen(new CarpetPlayersScreen(client.screen));
                });
            });

        // Register handler for receiving bot list data
        ClientPlayNetworking.registerGlobalReceiver(ModPackets.BOT_LIST,
            (Minecraft client, ClientPacketListener handler, FriendlyByteBuf buf, PacketSender responseSender) -> {
                // Parse bot list on network thread, then update UI on render thread
                int count = buf.readInt();
                String[][] bots = new String[count][3]; // name, health, state
                boolean[] isPvp = new boolean[count];
                for (int i = 0; i < count; i++) {
                    bots[i][0] = buf.readUtf();
                    bots[i][1] = String.valueOf(buf.readFloat());
                    bots[i][2] = buf.readUtf();
                    isPvp[i] = buf.readBoolean();
                }
                client.execute(() -> {
                    if (client.screen instanceof CarpetPlayersScreen) {
                        ((CarpetPlayersScreen) client.screen).updateBotList(bots, isPvp);
                    }
                });
            });
    }

    public static void requestBots() {
        ClientPlayNetworking.send(ModPackets.REQUEST_BOTS, new FriendlyByteBuf(
            net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create()));
    }
}
