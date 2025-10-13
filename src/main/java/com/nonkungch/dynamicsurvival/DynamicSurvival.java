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

public final class DynamicSurvival extends JavaPlugin {

    private TimeManager timeManager;
    private TemperatureManager temperatureManager;
    private ThirstManager thirstManager;

    @Override
    public void onEnable() {
        getLogger().info("DynamicSurvival has been enabled!");

        // สร้าง Managers
        timeManager = new TimeManager(this);
        temperatureManager = new TemperatureManager(this);
        thirstManager = new ThirstManager(this);

        // ลงทะเบียน event & คำสั่ง
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getCommand("dsurvival").setExecutor(new CommandManager(this));

        // ตั้งเวลาอัปเดตทุก 1 วินาที
        new BukkitRunnable() {
            @Override
            public void run() {
                timeManager.updateTimeAndSeason();

                for (Player player : Bukkit.getOnlinePlayers()) {
                    temperatureManager.processTemperature(player);
                    thirstManager.processThirst(player);
                }
            }
        }.runTaskTimer(this, 0L, 20L);
    }

    @Override
    public void onDisable() {
        getLogger().info("DynamicSurvival has been disabled!");
    }

    public TimeManager getTimeManager() {
        return timeManager;
    }

    public TemperatureManager getTemperatureManager() {
        return temperatureManager;
    }

    public ThirstManager getThirstManager() {
        return thirstManager;
    }

    // ✅ แสดงข้อมูลสถานะผ่าน ActionBar (รองรับ Spigot & Paper)
    public void sendActionBar(Player player, String message) {
        if (Bukkit.getServer().getName().contains("Paper")) {
            // Paper API
            player.sendActionBar(Component.text(message));
        } else {
            // Spigot API
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
        }
    }

    // ✅ ตัวอย่างการเรียกใช้ (สามารถเรียกได้จากที่อื่น)
    public void showPlayerStatus(Player player) {
        String status = ChatColor.GOLD + "[Day " + timeManager.getGameDay() + "] "
                + timeManager.getSeasonDisplay() + " "
                + timeManager.getSeasonMonthUI();
        sendActionBar(player, status);
    }
}