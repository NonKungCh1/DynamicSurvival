package com.nonkungch.dynamicsurvival;

import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {

    private final DynamicSurvival plugin;
    private FileConfiguration config;

    public ConfigManager(DynamicSurvival plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig(); 
    }

    public void loadConfig() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }
    
    // --- Season Durations ---
    public int getSeasonDuration(Season season) {
        return config.getInt("season-duration." + season.toString().toLowerCase(), 20);
    }
    
    // --- Thirst Settings ---
    public int getMaxThirst() {
        return config.getInt("thirst.max-level", 100);
    }
    public int getBaseThirstLoss() {
        return config.getInt("thirst.base-loss-per-update", 1);
    }
    public int getThirstDangerLevel() {
        return config.getInt("thirst.danger-level", 10);
    }
    public int getWaterBottleRestore() {
        return config.getInt("thirst.water-bottle-restore", 40);
    }
    
    // --- Temperature Settings ---
    public float getBaseTemp(Season season) {
        return (float) config.getDouble("temperature.base-temp." + season.toString().toLowerCase(), 25.0);
    }
    public float getNormalTempThreshold() {
        return (float) config.getDouble("temperature.thresholds.normal", 25.0);
    }
    public float getHotTempThreshold() {
        return (float) config.getDouble("temperature.thresholds.hot", 35.0);
    }
    public float getDeadlyHotTempThreshold() {
        return (float) config.getDouble("temperature.thresholds.deadly-hot", 40.0);
    }
    public float getFreezingTempThreshold() {
        return (float) config.getDouble("temperature.thresholds.freezing", 5.0);
    }
    public float getColdTempThreshold() {
        return (float) config.getDouble("temperature.thresholds.deadly-cold", -5.0);
    }
    
    // --- Weather Settings ---
    public double getWeatherChance() {
        return config.getDouble("weather.chance-to-change", 0.2);
    }
    
    // **NEW: Display Mode Setting**
    /**
     * Gets the preferred display mode for player stats. 
     * Valid values are "ACTION_BAR" or "SCOREBOARD". Defaults to "ACTION_BAR".
     */
    public String getDisplayMode() {
        return config.getString("display.mode", "ACTION_BAR").toUpperCase();
    }
}
