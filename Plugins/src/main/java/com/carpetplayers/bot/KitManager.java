package com.carpetplayers.bot;

import com.carpetplayers.nms.FakePlayer;
import net.minecraft.server.v1_16_R3.Blocks;
import net.minecraft.server.v1_16_R3.Enchantment;
import net.minecraft.server.v1_16_R3.Enchantments;
import net.minecraft.server.v1_16_R3.EnumItemSlot;
import net.minecraft.server.v1_16_R3.Item;
import net.minecraft.server.v1_16_R3.ItemStack;
import net.minecraft.server.v1_16_R3.Items;
import net.minecraft.server.v1_16_R3.PotionUtil;
import net.minecraft.server.v1_16_R3.Potions;

import java.util.HashMap;
import java.util.Map;

public final class KitManager {

    private KitManager() {
    }

    /**
     * Melengkapi bot dengan kit PvP yang dipilih.
     * @return true jika kit dikenal dan berhasil dipasang, false jika tidak dikenal.
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
        FakePlayer fake = bot.getBot();
        fake.inventory.clear();
        applyArmor(bot, netherite);
        applySword(bot, netherite, crystal);
        addItems(bot, Items.TOTEM_OF_UNDYING, 3);
        if (crystal) {
            addItems(bot, Items.END_CRYSTAL, 3);
            addItems(bot, Item.getItemOf(Blocks.OBSIDIAN), 64);
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
        bot.getBot().setItemSlot(EnumItemSlot.HEAD,
                enchanted(new ItemStack(netherite ? Items.NETHERITE_HELMET : Items.DIAMOND_HELMET), enchants));
        bot.getBot().setItemSlot(EnumItemSlot.CHEST,
                enchanted(new ItemStack(netherite ? Items.NETHERITE_CHESTPLATE : Items.DIAMOND_CHESTPLATE), enchants));
        bot.getBot().setItemSlot(EnumItemSlot.LEGS,
                enchanted(new ItemStack(netherite ? Items.NETHERITE_LEGGINGS : Items.DIAMOND_LEGGINGS), enchants));
        bot.getBot().setItemSlot(EnumItemSlot.FEET,
                enchanted(new ItemStack(netherite ? Items.NETHERITE_BOOTS : Items.DIAMOND_BOOTS), enchants));
    }

    private static Map<Enchantment, Integer> armorEnchants() {
        Map<Enchantment, Integer> enchants = new HashMap<>();
        enchants.put(Enchantments.PROTECTION_ENVIRONMENTAL, 4);
        enchants.put(Enchantments.DURABILITY, 3);
        enchants.put(Enchantments.MENDING, 1);
        return enchants;
    }

    private static void applySword(BotBrain bot, boolean netherite, boolean crystal) {
        Map<Enchantment, Integer> enchants = new HashMap<>();
        enchants.put(Enchantments.DAMAGE_ALL, 5);
        enchants.put(Enchantments.DURABILITY, 3);
        enchants.put(Enchantments.MENDING, 1);
        if (crystal) {
            enchants.put(Enchantments.LOOT_BONUS_MOBS, 3);
        }
        ItemStack sword = new ItemStack(netherite ? Items.NETHERITE_SWORD : Items.DIAMOND_SWORD);
        bot.getBot().inventory.setItem(0, enchanted(sword, enchants));
        bot.getBot().inventory.itemInHandIndex = 0;
    }

    private static void addSplashHealthPotions(BotBrain bot, int count) {
        for (int i = 0; i < count; i++) {
            ItemStack pot = new ItemStack(Items.SPLASH_POTION, 1);
            PotionUtil.addPotionToItemStack(pot, Potions.STRONG_HEALING);
            addItemToInventory(bot, pot);
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
            addItemToInventory(bot, new ItemStack(item, size));
            remaining -= size;
        }
    }

    private static void addItemToInventory(BotBrain bot, ItemStack stack) {
        FakePlayer fake = bot.getBot();
        for (int i = 0; i < fake.inventory.getSize(); i++) {
            ItemStack slot = fake.inventory.getItem(i);
            if (!slot.isEmpty() && slot.getItem() == stack.getItem()
                    && slot.getCount() + stack.getCount() <= stack.getItem().getMaxStackSize()) {
                slot.setCount(slot.getCount() + stack.getCount());
                return;
            }
            if (slot.isEmpty()) {
                fake.inventory.setItem(i, stack);
                return;
            }
        }
    }

    private static ItemStack enchanted(ItemStack stack, Map<Enchantment, Integer> enchantments) {
        if (enchantments != null && !enchantments.isEmpty()) {
            for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                stack.addEnchantment(entry.getKey(), entry.getValue());
            }
        }
        return stack;
    }
}