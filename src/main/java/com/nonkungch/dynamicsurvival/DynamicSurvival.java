package com.nonkungch.dynamicsurvival;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import com.nonkungch.dynamicsurvival.managers.TemperatureManager;
import com.nonkungch.dynamicsurvival.managers.ThirstManager;
import com.nonkungch.dynamicsurvival.managers.TimeManager; // ตรวจสอบว่า package นี้ถูกต้อง

import java.util.Random;

// ====================================================================================
// ENUMERATIONS (Season)
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

    public String getThaiName() { return thaiName; }
    public String getChatColor() { return chatColor; }
    public static Season getNextSeason(Season current) {
        return Season.values()[(current.ordinal() + 1) % Season.values().length];
    }
}

// ====================================================================================
// MAIN PLUGIN CLASS
// ====================================================================================

public class DynamicSurvival extends JavaPlugin implements Listener { // เพิ่ม implements Listener

    private static DynamicSurvival instance;

    // Managers
    private ConfigManager configManager;
    private TemperatureManager temperatureManager;
    private ThirstManager thirstManager;
    private TimeManager timeManager;
    
    // Core State
    private World trackedWorld;
    private final Random random = new Random(); // เปลี่ยนเป็น final
    
    // Season & Time State
    private Season currentSeason = Season.SPRING;
    private int currentDay = 1;
    private long lastDayTime = 0; 
    
    // **WEATHER STATE**
    private boolean isWeatherRunning = false;
    private int weatherDurationLeft = 0; 

    private BukkitAudiences adventure;

    @Override
    public void onEnable() {
        instance = this;
        adventure = BukkitAudiences.create(this);
        
        // 1. Init Config & Managers
        this.configManager = new ConfigManager(this);
        configManager.loadConfig();
        this.temperatureManager = new TemperatureManager(this);
        this.thirstManager = new ThirstManager(this);
        this.timeManager = new TimeManager(this);
        
        // 2. Setup World (ใช้ Config)
        String worldName = configManager.getTrackedWorldName();
        trackedWorld = Bukkit.getWorld(worldName);
        if (trackedWorld == null) {
            getLogger().warning("ไม่พบโลกที่ระบุใน Config: " + worldName + "! ใช้โลกแรกแทน");
            if (!Bukkit.getWorlds().isEmpty()) {
                trackedWorld = Bukkit.getWorlds().get(0);
            }
        }
        if (trackedWorld != null) {
            this.isWeatherRunning = trackedWorld.hasStorm() || trackedWorld.isThundering();
        }
        
        // 3. Register Events & Command
        // ต้องมั่นใจว่า DSCommand และ CalendarGUI.java อยู่ใน package เดียวกัน
        getCommand("ds").setExecutor(new DSCommand(this)); // Assume DSCommand exists
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this); // assumed PlayerListener exists
        getServer().getPluginManager().registerEvents(new ThirstListener(this), this); 
        getServer().getPluginManager().registerEvents(this, this); // สำหรับ PlayerJoin/Quit

