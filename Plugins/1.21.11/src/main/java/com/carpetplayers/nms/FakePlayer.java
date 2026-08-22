package com.carpetplayers.nms;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.bukkit.craftbukkit.entity.CraftPlayer;

import java.util.Collection;

/**
 * Fake player entity based on Paper 1.21.11 NMS (Mojang-mapped). Behaves like a
 * real player for PvP and interaction purposes, but uses a fake connection.
 */
public class FakePlayer extends ServerPlayer {

    public boolean isFake = true;

    // Movement input controlled by the bot engine (reset every tick by the world).
    private float inputForward;
    private float inputStrafe;
    private boolean inputJump;
    private boolean inputSprint;

    public FakePlayer(MinecraftServer server, ServerLevel world, GameProfile profile) {
        super(server, world, profile, ClientInformation.createDefault());
        setupDummyConnection(server);
        // The ServerGamePacketListenerImpl constructor overrides the player level to overworld
        // (player.setServerLevel(server.overworld())). Restore the target world so registration
        // and despawn stay consistent with the world where the bot was spawned.
        this.setServerLevel(world);
    }

    private void setupDummyConnection(MinecraftServer server) {
        try {
            // Note: the simple name "Connection" is shadowed by the nested type
            // WaypointTransmitter.Connection (inherited by ServerPlayer), so the
            // fully qualified name must be used here.
            net.minecraft.network.Connection connection = new net.minecraft.network.Connection(PacketFlow.CLIENTBOUND);
            EmbeddedChannel channel = new EmbeddedChannel();
            channel.close().syncUninterruptibly();
            connection.channel = channel;
            this.connection = new FakePlayerConnection(server, connection, this);
        } catch (Exception e) {
            // Fallback: without a connection, the bot can still be registered into the world
        }
    }

    // ============ Position helpers (inherited from Mojang-mapped Entity) ============
    // getX()/getY()/getZ() are final on Entity, and distanceToSqr(Entity) /
    // distanceToSqr(double,double,double) are already available with identical
    // implementations. The public contract is satisfied through inheritance - no override needed.

    // ============ Equipment & movement ============

    public ItemStack getItemInMainHand() {
        return getMainHandItem();
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack item) {
        super.setItemSlot(slot, item);
    }

    public void setMovementInput(float forward, float strafe, boolean jumping) {
        this.inputForward = forward;
        this.inputStrafe = strafe;
        this.inputJump = jumping;
    }

    public void setSprinting(boolean sprinting) {
        this.inputSprint = sprinting;
    }

    public boolean isSprinting() {
        return inputSprint;
    }

    public void moveLocation(double x, double y, double z, float yaw, float pitch) {
        // Entity.moveTo(double,double,double,float,float) does NOT exist in 1.21.11
        // (removed/renamed). Safe fallback: setPos + setYRot + setXRot + setYHeadRot.
        setPos(x, y, z);
        setYRot(yaw);
        setXRot(pitch);
        setYHeadRot(yaw);
    }

    @Override
    public void tick() {
        super.tick();
        applyManualMovement();
    }

    /**
     * Applies manual movement based on input. It does not rely on NMS internal
     * input fields (xxa/zza are not public), so bot movement is deterministic.
     */
    private void applyManualMovement() {
        if (inputForward == 0.0F && inputStrafe == 0.0F) {
            return;
        }
        double rad = Math.toRadians(getYRot());
        double forward = inputForward;
        double strafe = inputStrafe;
        double fx = -Math.sin(rad) * forward;
        double fz = Math.cos(rad) * forward;
        double sx = Math.cos(rad) * strafe;
        double sz = Math.sin(rad) * strafe;
        double speed = inputSprint ? 0.28D : 0.22D;
        Vec3 mot = getDeltaMovement();
        setDeltaMovement((fx + sx) * speed, mot.y, (fz + sz) * speed);
        if (inputJump && onGround()) {
            setDeltaMovement(mot.x, 0.42D, mot.z);
        }
    }

    // ============ Chat ============

    public void sendChat(String message) {
        CraftPlayer bukkit = getBukkitPlayer();
        if (bukkit != null) {
            // Message formatting and broadcast are left to the calling plugin/layer;
            // here it is enough to send through Bukkit to avoid internal NMS chat.
            bukkit.sendMessage("<" + getName().getString() + "> " + message);
        }
    }

    // ============ Bukkit ============

    public CraftPlayer getBukkitPlayer() {
        return (CraftPlayer) getBukkitEntity();
    }

    // ============ Misc helpers ============

    public boolean isBotAlive() {
        return isAlive();
    }

    public AABB getBoundingBoxInflated(double radius) {
        return getBoundingBox().inflate(radius);
    }

    // ============ Helpers used by BotBrain ============

    public MinecraftServer getServer() {
        // The server field on ServerPlayer is private in Mojang-mapped;
        // MinecraftServer.getServer() (static, verified) is a safe access.
        return MinecraftServer.getServer();
    }

    @Override
    public BlockPos blockPosition() {
        return super.blockPosition();
    }

    @Override
    public FoodData getFoodData() {
        return super.getFoodData();
    }

    @Override
    public Collection<MobEffectInstance> getActiveEffects() {
        return super.getActiveEffects();
    }

    public int getHurtTicks() {
        return hurtTime;
    }

    @Override
    public boolean isUsingItem() {
        return super.isUsingItem();
    }

    @Override
    public ItemStack getUseItem() {
        ItemStack stack = super.getUseItem();
        return stack != null ? stack : ItemStack.EMPTY;
    }

    @Override
    public void startUsingItem(InteractionHand hand) {
        super.startUsingItem(hand);
    }

    @Override
    public void releaseUsingItem() {
        super.releaseUsingItem();
    }

    public void setSneak(boolean sneak) {
        setShiftKeyDown(sneak);
    }

    public void lookAt(double x, double y, double z) {
        double dx = x - getX();
        double dy = y - (getY() + getEyeHeight());
        double dz = z - getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0F);
        float pitch = (float) (-Math.toDegrees(Math.atan2(dy, Math.max(horizontal, 0.0001D))));
        setYRot(yaw);
        setXRot(pitch);
    }

    public void lookRotation(float yaw, float pitch) {
        setYRot(yaw);
        setXRot(pitch);
    }

    @Override
    public void setJumping(boolean jumping) {
        this.jumping = jumping;
        super.setJumping(jumping);
    }

    public boolean isJumpingRequested() {
        return jumping;
    }

    public float getYaw() {
        return getYRot();
    }

    public float getPitch() {
        return getXRot();
    }
}
