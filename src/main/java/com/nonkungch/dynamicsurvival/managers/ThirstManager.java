package com.nonkungch.dynamicsurvival.managers;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Scoreboard;

public class ThirstManager {

    private final JavaPlugin plugin;
    private final Scoreboard board;
    private static final int MAX_THIRST = 20;
    
    public ThirstManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.board = Bukkit.getScoreboardManager().getMainScoreboard();
    }

    public void checkAndSetupPlayer(Player player) {
        // ตั้งค่า Thirst เป็นค่าเริ่มต้นจาก Config ถ้าผู้เล่นคนนี้ไม่เคยมี Score มาก่อน
        if (getScore(player, "thirst") == 0) {
            int defaultThirst = plugin.getConfig().getInt("default-thirst", MAX_THIRST);
            setScore(player, "thirst", defaultThirst);
        }
    }

    public void processThirst(Player player) {
        // เพิ่มค่าใน Thirst Timer ทุก 1 วินาที
        setScore(player, "thirst_timer", getScore(player, "thirst_timer") + 1);

        // ลดน้ำทุก 30 วินาที (30 วินาที = 30 ticks ของ Loop)
        if (getScore(player, "thirst_timer") >= 30) {
            setScore(player, "thirst", getScore(player, "thirst") - 1);
            setScore(player, "thirst_timer", 0); // Reset Timer
        }

        int currentThirst = getScore(player, "thirst");
        
        // ผลกระทบเมื่อขาดน้ำ
        if (currentThirst <= 5 && currentThirst > 0) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, 0, false, false));
        } else if (currentThirst <= 0) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 60, 1, false, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 1, false, false));
        }
    }
    
    public void refillThirst(Player player, int amount) {
        int newThirst = Math.min(MAX_THIRST, getScore(player, "thirst") + amount);
        setScore(player, "thirst", newThirst);
        
        // ใช้ข้อความจาก Config
        String message = plugin.getConfig().getString("messages.thirst-refill", "&b💧 ระดับน้ำเพิ่มขึ้น +%amount%!");
        message = message.replace("%amount%", String.valueOf(amount));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    // Helper methods สำหรับจัดการ Scoreboard
    public int getScore(Player player, String objectiveName) {
        return board.getObjective(objectiveName).getScore(player.getName()).getScore();
    }

    public void setScore(Player player, String objectiveName, int value) {
        board.getObjective(objectiveName).getScore(player.getName()).setScore(value);
    }
}
