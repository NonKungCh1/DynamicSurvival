package com.nonkungch.dynamicsurvival;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;

import com.nonkungch.dynamicsurvival.managers.TemperatureManager;
import com.nonkungch.dynamicsurvival.managers.ThirstManager;
import com.nonkungch.dynamicsurvival.managers.TimeManager;

public class DynamicSurvival extends JavaPlugin {

    private static DynamicSurvival instance;

    private TemperatureManager temperatureManager;
    private ThirstManager thirstManager;
    private TimeManager timeManager;

    private BukkitAudiences adventure;

    @Override
    public void onEnable() {
        instance = this;
        adventure = BukkitAudiences.create(this);

        // Init managers
        this.temperatureManager = new TemperatureManager(this);
        this.thirstManager = new ThirstManager(this);
        this.timeManager = new TimeManager(this);

        // Register command & listener
        getCommand("dsurvival").setExecutor(new CommandManager(this));
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        // เริ่มระบบอัปเดตทุกวินาที
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    updatePlayer(player);
                }
            }
        }.runTaskTimer(this, 0, 20);

        getLogger().info("DynamicSurvival enabled!");
    }

    @Override
    public void onDisable() {
        if (adventure != null) {
            adventure.close();
            adventure = null;
        }
        getLogger().info("DynamicSurvival disabled!");
    }

    public static DynamicSurvival getInstance() {
        return instance;
    }

    public TemperatureManager getTempManager() {
        return temperatureManager;
    }

    public ThirstManager getThirstManager() {
        return thirstManager;
    }

    public TimeManager getTimeManager() {
        return timeManager;
    }

    // ส่ง ActionBar ผ่าน Adventure Platform
    public void sendActionBar(Player player, String message) {
        adventure.player(player).sendActionBar(Component.text(message));
    }

    // ระบบหลัก: อัปเดตผู้เล่น
    private void updatePlayer(Player player) {
        double temp = temperatureManager.getTemperature(player);
        double thirst = thirstManager.getThirst(player);

        // ลดค่าตามเวลา
        temp -= 0.1;
        thirst -= 0.5;

        temperatureManager.setTemperature(player, temp);
        thirstManager.setThirst(player, thirst);

        // ส่ง ActionBar
        sendActionBar(player,
                "§eTemp: §b" + String.format("%.1f", temp) + "°C §7| Thirst: §b" + String.format("%.0f", thirst) + "%");

        // เอฟเฟกต์ตามสภาพ
        if (temp < 35) player.setFreezeTicks(100);   // หนาวเกิน → Freezing
        if (temp > 40) player.damage(1);            // ร้อนเกิน → Damage
        if (thirst < 20) player.damage(1);          // กระหายน้ำ → Damage
    }
}