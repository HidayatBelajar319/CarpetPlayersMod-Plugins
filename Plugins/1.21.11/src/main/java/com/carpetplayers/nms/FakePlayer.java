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
 * Entitas player palsu berbasis NMS Paper 1.21.11 (Mojang-mapped). Bertindak seperti
 * player sungguhan untuk keperluan PvP dan interaksi, namun menggunakan koneksi palsu.
 */
public class FakePlayer extends ServerPlayer {

    public boolean isFake = true;

    // Input movement yang dikontrol bot engine (di-reset tiap tick oleh world).
    private float inputForward;
    private float inputStrafe;
    private boolean inputJump;

    public FakePlayer(MinecraftServer server, ServerLevel world, GameProfile profile) {
        super(server, world, profile, ClientInformation.createDefault());
        setupDummyConnection(server);
        // Ctor ServerGamePacketListenerImpl menimpa level player ke overworld
        // (player.setServerLevel(server.overworld())). Kembalikan ke world target
        // agar registrasi & despawn konsisten dengan world tempat bot di-spawn.
        this.setServerLevel(world);
    }

    private void setupDummyConnection(MinecraftServer server) {
        try {
            // Catatan: nama sederhana "Connection" di-shadow oleh nested type
            // WaypointTransmitter.Connection (diwariskan ServerPlayer), jadi
            // wajib pakai nama berkualifikasi penuh di sini.
            net.minecraft.network.Connection connection = new net.minecraft.network.Connection(PacketFlow.CLIENTBOUND);
            EmbeddedChannel channel = new EmbeddedChannel();
            channel.close().syncUninterruptibly();
            connection.channel = channel;
            this.connection = new FakePlayerConnection(server, connection, this);
        } catch (Exception e) {
            // Fallback: tanpa koneksi, bot tetap bisa didaftarkan ke world
        }
    }

    // ============ Helper posisi (warisan dari Entity Mojang-mapped) ============
    // getX()/getY()/getZ() bersifat final di Entity, dan distanceToSqr(Entity) /
    // distanceToSqr(double,double,double) sudah tersedia dengan implementasi yang
    // identik. Kontrak publik terpenuhi lewat pewarisan — tidak perlu dioverride.

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

    public void moveLocation(double x, double y, double z, float yaw, float pitch) {
        // Entity.moveTo(double,double,double,float,float) TIDAK ada di 1.21.11
        // (sudah dihapus/renamed). Fallback aman: setPos + setYRot + setXRot + setYHeadRot.
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
     * Menerapkan gerakan manual berdasarkan input. Tidak bergantung pada
     * field input internal NMS (xxa/zza tidak tersedia public), sehingga
     * pergerakan bot deterministik.
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
        double speed = 0.22D;
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
            // Format & broadcast pesan diserahkan ke plugin/lane pemanggil;
            // di sini cukup kirim lewat Bukkit agar tidak memakai NMS chat intern.
            bukkit.sendMessage("<" + getName().getString() + "> " + message);
        }
    }

    // ============ Bukkit ============

    public CraftPlayer getBukkitPlayer() {
        return (CraftPlayer) getBukkitEntity();
    }

    // ============ Bantuan misc ============

    public boolean isBotAlive() {
        return isAlive();
    }

    public AABB getBoundingBoxInflated(double radius) {
        return getBoundingBox().inflate(radius);
    }

    // ============ Helper yang dipakai BotBrain ============

    public MinecraftServer getServer() {
        // Field server di ServerPlayer bersifat private di Mojang-mapped;
        // MinecraftServer.getServer() (static, diverifikasi) adalah akses yang aman.
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
