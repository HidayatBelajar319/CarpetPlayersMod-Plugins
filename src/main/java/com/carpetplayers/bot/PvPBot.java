package com.carpetplayers.bot;

import carpet.patches.EntityPlayerMPFake;
import com.carpetplayers.config.ModConfig;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * Enhanced PvP bot with advanced movement: strafing, sprint-reset,
 * gap management, and jump combat.
 *
 * Movement modes (set via /carpetplayers pvp movement <mode>):
 * - aggressive: rush target, sprint always, close gap fast
 * - defensive: maintain distance, strafe more, retreat when hurt
 * - balanced (default): mix of approach and strafe, sprint-reset on attack
 */
public class PvPBot extends BotBrain {

    /** Ticks between direction changes when proactively strafing in range */
    private int strafeChangeCooldown;
    /** Ticks until next jump in combat */
    private int jumpCooldown;
    /** Ticks sprint has been stopped for sprint-reset */
    private int sprintResetTicks;
    /** Whether sprint was active before sprint-reset */
    private boolean wasSprinting;

    public PvPBot(EntityPlayerMPFake bot) {
        super(bot);
        this.state = BotState.PVP;
        this.strafeChangeCooldown = 0;
        this.jumpCooldown = 0;
        this.sprintResetTicks = 0;
        this.wasSprinting = false;
    }

    @Override
    protected int targetRadius() {
        return ModConfig.instance.pvpTargetRadius;
    }

