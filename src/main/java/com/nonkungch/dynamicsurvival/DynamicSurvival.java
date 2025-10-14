package com.nonkungch.dynamicsurvival;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

// ====================================================================================
// ENUMERATIONS
// ====================================================================================

enum Season {
    SPRING("ฤดูใบไม้ผลิ", "§a§l"), 
    SUMMER("ฤดูร้อน", "§6§l"),  
    AUTUMN("ฤดูใบไม้ร่วง", "§c§l"), 
    WINTER("ฤดูหนาว", "§b§l");  

    private final String thaiName;
    private final String chatColor;

    Season(String thaiName, String chatColor) {
        this.thaiName = thaiName;
        this.chatColor = chatColor;
    }

    public Season next() {
        return values()[(ordinal() + 1) % values().length];
    }
    
    public String getThaiName() { return thaiName; }
    public String getChatColor() { return chatColor; }
    
    public void processSeasonStart(DynamicSurvival plugin) {
        new SeasonProcessor(plugin, this).runTask(plugin);
    }
}

// ====================================================================================
// MAIN PLUGIN CLASS: DynamicSurvival
// ====================================================================================

public class DynamicSurvival extends JavaPlugin implements Listener {

    private Season currentSeason = Season.SPRING;
    private int currentDay = 1;
    private long lastDayTime = 0;
    private final Map<Player, PlayerStats> playerStats = new HashMap<>();
    private final Map<Player, Scoreboard> playerBoards = new HashMap<>(); 
    
    public final Random random = new Random(); 
    
    private World trackedWorld; 
    private BukkitAudiences audiences;
    private ConfigManager configManager; 

    @Override
    public void onEnable() {
        getLogger().info("DynamicSurvival Plugin (v" + getDescription().getVersion() + ") กำลังทำงาน!");
        
        // 1. Initialise Managers/API
        this.configManager = new ConfigManager(this);
        this.configManager.loadConfig();
        this.audiences = BukkitAudiences.create(this);
        
        // 2. Setup World
        if (!Bukkit.getWorlds().isEmpty()) {
            trackedWorld = Bukkit.getWorlds().get(0);
        } else {
            getLogger().warning("ไม่พบโลก! ระบบฤดูกาลอาจทำงานไม่ถูกต้อง");
        }
        
        // 3. Register Listeners and Commands
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getPluginManager().registerEvents(new ThirstListener(this), this);
        this.getCommand("ds").setExecutor(new DSCommand(this));
        new CalendarGUI(this); 
        
        // 4. Start Loops
        startSeasonAndWeatherLoop();
        startStatsUpdateLoop();
    }
    
    @Override
    public void onDisable() {
        getLogger().info("DynamicSurvival Plugin ถูกปิดการทำงาน!");
        Bukkit.getScheduler().cancelTasks(this);
        if (this.audiences != null) {
            this.audiences.close();
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        playerStats.putIfAbsent(player, new PlayerStats(configManager.getBaseTemp(currentSeason), configManager.getMaxThirst()));
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard()); 
    }
    