        // 4. Start Loops
        startSeasonAndWeatherLoop(); 
        startStatsUpdateLoop();
        
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
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // เมื่อผู้เล่นเข้าเกม ค่าเริ่มต้นจะถูกกำหนดใน Manager
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // อาจจะลบข้อมูลผู้เล่นออกจาก Map ใน Manager เพื่อประหยัดหน่วยความจำ
    }


    // --- Getters ---
    public static DynamicSurvival getInstance() { return instance; }
    public ConfigManager getConfigManager() { return configManager; }
    public TemperatureManager getTempManager() { return temperatureManager; }
    public ThirstManager getThirstManager() { return thirstManager; }
    public TimeManager getTimeManager() { return timeManager; }
    public Season getCurrentSeason() { return currentSeason; }
    public int getCurrentDay() { return currentDay; }
    public Random getRandom() { return random; } // แก้ไข: เพิ่ม Getter สำหรับ random
    
    public void sendActionBar(Player player, double temp, double thirst) {
        ChatColor tempColor = getTemperatureColor(temp);
        String thirstBar = getThirstBar(thirst);
        String seasonDisplay = currentSeason.getChatColor() + currentSeason.getThaiName();
        String dayDisplay = String.valueOf(currentDay);

        String message = String.format("§f[ปฏิทิน: %s§f - วันที่ %s]   |   [อุณหภูมิ: %s%.1f°C§f]   |   [น้ำ: %s§f]",
            seasonDisplay, dayDisplay, 
            tempColor, temp, thirstBar);

        Component component = LegacyComponentSerializer.legacySection().deserialize(message);
        adventure.player(player).sendActionBar(component);
    }
    
    // ====================================================================================
    // 1. ระบบฤดูกาล & ปฏิทิน & สภาพอากาศ (Logic ถูกต้องตาม Config)
    // ====================================================================================

    private void startSeasonAndWeatherLoop() {
        // Logic การตรวจจับวันใหม่ และการเริ่ม/หยุดสภาพอากาศ... (โค้ดเดิม)
        new BukkitRunnable() {
            @Override
            public void run() {
                if (trackedWorld == null) return;
                
                long currentTime = trackedWorld.getFullTime();
                if (currentTime / 24000 > lastDayTime / 24000) {
                    onNewDay(); 
                }
                lastDayTime = currentTime;
                
                if (isWeatherRunning && trackedWorld.getFullTime() % 24000 == 1000) { 
                    if (weatherDurationLeft > 0) {
                         weatherDurationLeft--;
                    }
                    if (weatherDurationLeft <= 0) {
                        stopWeather();
                    }
                }
            }
        }.runTaskTimer(this, 20L, 20L);
    }
    
    private void onNewDay() {
        currentDay++;
        
        if (!isWeatherRunning) {
            checkAndStartWeather();
        }

        if (currentDay > configManager.getSeasonDuration(currentSeason)) {
            changeSeason();
        } else {
            int daysLeft = configManager.getSeasonDuration(currentSeason) - currentDay;
            Bukkit.broadcastMessage(String.format("§e[ปฏิทิน] วันที่ %d ใน %s%s§e (%d วันที่เหลือ)", 
                currentDay, currentSeason.getChatColor(), currentSeason.getThaiName(), daysLeft));
        }
    }
    
    private void checkAndStartWeather() {
        
        int normalMin = configManager.getNormalRainMinDay();
        int normalMax = configManager.getNormalRainMaxDay();
        double normalChance = configManager.getNormalRainChance();
        int normalDurationMin = configManager.getNormalRainDurationMin();
        int normalDurationMax = configManager.getNormalRainDurationMax();

        // 1. ฝนปกติ
        if (currentDay >= normalMin && currentDay <= normalMax) {
            if (random.nextDouble() < normalChance) { 
                int duration = random.nextInt(normalDurationMax - normalDurationMin + 1) + normalDurationMin;
                startRain(duration); 
                return;
            }
        }

        int heavyMin = configManager.getHeavyStormMinDay();
        int heavyMax = configManager.getHeavyStormMaxDay();
        double heavyChance = configManager.getHeavyStormChance();
        int heavyDurationMin = configManager.getHeavyStormDurationMin();
        int heavyDurationMax = configManager.getHeavyStormDurationMax();

        // 2. ฝนตกหนัก
        if (currentDay >= heavyMin && currentDay <= heavyMax) {
            if (random.nextDouble() < heavyChance) { 
                int duration = random.nextInt(heavyDurationMax - heavyDurationMin + 1) + heavyDurationMin;
                startStorm(duration); 
                return;
            }
        }
        
        // รีเซ็ตตัวนับวันถ้าเกินรอบสูงสุดของโอกาสเกิดสภาพอากาศ
        if (currentDay > heavyMax) {
            currentDay = 1; 
        }
    }

    private void startRain(int durationDays) {
        if (trackedWorld == null) return;
        trackedWorld.setStorm(true);
        trackedWorld.setThundering(false);
        trackedWorld.setWeatherDuration(durationDays * 24000); 
        isWeatherRunning = true;
        weatherDurationLeft = durationDays;
        Bukkit.broadcastMessage("§e[สภาพอากาศ] ฝนเริ่มตกปกติ คาดว่าจะนาน " + durationDays + " วัน.");
    }

    private void startStorm(int durationDays) {
        if (trackedWorld == null) return;
        trackedWorld.setStorm(true);
        trackedWorld.setThundering(true);
        trackedWorld.setWeatherDuration(durationDays * 24000); 
        isWeatherRunning = true;
        weatherDurationLeft = durationDays;
        Bukkit.broadcastMessage("§c[สภาพอากาศ] ฝนตกหนักและมีพายุ คาดว่าจะนาน " + durationDays + " วัน!");
    }
    
    private void stopWeather() {
        if (trackedWorld == null) return;
        trackedWorld.setStorm(false);
        trackedWorld.setThundering(false);
        isWeatherRunning = false;
        weatherDurationLeft = 0;
        Bukkit.broadcastMessage("§e[สภาพอากาศ] เมฆฝนเคลื่อนตัวผ่านไป ท้องฟ้าแจ่มใสแล้ว.");
    }
    
    private void changeSeason() {
        Season nextSeason = Season.getNextSeason(currentSeason);
        currentSeason = nextSeason;
        currentDay = 1; 
        
        Bukkit.broadcastMessage(String.format("§6[ปฏิทิน] ฤดูกาลเปลี่ยนเป็น: %s%s§6 แล้ว!", 
            currentSeason.getChatColor(), currentSeason.getThaiName()));
        
        // รัน SeasonProcessor ใน Asynchronous thread
        new SeasonProcessor(this, currentSeason).runTaskAsynchronously(this);
    }

    // ====================================================================================
    // 2. ระบบอัปเดตสถานะผู้เล่น
    // ====================================================================================
    
    private void startStatsUpdateLoop() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    updatePlayer(player);
                }
            }
        }.runTaskTimer(this, 0, configManager.getUpdateFrequencyTicks()); // ใช้ค่า Config
    }

    private void updatePlayer(Player player) {
        double temp = temperatureManager.getTemperature(player);
        double thirst = thirstManager.getThirst(player);
        
        // 1. Thirst Loss Calculation (ใช้ Config)
        double thirstLoss = configManager.getBaseThirstLoss();
        if (player.isSprinting()) {
            thirstLoss *= configManager.getSprintMultiplier();
        }

        // Apply Thirst Loss
        thirstManager.setThirst(player, thirst - thirstLoss);
        
        // 2. Temperature Logic (การปรับค่าอุณหภูมิพื้นฐาน)
        double tempChange = -0.1; // ลดลงช้าๆ เมื่อไม่ได้ทำอะไร
        if (trackedWorld != null && trackedWorld.hasStorm()) {
            // ลดอุณหภูมิเพิ่มเติมเมื่อมีฝน
            tempChange += configManager.getTempModifierOnRain() / 10.0; 
        }
        temperatureManager.setTemperature(player, temp + tempChange);

        // 3. Apply Effects (ใช้ Config)
        double currentTemp = temperatureManager.getTemperature(player);
        double currentThirst = thirstManager.getThirst(player);
        double damage = configManager.getDamageAmount();

        if (currentTemp < configManager.getFreezingThreshold()) {
            player.setFreezeTicks(100);
            player.damage(damage); 
        }
        if (currentTemp > configManager.getHotThreshold()) {
            player.damage(damage); 
        }
        if (currentThirst <= configManager.getThirstDangerLevel()) {
            player.damage(damage); 
        }
        
        // 4. Display Status (ใช้ Config)
        if (configManager.getDisplayMode().equalsIgnoreCase("SCOREBOARD")) {
            // (ต้องมี ScoreboardManager.java)
        } else {
            sendActionBar(player, currentTemp, currentThirst);
        }
    }
    
    private ChatColor getTemperatureColor(double temp) {
        if (temp < configManager.getFreezingThreshold()) return ChatColor.AQUA;
        if (temp > configManager.getHotThreshold()) return ChatColor.RED;
        return ChatColor.YELLOW;
    }
    
    private String getThirstBar(double thirst) {
        int max = configManager.getMaxThirst();
        int bars = 10;
        int filledBars = (int) Math.ceil(thirst / max * bars);
        
        StringBuilder bar = new StringBuilder();
        
        ChatColor color;
        if (thirst <= configManager.getThirstDangerLevel()) {
            color = ChatColor.RED;
        } else if (thirst < max / 2) {
            color = ChatColor.YELLOW;
        } else {
            color = ChatColor.BLUE;
        }
        
        bar.append(color).append(ChatColor.BOLD);
        for (int i = 0; i < bars; i++) {
            if (i < filledBars) {
                bar.append("█");
            } else {
                bar.append("§7█");
            }
        }
        bar.append(ChatColor.RESET).append(" (").append(String.format("%.0f", thirst)).append(")");
        return bar.toString();
    }
}
