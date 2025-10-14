package com.nonkungch.dynamicsurvival.managers;

import org.bukkit.entity.Player;
import com.nonkungch.dynamicsurvival.DynamicSurvival;

import java.util.HashMap;
import java.util.Map;

public class ThirstManager {

    private final DynamicSurvival plugin;
    private final Map<Player, Double> thirsts = new HashMap<>(); 
    // ใช้ค่าเริ่มต้นเป็น Max Level จาก Config (แต่ต้องมีการโหลด Config ก่อน)
    private final double DEFAULT_THIRST = 100.0; 

    public ThirstManager(DynamicSurvival plugin) {
        this.plugin = plugin;
    }

    public double getThirst(Player player) {
        // ให้ค่าเริ่มต้นเป็น Max Level จาก Config ถ้าผู้เล่นยังไม่เคยมีข้อมูล
        return thirsts.getOrDefault(player, (double)plugin.getConfigManager().getMaxThirst()); 
    }

    public void setThirst(Player player, double value) {
        double maxThirst = plugin.getConfigManager().getMaxThirst();
        // จำกัดค่าให้อยู่ระหว่าง 0 ถึง MaxThirst
        double clampedValue = Math.min(maxThirst, Math.max(0, value));
        thirsts.put(player, clampedValue);
    }
    
    /**
     * เพิ่มค่าความกระหาย
     */
    public void addThirst(Player player, double amount) {
        double current = getThirst(player);
        double newThirst = current + amount;
        setThirst(player, newThirst);
    }
}