    @EventHandler 
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        playerBoards.remove(player);
    }
    
    // ====================================================================================
    // GETTERS
    // ====================================================================================
    public Season getCurrentSeason() { return currentSeason; }
    public int getCurrentDay() { return currentDay; }
    public PlayerStats getPlayerStats(Player p) { 
        return playerStats.getOrDefault(p, new PlayerStats(configManager.getBaseTemp(currentSeason), configManager.getMaxThirst()));
    }
    public ConfigManager getConfigManager() { return configManager; }
    
    // ====================================================================================
    // 1. ระบบฤดูกาล & ปฏิทิน & สภาพอากาศ
    // ====================================================================================

    private void startSeasonAndWeatherLoop() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (trackedWorld == null) return;
                
                long currentTime = trackedWorld.getFullTime();
                if (currentTime / 24000 > lastDayTime / 24000) {
                    onNewDay();
                }
                lastDayTime = currentTime;

                // แก้ไข: getWeatherChance() ถูกเพิ่มใน ConfigManager แล้ว
                if (random.nextDouble() < configManager.getWeatherChance()) { 
                    applyRandomWeather();
                }
            }
        }.runTaskTimer(this, 20L, 20L);
    }
    
    private void onNewDay() {
        currentDay++;
        
        if (currentDay > configManager.getSeasonDuration(currentSeason)) {
            changeSeason();
        } else {
            int daysLeft = configManager.getSeasonDuration(currentSeason) - currentDay;
            Bukkit.broadcastMessage(String.format("§e[ปฏิทิน] วันที่ %d ใน %s%s§e (%d วันที่เหลือ)", 
                currentDay, currentSeason.getChatColor(), currentSeason.getThaiName(), daysLeft));
        }
    }

    private void changeSeason() {
        currentSeason = currentSeason.next();
        currentDay = 1;

        String msg = String.format("§l§e--- %sการเปลี่ยนฤดูกาลครั้งใหญ่!%s ---", ChatColor.GOLD, ChatColor.RESET);
        String seasonMsg = String.format("%s! ฤดูกาลใหม่คือ: %s%s", ChatColor.YELLOW, currentSeason.getChatColor(), currentSeason.getThaiName());
        
        Bukkit.broadcastMessage(msg);
        Bukkit.broadcastMessage(seasonMsg);

        currentSeason.processSeasonStart(this);
        
        Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage("§7(โปรดทราบว่าสภาพแวดล้อมได้เปลี่ยนไปแล้ว!)"));
    }
    
    private void applyRandomWeather() {
        if (trackedWorld == null) return;
        
        if (trackedWorld.hasStorm() || trackedWorld.isThundering()) {
            if (random.nextDouble() < 0.5) { 
                trackedWorld.setStorm(false);
                trackedWorld.setThundering(false);
                Bukkit.broadcastMessage("§e[สภาพอากาศ] เมฆฝนเคลื่อนตัวผ่านไป ท้องฟ้าแจ่มใสแล้ว.");
                return;
            }
        }
        
        double chance = random.nextDouble();
        
        if (chance < 0.1) {
            trackedWorld.setStorm(true);
            trackedWorld.setThundering(true); 
            Bukkit.broadcastMessage("§4[สภาพอากาศ] เกิดพายุตกหนัก! อุณหภูมิจะลดลงอย่างรวดเร็ว.");
        } else if (chance < 0.25) { 
            trackedWorld.setStorm(true);
            trackedWorld.setThundering(false); 
            Bukkit.broadcastMessage("§b[สภาพอากาศ] มีฝนตก/หิมะตก อุณหภูมิจะลดลง.");
        } else if (chance < 0.45) {
            trackedWorld.setStorm(false);
            trackedWorld.setThundering(false); 
            Bukkit.broadcastMessage("§e[สภาพอากาศ] ท้องฟ้าแจ่มใส อากาศดี.");
        }
    }

    // ====================================================================================
    // 2. ระบบอุณหภูมิ & หลอดน้ำ (แสดงผลใน Action Bar/Scoreboard)
    // ====================================================================================

    private void startStatsUpdateLoop() {
        new BukkitRunnable() {
            @Override
            public void run() {
                String mode = configManager.getDisplayMode(); 
                
                for (Player player : Bukkit.getOnlinePlayers()) {
                    PlayerStats stats = getPlayerStats(player);
                    
                    float newTemp = calculateTemperature(player); 
                    stats.setTemperature(newTemp);
                    
                    updateThirst(player, stats, newTemp);
                    
                    if (mode.equals("SCOREBOARD")) {
                        sendScoreboardUpdate(player, stats);
                        Component emptyComponent = LegacyComponentSerializer.legacySection().deserialize("");
                        audiences.player(player).sendActionBar(emptyComponent); 
                    } else { // Defaults to ACTION_BAR
                        sendActionBarUpdate(player, stats);
                        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
                        playerBoards.remove(player);
                    }
                    
                    applyTemperatureEffects(player, newTemp);
                }
            }
        }.runTaskTimer(this, 40L, 40L);
    }
    
    // Helper Method: ดึงสีตามอุณหภูมิ
    private ChatColor getTemperatureColor(float temp) {
        // แก้ไข: getThresholds ถูกเพิ่มใน ConfigManager แล้ว
        if (temp < configManager.getColdTempThreshold()) {
            return ChatColor.DARK_BLUE; 
        } else if (temp < configManager.getFreezingTempThreshold()) {
            return ChatColor.BLUE; 
        } else if (temp > configManager.getDeadlyHotTempThreshold()) {
            return ChatColor.DARK_RED;
        } else if (temp > configManager.getHotTempThreshold()) {
            return ChatColor.RED; 
        } else if (temp > configManager.getNormalTempThreshold()) {
            return ChatColor.YELLOW; 
        } else {
            return ChatColor.GREEN; 
        }
    }

    // Helper Method: สร้างแถบหลอดน้ำ
    private String getThirstBar(PlayerStats stats) {
        int thirstLevel = stats.getThirst();
        String thirstBar = "";
        int maxThirst = configManager.getMaxThirst();
        int fullBlocks = thirstLevel / (maxThirst / 10);
        int emptyBlocks = 10 - fullBlocks;
        
        thirstBar += ChatColor.AQUA + "💧".repeat(fullBlocks);
        thirstBar += ChatColor.DARK_GRAY + "💧".repeat(emptyBlocks);
        return thirstBar;
    }
    
    // Scoreboard Update Method
    private void sendScoreboardUpdate(Player player, PlayerStats stats) {
        Scoreboard board = playerBoards.get(player);
        if (board == null || board == Bukkit.getScoreboardManager().getMainScoreboard()) {
            board = Bukkit.getScoreboardManager().getNewScoreboard();
            Objective obj = board.registerNewObjective("ds_stats", "dummy", ChatColor.AQUA + "§lDynamicSurvival");
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
            playerBoards.put(player, board);
            player.setScoreboard(board);
        }

        Objective obj = board.getObjective("ds_stats");
        if (obj == null) return; 

        for (String entry : board.getEntries()) {
            board.resetScores(entry);
        }

        // --- เตรียมข้อมูล ---
        String seasonLine = currentSeason.getChatColor() + "§l" + currentSeason.getThaiName();
        String dayLine = "§eวันที่: §f" + currentDay + " / " + configManager.getSeasonDuration(currentSeason);
        ChatColor tempColor = getTemperatureColor(stats.getTemperature());
        String tempLine = "§eอุณหภูมิ: " + tempColor + String.format("%.1f°C", stats.getTemperature());
        String thirstLine = getThirstBar(stats);

        // --- อัปเดต Scores (เรียงจากล่างขึ้นบน) ---
        obj.getScore("§7§m=============").setScore(7); 
        obj.getScore(seasonLine).setScore(6); 
        obj.getScore(dayLine).setScore(5); 
        obj.getScore("§a").setScore(4); 
        obj.getScore(tempLine).setScore(3); 
        obj.getScore("§b").setScore(2); 
        obj.getScore("§eน้ำ: " + thirstLine).setScore(1); 
        obj.getScore("§7§m------------").setScore(0); 
    }
    
    private float calculateTemperature(Player player) {
        float temp = configManager.getBaseTemp(currentSeason);
        
        long time = player.getWorld().getTime(); 
        if (time < 12000) { 
            temp += (12000 - time) / 12000.0f * 5.0f; 
        } else { 
            temp -= (time - 12000) / 12000.0f * 5.0f; 
        }

        if (player.getWorld().hasStorm()) temp -= 3.0f; 
        if (player.getWorld().isThundering()) temp -= 5.0f; 

        String biome = player.getLocation().getBlock().getBiome().toString();
        if (biome.contains("DESERT")) temp += 5.0f;
        if (biome.contains("SNOW") || biome.contains("TAIGA")) temp -= 5.0f;

        if (player.getLocation().getBlock().getRelative(0, -1, 0).getType() == Material.FIRE || 
            player.getLocation().getBlock().getRelative(0, -1, 0).getType() == Material.LAVA) {
            temp += 5.0f;
        }

        return temp;
    }
    
    private void updateThirst(Player player, PlayerStats stats, float currentTemp) {
        int thirstLoss = configManager.getBaseThirstLoss();
        // แก้ไข: getHotTempThreshold() ถูกเพิ่มใน ConfigManager แล้ว
        if (currentTemp > configManager.getHotTempThreshold()) thirstLoss = 3; 
        if (player.isSprinting()) thirstLoss += 1; 
        
        stats.setThirst(Math.max(0, stats.getThirst() - thirstLoss));
    }
    
    private void applyTemperatureEffects(Player player, float temp) {
        // แก้ไข: getColdTempThreshold() และอื่นๆ ถูกเพิ่มใน ConfigManager แล้ว
        if (temp < configManager.getColdTempThreshold()) {
            player.damage(1.0);
            player.sendTitle("", "§bคุณกำลังจะแข็งตาย! (-" + (int)Math.abs(temp) + "°C)", 10, 20, 10);
        } else if (temp < configManager.getFreezingTempThreshold()) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 0));
        } else if (temp > configManager.getDeadlyHotTempThreshold()) {
            player.damage(1.0);
            player.sendTitle("", "§4คุณเป็นโรคลมแดด! (+" + (int)temp + "°C)", 10, 20, 10);
        } else if (temp > configManager.getHotTempThreshold()) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 40, 0));
        }

        if (getPlayerStats(player).getThirst() <= configManager.getThirstDangerLevel()) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 40, 0));
        }
    }
    
    private void sendActionBarUpdate(Player player, PlayerStats stats) {
        ChatColor tempColor = getTemperatureColor(stats.getTemperature());
        String thirstBar = getThirstBar(stats);

        String message = String.format("§f[ปฏิทิน: %s%s§f - วันที่ %d]   |   [อุณหภูมิ: %s%.1f°C§f]   |   [น้ำ: %s§f]",
            currentSeason.getChatColor(), currentSeason.getThaiName(), currentDay, 
            tempColor, stats.getTemperature(), thirstBar);

        Component component = LegacyComponentSerializer.legacySection().deserialize(message);
        Audience playerAudience = this.audiences.player(player);
        playerAudience.sendActionBar(component);
    }

    // ====================================================================================
    // PLAYER STATS CLASS
    // ====================================================================================

    public static class PlayerStats {
        private float temperature;
        private int thirst; // 0-MaxThirst

        public PlayerStats(float temp, int thirst) {
            this.temperature = temp;
            this.thirst = thirst;
        }
        
        public float getTemperature() { return temperature; }
        public void setTemperature(float temperature) { this.temperature = temperature; }
        public int getThirst() { return thirst; }
        public void setThirst(int thirst) { this.thirst = thirst; }
        public void addThirst(int amount, int maxThirst) {
            this.thirst = Math.min(maxThirst, this.thirst + amount);
        }
    }
}
