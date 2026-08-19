package com.carpetplayers.bot;

import com.carpetplayers.CarpetPlayersPlugin;
import com.carpetplayers.config.ModConfig;
import com.carpetplayers.nms.FakePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;

public class BotBrain {

    public enum BotState {
        FOLLOW, WANDER, PVP, CHILL, EAT
    }

    private static final String[] CHAT_MESSAGES = {
            "Hello!",
            "How are you?",
            "Nice weather, huh?",
            "Let's fight!",
            "I need food"
    };

    protected final FakePlayer bot;
    protected final UUID uuid;
    protected final Random random;

    protected Vec3 wanderTarget;
    protected int wanderCooldown;
    protected int chatCooldown;
    protected int attackCooldown;
    protected int eatCooldown;
    protected int strafeTicks;
    protected int lastStrafeDirection;
    protected int throwCooldown;
    protected int bowChargeTicks;
    protected int mineCooldown;

    protected Entity target;
    protected int lastMoveDirection;
    protected boolean pendingTapAttack;

    protected BotState state = BotState.FOLLOW;
    protected UUID ownerUuid;

    protected final List<AIAction> aiQueue = new ArrayList<>();
    protected boolean aiQueueActive;
    protected float aiLastForward;
    protected float aiLastStrafe;

    public String pendingReply;

    private Vec3 controllerLastPos;
    private float controllerPrevAttackScale;

    public BotBrain(FakePlayer bot) {
        this.bot = bot;
        this.uuid = bot.getUUID();
        this.random = new Random(uuid.getMostSignificantBits() ^ (long) (uuid.hashCode() * 31) ^ System.nanoTime());
        this.chatCooldown = random.nextInt(600) + 600;
        this.controllerPrevAttackScale = -1.0F;
    }

