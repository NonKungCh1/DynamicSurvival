package com.nonkungch.dynamicsurvival.listeners;

import com.nonkungch.dynamicsurvival.DynamicSurvival;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

public class PlayerListener implements Listener {
    
    private final DynamicSurvival plugin;
    
    public PlayerListener(DynamicSurvival plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerConsume(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        
        // ตรวจสอบว่าเป็นขวดน้ำ (Water Bottle)
        if (item.getType() == Material.POTION) {
            PotionMeta meta = (PotionMeta) item.getItemMeta();
            
            // ตรวจสอบว่าเป็น Potion ธรรมดาที่ไม่มี Effect (คือขวดน้ำเปล่า)
            if (meta.getBasePotionType() == PotionType.WATER || meta.getCustomEffects().isEmpty()) {
                plugin.getThirstManager().refillThirst(event.getPlayer(), 5); // เติม 5 หน่วย
            }
        }
    }
}
