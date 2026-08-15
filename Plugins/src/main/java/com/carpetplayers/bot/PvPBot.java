package com.carpetplayers.bot;

import com.carpetplayers.config.ModConfig;
import com.carpetplayers.nms.FakePlayer;
import net.minecraft.server.v1_16_R3.EnumItemSlot;
import net.minecraft.server.v1_16_R3.ItemStack;
import net.minecraft.server.v1_16_R3.Items;

public class PvPBot extends BotBrain {

    public PvPBot(FakePlayer bot) {
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
        if (bot.getHurtTicks() > 0 && random.nextInt(4) == 0) {
            lastStrafeDirection = random.nextBoolean() ? 1 : -1;
            strafeTicks = 10;
        }
        super.combatTick();
        if (strafeTicks > 0) {
            strafeTicks--;
            setMovementInput(0.0F, lastStrafeDirection);
        }
    }

    public static void equip(FakePlayer bot) {
        bot.setItemSlot(EnumItemSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
        bot.setItemSlot(EnumItemSlot.CHEST, new ItemStack(Items.NETHERITE_CHESTPLATE));
        bot.setItemSlot(EnumItemSlot.LEGS, new ItemStack(Items.NETHERITE_LEGGINGS));
        bot.setItemSlot(EnumItemSlot.FEET, new ItemStack(Items.NETHERITE_BOOTS));
        bot.inventory.setItem(0, new ItemStack(Items.NETHERITE_SWORD));
        bot.inventory.setItem(1, new ItemStack(Items.BOW));
        bot.inventory.setItem(2, new ItemStack(Items.GOLDEN_APPLE));
        bot.inventory.setItem(3, new ItemStack(Items.SPLASH_POTION));
        bot.inventory.setItem(4, new ItemStack(Items.ARROW, 64));
        bot.inventory.itemInHandIndex = 0;
    }
}