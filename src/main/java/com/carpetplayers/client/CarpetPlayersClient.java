package com.carpetplayers.client;

import com.carpetplayers.client.gui.CarpetPlayersScreen;
import com.carpetplayers.network.ModPackets;
import com.carpetplayers.waypoint.WaypointManager;
import com.carpetplayers.waypoint.WaypointRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.ChatFormatting;
import org.lwjgl.glfw.GLFW;

public class CarpetPlayersClient implements ClientModInitializer {
    private static CarpetPlayersScreen pendingScreen = null;

    // Keybind to open Carpet Players Menu
    public static KeyMapping openMenuKey;

    @Override
    public void onInitializeClient() {
        // Register keybind: K key opens Carpet Players Menu
        openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.carpetplayers.open_menu",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                "Carpet Players"
        ));

        // Listen for key press every tick
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenuKey.consumeClick()) {
                if (client.screen == null && client.level != null && client.player != null) {
                    client.setScreen(new CarpetPlayersScreen(null));
                }
            }
        });

        // Initialize waypoint renderer
        WaypointRenderer.init();

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

        // Register handler for death waypoint notification
        ClientPlayNetworking.registerGlobalReceiver(ModPackets.DEATH_WAYPOINT,
            (Minecraft client, ClientPacketListener handler, FriendlyByteBuf buf, PacketSender responseSender) -> {
                String name = buf.readUtf();
                double x = buf.readDouble();
                double y = buf.readDouble();
                double z = buf.readDouble();
                client.execute(() -> {
                    if (client.player != null) {
                        client.player.sendMessage(
                            new TextComponent("[Waypoint] " + name + " created at X=" + (int)x + " Y=" + (int)y + " Z=" + (int)z)
                                .withStyle(ChatFormatting.RED),
                            client.player.getUUID());
                    }
                });
            });
    }

    public static void requestBots() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return; // not in-game, can't send packets
        }
        ClientPlayNetworking.send(ModPackets.REQUEST_BOTS, new FriendlyByteBuf(
            PacketByteBufs.create()));
    }
}
