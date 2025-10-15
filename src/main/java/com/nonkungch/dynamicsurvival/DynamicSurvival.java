package com.nonkungch.dynamicsurvival;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.Material;

import java.util.*;

// (Enum Season ไม่มีการเปลี่ยนแปลง)
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

public class DynamicSurvival extends JavaPlugin implements Listener {

    private Season currentSeason = Season.SPRING;
    private int currentDay = 1;
    private long lastDayTime = 0;
    private long nextWeatherChangeTick = 0;

    private final Map<Player, PlayerStats> playerStats = new HashMap<>();
    private final Map<Player, Scoreboard> playerBoards = new HashMap<>();
    public final Random random = new Random();
    private World trackedWorld;
    private ConfigManager configManager;

    @Override
    public void onEnable() {
        getLogger().info("DynamicSurvival Plugin (v" + getDescription().getVersion() + ") is enabled!");
        
        this.configManager = new ConfigManager(this);
        this.configManager.loadConfig();
        
        if (!Bukkit.getWorlds().isEmpty()) {
            trackedWorld = Bukkit.getWorlds().get(0);
        } else {
            getLogger().warning("No world found! Season system might not work correctly.");
        }
        
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getPluginManager().registerEvents(new ThirstListener(this), this);
        this.getCommand("ds").setExecutor(new DSCommand(this));
        new CalendarGUI(this);
        
        startSeasonAndWeatherLoop();
        startStatsUpdateLoop();
    }

