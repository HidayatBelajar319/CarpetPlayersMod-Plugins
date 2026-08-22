package com.carpetplayers.nms;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.v1_16_R3.AxisAlignedBB;
import net.minecraft.server.v1_16_R3.BlockPosition;
import net.minecraft.server.v1_16_R3.ChatComponentText;
import net.minecraft.server.v1_16_R3.ChatMessageType;
import net.minecraft.server.v1_16_R3.Entity;
import net.minecraft.server.v1_16_R3.EntityPlayer;
import net.minecraft.server.v1_16_R3.EnumHand;
import net.minecraft.server.v1_16_R3.EnumItemSlot;
import net.minecraft.server.v1_16_R3.EnumProtocolDirection;
import net.minecraft.server.v1_16_R3.FoodMetaData;
import net.minecraft.server.v1_16_R3.IChatBaseComponent;
import net.minecraft.server.v1_16_R3.ItemStack;
import net.minecraft.server.v1_16_R3.MinecraftServer;
import net.minecraft.server.v1_16_R3.MobEffect;
import net.minecraft.server.v1_16_R3.NetworkManager;
import net.minecraft.server.v1_16_R3.PlayerInteractManager;
import net.minecraft.server.v1_16_R3.Vec3D;
import net.minecraft.server.v1_16_R3.WorldServer;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;

import java.util.Collection;

/**
 * Entitas player palsu berbasis NMS v1_16_R3. Bertindak seperti player sungguhan
 * untuk keperluan PvP dan interaksi, namun menggunakan koneksi palsu.
 */
public class FakePlayer extends EntityPlayer {

    public boolean isFake = true;

    // Input movement yang dikontrol bot engine (di-reset tiap tick oleh world).
    private float inputForward;
    private float inputStrafe;
    private boolean inputJump;
    private boolean inputSprint;

    public FakePlayer(MinecraftServer server, WorldServer world, GameProfile profile,
                      PlayerInteractManager interactManager) {
        super(server, world, profile, interactManager);
        setupDummyConnection(server);
    }

    private void setupDummyConnection(MinecraftServer server) {
        try {
            NetworkManager networkManager = new NetworkManager(EnumProtocolDirection.CLIENTBOUND);
            io.netty.channel.embedded.EmbeddedChannel channel = new io.netty.channel.embedded.EmbeddedChannel();
            channel.close().syncUninterruptibly();
            networkManager.channel = channel;
            this.playerConnection = new FakePlayerConnection(server, networkManager, this);
        } catch (Exception e) {
            // Fallback: tanpa koneksi, bot tetap bisa didaftarkan ke world
        }
    }

    // ============ Helper posisi (setara Mojang getX/getY/getZ) ============

    public double getX() {
        return locX();
    }

    public double getY() {
        return locY();
    }

    public double getZ() {
        return locZ();
    }

    public double distanceToSqr(Entity entity) {
        return distanceToSqr(entity.locX(), entity.locY(), entity.locZ());
    }

    public double distanceToSqr(double x, double y, double z) {
        double dx = getX() - x;
        double dy = getY() - y;
        double dz = getZ() - z;
        return dx * dx + dy * dy + dz * dz;
    }

    // ============ Equipment & movement ============

    public ItemStack getItemInMainHand() {
        return getEquipment(EnumItemSlot.MAINHAND);
    }

    public void setItemSlot(EnumItemSlot slot, ItemStack item) {
        setSlot(slot, item);
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
        this.setLocation(x, y, z, yaw, pitch);
    }

    @Override
    public void tick() {
        super.tick();
        applyManualMovement();
    }

    /**
     * Menerapkan gerakan manual berdasarkan input. Tidak bergantung pada
     * field input internal NMS (xxa/zza tidak tersedia public), sehingga
     * pergerakan bot deterministik.
     */
    private void applyManualMovement() {
        if (inputForward == 0.0F && inputStrafe == 0.0F) {
            return;
        }
        double rad = Math.toRadians(yaw);
        double forward = inputForward;
        double strafe = inputStrafe;
        double fx = -Math.sin(rad) * forward;
        double fz = Math.cos(rad) * forward;
        double sx = Math.cos(rad) * strafe;
        double sz = Math.sin(rad) * strafe;
        double speed = inputSprint ? 0.28D : 0.22D;
        Vec3D mot = getMot();
        setMot((fx + sx) * speed, mot.y, (fz + sz) * speed);
        if (inputJump && onGround) {
            setMot(mot.x, 0.42D, mot.z);
        }
    }

    // ============ Chat ============

    public void sendChat(String message) {
        IChatBaseComponent component = new ChatComponentText("<" + getName() + "> " + message);
        this.server.getPlayerList().sendMessage(component, ChatMessageType.CHAT, getUniqueID());
    }

    // ============ Bukkit ============

    public CraftPlayer getBukkitPlayer() {
        return (CraftPlayer) getBukkitEntity();
    }

    // ============ Bantuan misc ============

    public boolean isBotAlive() {
        return isAlive();
    }

    public AxisAlignedBB getBoundingBoxInflated(double radius) {
        return getBoundingBox().grow(radius);
    }

    // ============ Helper yang dipakai BotBrain ============

    public MinecraftServer getServer() {
        return this.server;
    }

    public BlockPosition blockPosition() {
        return getChunkCoordinates();
    }

    public FoodMetaData getFoodData() {
        return foodData;
    }

    public Collection<MobEffect> getActiveEffects() {
        return getEffects();
    }

    public int getHurtTicks() {
        return hurtTicks;
    }

    public boolean isUsingItem() {
        return activeItem != null;
    }

    public ItemStack getUseItem() {
        return activeItem != null ? activeItem : ItemStack.b;
    }

    public void startUsingItem(EnumHand hand) {
        this.c(hand);
    }

    public void releaseUsingItem() {
        this.clearActiveItem();
    }

    public void setSneak(boolean sneak) {
        setSneaking(sneak);
    }

    public void lookAt(double x, double y, double z) {
        double dx = x - locX();
        double dy = y - (locY() + getHeadHeight());
        double dz = z - locZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0F);
        float pitch = (float) (-Math.toDegrees(Math.atan2(dy, Math.max(horizontal, 0.0001D))));
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public void lookRotation(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
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
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }
}