package com.carpetplayers.mixin;

import carpet.patches.EntityPlayerMPFake;
import com.carpetplayers.ai.AIController;
import com.carpetplayers.ai.AIProviderManager;
import com.carpetplayers.bot.BotBrain;
import com.carpetplayers.bot.BotManager;
import com.carpetplayers.config.ModConfig;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {
    @Shadow
    public ServerPlayer player;

    @Inject(method = "handleChat(Ljava/lang/String;)V", at = @At("HEAD"), cancellable = true)
    private void carpetplayers$onHandleChat(String message, CallbackInfo ci) {
        if (message.startsWith("/")) {
            return;
        }
        ServerPlayer sender = this.player;
        if (sender == null) {
            return;
        }
        EntityPlayerMPFake controlled = BotManager.CONTROLLED.get(sender.getUUID());
        if (controlled != null && controlled.isAlive()) {
            ci.cancel();
            broadcastAsBot(controlled, message);
            return;
        }
        if (ModConfig.instance.interactiveEnabled) {
            String lower = message.toLowerCase();
            String command = extractCommand(lower);
            boolean aiChat = AIProviderManager.instance().isEnabled()
                    && AIProviderManager.instance().isChatEnabled();
            boolean addressed = false;
            for (BotBrain brain : BotManager.BRAINS.values()) {
                if (lower.contains(brain.getBotName().toLowerCase())) {
                    addressed = true;
                    if (aiChat) {
                        AIController.runChat(brain.getBotName(), message);
                    } else if (command != null) {
                        brain.handleChatCommand(command);
                    } else {
                        brain.pendingReply = getReply();
                    }
                }
            }
            if (!addressed && command != null && lower.startsWith("bot ")) {
                for (BotBrain brain : BotManager.BRAINS.values()) {
                    if (aiChat) {
                        AIController.runChat(brain.getBotName(), message);
                    } else {
                        brain.handleChatCommand(command);
                    }
                    break;
                }
            }
        }
    }

    private static void broadcastAsBot(EntityPlayerMPFake bot, String message) {
        bot.getServer().getPlayerList().broadcastMessage(
                new TextComponent("<" + bot.getName().getString() + "> " + message),
                ChatType.CHAT, bot.getUUID());
    }

    private static String extractCommand(String lower) {
        if (lower.contains("follow") || lower.contains("ikut")) {
            return "follow";
        }
        if (lower.contains("stop") || lower.contains("berhenti") || lower.contains("diam")) {
            return "stop";
        }
        if (lower.contains("pvp") || lower.contains("bertarung")) {
            return "pvp";
        }
        if (lower.contains("chill") || lower.contains("santai")) {
            return "chill";
        }
        if (lower.contains("wander") || lower.contains("jalan")) {
            return "wander";
        }
        if (lower.contains("makan") || lower.contains("eat")) {
            return "makan";
        }
        if (lower.contains("menu")) {
            return "menu";
        }
        return null;
    }

    private static String getReply() {
        String[] replies = {
                "Ya? Kamu memanggilku?",
                "Halo juga!",
                "Apa yang bisa kubantu?",
                "Siap!",
                "Aku di sini."
        };
        return replies[(int) (Math.random() * replies.length)];
    }
}
