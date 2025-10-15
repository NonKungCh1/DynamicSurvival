package com.nonkungch.dynamicsurvival;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class PouchManager {

    private final DynamicSurvival plugin;
    public final NamespacedKey TIER_KEY;
    public final NamespacedKey CURRENT_WATER_KEY;
    public final NamespacedKey MAX_WATER_KEY;

    public PouchManager(DynamicSurvival plugin) {
        this.plugin = plugin;
        this.TIER_KEY = new NamespacedKey(plugin, "pouch_tier");
        this.CURRENT_WATER_KEY = new NamespacedKey(plugin, "pouch_current_water");
        this.MAX_WATER_KEY = new NamespacedKey(plugin, "pouch_max_water");
    }

    public ItemStack createPouch(int tier) {
        int capacity = plugin.getConfigManager().getPouchCapacity(tier);
        
        ItemStack pouch = new ItemStack(Material.POTION); // ใช้ Potion เป็นไอเท็มฐาน
        ItemMeta meta = pouch.getItemMeta();
        
        meta.setDisplayName(ChatColor.AQUA + "กระเป๋าน้ำ [ขั้น " + tier + "]");
        
        // เก็บข้อมูล NBT
        meta.getPersistentDataContainer().set(TIER_KEY, PersistentDataType.INTEGER, tier);
        meta.getPersistentDataContainer().set(CURRENT_WATER_KEY, PersistentDataType.INTEGER, 0); // เริ่มต้นที่น้ำ 0
        meta.getPersistentDataContainer().set(MAX_WATER_KEY, PersistentDataType.INTEGER, capacity);
        
        pouch.setItemMeta(meta);
        
        return updatePouchLore(pouch);
    }

    public ItemStack updatePouchLore(ItemStack pouch) {
        if (pouch == null || !pouch.hasItemMeta()) return pouch;

        ItemMeta meta = pouch.getItemMeta();
        int current = meta.getPersistentDataContainer().getOrDefault(CURRENT_WATER_KEY, 0);
        int max = meta.getPersistentDataContainer().getOrDefault(MAX_WATER_KEY, 0);

        List<String> lore = new ArrayList<>();
        lore.add("§7บรรจุน้ำสำหรับดื่ม");
        lore.add("§fน้ำ: §b" + current + "§f/" + max + " ขวด");
        lore.add("");
        lore.add("§eคลิกขวาที่แหล่งน้ำเพื่อเติม");
        lore.add("§eคลิกขวาในอากาศเพื่อดื่ม");
        
        meta.setLore(lore);
        pouch.setItemMeta(meta);
        return pouch;
    }

    public boolean isPouch(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(TIER_KEY, PersistentDataType.INTEGER);
    }
}
