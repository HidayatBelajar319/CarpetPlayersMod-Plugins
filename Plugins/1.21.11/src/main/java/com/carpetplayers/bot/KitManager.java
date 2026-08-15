package com.carpetplayers.bot;

import com.carpetplayers.nms.FakePlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.bukkit.Material;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

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
        fake.getInventory().clearContent();
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
        enchants.put(Enchantment.PROTECTION, 4);
        enchants.put(Enchantment.UNBREAKING, 3);
        enchants.put(Enchantment.MENDING, 1);
        return enchants;
    }

    private static void applySword(BotBrain bot, boolean netherite, boolean crystal) {
        Map<Enchantment, Integer> enchants = new HashMap<>();
        enchants.put(Enchantment.SHARPNESS, 5);
        enchants.put(Enchantment.UNBREAKING, 3);
        enchants.put(Enchantment.MENDING, 1);
        if (crystal) {
            enchants.put(Enchantment.LOOTING, 3);
        }
        ItemStack sword = new ItemStack(netherite ? Items.NETHERITE_SWORD : Items.DIAMOND_SWORD);
        bot.getBot().getInventory().setItem(0, enchanted(sword, enchants));
        bot.getBot().getInventory().setSelectedSlot(0);
    }

    private static void addSplashHealthPotions(BotBrain bot, int count) {
        for (int i = 0; i < count; i++) {
            // Potion via Bukkit API (potion kini DataComponent) — paling tahan versi.
            org.bukkit.inventory.ItemStack potion = new org.bukkit.inventory.ItemStack(Material.SPLASH_POTION, 1);
            PotionMeta meta = (PotionMeta) potion.getItemMeta();
            if (meta != null) {
                meta.setBasePotionType(PotionType.STRONG_HEALING);
                potion.setItemMeta(meta);
            }
            addItemToInventory(bot, CraftItemStack.asNMSCopy(potion));
        }
    }

    private static void addItems(BotBrain bot, Item item, int count) {
        if (count <= 0) {
            return;
        }
        int maxStack = new ItemStack(item).getMaxStackSize();
        int remaining = count;
        while (remaining > 0) {
            int size = Math.min(remaining, maxStack);
            addItemToInventory(bot, new ItemStack(item, size));
            remaining -= size;
        }
    }

    private static void addItemToInventory(BotBrain bot, ItemStack stack) {
        FakePlayer fake = bot.getBot();
        for (int i = 0; i < fake.getInventory().getContainerSize(); i++) {
            ItemStack slot = fake.getInventory().getItem(i);
            if (!slot.isEmpty() && slot.getItem() == stack.getItem()
                    && slot.getCount() + stack.getCount() <= stack.getMaxStackSize()) {
                slot.setCount(slot.getCount() + stack.getCount());
                return;
            }
            if (slot.isEmpty()) {
                fake.getInventory().setItem(i, stack);
                return;
            }
        }
    }

    /**
     * Enchant via Bukkit API (Enchantment kini registry Holder<Enchantment> + DataComponents).
     * Convert NMS -> Bukkit, pasang ItemMeta.addEnchant, lalu kembali ke NMS.
     */
    private static ItemStack enchanted(ItemStack stack, Map<Enchantment, Integer> enchantments) {
        if (enchantments != null && !enchantments.isEmpty()) {
            org.bukkit.inventory.ItemStack bukkit = CraftItemStack.asBukkitCopy(stack);
            ItemMeta meta = bukkit.getItemMeta();
            if (meta != null) {
                for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                    meta.addEnchant(entry.getKey(), entry.getValue(), true);
                }
                bukkit.setItemMeta(meta);
            }
            return CraftItemStack.asNMSCopy(bukkit);
        }
        return stack;
    }
}
