package com.carpetplayers.bot;

import carpet.patches.EntityPlayerMPFake;
import com.carpetplayers.ai.AIProviderManager;
import com.carpetplayers.config.ModConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Offline behavior tree for bots when AI provider is unavailable.
 * Simple decision tree: Fight → Eat → Follow Owner → Wander
 */
public class OfflineBehavior {

    private static final Random RANDOM = new Random();
    private static final double FOLLOW_DISTANCE = 8.0;
    private static final double WANDER_RADIUS = 16.0;
    private static final double ATTACK_RANGE = 3.5;
    private static final float LOW_HEALTH_THRESHOLD = 0.3f;
    private static final float LOW_FOOD_THRESHOLD = 6.0f; // hunger level (0-20)

    private final BotBrain brain;
    private final EntityPlayerMPFake bot;
    private long lastDecisionTime = 0;
    private long lastWanderTime = 0;
    private BlockPos wanderTarget = null;
    private UUID ownerUuid = null;
    private int stuckCounter = 0;
    private BlockPos lastPos = null;

    public OfflineBehavior(BotBrain brain) {
        this.brain = brain;
        this.bot = brain.getBot();
    }

    /**
     * Main tick - called from BotBrain when offlineMode is enabled
     */
    public void tick() {
        if (!ModConfig.instance.aiConfig.offlineMode) {
            return;
        }

        // Check if AI provider is available - if so, don't run offline behavior
        AIProviderManager manager = AIProviderManager.instance();
        if (manager.getActiveProvider() != null) {
            return; // AI is available, let normal AI handle it
        }

        long now = System.currentTimeMillis();
        if (now - lastDecisionTime < 2000) { // Decision every 2 seconds (40 ticks)
            executeCurrentAction();
            return;
        }
        lastDecisionTime = now;

        // Decision tree priority:
        // 1. Defend self if attacked
        // 2. Eat if low health/food
        // 3. Follow owner if nearby
        // 4. Attack hostile mobs nearby
        // 5. Wander

        if (defendSelf()) return;
        if (eatIfNeeded()) return;
        if (followOwner()) return;
        if (attackNearbyHostile()) return;
        wander();
    }

    private void executeCurrentAction() {
        // Continue current movement/action
        // BotBrain handles movement execution
    }

    /**
     * 1. Defend self - if being attacked, fight back
     */
    private boolean defendSelf() {
        // Check if bot was recently hurt
        if (bot.hurtTime > 0) {
            // Find attacker
            LivingEntity attacker = bot.getLastHurtByMob();
            if (attacker != null && attacker.isAlive() && bot.distanceTo(attacker) <= ATTACK_RANGE * 2) {
                attackEntity(attacker);
                return true;
            }
        }
        return false;
    }

    /**
     * 2. Eat if low health or low food
     */
    private boolean eatIfNeeded() {
        float healthPct = bot.getHealth() / bot.getMaxHealth();
        int foodLevel = bot.getFoodData().getFoodLevel();

        if (healthPct < LOW_HEALTH_THRESHOLD || foodLevel < LOW_FOOD_THRESHOLD) {
            // Find food in inventory
            for (int i = 0; i < bot.inventory.getContainerSize(); i++) {
                ItemStack stack = bot.inventory.getItem(i);
                if (stack.getItem().isEdible()) {
                    useItem(i);
                    return true;
                }
            }
            // No food - try to find animals to kill for food
            if (healthPct < LOW_HEALTH_THRESHOLD) {
                return huntForFood();
            }
        }
        return false;
    }

    private boolean huntForFood() {
        ServerLevel level = (ServerLevel) bot.level;
        List<LivingEntity> animals = level.getEntitiesOfClass(LivingEntity.class,
                new AABB(bot.blockPosition()).inflate(16),
                e -> e != bot && e.isAlive() && isFoodSource(e));

        if (!animals.isEmpty()) {
            LivingEntity target = animals.get(0);
            moveTo(target.blockPosition());
            if (bot.distanceTo(target) <= ATTACK_RANGE) {
                attackEntity(target);
            }
            return true;
        }
        return false;
    }

