package com.nonkungch.dynamicsurvival;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import net.kyori.adventure.text.Component;

import com.nonkungch.dynamicsurvival.managers.TemperatureManager;
import com.nonkungch.dynamicsurvival.managers.ThirstManager;
import com.nonkungch.dynamicsurvival.managers.TimeManager;

public class DynamicSurvival extends JavaPlugin {

    private static DynamicSurvival instance;

    private TemperatureManager temperatureManager;
    private ThirstManager thirstManager;
    private TimeManager timeManager;

    @Override
    public void onEnable() {
        instance = this;

        // Init managers
        this.temperatureManager = new TemperatureManager(this);
        this.thirstManager = new ThirstManager(this);
        this.timeManager = new TimeManager(this);

        // Register command & listener
        getCommand("dsurvival").setExecutor(new CommandManager(this));
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        // เริ่มระบบอัปเดตทุก tick (1 tick = 1/20 sec)
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    updatePlayer(player);
                }
            }
        }.runTaskTimer(this, 0, 20); // ทุกวินาที

        getLogger().info("DynamicSurvival enabled!");
    }

    @Override
    public void onDisable() {
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

    public void sendActionBar(Player player, String message) {
        player.sendActionBar(Component.text(message));
    }

    // ---- ระบบหลักอัปเดตผู้เล่น ----
    private void updatePlayer(Player player) {
        double temp = temperatureManager.getTemperature(player);
        double thirst = thirstManager.getThirst(player);

        // อัปเดต Temperature (ตัวอย่าง: ลด 0.1 ทุก tick)
        temp -= 0.1;
        temperatureManager.setTemperature(player, temp);

        // อัปเดต Thirst (ตัวอย่าง: ลด 0.5 ทุก tick)
        thirst -= 0.5;
        thirstManager.setThirst(player, thirst);

        // ActionBar แสดงค่าเรียลไทม์
        sendActionBar(player,
                "§eTemp: §b" + String.format("%.1f", temp) + "°C §7| Thirst: §b" + String.format("%.0f", thirst) + "%");

        // เอฟเฟกต์ตามสภาพ
        if (temp < 35) player.setFreezeTicks(100); // ตัวอย่าง: หนาวเกิน → Freezing
        if (temp > 40) player.damage(1);          // ตัวอย่าง: ร้อนเกิน → Damage
        if (thirst < 20) player.damage(1);        // ตัวอย่าง: กระหายน้ำ → Damage
    }
}