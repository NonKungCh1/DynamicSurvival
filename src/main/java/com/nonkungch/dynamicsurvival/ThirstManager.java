package com.nonkungch.dynamicsurvival.managers;

import org.bukkit.entity.Player;
import com.nonkungch.dynamicsurvival.DynamicSurvival;

import java.util.HashMap;
import java.util.Map;

public class ThirstManager {

    private final DynamicSurvival plugin;
    // ใช้ Double เพื่อความแม่นยำในการคำนวณ loss
    private final Map<Player, Double> thirsts = new HashMap<>(); 
    private final double DEFAULT_THIRST = 100.0;

    public ThirstManager(DynamicSurvival plugin) {
        this.plugin = plugin;
    }

    public double getThirst(Player player) {
        return thirsts.getOrDefault(player, DEFAULT_THIRST);
    }

    public void setThirst(Player player, double value) {
        double maxThirst = plugin.getConfigManager().getMaxThirst();
        // จำกัดค่าให้อยู่ระหว่าง 0 ถึง MaxThirst
        double clampedValue = Math.min(maxThirst, Math.max(0, value));
        thirsts.put(player, clampedValue);
    }
    
    /**
     * **NEW: เพิ่มค่าความกระหาย**
     */
    public void addThirst(Player player, double amount) {
        double maxThirst = plugin.getConfigManager().getMaxThirst();
        double current = getThirst(player);
        double newThirst = current + amount;
        setThirst(player, newThirst);
    }
}