    private boolean isFoodSource(LivingEntity entity) {
        // Cows, pigs, sheep, chickens drop food
        String name = entity.getType().toString().toLowerCase();
        return name.contains("cow") || name.contains("pig") ||
               name.contains("sheep") || name.contains("chicken") ||
               name.contains("rabbit");
    }

    /**
     * 3. Follow owner if nearby
     */
    private boolean followOwner() {
        if (ownerUuid == null) {
            // Try to find owner from bot's custom name or persistent data
            // For now, find nearest player
            ServerLevel level = (ServerLevel) bot.level;
            List<ServerPlayer> players = level.getPlayers(p -> p.distanceTo(bot) <= FOLLOW_DISTANCE * 2);
            if (!players.isEmpty()) {
                ownerUuid = players.get(0).getUUID();
            }
        }

        if (ownerUuid != null) {
            Player owner = ((ServerLevel) bot.level).getPlayerByUUID(ownerUuid);
            if (owner != null && owner.isAlive()) {
                double dist = bot.distanceTo(owner);
                if (dist > FOLLOW_DISTANCE) {
                    moveTo(owner.blockPosition());
                    return true;
                } else if (dist < 2.0) {
                    // Too close - back up a bit
                    brain.setMovement(0.0F, 0.0F);
                    return true;
                }
            } else {
                ownerUuid = null; // Owner gone
            }
        }
        return false;
    }

    /**
     * 4. Attack nearby hostile mobs
     */
    private boolean attackNearbyHostile() {
        ServerLevel level = (ServerLevel) bot.level;
        List<LivingEntity> hostiles = level.getEntitiesOfClass(LivingEntity.class,
                new AABB(bot.blockPosition()).inflate(16),
                e -> e != bot && e.isAlive() && isHostile(e));

        if (!hostiles.isEmpty()) {
            LivingEntity target = hostiles.get(0);
            moveTo(target.blockPosition());
            if (bot.distanceTo(target) <= ATTACK_RANGE) {
                attackEntity(target);
            }
            return true;
        }
        return false;
    }

    private boolean isHostile(LivingEntity entity) {
        // Check if entity is hostile (monster)
        return entity.getType().getCategory() == net.minecraft.world.entity.MobCategory.MONSTER;
    }

    /**
     * 5. Wander randomly
     */
    private void wander() {
        long now = System.currentTimeMillis();
        if (now - lastWanderTime < 10000 + RANDOM.nextInt(20000)) { // 10-30 seconds
            // Continue current wander
            if (wanderTarget != null) {
                moveTo(wanderTarget);
                checkStuck();
            }
            return;
        }

        // Pick new wander target
        ServerLevel level = (ServerLevel) bot.level;
        int attempts = 0;
        int minY = level.getHeight(); // Use getHeight() as fallback for min build height
        while (attempts < 10) {
            int dx = RANDOM.nextInt((int) WANDER_RADIUS * 2) - (int) WANDER_RADIUS;
            int dz = RANDOM.nextInt((int) WANDER_RADIUS * 2) - (int) WANDER_RADIUS;
            BlockPos target = bot.blockPosition().offset(dx, 0, dz);

            // Find safe Y level
            for (int y = target.getY(); y > minY; y--) {
                BlockPos checkPos = new BlockPos(target.getX(), y, target.getZ());
                if (level.getBlockState(checkPos).isAir() &&
                    level.getBlockState(checkPos.above()).isAir() &&
                    !level.getBlockState(checkPos.below()).isAir()) {
                    wanderTarget = checkPos;
                    lastWanderTime = now;
                    moveTo(wanderTarget);
                    return;
                }
            }
            attempts++;
        }
    }

