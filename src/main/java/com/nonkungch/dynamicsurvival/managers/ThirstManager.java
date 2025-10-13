package com.nonkungch.dynamicsurvival.managers;

import com.nonkungch.dynamicsurvival.DynamicSurvival;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

public class ThirstManager {

    private final DynamicSurvival plugin;
    private final int MAX_THIRST = 100;
    private final int MIN_THIRST = 0;

    public ThirstManager(DynamicSurvival plugin) {
        this.plugin = plugin;
    }

    public void checkAndSetupPlayer(Player player) {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        if (getScore(player, "thirst") == 0) {
            Objective obj = board.getObjective("thirst");
            if (obj != null) obj.getScore(player.getName()).setScore(100);
        }
    }

    public void processThirst(Player player) {
        int thirst = getScore(player, "thirst");
        thirst = Math.max(MIN_THIRST, thirst - 1);

        Objective obj = Bukkit.getScoreboardManager().getMainScoreboard().getObjective("thirst");
        if (obj != null) obj.getScore(player.getName()).setScore(thirst);

        if (thirst <= 10) {
            player.sendMessage(ChatColor.AQUA + "คุณกำลังจะขาดน้ำ! รีบดื่มน้ำโดยเร็ว");
        }

        if (thirst == 0) {
            player.damage(1.0);
        }
    }

    public void drinkWater(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.POTION) {
            PotionMeta meta = (PotionMeta) item.getItemMeta();
            PotionData data = meta.getBasePotionData();

            if (data.getType() == PotionType.WATER) {
                setScore(player, "thirst", 100);
                player.sendMessage(ChatColor.GREEN + "คุณดื่มน้ำและรู้สึกสดชื่นขึ้น!");
                item.setAmount(item.getAmount() - 1);
            }
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