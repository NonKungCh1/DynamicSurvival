package com.nonkungch.dynamicsurvival;

import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.entity.Player;

public class PlayerListener implements Listener {

    private final DynamicSurvival plugin;

    public PlayerListener(DynamicSurvival plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.sendActionBar(player, "§aยินดีต้อนรับสู่ DynamicSurvival!");
    }
}