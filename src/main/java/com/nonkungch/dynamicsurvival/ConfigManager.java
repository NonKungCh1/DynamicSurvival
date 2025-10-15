package com.nonkungch.dynamicsurvival;

import org.bukkit.configuration.file.FileConfiguration;
import java.util.List;

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
    
    public int getLeafArmorRequiredPieces() {
        return config.getInt("special-effects.leaf-armor-set-bonus.required-pieces", 3);
    }
    public float getLeafArmorHeatReduction() {
        return (float) config.getDouble("special-effects.leaf-armor-set-bonus.nether-heat-reduction", 60.0);
    }
    public float getNetherTempIncrease() {
        return (float) config.getDouble("special-effects.nether.temperature-increase", 60.0);
    }
    public float getNetherThirstMultiplier() {
        return (float) config.getDouble("special-effects.nether.thirst-loss-multiplier", 2.0);
    }
    public float getFireTempIncrease() {
        return (float) config.getDouble("special-effects.environment.fire-temp-increase", 10.0);
    }
    public float getWaterSnowTempDecrease() {
        return (float) config.getDouble("special-effects.environment.water-snow-temp-decrease", -10.0);
    }
    public String getScoreboardTitle() {
        return config.getString("scoreboard.title", "§b§lDynamicSurvival");
    }
    public List<String> getScoreboardLines() {
        return config.getStringList("scoreboard.lines");
    }
    public int getWeatherChangeCooldownMinutes() {
        return config.getInt("weather.change-cooldown-minutes", 15);
    }
    public int getSeasonDuration(Season season) {
        return config.getInt("season-duration." + season.toString().toLowerCase(), 20);
    }
    public int getMaxThirst() {
        return config.getInt("thirst.max-level", 100);
    }
    public int getBaseThirstLoss() {
        return config.getInt("thirst.base-loss-per-update", 1);
    }
    public int getWaterBottleRestore() {
        return config.getInt("thirst.water-bottle-restore", 30);
    }
     public int getThirstDangerLevel() {
        return config.getInt("thirst.danger-level", 10);
    }
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
