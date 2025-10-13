package com.nonkungch.dynamicsurvival;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import net.kyori.adventure.text.Component;

import com.nonkungch.dynamicsurvival.managers.TemperatureManager;
import com.nonkungch.dynamicsurvival.managers.TimeManager;
import com.nonkungch.dynamicsurvival.CommandManager;

public class DynamicSurvival extends JavaPlugin {

    private static DynamicSurvival instance;
    private TemperatureManager temperatureManager;
    private TimeManager timeManager;

    @Override
    public void onEnable() {
        instance = this;

        // สร้าง Manager
        this.temperatureManager = new TemperatureManager(this);
        this.timeManager = new TimeManager(this);

        // ลงทะเบียนคำสั่ง
        getCommand("dsurvival").setExecutor(new CommandManager(this));

        getLogger().info("DynamicSurvival has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("DynamicSurvival has been disabled!");
    }

    public static DynamicSurvival getInstance() {
        return instance;
    }

    public TemperatureManager getTempManager() {
        return temperatureManager;
    }

    public TimeManager getTimeManager() {
        return timeManager;
    }

    public void sendActionBar(Player player, String message) {
        // ใช้ Adventure API ส่งข้อความ ActionBar แบบใหม่
        player.sendActionBar(Component.text(message));
    }
}