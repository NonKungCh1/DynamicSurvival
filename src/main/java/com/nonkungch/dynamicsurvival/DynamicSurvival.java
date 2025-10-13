package com.nonkungch.dynamicsurvival;

import com.nonkungch.dynamicsurvival.commands.CommandManager;
import com.nonkungch.dynamicsurvival.listeners.PlayerListener;
import com.nonkungch.dynamicsurvival.managers.TemperatureManager;
import com.nonkungch.dynamicsurvival.managers.ThirstManager;
import com.nonkungch.dynamicsurvival.managers.TimeManager;
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
    
    private final long LOOP_INTERVAL = 20L; // รันทุก 1 วินาที (20 ticks)

    @Override
    public void onEnable() {
        // 1. จัดการ Config
        saveDefaultConfig(); 
        
        // 2. Setup Scoreboard และ Manager
        setupScoreboard();
        this.timeManager = new TimeManager(this);
        this.thirstManager = new ThirstManager(this);
        this.tempManager = new TemperatureManager(this);

        // 3. ลงทะเบียน Event และ Command
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getCommand("ds").setExecutor(new CommandManager(this));

        // 4. เริ่ม Task หลัก
        startSurvivalLoop();

        getLogger().info("[DS] Dynamic Survival Plugin Enabled! (Author: NonKungCh)");
    }

    private void setupScoreboard() {
        // Objectives สำหรับผู้เล่น
        Bukkit.getScoreboardManager().getMainScoreboard().registerNewObjective("thirst", "dummy", "💧 Thirst");
        Bukkit.getScoreboardManager().getMainScoreboard().registerNewObjective("temp", "dummy", "🌡️ Temp");
        
        // Objectives สำหรับ Global Ticks/Season
        Bukkit.getScoreboardManager().getMainScoreboard().registerNewObjective("global_timer", "dummy");
        Bukkit.getScoreboardManager().getMainScoreboard().registerNewObjective("season", "dummy");
        Bukkit.getScoreboardManager().getMainScoreboard().registerNewObjective("thirst_timer", "dummy"); // Timer แยกสำหรับ Thirst
    }

    private void startSurvivalLoop() {
        new BukkitRunnable() {
            @Override
            public void run() {
                // อัพเดท Global System
                timeManager.updateTimeAndSeason(); 

                for (Player player : Bukkit.getOnlinePlayers()) {
                    // 1. ตรวจสอบผู้เล่นใหม่และตั้งค่าเริ่มต้น
                    thirstManager.checkAndSetupPlayer(player);
                    tempManager.checkAndSetupPlayer(player);
                    
                    // 2. รันตรรกะระบบ
                    thirstManager.processThirst(player);
                    tempManager.processTemperature(player);
                    
                    // 3. แสดงผล Action Bar
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
        
        // --- การสร้าง Action Bar UI ---
        String thirstColor = (thirst <= 5) ? ChatColor.RED + "💧" : ChatColor.BLUE + "💧";
        String tempColor = (temp > 75 || temp < 25) ? ChatColor.RED + "🌡️" : ChatColor.GOLD + "🌡️";

        String message = 
            thirstColor + thirst + "/20 | " + tempColor + temp + "/100 | " 
            + seasonDisplay + " " + seasonUI + ChatColor.YELLOW + " Day " + gameDay;
        
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
    }
    
    @Override
    public void onDisable() {
        getLogger().info("[DS] Dynamic Survival Plugin Disabled.");
    }

    // Getter Methods
    public ThirstManager getThirstManager() { return thirstManager; }
    public TemperatureManager getTempManager() { return tempManager; }
    public TimeManager getTimeManager() { return timeManager; }
}
