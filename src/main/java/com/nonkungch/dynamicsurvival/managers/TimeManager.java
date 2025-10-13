package com.nonkungch.dynamicsurvival.managers;

import com.nonkungch.dynamicsurvival.DynamicSurvival;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

public class TimeManager {

    private final DynamicSurvival plugin;
    private final int MAX_TEMP = 100;
    private final int MIN_TEMP = 0;

    public TimeManager(DynamicSurvival plugin) {
        this.plugin = plugin;
    }

    public void checkAndSetupPlayer(Player player) {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        if (getScore(player, "temp") == 0) {
            Objective obj = board.getObjective("temp");
            if (obj != null) obj.getScore(player.getName()).setScore(50); // ค่าเริ่มต้น 50
        }
    }

    public void processTemperature(Player player) {
        long gameTime = plugin.getTimeManager().getCurrentDayTime();
        int currentTemp = getScore(player, "temp");
        int newTemp = currentTemp;

        // กลางวันร้อน กลางคืนเย็น
        if (gameTime > 2000 && gameTime < 13000) {
            newTemp += 1;
        } else {
            newTemp -= 1;
        }

        // จำกัดขอบเขต 0–100
        newTemp = Math.min(Math.max(newTemp, MIN_TEMP), MAX_TEMP);

        Objective obj = Bukkit.getScoreboardManager().getMainScoreboard().getObjective("temp");
        if (obj != null) obj.getScore(player.getName()).setScore(newTemp);

        applyEffects(player, newTemp);
    }

    private void applyEffects(Player player, int temp) {
        if (temp > 85) {
            player.addPotionEffect(PotionEffectType.MINING_FATIGUE.createEffect(20 * 15, 1));
            player.setFireTicks(20);
            player.sendMessage(ChatColor.RED + "คุณร้อนเกินไป! ต้องหาที่เย็นๆ");
        } else if (temp < 15) {
            player.addPotionEffect(PotionEffectType.WEAKNESS.createEffect(20 * 15, 0));
            player.sendMessage(ChatColor.AQUA + "คุณหนาวสั่น! ต้องหาที่อุ่นๆ");
        } else {
            player.removePotionEffect(PotionEffectType.MINING_FATIGUE);
            player.removePotionEffect(PotionEffectType.WEAKNESS);
        }
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