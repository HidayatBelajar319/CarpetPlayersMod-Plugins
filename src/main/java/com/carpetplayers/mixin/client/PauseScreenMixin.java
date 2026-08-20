package com.carpetplayers.mixin.client;

import com.carpetplayers.client.gui.CarpetPlayersScreen;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TextComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin extends Screen {
    protected PauseScreenMixin(TextComponent title) {
        super(title);
    }

    @Shadow
    protected abstract <T extends AbstractWidget> T addButton(T button);

    @Inject(method = "init", at = @At("RETURN"))
    private void carpetplayers_addPauseMenuButton(CallbackInfo ci) {
        this.addButton(
            new Button(4, 4, 120, 20, new TextComponent("Carpet Players"),
                button -> net.minecraft.client.Minecraft.getInstance().setScreen(
                    new CarpetPlayersScreen(this)
                )
            )
        );
    }
}
