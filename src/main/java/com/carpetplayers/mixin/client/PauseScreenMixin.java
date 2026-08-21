package com.carpetplayers.mixin.client;

import com.carpetplayers.client.gui.CarpetPlayersScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TextComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin extends Screen {
    protected PauseScreenMixin(TextComponent title) {
        super(title);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void carpetplayers_addPauseMenuButton(CallbackInfo ci) {
        // Place button next to Options row (Options is at centerX-100, height/4+96, LAN at centerX+2)
        // Put Carpet Players right after LAN button
        this.addButton(
            new Button(this.width / 2 + 104, this.height / 4 + 96, 98, 20, new TextComponent("Carpet Players"),
                button -> net.minecraft.client.Minecraft.getInstance().setScreen(
                    new CarpetPlayersScreen(this)
                )
            )
        );
    }
}