    @Override
    protected void combatTick() {
        ModConfig cfg = ModConfig.instance;

        // 1. Use potion if low
        if (cfg.useItemEnabled) {
            usePotionIfLow();
        }

        // 2. Find / validate target
        if (target == null || !target.isAlive()) {
            target = findTarget();
            if (target == null) {
                actions().setForward(0.0F);
                actions().setStrafing(0.0F);
                bot.setSprinting(false);
                return;
            }
        }

        // 3. Aim at target eye level
        Vec3 targetPos = target.getEyePosition(1.0F);
        actions().lookAt(targetPos);

        // 4. Distance & health
        double distanceSq = bot.distanceToSqr(target);
        double distance = Math.sqrt(distanceSq);
        float healthPct = bot.getHealth() / bot.getMaxHealth();

        // 5. Strafing — proactive direction changes when in close range
        if (cfg.pvpStrafeEnabled && distance < 5.0) {
            strafeChangeCooldown--;
            if (strafeChangeCooldown <= 0) {
                // Change strafe direction every 10-20 ticks
                lastStrafeDirection = random.nextBoolean() ? 1 : -1;
                strafeChangeCooldown = 10 + random.nextInt(11);
            }
            strafeTicks = 2; // keep strafing active
        }

        // Reactive strafe when hit
        if (bot.hurtTime > 0 && random.nextInt(4) == 0) {
            lastStrafeDirection = random.nextBoolean() ? 1 : -1;
            strafeTicks = 10 + random.nextInt(6);
        }

        // 6. Movement based on mode
        float forward = 0.0F;
        float strafe = 0.0F;
        boolean sprint = false;

        String mode = cfg.pvpMovementMode;
        if (mode == null) mode = "balanced";

        // === AGGRESSIVE ===
        if ("aggressive".equals(mode)) {
            if (distance > 3.5) {
                // Rush target
                forward = 1.0F;
                sprint = distance > 5.0;
                if (cfg.pvpStrafeEnabled && strafeTicks > 0) {
                    strafe = lastStrafeDirection;
                }
            } else {
                // In attack range — strafe while attacking
                forward = 0.0F;
                if (cfg.pvpStrafeEnabled && strafeTicks > 0) {
                    strafe = lastStrafeDirection;
                }
            }
        }
        // === DEFENSIVE ===
        else if ("defensive".equals(mode)) {
            if (distance > 6.0) {
                // Too far — approach cautiously
                forward = 0.5F;
                sprint = false;
                if (cfg.pvpStrafeEnabled && strafeTicks > 0) {
                    strafe = lastStrafeDirection;
                }
            } else if (distance < 2.5 && healthPct > 0.3F) {
                // Too close but healthy — push back slightly
                forward = 0.0F;
                if (cfg.pvpStrafeEnabled && strafeTicks > 0) {
                    strafe = lastStrafeDirection;
                }
            } else if (distance < 2.5 && healthPct <= 0.3F) {
                // Too close and hurt — retreat!
                forward = -0.8F;
                sprint = false;
                if (cfg.pvpStrafeEnabled) {
                    strafe = lastStrafeDirection;
                }
            } else {
                // Good distance — strafe circle
                forward = 0.2F;
                if (cfg.pvpStrafeEnabled && strafeTicks > 0) {
                    strafe = lastStrafeDirection;
                }
            }
        }
        // === BALANCED (default) ===
        else {
            if (distance > 3.5) {
                // Approach
                forward = 1.0F;
                sprint = cfg.pvpSprintResetEnabled && distance > 4.5;

                // Sprint-reset: stop sprint 3 ticks before attack range
                if (cfg.pvpSprintResetEnabled && distance < 4.5 && sprint) {
                    sprint = false;
                }

                if (cfg.pvpStrafeEnabled && strafeTicks > 0) {
                    strafe = lastStrafeDirection;
                }
            } else {
                // In attack range — strafe circle
                forward = 0.0F;
                if (cfg.pvpStrafeEnabled && strafeTicks > 0) {
                    strafe = lastStrafeDirection;
                }
            }
        }

        // 7. Sprint-reset: when about to attack, stop sprint briefly
        if (cfg.pvpSprintResetEnabled && distance <= 3.5 && wasSprinting) {
            sprintResetTicks++;
            sprint = false;
            if (sprintResetTicks >= 3) {
                // Resume sprint after reset
                sprintResetTicks = 0;
                wasSprinting = false;
            }
        } else if (sprint) {
            wasSprinting = true;
            sprintResetTicks = 0;
        } else {
            wasSprinting = false;
            sprintResetTicks = 0;
        }

        // 8. Gap management — retreat when critically low
        if (healthPct < 0.25F && distance < 4.0) {
            forward = -0.8F;
            sprint = false;
            strafe = lastStrafeDirection; // strafe while retreating
        }

        // 9. Apply movement
        actions().setForward(forward);
        actions().setStrafing(strafe);
        bot.setSprinting(sprint);

        // 10. Jump combat — random jump when close to dodge/crit
        if (cfg.pvpStrafeEnabled && distance <= 4.0 && distance >= 2.0) {
            jumpCooldown--;
            if (jumpCooldown <= 0 && random.nextInt(8) == 0) {
                bot.setJumping(true);
                jumpCooldown = 15 + random.nextInt(20);
            }
        }
        if (jumpCooldown > 0 && bot.getDeltaMovement().y == 0.0D) {
            bot.setJumping(false);
        }

        // 11. Attack when in range
        if (distance <= 3.5) {
            // Sprint-reset attack: stop sprint, then attack
            if (cfg.pvpSprintResetEnabled && sprintResetTicks > 0 && sprintResetTicks < 3) {
                // Waiting for sprint-reset — don't attack yet
            } else {
                attackIfReady();
            }
        }

        // 12. Weapon management
        if (cfg.multiWeaponEnabled) {
            manageWeapon();
        }
    }

    public static void equip(EntityPlayerMPFake bot) {
        bot.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
        bot.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.NETHERITE_CHESTPLATE));
        bot.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.NETHERITE_LEGGINGS));
        bot.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.NETHERITE_BOOTS));
        bot.inventory.setItem(0, new ItemStack(Items.NETHERITE_SWORD));
        bot.inventory.setItem(1, new ItemStack(Items.BOW));
        bot.inventory.setItem(2, new ItemStack(Items.GOLDEN_APPLE));
        bot.inventory.setItem(3, new ItemStack(Items.SPLASH_POTION));
        bot.inventory.setItem(4, new ItemStack(Items.ARROW, 64));
        bot.inventory.selected = 0;
    }
}
