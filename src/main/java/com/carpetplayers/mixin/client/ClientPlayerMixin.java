package com.carpetplayers.mixin.client;

import com.carpetplayers.network.ModPackets;
import com.carpetplayers.waypoint.WaypointManager;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Detects local player death on client side.
 * Sends death position to server for death waypoint creation.
 */
@Mixin(LocalPlayer.class)
public abstract class ClientPlayerMixin {

    @Inject(method = "die", at = @At("HEAD"))
    private void carpetplayers$onDeath(net.minecraft.world.damagesource.DamageSource source, CallbackInfo ci) {
        LocalPlayer self = (LocalPlayer) (Object) this;
        if (self.getHealth() <= 0 && !self.level.isClientSide) {
            return; // server side, ignore
        }
        // Client side: send death position to server
        if (self.level.isClientSide) {
            ResourceKey<Level> dimKey = self.level.dimension();
            String dim = WaypointManager.dimensionToString(dimKey);

            FriendlyByteBuf buf = PacketByteBufs.create();
            buf.writeDouble(self.getX());
            buf.writeDouble(self.getY());
            buf.writeDouble(self.getZ());
            buf.writeUtf(dim);
            ClientPlayNetworking.send(ModPackets.DEATH_WAYPOINT, buf);
        }
    }
}
