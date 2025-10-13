package com.nonkungch.dynamicsurvival.managers;

import org.bukkit.entity.Player;
import com.nonkungch.dynamicsurvival.DynamicSurvival;

import java.util.HashMap;
import java.util.Map;

public class ThirstManager {

    private final DynamicSurvival plugin;
    private final Map<Player, Double> thirsts = new HashMap<>();

    public ThirstManager(DynamicSurvival plugin) {
        this.plugin = plugin;
    }

    public double getThirst(Player player) {
        return thirsts.getOrDefault(player, 100.0);
    }

    public void setThirst(Player player, double value) {
        thirsts.put(player, value);
    }
}