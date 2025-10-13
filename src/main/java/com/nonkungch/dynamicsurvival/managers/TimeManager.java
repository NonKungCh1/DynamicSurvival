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

        Objective timerObj = board.getObjective("global_timer");
        if (timerObj == null)
            timerObj = board.registerNewObjective("global_timer", "dummy", "Global Ticks");
        timerObj.getScore("GLOBAL_TICK").setScore(0);

        Objective seasonObj = board.getObjective("season");
        if (seasonObj == null)
            board.registerNewObjective("season", "dummy", "Season");
    }

    public void updateTimeAndSeason() {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        Objective timerObj = board.getObjective("global_timer");

        if (timerObj != null) {
            globalTicks = timerObj.getScore("GLOBAL_TICK").getScore() + 1;
            timerObj.getScore("GLOBAL_TICK").setScore((int) globalTicks);
        }

        // 1 วันในเกม = 1200 ticks (ประมาณ 60 วินาที)
        if (globalTicks % 1200 == 0) {
            gameDay++;
            updateSeason();
        }
    }

    private void updateSeason() {
        long dayInSeason = gameDay % 120;

        if (dayInSeason < 30) currentSeason = "Spring";
        else if (dayInSeason < 60) currentSeason = "Summer";
        else if (dayInSeason < 90) currentSeason = "Autumn";
        else currentSeason = "Winter";
    }

    public long getCurrentDayTime() {
        if (Bukkit.getWorlds().isEmpty()) return 6000;
        World world = Bukkit.getWorlds().get(0);
        return world != null ? world.getTime() : 6000;
    }

    public long getGameDay() {
        return gameDay;
    }

    public String getSeasonDisplay() {
        ChatColor color;
        switch (currentSeason) {
            case "Summer": color = ChatColor.RED; break;
            case "Autumn": color = ChatColor.GOLD; break;
            case "Winter": color = ChatColor.AQUA; break;
            default: color = ChatColor.GREEN; break;
        }
        return color + currentSeason;
    }

    public String getSeasonMonthUI() {
        long dayInSeason = gameDay % 30;
        if (dayInSeason < 10) return "[Month 1]";
        if (dayInSeason < 20) return "[Month 2]";
        return "[Month 3]";
    }
}