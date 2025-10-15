package com.nonkungch.dynamicsurvival;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;
import com.nonkungch.dynamicsurvival.DynamicSurvival.PlayerStats;

public class ThirstListener implements Listener {

    private final DynamicSurvival plugin;

    public ThirstListener(DynamicSurvival plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            
            if (item != null && item.getType() == Material.POTION && item.hasItemMeta()) {
                if (item.getItemMeta() instanceof PotionMeta) {
                    PotionMeta meta = (PotionMeta) item.getItemMeta();
                    
                    if (meta.getBasePotionData().getType() == PotionType.WATER) {
                        
                        PlayerStats stats = plugin.getPlayerStats(player);
                        int restoreAmount = plugin.getConfigManager().getWaterBottleRestore();
                        int maxThirst = plugin.getConfigManager().getMaxThirst();
                        
                        stats.addThirst(restoreAmount, maxThirst);
                        
                        PlayerInventory inv = player.getInventory();
                        if (item.getAmount() > 1) {
                            item.setAmount(item.getAmount() - 1);
                            inv.addItem(new ItemStack(Material.GLASS_BOTTLE));
                        } else {
                            inv.setItemInMainHand(new ItemStack(Material.GLASS_BOTTLE));
                        }

                        player.sendMessage("§b[DynamicSurvival] คุณได้ดื่มน้ำแล้ว!");
                        event.setCancelled(true);
                    }
                }
            }
        }
    }
}
