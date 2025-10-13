package com.nonkungch.dynamicsurvival;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;

import java.util.HashMap;
import java.util.Map;

// คลาสหลักของปลั๊กอิน (DynamicSurvival)
public class DynamicSurvival extends JavaPlugin {

    private static DynamicSurvival instance;

    private TemperatureManager temperatureManager;
    private ThirstManager thirstManager;
    private TimeManager timeManager;

    private BukkitAudiences adventure;

    @Override
    public void onEnable() {
        instance = this;
        // การใช้ Adventure API ต้องใช้ BukkitAudiences
        adventure = BukkitAudiences.create(this); 

        // Init managers
        // manager classes ถูกย้ายมาเป็น nested static class
        this.temperatureManager = new TemperatureManager(this); 
        this.thirstManager = new ThirstManager(this);
        this.timeManager = new TimeManager(this);

        // Register command & listener
        // ใช้ชื่อคำสั่ง 'ds'
        getCommand("ds").setExecutor(new CommandManager(this));
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        // เริ่มระบบอัปเดตทุกวินาที
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    updatePlayer(player);
                }
            }
        }.runTaskTimer(this, 0, 20); // 20 ticks = 1 วินาที

        getLogger().info("DynamicSurvival enabled!");
    }

    @Override
    public void onDisable() {
        if (adventure != null) {
            adventure.close();
            adventure = null;
        }
        getLogger().info("DynamicSurvival disabled!");
    }

    public static DynamicSurvival getInstance() {
        return instance;
    }

    public TemperatureManager getTempManager() {
        return temperatureManager;
    }

    public ThirstManager getThirstManager() {
        return thirstManager;
    }

    public TimeManager getTimeManager() {
        return timeManager;
    }

    // ส่ง ActionBar ผ่าน Adventure Platform
    public void sendActionBar(Player player, String message) {
        // ใช้ Component.text() จาก Adventure API
        adventure.player(player).sendActionBar(Component.text(message));
    }

    // ระบบหลัก: อัปเดตผู้เล่น
    private void updatePlayer(Player player) {
        double temp = temperatureManager.getTemperature(player);
        double thirst = thirstManager.getThirst(player);

        // ลดค่าตามเวลา
        temp -= 0.1; // ลดอุณหภูมิลง 0.1 ทุกวินาที
        thirst -= 0.5; // ลดความกระหายน้ำ 0.5% ทุกวินาที

        temperatureManager.setTemperature(player, temp);
        thirstManager.setThirst(player, thirst);

        // ส่ง ActionBar แสดงสถานะ
        sendActionBar(player,
                "§eTemp: §b" + String.format("%.1f", temp) + "°C §7| Thirst: §b" + String.format("%.0f", thirst) + "%");

        // เอฟเฟกต์ตามสภาพ
        if (temp < 35) player.setFreezeTicks(100);   // หนาวเกิน → Freezing
        if (temp > 40) player.damage(1);            // ร้อนเกิน → Damage
        if (thirst < 20) player.damage(1);          // กระหายน้ำ → Damage
    }

    // =================================================================================
    //                                NESTED STATIC CLASSES (เดิมคือไฟล์แยก)
    // =================================================================================

    // 1. PlayerListener.java (ตัวจัดการ Events)
    public static class PlayerListener implements Listener {

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

    // 2. CommandManager.java (ตัวจัดการคำสั่ง /ds)
    public static class CommandManager implements CommandExecutor {

        private final DynamicSurvival plugin;

        public CommandManager(DynamicSurvival plugin) {
            this.plugin = plugin;
        }

        @Override
        public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

            // ตรวจสอบ Permission 'dynamicsurvival.admin'
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
                    // ณ ตอนนี้แค่แสดงข้อความ reload
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
                    sender.sendMessage("§eSeason system is not implemented yet.");
                }

                default -> sender.sendMessage("§cUnknown subcommand. Usage: /ds <setseason|setthirst|settemp|info|reload>");
            }

            return true;
        }
    }

    // 3. TemperatureManager.java
    public static class TemperatureManager {

        private final DynamicSurvival plugin;
        private final Map<Player, Double> temperatures = new HashMap<>();

        public TemperatureManager(DynamicSurvival plugin) {
            this.plugin = plugin;
        }

        public double getTemperature(Player player) {
            return temperatures.getOrDefault(player, 37.0); // ค่าเริ่มต้น 37.0
        }

        public void setTemperature(Player player, double temp) {
            temperatures.put(player, temp);
        }
    }

    // 4. ThirstManager.java
    public static class ThirstManager {

        private final DynamicSurvival plugin;
        private final Map<Player, Double> thirsts = new HashMap<>();

        public ThirstManager(DynamicSurvival plugin) {
            this.plugin = plugin;
        }

        public double getThirst(Player player) {
            return thirsts.getOrDefault(player, 100.0); // ค่าเริ่มต้น 100.0
        }

        public void setThirst(Player player, double value) {
            thirsts.put(player, value);
        }
    }

    // 5. TimeManager.java
    public static class TimeManager {

        // เปลี่ยน JavaPlugin เป็น DynamicSurvival เพื่อความชัดเจนในการเรียกใช้
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
}
