package com.nonkungch.dynamicsurvival;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CommandManager implements CommandExecutor {

    private final DynamicSurvival plugin;

    public CommandManager(DynamicSurvival plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        // 1. ตรวจสอบ Permission ก่อน
        if (!sender.hasPermission("dynamicsurvival.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§eUsage: /ds <setseason|setthirst|settemp|info|reload>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "info" -> {
                sender.sendMessage("§aDynamicSurvival Plugin v" + plugin.getDescription().getVersion());
                sender.sendMessage("§7Author: NonKungCh");
            }

            case "reload" -> {
                // ส่วนนี้ปกติจะใช้โหลด config ใหม่ (ถ้ามี)
                // plugin.reloadConfig();
                sender.sendMessage("§aDynamicSurvival reloaded!");
            }

            case "setthirst" -> {
                // /ds setthirst <player> <value>
                if (args.length != 3) {
                    sender.sendMessage("§cUsage: /ds setthirst <player> <value>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage("§cPlayer not found: " + args[1]);
                    return true;
                }
                try {
                    double value = Double.parseDouble(args[2]);
                    plugin.getThirstManager().setThirst(target, value);
                    sender.sendMessage("§aSet " + target.getName() + "'s thirst to " + value + "%.");
                    target.sendMessage("§aYour thirst has been set to " + value + "%.");
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cInvalid number: " + args[2]);
                }
            }

            case "settemp" -> {
                // /ds settemp <player> <value>
                if (args.length != 3) {
                    sender.sendMessage("§cUsage: /ds settemp <player> <value>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage("§cPlayer not found: " + args[1]);
                    return true;
                }
                try {
                    double value = Double.parseDouble(args[2]);
                    plugin.getTempManager().setTemperature(target, value);
                    sender.sendMessage("§aSet " + target.getName() + "'s temperature to " + value + "°C.");
                    target.sendMessage("§aYour temperature has been set to " + value + "°C.");
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cInvalid number: " + args[2]);
                }
            }

            case "setseason" -> {
                // ในอนาคตคุณอาจจะเพิ่มระบบฤดูกาล
                sender.sendMessage("§eSeason system is not implemented yet.");
            }

            default -> sender.sendMessage("§cUnknown subcommand. Usage: /ds <setseason|setthirst|settemp|info|reload>");
        }

        return true;
    }
}
