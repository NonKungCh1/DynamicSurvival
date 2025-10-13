package com.nonkungch.dynamicsurvival.managers;

import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Scoreboard;

import java.util.Map;

public class TemperatureManager {

    private final JavaPlugin plugin;
    private final Scoreboard board;
    private final TimeManager timeManager;
    private static final int MAX_TEMP = 100;

    public TemperatureManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.board = Bukkit.getScoreboardManager().getMainScoreboard();
        this.timeManager = plugin.getTimeManager();
    }

    public void checkAndSetupPlayer(Player player) {
        if (getScore(player, "temp") == 0) {
            int defaultTemp = plugin.getConfig().getInt("default-temp", 50);
            setScore(player, "temp", defaultTemp);
        }
    }

    public void processTemperature(Player player) {
        int currentTemp = getScore(player, "temp");
        int tempChange = 0;
        Biome biome = player.getLocation().getBlock().getBiome();
        int season = timeManager.getCurrentSeason();

        // 1. ปรับตาม Biome
        if (biome.name().contains("DESERT") || biome.name().contains("NETHER")) {
            tempChange += 2;
        } else if (biome.name().contains("FROZEN") || biome.name().contains("SNOWY")) {
            tempChange -= 2;
        }
        
        // 2. ปรับตามฤดูกาล (ดึง Modifier จาก Config)
        String seasonNameKey = timeManager.getSeasonName(season).toUpperCase();
        Map<String, Object> seasonalMods = plugin.getConfig().getConfigurationSection("seasonal-temp-modifier").getValues(false);
        if (seasonalMods.containsKey(seasonNameKey)) {
            tempChange += (int) seasonalMods.get(seasonNameKey);
        }
        
        // 3. ปรับ Scoreboard และจำกัดค่า
        int newTemp = Math.min(MAX_TEMP, Math.max(0, currentTemp + tempChange));
        setScore(player, "temp", newTemp);
        
        // 4. ผลกระทบเมื่ออุณหภูมิเกินขีดจำกัด
        if (newTemp > 90) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 40, 0, false, false));
        } else if (newTemp < 10) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, 1, false, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 40, 0, false, false));
        }
    }
    
    // Helper methods
    public int getScore(Player player, String objectiveName) {
        return board.getObjective(objectiveName).getScore(player.getName()).getScore();
    }

    public void setScore(Player player, String objectiveName, int value) {
        board.getObjective(objectiveName).getScore(player.getName()).setScore(value);
    }
}
