package com.carpetplayers.client.gui;

import com.carpetplayers.client.CarpetPlayersClient;
import com.carpetplayers.network.ModPackets;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.TextComponent;

public class CarpetPlayersScreen extends Screen {
    private final Screen parent;
    private final java.util.List<String[]> botEntries = new java.util.ArrayList<>(); // [name, health, state]
    private final java.util.List<Boolean> botPvpFlags = new java.util.ArrayList<>();
    private int selectedBotIndex = -1;
    private int kitIndex = 0;
    private final String[] KITS = {"netherite_crystal", "diamond_crystal", "netherite_pot", "diamond_pot", "netherite_basic", "diamond_basic"};
    private boolean useItemEnabled = true;
    private boolean interactiveEnabled = true;
    private boolean multiWeaponEnabled = true;

    public CarpetPlayersScreen(Screen parent) {
        super(new TextComponent("Carpet Players"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int y = 30;

        // Title
        // Row 1: Action buttons
        this.addButton(new Button(centerX - 200, y, 95, 20, new TextComponent("Spawn Bot"), button -> {
            sendAction("spawn");
        }));
        this.addButton(new Button(centerX - 100, y, 95, 20, new TextComponent("Spawn PvP"), button -> {
            sendAction("spawn_pvp");
        }));
        this.addButton(new Button(centerX + 5, y, 95, 20, new TextComponent("Remove Selected"), button -> {
            if (selectedBotIndex >= 0 && selectedBotIndex < botEntries.size()) {
                sendActionWith("remove", botEntries.get(selectedBotIndex)[0]);
            }
        }));
        this.addButton(new Button(centerX + 105, y, 95, 20, new TextComponent("Request List"), button -> {
            CarpetPlayersClient.requestBots();
        }));

        y += 25;
        // Row 2: Kit
        this.addButton(new Button(centerX - 200, y, 130, 20, new TextComponent("Kit: " + KITS[kitIndex]), button -> {
            kitIndex = (kitIndex + 1) % KITS.length;
            button.setMessage(new TextComponent("Kit: " + KITS[kitIndex]));
        }));
        this.addButton(new Button(centerX - 60, y, 90, 20, new TextComponent("Apply Kit"), button -> {
            if (selectedBotIndex >= 0 && selectedBotIndex < botEntries.size()) {
                sendKitAction(botEntries.get(selectedBotIndex)[0], KITS[kitIndex]);
            }
        }));
        this.addButton(new Button(centerX + 40, y, 90, 20, new TextComponent("Control"), button -> {
            if (selectedBotIndex >= 0 && selectedBotIndex < botEntries.size()) {
                sendActionWith("control", botEntries.get(selectedBotIndex)[0]);
            }
        }));
        this.addButton(new Button(centerX + 140, y, 90, 20, new TextComponent("Release"), button -> {
            sendAction("release");
        }));

        y += 25;
        // Row 3: Settings toggles
        this.addButton(new Button(centerX - 200, y, 95, 20, new TextComponent("Use Item: " + (useItemEnabled ? "ON" : "OFF")), button -> {
            useItemEnabled = !useItemEnabled;
            button.setMessage(new TextComponent("Use Item: " + (useItemEnabled ? "ON" : "OFF")));
            sendToggle("toggle_useitem", useItemEnabled);
        }));
        this.addButton(new Button(centerX - 100, y, 95, 20, new TextComponent("Interactive: " + (interactiveEnabled ? "ON" : "OFF")), button -> {
            interactiveEnabled = !interactiveEnabled;
            button.setMessage(new TextComponent("Interactive: " + (interactiveEnabled ? "ON" : "OFF")));
            sendToggle("toggle_interactive", interactiveEnabled);
        }));
        this.addButton(new Button(centerX + 5, y, 100, 20, new TextComponent("Multi-Weapon: " + (multiWeaponEnabled ? "ON" : "OFF")), button -> {
            multiWeaponEnabled = !multiWeaponEnabled;
            button.setMessage(new TextComponent("Multi-Weapon: " + (multiWeaponEnabled ? "ON" : "OFF")));
            sendToggle("toggle_multiweapon", multiWeaponEnabled);
        }));

        y += 30;
        // Row 4: Done button
        this.addButton(new Button(centerX - 50, this.height - 30, 100, 20, new TextComponent("Done"), button -> {
            this.minecraft.setScreen(parent);
        }));

        // Request bot list on open
        CarpetPlayersClient.requestBots();
    }

    @Override
    public void render(PoseStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        drawCenteredString(matrices, this.font, "CARPET PLAYERS", this.width / 2, 12, 0xFFFFFF);

        // Draw bot list area
        int listY = 100;
        int listX = this.width / 2 - 200;
        int listWidth = 400;
        int entryHeight = 14;

        // Bot list header
        drawString(matrices, this.font, "Bot List:", listX, listY - 14, 0xAAAAAA);
        
        if (botEntries.isEmpty()) {
            drawCenteredString(matrices, this.font, "No bots active", this.width / 2, listY + 10, 0x888888);
        } else {
            for (int i = 0; i < botEntries.size(); i++) {
                int ey = listY + i * entryHeight;
                String[] bot = botEntries.get(i);
                boolean pvp = botPvpFlags.get(i);
                
                // Highlight selected
                if (i == selectedBotIndex) {
                    fill(matrices, listX - 2, ey - 1, listX + listWidth, ey + entryHeight - 2, 0x4400AAFF);
                }

                // Click to select
                if (mouseX >= listX && mouseX <= listX + listWidth && mouseY >= ey && mouseY <= ey + entryHeight - 1) {
                    if (wasClicked()) {
                        selectedBotIndex = i;
                    }
                }

                String typeTag = pvp ? " [PvP]" : "";
                String line = bot[0] + typeTag + "  HP:" + bot[1] + "  [" + bot[2] + "]";
                drawString(matrices, this.font, line, listX, ey, i == selectedBotIndex ? 0x55FFFF : 0xFFFFFF);
            }
        }

        // Draw settings summary at bottom of list area
        int settingsY = listY + Math.max(botEntries.size(), 1) * entryHeight + 10;
        drawString(matrices, this.font, "Settings: UseItem=" + useItemEnabled + " Interactive=" + interactiveEnabled + " MultiWeapon=" + multiWeaponEnabled, listX, settingsY, 0x888888);

        super.render(matrices, mouseX, mouseY, delta);
    }

    private boolean wasClicked() {
        return true; // called from within render when mouse is over an entry
    }

    // Fix: override mouseClicked instead
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean result = super.mouseClicked(mouseX, mouseY, button);
        if (button == 0) {
            int listY = 100;
            int listX = this.width / 2 - 200;
            int listWidth = 400;
            for (int i = 0; i < botEntries.size(); i++) {
                int ey = listY + i * 14;
                if (mouseX >= listX && mouseX <= listX + listWidth && mouseY >= ey && mouseY <= ey + 13) {
                    selectedBotIndex = i;
                    return true;
                }
            }
        }
        return result;
    }

    private void sendAction(String action) {
        if (this.minecraft.level == null || this.minecraft.player == null) return;
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeUtf(action);
        ClientPlayNetworking.send(ModPackets.BOT_ACTION, buf);
    }

    private void sendActionWith(String action, String param) {
        if (this.minecraft.level == null || this.minecraft.player == null) return;
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeUtf(action);
        buf.writeUtf(param);
        ClientPlayNetworking.send(ModPackets.BOT_ACTION, buf);
    }

    private void sendKitAction(String botname, String kit) {
        if (this.minecraft.level == null || this.minecraft.player == null) return;
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeUtf("kit");
        buf.writeUtf(botname);
        buf.writeUtf(kit);
        ClientPlayNetworking.send(ModPackets.BOT_ACTION, buf);
    }

    private void sendToggle(String action, boolean enabled) {
        if (this.minecraft.level == null || this.minecraft.player == null) return;
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeUtf(action);
        buf.writeBoolean(enabled);
        ClientPlayNetworking.send(ModPackets.BOT_ACTION, buf);
    }

    public void updateBotList(String[][] bots, boolean[] isPvp) {
        botEntries.clear();
        botPvpFlags.clear();
        for (int i = 0; i < bots.length; i++) {
            botEntries.add(bots[i]);
            botPvpFlags.add(isPvp[i]);
        }
        selectedBotIndex = -1;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
