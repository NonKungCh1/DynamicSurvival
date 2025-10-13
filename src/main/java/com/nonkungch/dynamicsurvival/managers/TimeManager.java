package com.nonkungch.dynamicsurvival.managers;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class TimeManager {

    private final JavaPlugin plugin;

    public TimeManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void setDay() {
        Bukkit.getWorlds().forEach(w -> w.setTime(1000));
    }

    public void setNight() {
        Bukkit.getWorlds().forEach(w -> w.setTime(13000));
    }
}