    public FakePlayer getBot() {
        return bot;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getBotName() {
        // Entity.getName() in Mojang-mapped 1.21.11 returns a Component -> extract its String text.
        return bot.getName().getString();
    }

    public BotState getState() {
        return state;
    }

    // ----- AI tool API -----

    protected enum AiActionType {
        MOVE, JUMP, SNEAK, ATTACK
    }

    protected static class AIAction {
        final AiActionType type;
        final float forward;
        final float strafe;
        final boolean sneak;
        int remaining;

        AIAction(AiActionType type, float forward, float strafe, boolean sneak, int ticks) {
            this.type = type;
            this.forward = forward;
            this.strafe = strafe;
            this.sneak = sneak;
            this.remaining = ticks;
        }
    }

    public void aiSetState(BotState state) {
        this.state = state;
        aiStop();
    }

    public String aiGetStateInfo() {
        try {
            String pos = String.format(Locale.US, "(%.1f,%.1f,%.1f)", bot.getX(), bot.getY(), bot.getZ());
            String dim = bot.level().dimension().identifier().toString();
            String hand;
            try {
                ItemStack main = bot.getItemInMainHand();
                if (main == null || main.isEmpty()) {
                    hand = "empty";
                } else {
                    hand = main.getHoverName().getString();
                    if (hand == null || hand.isEmpty()) {
                        hand = main.getItem().getDescriptionId();
                    }
                }
            } catch (Exception e) {
                hand = "unknown";
            }
            return String.format(Locale.US,
                    "[Bot %s] pos=%s dim=%s hp=%.1f/%.1f food=%d hand=%s state=%s",
                    getBotName(), pos, dim, bot.getHealth(), bot.getMaxHealth(),
                    bot.getFoodData().getFoodLevel(), hand, state);
        } catch (Exception e) {
            return "[Bot " + getBotName() + "] state=" + state;
        }
    }

    public void aiMove(float forward, float strafe, int ticks) {
        if (ticks <= 0) {
            return;
        }
        aiQueue.clear();
        aiQueueActive = true;
        bot.setJumping(false);
        setSneaking(false);
        aiLastForward = forward;
        aiLastStrafe = strafe;
        aiQueue.add(new AIAction(AiActionType.MOVE, forward, strafe, false, ticks));
    }

    public void aiJump(int ticks) {
        if (ticks <= 0) {
            return;
        }
        aiQueueActive = true;
        aiQueue.add(new AIAction(AiActionType.JUMP, 0.0F, 0.0F, false, ticks));
    }

    public void aiSneak(boolean sneak, int ticks) {
        if (ticks <= 0) {
            return;
        }
        aiQueueActive = true;
        aiQueue.add(new AIAction(AiActionType.SNEAK, 0.0F, 0.0F, sneak, ticks));
    }

    public void aiLookAt(double x, double y, double z) {
        bot.lookAt(x, y, z);
    }

    public void aiAttack(String targetName) {
        if (targetName == null) {
            return;
        }
        ServerPlayer player = bot.getServer().getPlayerList().getPlayer(targetName);
        if (player == null) {
            return;
        }
        setTarget(player);
        aiQueueActive = true;
        aiQueue.add(new AIAction(AiActionType.ATTACK, 0.0F, 0.0F, false, 1));
    }

    public void aiEat() {
        tryEat();
    }

    public void aiChat(String message) {
        if (message == null) {
            return;
        }
        broadcastBot(message);
    }

    public void aiMineAt(int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        ServerLevel level = bot.level();
        if (level == null || !level.isLoaded(pos)) {
            return;
        }
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return;
        }
        Material material = state.getBukkitMaterial();
        if (material == Material.LAVA || material == Material.WATER || material == Material.FIRE) {
            return;
        }
        if (state.getDestroySpeed(level, pos) < 0.0F) {
            return;
        }
        bot.lookAt(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        breakBlockAt(pos);
    }

    public void aiMineNearest() {
        BlockPos mine = findMineableBlock();
        if (mine == null) {
            return;
        }
        bot.lookAt(mine.getX() + 0.5, mine.getY() + 0.5, mine.getZ() + 0.5);
        breakBlockAt(mine);
    }

    public void aiUseItem() {
        useItemMainHand();
    }

    public void aiDropItem(boolean all) {
        bot.drop(all);
    }

    public void aiSelectSlot(int slot) {
        if (slot < 0 || slot > 8) {
            return;
        }
        setSelectedSlot(slot);
    }

    public void aiStop() {
        aiQueue.clear();
        aiQueueActive = false;
        setMovementInput(0.0F, 0.0F);
        bot.setJumping(false);
        setSneaking(false);
    }

    /**
     * Runs any command on behalf of the bot (as if the bot typed the command).
     * For the "help" command, return the list of available commands so the AI
     * can learn which commands can be run on this server.
     */
    public String aiRunCommand(String command) {
        if (command == null || command.trim().isEmpty()) {
            return "Empty command";
        }
        String cmd = command.trim();
        if (cmd.startsWith("/")) {
            cmd = cmd.substring(1);
        }
        // Detect /help: give the AI the list of available commands (not raw client output)
        String lower = cmd.toLowerCase(Locale.ROOT);
        if (lower.equals("help") || lower.startsWith("help ")) {
            StringBuilder sb = new StringBuilder("Available commands: ");
            for (String name : Bukkit.getCommandMap().getKnownCommands().keySet()) {
                if (name != null && !name.isEmpty() && !name.contains(":")) {
                    sb.append('/').append(name).append(' ');
                }
            }
            return sb.toString();
        }
        try {
            boolean ok = bot.getBukkitPlayer().performCommand(cmd);
            return ok ? "Command executed: /" + cmd : "Failed to execute command: /" + cmd;
        } catch (Exception e) {
            return "Error executing command /" + cmd + ": "
                    + (e.getMessage() != null ? e.getMessage() : e.toString());
        }
    }

    protected void setMovementInput(float forward, float strafe) {
        bot.setMovementInput(forward, strafe, bot.isJumpingRequested());
    }

    protected void setSneaking(boolean sneak) {
        bot.setSneak(sneak);
    }

    protected void tickAiActions() {
        AIAction action = aiQueue.get(0);
        switch (action.type) {
            case MOVE:
                aiLastForward = action.forward;
                aiLastStrafe = action.strafe;
                setMovementInput(action.forward, action.strafe);
                break;
            case JUMP:
                bot.setJumping(true);
                break;
            case SNEAK:
                setSneaking(action.sneak);
                break;
            case ATTACK:
                attackIfReady();
                break;
        }
        action.remaining--;
        if (action.remaining <= 0) {
            if (action.type == AiActionType.JUMP) {
                bot.setJumping(false);
            } else if (action.type == AiActionType.SNEAK) {
                setSneaking(false);
            }
            aiQueue.remove(0);
        }
        if (aiQueue.isEmpty()) {
            aiQueueActive = false;
            setMovementInput(0.0F, 0.0F);
        }
    }

    public void setOwnerUuid(UUID ownerUuid) {
        this.ownerUuid = ownerUuid;
    }

    protected int targetRadius() {
        return ModConfig.instance.baseTargetRadius;
    }

    public void tick() {
        if (!bot.isAlive()) {
            return;
        }
        if (ModConfig.instance.useItemEnabled) {
            tryEat();
            usePotionIfLow();
            tryUseMilk();
        }
        tickBowRelease();
        if (ModConfig.instance.interactiveEnabled) {
            tickChat();
        }
        if (!aiQueue.isEmpty()) {
            tickAiActions();
            return;
        }
        switch (state) {
            case PVP:
                combatTick();
                break;
            case FOLLOW:
                tickFollow();
                break;
            case WANDER:
                tickWander();
                tickMine();
                break;
            case CHILL:
                setMovementInput(0.0F, 0.0F);
                break;
            case EAT:
                tickEat();
                break;
        }
    }

    protected void combatTick() {
        if (ModConfig.instance.useItemEnabled) {
            tryUseItemOnTarget();
        }
        if (target == null || !target.isAlive()) {
            target = findTarget();
            if (target == null) {
                setMovementInput(0.0F, 0.0F);
                return;
            }
        }
        Vec3 targetPos = target.position();
        bot.lookAt(targetPos.x, targetPos.y, targetPos.z);
        double distanceSq = bot.distanceToSqr(target);
        if (distanceSq > 9.0) {
            setMovement(1.0F, 0.0F);
        } else {
            setMovement(0.0F, 0.0F);
            attackIfReady();
        }
        if (ModConfig.instance.multiWeaponEnabled) {
            manageWeapon();
        }
    }

    protected Entity findTarget() {
        int radius = targetRadius();
        AABB box = bot.getBoundingBoxInflated(radius);
        List<ServerPlayer> players = bot.level().getEntitiesOfClass(ServerPlayer.class, box,
                p -> p != bot && p.isAlive());
        Entity nearest = null;
        double best = (double) radius * radius;
        for (ServerPlayer player : players) {
            double d = bot.distanceToSqr(player);
            if (d < best) {
                best = d;
                nearest = player;
            }
        }
        return nearest;
    }

    protected void tickFollow() {
        ServerPlayer owner = getOwner();
        if (owner == null || !owner.isAlive()) {
            setMovementInput(0.0F, 0.0F);
            return;
        }
        double distanceSq = bot.distanceToSqr(owner);
        if (distanceSq > 9.0) {
            bot.lookAt(owner.getX(), owner.getY(), owner.getZ());
            setMovement(1.0F, 0.0F);
        } else {
            setMovement(0.0F, 0.0F);
        }
    }

    protected ServerPlayer getOwner() {
        if (ownerUuid == null) {
            return null;
        }
        return bot.getServer().getPlayerList().getPlayer(ownerUuid);
    }

    protected void tickEat() {
        if (bot.getFoodData().getFoodLevel() >= 15) {
            state = BotState.FOLLOW;
            return;
        }
        tryEat();
        setMovementInput(0.0F, 0.0F);
    }

    protected void attackIfReady() {
        if (attackCooldown > 0) {
            attackCooldown--;
            return;
        }
        boolean tap = pendingTapAttack;
        pendingTapAttack = false;
        if (tap) {
            if (target != null) {
                bot.attack(target);
            }
            attackCooldown = 8;
        }
    }

    protected void setMovement(float forward, float strafe) {
        setMovementInput(forward, strafe);
        if (forward == 0.0F && strafe == 0.0F) {
            lastMoveDirection = 0;
            return;
        }
        int dir;
        if (forward > 0.0F) {
            dir = 1;
        } else if (forward < 0.0F) {
            dir = -1;
        } else if (strafe > 0.0F) {
            dir = 2;
        } else {
            dir = -2;
        }
        if (dir != lastMoveDirection && ModConfig.instance.interactiveEnabled && isTapEnabled(dir)) {
            pendingTapAttack = true;
        }
        lastMoveDirection = dir;
    }

    protected boolean isTapEnabled(int dir) {
        ModConfig config = ModConfig.instance;
        switch (dir) {
            case 1:
                return config.tapWEnabled;
            case -1:
                return config.tapSEnabled;
            case 2:
                return config.tapDEnabled;
            case -2:
                return config.tapAEnabled;
            default:
                return false;
        }
    }

    protected void manageWeapon() {
        if (bot.isUsingItem()) {
            return;
        }
        int best = selectBestWeaponSlot();
        if (best >= 0 && best != getSelectedSlot()) {
            setSelectedSlot(best);
        }
    }

    protected int selectBestWeaponSlot() {
        double distance = target != null ? Math.sqrt(bot.distanceToSqr(target)) : 8.0;
        boolean hurt = bot.getHurtTicks() > 0;
        int bestSlot = -1;
        int bestScore = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = bot.getInventory().getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            int score = weaponScore(stack, distance, hurt);
            if (score > bestScore) {
                bestScore = score;
                bestSlot = i;
            }
        }
        return bestSlot;
    }

