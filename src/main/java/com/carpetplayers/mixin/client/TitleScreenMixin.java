package com.carpetplayers.mixin.client;

import com.carpetplayers.client.gui.CarpetPlayersScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.TextComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
    protected TitleScreenMixin(TextComponent title) {
        super(title);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void carpetplayers_addMenuButton(CallbackInfo ci) {
        // Place button right below the Options/Quit row (height/4 + 148 is Options row)
        int centerX = this.width / 2;
        this.addButton(
            new Button(centerX - 100, this.height / 4 + 172, 200, 20, new TextComponent("Carpet Players"),
                button -> net.minecraft.client.Minecraft.getInstance().setScreen(
                    new CarpetPlayersScreen(this)
                )
            )
        );
    }
}
