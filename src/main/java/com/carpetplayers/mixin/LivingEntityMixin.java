package com.carpetplayers.mixin;

import carpet.patches.EntityPlayerMPFake;
import com.carpetplayers.ai.AIController;
import com.carpetplayers.ai.AIProviderManager;
import com.carpetplayers.bot.BotBrain;
import com.carpetplayers.bot.BotManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "hurt", at = @At("HEAD"))
    private void carpetplayers$onHurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        Object self = this;
        if (!(self instanceof EntityPlayerMPFake)) {
            return;
        }
        Entity attacker = source.getEntity();
        if (!(attacker instanceof ServerPlayer)) {
            return;
        }
        BotBrain brain = BotManager.BRAINS.get(((Entity) self).getUUID());
        if (brain != null) {
            brain.onAttacked(attacker);
            if (AIProviderManager.instance().isEnabled()
                    && AIProviderManager.instance().isDefensiveEnabled()) {
                String attackerName = attacker.getName().getString();
                AIController.runChat(brain.getBotName(),
                        "Kamu baru saja diserang oleh pemain " + attackerName
                                + ". Bereaksilah sesuai keadaanmu (bisa membalas, kabur, atau meminta bantuan).");
            }
        }
    }
}
