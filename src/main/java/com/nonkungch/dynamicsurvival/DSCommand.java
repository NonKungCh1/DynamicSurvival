package com.nonkungch.dynamicsurvival;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DSCommand implements CommandExecutor {

    private final DynamicSurvival plugin;

    public DSCommand(DynamicSurvival plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        
        if (args.length == 0) {
            sender.sendMessage("§e--- DynamicSurvival (v" + plugin.getDescription().getVersion() + ") ---");
            if (sender.hasPermission("ds.calendar")) {
            sender.sendMessage("§e/ds calendar §7- เปิดปฏิทิน GUI");
            }
            if (sender.hasPermission("ds.admin")) {
                sender.sendMessage("§e/ds reload §7- โหลด Config ใหม่");
            }
            return true;
        }

        String subCommand = args[0].toLowerCase();

        if (subCommand.equals("reload")) {
            if (!sender.hasPermission("ds.admin")) {
                sender.sendMessage("§cคุณไม่มีสิทธิ์ใช้คำสั่งนี้!");
                return true;
            }
            plugin.getConfigManager().loadConfig();
            sender.sendMessage("§a[DynamicSurvival] โหลด Config ใหม่แล้ว!");
            return true;
        } 
        
        if (subCommand.equals("calendar")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cต้องเป็นผู้เล่นเท่านั้นที่ใช้คำสั่งนี้ได้");
                return true;
            }
            CalendarGUI.openCalendar((Player) sender, plugin);
            return true;
        }

        sender.sendMessage("§cคำสั่งไม่ถูกต้อง. ใช้ /ds เพื่อดูรายการคำสั่ง.");
        return true;
    }
}
