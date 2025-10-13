package com.nonkungch.dynamicsurvival.managers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;

public class TimeManager {
    
    private final JavaPlugin plugin;
    private final Scoreboard board;
    private final long DAYS_PER_SEASON; 
    
    // Constants สำหรับฤดูกาล
    public static final int SPRING = 1;
    public static final int SUMMER = 2;
    public static final int AUTUMN = 3;
    public static final int WINTER = 4;
    private static final long TICKS_IN_DAY = 24000L;

    public TimeManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.board = Bukkit.getScoreboardManager().getMainScoreboard();
        
        this.DAYS_PER_SEASON = plugin.getConfig().getLong("season-length-days", 30L); 
        
        if (getGlobalScore("season") == 0) {
            setGlobalScore("season", SPRING);
        }
    }
    
    public void updateTimeAndSeason() {
        setGlobalScore("global_timer", getGlobalScore("global_timer") + 20); 
        
        long totalDays = getGlobalScore("global_timer") / TICKS_IN_DAY;
        
        // ตรวจสอบการเปลี่ยนฤดูกาล (โดยใช้ Modulo เพื่อจัดการการ Reset Timer)
        if (totalDays > 0 && totalDays % DAYS_PER_SEASON == 0) {
             changeSeason();
             // Reset global_timer เพื่อให้ dayInSeason นับใหม่
             setGlobalScore("global_timer", 0); 
        }
    }
    
    public void changeSeason() {
        int currentSeason = getCurrentSeason();
        int nextSeason = (currentSeason % 4) + 1;
        setGlobalScore("season", nextSeason);
        
        String seasonName = getSeasonName(nextSeason);
        
        // ส่งข้อความแจ้งเตือนตาม Config
        String message = plugin.getConfig().getString("messages.season-change", "&b[DS] ฤดูกาลได้เปลี่ยนเป็น &a%season_name% &bแล้ว!");
        message = message.replace("%season_name%", seasonName);
        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', message));
        
        // *** ระบบ Biome Color/Global Effect: ***
        // (WINTER มีหิมะตก - สามารถใช้ Bukkit API ได้)
        if (nextSeason == WINTER) {
            Bukkit.getWorlds().forEach(world -> world.setStorm(true));
        } else {
            Bukkit.getWorlds().forEach(world -> world.setStorm(false));
        }
    }

    public int getCurrentSeason() {
        return getGlobalScore("season");
    }
    
    public long getGameDay() {
        return getGlobalScore("global_timer") / TICKS_IN_DAY;
    }
    
    public String getSeasonName(int season) {
        switch (season) {
            case SPRING: return "ใบไม้ผลิ";
            case SUMMER: return "ร้อน";
            case AUTUMN: return "ใบไม้ร่วง";
            case WINTER: return "หนาว";
            default: return "Unknown";
        }
    }
    
    public String getSeasonDisplay() {
        String name = getSeasonName(getCurrentSeason());
        // กำหนดสีตามฤดู (ตามความต้องการของผู้ใช้)
        ChatColor color = ChatColor.AQUA;
        if (getCurrentSeason() == SUMMER) color = ChatColor.RED;
        else if (getCurrentSeason() == WINTER) color = ChatColor.WHITE;
        else if (getCurrentSeason() == AUTUMN) color = ChatColor.GOLD;
        return color + name;
    }
    
    public String getSeasonMonthUI() {
        long totalTicks = getGlobalScore("global_timer");
        long dayInSeason = totalTicks / TICKS_IN_DAY; // นับ 0-29
        
        int blocks = 10;
        int filled = (int) Math.floor((double) dayInSeason / DAYS_PER_SEASON * blocks);
        
        ChatColor filledColor = ChatColor.GREEN;
        if (getCurrentSeason() == WINTER) filledColor = ChatColor.BLUE;
        else if (getCurrentSeason() == SUMMER) filledColor = ChatColor.RED;
        else if (getCurrentSeason() == AUTUMN) filledColor = ChatColor.GOLD;
        
        String filledBar = filledColor + "█".repeat(filled);
        String emptyBar = ChatColor.GRAY + "█".repeat(blocks - filled);
        
        return filledBar + emptyBar + ChatColor.RESET;
    }

    // Helper methods สำหรับ Global Scoreboard
    public int getGlobalScore(String objectiveName) {
        return board.getObjective(objectiveName).getScore("Global").getScore();
    }
    
    public void setGlobalScore(String objectiveName, int value) {
        board.getObjective(objectiveName).getScore("Global").setScore(value);
    }
          }
