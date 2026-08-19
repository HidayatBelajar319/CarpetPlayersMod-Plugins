package com.carpetplayers.bot;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.HashMap;
import java.util.Map;

public final class KitManager {

    private KitManager() {
    }

    /**
     * Equips the bot with the selected PvP kit.
     * @return true if the kit is known and was equipped successfully, false otherwise.
     */
    public static boolean applyKit(BotBrain bot, String kitName) {
        if (bot == null || kitName == null) {
            return false;
        }
        switch (kitName) {
            case "netherite_crystal":
                equipKit(bot, true, true, false);
                return true;
            case "diamond_crystal":
                equipKit(bot, false, true, false);
                return true;
            case "netherite_pot":
                equipKit(bot, true, false, true);
                return true;
            case "diamond_pot":
                equipKit(bot, false, false, true);
                return true;
            case "netherite_basic":
                equipKit(bot, true, false, false);
                return true;
            case "diamond_basic":
                equipKit(bot, false, false, false);
                return true;
            default:
                return false;
        }
    }

    private static void equipKit(BotBrain bot, boolean netherite, boolean crystal, boolean pot) {
        bot.getBot().inventory.clearContent();
        applyArmor(bot, netherite);
        applySword(bot, netherite, crystal);
        addItems(bot, Items.TOTEM_OF_UNDYING, 3);
        if (crystal) {
            addItems(bot, Items.END_CRYSTAL, 3);
            addItems(bot, Items.OBSIDIAN, 64);
            addItems(bot, Items.ENDER_PEARL, 16);
            addItems(bot, Items.EXPERIENCE_BOTTLE, 8);
        } else {
            addItems(bot, Items.GOLDEN_APPLE, 8);
            addItems(bot, Items.COOKED_BEEF, 64);
            if (pot) {
                addSplashHealthPotions(bot, 16);
            }
        }
    }

    private static void applyArmor(BotBrain bot, boolean netherite) {
        Map<Enchantment, Integer> enchants = armorEnchants();
        bot.getBot().setItemSlot(EquipmentSlot.HEAD,
                enchanted(new ItemStack(netherite ? Items.NETHERITE_HELMET : Items.DIAMOND_HELMET), enchants));
        bot.getBot().setItemSlot(EquipmentSlot.CHEST,
                enchanted(new ItemStack(netherite ? Items.NETHERITE_CHESTPLATE : Items.DIAMOND_CHESTPLATE), enchants));
        bot.getBot().setItemSlot(EquipmentSlot.LEGS,
                enchanted(new ItemStack(netherite ? Items.NETHERITE_LEGGINGS : Items.DIAMOND_LEGGINGS), enchants));
        bot.getBot().setItemSlot(EquipmentSlot.FEET,
                enchanted(new ItemStack(netherite ? Items.NETHERITE_BOOTS : Items.DIAMOND_BOOTS), enchants));
    }

    private static Map<Enchantment, Integer> armorEnchants() {
        Map<Enchantment, Integer> enchants = new HashMap<>();
        enchants.put(Enchantments.ALL_DAMAGE_PROTECTION, 4);
        enchants.put(Enchantments.UNBREAKING, 3);
        enchants.put(Enchantments.MENDING, 1);
        return enchants;
    }

    private static void applySword(BotBrain bot, boolean netherite, boolean crystal) {
        Map<Enchantment, Integer> enchants = new HashMap<>();
        enchants.put(Enchantments.SHARPNESS, 5);
        enchants.put(Enchantments.UNBREAKING, 3);
        enchants.put(Enchantments.MENDING, 1);
        if (crystal) {
            enchants.put(Enchantments.MOB_LOOTING, 3);
        }
        ItemStack sword = new ItemStack(netherite ? Items.NETHERITE_SWORD : Items.DIAMOND_SWORD);
        bot.getBot().inventory.setItem(0, enchanted(sword, enchants));
        bot.getBot().inventory.selected = 0;
    }

    private static void addSplashHealthPotions(BotBrain bot, int count) {
        for (int i = 0; i < count; i++) {
            ItemStack pot = new ItemStack(Items.SPLASH_POTION, 1);
            PotionUtils.setPotion(pot, Potions.STRONG_HEALING);
            bot.getBot().addItem(pot);
        }
    }

    private static void addItems(BotBrain bot, Item item, int count) {
        if (count <= 0) {
            return;
        }
        int maxStack = item.getMaxStackSize();
        int remaining = count;
        while (remaining > 0) {
            int size = Math.min(remaining, maxStack);
            bot.getBot().addItem(new ItemStack(item, size));
            remaining -= size;
        }
    }

    private static ItemStack enchanted(ItemStack stack, Map<Enchantment, Integer> enchantments) {
        if (enchantments != null && !enchantments.isEmpty()) {
            EnchantmentHelper.setEnchantments(enchantments, stack);
        }
        return stack;
    }
}