    protected int weaponScore(ItemStack stack, double distance, boolean hurt) {
        if (hurt && stack.getItem() == Items.SHIELD) {
            return 100;
        }
        if (hurt && distance > 3.0 && (stack.getItem() == Items.BOW || stack.getItem() == Items.CROSSBOW)) {
            return 95;
        }
        if (distance > 8.0) {
            if (stack.getItem() == Items.BOW || stack.getItem() == Items.CROSSBOW) {
                return 90;
            }
            return 10;
        }
        if (distance < 3.0) {
            if (isSword(stack)) {
                return 80;
            }
            if (isAxe(stack)) {
                return 60;
            }
            return 10;
        }
        if (isSword(stack)) {
            return 70;
        }
        if (isAxe(stack)) {
            return 55;
        }
        if (stack.getItem() == Items.BOW || stack.getItem() == Items.CROSSBOW) {
            return 50;
        }
        return 5;
    }

    protected boolean isSword(ItemStack stack) {
        return stack.getItem() == Items.NETHERITE_SWORD || stack.getItem() == Items.DIAMOND_SWORD
                || stack.getItem() == Items.GOLDEN_SWORD || stack.getItem() == Items.IRON_SWORD
                || stack.getItem() == Items.STONE_SWORD || stack.getItem() == Items.WOODEN_SWORD;
    }

