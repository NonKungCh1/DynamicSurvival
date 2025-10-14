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
    
    // --- New Settings (สำหรับ Weather/Display) ---
    // แก้ไข: เพิ่มเมธอดที่ขาดหายไป
    public double getWeatherChance() {
        return config.getDouble("weather.change-chance", 0.05); // 5% chance per second tick
    }
    // แก้ไข: เพิ่มเมธอดที่ขาดหายไป
    public String getDisplayMode() {
        // Options: ACTION_BAR or SCOREBOARD
        return config.getString("display-mode", "ACTION_BAR"); 
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
    
    // --- Temperature Settings (แก้ไข: เมธอดทั้งหมดถูกเพิ่มแล้ว) ---
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
        return (float) config.getDouble("temperature.thresholds.cold", 0.0);
    }
}
