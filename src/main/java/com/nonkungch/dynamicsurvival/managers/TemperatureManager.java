package com.nonkungch.dynamicsurvival.managers;

import org.bukkit.entity.Player;
import com.nonkungch.dynamicsurvival.DynamicSurvival;

import java.util.HashMap;
import java.util.Map;

public class TemperatureManager {

    private final DynamicSurvival plugin;
    private final Map<Player, Double> temperatures = new HashMap<>();

    public TemperatureManager(DynamicSurvival plugin) {
        this.plugin = plugin;
    }

    public double getTemperature(Player player) {
        return temperatures.getOrDefault(player, 37.0);
    }

    public void setTemperature(Player player, double temp) {
        temperatures.put(player, temp);
    }
}