    protected boolean isAxe(ItemStack stack) {
        return stack.getItem() == Items.NETHERITE_AXE || stack.getItem() == Items.DIAMOND_AXE
                || stack.getItem() == Items.GOLDEN_AXE || stack.getItem() == Items.IRON_AXE
                || stack.getItem() == Items.STONE_AXE || stack.getItem() == Items.WOODEN_AXE;
    }

    protected void tryEat() {
        if (eatCooldown > 0) {
            eatCooldown--;
            return;
        }
        if (bot.isUsingItem()) {
            return;
        }
        FoodData foodData = bot.getFoodData();
        if (foodData.getFoodLevel() >= 15) {
            return;
        }
        int slot = findBestFoodSlot();
        if (slot < 0) {
            return;
        }
        setSelectedSlot(slot);
        bot.startUsingItem(InteractionHand.MAIN_HAND);
        eatCooldown = 200;
    }

    protected void usePotionIfLow() {
        if (bot.isUsingItem()) {
            return;
        }
        if (bot.getHealth() >= bot.getMaxHealth() * 0.5F) {
            return;
        }
        int slot = findSlotWithPotion();
        if (slot < 0) {
            return;
        }
        setSelectedSlot(slot);
        bot.startUsingItem(InteractionHand.MAIN_HAND);
    }

