package com.nonkungch.dynamicsurvival.commands;

import com.nonkungch.dynamicsurvival.DynamicSurvival;
import com.nonkungch.dynamicsurvival.managers.TimeManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("dynamicsurvival.admin")) {
            sender.sendMessage(ChatColor.RED + "คุณไม่มีสิทธิ์ใช้คำสั่งนี้");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "/ds <setseason|setthirst|settemp|info>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "setseason":
                return handleSetSeason(sender, args);
            case "setthirst":
                return handleSetPlayerStat(sender, args, "thirst", plugin.getThirstManager());
            case "settemp":
                return handleSetPlayerStat(sender, args, "temp", plugin.getTempManager());
            case "info":
                return handleInfo(sender);
            default:
                sender.sendMessage(ChatColor.RED + "คำสั่งไม่ถูกต้อง: " + args[0]);
                return true;
        }
    }
    
    private boolean handleSetSeason(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /ds setseason <spring|summer|autumn|winter>");
            return true;
        }
        int seasonValue;
        try {
            seasonValue = parseSeason(args[1]);
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + "ฤดูกาลไม่ถูกต้อง: " + args[1]);
            return true;
        }

        plugin.getTimeManager().setGlobalScore("season", seasonValue);
        plugin.getTimeManager().setGlobalScore("global_timer", 0); // รีเซ็ตวันในฤดูกาล
        plugin.getTimeManager().changeSeason(); // รันฟังก์ชัน changeSeason เพื่อ Broadcast และ Effect
        sender.sendMessage(ChatColor.GREEN + "ตั้งค่าฤดูกาลเป็น " + plugin.getTimeManager().getSeasonName(seasonValue) + " และรีเซ็ตวันแล้ว");
        return true;
    }

    private boolean handleSetPlayerStat(CommandSender sender, String[] args, String statName, Object manager) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /ds set" + statName + " <player> <value>");
            return true;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "ไม่พบผู้เล่น: " + args[1]);
            return true;
        }
        int value;
        try {
            value = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "ค่าต้องเป็นตัวเลข");
            return true;
        }

        if (statName.equals("thirst")) {
            plugin.getThirstManager().setScore(target, "thirst", value);
        } else {
            plugin.getTemperatureManager().setScore(target, "temp", value);
        }

        String msgKey = statName.equals("thirst") ? "messages.thirst-set" : "messages.temp-set";
        String message = plugin.getConfig().getString(msgKey, "&eค่าถูกตั้งค่าแล้ว!");
        message = message.replace("%value%", String.valueOf(value));
        target.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        sender.sendMessage(ChatColor.GREEN + "ตั้งค่า " + statName + " ของ " + target.getName() + " เป็น " + value + " แล้ว");
        return true;
    }
    
    private boolean handleInfo(CommandSender sender) {
        TimeManager tm = plugin.getTimeManager();
        sender.sendMessage(ChatColor.YELLOW + "--- Dynamic Survival Info ---");
        sender.sendMessage(ChatColor.AQUA + "Current Season: " + tm.getSeasonDisplay());
        sender.sendMessage(ChatColor.AQUA + "Game Day: " + tm.getGameDay());
        sender.sendMessage(ChatColor.AQUA + "Season Progress: " + tm.getSeasonMonthUI());
        sender.sendMessage(ChatColor.YELLOW + "-----------------------------");
        return true;
    }
    
    private int parseSeason(String input) {
        switch (input.toLowerCase()) {
            case "spring": return TimeManager.SPRING;
            case "summer": return TimeManager.SUMMER;
            case "autumn": return TimeManager.AUTUMN;
            case "winter": return TimeManager.WINTER;
            default: throw new IllegalArgumentException("Invalid season name");
        }
    }
                                      }
