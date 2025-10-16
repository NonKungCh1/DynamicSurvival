// /src/main/java/com/nonkungch/dynamicsurvival/DynamicSurvivalAPI.java

package com.nonkungch.dynamicsurvival;

import org.bukkit.entity.Player;

public class DynamicSurvivalAPI {

    private static DynamicSurvival plugin;

    // เมธอดนี้สำหรับให้ปลั๊กอินหลักเรียกใช้เท่านั้น
    public static void initialize(DynamicSurvival instance) {
        plugin = instance;
    }

    // เมธอดนี้สำหรับให้ Addon เรียกใช้เพื่อเข้าถึง API
    public static DynamicSurvivalAPI getInstance() {
        if (plugin == null || !plugin.isEnabled()) {
            throw new IllegalStateException("DynamicSurvival plugin is not ready or the API has not been initialized!");
        }
        return new DynamicSurvivalAPI();
    }

    // --- เมธอดสาธารณะที่ Addon จะเรียกใช้ ---

    /**
     * รับฤดูกาลปัจจุบันของเซิร์ฟเวอร์
     * @return ฤดูกาลปัจจุบัน (Season)
     */
    public Season getCurrentSeason() {
        return plugin.getCurrentSeason();
    }
    
    /**
     * รับค่าสถิติ (อุณหภูมิ, ความกระหาย) ของผู้เล่น
     * @param player ผู้เล่นที่ต้องการดูข้อมูล
     * @return อ็อบเจกต์ PlayerStats
     */
    public DynamicSurvival.PlayerStats getPlayerStats(Player player) {
        return plugin.getPlayerStats(player);
    }
}
