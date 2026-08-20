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
        // Place button above "Back to Game" in the pause menu
        this.addButton(
            new Button(this.width / 2 - 100, this.height / 4 - 34, 200, 20, new TextComponent("Carpet Players"),
                button -> net.minecraft.client.Minecraft.getInstance().setScreen(
                    new CarpetPlayersScreen(this)
                )
            )
        );
    }
}
