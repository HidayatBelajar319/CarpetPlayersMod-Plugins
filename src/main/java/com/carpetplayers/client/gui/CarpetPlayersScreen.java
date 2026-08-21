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
        int y = 36;

        // Row 1: Spawn buttons (centered, paired)
        int spawnRowWidth = 200;
        this.addButton(new Button(centerX - spawnRowWidth / 2, y, 95, 20, new TextComponent("Spawn Bot"), button -> {
            sendAction("spawn");
        }));
        this.addButton(new Button(centerX - spawnRowWidth / 2 + 100, y, 95, 20, new TextComponent("Spawn PvP"), button -> {
            sendAction("spawn_pvp");
        }));

        y += 24;
        // Row 2: Bot actions (centered, 4 buttons)
        int actionRowWidth = 390;
        int actionStart = centerX - actionRowWidth / 2;
        this.addButton(new Button(actionStart, y, 90, 20, new TextComponent("Remove"), button -> {
            if (selectedBotIndex >= 0 && selectedBotIndex < botEntries.size()) {
                sendActionWith("remove", botEntries.get(selectedBotIndex)[0]);
            }
        }));
        this.addButton(new Button(actionStart + 95, y, 90, 20, new TextComponent("Control"), button -> {
            if (selectedBotIndex >= 0 && selectedBotIndex < botEntries.size()) {
                sendActionWith("control", botEntries.get(selectedBotIndex)[0]);
            }
        }));
        this.addButton(new Button(actionStart + 190, y, 90, 20, new TextComponent("Release"), button -> {
            sendAction("release");
        }));
        this.addButton(new Button(actionStart + 285, y, 100, 20, new TextComponent("Request List"), button -> {
            CarpetPlayersClient.requestBots();
        }));

        y += 24;
        // Row 3: Kit selector (centered)
        int kitRowWidth = 300;
        int kitStart = centerX - kitRowWidth / 2;
        this.addButton(new Button(kitStart, y, 150, 20, new TextComponent("Kit: " + KITS[kitIndex]), button -> {
            kitIndex = (kitIndex + 1) % KITS.length;
            button.setMessage(new TextComponent("Kit: " + KITS[kitIndex]));
        }));
        this.addButton(new Button(kitStart + 155, y, 140, 20, new TextComponent("Apply Kit"), button -> {
            if (selectedBotIndex >= 0 && selectedBotIndex < botEntries.size()) {
                sendKitAction(botEntries.get(selectedBotIndex)[0], KITS[kitIndex]);
            }
        }));

        y += 24;
        // Row 4: Settings toggles (centered)
        int toggleRowWidth = 395;
        int toggleStart = centerX - toggleRowWidth / 2;
        this.addButton(new Button(toggleStart, y, 125, 20, new TextComponent("Use Item: " + (useItemEnabled ? "ON" : "OFF")), button -> {
            useItemEnabled = !useItemEnabled;
            button.setMessage(new TextComponent("Use Item: " + (useItemEnabled ? "ON" : "OFF")));
            sendToggle("toggle_useitem", useItemEnabled);
        }));
        this.addButton(new Button(toggleStart + 130, y, 130, 20, new TextComponent("Interactive: " + (interactiveEnabled ? "ON" : "OFF")), button -> {
            interactiveEnabled = !interactiveEnabled;
            button.setMessage(new TextComponent("Interactive: " + (interactiveEnabled ? "ON" : "OFF")));
            sendToggle("toggle_interactive", interactiveEnabled);
        }));
        this.addButton(new Button(toggleStart + 265, y, 125, 20, new TextComponent("Multi-Weapon: " + (multiWeaponEnabled ? "ON" : "OFF")), button -> {
            multiWeaponEnabled = !multiWeaponEnabled;
            button.setMessage(new TextComponent("Multi-Weapon: " + (multiWeaponEnabled ? "ON" : "OFF")));
            sendToggle("toggle_multiweapon", multiWeaponEnabled);
        }));

        y += 24;
        // Row 5: Done button
        this.addButton(new Button(centerX - 50, this.height - 30, 100, 20, new TextComponent("Done"), button -> {
            this.minecraft.setScreen(parent);
        }));

        // Request bot list on open
        CarpetPlayersClient.requestBots();
    }

    @Override
    public void render(PoseStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        drawCenteredString(matrices, this.font, "\u00a76CARPET PLAYERS", this.width / 2, 16, 0xFFFFFF);

        // Draw bot list area — starts below button rows
        int listY = 118;
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

                String typeTag = pvp ? " \u00a7c[PvP]" : " \u00a7a[Normal]";
                String line = bot[0] + typeTag + "  \u00a74HP:" + bot[1] + "  \u00a77[" + bot[2] + "]";
                drawString(matrices, this.font, line, listX, ey, i == selectedBotIndex ? 0x55FFFF : 0xFFFFFF);
            }
        }

        // Draw settings summary at bottom of list area
        int settingsY = listY + Math.max(botEntries.size(), 1) * entryHeight + 10;
        drawString(matrices, this.font,
                "Settings: UseItem=" + useItemEnabled + " Interactive=" + interactiveEnabled + " MultiWeapon=" + multiWeaponEnabled,
                listX, settingsY, 0x888888);

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
            int listY = 118;
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
