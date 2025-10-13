package com.nonkungch.dynamicsurvival;

import com.nonkungch.dynamicsurvival.commands.CommandManager;
import com.nonkungch.dynamicsurvival.listeners.PlayerListener;
import com.nonkungch.dynamicsurvival.managers.TemperatureManager;
import com.nonkungch.dynamicsurvival.managers.ThirstManager;
import com.nonkungch.dynamicsurvival.managers.TimeManager;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class DynamicSurvival extends JavaPlugin {

    private TimeManager timeManager;
    private ThirstManager thirstManager;
    private TemperatureManager tempManager;

    private final long LOOP_INTERVAL = 20L; // 1 second

    @Override
    public void onEnable() {
        saveDefaultConfig();
        setupScoreboard();

        this.timeManager = new TimeManager(this);
        this.thirstManager = new ThirstManager(this);
        this.tempManager = new TemperatureManager(this);

        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getCommand("ds").setExecutor(new CommandManager(this));

        startSurvivalLoop();
        getLogger().info("[DS] Dynamic Survival Plugin Enabled! (Author: NonKungCh)");
    }

    private void setupScoreboard() {
        var board = Bukkit.getScoreboardManager().getMainScoreboard();

        if (board.getObjective("thirst") == null)
            board.registerNewObjective("thirst", "dummy", "💧 Thirst");
        if (board.getObjective("temp") == null)
            board.registerNewObjective("temp", "dummy", "🌡️ Temp");
        if (board.getObjective("global_timer") == null)
            board.registerNewObjective("global_timer", "dummy", "Global Timer");
        if (board.getObjective("season") == null)
            board.registerNewObjective("season", "dummy", "Season");
        if (board.getObjective("thirst_timer") == null)
            board.registerNewObjective("thirst_timer", "dummy", "Thirst Timer");
    }

    private void startSurvivalLoop() {
        new BukkitRunnable() {
            @Override
            public void run() {
                timeManager.updateTimeAndSeason();

                for (Player player : Bukkit.getOnlinePlayers()) {
                    thirstManager.checkAndSetupPlayer(player);
                    tempManager.checkAndSetupPlayer(player);

                    thirstManager.processThirst(player);
                    tempManager.processTemperature(player);

                    displaySurvivalStats(player);
                }
            }
        }.runTaskTimer(this, 0L, LOOP_INTERVAL);
    }

    private void displaySurvivalStats(Player player) {
        int thirst = thirstManager.getScore(player, "thirst");
        int temp = tempManager.getScore(player, "temp");
        String seasonDisplay = timeManager.getSeasonDisplay();
        String seasonUI = timeManager.getSeasonMonthUI();
        long gameDay = timeManager.getGameDay();

        String thirstColor = (thirst <= 5) ? ChatColor.RED + "💧" : ChatColor.BLUE + "💧";
        String tempColor = (temp > 75 || temp < 25) ? ChatColor.RED + "🌡️" : ChatColor.GOLD + "🌡️";

        String message = thirstColor + thirst + "/20 | " + tempColor + temp + "/100 | "
                + seasonDisplay + " " + seasonUI + ChatColor.YELLOW + " Day " + gameDay;

        if (Bukkit.getServer().getName().contains("Paper")) {
            player.sendActionBar(Component.text(ChatColor.stripColor(message)));
        } else {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
        }
    }

    @Override
    public void onDisable() {
        var board = Bukkit.getScoreboardManager().getMainScoreboard();
        for (String obj : new String[]{"thirst", "temp", "global_timer", "season", "thirst_timer"}) {
            if (board.getObjective(obj) != null) board.getObjective(obj).unregister();
        }
        getLogger().info("[DS] Dynamic Survival Plugin Disabled.");
    }

    public ThirstManager getThirstManager() { return thirstManager; }
    public TemperatureManager getTempManager() { return tempManager; }
    public TimeManager getTimeManager() { return timeManager; }
}