    protected void tryUseMilk() {
        if (bot.isUsingItem()) {
            return;
        }
        boolean hasBadEffect = bot.getActiveEffects().stream()
                .anyMatch(effect -> effect.getEffect() != null && isHarmful(effect));
        if (!hasBadEffect) {
            return;
        }
        for (int i = 0; i < 9; i++) {
            if (bot.getInventory().getItem(i).getItem() == Items.MILK_BUCKET) {
                setSelectedSlot(i);
                useItemMainHand();
                return;
            }
        }
    }

    protected boolean isHarmful(MobEffectInstance effect) {
        try {
            MobEffect mobEffect = effect.getEffect().value();
            return mobEffect.getCategory() != MobEffectCategory.BENEFICIAL && mobEffect.getColor() != 0;
        } catch (Exception e) {
            return true;
        }
    }

    protected void tryUseItemOnTarget() {
        if (target == null || !target.isAlive()) {
            return;
        }
        if (bot.isUsingItem()) {
            return;
        }
        double distance = Math.sqrt(bot.distanceToSqr(target));
        ItemStack main = bot.getItemInMainHand();
        if (main.getItem() == Items.BOW && distance > 5.0 && distance <= 30.0 && hasArrows()) {
            bot.startUsingItem(InteractionHand.MAIN_HAND);
            return;
        }
        if (throwCooldown > 0) {
            throwCooldown--;
            return;
        }
        int slot = findThrowableSlot();
        if (slot >= 0 && distance > 4.0 && distance <= 14.0) {
            setSelectedSlot(slot);
            useItemMainHand();
            throwCooldown = 30;
        }
    }

    protected void tickBowRelease() {
        if (!bot.isUsingItem()) {
            return;
        }
        if (bot.getUseItem().getItem() == Items.BOW) {
            bowChargeTicks++;
            if (bowChargeTicks >= 18) {
                bot.releaseUsingItem();
                bowChargeTicks = 0;
            }
        }
    }

    protected boolean hasArrows() {
        for (int i = 0; i < bot.getInventory().getContainerSize(); i++) {
            if (bot.getInventory().getItem(i).getItem() == Items.ARROW) {
                return true;
            }
        }
        return false;
    }

