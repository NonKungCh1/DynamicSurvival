package com.nonkungch.dynamicsurvival;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandManager implements CommandExecutor {

    private final DynamicSurvival plugin;

    public CommandManager(DynamicSurvival plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cคำสั่งนี้ใช้ได้เฉพาะผู้เล่นเท่านั้น!");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§eใช้คำสั่ง: /dsurvival temp §fดูอุณหภูมิ, /dsurvival thirst §fดูความกระหาย");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "temp" -> {
                double temp = plugin.getTempManager().getTemperature(player);
                player.sendMessage("§bอุณหภูมิของคุณ: §f" + String.format("%.1f", temp) + "°C");
            }
            case "thirst" -> {
                double thirst = plugin.getThirstManager().getThirst(player);
                player.sendMessage("§bความกระหายของคุณ: §f" + String.format("%.0f", thirst) + "%");
            }
            default -> player.sendMessage("§cไม่พบคำสั่งย่อยนี้!");
        }

        return true;
    }
}