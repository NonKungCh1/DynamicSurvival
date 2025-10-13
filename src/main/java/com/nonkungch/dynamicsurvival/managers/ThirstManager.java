package com.nonkungch.dynamicsurvival.managers;

import com.nonkungch.dynamicsurvival.DynamicSurvival;
import org.bukkit.Bukkit; // <-- แก้ไข: Import Bukkit
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

public class ThirstManager implements Listener {

    private final DynamicSurvival plugin;
    private final int MAX_THIRST = 20;

    public ThirstManager(DynamicSurvival plugin) {
        this.plugin = plugin;
    }

    public void checkAndSetupPlayer(Player player) {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        // ตรวจสอบและตั้งค่าเริ่มต้นความกระหายน้ำสำหรับผู้เล่นใหม่
        if (getScore(player, "thirst") == 0) {
            Objective obj = board.getObjective("thirst");
            if (obj != null) {
                obj.getScore(player.getName()).setScore(MAX_THIRST); // ตั้งค่าเริ่มต้นที่ 20
            }
        }
    }

    public void processThirst(Player player) {
        // ตรวจสอบว่าถึงเวลาลดค่าความกระหายน้ำหรือยัง (ใช้ Timer จาก Scoreboard)
        Objective timerObj = Bukkit.getScoreboardManager().getMainScoreboard().getObjective("thirst_timer");
        if (timerObj != null && timerObj.getScore("THIRST_TICK").getScore() % (20 * 5) == 0) { // ลดทุก 5 วินาที
            
            int currentThirst = getScore(player, "thirst");
            if (currentThirst > 0) {
                currentThirst -= 1; // ลดค่าความกระหายน้ำ
                
                Objective thirstObj = Bukkit.getScoreboardManager().getMainScoreboard().getObjective("thirst");
                 if (thirstObj != null) {
                    thirstObj.getScore(player.getName()).setScore(currentThirst);
                }
                
                if (currentThirst == 0) {
                    player.damage(1.0); // ถ้าหมดกระหายน้ำจะเริ่มได้รับความเสียหาย
                    player.sendMessage(ChatColor.RED + "คุณขาดน้ำจนอ่อนแรง!");
                }
            }
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
