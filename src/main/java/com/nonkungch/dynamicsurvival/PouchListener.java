package com.nonkungch.dynamicsurvival;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class PouchListener implements Listener {

    private final DynamicSurvival plugin;
    private final PouchManager pouchManager;

    public PouchListener(DynamicSurvival plugin, PouchManager pouchManager) {
        this.plugin = plugin;
        this.pouchManager = pouchManager;
    }

    @EventHandler
    public void onPouchInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || !pouchManager.isPouch(item)) return;

        Action action = event.getAction();
        
        // --- การดื่มน้ำ ---
        if (action == Action.RIGHT_CLICK_AIR) {
            event.setCancelled(true);
            ItemMeta meta = item.getItemMeta();
            int current = meta.getPersistentDataContainer().getOrDefault(pouchManager.CURRENT_WATER_KEY, 0);

            if (current > 0) {
                // ลดน้ำในกระเป๋า
                meta.getPersistentDataContainer().set(pouchManager.CURRENT_WATER_KEY, PersistentDataType.INTEGER, current - 1);
                item.setItemMeta(meta);
                pouchManager.updatePouchLore(item);

                // เพิ่มค่าความกระหาย
                DynamicSurvival.PlayerStats stats = plugin.getPlayerStats(player);
                stats.addThirst(plugin.getConfigManager().getPouchRestoreAmount(), plugin.getConfigManager().getMaxThirst());
                
                player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_DRINK, 1.0f, 1.0f);
                player.sendMessage("§b[DynamicSurvival] คุณดื่มน้ำจากกระเป๋า");
            } else {
                player.sendMessage("§c[DynamicSurvival] กระเป๋าน้ำของคุณว่างเปล่า!");
            }
        }
        
        // --- การเติมน้ำ ---
        if (action == Action.RIGHT_CLICK_BLOCK) {
            Block clickedBlock = event.getClickedBlock();
            if (clickedBlock == null) return;

            boolean isWaterSource = clickedBlock.getType() == Material.WATER || 
                                    (clickedBlock.getType() == Material.CAULDRON && ((Levelled) clickedBlock.getBlockData()).getLevel() > 0);

            if (isWaterSource) {
                event.setCancelled(true);
                ItemMeta meta = item.getItemMeta();
                int current = meta.getPersistentDataContainer().getOrDefault(pouchManager.CURRENT_WATER_KEY, 0);
                int max = meta.getPersistentDataContainer().getOrDefault(pouchManager.MAX_WATER_KEY, 0);

                if (current < max) {
                    meta.getPersistentDataContainer().set(pouchManager.CURRENT_WATER_KEY, PersistentDataType.INTEGER, max); // เติมเต็มถัง
                    item.setItemMeta(meta);
                    pouchManager.updatePouchLore(item);

                    player.playSound(player.getLocation(), Sound.ITEM_BUCKET_FILL, 1.0f, 1.0f);
                    player.sendMessage("§b[DynamicSurvival] คุณเติมน้ำใส่กระเป๋าจนเต็มแล้ว");
                } else {
                    player.sendMessage("§e[DynamicSurvival] กระเป๋าน้ำของคุณเต็มแล้ว");
                }
            }
        }
    }
}
