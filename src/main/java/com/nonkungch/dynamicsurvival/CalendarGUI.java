package com.nonkungch.dynamicsurvival;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import java.util.Arrays;
import java.util.List;
import com.nonkungch.dynamicsurvival.DynamicSurvival.PlayerStats;

public class CalendarGUI implements Listener {

    private static final String GUI_TITLE = "§9§lDynamicSurvival ปฏิทิน";
    
    public CalendarGUI(DynamicSurvival plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private static Material getGlassMaterialForSeason(Season season) {
        switch (season) {
            case SPRING: return Material.LIME_STAINED_GLASS_PANE;
            case SUMMER: return Material.RED_STAINED_GLASS_PANE;
            case AUTUMN: return Material.ORANGE_STAINED_GLASS_PANE;
            case WINTER: return Material.LIGHT_BLUE_STAINED_GLASS_PANE;
            default: return Material.GRAY_STAINED_GLASS_PANE;
        }
    }
    
    public static void openCalendar(Player player, DynamicSurvival plugin) {
        Inventory gui = Bukkit.createInventory(player, 27, GUI_TITLE);

        Season currentSeason = plugin.getCurrentSeason();
        ItemStack seasonInfo = createItem(getGlassMaterialForSeason(currentSeason), 
            currentSeason.getChatColor() + currentSeason.getThaiName(),
            Arrays.asList(
                "§7--- ฤดูกาลปัจจุบัน ---",
                "§eวันที่: §f" + plugin.getCurrentDayInSeason() + " / " + plugin.getConfigManager().getSeasonDuration(currentSeason),
                "§eคงเหลือ: §f" + (plugin.getConfigManager().getSeasonDuration(currentSeason) - plugin.getCurrentDayInSeason()) + " วัน",
                " ",
                "§7อุณหภูมิพื้นฐาน: §f" + plugin.getConfigManager().getBaseTemp(currentSeason) + "°C"
            ));
        gui.setItem(13, seasonInfo);

        PlayerStats stats = plugin.getPlayerStats(player);
        ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) playerHead.getItemMeta();
        meta.setOwningPlayer(player);
        meta.setDisplayName("§a§lข้อมูลผู้เล่น");
        meta.setLore(Arrays.asList(
            "§7--- สถิติปัจจุบัน ---",
            "§eอุณหภูมิ: §f" + String.format("%.1f°C", stats.getTemperature()),
            "§eระดับน้ำ: §f" + stats.getThirst() + "/" + plugin.getConfigManager().getMaxThirst()
        ));
        playerHead.setItemMeta(meta);
        gui.setItem(4, playerHead);

        int slot = 18;
        for (Season season : Season.values()) {
            if (season != currentSeason) {
                gui.setItem(slot++, createItem(getGlassMaterialForSeason(season), 
                    season.getChatColor() + season.getThaiName(),
                    Arrays.asList(
                        "§7--- ข้อมูล ---",
                        "§eระยะเวลา: §f" + plugin.getConfigManager().getSeasonDuration(season) + " วัน",
                        "§eอุณหภูมิพื้นฐาน: §f" + plugin.getConfigManager().getBaseTemp(season) + "°C"
                    )));
            }
        }
        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(GUI_TITLE)) {
            event.setCancelled(true);
        }
    }

    private static ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
