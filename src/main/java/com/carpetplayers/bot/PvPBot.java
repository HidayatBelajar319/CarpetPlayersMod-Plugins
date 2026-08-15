package com.carpetplayers.bot;

import carpet.patches.EntityPlayerMPFake;
import com.carpetplayers.config.ModConfig;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class PvPBot extends BotBrain {

    public PvPBot(EntityPlayerMPFake bot) {
        super(bot);
        this.state = BotState.PVP;
    }

    @Override
    protected int targetRadius() {
        return ModConfig.instance.pvpTargetRadius;
    }

    @Override
    protected void combatTick() {
        if (ModConfig.instance.useItemEnabled) {
            usePotionIfLow();
        }
        if (bot.hurtTime > 0 && random.nextInt(4) == 0) {
            lastStrafeDirection = random.nextBoolean() ? 1 : -1;
            strafeTicks = 10;
        }
        super.combatTick();
        if (strafeTicks > 0) {
            strafeTicks--;
            actions().setStrafing(lastStrafeDirection);
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
