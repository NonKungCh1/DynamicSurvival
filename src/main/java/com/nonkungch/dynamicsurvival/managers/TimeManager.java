package com.nonkungch.dynamicsurvival.managers;

import com.nonkungch.dynamicsurvival.DynamicSurvival;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

public class TimeManager {

    private final DynamicSurvival plugin;
    private long globalTicks = 0;
    private long gameDay = 0;
    private String currentSeason = "Spring";

    public TimeManager(DynamicSurvival plugin) {
        this.plugin = plugin;
        setupInitialValues();
    }
    
    private void setupInitialValues() {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        
        // Initial setup for Global Timer
        Objective timerObj = board.getObjective("global_timer");
        if (timerObj == null) {
            timerObj = board.registerNewObjective("global_timer", "dummy", "Global Ticks");
        }
        timerObj.getScore("GLOBAL_TICK").setScore(0);
        
        // Initial setup for Thirst Timer
        Objective thirstTimerObj = board.getObjective("thirst_timer");
        if (thirstTimerObj == null) {
            thirstTimerObj = board.registerNewObjective("thirst_timer", "dummy", "Thirst Ticks");
        }
        thirstTimerObj.getScore("THIRST_TICK").setScore(0);
    }

    public void updateTimeAndSeason() {
        // 1. Update Global Ticks (รันทุก 1 วินาทีตาม LOOP_INTERVAL)
        Objective timerObj = Bukkit.getScoreboardManager().getMainScoreboard().getObjective("global_timer");
        if (timerObj != null) {
            globalTicks = timerObj.getScore("GLOBAL_TICK").getScore() + 1;
            timerObj.getScore("GLOBAL_TICK").setScore((int) globalTicks);
        }
        
        // 2. Update Thirst Ticks (รันแยก)
        Objective thirstTimerObj = Bukkit.getScoreboardManager().getMainScoreboard().getObjective("thirst_timer");
        if (thirstTimerObj != null) {
            int currentThirstTick = thirstTimerObj.getScore("THIRST_TICK").getScore() + 1;
            if (currentThirstTick > (20 * 60 * 60)) { // Reset ทุกชั่วโมงเพื่อไม่ให้ Score ล้น
                currentThirstTick = 1;
            }
            thirstTimerObj.getScore("THIRST_TICK").setScore(currentThirstTick);
        }

        // 3. Update Game Day (สมมติ 1 วันเกม = 1,200 Global Ticks)
        if (globalTicks % 1200 == 0) { 
            gameDay++;
            updateSeason();
        }
    }
    
    private void updateSeason() {
        // ตัวอย่าง: ฤดูเปลี่ยนทุก 30 วัน
        long dayInSeason = gameDay % 120; // 120 วันต่อปี
        
        if (dayInSeason < 30) {
            currentSeason = "Spring";
        } else if (dayInSeason < 60) {
            currentSeason = "Summer";
        } else if (dayInSeason < 90) {
            currentSeason = "Autumn";
        } else {
            currentSeason = "Winter";
        }
        
        // อัพเดท Scoreboard
        Objective seasonObj = Bukkit.getScoreboardManager().getMainScoreboard().getObjective("season");
        if (seasonObj != null) {
            seasonObj.getScore("CURRENT_SEASON_VALUE").setScore((int)(dayInSeason));
        }
    }

    /**
     * ดึงค่าเวลาของโลกหลัก (World time)
     * @return ค่าเวลาในเกม (0-24000)
     */
    public long getCurrentDayTime() {
        if (Bukkit.getWorlds().isEmpty()) {
            return 6000;
        }
        World world = Bukkit.getWorlds().get(0);
        return world != null ? world.getTime() : 6000;
    }
    
    public long getGameDay() {
        return gameDay;
    }

    public String getSeasonDisplay() {
        ChatColor color;
        switch (currentSeason) {
            case "Summer":
                color = ChatColor.RED;
                break;
            case "Autumn":
                color = ChatColor.GOLD;
                break;
            case "Winter":
                color = ChatColor.AQUA;
                break;
            case "Spring":
            default:
                color = ChatColor.GREEN;
                break;
        }
        return color + currentSeason;
    }
    
    public String getSeasonMonthUI() {
        // ตัวอย่าง: แสดงความคืบหน้าของฤดู (สมมติว่า 1 เดือน = 10 วัน)
        long dayInSeason = gameDay % 30;
        if (dayInSeason < 10) return "[Month 1]";
        if (dayInSeason < 20) return "[Month 2]";
        return "[Month 3]";
    }
}
