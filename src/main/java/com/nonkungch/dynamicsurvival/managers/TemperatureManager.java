package com.nonkungch.dynamicsurvival.managers;

import com.nonkungch.dynamicsurvival.DynamicSurvival;
import org.bukkit.Bukkit; // <-- แก้ไข: Import Bukkit
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType; // MINING_FATIGUE ใช้ได้ใน 1.21+
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

public class TemperatureManager implements Listener {

    private final DynamicSurvival plugin;
    private final int MAX_TEMP = 100;
    private final int MIN_TEMP = 0;

    public TemperatureManager(DynamicSurvival plugin) { 
        this.plugin = plugin;
    }

    public void checkAndSetupPlayer(Player player) {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        // ตรวจสอบและตั้งค่าเริ่มต้นอุณหภูมิสำหรับผู้เล่นใหม่
        if (getScore(player, "temp") == 0) {
            Objective obj = board.getObjective("temp");
            if (obj != null) {
                obj.getScore(player.getName()).setScore(50); // ตั้งค่าเริ่มต้นที่ 50
            }
        }
    }

    public void processTemperature(Player player) {
        long gameTime = plugin.getTimeManager().getCurrentDayTime();
        int currentTemp = getScore(player, "temp");
        int newTemp = currentTemp;

        // ตัวอย่างตรรกะ: อุณหภูมิจะขึ้นในช่วงกลางวัน
        if (gameTime > 2000 && gameTime < 13000) {
            newTemp += 1; // อุณหภูมิเพิ่มขึ้น
        } else {
            newTemp -= 1; // อุณหภูมิลดลง
        }

        // จำกัดค่า
        newTemp = Math.min(Math.max(newTemp, MIN_TEMP), MAX_TEMP);

        // นำไปใช้กับ Scoreboard
        Objective obj = Bukkit.getScoreboardManager().getMainScoreboard().getObjective("temp");
        if (obj != null) {
            obj.getScore(player.getName()).setScore(newTemp);
        }

        // ผลกระทบจากอุณหภูมิ (Effects)
        applyEffects(player, newTemp);
    }
    
    private void applyEffects(Player player, int temp) {
        // Hot Effects
        if (temp > 85) {
            // ใช้ MINING_FATIGUE ซึ่งถูกต้องสำหรับ 1.21+ (แก้ไข Error 4)
            player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 20 * 15, 1, true, true)); 
            player.setFireTicks(20); // ลุกไหม้เล็กน้อย
            player.sendMessage(ChatColor.RED + "คุณร้อนเกินไป! ต้องหาที่เย็นๆ");
        } else if (temp < 15) {
            // Cold Effects
            player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 20 * 15, 0, true, true));
            player.sendMessage(ChatColor.AQUA + "คุณหนาวสั่น! ต้องหาที่อุ่นๆ");
        } else {
            // Remove Effects
            player.removePotionEffect(PotionEffectType.MINING_FATIGUE);
            player.removePotionEffect(PotionEffectType.WEAKNESS);
        }
    }

    public int getScore(Player player, String objectiveName) {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        Objective obj = board.getObjective(objectiveName);
        if (obj != null && obj.getScore(player.getName()).isScoreSet()) {
            return obj.getScore(player.getName()).getScore();
        }
        return 0;
    }
}