    @Override
    public void onDisable() {
        getLogger().info("DynamicSurvival Plugin is disabled!");
        Bukkit.getScheduler().cancelTasks(this);
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
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    public Season getCurrentSeason() { return currentSeason; }
    public int getCurrentDay() { return currentDay; }
    public PlayerStats getPlayerStats(Player p) {
        return playerStats.getOrDefault(p, new PlayerStats(configManager.getBaseTemp(currentSeason), configManager.getMaxThirst()));
    }
    public ConfigManager getConfigManager() { return configManager; }

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
                
                if (currentTime >= nextWeatherChangeTick) {
                    applyRandomWeather();
                    long cooldownTicks = (long) configManager.getWeatherChangeCooldownMinutes() * 60 * 20;
                    nextWeatherChangeTick = currentTime + cooldownTicks;
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
                Bukkit.broadcastMessage("§e[สภาพอากาศ] ท้องฟ้าแจ่มใสแล้ว");
                return;
            }
        }
        double chance = random.nextDouble();
        if (chance < 0.1) {
            trackedWorld.setStorm(true);
            trackedWorld.setThundering(true);
            Bukkit.broadcastMessage("§4[สภาพอากาศ] เกิดพายุตกหนัก!");
        } else if (chance < 0.25) {
            trackedWorld.setStorm(true);
            trackedWorld.setThundering(false);
            Bukkit.broadcastMessage("§b[สภาพอากาศ] มีฝน/หิมะตก");
        }
    }

    private void startStatsUpdateLoop() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    PlayerStats stats = getPlayerStats(player);
                    float newTemp = calculateTemperature(player);
                    stats.setTemperature(newTemp);
                    updateThirst(player, stats, newTemp);
                    sendScoreboardUpdate(player, stats);
                    
                    // **แก้ไข: ส่งค่า stats เข้าไปในเมธอด**
                    applyTemperatureEffects(player, stats, newTemp);
                }
            }
        }.runTaskTimer(this, 40L, 40L);
    }

    private void sendScoreboardUpdate(Player player, PlayerStats stats) {
        Scoreboard board = playerBoards.computeIfAbsent(player, p -> {
            Scoreboard newBoard = Bukkit.getScoreboardManager().getNewScoreboard();
            p.setScoreboard(newBoard);
            return newBoard;
        });

        Objective obj = board.getObjective("ds_stats");
        if (obj == null) {
            obj = board.registerNewObjective("ds_stats", "dummy", ChatColor.translateAlternateColorCodes('&', configManager.getScoreboardTitle()));
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        }

        obj.setDisplayName(ChatColor.translateAlternateColorCodes('&', configManager.getScoreboardTitle()));
        
        for (String entry : board.getEntries()) {
            board.resetScores(entry);
        }

        List<String> lines = configManager.getScoreboardLines();
        int score = lines.size();
        for (String line : lines) {
            String processedLine = replacePlaceholders(line, player, stats);
            
            if (processedLine.isEmpty()) {
                processedLine = "§" + Integer.toHexString(score) + "§r"; 
            }
            
            obj.getScore(processedLine).setScore(score--);
        }
    }

    private String replacePlaceholders(String line, Player player, PlayerStats stats) {
        int daysLeft = configManager.getSeasonDuration(currentSeason) - currentDay;
        
        line = ChatColor.translateAlternateColorCodes('&', line);
        line = line.replace("%player_name%", player.getName());
        line = line.replace("%season_name%", currentSeason.getThaiName());
        line = line.replace("%season_color%", currentSeason.getChatColor());
        line = line.replace("%current_day%", String.valueOf(currentDay));
        line = line.replace("%season_duration%", String.valueOf(configManager.getSeasonDuration(currentSeason)));
        line = line.replace("%days_left%", String.valueOf(daysLeft));
        line = line.replace("%temperature%", getTemperatureColor(stats.getTemperature()) + String.format("%.1f°C", stats.getTemperature()));
        line = line.replace("%thirst_bar%", getThirstBar(stats));
        line = line.replace("%thirst_value%", String.valueOf(stats.getThirst()));
        line = line.replace("%thirst_max%", String.valueOf(configManager.getMaxThirst()));
        
        return line;
    }
    
    private float calculateTemperature(Player player) {
        float temp = configManager.getBaseTemp(currentSeason);
        long time = player.getWorld().getTime();
        if (time < 12000) temp += (12000 - time) / 12000.0f * 5.0f;
        else temp -= (time - 12000) / 12000.0f * 5.0f;
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
        if (currentTemp > configManager.getHotTempThreshold()) thirstLoss *= 2;
        if (player.isSprinting()) thirstLoss += 1;
        stats.setThirst(Math.max(0, stats.getThirst() - thirstLoss));
    }

    /**
     * **แก้ไข: เพิ่มพารามิเตอร์ PlayerStats stats**
     */
    private void applyTemperatureEffects(Player player, PlayerStats stats, float temp) {
        if (temp < configManager.getColdTempThreshold()) {
            player.damage(1.0);
            player.sendTitle("", "§bคุณกำลังจะแข็งตาย!", 10, 40, 10);
        } else if (temp < configManager.getFreezingTempThreshold()) {
             player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 0));
        } else if (temp > configManager.getDeadlyHotTempThreshold()) {
            player.damage(1.0);
            player.sendTitle("", "§cคุณกำลังจะถูกเผา!", 10, 40, 10);
        } else if (temp > configManager.getHotTempThreshold()) {
             player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 0));
        }

        // **แก้ไข: ตอนนี้เมธอดรู้จักตัวแปร stats แล้ว**
        if (stats.getThirst() <= configManager.getThirstDangerLevel()) {
             player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 60, 0));
        }
    }
    
    private ChatColor getTemperatureColor(float temp) {
        if (temp < configManager.getColdTempThreshold()) return ChatColor.DARK_BLUE;
        if (temp < configManager.getFreezingTempThreshold()) return ChatColor.BLUE;
        if (temp > configManager.getDeadlyHotTempThreshold()) return ChatColor.DARK_RED;
        if (temp > configManager.getHotTempThreshold()) return ChatColor.RED;
        if (temp > configManager.getNormalTempThreshold()) return ChatColor.YELLOW;
        return ChatColor.GREEN;
    }

    private String getThirstBar(PlayerStats stats) {
        int thirstLevel = stats.getThirst();
        int maxThirst = configManager.getMaxThirst();
        int fullBlocks = (maxThirst > 0) ? (thirstLevel * 10 / maxThirst) : 0;
        int emptyBlocks = 10 - fullBlocks;
        return "§b" + "💧".repeat(fullBlocks) + "§8" + "💧".repeat(emptyBlocks);
    }
    
    public static class PlayerStats {
        private float temperature;
        private int thirst;
        public PlayerStats(float temp, int thirst) { this.temperature = temp; this.thirst = thirst; }
        public float getTemperature() { return temperature; }
        public void setTemperature(float t) { this.temperature = t; }
        public int getThirst() { return thirst; }
        public void setThirst(int t) { this.thirst = t; }
        public void addThirst(int amount, int max) { this.thirst = Math.min(max, this.thirst + amount); }
    }
}
