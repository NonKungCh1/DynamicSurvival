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
    
    // --- System Settings ---
    public int getUpdateFrequencyTicks() {
        return config.getInt("system.update-frequency-ticks", 60);
    }
    public String getTrackedWorldName() {
        return config.getString("system.tracked-world-name", "world");
    }
    public String getDisplayMode() {
        return config.getString("system.display-mode", "ACTION_BAR");
    }

    // --- Season Durations ---
    public int getSeasonDuration(Season season) {
        return config.getInt("season-duration." + season.toString().toLowerCase(), 20);
    }
    
    // --- Thirst Settings ---
    public int getMaxThirst() {
        return config.getInt("thirst.max-level", 100);
    }
    public double getBaseThirstLoss() { 
        return config.getDouble("thirst.base-loss-per-update", 1.0);
    }
    public int getThirstDangerLevel() {
        return config.getInt("thirst.danger-level", 10);
    }
    public int getWaterBottleRestore() {
        return config.getInt("thirst.water-bottle-restore", 40);
    }
    public double getSprintMultiplier() {
        return config.getDouble("thirst.sprint-multiplier", 2.0);
    }
    public double getActionLossPerBlock() {
        return config.getDouble("thirst.action-loss-per-block", 0.5);
    }
    
    // --- Temperature Settings ---
    public float getBaseTemp(Season season) {
        return (float) config.getDouble("temperature.base-temp." + season.toString().toLowerCase(), 25.0);
    }
    public float getFreezingThreshold() {
        return (float) config.getDouble("temperature.effects.freezing-threshold", 5.0);
    }
    public float getHotThreshold() {
        return (float) config.getDouble("temperature.effects.hot-threshold", 40.0);
    }
    public double getDamageAmount() {
        return config.getDouble("temperature.effects.damage-amount", 1.0);
    }
    
    // --- Weather Settings ---
    public double getTempModifierOnRain() {
        return config.getDouble("weather.temp-modifier-on-rain", -5.0);
    }
    // Normal Rain
    public double getNormalRainChance() {
        return config.getDouble("weather.normal-rain.chance", 0.33);
    }
    public int getNormalRainMinDay() {
        return config.getInt("weather.normal-rain.min-day-occurrence", 1);
    }
    public int getNormalRainMaxDay() {
        return config.getInt("weather.normal-rain.max-day-occurrence", 3);
    }
    public int getNormalRainDurationMin() {
        return config.getInt("weather.normal-rain.duration-min-days", 1);
    }
    public int getNormalRainDurationMax() {
        return config.getInt("weather.normal-rain.duration-max-days", 2);
    }
    // Heavy Storm
    public double getHeavyStormChance() {
        return config.getDouble("weather.heavy-storm.chance", 0.50);
    }
    public int getHeavyStormMinDay() {
        return config.getInt("weather.heavy-storm.min-day-occurrence", 5);
    }
    public int getHeavyStormMaxDay() {
        return config.getInt("weather.heavy-storm.max-day-occurrence", 6);
    }
    public int getHeavyStormDurationMin() {
        return config.getInt("weather.heavy-storm.duration-min-days", 4);
    }
    public int getHeavyStormDurationMax() {
        return config.getInt("weather.heavy-storm.duration-max-days", 5);
    }
}
