package com.nonkungch.dynamicsurvival.commands;

import com.nonkungch.dynamicsurvival.DynamicSurvival;
import com.nonkungch.dynamicsurvival.managers.TimeManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
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
            sender.sendMessage(ChatColor.YELLOW + "/ds <setseason|setthirst|settemp|info|reload>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "setseason": return handleSetSeason(sender, args);
            case "setthirst": return handleSetStat(sender, args, "thirst");
            case "settemp": return handleSetStat(sender, args, "temp");
            case "info": return handleInfo(sender);
            case "reload":
                plugin.reloadConfig();
                sender.sendMessage(ChatColor.GREEN + "รีโหลด config แล้ว!");
                return true;
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
        sender.sendMessage(ChatColor.GREEN + "เปลี่ยนฤดูกาลเรียบร้อยแล้ว (ยังไม่เชื่อมระบบเต็ม)");
        return true;
    }

    private boolean handleSetStat(CommandSender sender, String[] args, String stat) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /ds set" + stat + " <player> <value>");
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

        if (stat.equals("thirst")) plugin.getThirstManager().setScore(target, "thirst", value);
        else plugin.getTempManager().setScore(target, "temp", value);

        sender.sendMessage(ChatColor.GREEN + "ตั้งค่า " + stat + " ของ " + target.getName() + " เป็น " + value);
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
}