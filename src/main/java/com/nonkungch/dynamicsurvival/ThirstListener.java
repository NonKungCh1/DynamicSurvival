package com.nonkungch.dynamicsurvival;

import org.bukkit.Material;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;
import com.nonkungch.dynamicsurvival.managers.ThirstManager;

public class ThirstListener implements Listener {

    private final DynamicSurvival plugin;

    public ThirstListener(DynamicSurvival plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK)) {
            return;
        }

        // 1. ตรวจสอบว่าเป็นขวดน้ำ (Potion) หรือไม่
        if (item.getType() == Material.POTION && item.getItemMeta() instanceof PotionMeta) {
            PotionMeta meta = (PotionMeta) item.getItemMeta();
            boolean isWater = false;
            
            // Check 1: ขวดน้ำเปล่ามาตรฐาน (Base Potion Data is WATER)
            if (meta.getBasePotionData().getType() == PotionType.WATER && !meta.hasCustomEffects()) {
                isWater = true;
            }
            
            // Check 2: ขวดที่ถูกตั้งชื่อเองเป็น "Water Bottle" หรือมีคำว่า "น้ำ"
            if (meta.hasDisplayName()) {
                String cleanName = ChatColor.stripColor(meta.getDisplayName());
                if (cleanName.equalsIgnoreCase("Water Bottle") || cleanName.contains("น้ำ")) {
                    isWater = true;
                }
            }
            
            if (isWater) {
                ThirstManager thirstManager = plugin.getThirstManager();
                int restoreAmount = plugin.getConfigManager().getWaterBottleRestore();
                
                // *** เพิ่มค่าความกระหายโดยใช้ Manager ***
                thirstManager.addThirst(player, restoreAmount);
                
                // *** Consumption Logic: เปลี่ยนขวดน้ำเป็นขวดเปล่า ***
                
                ItemStack emptyBottle = new ItemStack(Material.GLASS_BOTTLE);
                
                if (item.getAmount() > 1) {
                    item.setAmount(item.getAmount() - 1);
                    player.getInventory().addItem(emptyBottle);
                } else {
                    // ถ้าเหลือขวดเดียว ให้เปลี่ยนเป็นขวดเปล่าในมือทันที
                    PlayerInventory inv = player.getInventory();
                    // ตรวจสอบทั้ง MainHand และ OffHand 
                    if (inv.getItemInMainHand().equals(item)) {
                        inv.setItemInMainHand(emptyBottle);
                    } else if (inv.getItemInOffHand().equals(item)) {
                        inv.setItemInOffHand(emptyBottle);
                    }
                }
                
                player.sendMessage("§b[Thirst] ดื่มน้ำ! หลอดน้ำเพิ่มขึ้น " + restoreAmount + " หน่วย.");
                event.setCancelled(true);
            }
        }
    }
}
