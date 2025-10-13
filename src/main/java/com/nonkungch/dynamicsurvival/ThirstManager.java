package com.nonkungch.dynamicsurvival.managers;

import com.nonkungch.dynamicsurvival.DynamicSurvival;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

public class ThirstManager {

    private final DynamicSurvival plugin;
    private final int MAX_THIRST = 20;

    public ThirstManager(DynamicSurvival plugin) {
        this.plugin = plugin;
    }

    public void checkAndSetupPlayer(Player player) {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        if (getScore(player, "thirst") == 0) {
            Objective obj = board.getObjective("thirst");
            if (obj != null) obj.getScore(player.getName()).setScore(MAX_THIRST);
        }
    }

    public void processThirst(Player player) {
        Objective timerObj = Bukkit.getScoreboardManager().getMainScoreboard().getObjective("thirst_timer");
        if (timerObj != null && timerObj.getScore("THIRST_TICK").getScore() % (20 * 5) == 0) {
            int currentThirst = getScore(player, "thirst");
            if (currentThirst > 0) {
                currentThirst--;
                Objective thirstObj = Bukkit.getScoreboardManager().getMainScoreboard().getObjective("thirst");
                if (thirstObj != null) thirstObj.getScore(player.getName()).setScore(currentThirst);

                if (currentThirst == 0) {
                    player.damage(1.0);
                    player.sendMessage(ChatColor.RED + "คุณขาดน้ำจนอ่อนแรง!");
                }
            }
        }
    }

    public void refillThirst(Player player, int amount) {
        int current = getScore(player, "thirst");
        int newThirst = Math.min(current + amount, MAX_THIRST);
        Objective obj = Bukkit.getScoreboardManager().getMainScoreboard().getObjective("thirst");
        if (obj != null) obj.getScore(player.getName()).setScore(newThirst);
        player.sendMessage(ChatColor.AQUA + "คุณดื่มน้ำและฟื้นคืนความกระหาย!");
    }

    public int getScore(Player player, String objectiveName) {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        Objective obj = board.getObjective(objectiveName);
        if (obj != null && obj.getScore(player.getName()).isScoreSet())
            return obj.getScore(player.getName()).getScore();
        return 0;
    }

    public void setScore(Player player, String obj, int value) {
        Objective o = Bukkit.getScoreboardManager().getMainScoreboard().getObjective(obj);
        if (o != null) o.getScore(player.getName()).setScore(value);
    }
}