    private void checkStuck() {
        if (lastPos != null && bot.blockPosition().distSqr(lastPos) < 0.5) {
            stuckCounter++;
            if (stuckCounter > 20) { // Stuck for ~40 seconds
                wanderTarget = null; // Force new wander target
                stuckCounter = 0;
            }
        } else {
            stuckCounter = 0;
        }
        lastPos = bot.blockPosition();
    }

    // Helper methods using BotBrain's API

    private void attackEntity(LivingEntity target) {
        brain.actions().lookAt(target.getEyePosition(1.0F));
        brain.actions().start(carpet.helpers.EntityPlayerActionPack.ActionType.ATTACK,
                carpet.helpers.EntityPlayerActionPack.Action.once());
    }

    private void useItem(int slot) {
        bot.inventory.selected = slot;
        bot.gameMode.useItem(bot, bot.getLevel(), bot.getMainHandItem(), net.minecraft.world.InteractionHand.MAIN_HAND);
    }

    private void moveTo(BlockPos target) {
        brain.actions().lookAt(Vec3.atCenterOf(target));
        // Calculate direction to target
        Vec3 toTarget = Vec3.atCenterOf(target).subtract(bot.position());
        double dist = toTarget.length();
        if (dist > 1.0) {
            // Simple movement towards target
            brain.setMovement(1.0F, 0.0F);
        } else {
            brain.setMovement(0.0F, 0.0F);
        }
    }

    /**
     * Handle chat commands without AI
     */
    public boolean handleChatCommand(String command, ServerPlayer sender) {
        String cmd = command.toLowerCase().trim();

        if (cmd.startsWith("follow")) {
            ownerUuid = sender.getUUID();
            moveTo(sender.blockPosition());
            sender.sendMessage(new net.minecraft.network.chat.TextComponent("§a[Offline] Bot will follow you."), sender.getUUID());
            return true;
        }

        if (cmd.equals("stop") || cmd.equals("stay")) {
            ownerUuid = null;
            brain.setMovement(0.0F, 0.0F);
            sender.sendMessage(new net.minecraft.network.chat.TextComponent("§a[Offline] Bot stopped."), sender.getUUID());
            return true;
        }

        if (cmd.equals("wander")) {
            ownerUuid = null;
            wanderTarget = null;
            sender.sendMessage(new net.minecraft.network.chat.TextComponent("§a[Offline] Bot will wander."), sender.getUUID());
            return true;
        }

        if (cmd.equals("chill") || cmd.equals("idle")) {
            ownerUuid = null;
            brain.setMovement(0.0F, 0.0F);
            sender.sendMessage(new net.minecraft.network.chat.TextComponent("§a[Offline] Bot idling."), sender.getUUID());
            return true;
        }

        if (cmd.equals("eat")) {
            if (eatIfNeeded()) {
                sender.sendMessage(new net.minecraft.network.chat.TextComponent("§a[Offline] Bot eating."), sender.getUUID());
            } else {
                sender.sendMessage(new net.minecraft.network.chat.TextComponent("§c[Offline] No food in inventory."), sender.getUUID());
            }
            return true;
        }

        if (cmd.startsWith("goto ")) {
            String coords = cmd.substring(5).trim();
            String[] parts = coords.split(" ");
            if (parts.length >= 3) {
                try {
                    int x = Integer.parseInt(parts[0]);
                    int y = Integer.parseInt(parts[1]);
                    int z = Integer.parseInt(parts[2]);
                    BlockPos target = new BlockPos(x, y, z);
                    moveTo(target);
                    sender.sendMessage(new net.minecraft.network.chat.TextComponent("§a[Offline] Moving to " + target), sender.getUUID());
                    return true;
                } catch (NumberFormatException e) {
                    sender.sendMessage(new net.minecraft.network.chat.TextComponent("§c[Offline] Invalid coordinates."), sender.getUUID());
                    return true;
                }
            }
        }

        return false;
    }
}