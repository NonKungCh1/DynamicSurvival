// /src/main/java/com/nonkungch/dynamicsurvival/ConfigManager.java (ฉบับสมบูรณ์)

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

    // --- Biome Effects (ส่วนที่เพิ่มเข้ามา) ---
    public double getDesertThirstMultiplier() {
        return config.getDouble("biome-effects.desert.thirst-loss-multiplier", 2.0);
    }
    public float getColdBiomeTempDecrease() {
        return (float) config.getDouble("biome-effects.cold-biome.temperature-decrease", -7.0);
    }

    // --- Special Effects: Leaf Armor ---
    public int getLeafArmorRequiredPieces() {
        return config.getInt("special-effects.leaf-armor-set-bonus.required-pieces", 3);
    }
    public float getLeafArmorHeatReduction() {
        return (float) config.getDouble("special-effects.leaf-armor-set-bonus.nether-heat-reduction", 60.0);
    }

    // --- Special Effects: Leather Armor ---
    public int getLeatherArmorRequiredPieces() {
        return config.getInt("special-effects.leather-armor-set-bonus.required-pieces", 3);
    }
    public float getLeatherArmorWarmthBonus() {
        return (float) config.getDouble("special-effects.leather-armor-set-bonus.warmth-bonus", 15.0);
    }

    // --- Special Effects: Nether & Environment ---
    public float getNetherTempIncrease() {
        return (float) config.getDouble("special-effects.nether.temperature-increase", 60.0);
    }
    public float getNetherThirstMultiplier() {
        return (float) config.getDouble("special-effects.nether.thirst-loss-multiplier", 2.5);
    }
    public float getFireTempIncrease() {
        return (float) config.getDouble("special-effects.environment.fire-temp-increase", 10.0);
    }
    public float getWaterSnowTempDecrease() {
        return (float) config.getDouble("special-effects.environment.water-snow-temp-decrease", -10.0);
    }
    public float getPowderSnowTempDecrease() {
        return (float) config.getDouble("special-effects.environment.powder-snow-temp-decrease", -20.0);
    }

    // --- Water Pouch Settings ---
    public int getPouchCapacity(int tier) {
        return config.getInt("water-pouch.tiers." + tier + ".capacity", 6);
    }
    public int getPouchRestoreAmount() {
        return config.getInt("water-pouch.thirst-restore-amount", 30);
    }
    public double getPouchDrinkCooldown() {
        return config.getDouble("water-pouch.drink-cooldown-seconds", 1.5);
    }

    // --- Scoreboard Settings ---
    public String getScoreboardTitle() {
        return config.getString("scoreboard.title", "§b§lDynamicSurvival");
    }
    public List<String> getScoreboardLines() {
        return config.getStringList("scoreboard.lines");
    }

    // --- Weather Settings ---
    public int getWeatherChangeCooldownMinutes() {
        return config.getInt("weather.change-cooldown-minutes", 15);
    }

    // --- Season Duration ---
    public int getSeasonDuration(Season season) {
        return config.getInt("season-duration." + season.toString().toLowerCase(), 20);
    }

    // --- Thirst Settings ---
    public int getMaxThirst() {
        return config.getInt("thirst.max-level", 100);
    }
    public double getBaseThirstLoss() {
        return config.getDouble("thirst.base-loss-per-update", 0.4);
    }
    public int getWaterBottleRestore() {
        return config.getInt("thirst.water-bottle-restore", 30);
    }
     public int getThirstDangerLevel() {
        return config.getInt("thirst.danger-level", 10);
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
        return (float) config.getDouble("temperature.thresholds.cold", 0.0);
    }
}