    protected int findThrowableSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = bot.getInventory().getItem(i);
            if (!stack.isEmpty() && isThrowable(stack)) {
                return i;
            }
        }
        for (int i = 9; i < 36; i++) {
            ItemStack stack = bot.getInventory().getItem(i);
            if (!stack.isEmpty() && isThrowable(stack)) {
                return i;
            }
        }
        return -1;
    }

    protected boolean isThrowable(ItemStack stack) {
        return stack.getItem() == Items.SNOWBALL || stack.getItem() == Items.EGG
                || stack.getItem() == Items.SPLASH_POTION || stack.getItem() == Items.LINGERING_POTION;
    }

    protected int findBestFoodSlot() {
        int bestSlot = -1;
        int bestScore = -1;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = bot.getInventory().getItem(i);
            if (stack.isEmpty() || stack.get(DataComponents.FOOD) == null
                    || stack.getItem() == Items.POTION
                    || stack.getItem() == Items.SPLASH_POTION || stack.getItem() == Items.LINGERING_POTION) {
                continue;
            }
            int score = foodScore(stack);
            if (score > bestScore) {
                bestScore = score;
                bestSlot = i;
            }
        }
        return bestSlot;
    }

    protected int foodScore(ItemStack stack) {
        if (stack.getItem() == Items.COOKED_BEEF) {
            return 5;
        }
        if (stack.getItem() == Items.COOKED_CHICKEN || stack.getItem() == Items.COOKED_PORKCHOP) {
            return 4;
        }
        if (stack.getItem() == Items.COOKED_MUTTON || stack.getItem() == Items.COOKED_RABBIT) {
            return 3;
        }
        if (stack.getItem() == Items.COOKED_COD || stack.getItem() == Items.COOKED_SALMON) {
            return 2;
        }
        return 1;
    }

    protected int findSlotWithPotion() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = bot.getInventory().getItem(i);
            if (!stack.isEmpty() && (stack.getItem() == Items.POTION || stack.getItem() == Items.SPLASH_POTION
                    || stack.getItem() == Items.LINGERING_POTION)) {
                return i;
            }
        }
        return -1;
    }

    protected void tickChat() {
        if (pendingReply != null) {
            broadcastBot(pendingReply);
            pendingReply = null;
            chatCooldown = random.nextInt(600) + 600;
            return;
        }
        if (chatCooldown > 0) {
            chatCooldown--;
            return;
        }
        chatCooldown = random.nextInt(600) + 600;
        broadcastBot(CHAT_MESSAGES[random.nextInt(CHAT_MESSAGES.length)]);
    }

    protected void broadcastBot(String message) {
        PlayerList list = bot.getServer().getPlayerList();
        list.broadcastSystemMessage(Component.literal("<" + getBotName() + "> " + message), false);
    }

    protected void tickWander() {
        if (target != null) {
            return;
        }
        if (wanderTarget == null || bot.distanceToSqr(wanderTarget.x, wanderTarget.y, wanderTarget.z) < 1.0) {
            if (wanderCooldown > 0) {
                wanderCooldown--;
                setMovementInput(0.0F, 0.0F);
                return;
            }
            wanderCooldown = random.nextInt(100) + 40;
            pickWanderTarget();
            return;
        }
        if (hazardAhead()) {
            pickWanderTarget();
            return;
        }
        bot.lookAt(wanderTarget.x, wanderTarget.y, wanderTarget.z);
        setMovement(1.0F, 0.0F);
    }

    protected void pickWanderTarget() {
        ServerLevel level = bot.level();
        BlockPos pos = bot.blockPosition();
        int radius = ModConfig.instance.wanderRadius;
        for (int i = 0; i < 8; i++) {
            int dx = (int) ((random.nextDouble() * 2 - 1) * radius);
            int dz = (int) ((random.nextDouble() * 2 - 1) * radius);
            if (dx == 0 && dz == 0) {
                continue;
            }
            BlockPos candidate = new BlockPos(pos.getX() + dx, pos.getY(), pos.getZ() + dz);
            if (safeSpot(level, candidate)) {
                wanderTarget = new Vec3(candidate.getX() + 0.5, candidate.getY(), candidate.getZ() + 0.5);
                return;
            }
        }
        wanderTarget = null;
    }

    protected boolean safeSpot(ServerLevel level, BlockPos pos) {
        BlockStatePos below = stateAt(level, pos.below());
        if (below.isLavaFire) {
            return false;
        }
        if (stateAt(level, pos).isSolid) {
            return false;
        }
        if (stateAt(level, pos.above()).isSolid) {
            return false;
        }
        return true;
    }

    protected boolean hazardAhead() {
        BlockStatePos below = stateAt(bot.level(), bot.blockPosition().below());
        return below.isLavaFire;
    }

    protected BlockStatePos stateAt(ServerLevel level, BlockPos pos) {
        Material material = level.getBlockState(pos).getBukkitMaterial();
        return new BlockStatePos(material.isSolid(), material == Material.LAVA || material == Material.FIRE);
    }

    protected void tickMine() {
        if (mineCooldown > 0) {
            mineCooldown--;
            return;
        }
        BlockPos mine = findMineableBlock();
        if (mine == null) {
            mineCooldown = 20;
            return;
        }
        bot.lookAt(mine.getX() + 0.5, mine.getY() + 0.5, mine.getZ() + 0.5);
        breakBlockAt(mine);
        mineCooldown = 30;
    }

    protected BlockPos findMineableBlock() {
        ServerLevel level = bot.level();
        BlockPos pos = bot.blockPosition();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    BlockPos p = pos.offset(dx, dy, dz);
                    if (p.equals(pos.below())) {
                        continue;
                    }
                    BlockState state = level.getBlockState(p);
                    if (state.isAir()) {
                        continue;
                    }
                    Material material = state.getBukkitMaterial();
                    if (material == Material.LAVA || material == Material.WATER || material == Material.FIRE) {
                        continue;
                    }
                    if (state.getDestroySpeed(level, p) < 0.0F) {
                        continue;
                    }
                    return p;
                }
            }
        }
        return null;
    }

    public void tickControlled(ServerPlayer controller) {
        bot.lookRotation(controller.getYRot(), controller.getXRot());
        Vec3 pos = controller.position();
        if (controllerLastPos != null) {
            double dx = pos.x - controllerLastPos.x;
            double dz = pos.z - controllerLastPos.z;
            float yawRad = (float) Math.toRadians(controller.getYRot());
            double sin = Math.sin(yawRad);
            double cos = Math.cos(yawRad);
            double fwd = -dx * sin + dz * cos;
            double right = dx * cos + dz * sin;
            float forward = clampMovement((float) (fwd * 6.0F));
            float strafe = clampMovement((float) (-right * 6.0F));
            setMovement(forward, strafe);
        }
        controllerLastPos = pos;
        float scale = controller.getAttackStrengthScale(1.0F);
        if (controllerPrevAttackScale >= 0.0F && scale < controllerPrevAttackScale - 0.2F) {
            if (target != null) {
                bot.attack(target);
            }
        }
        controllerPrevAttackScale = scale;
        if (ModConfig.instance.multiWeaponEnabled) {
            manageWeapon();
        }
    }

    protected float clampMovement(float value) {
        return Math.max(-1.0F, Math.min(1.0F, value));
    }

    public void handleChatCommand(String command) {
        switch (command) {
            case "follow":
                state = BotState.FOLLOW;
                pendingReply = "Okay, I'm following you!";
                break;
            case "stop":
                state = BotState.CHILL;
                pendingReply = "Okay, I'll stay still.";
                break;
            case "pvp":
            case "fight":
                state = BotState.PVP;
                pendingReply = "Okay, I'm ready to fight!";
                break;
            case "chill":
                state = BotState.CHILL;
                pendingReply = "Okay, I'll chill for now.";
                break;
            case "wander":
                state = BotState.WANDER;
                pendingReply = "Okay, I'll take a walk.";
                break;
            case "eat":
                state = BotState.EAT;
                pendingReply = "Okay, I'll eat first.";
                break;
            case "menu":
            default:
                pendingReply = "My menu: follow, wander, pvp, chill, eat";
                break;
        }
        if (ModConfig.instance.debugLogging) {
            CarpetPlayersPlugin.log("Bot " + getBotName() + " state set to " + state + " via '" + command + "'");
        }
    }

    public void onAttacked(Entity attacker) {
        if (attacker instanceof ServerPlayer) {
            setTarget(attacker);
            state = BotState.PVP;
        }
    }

    protected void setTarget(Entity entity) {
        this.target = entity;
        this.pendingTapAttack = true;
    }

    protected void clearTarget() {
        this.target = null;
        this.pendingTapAttack = false;
        this.attackCooldown = 0;
    }

    // ============ NMS access helpers (Mojang-mapped 1.21.11) ============

    /**
     * Access ServerPlayerGameMode (replacement for the old PlayerInteractManager).
     * Public final field on ServerPlayer; CraftPlayer.getHandle() -> ServerPlayer.
     */
    protected ServerPlayerGameMode gameMode() {
        return bot.getBukkitPlayer().getHandle().gameMode;
    }

    /**
     * Replacement for the old playerInteractManager.a(...) / useItem: use the item in the main hand.
     */
    protected void useItemMainHand() {
        gameMode().useItem(bot, bot.level(), bot.getItemInMainHand(), InteractionHand.MAIN_HAND);
    }

    /**
     * Replacement for the old playerInteractManager.breakBlock(BlockPos) — mines a block as a survival player.
     */
    protected void breakBlockAt(BlockPos pos) {
        gameMode().destroyBlock(pos);
    }

    /**
     * Replacement for the old Inventory.itemInHandIndex field — active hotbar slot (setter).
     */
    protected void setSelectedSlot(int slot) {
        bot.getInventory().setSelectedSlot(slot);
    }

    /**
     * Replacement for the old Inventory.itemInHandIndex field — read the active hotbar slot.
     */
    protected int getSelectedSlot() {
        return bot.getInventory().getSelectedSlot();
    }

    protected static class BlockStatePos {
        final boolean isSolid;
        final boolean isLavaFire;

        BlockStatePos(boolean isSolid, boolean isLavaFire) {
            this.isSolid = isSolid;
            this.isLavaFire = isLavaFire;
        }
    }
}
